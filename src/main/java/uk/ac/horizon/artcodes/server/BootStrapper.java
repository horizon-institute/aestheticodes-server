package uk.ac.horizon.artcodes.server;

import com.googlecode.objectify.ObjectifyService;
import uk.ac.horizon.aestheticodes.model.*;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class BootStrapper implements ServletContextListener
{
	public void contextInitialized(ServletContextEvent event) {
		ObjectifyService.init();
		ObjectifyService.register(ExperienceEntry.class);
		ObjectifyService.register(ExperienceAvailability.class);
		ObjectifyService.register(ExperienceInteraction.class);
		ObjectifyService.register(ExperienceDeleted.class);
		ObjectifyService.register(ExperienceCache.class);
	}
}