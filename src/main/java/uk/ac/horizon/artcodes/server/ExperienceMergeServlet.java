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

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import uk.ac.horizon.aestheticodes.model.ExperienceEntry;
import uk.ac.horizon.artcodes.server.utils.ArtcodeServlet;
import uk.ac.horizon.artcodes.server.utils.DataStore;
import uk.ac.horizon.artcodes.server.utils.HTTPException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.logging.Logger;

public class ExperienceMergeServlet extends ArtcodeServlet
{
	private static final Logger logger = Logger.getLogger(ExperienceMergeServlet.class.getSimpleName());
	private static final String[] originExperienceIDs = {"54b01145-75ea-4580-9eaa-09b732edde43", "67e7e177-ef51-4b7b-9edd-c851436e0f0c", "ffc4dff4-606c-47d4-a49f-67822423d52d", "ca91204b-6ad7-4fc3-851c-167958189ba4"};
	private static final String mergedExperienceID = "1ea88879-d5dc-40d0-93d0-78ebb504fcaa";

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		try
		{
			final ExperienceEntry mergedExperience = DataStore.load().type(ExperienceEntry.class).id(mergedExperienceID).now();
			if (mergedExperience != null)
			{
				Gson gson = new Gson();
				final Map<String, ExperienceEntry> experiences = DataStore.get().cache(false).load().type(ExperienceEntry.class).ids(originExperienceIDs);
				final JsonArray array = new JsonArray();
				for (String id : experiences.keySet())
				{
					ExperienceEntry experience = experiences.get(id);
					if (experience != null)
					{
						logger.info("Experience " + id + " not found");
						final JsonObject json = gson.fromJson(experience.getJson(), JsonObject.class);
						if (json.get("actions").isJsonArray())
						{
							array.addAll(json.getAsJsonArray("actions"));
						}
					}
				}

				JsonObject mergedJson = gson.fromJson(mergedExperience.getJson(), JsonObject.class);
				mergedJson.add("actions", array);
				final String finalJson = gson.toJson(mergedJson);
				logger.info(finalJson);
				mergedExperience.setJson(finalJson);
				mergedExperience.setEtag(Hashing.md5().hashString(finalJson, Charset.forName("UTF-8")).toString());

				DataStore.get().transact(() ->
				{
					DataStore.save().entity(mergedExperience);
				});
				writeExperience(response, mergedExperience);
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
