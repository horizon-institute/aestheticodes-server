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

package uk.ac.horizon.aestheticodes.model;

import java.util.ArrayList;
import java.util.List;

public class ExperienceList
{
	private List<Experience> experiences = new ArrayList<Experience>();
	private String version;
	private double lat;
	private double lon;

	public List<Experience> getExperiences()
	{
		return experiences;
	}

	public String getVersion()
	{
		return version;
	}

	public void setExperiences(List<Experience> experiences)
	{
		this.experiences = experiences;
	}

	public boolean hasExperience(String id)
	{
		for (Experience experience : experiences)
		{
			if (id.equals(experience.getId()))
			{
				return true;
			}
		}
		return false;
	}

	public double getLat()
	{
		return lat;
	}

	public double getLon()
	{
		return lon;
	}

	public void setVersion(String version)
	{
		this.version = version;
	}
}
