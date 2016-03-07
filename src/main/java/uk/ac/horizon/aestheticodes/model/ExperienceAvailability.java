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

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.logging.Logger;

@Entity
public class ExperienceAvailability
{
	@Id
	Long id;

	@Index
	private Double lat;

	@Index
	private Double lon;

	@Index
	private long start = Long.MIN_VALUE;

	@Index
	private long end = Long.MAX_VALUE;

	@Index
	private String uri;

	public ExperienceAvailability()
	{

	}

	public long getEnd()
	{
		return end;
	}

	public Double getLat()
	{
		return lat;
	}

	public Double getLon()
	{
		return lon;
	}

	public String getUri()
	{
		return uri;
	}

	public void setEnd(long end)
	{
		this.end = end;
	}

	public long getStart()
	{
		return start;
	}

	public void setStart(long start)
	{
		this.start = start;
	}

	public void setLat(double lat)
	{
		this.lat = lat;
	}

	public void setLon(double lon)
	{
		this.lon = lon;
	}

	public boolean isActive(long now)
	{
		return start <= now && end >= now;
	}

	// in miles
	private static double distance(double lat1, double lon1, double lat2, double lon2)
	{
		double theta = lon1 - lon2;
		double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
		return dist;
	}

	private static double deg2rad(double deg)
	{
		return (deg * Math.PI / 180.0);
	}

	private static double rad2deg(double rad)
	{
		return (rad * 180 / Math.PI);
	}

	public double getMilesFrom(double lat, double lon)
	{
		return distance(this.lat, this.lon, lat, lon);
	}

	public void setUri(String uri)
	{
		this.uri = uri;
	}
}
