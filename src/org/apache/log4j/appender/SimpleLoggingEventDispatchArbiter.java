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

/**
 * Dependent upon built-in Java synchronization, i.e pre-Java 5.0.
 * 
 * @author <a href="mailto:simon_park_mail AT yahoo DOT co DOT uk">Simon Park</a>
 * @version 1.3
 */
final class SimpleLoggingEventDispatchArbiter implements
    LoggingEventDispatchArbiter {

  private volatile boolean locked = false;

  private volatile boolean acceptingEvents = true;

  /**
   * Configuration properties.
   */
  private final ActiveAsynchronousAppenderProperties properties;

  /**
   * {@link LoggingEvent} buffer - only the size is relevant to this object.
   */
  private final LoggingEventDeque deque;

  /**
   * Shared monitor.
   */
  private final Object dispatchLock = new Object();

  SimpleLoggingEventDispatchArbiter(
      final ActiveAsynchronousAppenderProperties appenderProperties,
      final LoggingEventDeque deque) {
    super();
    this.properties = appenderProperties;
    this.deque = deque;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.log4j.appender.LoggingEventDispatchArbiter#acceptEvents()
   */
  public final boolean acceptEvents() {
    if (this.acceptingEvents) {
      if (this.isBufferFull()) {
        this.lock();
      } else {
        this.unlock();
      }
    }
    return (this.acceptingEvents && (!this.locked));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.log4j.appender.LoggingEventDispatchArbiter#awaitDispatch()
   */
  public final boolean awaitDispatch() {
    if (this.acceptingEvents) {
      if (this.isBlocking()) {
        this.awaitUnlock();
      }
    }
    return this.acceptingEvents;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.log4j.appender.LoggingEventDispatchArbiter#lock()
   */
  public final void lock() {
    if (!this.locked) {
      this.locked = true;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.log4j.appender.LoggingEventDispatchArbiter#end()
   */
  public final void end() {
    this.acceptingEvents = false;
    this.unlock();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.log4j.appender.LoggingEventDispatchArbiter#unlock()
   */
  public final void unlock() {
    if (this.locked || this.isBufferEmpty()) {
      synchronized (this.dispatchLock) {
        this.locked = false;
        this.dispatchLock.notifyAll();
      }
    }
  }

  private void awaitUnlock() {
    try {
      synchronized (this.dispatchLock) {
        while (this.isBlocking()) {
          this.dispatchLock.wait();
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private boolean isBlocking() {
    return (this.locked || this.isBufferFull());
  }

  private boolean isBufferEmpty() {
    return this.deque.isEmpty();
  }

  private boolean isBufferFull() {
    return this.deque.size() >= this.properties.getMaxSize();
  }
}
