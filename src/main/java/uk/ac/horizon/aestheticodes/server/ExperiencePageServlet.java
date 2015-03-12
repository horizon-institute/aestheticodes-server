package uk.ac.horizon.aestheticodes.server;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import uk.ac.horizon.aestheticodes.model.Experience;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ExperiencePageServlet extends HttpServlet
{
	private static final Logger logger = Logger.getLogger(ExperiencesEndpoint.class.getName());

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		final String url = req.getRequestURL().toString();
		final String experienceID = url.substring(url.lastIndexOf("/") + 1);

		logger.info(url);
		logger.info(experienceID);

		final Experience experience = DataStore.load().type(Experience.class).id(experienceID).now();
		if(experience == null)
		{
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
		else
		{
			final Map<String, String> variables = new HashMap<>();

			variables.put("id", experience.getId());
			variables.put("title", experience.getName());
			variables.put("description", experience.getDescription());
			variables.put("author", experience.getOwner().getName());
			variables.put("image", experience.getImage());
			variables.put("icon", experience.getIcon());
			variables.put("url", req.getRequestURL().toString());

			resp.setContentType("text/html");

			final MustacheFactory mustacheFactory = new DefaultMustacheFactory();
			final Mustache mustache = mustacheFactory.compile("experience.mustache");
			mustache.execute(resp.getWriter(), variables).flush();
		}
	}
}
