package com.dotmarketing.listeners;

import com.dotcms.business.bytebuddy.ByteBuddyFactory;
import com.dotcms.util.AsciiArt;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 *
 * @author Steve Bolton
 *
 */
public class ByteBuddyContextListener implements ServletContextListener {

	public ByteBuddyContextListener() {
	    AsciiArt.doArt();
	}

    public void contextDestroyed(ServletContextEvent arg0) {
    }

	public void contextInitialized(ServletContextEvent arg0) {
        ByteBuddyFactory.init();
	}

}
