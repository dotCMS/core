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

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Condition;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantLock;

/**
 * Dependent upon the concurrency utility <a
 * href="http://backport-jsr166.sourceforge.net/">SourceForge
 * backport-util-concurrent</a>.
 * 
 * @author <a href="mailto:simon_park_mail AT yahoo DOT co DOT uk">Simon Park</a>
 * @version 1.2
 */
final class ConcurrentLoggingEventDispatchArbiter implements
    LoggingEventDispatchArbiter {

  private final Lock dispatchLock;

  private final Condition isLocked;

  private final AtomicBoolean locked = new AtomicBoolean(false);

  private final AtomicBoolean acceptingEvents = new AtomicBoolean(true);

  ConcurrentLoggingEventDispatchArbiter() {
    super();
    this.dispatchLock = new ReentrantLock();
    this.isLocked = this.dispatchLock.newCondition();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.log4j.appender.LoggingEventDispatchArbiter#acceptEvents()
   */
  public final boolean acceptEvents() {
    return this.acceptingEvents.get();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.log4j.appender.LoggingEventDispatchArbiter#awaitDispatch()
   */
  public final boolean awaitDispatch() {
    if (this.locked.get()) {
      this.awaitUnlock();
    }
    return this.acceptingEvents.get();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.log4j.appender.LoggingEventDispatchArbiter#lock()
   */
  public final void lock() {
    this.locked.set(true);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.log4j.appender.LoggingEventDispatchArbiter#end()
   */
  public final void end() {
    this.acceptingEvents.set(false);
    this.unlock();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.log4j.appender.LoggingEventDispatchArbiter#unlock()
   */
  public final void unlock() {
    this.dispatchLock.lock();
    try {
      this.locked.set(false);
      this.isLocked.signalAll();
    } finally {
      this.dispatchLock.unlock();
    }
  }

  private void awaitUnlock() {
    this.dispatchLock.lock();
    try {
      while (this.locked.get()) {
        this.isLocked.await();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      this.dispatchLock.unlock();
    }
  }
}
