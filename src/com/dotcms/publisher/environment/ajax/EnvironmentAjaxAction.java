package com.dotcms.publisher.environment.ajax;

import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publisher.environment.business.EnvironmentAPI;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.cms.login.factories.LoginFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.servlets.ajax.AjaxAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.language.LanguageException;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EnvironmentAjaxAction extends AjaxAction {

	@Override
	public void action(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		return;
	}

	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map<String, String> map = getURIParams();
		String cmd = map.get("cmd");
		Method dispatchMethod = null;

		User user = getUser();

		try{
			// Check permissions if the user has access to the CMS Maintenance Portlet
			if (user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", user)) {
				String userName = map.get("u") !=null
					? map.get("u")
						: map.get("user") !=null
							? map.get("user")
								: null;

				String password = map.get("p") !=null
					? map.get("p")
							: map.get("passwd") !=null
								? map.get("passwd")
									: null;



				LoginFactory.doLogin(userName, password, false, request, response);
				user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
				if(user==null) {
				    setUser(request);
	                user = getUser();
				}
				if(user==null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CONTENT_PUBLISHING_TOOL", user)){
					response.sendError(401);
					return;
				}
			}
		}
		catch(Exception e){
			Logger.error(this.getClass(), e.getMessage());
			response.sendError(401);
			return;
		}

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

	public void addEnvironment(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, LanguageException, DotSecurityException {
        try {
        	String identifier = request.getParameter("identifier");
        	if(UtilMethods.isSet(identifier)){
        		editEnvironment(request, response);
        		return;
        	}

        	String name = request.getParameter("environmentName");

        	Environment existingEnv = APILocator.getEnvironmentAPI().findEnvironmentByName(name);

        	if(existingEnv!=null) {
        		Logger.info(getClass(), "Can't save Environment. An Environment with the given name already exists. ");
        		User user = getUser();
    			response.getWriter().println("FAILURE: " + LanguageUtil.get(user, "publisher_Environment_name_exists"));
    			return;
        	}

        	String whoCanUseTmp = request.getParameter("whoCanUse");

        	Environment environment = new Environment();
        	environment.setName(name);
        	environment.setPushToAll("pushToAll".equals(request.getParameter("pushType")));

        	List<String> whoCanUse = Arrays.asList(whoCanUseTmp.split(","));
        	List<Permission> permissions = new ArrayList<Permission>();

			for (String perm : whoCanUse) {
				if(!UtilMethods.isSet(perm)){
					continue;
				}

				Role test = resolveRole(perm);
				Permission p = new Permission(environment.getPermissionType(), environment.getId(), test.getId(), PermissionAPI.PERMISSION_USE);

				boolean exists=false;
				for(Permission curr : permissions)
				    exists=exists || curr.getRoleId().equals(p.getRoleId());

				if(!exists)
				    permissions.add(p);
			}


        	EnvironmentAPI eAPI = APILocator.getEnvironmentAPI();
			eAPI.saveEnvironment(environment, permissions);

		} catch (DotDataException e) {
			Logger.info(getClass(), e.getMessage());
			throw new DotRuntimeException(e.getMessage());
		}
	}

	public void editEnvironment(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, LanguageException, DotSecurityException {

		try {

            //Reading the parameters
	        String identifier = request.getParameter("identifier");
	        String name = request.getParameter("environmentName");

            //Verify the environment exist
            Environment existingEnv = APILocator.getEnvironmentAPI().findEnvironmentByName( name );
            if ( existingEnv != null && !existingEnv.getId().equals( identifier ) ) {
                Logger.info( getClass(), "Can't save Environment. An Environment with the given name already exists. " );
                User user = getUser();
                response.getWriter().println( "FAILURE: " + LanguageUtil.get( user, "publisher_Environment_name_exists" ) );
                return;
            }

        	String whoCanUseTmp = request.getParameter("whoCanUse");

	        Environment environment = new Environment();
	        environment.setId(identifier);
        	environment.setName(name);
        	environment.setPushToAll("pushToAll".equals(request.getParameter("pushType")));

        	List<String> whoCanUse = Arrays.asList(whoCanUseTmp.split(","));
        	List<Permission> permissions = new ArrayList<Permission>();

			for (String perm : whoCanUse) {
				if(!UtilMethods.isSet(perm)){
					continue;
				}

				Role test = resolveRole(perm);
				Permission p = new Permission(environment.getPermissionType(), environment.getId(), test.getId(), PermissionAPI.PERMISSION_USE);

				boolean exists=false;
				for(Permission curr : permissions)
				    exists=exists || curr.getRoleId().equals(p.getRoleId());

				if(!exists)
				    permissions.add(p);
			}

        	EnvironmentAPI eAPI = APILocator.getEnvironmentAPI();
			eAPI.updateEnvironment(environment, permissions);

            //If it was updated successfully lets set the session
            if ( UtilMethods.isSet( request.getSession().getAttribute( WebKeys.SELECTED_ENVIRONMENTS ) ) ) {

                //Get the selected environments from the session
                List<Environment> lastSelectedEnvironments = (List<Environment>) request.getSession().getAttribute( WebKeys.SELECTED_ENVIRONMENTS );

                Integer indexToReplace = null;
                for ( Environment currentEnv : lastSelectedEnvironments ) {
                    //Verify if the current env is on the ones stored in session
                    if ( currentEnv.getId().equals( environment.getId() ) ) {
                        indexToReplace = lastSelectedEnvironments.indexOf( currentEnv );
                    }
                }

                //If we found it lets use the updated
                if ( indexToReplace != null ) {
                    lastSelectedEnvironments.set( indexToReplace, environment );
                }
            }

		} catch (DotDataException e) {
			Logger.info(getClass(), e.getMessage());
			throw new DotRuntimeException(e.getMessage());
		}
	}

	private Role resolveRole(String id) throws DotDataException{
		Role test = null;

		String newid = id.substring(id.indexOf("-") + 1, id.length());

		if(id.startsWith("user-")){
			test = APILocator.getRoleAPI().loadRoleByKey(newid);
		}
		else if(id.startsWith("role-")){
			test = APILocator.getRoleAPI().loadRoleById(newid);
		}else{
			test = APILocator.getRoleAPI().loadRoleById(id);
		}
		return test;

	}

}
