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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.horizon.aestheticodes.model.ExperienceDetails;
import uk.ac.horizon.aestheticodes.model.ExperienceEntry;

public class ExperiencePageServlet extends ArtcodeServlet
{
	private final Mustache mustache;

	public ExperiencePageServlet()
	{
		final MustacheFactory mustacheFactory = new DefaultMustacheFactory();
		mustache = mustacheFactory.compile("experience.mustache");
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		final String experienceID = getExperienceID(req);
		final Map<String, String> variables = new HashMap<>();
		final ExperienceEntry entry = DataStore.load().type(ExperienceEntry.class).id(experienceID).now();
		if (entry != null)
		{
			final ExperienceDetails experience = new Gson().fromJson(entry.getJson(), ExperienceDetails.class);
			variables.put("id", entry.getId());
			variables.put("title", experience.getName());
			variables.put("description", experience.getDescription());
			variables.put("author", experience.getAuthor());
			variables.put("image", experience.getImage());
			variables.put("icon", experience.getIcon());

			resp.setContentType("text/html");
			writeExperienceCacheHeaders(resp, entry);
			mustache.execute(resp.getWriter(), variables).flush();
		}
		else
		{
			// TODO Write not found exception
		}
	}
}
