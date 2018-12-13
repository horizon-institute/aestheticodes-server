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

package uk.ac.horizon.artcodes.server.christmas;

import uk.ac.horizon.artcodes.server.utils.HTTPException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ImageServlet extends uk.ac.horizon.artcodes.server.ImageServlet
{
	private static final int image_size = 512 * 1024;

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		doPut(request, response);
	}

	@Override
	protected String[] getMethods()
	{
		return new String[]{"OPTIONS", "GET", "PUT", "POST", "HEAD"};
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

			verifyApp(request);
			final String id = getImageID(request);
			readImage(id, request);
			setAccessControlHeaders(response);
		}
		catch (HTTPException e)
		{
			e.writeTo(response);
		}
	}
}
