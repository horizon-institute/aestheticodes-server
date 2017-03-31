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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.appengine.repackaged.com.google.gson.Gson;
import uk.ac.horizon.aestheticodes.model.ExperienceDetails;
import uk.ac.horizon.aestheticodes.model.ExperienceEntry;
import uk.ac.horizon.artcodes.server.utils.ArtcodeServlet;
import uk.ac.horizon.artcodes.server.utils.DataStore;
import uk.ac.horizon.artcodes.server.utils.ExperienceItems;
import uk.ac.horizon.artcodes.server.utils.HTTPException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class ExperienceServlet extends ArtcodeServlet
{
	private static final Logger logger = Logger.getLogger(ExperienceServlet.class.getSimpleName());
	private final Mustache mustache;

	public ExperienceServlet()
	{
		final MustacheFactory mustacheFactory = new DefaultMustacheFactory();
		mustache = mustacheFactory.compile("christmas.mustache");
	}


	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		try
		{
			String experienceID = getExperienceID(request);

			logger.info(experienceID);

			final ExperienceEntry entry = DataStore.load().type(ExperienceEntry.class).id(experienceID).now();
			if (entry != null)
			{
				final String userAgent = request.getHeader("User-Agent").toLowerCase();
				if (userAgent.startsWith("facebook") || userAgent.startsWith("twitter") || "html".equals(request.getParameter("format")))
				{
					final ExperienceDetails experience = new Gson().fromJson(entry.getJson(), ExperienceDetails.class);
					final Map<String, String> variables = new HashMap<>();
					variables.put("title", "Christmas with Artcodes, from " + experience.getName());
					logger.info(experience.getName());
					variables.put("author", experience.getName());
					variables.put("description", "This is a personalised layer for the Artcodes advent calendar. The advent calendar is a beautifully illustrated, freestanding advent calendar. Traditional in style yet features innovative scannable Artcodes that open digital content.");

					response.setCharacterEncoding("UTF-8");
					response.setContentType("text/html");
					writeExperienceCacheHeaders(response, entry);
					mustache.execute(response.getWriter(), variables).flush();
				}
				else
				{
					if (request.getHeader("If-None-Match") != null && request.getHeader("If-None-Match").equals(entry.getEtag()))
					{
						throw new HTTPException(HttpServletResponse.SC_NOT_MODIFIED, "No Change");
					}
					else
					{
						writeExperience(response, entry);
					}
				}
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

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		try
		{
			verifyApp(request);
			final String experienceID = UUID.randomUUID().toString();
			final ExperienceItems items = ExperienceItems.create(experienceID, request.getReader());
			items.getEntry().setAuthorID("chrimbocodes@gmail.com");
			items.getEntry().setJson(items.getEntry().getJson().replace("http://aestheticodes.appspot.com/experience/", "http://aestheticodes.appspot.com/christmas/"));
			items.save();

			logger.info("Created experience " + items.getEntry().getPublicID());

			writeExperience(response, items.getEntry());
		}
		catch (HTTPException e)
		{
			e.writeTo(response);
		}
	}
}
