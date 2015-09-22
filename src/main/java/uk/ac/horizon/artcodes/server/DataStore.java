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

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cmd.Loader;
import com.googlecode.objectify.cmd.Saver;
import uk.ac.horizon.aestheticodes.model.Experience;
import uk.ac.horizon.aestheticodes.model.ExperienceAvailability;
import uk.ac.horizon.aestheticodes.model.ExperienceEntry;
import uk.ac.horizon.aestheticodes.model.ExperienceInteraction;
import uk.ac.horizon.aestheticodes.model.UserExperiences;

public class DataStore
{
	static
	{
		factory().register(Experience.class);
		factory().register(ExperienceEntry.class);
		factory().register(ExperienceAvailability.class);
		factory().register(ExperienceInteraction.class);
		factory().register(UserExperiences.class);
	}

	public static Loader load() { return get().load(); }

	public static Saver save() { return get().save(); }

	public static Objectify get()
	{
		return ObjectifyService.ofy();
	}

	public static ObjectifyFactory factory()
	{
		return ObjectifyService.factory();
	}
}