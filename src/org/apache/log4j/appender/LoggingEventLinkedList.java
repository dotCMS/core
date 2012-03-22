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

import java.util.LinkedList;

import org.apache.log4j.spi.LoggingEvent;

/**
 * Double-ended queue used to buffer {@link LoggingEvent}s for asynchronous
 * dispatch. This is backed by a {@link LinkedList} and is thread-safe.
 * 
 * @author <a href="mailto:simon_park_mail AT yahoo DOT co DOT uk">Simon Park</a>
 * @version 1.2
 */
final class LoggingEventLinkedList implements LoggingEventDeque {

  private final LinkedList linkedList;

  public LoggingEventLinkedList() {
    super();
    this.linkedList = new LinkedList();
  }

  public final boolean isEmpty() {
    synchronized (this.linkedList) {
      return this.linkedList.isEmpty();
    }
  }

  /**
   * @return the tail of this deque, or <tt>null</tt> if this deque is empty.
   */
  public final LoggingEvent pollLast() {
    synchronized (this.linkedList) {
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
    synchronized (this.linkedList) {
      this.linkedList.addFirst(loggingEvent);
      // Strictly no need to notifyAll() since there's only one thread waiting,
      // if any
      this.linkedList.notify();
    }
  }

  /**
   * @return the number of elements in this deque.
   */
  public final int size() {
    synchronized (this.linkedList) {
      return this.linkedList.size();
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
    synchronized (this.linkedList) {
      while (this.isEmpty()) {
        this.linkedList.wait();
      }
      return this.removeLast();
    }
  }

  /**
   * @return the tail of this deque, or <tt>null</tt> if the deque is empty.
   */
  private LoggingEvent removeLast() {
    return (!this.isEmpty()) ? (LoggingEvent) this.linkedList.removeLast()
        : null;
  }
}
