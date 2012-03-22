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

import java.util.Enumeration;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.AsyncAppender;
import org.apache.log4j.helpers.AppenderAttachableImpl;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.OptionHandler;

/**
 * This appender logs {@link LoggingEvent}s asynchronously. It acts solely as
 * an event dispatcher, and must therefore be attached to one or more child
 * appenders in order to do useful logging. It is the user's responsibility to
 * close appenders, typically at the end of the application lifecycle.
 * <p>
 * The appender buffers events into the front of a deque on the application
 * thread(s). The appender's dispatch thread takes events from the back of the
 * deque, and dispatches them to all the appenders that are attached to this
 * appender.
 * <p>
 * The appender's event buffer is configured with a maximum size. If the buffer
 * is filled up, then application threads are blocked from logging new events
 * until the dispatch thread has had a chance to dispatch one or more events.
 * When the buffer is no longer at its maximum configured capacity, application
 * threads are notified, and are able to start logging events once more.
 * Asynchronous logging therefore becomes pseudo-synchronous when the appender
 * is operating at or near the capacity of its event buffer. This is not
 * necessarily a bad thing, it's the price a threaded application will have to
 * pay sometimes; the appender is designed to allow the application to keep on
 * running, albeit taking slightly more time to log events until the pressure on
 * the appender's buffer eases.
 * <p>
 * Optimally tuning the size of the appender's event buffer for maximum
 * application throughput depends upon several factors. Any or all of the
 * following factors are likely to cause pseudo-synchronous behaviour to be
 * exhibited:
 * <ul>
 * <li>Large numbers of application threads</li>
 * <li>Large numbers of logging events per application call</li>
 * <li>Large amounts of data per logging event</li>
 * <li>High latency of child appenders</li>
 * </ul>
 * As a strategem to keep things moving, increasing the size of the appender's
 * buffer will generally help, at the expense of heap available to the
 * application when large numbers of logging events are buffered.
 * <p>
 * It is possible to attach separate instances of this appender to one another
 * to achieve multi-threaded dispatch. For example, assume a requirement for two
 * child appenders that each perform relatively expensive operations such as
 * messaging and file IO. For this case, one could set up a graph of appenders
 * such that the parent asynchronous appender, A, has two child asynchronous
 * appenders attached, B and C. Let's say B in turn has a child file IO appender
 * attached, and C has a child messaging appender attached. This will result in
 * fast dispatch from A to both B and C via A's dispatch thread. B's dispatch
 * thread will be dedicated to logging to file IO, and C's dispatch thread will
 * be dedicated to logging via messaging.
 * <p>
 * {@link RuntimeException}s are caught by the dispatch thread. For this case,
 * the appender can be configured either to continue dispatching events
 * asynchronously, or to fail back to synchronous logging. By default the
 * appender fails back to synchronous logging, since this is the default
 * behaviour of the standard Log4J {@link AsyncAppender}. To configure the
 * appender to continue asynchronous dispatch, set the <tt>FailToSync</tt>
 * flag to <tt>false</tt>.
 * <p>
 * The appender can be configured to attempt to use a dispatch implementation
 * that uses the <a href="http://backport-jsr166.sourceforge.net/">SourceForge
 * backport-util-concurrent</a> concurrency JAR. The appender will attempt to
 * use the backport-util-concurrent implementation if the
 * <tt>UseConcurrentBackport</tt> flag to <tt>true</tt>. If the
 * backport-util-concurrent JAR is not in the CLASSPATH, or the flag is false,
 * the appender will use the default dispatch implementation instead. The
 * default uses JSR166 Java concurrency.
 * <p>
 * Sample configuration:<br/>&lt;appender
 * name=&quot;active-async-appender&quot;
 * class=&quot;org.apache.log4j.appender.ActiveAsynchronousAppender&quot;&gt;<br/>
 * &lt;appender-ref ref=&quot;console&quot; /&gt;<br/>&lt;param
 * name=&quot;BufferSize&quot; value=&quot;16&quot; /&gt; &lt;!-- Default is 32
 * --&gt;<br/>&lt;param name=&quot;FailToSync&quot; value=&quot;false&quot;
 * /&gt; &lt;!-- Default is true --&gt;<br/>&lt;param
 * name=&quot;LocationInfo&quot; value=&quot;false&quot; /&gt; &lt;!-- Default
 * is false --&gt;<br/>&lt;param name=&quot;UseConcurrentBackport&quot;
 * value=&quot;true&quot; /&gt; &lt;!-- Default is true --&gt;<br/>&lt;/appender&gt;
 * 
 * @see AsyncAppender
 * @author <a href="mailto:simon_park_mail AT yahoo DOT co DOT uk">Simon Park</a>
 * @version 1.3
 */
public final class ActiveAsynchronousAppender extends AppenderSkeleton
    implements Appender, AppenderAttachable, OptionHandler {

  /**
   * The default buffer size is set to 32 events.
   */
  public static final int DEFAULT_BUFFER_SIZE = 32;

  /**
   * Only for compatibility.
   */
  AppenderAttachableImpl aai;

  /**
   * Nested appenders.
   */
  private AppenderAttachable appenderAttachable = null;

  /**
   * Buffer size.
   */
  private int bufferSize = DEFAULT_BUFFER_SIZE;

  /**
   * {@link LoggingEvent} dispatcher, responsible for forwarding events to
   * attached appenders.
   */
  private LoggingEventDispatcher dispatcher = null;

  /**
   * Configuration properties.
   */
  private final ActiveAsynchronousAppenderProperties properties = new ActiveAsynchronousAppenderProperties();

  public ActiveAsynchronousAppender() {
    super();
    this.aai = new AppenderAttachableImpl();
    this.appenderAttachable = this.aai; // Weird compatibility thing
    this.getProperties().setMaxSize(
        ActiveAsynchronousAppender.DEFAULT_BUFFER_SIZE);
    this.dispatcher = new LoggingEventDispatcher(this.appenderAttachable, this
        .getProperties());
  }

  /**
   * @see org.apache.log4j.AppenderSkeleton#activateOptions()
   */
  public final void activateOptions() {
    this.dispatcher.begin();
  }

  /**
   * @see org.apache.log4j.spi.AppenderAttachable#addAppender(org.apache.log4j.Appender)
   */
  public final void addAppender(final Appender newAppender) {
    synchronized (this.appenderAttachable) {
      this.appenderAttachable.addAppender(newAppender);
    }
  }

  /**
   * Closes all attached appenders.
   * 
   * @see org.apache.log4j.Appender#close()
   */
  public final void close() {
    if (!this.isClosed()) {
      this.setClosed(true);
      this.dispatcher.end();
      final AppenderCommand appenderCommand = new CloseAppenderCommand(
          this.appenderAttachable);
      AppenderCommandRunner.runOnAllAppenders(appenderCommand);
    }
  }

  /**
   * Non-synchronized copy-and-paste override of
   * {@link AppenderSkeleton#doAppend(LoggingEvent)}.
   * 
   * @see org.apache.log4j.AppenderSkeleton#doAppend(org.apache.log4j.spi.LoggingEvent)
   */
  public final void doAppend(final LoggingEvent event) {
    if (this.isClosed()) {
      LogLog.error("Attempted to append to closed appender named [" + name
          + "].");
      return;
    }
    if (!super.isAsSevereAsThreshold(event.getLevel())) {
      return;
    }
    for (Filter f = super.getFirstFilter(); f != null;) {
      switch (f.decide(event)) {
      case Filter.DENY:
        return;
      case Filter.NEUTRAL:
        f = f.getNext();
        break;
      case Filter.ACCEPT:
        f = null;
        break;
      default:
        f = null;
        LogLog.error("Unknown Filter type");
      }
    }
    this.append(event);
  }

  /**
   * @see org.apache.log4j.spi.AppenderAttachable#getAllAppenders()
   */
  public final Enumeration getAllAppenders() {
    synchronized (this.appenderAttachable) {
      return this.appenderAttachable.getAllAppenders();
    }
  }

  /**
   * @see org.apache.log4j.spi.AppenderAttachable#getAppender(java.lang.String)
   */
  public final Appender getAppender(final String name) {
    synchronized (this.appenderAttachable) {
      return this.appenderAttachable.getAppender(name);
    }
  }

  /**
   * Gets the current buffer size.
   * 
   * @return the current value of the <b>BufferSize</b> option.
   */
  public final int getBufferSize() {
    return bufferSize;
  }

  /**
   * @return the current value of the <b>LocationInfo</b> option.
   * @see AsyncAppender#getLocationInfo()
   */
  public final boolean getLocationInfo() {
    return this.getProperties().isLocationInfo();
  }

  /**
   * @return the current value of the <b>UseConcurrentBackport</b> option
   */
  public final boolean getUseConcurrentBackport() {
    return this.getProperties().isUseConcurrentBackport();
  }

  /**
   * @see org.apache.log4j.spi.AppenderAttachable#isAttached(org.apache.log4j.Appender)
   */
  public final boolean isAttached(final Appender appender) {
    synchronized (this.appenderAttachable) {
      return this.appenderAttachable.isAttached(appender);
    }
  }

  /**
   * @see org.apache.log4j.spi.AppenderAttachable#removeAllAppenders()
   */
  public final void removeAllAppenders() {
    synchronized (this.appenderAttachable) {
      this.appenderAttachable.removeAllAppenders();
    }
  }

  /**
   * @see org.apache.log4j.spi.AppenderAttachable#removeAppender(org.apache.log4j.Appender)
   */
  public final void removeAppender(final Appender appender) {
    synchronized (this.appenderAttachable) {
      this.appenderAttachable.removeAppender(appender);
    }
  }

  /**
   * @see org.apache.log4j.spi.AppenderAttachable#removeAppender(java.lang.String)
   */
  public final void removeAppender(final String name) {
    synchronized (this.appenderAttachable) {
      this.appenderAttachable.removeAppender(name);
    }
  }

  /**
   * @see org.apache.log4j.Appender#requiresLayout()
   */
  public final boolean requiresLayout() {
    return false;
  }

  /**
   * @param size
   *                buffer size, must be positive.
   * @see AsyncAppender#setBufferSize(int)
   */
  public final void setBufferSize(final int size) {
    // Same behaviour as AsyncAppender for compatibility
    if (size < 0) {
      throw new java.lang.NegativeArraySizeException("size");
    }
    this.getProperties().setMaxSize((size < 1) ? 1 : size);
  }

  /**
   * @param flag
   *                true if the asynchronous appender should fail-over to
   *                synchronous logging in the event that an attempt to append a
   *                LoggingEvent fails due to a {@link RuntimeException}.
   */
  public final void setFailToSync(final boolean flag) {
    this.getProperties().setFailToSync(flag);
  }

  /**
   * @param flag
   *                true if location information should be extracted.
   * @see AsyncAppender#setLocationInfo(boolean)
   */
  public final void setLocationInfo(final boolean flag) {
    this.getProperties().setLocationInfo(flag);
  }

  /**
   * @param flag
   *                true if SourceForge backport-util-concurrent should be used.
   */
  public final void setUseConcurrentBackport(final boolean flag) {
    this.getProperties().setUseConcurrentBackport(flag);
  }

  /**
   * @return true if this appender has already been closed.
   */
  final boolean isClosed() {
    return super.closed;
  }

  /**
   * @param closed
   */
  final void setClosed(final boolean closed) {
    super.closed = closed;
  }

  /**
   * Forwards {@link LoggingEvent}s to the dispatcher, which in turn delegates
   * append operations to the attached appenders.
   * 
   * @see org.apache.log4j.AppenderSkeleton#append(org.apache.log4j.spi.LoggingEvent)
   */
  protected final void append(final LoggingEvent event) {
    // ***
    // LoggingEvent::getXXX() operations copied-and-pasted from AsyncAppender
    // for compatibility.
    // ***
    // Set the NDC and thread name for the calling thread as these
    // LoggingEvent fields were not set at event creation time.
    event.getNDC();
    event.getThreadName();
    // Get a copy of this thread's MDC.
    event.getMDCCopy();
    if (this.getLocationInfo()) {
      event.getLocationInformation();
    }

    this.dispatcher.dispatch(event);
  }

  private ActiveAsynchronousAppenderProperties getProperties() {
    return this.properties;
  }
}
