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
