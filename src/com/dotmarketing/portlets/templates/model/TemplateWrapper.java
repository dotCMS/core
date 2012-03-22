package com.dotmarketing.portlets.templates.model;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;

public class TemplateWrapper extends Template{
	

	private static final long serialVersionUID = 1L;

	private Host host;
	

	public TemplateWrapper(Template template, String hostInode) throws IllegalAccessException, InvocationTargetException, DotDataException, DotSecurityException {
		BeanUtils.copyProperties(this, template);
		if(UtilMethods.isSet(hostInode)){
			this.host = APILocator.getHostAPI().find(hostInode, APILocator.getUserAPI().getSystemUser(), false);
		}

	}

	public Host getHost() {
		return host;
	}

	public void setHost(Host host) {
		this.host = host;
	}
   
	
	
}
