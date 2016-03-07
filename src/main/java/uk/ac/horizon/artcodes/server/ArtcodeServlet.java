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

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.oauth.OAuthService;
import com.google.appengine.api.oauth.OAuthServiceFactory;
import com.google.appengine.api.users.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
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
	private static final GoogleIdTokenVerifier verifier;

	static
	{
		allowedClients.add(EndpointConstants.WEB_CLIENT_ID);
		allowedClients.add(EndpointConstants.ANDROID_CLIENT_ID);
		allowedClients.add(EndpointConstants.IOS_CLIENT_ID);

		// If you retrieved the token on Android using the Play Services 8.3 API or newer, set
		// the issuer to "https://accounts.google.com". Otherwise, set the issuer to
		// "accounts.google.com". If you need to verify tokens from multiple sources, build
		// a GoogleIdTokenVerifier for each issuer and try them both.
		verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
				//.setAudience(Collections.singletonList(EndpointConstants.ANDROID_AUDIENCE))
				.setAudience(allowedClients)
				// If you retrieved the token on Android using the Play Services 8.3 API or newer, set
				// the issuer to "https://accounts.google.com". Otherwise, set the issuer to
				// "accounts.google.com". If you need to verify tokens from multiple sources, build
				// a GoogleIdTokenVerifier for each issuer and try them both.
				.setIssuers(Arrays.asList("https://accounts.google.com", "accounts.google.com"))
				.build();
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

	static void writeJSON(HttpServletResponse resp, Object item) throws IOException
	{
		String json = gson.toJson(item);
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		resp.getWriter().write(json);
	}

	void write(HttpServletResponse resp, int status, String message)
	{
		logger.info(status + ": " + message);
		resp.setStatus(status);
	}

	static String getExperienceID(HttpServletRequest req)
	{
		String url = req.getRequestURL().toString();
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
				final User user = oauth.getCurrentUser(EndpointConstants.EMAIL_SCOPE);
				final String tokenAudience = oauth.getClientId(EndpointConstants.EMAIL_SCOPE);
				if (!allowedClients.contains(tokenAudience))
				{
					throw new OAuthRequestException("audience of token '" + tokenAudience + "' is not in allowed client list");
				}

				logger.info("Authenticated as " + user.getEmail() + " (" + user.getUserId() + ")");

				try
				{
					final String idTokenString = authHeader.substring("Bearer ".length());
					//logger.info(idTokenString);
					final GoogleIdToken idToken = verifier.verify(idTokenString);
					if (idToken != null)
					{
						GoogleIdToken.Payload payload = idToken.getPayload();

						// Print user identifier
						String userId = payload.getSubject();
						System.out.println("User ID: " + userId);

						for (String key : payload.keySet())
						{
							logger.info(key + " = " + payload.get(key));
						}
					}
					else
					{
						logger.info("Invalid ID token.");
					}
				}
				catch (Exception e)
				{
					//logger.log(Level.INFO, e.getMessage(), e);
				}

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
