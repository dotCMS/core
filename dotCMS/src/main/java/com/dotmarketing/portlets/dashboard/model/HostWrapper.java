package com.dotmarketing.portlets.dashboard.model;


import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;



public class HostWrapper extends Host{
	

	private static final long serialVersionUID = 1L;

	private long pageViews;
	
	private long pageViewsDiff;
	
	private Map<String, Object> contentletMap = new HashMap<String, Object>(); 
	

	public HostWrapper(Host host, long pageViews) throws IllegalAccessException, InvocationTargetException{
		BeanUtils.copyProperties(this, host);
		this.pageViews = pageViews;
	}
	
	public HostWrapper(Host host, long pageViews, long pageViewsDiff) throws IllegalAccessException, InvocationTargetException{
		BeanUtils.copyProperties(this, host);
		this.pageViews = pageViews;
		this.pageViewsDiff = pageViewsDiff;
	}
	
	public HostWrapper(final Contentlet contentlet, final long pageViews, final long pageViewsDiff)
            throws InvocationTargetException, IllegalAccessException {
        BeanUtils.copyProperties(this, contentlet);
		this.contentletMap = contentlet.getMap();
		this.pageViews = pageViews;
		this.pageViewsDiff = pageViewsDiff;
	}


	public long getPageViews() {
		return pageViews;
	}
	public void setPageViews(long pageViews) {
		this.pageViews = pageViews;
	}

	public void setPageViewsDiff(long pageViewsDiff) {
		this.pageViewsDiff = pageViewsDiff;
	}

	public long getPageViewsDiff() {
		return pageViewsDiff;
	}

	public Map<String, Object> getContentletMap() {
		return contentletMap;
	}

	public void setContentletMap(Map<String, Object> map) {
		this.contentletMap = map;
	}

  
	
   
	
	
}
