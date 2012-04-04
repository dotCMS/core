package com.dotmarketing.osgi;

import java.util.Arrays;

import org.apache.velocity.tools.view.PrimitiveToolboxManager;
import org.apache.velocity.tools.view.ToolInfo;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.dotmarketing.util.Logger;

public class AbstractViewToolActivator implements BundleActivator, ServiceListener {
	
	private PrimitiveToolboxManager tm;
	
	private BundleContext context;
	
	private ToolInfo info;
	
	public AbstractViewToolActivator(ToolInfo info) {
		this.info = info;
	}
	
	public void start(BundleContext context) throws Exception {
		
		// Save OSGI context
		this.context = context;
		
		// Try to register to ViewTool service
		doRegister();

		// Register itself as listener for service adding/removal
		context.addServiceListener(this);
		
	}

	public void stop(BundleContext context) throws Exception {
		
		// Try to remove ViewTool
		unregister();
		
		// Remove itselt as listener
		context.removeServiceListener(this);

	}
	
	private void doRegister() {
		
		ServiceReference<?> serviceRefSelected = context.getServiceReference(PrimitiveToolboxManager.class.getName());

		if ( serviceRefSelected == null )
			return;
		
		Object service = context.getService(serviceRefSelected);
		this.tm = (PrimitiveToolboxManager) service;
		register();
		
	}
		
	private void register() {
		tm.addTool(info);
	    System.out.println("Added View Tool: " + info.getKey());
	}
	
	private void unregister() {
		if ( tm != null ) {
			tm.removeTool(info);
		}
		System.out.println("Removed View Tool: " + info.getKey());
	}

	public void serviceChanged(ServiceEvent serviceEvent) {
		
		String[] objectClass = (String[]) serviceEvent.getServiceReference().getProperty("objectClass");

		if ( objectClass == null )
			return;
		
		if ( objectClass.length == 0 )
			return;
		
		if (!Arrays.asList(objectClass).contains(PrimitiveToolboxManager.class.getName()))
			return;

		switch (serviceEvent.getType()) {
			
			case ServiceEvent.MODIFIED:
				unregister();
				doRegister();
				break;

			case ServiceEvent.MODIFIED_ENDMATCH:
				break;

			case ServiceEvent.REGISTERED:
				doRegister();
				break;
				
			case ServiceEvent.UNREGISTERING:
				unregister();
				break;
		}
				
	}

}
