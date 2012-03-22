
package com.dotmarketing.business.web;

import com.dotmarketing.business.Locator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.web.ContentletWebAPI;
import com.dotmarketing.portlets.contentlet.business.web.ContentletWebAPIImpl;
import com.dotmarketing.util.Logger;


/**
 * APILocator is a factory method to get single(ton) service objects. 
 * This is intended to serve web APIs the ones that live into the web folder. 
 * This is a kind of implementation, and there may be others.
 * @author David Torres
 * @version 1.6
 * @since 1.6
 */

// EVALUAR UNA CLASE BASE Locator
public class WebAPILocator extends Locator<WebAPIIndex>{
	
	protected static WebAPILocator instance;

	private WebAPILocator() {
		super();
	}

	public synchronized static void init(){
		if(instance != null)
			return;
		instance = new WebAPILocator();
	}
	
	public static UserWebAPI getUserWebAPI() {
        return (UserWebAPI)getInstance(WebAPIIndex.USER_WEB_API);
    }

	//http://jira.dotmarketing.net/browse/DOTCMS-2273
	public static ContentletWebAPI getContentletWebAPI() {
        return (ContentletWebAPI)getInstance(WebAPIIndex.CONTENTLET_WEB_API);
    }

	public static LanguageWebAPI getLanguageWebAPI() {
        return (LanguageWebAPI)getInstance(WebAPIIndex.LANGUAGE_WEB_API);
    }

	public static HostWebAPI getHostWebAPI() {
        return (HostWebAPI)getInstance(WebAPIIndex.HOST_WEB_API);
    }

	private static Object getInstance(WebAPIIndex index) {
		
		if(instance == null){
			init();
			if(instance == null){
				Logger.fatal(WebAPILocator.class, "WebAPI Locator IS NOT INITIALIZED : THIS SHOULD NEVER HAPPEN");
				throw new DotRuntimeException("WebAPI Locator IS NOT INITIALIZED : THIS SHOULD NEVER HAPPEN");
			}
		}

		Object serviceRef = instance.getServiceInstance(index);

		Logger.debug(WebAPILocator.class, instance.audit(index));

		return serviceRef;
		
	 }
	
	@Override
	protected Object createService(WebAPIIndex enumObj) {
		return enumObj.create();
	}

	@Override
	protected Locator<WebAPIIndex> getLocatorInstance() {
		return instance;
	}

}

enum WebAPIIndex
{ 
	USER_WEB_API,
	CONTENTLET_WEB_API,
	LANGUAGE_WEB_API,
	PERMISSION_WEB_API,
	HOST_WEB_API;

	Object create() {
		switch(this) {
			case USER_WEB_API: 
				return new UserWebAPIImpl();				
			
			case CONTENTLET_WEB_API: 
				return new ContentletWebAPIImpl();
				
			case LANGUAGE_WEB_API: 
				return new LanguageWebAPIImpl();
				
			case HOST_WEB_API:
				return new HostWebAPIImpl();

		}
		throw new AssertionError("Unknown API index: " + this);
	}
}
