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

package com.derpgroup.echodebugger.configuration;

import io.dropwizard.Configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Top-level configuration class.
 *
 * @author David Phillips
 * @since 0.0.1
 */
public class MainConfig extends Configuration {
  private boolean prettyPrint = true;
  private EchoDebuggerConfig echoDebuggerConfig;

  @JsonProperty
  public boolean isPrettyPrint() {
    return prettyPrint;
  }

  @JsonProperty
  public void setPrettyPrint(boolean prettyPrint) {
    this.prettyPrint = prettyPrint;
  }

  @JsonProperty
  public EchoDebuggerConfig getEchoDebuggerConfig() {
    return echoDebuggerConfig;
  }

  @JsonProperty
  public void setEchoDebuggerConfig(EchoDebuggerConfig echoDebuggerConfig) {
    this.echoDebuggerConfig = echoDebuggerConfig;
  }
}
