package org.apache.log4j.appender;

/**
 * Provides the means for the dispatcher thread to block application threads
 * when the deque is filled to the configured capacity limit.
 * 
 * @author <a href="mailto:simon_park_mail AT yahoo DOT co DOT uk">Simon Park</a>
 * @version 1.1
 */
interface LoggingEventDispatchArbiter {
  /**
   * Invoked by the dispatch thread, this method determines whether application
   * threads should be blocked until space becomes available in the deque.
   * Closes or opens the gate accordingly.
   * 
   * @return <tt>true</tt> if there is space in the deque and the gate is
   *         open.
   */
  boolean acceptEvents();

  /**
   * Invoked by application threads, this method blocks until space becomes
   * available in the deque, or until the dispatch thread explicitly releases
   * the lock.
   * 
   * @return <tt>true</tt> if dispatch is allowed.
   */
  boolean awaitDispatch();

  /**
   * Closes the gate.
   */
  void lock();

  void end();

  /**
   * Opens the gate.
   */
  void unlock();
}