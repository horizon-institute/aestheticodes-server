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

import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.oauth.OAuthService;
import com.google.appengine.api.oauth.OAuthServiceFactory;
import com.google.appengine.api.users.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.horizon.aestheticodes.model.ExperienceEntry;

abstract class ArtcodeServlet extends HttpServlet
{
	private static final Gson gson = new GsonBuilder().create();
	private static final Logger logger = Logger.getLogger(ArtcodeServlet.class.getSimpleName());
	private static final Set<String> allowedClients = new HashSet<>();
	private static final OAuthService oauth = OAuthServiceFactory.getOAuthService();
	private static final Properties properties = new Properties();

	static
	{
		try
		{
			properties.load(ArtcodeServlet.class.getResourceAsStream("/oauth.properties"));
		}
		catch (Exception e)
		{
			logger.log(Level.WARNING, e.getMessage(), e);
		}

		allowedClients.add(properties.getProperty("web_Client_ID"));
		allowedClients.add(properties.getProperty("android_Client_ID"));
		allowedClients.add(properties.getProperty("ios_Client_ID"));
	}

	static String getIndexName()
	{
		return properties.getProperty("index");
	}

	static void verifyUser(User user) throws HTTPException
	{
		if (user == null)
		{
			throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
		}
	}

	static void writeExperience(HttpServletResponse resp, ExperienceEntry entry) throws IOException
	{
		resp.setContentType("application/x-artcode");
		resp.setCharacterEncoding("UTF-8");
		writeExperienceCacheHeaders(resp, entry);

		resp.getWriter().write(entry.getJson());
	}

	static void writeExperienceCacheHeaders(HttpServletResponse resp, ExperienceEntry entry)
	{
		if (entry.getModified() != null)
		{
			resp.setDateHeader("Last-Modified", entry.getModified().getTime());
		}
		if (entry.getEtag() != null)
		{
			resp.setHeader("Cache-Control", "max-age=300, stale-while-revalidate=604800");
			resp.setHeader("ETag", entry.getEtag());
		}
	}

	static boolean isAdmin(User user)
	{
		return user != null && user.getUserId() != null && user.getUserId().equals(properties.getProperty("admin_user"));
	}

	static void writeJSON(HttpServletResponse resp, Object item) throws IOException
	{
		String json = gson.toJson(item);
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		resp.getWriter().write(json);
	}

	static String getExperienceID(HttpServletRequest req)
	{
		String url = req.getRequestURL().toString();
		return getEntryID(url);
	}

	static String getEntryID(String url)
	{
		String experienceID = url.substring(url.lastIndexOf("/") + 1);
		if (experienceID.endsWith(".artcode"))
		{
			experienceID = experienceID.substring(0, experienceID.indexOf(".artcode"));
		}

		return experienceID;
	}

	static User getUser(HttpServletRequest request)
	{
		try
		{
			final String authHeader = request.getHeader("Authorization");
			if (authHeader != null && authHeader.startsWith("Bearer "))
			{
				String scope = properties.getProperty("scope");
				final User user = oauth.getCurrentUser(scope);
				final String tokenAudience = oauth.getClientId(scope);
				if (!allowedClients.contains(tokenAudience))
				{
					throw new OAuthRequestException("audience of token '" + tokenAudience + "' is not in allowed client list");
				}

				logger.info("Authenticated as " + user.getEmail() + " (" + user.getUserId() + ")");

				return user;
			}
		}
		catch (Exception e)
		{
			logger.info("Authentication failed: " + e.getMessage() + " " + e);
			return null;
		}
		logger.info("No Authentication");
		return null;
	}
}
