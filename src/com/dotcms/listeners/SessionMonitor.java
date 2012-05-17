package com.dotcms.listeners;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;

/** 
 *  Listener that keeps track of logged in users
 *  by monitoring for USER_ID session attribute addition.
 *  
 *  By: IPFW Web Team
 */
public class SessionMonitor implements HttpSessionAttributeListener {
  
  // this will hold all logged in users
  private Map<String, String> sysUsers = new HashMap<String, String>();

  // getter for the bean - sysUsers
  public Map<String, String> getUsers(){
	return( sysUsers );
  }
	
  /** Checks if the attribute added was USER_ID
   *  if so, adds the login user to the bean.
   *  
   *  The bean is then saved to the servlet context.
   */
  public void attributeAdded(HttpSessionBindingEvent event) {
    String currentAttributeName = event.getName().toString();
    
	if(currentAttributeName.equals("USER_ID")){
		String currentItemName = event.getValue().toString();
		String id = event.getSession().getId();
		sysUsers.put(id, currentItemName);
		
		ServletContext context = event.getSession().getServletContext();			
		context.setAttribute("ipfwUsers", sysUsers);	
	}
	   
  }//end of attributeAdded method

  /** 
   *  Checks if the attribute removed is "USER_ID".
   *  If so, remove the logout user from the bean.
   * 
   */
  public void attributeRemoved(HttpSessionBindingEvent event) {
	String currentAttributeName = event.getName().toString();
    
	if(currentAttributeName.equals("USER_ID")){
		
		String id = event.getSession().getId();
		
		if(sysUsers.containsKey(id)){
			String currentItemName = event.getValue().toString();
			ServletContext context = event.getSession().getServletContext();			
			sysUsers.remove(id);			
			context.setAttribute("ipfwUsers", sysUsers);

		}
	}
  }

  /** 
   *  Do nothing here.
   */
  public void attributeReplaced(HttpSessionBindingEvent event) {}

}
