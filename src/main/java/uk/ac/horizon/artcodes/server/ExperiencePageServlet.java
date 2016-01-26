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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.appengine.repackaged.com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.horizon.aestheticodes.model.Experience;
import uk.ac.horizon.aestheticodes.model.ExperienceDetails;
import uk.ac.horizon.aestheticodes.model.ExperienceEntry;

public class ExperiencePageServlet extends ArtcodeServlet
{
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		final String experienceID = getExperienceID(req);
		final Map<String, String> variables = new HashMap<>();
		final ExperienceEntry experienceEntry = DataStore.load().type(ExperienceEntry.class).id(experienceID).now();
		if (experienceEntry != null)
		{
			final ExperienceDetails experience = new Gson().fromJson(experienceEntry.getJson(), ExperienceDetails.class);
			variables.put("id", experienceEntry.getId());
			variables.put("title", experience.getName());
			variables.put("description", experience.getDescription());
			variables.put("author", experience.getAuthor());
			variables.put("image", experience.getImage());
			variables.put("icon", experience.getIcon());
		}
		else
		{
			final Experience experience = DataStore.load().type(Experience.class).id(experienceID).now();
			if (experience == null)
			{
				throw new HTTPException(HttpServletResponse.SC_NOT_FOUND, "Not found");
			}
			else
			{
				variables.put("id", experience.getId());
				variables.put("title", experience.getName());
				variables.put("description", experience.getDescription());
				variables.put("author", experience.getOwner().getName());
				variables.put("image", experience.getImage());
				variables.put("icon", experience.getIcon());
			}
		}

		resp.setContentType("text/html");

		final MustacheFactory mustacheFactory = new DefaultMustacheFactory();
		final Mustache mustache = mustacheFactory.compile("experience.mustache");
		mustache.execute(resp.getWriter(), variables).flush();
	}

	private String getExperienceID(HttpServletRequest req)
	{
		String url = req.getRequestURL().toString();
		String experienceID = url.substring(url.lastIndexOf("/") + 1);
		if (experienceID.endsWith(".artcode"))
		{
			experienceID = experienceID.substring(0, experienceID.indexOf(".artcode"));
		}

		return experienceID;
	}
}
