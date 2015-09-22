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

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.condition.IfNotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Entity
public class UserExperiences implements Iterable<Experience>
{
	@Id
	private String userName;

	@Index(IfNotNull.class)
	private String userID;

	private boolean admin = false;

	@Load
	private List<Ref<Experience>> experiences = new ArrayList<>();

	public String getUserName()
	{
		return userName;
	}

	public boolean isAdmin()
	{
		return admin;
	}

	public void setAdmin(boolean admin)
	{
		this.admin = admin;
	}

	public boolean hasExperience(String id)
	{
		return getExperience(id) != null;
	}

	public Experience getExperience(String id)
	{
		for(Ref<Experience> experienceKey: experiences)
		{
			Experience experience = experienceKey.get();
			if(id.equals(experience.getId()))
			{
				return experience;
			}
		}
		return null;
	}

	public void clear()
	{
		experiences.clear();
	}

	public void remove(String id)
	{
		if(id == null)
		{
			return;
		}
		for(Ref<Experience> experienceKey: experiences)
		{
			Experience experience = experienceKey.get();
			if(experience != null)
			{
				if (id.equals(experience.getId()))
				{
					experiences.remove(experienceKey);
					return;
				}
			}
		}
	}

	public void add(Experience experience)
	{
		if(!hasExperience(experience.getId()))
		{
			experiences.add(Ref.create(experience));
		}
	}

	public String getUserID()
	{
		return userID;
	}

	public void setUserID(String userID)
	{
		this.userID = userID;
	}

	@Override
	public Iterator<Experience> iterator()
	{
		final Iterator<Ref<Experience>> iterator = experiences.iterator();
		return new Iterator<Experience>() {


			@Override
			public boolean hasNext()
			{
				return iterator.hasNext();
			}

			@Override
			public Experience next()
			{
				return iterator.next().get();
			}
		};
	}

	public void setUserName(String userName)
	{
		this.userName = userName;
	}
}
