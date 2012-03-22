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

import org.apache.log4j.spi.LoggingEvent;

import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingDeque;

/**
 * Double-ended queue used to buffer {@link LoggingEvent}s for asynchronous
 * dispatch. This is backed by a {@link LinkedBlockingDeque} and is thread-safe.
 * 
 * @author <a href="mailto:simon_park_mail AT yahoo DOT co DOT uk">Simon Park</a>
 * @version 1.0
 */
final class LoggingEventBlockingDeque implements LoggingEventDeque {

  private final LinkedBlockingDeque deque;

  public LoggingEventBlockingDeque(
      final ActiveAsynchronousAppenderProperties properties) {
    super();
    this.deque = new LinkedBlockingDeque(properties.getMaxSize());
  }

  public final boolean isEmpty() {
    return this.deque.isEmpty();
  }

  /**
   * @return the tail of this deque, or <tt>null</tt> if this deque is empty.
   */
  public final LoggingEvent pollLast() {
    return (LoggingEvent) this.deque.removeLast();
  }

  /**
   * Inserts the specified event at the front of this deque, and notifies all
   * waiting threads.
   * 
   * @param loggingEvent
   */
  public final void putFirst(final LoggingEvent loggingEvent)
      throws InterruptedException {
    if (loggingEvent == null) {
      return;
    }
    this.deque.putFirst(loggingEvent);
  }

  /**
   * @return the number of elements in this deque.
   */
  public final int size() {
    return this.deque.size();
  }

  /**
   * Retrieves and removes the last element of this deque, waiting if necessary
   * until an element becomes available.
   * 
   * @return the tail of this deque.
   * @throws InterruptedException
   *                 if interrupted whilst waiting.
   */
  public final LoggingEvent takeLast() throws InterruptedException {
    return (LoggingEvent) this.deque.takeLast();
  }
}
