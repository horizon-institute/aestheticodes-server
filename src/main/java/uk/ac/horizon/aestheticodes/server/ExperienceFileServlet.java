package uk.ac.horizon.aestheticodes.server;

import com.google.gson.Gson;
import uk.ac.horizon.aestheticodes.model.Experience;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public class ExperienceFileServlet extends HttpServlet
{
	private static final Logger logger = Logger.getLogger(ExperiencesEndpoint.class.getName());

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		final String url = req.getRequestURL().toString();
		String experienceID = url.substring(url.lastIndexOf("/") + 1);
		if(experienceID.endsWith(".artcode"))
		{
			experienceID = experienceID.substring(0, experienceID.indexOf(".artcode"));
		}

		logger.info(url);
		logger.info(experienceID);

		final Experience experience = DataStore.load().type(Experience.class).id(experienceID).now();
		if(experience == null)
		{
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
		else
		{

			resp.setContentType("application/x-artcode");

			Gson gson = ExperienceParser.createParser();
			gson.toJson(experience, resp.getWriter());
		}
	}
}
