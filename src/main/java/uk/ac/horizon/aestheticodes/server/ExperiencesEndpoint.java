/*
 * Aestheticodes recognises a different marker scheme that allows the
 * creation of aesthetically pleasing, even beautiful, codes.
 * Copyright (C) 2014  Aestheticodes
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

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.users.User;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.VoidWork;
import uk.ac.horizon.aestheticodes.model.Experience;
import uk.ac.horizon.aestheticodes.model.ExperienceList;
import uk.ac.horizon.aestheticodes.model.ExperienceResults;
import uk.ac.horizon.aestheticodes.model.UserExperiences;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Api(
		name = "experiences",
		version = "v1",
		resource = "experiences",
		scopes = {EndpointConstants.EMAIL_SCOPE},
		clientIds = {EndpointConstants.WEB_CLIENT_ID,
		             EndpointConstants.ANDROID_CLIENT_ID,
		             EndpointConstants.IOS_CLIENT_ID,
		             EndpointConstants.API_EXPLORER_ID},
		audiences = {EndpointConstants.ANDROID_AUDIENCE},
		namespace = @ApiNamespace(ownerDomain = "server.aestheticodes.horizon.ac.uk", ownerName = "server.aestheticodes.horizon.ac.uk", packagePath = "")
)
public class ExperiencesEndpoint
{
	private static final Logger logger = Logger.getLogger(ExperiencesEndpoint.class.getName());
	private static final String defaultExperiences = "-default-";

	@ApiMethod(
			name = "getRecommended",
			path = "getRecommended",
			httpMethod = ApiMethod.HttpMethod.GET
	)
	public List<Experience> getRecommended(@Named("lat") double lat, @Named("lon") double lon)
	{
		return null;
	}

	@ApiMethod(
			name = "addDefault",
			path = "addDefault",
			httpMethod = ApiMethod.HttpMethod.PUT
	)
	public void addDefault(@Named("experienceID") String experienceID, User user) throws UnauthorizedException, NotFoundException
	{
		if (user == null)
		{
			throw new UnauthorizedException("Authorization Required");
		}

		UserExperiences userExperiences = getExperiences(user);
		if (!userExperiences.isAdmin())
		{
			throw new UnauthorizedException("Admin users only");
		}

		final Experience newExperience = DataStore.load().type(Experience.class).id(experienceID).now();
		if (newExperience == null)
		{
			throw new NotFoundException("Experience " + experienceID + " not found");
		}
		else
		{
			final List<UserExperiences> experiencesList = DataStore.load().type(UserExperiences.class).list();
			for (final UserExperiences experiences : experiencesList)
			{
				if (!experiences.hasExperience(experienceID))
				{
					logger.warning("Updating " + experiences.getUserName());
					DataStore.get().transact(new VoidWork()
					{
						@Override
						public void vrun()
						{
							experiences.add(newExperience);
							DataStore.save().entity(experiences);
						}
					});
				}
				else
				{
					logger.warning("User " + experiences.getUserName() + " already has experience");
				}
			}
		}
	}

	@ApiMethod(
			name = "removeDefault",
			path = "removeDefault",
			httpMethod = ApiMethod.HttpMethod.PUT
	)
	public void removeDefault(@Named("experienceID") final String experienceID, User user) throws UnauthorizedException, NotFoundException
	{
		if (user == null)
		{
			throw new UnauthorizedException("Authorization Required");
		}

		UserExperiences userExperiences = getExperiences(user);
		if (!userExperiences.isAdmin())
		{
			throw new UnauthorizedException("Admin users only");
		}

		final Experience newExperience = DataStore.load().type(Experience.class).id(experienceID).now();
		if (newExperience == null)
		{
			throw new NotFoundException("Experience " + experienceID + " not found");
		}
		else
		{
			final List<UserExperiences> experiencesList = DataStore.load().type(UserExperiences.class).list();
			for (final UserExperiences experiences : experiencesList)
			{
				if (experiences.hasExperience(experienceID))
				{
					logger.warning("Updating " + experiences.getUserName());
					DataStore.get().transact(new VoidWork()
					{
						@Override
						public void vrun()
						{
							experiences.remove(experienceID);
							DataStore.save().entity(experiences);
						}
					});
				}
			}
		}
	}

	@ApiMethod(
			name = "update",
			path = "experiences",
			httpMethod = ApiMethod.HttpMethod.PUT)
	public ExperienceResults update(ExperienceList experiences, final User user)
	{
		final UserExperiences userExperiences = getExperiences(user);
		final ExperienceResults results = new ExperienceResults();
		final List<Experience> toSave = new ArrayList<>();

		if (experiences != null)
		{
			logger.info("Updating using version " + experiences.getVersion());
			for (Experience experience : experiences.getExperiences())
			{
				logger.info(experience.getOp() + " " + experience.getId());
				if (experience.getOp() == null || experience.getOp() == Experience.Operation.retrieve)
				{
					Experience existing = userExperiences.getExperience(experience.getId());
					if (existing == null)
					{
						existing = DataStore.load().type(Experience.class).id(experience.getId()).now();
						if (existing != null)
						{
							userExperiences.add(existing);
						}
					}

					if (existing == null)
					{
						if(user != null)
						{
							Experience removal = new Experience();
							removal.setOp(Experience.Operation.remove);
							results.getExperiences().put(experience.getId(), removal);
						}
					}
					else if (experience.getVersion() == null || experience.getVersion() < existing.getVersion())
					{
						results.getExperiences().put(experience.getId(), existing);
					}
				}
				else if (experience.getOp() == Experience.Operation.add)
				{
					Experience existing = null;
					if (experience.getId() != null)
					{
						existing = DataStore.load().type(Experience.class).id(experience.getId()).now();
					}

					if (existing != null)
					{
						userExperiences.add(existing);
						if (experience.getVersion() == null || experience.getVersion() < existing.getVersion())
						{
							results.getExperiences().put(experience.getId(), existing);
						}
					}
				}
				else if (user != null)
				{
					if (experience.getOp() == Experience.Operation.create)
					{
						if (experience.getId() != null)
						{
							results.getExperiences().put(experience.getId(), experience);
							experience.setId(UUID.randomUUID().toString());
						}
						else
						{
							experience.setId(UUID.randomUUID().toString());
							results.getExperiences().put(experience.getId(), experience);
						}

						toSave.add(experience);

						userExperiences.add(experience);
					}
					else if (experience.getOp() == Experience.Operation.update)
					{
						Experience existing = DataStore.load().type(Experience.class).id(experience.getId()).now();
						if (existing != null)
						{
							Key<UserExperiences> experiencesKey = Key.create(userExperiences);
							if (existing.getOwner() == null || experiencesKey.equals(existing.getOwner()))
							{
								toSave.add(experience);

								results.getExperiences().put(experience.getId(), experience);
								userExperiences.add(experience);
							}
							else
							{
								experience.setOriginalID(existing.getId());
								experience.setId(UUID.randomUUID().toString());
								if (experience.getName() == null || experience.getName().equals(existing.getName()))
								{
									experience.setName("Copy of " + existing.getName());
								}
								toSave.add(experience);

								results.getExperiences().put(experience.getId(), experience);
								userExperiences.add(experience);
							}
						}
						// TODO else do something???
					}
					else if (experience.getOp() == Experience.Operation.remove)
					{
						userExperiences.remove(experience.getId());

						Experience removal = new Experience();
						removal.setOp(Experience.Operation.remove);
						results.getExperiences().put(experience.getId(), removal);
					}
				}
			}
		}

		if (user != null)
		{
			for (Experience experience : toSave)
			{
				experience.setOp(null);
				if (experience.getVersion() == null)
				{
					experience.setVersion(1);
				}
				else
				{
					experience.setVersion(experience.getVersion() + 1);
				}
				experience.setOwner(userExperiences);
			}

			DataStore.get().transact(new VoidWork()
			{
				@Override
				public void vrun()
				{
					DataStore.save().entities(toSave);
					DataStore.save().entity(userExperiences);
				}
			});
		}

		for (Experience experience : userExperiences)
		{
			if ((experiences == null || !experiences.hasExperience(experience.getId())) && !results.getExperiences().containsKey(experience.getId()))
			{
				results.getExperiences().put(experience.getId(), experience);
			}
		}


		return results;
	}

	private UserExperiences getExperiences(final User user)
	{
		if (user != null)
		{
			if (user.getUserId() != null)
			{
				List<UserExperiences> experienceList = DataStore.load().type(UserExperiences.class).filter("userID", user.getUserId()).list();
				if (experienceList != null && experienceList.size() == 1)
				{
					return experienceList.get(0);
				}
			}

			UserExperiences experiences = DataStore.load().type(UserExperiences.class).id(user.getEmail()).now();
			if (experiences != null)
			{
				if (user.getUserId() != null)
				{
					experiences.setUserID(user.getUserId());
				}

				return experiences;
			}
		}

		UserExperiences experiences = DataStore.load().type(UserExperiences.class).id(defaultExperiences).now();
		if (experiences == null)
		{
			experiences = new UserExperiences();
			experiences.setUserName(defaultExperiences);
		}

		if (user != null)
		{
			experiences.setUserName(user.getEmail());
			if (user.getUserId() != null)
			{
				experiences.setUserID(user.getUserId());
			}
		}

		return experiences;

	}
}