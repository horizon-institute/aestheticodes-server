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
import uk.ac.horizon.aestheticodes.model.Experience;
import uk.ac.horizon.aestheticodes.model.ExperienceEntry;
import uk.ac.horizon.aestheticodes.model.UserExperiences;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

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

			Collection<String> list = new HashSet<>();
			List<ExperienceEntry> wrappers = DataStore.load().type(ExperienceEntry.class)
					.filter("authorID ==", user.getUserId())
					.list();

			for(ExperienceEntry wrapper: wrappers)
			{
				list.add(wrapper.getPublicID());
			}

			if(true) // Disable eventually
			{
				List<UserExperiences> userExperiences = DataStore.load().type(UserExperiences.class).filter("userID ==", user.getUserId()).list();
				for(UserExperiences userEx: userExperiences)
				{
					for(Experience experience: userEx)
					{
						ExperienceEntry wrapper = DataStore.load().type(ExperienceEntry.class).id(experience.getId()).now();
						if(wrapper == null)
						{
							ExperienceItems items = ExperienceItems.create(experience);
							items.save();
							wrapper = items.getEntry();
						}

						if(user.getUserId().equals(wrapper.getAuthorID()))
						{
							list.add(wrapper.getPublicID());
						}
					}
				}
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
