/*
 * Aestheticodes recognises a different marker scheme that allows the
 * creation of aesthetically pleasing, even beautiful, codes.
 * Copyright (C) 2015  Aestheticodes
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

package uk.ac.horizon.aestheticodes.server;

import com.google.appengine.repackaged.org.codehaus.jackson.annotate.JsonIgnore;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import uk.ac.horizon.aestheticodes.model.Marker;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ExperienceParser
{
	public static class MarkerMapAdapter implements JsonDeserializer<Map<String, Marker>>, JsonSerializer<Map<String, Marker>>
	{
		@Override
		public Map<String, Marker> deserialize(JsonElement json, Type unused, JsonDeserializationContext context)
				throws JsonParseException
		{
			if (!json.isJsonArray())
			{
				throw new JsonParseException("Unexpected type: " + json.getClass().getSimpleName());
			}

			Map<String, Marker> result = new HashMap<String, Marker>();
			JsonArray array = json.getAsJsonArray();
			for (JsonElement element : array)
			{
				if (element.isJsonObject())
				{
					Marker marker = context.deserialize(element, Marker.class);
					result.put(marker.getCode(), marker);
				}
				else
				{
					throw new JsonParseException("some meaningful message");
				}
			}
			return result;
		}

		@Override
		public JsonElement serialize(Map<String, Marker> src, Type typeOfSrc, JsonSerializationContext context)
		{
			final JsonArray array = new JsonArray();
			for (Marker marker : src.values())
			{
				array.add(context.serialize(marker));
			}
			return array;
		}
	}

	public static Gson createParser()
	{
		GsonBuilder build = new GsonBuilder();
		build.registerTypeAdapter(new TypeToken<Map<String, Marker>>()
		{}.getType(), new MarkerMapAdapter());
		build.addSerializationExclusionStrategy(new ExclusionStrategy() {
			@Override
			public boolean shouldSkipField(FieldAttributes f)
			{
				final JsonIgnore ignore = f.getAnnotation(JsonIgnore.class);
				return ignore != null;
			}

			@Override
			public boolean shouldSkipClass(Class<?> clazz)
			{
				return false;
			}
		});
		return build.create();
	}
}
