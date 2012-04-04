package com.dotmarketing.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class HostActivator implements BundleActivator {
	
    private BundleContext m_context = null;

	private static HostActivator instance;

	private HostActivator() {
	}

	public static HostActivator instance() {
		if ( instance == null )
			instance = new HostActivator();
		return instance;
	}
	
    public void start(BundleContext context) {
        m_context = context;
    }

    public void stop(BundleContext context) {
        m_context = null;
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
