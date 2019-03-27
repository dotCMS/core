package com.dotmarketing.business;

import java.util.List;

/**
 * 
 * @author Jason Tesser
 *
 */
public abstract class LayoutCache implements Cachable {

	abstract protected Layout add(String key,Layout layout);

	abstract protected Layout get(String key);
	
	abstract public void clearCache();


	abstract protected void remove(Layout layout) ;
	abstract protected List<String> getPortlets(Layout layout) ;
	abstract protected List<String> addPortlets(Layout layout, List<String> portletIds) ;

}
