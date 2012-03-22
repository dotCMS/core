/* 
 * Copyright 1999,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.log4j.appender;

import java.util.Enumeration;

import org.apache.log4j.Appender;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Utility class to allow a single operation to be applied to multiple
 * appenders.
 * 
 * @author <a href="mailto:simon_park_mail AT yahoo DOT co DOT uk">Simon Park</a>
 * @version 1.0
 */
final class AppenderCommandRunner {

  private AppenderCommandRunner() {
    super();
  }

  /**
   * @param appenderCommand
   *                to execute on each appender.
   */
  static final void runOnAllAppenders(final AppenderCommand appenderCommand) {
    AppenderCommandRunner.runOnAllAppenders(appenderCommand, null);
  }

  /**
   * @param appenderCommand
   *                to execute on each appender.
   * @param event
   *                the {@link LoggingEvent} (may be null)
   */
  static final void runOnAllAppenders(final AppenderCommand appenderCommand,
      final LoggingEvent event) {
    synchronized (appenderCommand.attachable()) {
      for (final Enumeration enumeration = appenderCommand.attachable()
          .getAllAppenders(); ((enumeration != null) && enumeration
          .hasMoreElements());) {
        final Appender appender = (Appender) enumeration.nextElement();
        appenderCommand.execute(appender, event);
        if (appenderCommand.hasErrors()) {
          break;
        }
      }
    }
  }
}
