package com.dotcms.listeners;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

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
  private long totalSessionCount = 0;
  private long currentSessionCount = 0;
  private long maxSessionCount = 0;
  private ServletContext context = null;
  
  public void sessionCreated(HttpSessionEvent event) {
    totalSessionCount++;
    currentSessionCount++;	
	
    if (currentSessionCount > maxSessionCount) {
      maxSessionCount = currentSessionCount;
    }
    if (context == null) {
      storeInServletContext(event);
    }
  }//end of sessionCreated method

  public void sessionDestroyed(HttpSessionEvent event) {
	
	if(currentSessionCount > 0){
		currentSessionCount--;
	}
	
	String id = event.getSession().getId();
	
	//note, if no one has logged into the backend and there was a undestroyed session
	//left over from the previous restart of the system, this throws an exception.
	Map<String, String> allusers = (Map<String, String>)context.getAttribute("ipfwUsers");
	
	if(allusers != null && allusers.containsKey(id)){		
		String currentItemName = allusers.get(id);
		allusers.remove(id);
		context.setAttribute("ipfwUsers", allusers);				
	}
	
  }//end of sessionDestroyed method

  /** The total number of sessions created. */
  public long getTotalSessionCount() {
    return(totalSessionCount);
  }

  /** The number of sessions currently in memory. */
  public long getCurrentSessionCount() {
    return(currentSessionCount);
  }

  /** The largest number of sessions ever in memory
   *  at any one time.
   */
  public long getMaxSessionCount() {
    return(maxSessionCount);
  }

  /** Register self in the servlet context so that
   *  we can access the session counts.
   */
  private void storeInServletContext(HttpSessionEvent event) {
    context = event.getSession().getServletContext();
    context.setAttribute("sessionCounter", this);
  }
}
