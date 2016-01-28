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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.googlecode.objectify.VoidWork;
import uk.ac.horizon.aestheticodes.model.ExperienceAvailability;
import uk.ac.horizon.aestheticodes.model.ExperienceEntry;
import uk.ac.horizon.aestheticodes.model.ExperienceInteraction;

import javax.servlet.http.HttpServletResponse;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ExperienceItems
{
	public ExperienceEntry getEntry()
	{
		return entry;
	}

	public static ExperienceItems create(String experienceID, Reader experienceReader) throws HTTPException
	{
		Gson gson = ExperienceParser.createParser();
		final JsonElement element = gson.fromJson(experienceReader, JsonElement.class);

		return create(gson, element, experienceID);
	}

	private static JsonObject verifyExperience(JsonElement element) throws HTTPException
	{
		if (!element.isJsonObject())
		{
			throw new HTTPException(HttpServletResponse.SC_BAD_REQUEST, "Expected object");
		}

		JsonObject jsonObject = element.getAsJsonObject();
		jsonObject.remove("editable");

		return jsonObject;
	}

	private static ExperienceItems create(Gson gson, JsonElement element, String experienceID) throws HTTPException
	{
		final ExperienceEntry wrapper = gson.fromJson(element, ExperienceEntry.class);
		wrapper.setCreated(new Date());
		wrapper.setId(experienceID);

		final JsonObject experienceObject = verifyExperience(element);
		final String fullID = wrapper.getPublicID();
		if (experienceObject.has("id"))
		{
			String existing = experienceObject.get("id").getAsString();
			if(!fullID.equals(existing))
			{
				experienceObject.add("originalID", experienceObject.get("id"));
			}
		}
		experienceObject.addProperty("id", fullID);

		final String experienceJson = gson.toJson(experienceObject);
		wrapper.setJson(experienceJson);
		wrapper.setEtag(Hashing.md5().hashString(experienceJson, Charset.forName("UTF-8")).toString());

		final ExperienceItems items = new ExperienceItems();
		items.entry = wrapper;

		JsonArray availabilityArray = experienceObject.getAsJsonArray("availabilities");
		if (availabilityArray != null)
		{
			for (JsonElement availElement : availabilityArray)
			{
				ExperienceAvailability availability = gson.fromJson(availElement, ExperienceAvailability.class);
				availability.setUri(wrapper.getPublicID());
				items.availabilities.add(availability);
			}
		}

		return items;
	}

	public void save()
	{
		final List<ExperienceAvailability> existingAvails = DataStore.load()
				.type(ExperienceAvailability.class)
				.filter("uri", entry.getPublicID())
				.list();

		entry.modified();

		final ExperienceInteraction interaction = DataStore.load().type(ExperienceInteraction.class).id(entry.getPublicID()).now();

		DataStore.get().transact(new VoidWork()
		{
			@Override
			public void vrun()
			{
				DataStore.get().delete().entities(existingAvails);
				DataStore.save().entity(entry);
				if (!availabilities.isEmpty())
				{
					DataStore.save().entities(availabilities);
					if (interaction == null)
					{
						ExperienceInteraction experienceInteraction = new ExperienceInteraction();
						experienceInteraction.setUri(entry.getPublicID());
						DataStore.save().entity(experienceInteraction);
					}
				}
			}
		});
	}

	private ExperienceEntry entry;
	private List<ExperienceAvailability> availabilities = new ArrayList<>();
}
