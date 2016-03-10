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
import com.derpgroup.echodebugger.model.ContentDao;
import com.derpgroup.echodebugger.resource.EchoDebuggerResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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
  public void initialize(Bootstrap<MainConfig> bootstrap) {}

  @Override
  public void run(MainConfig config, Environment environment) throws IOException {
    if (config.isPrettyPrint()) {
      ObjectMapper mapper = environment.getObjectMapper();
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    // Health checks
    environment.healthChecks().register("basics", new BasicHealthCheck(config, environment));

    // Load up the content
    ContentDao contentDao = new ContentDao(config, environment);
    
    // Build the helper thread that saves data every X minutes
    
    EchoDebuggerResource debuggerResource = new EchoDebuggerResource(config, environment);
    debuggerResource.setContentDao(contentDao);
    
    // Resources
    environment.jersey().register(debuggerResource);
  }
}
