package com.dotmarketing.portlets.checkurl.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This singleton represent a RenderService memory for every portlets.
 * 
 * @author	Graziano Aliberti - Engineering Ingegneria Informatica
 * @date	Mar 13, 2012
 */
public class RenderServices {
	
	public static RenderServices INSTANCE = new RenderServices();
	private Map<String, List<DispatcherService>> renderServices = new HashMap<String, List<DispatcherService>>();
	
	private RenderServices(){}

	public List<DispatcherService> getRenderServicesByPortlet(String portletName) {
		return (renderServices.containsKey(portletName))?renderServices.get(portletName):null;
	}

	public void addRenderServicesForPortlet(List<DispatcherService> renderServices, String portletName) {
		if(this.renderServices.containsKey(portletName))
			this.renderServices.remove(portletName);			
		this.renderServices.put(portletName, renderServices);
	}
}
