package com.dotcms.listeners;

import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.dotmarketing.util.WebKeys;

/** 
 *  Listener that keeps track of the number of sessions
 *  that the Web application is currently using and has
 *  ever used in its life cycle.
 *
 *  SessionDestroyed checks if the session id is one associated
 *  with one of the login users. If so, makes sure to remove 
 *  that user from the bean.
 * 
 *  By: IPFW Web Team
 */
public class SessionCounter implements HttpSessionListener {
  private AtomicLong totalSessionCount = new AtomicLong(0L);
  private AtomicLong currentSessionCount = new AtomicLong(0L);
  private AtomicLong maxSessionCount = new AtomicLong(0L);
  private ServletContext context = null;
  
  public void sessionCreated(HttpSessionEvent event) {
    totalSessionCount.addAndGet(1);
    currentSessionCount.addAndGet(1);
	
    synchronized(maxSessionCount) {
        if (currentSessionCount.longValue() > maxSessionCount.longValue()) {
          maxSessionCount = currentSessionCount;
        }
    }
    
    if (context == null) {
      storeInServletContext(event);
    }
  }

  public void sessionDestroyed(HttpSessionEvent event) {
	currentSessionCount.decrementAndGet();
	currentSessionCount.compareAndSet(-1, 0);
  }

  /** The total number of sessions created. */
  public long getTotalSessionCount() {
    return(totalSessionCount.longValue());
  }

  /** The number of sessions currently in memory. */
  public long getCurrentSessionCount() {
    return(currentSessionCount.longValue());
  }

  /** The largest number of sessions ever in memory
   *  at any one time.
   */
  public long getMaxSessionCount() {
    return(maxSessionCount.longValue());
  }

  /** Register self in the servlet context so that
   *  we can access the session counts.
   */
  private void storeInServletContext(HttpSessionEvent event) {
    context = event.getSession().getServletContext();
    context.setAttribute(WebKeys.SESSION_COUNTER, this);
  }
}
