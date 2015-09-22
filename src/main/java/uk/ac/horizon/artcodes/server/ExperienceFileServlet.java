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

import com.google.gson.Gson;
import uk.ac.horizon.aestheticodes.model.Experience;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public class ExperienceFileServlet extends HttpServlet
{
	private static final Logger logger = Logger.getLogger(ExperienceFileServlet.class.getName());

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		final String url = req.getRequestURL().toString();
		String experienceID = url.substring(url.lastIndexOf("/") + 1);
		if(experienceID.endsWith(".artcode"))
		{
			experienceID = experienceID.substring(0, experienceID.indexOf(".artcode"));
		}

		logger.info(url);
		logger.info(experienceID);
		final Experience experience = DataStore.load().type(Experience.class).id(experienceID).now();
		if (experience == null)
		{
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
		else
		{
			resp.setContentType("application/x-artcode");

			Gson gson = ExperienceParser.createParser();
			gson.toJson(experience, resp.getWriter());
		}
	}
}
