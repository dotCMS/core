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

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentLinkedQueue;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Condition;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantLock;

/**
 * Double-ended queue used to buffer {@link LoggingEvent}s for asynchronous
 * dispatch. This is backed by an {@link ConcurrentLinkedQueue} and is
 * thread-safe.
 * 
 * @author <a href="mailto:simon_park_mail AT yahoo DOT co DOT uk">Simon Park</a>
 * @version 1.1
 * @deprecated
 */
final class LoggingEventConcurrentLinkedQueue implements LoggingEventDeque {

  private final ConcurrentLinkedQueue linkedQueue;

  private final ReentrantLock lock;

  private final Condition noEvents;

  private volatile AtomicInteger size = new AtomicInteger(0);

  public LoggingEventConcurrentLinkedQueue() {
    super();
    this.linkedQueue = new ConcurrentLinkedQueue();
    this.lock = new ReentrantLock();
    this.noEvents = this.lock.newCondition();
  }

  public final boolean isEmpty() {
    return (this.size() == 0);
  }

  /**
   * @return the tail of this deque, or <tt>null</tt> if this deque is empty.
   */
  public final LoggingEvent pollLast() {
    if (this.isEmpty()) {
      return null;
    }
    final LoggingEvent event = (LoggingEvent) this.linkedQueue.poll();
    if (event != null) {
      this.size.decrementAndGet();
    }
    return event;
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
    this.linkedQueue.add(loggingEvent);
    this.size.incrementAndGet();
    try {
      this.lock.lock();
      this.noEvents.signal();
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * @return the number of elements in this deque.
   */
  public final int size() {
    return this.size.get();
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
    try {
      this.lock.lock();
      while (this.isEmpty()) {
        this.noEvents.await();
      }
    } finally {
      this.lock.unlock();
    }
    return this.pollLast();
  }
}
