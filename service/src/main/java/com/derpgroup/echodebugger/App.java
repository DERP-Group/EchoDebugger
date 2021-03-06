/**
 * Copyright (C) 2015 David Phillips
 * Copyright (C) 2015 Eric Olson
 * Copyright (C) 2015 Rusty Gerard
 * Copyright (C) 2015 Paul Winters
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.derpgroup.echodebugger;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.io.IOException;

import com.derpgroup.echodebugger.configuration.MainConfig;
import com.derpgroup.echodebugger.health.BasicHealthCheck;
import com.derpgroup.echodebugger.jobs.UserDaoLocalImplThread;
import com.derpgroup.echodebugger.model.UserDaoLocalImpl;
import com.derpgroup.echodebugger.providers.ResponderExceptionMapper;
import com.derpgroup.echodebugger.resource.EchoDebuggerResource;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Main method for spinning up the HTTP server.
 *
 * @author David Phillips
 * @since 0.0.1
 */
public class App extends Application<MainConfig> {

	public static void main(String[] args) throws Exception {
		new App().run(args);
	}

	@Override
	public void initialize(Bootstrap<MainConfig> bootstrap) {
		bootstrap.getObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@Override
	public void run(MainConfig config, Environment environment) throws IOException {
		if (config.isPrettyPrint()) {
			ObjectMapper mapper = environment.getObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			mapper.registerModule(new JavaTimeModule());
			mapper.configure( SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false );
			mapper.configure( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false );
		}

		// Health checks
		environment.healthChecks().register("basics", new BasicHealthCheck(config, environment));

		// Load up the content
		UserDaoLocalImpl userDao = new UserDaoLocalImpl(config, environment);
		userDao.initialize();

		// Build the helper thread that saves data every X minutes
		UserDaoLocalImplThread userThread = new UserDaoLocalImplThread(config, userDao);
		userThread.start();

		EchoDebuggerResource debuggerResource = new EchoDebuggerResource(config, environment);
		debuggerResource.setUserDao(userDao);

		// Resources
		environment.jersey().register(debuggerResource);

		// Providers
		environment.jersey().register(new ResponderExceptionMapper());
	}
}
