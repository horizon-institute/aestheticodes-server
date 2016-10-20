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

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.horizon.aestheticodes.model.ExperienceEntry;
import uk.ac.horizon.artcodes.server.utils.ArtcodeServlet;
import uk.ac.horizon.artcodes.server.utils.DataStore;
import uk.ac.horizon.artcodes.server.utils.ExperienceItems;
import uk.ac.horizon.artcodes.server.utils.HTTPException;

public class ExperienceServlet extends ArtcodeServlet
{
	private static final Logger logger = Logger.getLogger(ExperienceServlet.class.getSimpleName());

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
			// TODO items.getEntry().setAuthorID(user.getUserId());
			items.save();

			writeExperience(response, items.getEntry());
		}
		catch (HTTPException e)
		{
			e.writeTo(response);
		}
	}
}
