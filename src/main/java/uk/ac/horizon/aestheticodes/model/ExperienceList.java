package uk.ac.horizon.aestheticodes.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExperienceList
{
	private List<Experience> experiences = new ArrayList<Experience>();

	public List<Experience> getExperiences()
	{
		return experiences;
	}

	public void setExperiences(List<Experience> experiences)
	{
		this.experiences = experiences;
	}

	public boolean hasExperience(String id)
	{
		for(Experience experience: experiences)
		{
			if(id.equals(experience.getId()))
			{
				return true;
			}
		}
		return false;
	}
}
