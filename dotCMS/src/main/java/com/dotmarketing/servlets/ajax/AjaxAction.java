package com.dotmarketing.servlets.ajax;

import com.dotcms.auth.BasicAuthentication;
import com.dotcms.rest.AnonymousAccess;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AjaxAction {
	User user;
	HttpServletRequest request;
	HttpServletResponse response;
	Map<String, String> params;
    private WebResource webResource;

	public AjaxAction() {
		this(new WebResource());
	}

	@VisibleForTesting
	public AjaxAction(final WebResource webResource) {
		this.webResource = webResource;
	}

	public void setWebResource(final WebResource webResource) {
		this.webResource = webResource;
	}

	/**
	 * Authenticates before dispatch, resolving explicitly enabled legacy aliases before the strict
	 * user check. The normal SSL, authentication and action permission checks remain in place.
	 *
	 * @param request incoming request
	 * @param response outgoing response
	 */
	public void init(final HttpServletRequest request, final HttpServletResponse response){
        if (BasicAuthentication.allowUrlCredentials()
                && BasicAuthentication.mode() == BasicAuthentication.Mode.ALL) {
            setURIParams(request);
            final String username = params.getOrDefault("u", params.get("user"));
            final String password = params.getOrDefault("p", params.get("passwd"));
            if (UtilMethods.isSet(username) && UtilMethods.isSet(password)) {
                final Map<String, String> credentials = new HashMap<>(params);
                credentials.put("user", username);
                credentials.put("password", password);
                this.user = webResource.initWithMap(credentials, request, response,
                        AnonymousAccess.NONE, null).getUser();
                return;
            }
        }
		setUser( request);
		setURIParams(request);
	}
	
	/**
	 * default action gets run if the "cmd" parameter is not specified in the request
	 * otherwise, the service method will try to run the method named "$cmd"
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	abstract public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException;
	
	
	
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		Logger.warn(this.getClass(), "Request being dropped, there is no method to service " + request.getMethod() + "s");
	};

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		service(request, response);
	};

	public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		service(request, response);
	};

	public void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		service(request, response);
	};

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		service(request, response);
	};

	public User getUser(){
		
		return user;
	}

    public void setUser(User user){
        this.user = user;
    }

	public void setUser(HttpServletRequest request){
		final InitDataObject initData = webResource.init(request.getRequestURI(), true, request, true, null);
	    this.user = initData.getUser();
	}
	
	
	public Map<String, String> getURIParams(){
		
		return this.params;
	}
	public void setURIParams(HttpServletRequest request){
		String url = request.getRequestURI().toString();
		url = (url.startsWith("/")) ? url.substring(1, url.length()) : url;
		String p[] = url.split("/");
		Map<String, String> map = new HashMap<>();
		
		String key =null;
		for(String x : p){
			if(key ==null){
				key = x;
			}
			else{
				map.put(key, x);
				key = null;
			}
		}
		
		Enumeration parameters=request.getParameterNames();
		while(parameters.hasMoreElements()) {
		    String par = (String)parameters.nextElement();
		    map.put(par, request.getParameter(par));
		}
		
		this.params = map;
		
	}
	
	
}
