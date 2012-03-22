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
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.LoggingEvent;

/**
 * @author <a href="mailto:simon_park_mail AT yahoo DOT co DOT uk">Simon Park</a>
 * @version 1.0
 */
final class AppendAppenderCommand implements AppenderCommand {

  private final ActiveAsynchronousAppenderProperties properties;

  private final AppenderAttachable attachable;

  private boolean errors = false;

  /**
   * 
   */
  AppendAppenderCommand(final ActiveAsynchronousAppenderProperties properties,
      final AppenderAttachable attachable) {
    super();
    this.properties = properties;
    this.attachable = attachable;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.log4j.appender.EventSpecificAppenderCommand#attachable()
   */
  public final AppenderAttachable attachable() {
    return this.attachable;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.log4j.appender.AppenderCommand#execute(org.apache.log4j.Appender)
   */
  public final void execute(final Appender appender, final LoggingEvent event) {
    try {
      appender.doAppend(event);
    } catch (RuntimeException e) {
      if (this.shouldThrow()) {
        throw e;
      } else {
        this.errors = true;
        LogLog
            .warn(
                Thread.currentThread().getName()
                    + " recovered from fault during LoggingEvent dispatch - events may have been lost",
                e);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.log4j.appender.AppenderCommand#hasErrors()
   */
  public final boolean hasErrors() {
    return this.errors;
  }

  private boolean shouldThrow() {
    return this.properties.isFailToSync();
  }
}
