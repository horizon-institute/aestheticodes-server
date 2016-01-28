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
import com.google.gson.GsonBuilder;
import com.googlecode.objectify.VoidWork;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.horizon.aestheticodes.model.ExperienceEntry;

public class ExperiencesServlet extends ArtcodeServlet
{
	//private static final Logger logger = Logger.getLogger(ExperiencesServlet.class.getName());

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		try
		{
			User user = getUser();
			verifyUser(user);

			final Collection<String> list = new HashSet<>();
			final List<ExperienceEntry> entries = DataStore.load().type(ExperienceEntry.class)
					.filter("authorID ==", user.getUserId())
					.list();

			for (final ExperienceEntry entry : entries)
			{
				list.add(entry.getPublicID());
			}

			final List<ExperienceEntry> oldEntries = DataStore.load().type(ExperienceEntry.class)
					.filter("authorID ==", user.getEmail())
					.list();

			for (final ExperienceEntry entry : oldEntries)
			{
				entry.setAuthorID(user.getUserId());
				DataStore.get().transact(new VoidWork()
				{
					@Override
					public void vrun()
					{
						DataStore.save().entity(entry);
					}
				});
				list.add(entry.getPublicID());
			}

			Gson gson = new GsonBuilder().create();
			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			resp.addHeader("Cache-Control", "max-age=60, stale-while-revalidate=604800");
			resp.getWriter().write(gson.toJson(list));
		}
		catch (HTTPException e)
		{
			e.writeTo(req, resp);
		}
	}
}
