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

import org.apache.log4j.Appender;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Utility interface to enable anonymous event-handler style operations to be
 * carried out upon collected appenders for a specific {@link LoggingEvent}.
 * 
 * @author <a href="mailto:simon_park_mail AT yahoo DOT co DOT uk">Simon Park</a>
 * @version 1.0
 */
interface AppenderCommand {

  /**
   * Operate upon the specified {@link Appender} in the context of the
   * {@link LoggingEvent}.
   * 
   * @param appender
   * @param loggingEvent
   */
  void execute(Appender appender, LoggingEvent loggingEvent);

  /**
   * @return The {@link AppenderAttachable} associated with this command.
   */
  AppenderAttachable attachable();

  /**
   * @return <tt>true</tt> if the command execution failed.
   */
  boolean hasErrors();
}
