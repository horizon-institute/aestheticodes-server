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

import com.google.appengine.api.users.User;
import com.google.gson.Gson;
import com.googlecode.objectify.VoidWork;

import uk.ac.horizon.aestheticodes.model.Experience;
import uk.ac.horizon.aestheticodes.model.ExperienceAvailability;
import uk.ac.horizon.aestheticodes.model.ExperienceDeleted;
import uk.ac.horizon.aestheticodes.model.ExperienceEntry;
import uk.ac.horizon.aestheticodes.model.ExperienceInteraction;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExperienceServlet extends ArtcodeServlet
{
	private static final Logger logger = Logger.getLogger(ExperienceServlet.class.getName());
	private final Gson gson = ExperienceParser.createParser();

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		try
		{
			User user = getUser();
			verifyUser(user);
			final String experienceID = getExperienceID(req);
			ExperienceEntry entry = DataStore.load().type(ExperienceEntry.class).id(experienceID).now();
			verifyCanEdit(entry, user);


			final List<ExperienceAvailability> existingAvails = DataStore.load()
					.type(ExperienceAvailability.class)
					.filter("uri", entry.getPublicID())
					.list();

			final ExperienceInteraction interaction = DataStore.load().type(ExperienceInteraction.class).id(entry.getPublicID()).now();

			final ExperienceDeleted deleted = new ExperienceDeleted(entry);

			DataStore.get().transact(new VoidWork()
			{
				@Override
				public void vrun()
				{
					if(interaction != null)
					{
						DataStore.get().delete().entity(interaction);
					}
					DataStore.get().delete().type(ExperienceEntry.class).id(experienceID);
					DataStore.get().delete().entities(existingAvails);
					DataStore.get().save().entity(deleted);
				}
			});
		}
		catch (HTTPException e)
		{
			e.writeTo(req, resp);
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		try
		{
			User user = getUser();
			String experienceID = getExperienceID(req);

			logger.info(experienceID);

			final ExperienceEntry experienceWrapper = DataStore.load().type(ExperienceEntry.class).id(experienceID).now();
			if (experienceWrapper != null)
			{
				if (req.getHeader("If-None-Match") != null && req.getHeader("If-None-Match").equals(experienceWrapper.getEtag()))
				{
					throw new HTTPException(HttpServletResponse.SC_NOT_MODIFIED, "No Change");
				}
				else
				{
					writeExperience(user, experienceWrapper, resp);
				}
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
					try
					{
						final ExperienceItems items = ExperienceItems.create(experience);
						items.save();
						writeExperience(user, items.getEntry(), resp);
					}
					catch (Exception e)
					{
						logger.log(Level.SEVERE, e.getMessage(), e);
					}
				}
			}
		}
		catch (HTTPException e)
		{
			e.writeTo(req, resp);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		try
		{
			final User user = getUser();
			verifyUser(user);
			final String experienceID = UUID.randomUUID().toString();
			final ExperienceItems items = ExperienceItems.create(experienceID, req.getReader());
			items.getEntry().setAuthorID(user.getUserId());
			items.save();

			writeExperience(user, items.getEntry(), resp);
		}
		catch (HTTPException e)
		{
			e.writeTo(req, resp);
		}
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
	{
		try
		{
			final User user = getUser();
			verifyUser(user);
			final String experienceID = getExperienceID(req);

			ExperienceEntry existing = DataStore.load().type(ExperienceEntry.class).id(experienceID).now();
			verifyCanEdit(existing, user);

			final ExperienceItems items = ExperienceItems.create(experienceID, req.getReader());
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
			writeExperience(user, items.getEntry(), resp);
		}
		catch (HTTPException e)
		{
			e.writeTo(req, resp);
		}
	}

	private void writeExperience(User user, ExperienceEntry wrapper, HttpServletResponse resp) throws IOException
	{
		resp.setContentType("application/x-artcode");
		resp.setCharacterEncoding("UTF-8");
		if(wrapper.getModified() != null)
		{
			resp.setDateHeader("Last-Modified", wrapper.getModified().getTime());
		}
		if (wrapper.getEtag() != null)
		{
			resp.setHeader("Cache-Control", "max-age=300, stale-while-revalidate=604800");
			resp.setHeader("ETag", wrapper.getEtag());
		}

//		JsonElement element = gson.fromJson(wrapper.getJson(), JsonElement.class);
//		if (element.isJsonObject())
//		{
//			JsonObject jsonObject = element.getAsJsonObject();
//			if (canEdit(wrapper, user))
//			{
//				jsonObject.addProperty("editable", true);
//			}
//			else
//			{
//				jsonObject.remove("editable");
//			}
//		}
		resp.getWriter().write(wrapper.getJson());
	}

	private boolean canEdit(ExperienceEntry wrapper, User user)
	{
		return user != null && (EndpointConstants.ADMIN_USER.equals(user.getUserId()) || user.getUserId().equals(wrapper.getAuthorID()));

	}

	private void verifyCanEdit(ExperienceEntry wrapper, User user) throws HTTPException
	{
		if (wrapper == null)
		{
			throw new HTTPException(HttpServletResponse.SC_NOT_FOUND, "Not found");
		}

		if(!canEdit(wrapper, user))
		{
			throw new HTTPException(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
		}
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
