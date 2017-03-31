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

import com.google.appengine.repackaged.com.google.api.client.util.IOUtils;
import uk.ac.horizon.artcodes.server.utils.ArtcodeServlet;
import uk.ac.horizon.artcodes.server.utils.HTTPException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

public class WellKnownServlet extends ArtcodeServlet
{
	public WellKnownServlet() {}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse response) throws IOException
	{
		final int index = req.getRequestURL().indexOf(".well-known/");
		final String filepath = "/" + req.getRequestURL().substring(index + 1);
		InputStream inputStream = WellKnownServlet.class.getResourceAsStream(filepath);
		if (inputStream != null)
		{
			// TODO Be more intelligent here
			response.setContentType("application/json");
			response.setCharacterEncoding("utf-8");
			IOUtils.copy(inputStream, response.getOutputStream());
		}
		else
		{
			new HTTPException(404, "File not found").writeTo(response);
		}
	}
}
