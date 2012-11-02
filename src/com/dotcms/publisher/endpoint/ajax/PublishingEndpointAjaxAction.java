package com.dotcms.publisher.endpoint.ajax;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.endpoint.business.PublisherEndpointAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class PublishingEndpointAjaxAction extends AjaxAction {

	@Override
	public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {		
		return;
	}

	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map<String, String> map = getURIParams();
		String cmd = map.get("cmd");
		Method dispatchMethod = null;
		if(null!=cmd){
			try {
				dispatchMethod = this.getClass().getMethod(cmd, new Class[]{HttpServletRequest.class, HttpServletResponse.class});
			} catch (Exception e) {
				try {
					dispatchMethod = this.getClass().getMethod("action", new Class[]{HttpServletRequest.class, HttpServletResponse.class});
				} catch (Exception e1) {
					Logger.error(this.getClass(), "Trying to get method:" + cmd);
					Logger.error(this.getClass(), e1.getMessage(), e1.getCause());
					throw new DotRuntimeException(e1.getMessage());
				}
			} 			
			try {
				dispatchMethod.invoke(this, new Object[]{request,response});
			} catch (Exception e) {
				Logger.error(this.getClass(), "Trying to invoke method:" + cmd);
				Logger.error(this.getClass(), e.getMessage(), e.getCause());
				throw new DotRuntimeException(e.getMessage());
			}			
		}
	}
	
	public void addEndpoint(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {		
        try {
        	String identifier = request.getParameter("identifier");
        	if(UtilMethods.isSet(identifier)){
        		editEndpoint(request, response);
        		return;
        	}
        	PublishingEndPoint endpoint = new PublishingEndPoint();
			endpoint.setServerName(new StringBuilder(request.getParameter("serverName")));
			endpoint.setAddress(request.getParameter("address"));
			endpoint.setPort(request.getParameter("port"));
			endpoint.setProtocol(request.getParameter("protocol"));
			endpoint.setAuthKey(new StringBuilder(PublicEncryptionFactory.encryptString(request.getParameter("authKey"))));
			endpoint.setEnabled(null!=request.getParameter("enabled"));
			endpoint.setSending("receive".equals(request.getParameter("sending")));									

			//Save the endpoint.
			PublisherEndpointAPI peAPI = APILocator.getPublisherEndpointAPI();
			peAPI.saveEndpoint(endpoint);
				
		} catch (DotDataException e) {
			response.getWriter().println("FAILURE: " + e.getMessage() );
			//throw new DotRuntimeException(e.getMessage());
		}		
	}
	
	public void editEndpoint(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {	
		try {
			PublishingEndPoint endpoint = new PublishingEndPoint();
	        String identifier = request.getParameter("identifier");
	        endpoint.setId(identifier);
			endpoint.setServerName(new StringBuilder(request.getParameter("serverName")));
			endpoint.setAddress(request.getParameter("address"));
			endpoint.setPort(request.getParameter("port"));
			endpoint.setProtocol(request.getParameter("protocol"));
			endpoint.setAuthKey(new StringBuilder(PublicEncryptionFactory.encryptString(request.getParameter("authKey"))));
			endpoint.setEnabled(null!=request.getParameter("enabled"));
			endpoint.setSending("receive".equals(request.getParameter("sending")));	
			
			//Update the endpoint.
			PublisherEndpointAPI peAPI = APILocator.getPublisherEndpointAPI();
			peAPI.updateEndpoint(endpoint);
			
		} catch (DotDataException e) {
			response.getWriter().println("FAILURE: " + e.getMessage() );
			//throw new DotRuntimeException(e.getMessage());
		} 
	}
		
}
