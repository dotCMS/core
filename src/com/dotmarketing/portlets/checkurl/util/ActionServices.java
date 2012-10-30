package com.dotmarketing.portlets.checkurl.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This singleton represent an ActionService memory for every portlets.
 * 
 * @author	Graziano Aliberti - Engineering Ingegneria Informatica
 * @date	Mar 13, 2012
 */
public class ActionServices {
	
	public static ActionServices INSTANCE = new ActionServices();
	private Map<String, List<DispatcherService>> actionServices = new HashMap<String, List<DispatcherService>>();
	
	private ActionServices(){}

	public List<DispatcherService> getActionServicesByPortlet(String portletName) {
		return (actionServices.containsKey(portletName))?actionServices.get(portletName):null;
	}

	public void addActionServicesForPortlet(List<DispatcherService> actionServices, String portletName) {
		if(this.actionServices.containsKey(portletName))
			this.actionServices.remove(portletName);			
		this.actionServices.put(portletName, actionServices);
	}
}
