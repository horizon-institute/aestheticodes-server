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

import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.MatchScorer;
import com.google.appengine.api.search.Query;
import com.google.appengine.api.search.QueryOptions;
import com.google.appengine.api.search.Results;
import com.google.appengine.api.search.ScoredDocument;
import com.google.appengine.api.search.SearchServiceFactory;
import com.google.appengine.api.search.SortOptions;
import com.google.appengine.api.users.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.horizon.aestheticodes.model.ExperienceAvailability;
import uk.ac.horizon.aestheticodes.model.ExperienceEntry;
import uk.ac.horizon.artcodes.server.utils.ArtcodeServlet;
import uk.ac.horizon.artcodes.server.utils.DataStore;
import uk.ac.horizon.artcodes.server.utils.ExperienceItems;
import uk.ac.horizon.artcodes.server.utils.HTTPException;

public class SearchServlet extends ArtcodeServlet
{
	//private static final Logger logger = Logger.getLogger(SearchServlet.class.getName());

	public static Index getIndex()
	{
		return SearchServiceFactory.getSearchService().getIndex(IndexSpec.newBuilder().setName(getIndexName()));
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		try
		{
			final String index = request.getParameter("index");
			if (index != null && index.equals("true"))
			{
				final User user = getUser(request);
				if (isAdmin(user))
				{
					final List<ExperienceAvailability> availabilities = DataStore.load()
							.type(ExperienceAvailability.class)
							.list();

					final Collection<String> ids = new HashSet<>();
					final List<ExperienceEntry> entries = new ArrayList<>();
					for (ExperienceAvailability availability : availabilities)
					{
						if (!ids.contains(availability.getUri()))
						{
							ids.add(availability.getUri());
							ExperienceEntry entry = DataStore.load().type(ExperienceEntry.class).id(getEntryID(availability.getUri())).now();
							if (entry != null)
							{
								entries.add(entry);
							}
						}
					}

					ExperienceItems.index(entries);
				}
			}

			if (request.getParameter("q") == null)
			{
				throw new HTTPException(400, "Missing query parameter q");
			}
			else
			{
				final Query query = Query.newBuilder()
						.setOptions(QueryOptions.newBuilder()
								.setLimit(20)
								.setReturningIdsOnly(true)
								.setSortOptions(SortOptions.newBuilder()
										.setMatchScorer(MatchScorer.newBuilder())
										.build()))
						.build(request.getParameter("q"));

				final Results<ScoredDocument> results = getIndex().search(query);

				final List<String> ids = new ArrayList<>();
				for (ScoredDocument result : results)
				{
					ids.add(result.getId());
				}

				writeJSON(response, ids);
			}
		}
		catch (HTTPException e)
		{
			e.writeTo(response);
		}
	}
}
