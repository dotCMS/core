/**
 * 
 */
package com.dotmarketing.business;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Locator is a base (template) class for Locator implementations.
 * @version 1.6
 * @since 1.6
 * @author Carlos Rivas
 *
 */
public abstract class Locator<T> {
	
	@SuppressWarnings("all")
	protected Map<T,Object> cache;
	
	protected abstract Locator<T> getLocatorInstance();
	protected abstract Object createService(T enumObj);
	
	protected Locator() {
		cache = new HashMap<T,Object>();
	}
	
	protected Object getServiceInstance(T enumObj) {
		Locator<T> instance = getLocatorInstance();
		Object serviceRef = null; 
		if (instance.cache.containsKey(enumObj)) {
			serviceRef = instance.cache.get(enumObj);
		}
		else {
			synchronized (enumObj.getClass()) {
				if (instance.cache.containsKey(enumObj)) {
					serviceRef = instance.cache.get(enumObj);
				} else {
					serviceRef = createService(enumObj);
					instance.cache.put(enumObj, serviceRef);
				}
			}

		}

		return serviceRef;
	}
	
	public String audit(T enumObj) {
//		String callerName = null;
//		Thread thread = Thread.currentThread();
//		StackTraceElement[] stackTrace = thread.getStackTrace();
//		int level = 0, invokerLevel = 0;
//		for( StackTraceElement ste : stackTrace) {
//			++level;
//			if( ste.getClassName().contains("Locator")) invokerLevel = level;
//		}
//		callerName =  stackTrace.length > invokerLevel ? String.format("%s.%s()", stackTrace[invokerLevel].getClassName(), stackTrace[invokerLevel].getMethodName()) : "unknown";
//	    
//		return String.format("Locator: Getting %s reference from '%s'. Request #%d", enumObj.toString(), callerName, auditCache.get(enumObj));
		return "";
	}
	

}
