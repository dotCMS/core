package com.dotmarketing.cmis.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.Cookie;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;

//http://jira.dotmarketing.net/browse/DOTCMS-3392
public class DotInvocationHandler implements InvocationHandler{
    private final Map map;
    
    public DotInvocationHandler(Map map) {
        this.map=map;
    }
    
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		/*
		 * Right now the interfaces handled are
		 * 
		 * 1. HttpServletRequest
		 * 2. HttpServletResponse
		 * 3. HttpSession
		 * 
		 * And the method calls to the above interfaces handled here are
		 * 
		 * 1. get,getAttribute,getParameter
		 * 2. put,setAttribute
		 * 3. getSession,session
		 * 4. addCookie
		 * 5. getCookies
		 * 6. getLocale
		 * 7. getServerName
		 * 8. getRequestURI
		 * 9. getRequestURL // This need to be corrected.
		 * 10. getParameterNames
		 * 11. getQueryString
		 * 12. getServerPort
		 * 
		 *  ** If other methods are invoked, returns a new java.lang.Object instance. 
		 *  
		 *  TODO Code Clean-up and Commenting.
		 */

		String methodName = method.getName();		
		
		if((methodName.equalsIgnoreCase("get") 
				|| method.getName().equalsIgnoreCase("getAttribute")
				|| method.getName().equalsIgnoreCase("getParameter")
				|| method.getName().equalsIgnoreCase("getHeader"))
				&& args != null 
				&& args[0]!= null){	
			return get(map,(String) args[0]);
		}

		if(methodName.equalsIgnoreCase("getParameterNames")){	
			return new StringTokenizer("","");
		}
		
		if(methodName.equalsIgnoreCase("getQueryString")){	
			return new String("");
		}
		
		if(methodName.equalsIgnoreCase("getServerPort")){	
			return new Integer(80);
		}
		
		if((methodName.equalsIgnoreCase("put") 
				|| method.getName().equalsIgnoreCase("setAttribute"))
				&& args != null 
				&& args[0]!= null){	
			return map.put((String)args[0],args[1]);
		}
		
		if(methodName.equalsIgnoreCase("getSession") || method.getName().equalsIgnoreCase("session")){
			Map sessionMap=(Map)map.get("___session__map");
			if(sessionMap==null) {
			    sessionMap=new HashMap();
			    map.put("___session__map", sessionMap);
			}
			    
			InvocationHandler ih =  new DotInvocationHandler(sessionMap);
			
			DotSessionProxy session = (DotSessionProxy) Proxy.newProxyInstance(DotSessionProxy.class.getClassLoader(),
	                new Class[] { DotSessionProxy.class },
	                ih);
			
			return session;
			
		}	
		
		if((methodName.equalsIgnoreCase("addCookie"))
				&& args != null 
				&& args[0]!= null){	
			return map.put("cookie",args[0]);
		}
		
		if((methodName.equalsIgnoreCase("getCookies"))){					
			return getCookies();
		}
		
		if((methodName.equalsIgnoreCase("getLocale"))){					
			return Locale.getDefault();
		}
		
		if((methodName.equalsIgnoreCase("getServerName"))){					
			com.liferay.portal.model.User user = (User) map.get("user");
			Host host = (Host) map.get("host");
			return host.getHostname();
		}
		
		if((methodName.equalsIgnoreCase("getRequestURI"))){					
			return (String)map.get("uri");
		}

		if((methodName.equalsIgnoreCase("getRequestURL"))){
			return new StringBuffer("http://localhost/cmis/file/").append((String)map.get("uri"));			 
		}

		return new Object();
	}
	
	public Cookie[] getCookies(){
		
		Cookie dmidCookie = new Cookie("starter.dotcms.org",UUIDGenerator.generateUuid());
		Cookie jsessionIdCookie = new Cookie("starter.dotcms.org",UUIDGenerator.generateUuid());
		Cookie login = new Cookie("starter.dotcms.org",UUIDGenerator.generateUuid());
		Cookie[] cookies = {dmidCookie,jsessionIdCookie,login};
		return cookies;
	}
	
	
	public Object get(Map proxy, String key){

		// LANG
		 if(key.equalsIgnoreCase(WebKeys.HTMLPAGE_LANGUAGE) && !proxy.containsKey(key))
			return Long.toString(APILocator.getLanguageAPI().getDefaultLanguage().getId());
					
		//COOKIE
		if(key.equalsIgnoreCase("cookies"))
			return getCookies();		
		
		//HOST_ID , DEFAULT HOST
		if(key.equalsIgnoreCase("host_id")){
			com.liferay.portal.model.User user = (User) proxy.get("user");
			Host host = (Host) proxy.get("host");
			return host.getHost();
		}
		
		//USER test@dotcms.org
		if(key.equalsIgnoreCase(com.dotmarketing.util.WebKeys.CMS_USER)){				
			com.liferay.portal.model.User user = (User) proxy.get("user");
			return user;
		}		
		
		return proxy.get((String)key);		
	}
}
