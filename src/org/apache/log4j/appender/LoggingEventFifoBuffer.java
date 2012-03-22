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

import org.apache.commons.collections.buffer.UnboundedFifoBuffer;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Double-ended queue used to buffer {@link LoggingEvent}s for asynchronous
 * dispatch. This is backed by an {@link UnboundedFifoBuffer} and is
 * thread-safe.
 * 
 * @author <a href="mailto:simon_park_mail AT yahoo DOT co DOT uk">Simon Park</a>
 * @version 1.1
 */
final class LoggingEventFifoBuffer implements LoggingEventDeque {

  private final UnboundedFifoBuffer fifoBuffer;

  public LoggingEventFifoBuffer() {
    super();
    this.fifoBuffer = new UnboundedFifoBuffer();
  }

  public final boolean isEmpty() {
    synchronized (this.fifoBuffer) {
      return this.fifoBuffer.isEmpty();
    }
  }

  /**
   * @return the tail of this deque, or <tt>null</tt> if this deque is empty.
   */
  public final LoggingEvent pollLast() {
    synchronized (this.fifoBuffer) {
      return this.removeLast();
    }
  }

  /**
   * Inserts the specified event at the front of this deque, and notifies all
   * waiting threads.
   * 
   * @param loggingEvent
   */
  public final void putFirst(final LoggingEvent loggingEvent) {
    if (loggingEvent == null) {
      return;
    }
    synchronized (this.fifoBuffer) {
      this.fifoBuffer.add(loggingEvent);
      // No need to notifyAll() since there's only one thread waiting, if any
      this.fifoBuffer.notify();
    }
  }

  /**
   * @return the number of elements in this deque.
   */
  public final int size() {
    synchronized (this.fifoBuffer) {
      return this.fifoBuffer.size();
    }
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
    synchronized (this.fifoBuffer) {
      while (this.isEmpty()) {
        this.fifoBuffer.wait();
      }
      return this.removeLast();
    }
  }

  /**
   * @return the tail of this deque, or <tt>null</tt> if the deque is empty.
   */
  private LoggingEvent removeLast() {
    return (!this.isEmpty()) ? (LoggingEvent) this.fifoBuffer.remove() : null;
  }
}
