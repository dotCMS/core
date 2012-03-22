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

/**
 * Configuration properties used by the {@link ActiveAsynchronousAppender}.
 * 
 * @author <a href="mailto:simon_park_mail AT yahoo DOT co DOT uk">Simon Park</a>
 * @version 1.1
 */
final class ActiveAsynchronousAppenderProperties {

  /**
   * Maximum capacity of the event buffer before blocking occurs.
   */
  private int maxSize = 0;

  /**
   * When true, the appender will fail-over to synchronous logging mode if an
   * asynchronous append operation fails. Defaults to true.
   */
  private boolean failToSync = true;

  /**
   * When true, the appender will use <a
   * href="http://backport-jsr166.sourceforge.net/">SourceForge
   * backport-util-concurrent</a>. Defaults to true.
   */
  private boolean useConcurrentBackport = true;

  /**
   * When true, location info will be included in dispatched messages. Defaults
   * to false.
   */
  private boolean locationInfo = false;

  ActiveAsynchronousAppenderProperties() {
    super();
  }

  final boolean isFailToSync() {
    return this.failToSync;
  }

  final void setFailToSync(final boolean failToSync) {
    this.failToSync = failToSync;
  }

  final int getMaxSize() {
    return this.maxSize;
  }

  final void setMaxSize(final int maxSize) {
    this.maxSize = maxSize;
  }

  final boolean isLocationInfo() {
    return this.locationInfo;
  }

  final void setLocationInfo(final boolean locationInfo) {
    this.locationInfo = locationInfo;
  }

  final boolean isUseConcurrentBackport() {
    return this.useConcurrentBackport;
  }

  final void setUseConcurrentBackport(final boolean useConcurrentBackport) {
    this.useConcurrentBackport = useConcurrentBackport;
  }

}
