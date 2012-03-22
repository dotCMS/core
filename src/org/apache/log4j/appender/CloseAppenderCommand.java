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
final class CloseAppenderCommand implements AppenderCommand {

  private final AppenderAttachable attachable;

  /**
   * 
   */
  CloseAppenderCommand(final AppenderAttachable attachable) {
    super();
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
      appender.close();
    } catch (RuntimeException e) {
      LogLog.error(Thread.currentThread().getName()
          + " failed to close appender named "
          + String.valueOf(appender.getName()), e);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.log4j.appender.AppenderCommand#hasErrors()
   */
  public final boolean hasErrors() {
    return false; // continue to close remaining appenders
  }
}
