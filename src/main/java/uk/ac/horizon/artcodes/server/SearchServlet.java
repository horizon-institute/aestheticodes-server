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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SearchServlet extends ArtcodeServlet
{
	//private static final Logger logger = Logger.getLogger(SearchServlet.class.getName());
	private static final String INDEX_NAME = "";

	public static Index getIndex()
	{
		return SearchServiceFactory.getSearchService().getIndex(IndexSpec.newBuilder().setName(SearchServlet.INDEX_NAME));

	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		try
		{
			getUser(request);


			final Query query = Query.newBuilder()
					.setOptions(QueryOptions.newBuilder()
							.setLimit(20)
							.setReturningIdsOnly(true)
							.setSortOptions(SortOptions.newBuilder()
									.setMatchScorer(MatchScorer.newBuilder())
									.build()))
					.build(request.getParameter("q"));

			Results<ScoredDocument> results = getIndex().search(query);

			List<String> ids = new ArrayList<>();
			for(ScoredDocument result: results)
			{
				ids.add(result.getId());
			}

			writeJSON(response, ids);
		}
		catch (HTTPException e)
		{
			e.writeTo(response);
		}
	}
}
