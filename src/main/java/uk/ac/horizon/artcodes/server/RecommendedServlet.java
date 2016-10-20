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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.horizon.aestheticodes.model.ExperienceAvailability;
import uk.ac.horizon.aestheticodes.model.ExperienceInteraction;
import uk.ac.horizon.artcodes.server.utils.ArtcodeServlet;
import uk.ac.horizon.artcodes.server.utils.DataStore;

public class RecommendedServlet extends ArtcodeServlet
{
	// TODO Optimise!

	private class Nearby
	{
		private final String uri;
		private final double distance;

		public Nearby(String uri, double distance)
		{
			this.uri = uri;
			this.distance = distance;
		}
	}

	private class LatLng
	{
		private final double latitude;
		private final double longitude;

		LatLng(double latitude, double longitude)
		{
			this.latitude = latitude;
			this.longitude = longitude;
		}
	}

	private static final int limit = 6;
	private static final long recent = 86400000;
	private static final Logger logger = Logger.getLogger(RecommendedServlet.class.getSimpleName());

	private LatLng getLocation(HttpServletRequest req)
	{
		try
		{
			if (req.getParameter("lat") != null && req.getParameter("lon") != null)
			{
				return new LatLng(Double.parseDouble(req.getParameter("lat")), Double.parseDouble(req.getParameter("lon")));
			}
			else if (req.getHeader("X-AppEngine-CityLatLong") != null)
			{
				String[] positions = req.getHeader("X-AppEngine-CityLatLong").split(",");
				return new LatLng(Double.parseDouble(positions[0]), Double.parseDouble(positions[1]));
			}
		}
		catch (Exception e)
		{
			logger.log(Level.WARNING, "Error reading location", e);
		}
		return null;
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		final long now = System.currentTimeMillis();
		int limit = RecommendedServlet.limit;
		if (req.getParameter("limit") != null)
		{
			try
			{
				limit = Integer.parseInt(req.getParameter("limit"));
			}
			catch (Exception e)
			{
				logger.info(e.getMessage());
			}
		}

		final Map<String, List<String>> result = new HashMap<>();
		final Set<String> ids = new HashSet<>();

		try
		{
			logger.info("Results = " + ids.size());
			if (ids.size() < limit)
			{
				List<ExperienceInteraction> interactions = DataStore.load().type(ExperienceInteraction.class)
						.filter("featured", true)
						.list();

				List<String> feturedIDs = new ArrayList<>();
				for (ExperienceInteraction interaction : interactions)
				{
					if (!ids.contains(interaction.getUri()))
					{
						List<ExperienceAvailability> availabilities = DataStore.load()
								.type(ExperienceAvailability.class)
								.filter("uri", interaction.getUri())
								.list();

						if (availabilities.isEmpty())
						{
							ids.add(interaction.getUri());
							feturedIDs.add(interaction.getUri());
						}
						else
						{
							for (ExperienceAvailability availability : availabilities)
							{
								if (availability.isActive(now))
								{
									ids.add(interaction.getUri());
									feturedIDs.add(interaction.getUri());
									break;
								}
							}
						}
					}
				}

				if (!feturedIDs.isEmpty())
				{
					result.put("featured", feturedIDs);
				}
			}

			final List<Nearby> nearby = new ArrayList<>();
			LatLng location = getLocation(req);
			if (location != null)
			{
				// TODO Optimise with grid?
				final List<ExperienceAvailability> availabilities = DataStore.load().type(ExperienceAvailability.class)
						.filter("lat !=", null)
						.list();

				logger.info("Found " + availabilities.size() + " location results");

				for (ExperienceAvailability availability : availabilities)
				{
					if (availability.isActive(now))
					{
						try
						{
							final double distance = availability.getMilesFrom(location.latitude, location.longitude);
							logger.info(availability.getUri() + " has distance = " + distance);
							if (distance < 100)
							{
								nearby.add(new Nearby(availability.getUri(), distance));
							}
						}
						catch (Exception e)
						{
							logger.log(Level.WARNING, e.getMessage(), e);
						}
					}
				}

				Collections.sort(nearby, new Comparator<Nearby>()
				{
					@Override
					public int compare(Nearby o1, Nearby o2)
					{
						return (int) ((o1.distance - o2.distance) * 10000);
					}
				});

				final List<String> nearbyIDs = new ArrayList<>();
				for (Nearby nearbyID : nearby)
				{
					if (!ids.contains(nearbyID.uri))
					{
						ids.add(nearbyID.uri);
						nearbyIDs.add(nearbyID.uri);
						if (nearbyIDs.size() >= limit)
						{
							break;
						}
					}
				}

				if (!nearbyIDs.isEmpty())
				{
					result.put("nearby", nearbyIDs);
				}
			}
		}
		catch (Exception e)
		{
			logger.log(Level.WARNING, e.getMessage(), e);
		}

		logger.info("Results = " + ids.size());
		List<ExperienceAvailability> availabilities = DataStore.load().type(ExperienceAvailability.class)
				.filter("start <", now)
				.filter("start >", now - recent)
				.order("-start")
				.list();

		List<String> newIDs = new ArrayList<>();
		for (ExperienceAvailability availability : availabilities)
		{
			if (!ids.contains(availability.getUri()))
			{
				if (availability.getLon() == null && availability.getLat() == null &&
						availability.isActive(now))
				{
					newIDs.add(availability.getUri());
					ids.add(availability.getUri());
				}

				if (newIDs.size() >= limit)
				{
					break;
				}
			}
		}

		if (!newIDs.isEmpty())
		{
			result.put("new", newIDs);
		}

		logger.info("Results = " + ids.size());

		List<ExperienceInteraction> interactions = DataStore.load().type(ExperienceInteraction.class)
				.filter("interactions !=", 0)
				.order("-interactions")
				.list();

		List<String> popularIDs = new ArrayList<>();
		for (ExperienceInteraction interaction : interactions)
		{
			if (interaction.getInteractions() > 0 && !ids.contains(interaction.getUri()))
			{
				availabilities = DataStore.load()
						.type(ExperienceAvailability.class)
						.filter("uri", interaction.getUri())
						.list();
				if (availabilities.isEmpty())
				{
					ids.add(interaction.getUri());
					popularIDs.add(interaction.getUri());
				}
				else
				{
					for (ExperienceAvailability availability : availabilities)
					{
						if (availability.isActive(now))
						{
							ids.add(interaction.getUri());
							popularIDs.add(interaction.getUri());
							break;
						}
					}
				}
			}
			if (popularIDs.size() >= limit)
			{
				break;
			}
		}

		if (!popularIDs.isEmpty())
		{
			result.put("popular", popularIDs);
		}

		resp.addHeader("Cache-Control", "max-age=60, stale-while-revalidate=604800");
		writeJSON(resp, result);
	}
}
