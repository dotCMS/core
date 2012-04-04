package com.dotmarketing.osgi;

import java.util.Arrays;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.dotmarketing.portlets.workflows.business.WorkflowAPIOsgiService;

public class AbstractWorkFlowActionletActivator implements BundleActivator, ServiceListener {

	private WorkflowAPIOsgiService service;

	private BundleContext context;

	private Class contentletClass;

	private String name;

	public AbstractWorkFlowActionletActivator(Class contentletClass) {
		this.contentletClass = contentletClass;
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

		ServiceReference<?> serviceRefSelected = context.getServiceReference(WorkflowAPIOsgiService.class.getName());

		if ( serviceRefSelected == null )
			return;

		Object service = context.getService(serviceRefSelected);
		this.service = (WorkflowAPIOsgiService) service;
		register();

	}

	private void register() {
		name = service.addActionlet(contentletClass);
		System.out.println("Added WorkFlowActionlet: " + contentletClass + " with name: " + name);
	}

	private void unregister() {
		service.removeActionlet(name);
		System.out.println("Removed WorkFlowActionlet: " + contentletClass + " with name: " + name);
	}

	public void serviceChanged(ServiceEvent serviceEvent) {

		String[] objectClass = (String[]) serviceEvent.getServiceReference().getProperty("objectClass");

		if ( objectClass == null )
			return;

		if ( objectClass.length == 0 )
			return;

		if (!Arrays.asList(objectClass).contains(WorkflowAPIOsgiService.class.getName()))
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
