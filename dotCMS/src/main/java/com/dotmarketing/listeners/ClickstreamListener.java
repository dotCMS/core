package com.dotmarketing.listeners;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.factories.ClickstreamFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

/**
 * The listener that keeps track of all clickstreams in the container as well as
 * the creating new Clickstream objects and initiating logging when the
 * clickstream dies (session has been invalidated).
 * 
 * @author <a href="plightbo@hotmail.com">Patrick Lightbody </a>
 */
public class ClickstreamListener implements ServletContextListener, HttpSessionListener {

    public static final String CLICKSTREAMS_ATTRIBUTE_KEY = "clickstreams";
    private static Map<String, Clickstream> clickstreams = Collections.synchronizedMap(new HashMap<String, Clickstream>());

    public ClickstreamListener() {
        Logger.debug(this, "ClickstreamLogger constructed");
    }

    public void contextInitialized(ServletContextEvent sce) {
    	if(Config.getBooleanProperty("ENABLE_CLICKSTREAM_TRACKING", false)){
    		sce.getServletContext().setAttribute(CLICKSTREAMS_ATTRIBUTE_KEY, clickstreams);
    	}
    }

    public void contextDestroyed(ServletContextEvent sce) {
    }

    public void sessionCreated(HttpSessionEvent hse) {
    	if(Config.getBooleanProperty("ENABLE_CLICKSTREAM_TRACKING", false)){
	        HttpSession session = hse.getSession();
	        Logger.debug(this, "Session " + session.getId() + " was created, adding a new clickstream.");
	        Clickstream clickstream = new Clickstream();
	        session.setAttribute("clickstream", clickstream);
	        clickstreams.put(session.getId(), clickstream);
    	}
    }

    public void sessionDestroyed(HttpSessionEvent hse) {
    	if(Config.getBooleanProperty("ENABLE_CLICKSTREAM_TRACKING", false)){
	        HttpSession session = hse.getSession();
	        Logger.debug(this, "Session " + session.getId() + " was destroyed, logging the clickstream and removing it.");
	        try {
	        	
	            Clickstream clickstream = (Clickstream) clickstreams.get(session.getId());
	            ClickstreamFactory.flushClickStream(clickstream);
				clickstreams.remove(session.getId());
	            
	        } catch (Exception e) {
	            Logger.error(this, "An error as ocurred when saving the clickstream");
	        } finally {
	        	try {
					HibernateUtil.closeSession();
				} catch (DotHibernateException e) {
					Logger.error(this, e.getMessage(),e);
				}
	        }
    	}
    }
    
    public static Clickstream getClickstream(String sessionId){
    	return clickstreams.get(sessionId);
    }
    
    
}