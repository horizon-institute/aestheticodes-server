package uk.ac.horizon.aestheticodes.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class ExperienceCache
{
	@Id
	private String id;
	private Date timestamp;
	private List<String> experiences = new ArrayList<>();

	public ExperienceCache()
	{

	}

	public ExperienceCache(String id, List<String> experiences)
	{
		this.id = id;
		this.timestamp = new Date();
		this.experiences.addAll(experiences);
	}

	public Date getTimestamp()
	{
		return timestamp;
	}

	public List<String> getExperiences()
	{
		return experiences;
	}
}
