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
import com.google.appengine.api.users.User;
import com.google.gson.Gson;
import com.googlecode.objectify.VoidWork;
import uk.ac.horizon.aestheticodes.model.*;
import uk.ac.horizon.artcodes.server.utils.ArtcodeServlet;
import uk.ac.horizon.artcodes.server.utils.DataStore;
import uk.ac.horizon.artcodes.server.utils.ExperienceItems;
import uk.ac.horizon.artcodes.server.utils.HTTPException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class ExperienceServlet extends ArtcodeServlet
{
	private static final Logger logger = Logger.getLogger(ExperienceServlet.class.getSimpleName());
	private final Mustache mustache;

	public ExperienceServlet()
	{
		final MustacheFactory mustacheFactory = new DefaultMustacheFactory();
		mustache = mustacheFactory.compile("experience.mustache");
	}

	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		try
		{
			User user = getUser(request);
			verifyUser(user);
			final String experienceID = getExperienceID(request);
			ExperienceEntry entry = DataStore.load().type(ExperienceEntry.class).id(experienceID).now();
			verifyUserCanEdit(entry, user);

			final List<ExperienceAvailability> existingAvails = DataStore.load()
					.type(ExperienceAvailability.class)
					.filter("uri", entry.getPublicID())
					.list();

			final List<ExperienceCache> toDelete = new ArrayList<>();
			final List<ExperienceCache> caches = DataStore.load().type(ExperienceCache.class).list();
			for (ExperienceCache cache : caches)
			{
				if (cache.getExperiences().contains(entry.getPublicID()))
				{
					toDelete.add(cache);
				}
			}
			final ExperienceInteraction interaction = DataStore.load().type(ExperienceInteraction.class).id(entry.getPublicID()).now();

			final ExperienceDeleted deleted = new ExperienceDeleted(entry);

			SearchServlet.getIndex().delete(entry.getPublicID());

			DataStore.get().transact(new VoidWork()
			{
				@Override
				public void vrun()
				{
					if (interaction != null)
					{
						DataStore.get().delete().entity(interaction);
					}
					DataStore.get().delete().type(ExperienceEntry.class).id(experienceID);
					DataStore.get().delete().entities(existingAvails);
					DataStore.get().delete().entities(toDelete);
					DataStore.get().save().entity(deleted);
				}
			});
		}
		catch (HTTPException e)
		{
			e.writeTo(response);
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		try
		{
			getUser(request);
			String experienceID = getExperienceID(request);

			logger.info(experienceID);

			final ExperienceEntry entry = DataStore.load().type(ExperienceEntry.class).id(experienceID).now();
			if (entry != null)
			{
				final String userAgent = request.getHeader("User-Agent").toLowerCase();
				if (userAgent.startsWith("artcodes/") || "json".equals(request.getParameter("format")))
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
				else
				{
					final ExperienceDetails experience = new Gson().fromJson(entry.getJson(), ExperienceDetails.class);
					final Map<String, String> variables = new HashMap<>();
					variables.put("title", experience.getName());
					variables.put("description", experience.getDescription());
					variables.put("author", experience.getAuthor());
					variables.put("image", experience.getImage());
					variables.put("icon", experience.getIcon());

					response.setCharacterEncoding("UTF-8");
					response.setContentType("text/html");
					writeExperienceCacheHeaders(response, entry);
					mustache.execute(response.getWriter(), variables).flush();
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
			final User user = getUser(request);
			verifyUser(user);
			final String experienceID = UUID.randomUUID().toString();
			final ExperienceItems items = ExperienceItems.create(experienceID, request.getReader());
			items.getEntry().setAuthorID(user.getUserId());
			items.save();

			logger.info("Created experience " + items.getEntry().getPublicID());

			writeExperience(response, items.getEntry());
		}
		catch (HTTPException e)
		{
			e.writeTo(response);
		}
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		try
		{
			final User user = getUser(request);
			verifyUser(user);
			final String experienceID = getExperienceID(request);

			final ExperienceEntry existing = DataStore.load().type(ExperienceEntry.class).id(experienceID).now();
			verifyUserCanEdit(existing, user);

			final ExperienceItems items = ExperienceItems.create(experienceID, request.getReader());
			items.getEntry().setId(experienceID);
			items.getEntry().setCreated(existing.getCreated());
			if (existing.getAuthorID() != null)
			{
				items.getEntry().setAuthorID(existing.getAuthorID());
			}
			else
			{
				items.getEntry().setAuthorID(user.getUserId());
			}

			items.save();
			writeExperience(response, items.getEntry());
		}
		catch (HTTPException e)
		{
			e.writeTo(response);
		}
	}

	private boolean canEdit(ExperienceEntry wrapper, User user)
	{
		return user != null && (isAdmin(user) || user.getUserId().equals(wrapper.getAuthorID()));
	}

	private void verifyUserCanEdit(ExperienceEntry wrapper, User user) throws HTTPException
	{
		if (wrapper == null)
		{
			throw new HTTPException(HttpServletResponse.SC_NOT_FOUND, "Not found");
		}

		if (!canEdit(wrapper, user))
		{
			throw new HTTPException(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
		}
	}
}
