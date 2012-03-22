package org.apache.log4j.appender;

import org.apache.log4j.spi.LoggingEvent;

/**
 * Double-ended queue used to buffer {@link LoggingEvent}s for asynchronous
 * dispatch.
 * 
 * @author <a href="mailto:simon_park_mail AT yahoo DOT co DOT uk">Simon Park</a>
 * @version 1.1
 */
interface LoggingEventDeque {

  boolean isEmpty();

  /**
   * @return the tail of this deque, or <tt>null</tt> if this deque is empty.
   */
  LoggingEvent pollLast();

  /**
   * Inserts the specified event at the front of this deque, and notifies all
   * waiting threads.
   * 
   * @param loggingEvent
   * @throws InterruptedException
   */
  void putFirst(LoggingEvent loggingEvent) throws InterruptedException;

  /**
   * @return the number of elements in this deque.
   */
  int size();

  /**
   * Retrieves and removes the last element of this deque, waiting if necessary
   * until an element becomes available.
   * 
   * @return the tail of this deque.
   * @throws InterruptedException
   *                 if interrupted whilst waiting.
   */
  LoggingEvent takeLast() throws InterruptedException;

}