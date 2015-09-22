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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract class ArtcodeServlet extends HttpServlet
{
	private static final Logger logger = Logger.getLogger(ArtcodeServlet.class.getName());
//	Enumeration<String> headers = req.getHeaderNames();
//	while (headers.hasMoreElements())
//	{
//		String headerName = headers.nextElement();
//		logger.info(headerName + " = " + req.getHeader(headerName));
//	}

	protected static void verifyUser(User user) throws HTTPException
	{
		if(user == null)
		{
			throw new HTTPException(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
		}
	}

	protected static User getUser()
	{
		OAuthService oauth = OAuthServiceFactory.getOAuthService();

		Set<String> allowedClients = new HashSet<>();
		allowedClients.add(EndpointConstants.WEB_CLIENT_ID);
		allowedClients.add(EndpointConstants.ANDROID_CLIENT_ID);
		allowedClients.add(EndpointConstants.IOS_CLIENT_ID);

		try
		{
			User user = oauth.getCurrentUser(EndpointConstants.EMAIL_SCOPE);
			String tokenAudience = oauth.getClientId(EndpointConstants.EMAIL_SCOPE);

			if (!allowedClients.contains(tokenAudience))
			{
				throw new OAuthRequestException("audience of token '" + tokenAudience + "' is not in allowed client list");
			}

			logger.info("Authenticated as " + user.getEmail() + " (" + user.getUserId() + ")");

			return user;
		}
		catch (Exception e)
		{
			logger.info("Authentication failed: " + e.getMessage());
			return null;
		}
	}
}
