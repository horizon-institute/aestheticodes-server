/*
 * Artcodes recognises a different marker scheme that allows the
 * creation of aesthetically pleasing, even beautiful, codes.
 * Copyright (C) 2013-2015  The University of Nottingham
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package uk.ac.horizon.artcodes.server;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.tools.cloudstorage.*;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.common.io.ByteStreams;
import uk.ac.horizon.artcodes.server.utils.ArtcodeServlet;
import uk.ac.horizon.artcodes.server.utils.HTTPException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.channels.Channels;

public class ImageServlet extends ArtcodeServlet
{
	private static final int image_size = 512 * 1024;

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		final String id = getImageID(req);
		final GcsFileMetadata metadata = getMetadata(req.getServerName(), id);
		if (metadata == null)
		{
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
		else
		{
			BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
			BlobKey blobKey = blobstoreService.createGsBlobKey("/gs/" + req.getServerName() + "/" + id);
			resp.addHeader("Cache-Control", "max-age=31556926");
			setAccessControlHeaders(resp);
			blobstoreService.serve(blobKey, resp);
		}
	}

	public static GcsFileMetadata getMetadata(String serverName, String id) throws IOException
	{
		final GcsService gcsService = GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());
		final GcsFilename filename = new GcsFilename(serverName, id);
		return gcsService.getMetadata(filename);
	}

	protected void readImage(String id, HttpServletRequest request) throws IOException
	{
		final GcsService gcsService = GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());
		final GcsFilename filename = new GcsFilename(request.getServerName(), id);

		final GcsFileMetadata metadata = gcsService.getMetadata(filename);
		if (metadata != null)
		{
			throw new HTTPException(HttpServletResponse.SC_FORBIDDEN, "Cannot modify");
		}

		final BufferedInputStream inputStream = new BufferedInputStream(request.getInputStream());
		final String mimetype = URLConnection.guessContentTypeFromStream(inputStream);
		if (mimetype == null)
		{
			throw new HTTPException(HttpServletResponse.SC_BAD_REQUEST, "Unrecognised image type");
		}

		final GcsFileOptions.Builder fileOptionsBuilder = new GcsFileOptions.Builder();
		fileOptionsBuilder.mimeType(mimetype);
		final GcsFileOptions fileOptions = fileOptionsBuilder.build();
		final GcsOutputChannel outputChannel = gcsService.createOrReplace(filename, fileOptions);

		final HashingOutputStream outputStream = new HashingOutputStream(Hashing.sha256(), Channels.newOutputStream(outputChannel));
		ByteStreams.copy(inputStream, outputStream);

		String hash = outputStream.hash().toString();
		if (!hash.equals(id))
		{
			gcsService.delete(filename);
			throw new HTTPException(HttpServletResponse.SC_BAD_REQUEST, "Invalid hash");
		}

		outputStream.close();
		outputChannel.close();
	}

	@Override
	protected String[] getMethods()
	{
		return new String[]{"OPTIONS", "GET", "PUT", "HEAD"};
	}

	@Override
	public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		try
		{
			if (request.getContentLength() > image_size)
			{
				throw new HTTPException(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Image too large");
			}

			verifyUser(getUser(request));
			final String id = getImageID(request);
			readImage(id, request);
			setAccessControlHeaders(response);
		}
		catch (HTTPException e)
		{
			e.writeTo(response);
		}
	}

	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		final String id = getImageID(req);
		final GcsFileMetadata metadata = getMetadata(req.getServerName(), id);
		setAccessControlHeaders(resp);
		if (metadata == null)
		{
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
		else
		{
			resp.setStatus(HttpServletResponse.SC_OK);
		}
	}

	protected String getImageID(HttpServletRequest req)
	{
		String url = req.getRequestURL().toString();
		return url.substring(url.lastIndexOf("/") + 1);
	}
}
