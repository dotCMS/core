package com.dotmarketing.osgi;

import com.dotcms.repackage.org.osgi.framework.Bundle;
import com.dotcms.repackage.org.osgi.framework.BundleActivator;
import com.dotcms.repackage.org.osgi.framework.BundleContext;

import javax.servlet.ServletContext;

public class HostActivator implements BundleActivator {
	
    private BundleContext m_context = null;
    private ServletContext servletContext;

    private static HostActivator instance;

	private HostActivator() {
	}

	public synchronized static HostActivator instance() {
		if ( instance == null )
			instance = new HostActivator();
		return instance;
	}
	
    public void start(BundleContext context) {

        if ( servletContext != null ) {
            servletContext.setAttribute( BundleContext.class.getName(), context );
        }
        m_context = context;
    }

    public void stop(BundleContext context) {
        m_context = null;
    }

    public void setServletContext ( ServletContext servletContext ) {
        this.servletContext = servletContext;
    }

    public BundleContext getBundleContext() {
    	return m_context;
    }
    
    public Bundle[] getBundles() {
        if (m_context != null)
            return m_context.getBundles();
        return null;
    }

}