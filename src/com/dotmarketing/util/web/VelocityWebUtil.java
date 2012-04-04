package com.dotmarketing.util.web;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.context.Context;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;

public class VelocityWebUtil {

	public static Context getVelocityContext(HttpServletRequest request, HttpServletResponse response) throws PortalException, SystemException, DotDataException, DotSecurityException{
		Context context = VelocityUtil.getWebContext(request, response); 
		return context;
	}

}