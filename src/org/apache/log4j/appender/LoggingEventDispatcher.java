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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Accepts {@link LoggingEvent}s from application threads and inserts them into
 * the head of a deque; uses a dispatch thread to take events from the tail of
 * the deque and forward them to attached appenders.
 * <p>
 * At construction time, if the configuration parameter <b>UseConcurrentBackport</b>
 * is true, this class searches the CLASSPATH for the class
 * {@link edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingDeque},
 * typically be found in the <a
 * href="http://backport-jsr166.sourceforge.net/">SourceForge
 * backport-util-concurrent</a> JAR. If the <tt>LinkedBlockingDeque</tt>
 * class is found, then the backport-util-concurrent implementations of the
 * {@link LoggingEventDispatchArbiter} and the {@link LoggingEventDeque} are
 * instantiated and used by this dispatcher. Otherwise the default Java 1.3
 * compatible implementations will be used.
 * 
 * @author <a href="mailto:simon_park_mail AT yahoo DOT co DOT uk">Simon Park</a>
 * @version 1.6
 */
final class LoggingEventDispatcher implements Runnable {

  private volatile boolean run = true;

  /**
   * Attached appenders.
   */
  private final AppenderAttachable appenderAttachable;

  /**
   * Configuration properties.
   */
  private final ActiveAsynchronousAppenderProperties properties;

  /**
   * Arbiter used to control the flow of {@link LoggingEvent}s accepted from
   * application threads.
   */
  private LoggingEventDispatchArbiter dispatchArbiter;

  /**
   * Deque used to buffer {@link LoggingEvent}s ready for dispatch.
   */
  private LoggingEventDeque deque;

  /**
   * A reference to the dispatch thread.
   */
  private Reference threadRef = null;

  LoggingEventDispatcher(final AppenderAttachable parent,
      final ActiveAsynchronousAppenderProperties appenderProperties) {
    super();
    this.appenderAttachable = parent;
    this.properties = appenderProperties;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Runnable#run()
   */
  public final void run() {
    try {
      while (this.isDispatching()) {
        if (this.isAcceptingEvents()) {
          try {
            this.dispatchAndWait();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        } else {
          this.dispatchNoWait();
        }
      }
    } catch (RuntimeException e) {
      this.run = false;
      LogLog.error(Thread.currentThread().getName()
          + " failed during LoggingEvent dispatch", e);
    } finally {
      this.endAsyncDispatch();
    }
  }

  /**
   * Fire up the dispatch thread.
   */
  final void begin() {
    LogLog.debug(Thread.currentThread().getName()
        + " starting asynchronous LoggingEvent dispatch...");
    this.initDispatchImpl();
    final Thread thread = new Thread(this, "Log4J Active Asynchronous Appender");
    thread.setDaemon(true);
    this.threadRef = new WeakReference(thread);
    thread.start();
    LogLog.debug(Thread.currentThread().getName()
        + " started asynchronous LoggingEvent dispatch");
  }

  /**
   * Performs asynchronous dispatch if the dispatcher thread is running,
   * otherwise does a direct dispatch to all attached appenders.
   * 
   * @param event
   */
  final void dispatch(final LoggingEvent event) {
    boolean dispatched = false;
    if (this.isDispatching()) {
      dispatched = this.asyncDispatch(event);
    }
    if (!dispatched) {
      this.syncDispatch(event);
    }
  }

  /**
   * Flush the deque and destroy the dispatcher thread.
   */
  final void end() {
    if (this.run) {
      this.run = false;
      LogLog.debug(Thread.currentThread().getName()
          + " ending asynchronous LoggingEvent dispatch...");
      final Thread thread = this.getDispatchThread();
      if (thread != null) {
        thread.interrupt();
        try {
          thread.join();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          LogLog.error(Thread.currentThread().getName()
              + " failed to stop cleanly - events may have been lost", e);
        }
      }
      this.threadRef.clear();
      LogLog.debug(Thread.currentThread().getName()
          + " ended asynchronous LoggingEvent dispatch");
    }
  }

  /**
   * Use an application thread to flush any lingering {@link LoggingEvent}s
   * from the deque.
   */
  private void appendRemainingAsyncLoggingEvents() {
    this.dispatchAllNoWait();
  }

  /**
   * Put the specified event into the head of the deque as soon as there is
   * space.
   * 
   * @param event
   * @return <tt>true</tt> if the event was successfully dispatched.
   */
  private boolean asyncDispatch(final LoggingEvent event) {
    boolean dispatched = false;
    final boolean dispatchAllowed = this.dispatchArbiter.awaitDispatch();
    if (dispatchAllowed) {
      try {
        this.deque.putFirst(event);
        dispatched = true;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    return dispatched;
  }

  /**
   * Flush the deque on close.
   */
  private void endAsyncDispatch() {
    try {
      this.dispatchArbiter.lock();
      this.dispatchAllNoWait();
    } finally {
      this.dispatchArbiter.end();
    }
  }

  /**
   * @return <tt>true</tt> if there is space in the deque.
   */
  private boolean isAcceptingEvents() {
    return this.dispatchArbiter.acceptEvents();
  }

  /**
   * Dispatch the specified {@link LoggingEvent} to the attached appenders.
   * 
   * @param event
   */
  private void append(final LoggingEvent event) {
    if (event != null) {
      final AppenderCommand command = new AppendAppenderCommand(
          this.properties, this.appenderAttachable);
      AppenderCommandRunner.runOnAllAppenders(command, event);
    }
  }

  /**
   * Flush the deque.
   */
  private void dispatchAllNoWait() {
    while (!this.deque.isEmpty()) {
      this.dispatchNoWait();
    }
  }

  /**
   * Waits for a {@link LoggingEvent} to become available and then dispatches it
   * to the attached appenders.
   * 
   * @throws InterruptedException
   *                 if interrupted during a wait.
   */
  private void dispatchAndWait() throws InterruptedException {
    final LoggingEvent event = this.deque.takeLast();
    this.append(event);
  }

  /**
   * Dispatch the event on the tail of the deque.
   */
  private void dispatchNoWait() {
    final LoggingEvent event = this.deque.pollLast();
    this.append(event);
  }

  private Thread getDispatchThread() {
    return (Thread) this.threadRef.get();
  }

  private void initDispatchImpl() {
    LoggingEventDeque loggingEventDeque = null;
    LoggingEventDispatchArbiter loggingEventDispatchArbiter = null;
    boolean backport = this.properties.isUseConcurrentBackport();
    if (backport) {
      try {
        Class
            .forName("edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingDeque");
        loggingEventDeque = new LoggingEventBlockingDeque(this.properties);
        // loggingEventDeque = new LoggingEventConcurrentLinkedQueue();
        loggingEventDispatchArbiter = new ConcurrentLoggingEventDispatchArbiter();
      } catch (ClassNotFoundException e) {
        LogLog.warn(Thread.currentThread().getName()
            + " unable to find backport-util-concurrent class"
            + ", defaulting to core Java concurrency", e);
        backport = false;
      }
    }
    if (!backport) {
      loggingEventDeque = new LoggingEventLinkedList();
      // loggingEventDeque = new LoggingEventFifoBuffer();
      loggingEventDispatchArbiter = new SimpleLoggingEventDispatchArbiter(
          this.properties, loggingEventDeque);
    }
    this.deque = loggingEventDeque;
    this.dispatchArbiter = loggingEventDispatchArbiter;
    LogLog.debug(Thread.currentThread().getName() + " created "
        + (backport ? "backport-util-concurrent" : "default")
        + " asynchronous dispatch implementation");
  }

  private boolean isDispatching() {
    if (this.run) {
      final Thread thread = this.getDispatchThread();
      return ((thread != null) && (!thread.isInterrupted()));
    }
    return this.run;
  }

  /**
   * Use an application thread to ensure that the deque is clear, then perform a
   * synchronous dispatch.
   * 
   * @param event
   */
  private void syncDispatch(final LoggingEvent event) {
    this.appendRemainingAsyncLoggingEvents();
    this.append(event);
  }
}
