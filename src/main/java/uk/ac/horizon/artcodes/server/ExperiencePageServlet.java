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
import com.google.gson.Gson;

import uk.ac.horizon.aestheticodes.model.ExperienceDetails;
import uk.ac.horizon.aestheticodes.model.ExperienceEntry;
import uk.ac.horizon.artcodes.server.utils.ArtcodeServlet;
import uk.ac.horizon.artcodes.server.utils.DataStore;
import uk.ac.horizon.artcodes.server.utils.HTTPException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ExperiencePageServlet extends ArtcodeServlet
{
	private final Mustache mustache;

	public ExperiencePageServlet()
	{
		final MustacheFactory mustacheFactory = new DefaultMustacheFactory();
		mustache = mustacheFactory.compile("experience.mustache");
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		try
		{
			final String experienceID = getExperienceID(request);
			final Map<String, String> variables = new HashMap<>();
			final ExperienceEntry entry = DataStore.load().type(ExperienceEntry.class).id(experienceID).now();
			if (entry != null)
			{
				final ExperienceDetails experience = new Gson().fromJson(entry.getJson(), ExperienceDetails.class);
				variables.put("id", entry.getId());
				variables.put("json", entry.getJson());
				variables.put("title", experience.getName());
				variables.put("description", experience.getDescription());
				variables.put("author", experience.getAuthor());
				variables.put("image", experience.getImage());
				variables.put("icon", experience.getIcon());

				response.setContentType("text/html");
				writeExperienceCacheHeaders(response, entry);
				mustache.execute(response.getWriter(), variables).flush();
			}
			else
			{
				throw new HTTPException(HttpServletResponse.SC_NOT_FOUND, "Not found");
			}
		}
		catch (HTTPException e)
		{
			e.writeTo(response);
		}
	}
}
