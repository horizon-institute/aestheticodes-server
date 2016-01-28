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

import com.googlecode.objectify.VoidWork;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.horizon.aestheticodes.model.Experience;
import uk.ac.horizon.aestheticodes.model.ExperienceEntry;
import uk.ac.horizon.aestheticodes.model.UserExperiences;

public class ConvertServlet extends ArtcodeServlet
{
	//private static final Logger logger = Logger.getLogger(ExperiencesServlet.class.getName());

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		try
		{
			List<UserExperiences> userExperiences = DataStore.load().type(UserExperiences.class).list();
			for (UserExperiences userEx : userExperiences)
			{
				for (Experience experience : userEx)
				{
					final ExperienceEntry entry = DataStore.load().type(ExperienceEntry.class).id(experience.getId()).now();
					if (entry == null)
					{
						final ExperienceItems items = ExperienceItems.create(experience);
						items.save();
					}
					else if(entry.getAuthorID() == null && experience.getOwner() != null)
					{
						entry.setAuthorID(experience.getOwner().getName());
						DataStore.get().transact(new VoidWork()
						{
							@Override
							public void vrun()
							{
								DataStore.save().entity(entry);
							}
						});
					}
				}
			}

			resp.setContentType("application/json");
			resp.setCharacterEncoding("UTF-8");
			resp.getWriter().write("finished");
		}
		catch (HTTPException e)
		{
			e.writeTo(req, resp);
		}
	}
}
