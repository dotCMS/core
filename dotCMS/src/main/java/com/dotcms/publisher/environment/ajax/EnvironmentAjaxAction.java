package com.dotcms.publisher.environment.ajax;

import com.dotcms.auth.BasicAuthentication;
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
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.SecurityLogger;
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
			if (user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("maintenance", user)) {
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



				// Only the compatibility mode may exchange URI credentials for a session.
				if (BasicAuthentication.mode() == BasicAuthentication.Mode.ALL) {
					LoginFactory.doLogin(userName, password, false, request, response);
				}
				user = (User) request.getSession().getAttribute(WebKeys.CMS_USER);
				if(user==null) {
				    setUser(request);
	                user = getUser();
				}
				if(user==null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("publishing-queue", user)){
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
				throw new DotRuntimeException(e.getMessage(),e);
			}
		}
	}

	/**
	 * Creates a publishing environment, or edits it when an identifier is sent. Only users allowed
	 * by {@link #hasManagementAccess(User)} can do it; any other user gets a 403.
	 *
	 * @param request  The current instance of the {@link HttpServletRequest}.
	 * @param response The current instance of the {@link HttpServletResponse}.
	 *
	 * @throws ServletException   An error occurred while handling the request.
	 * @throws IOException        An error occurred while writing the response.
	 * @throws LanguageException  A message could not be resolved.
	 * @throws DotSecurityException A permission check failed.
	 */
	public void addEnvironment(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, LanguageException, DotSecurityException {
		if (!hasManagementAccess(getUser())) {
			rejectManagementRequest(response);
			return;
		}

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
        	List<Permission> permissions = new ArrayList<>();

			for (String perm : whoCanUse) {
				if(!UtilMethods.isSet(perm)){
					continue;
				}

				Role test = resolveRole(perm);
				Permission p = new Permission(environment.getId(), test.getId(), PermissionAPI.PERMISSION_USE);

				boolean exists=false;
				for(Permission curr : permissions)
				    exists=exists || curr.getRoleId().equals(p.getRoleId());

				if(!exists)
				    permissions.add(p);
			}


        	EnvironmentAPI eAPI = APILocator.getEnvironmentAPI();
			eAPI.saveEnvironment(environment, permissions);

			response.getWriter().write(environment.getId());
		} catch (DotDataException e) {
			Logger.info(getClass(), e.getMessage());
			throw new DotRuntimeException(e.getMessage(),e);
		}
	}

	/**
	 * Updates a publishing environment. Only users allowed by {@link #hasManagementAccess(User)}
	 * can do it; any other user gets a 403.
	 *
	 * @param request  The current instance of the {@link HttpServletRequest}.
	 * @param response The current instance of the {@link HttpServletResponse}.
	 *
	 * @throws ServletException   An error occurred while handling the request.
	 * @throws IOException        An error occurred while writing the response.
	 * @throws LanguageException  A message could not be resolved.
	 * @throws DotSecurityException A permission check failed.
	 */
	public void editEnvironment(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, LanguageException, DotSecurityException {
		if (!hasManagementAccess(getUser())) {
			rejectManagementRequest(response);
			return;
		}


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
        	List<Permission> permissions = new ArrayList<>();

			for (String perm : whoCanUse) {
				if(!UtilMethods.isSet(perm)){
					continue;
				}

				Role test = resolveRole(perm);
				Permission p = new Permission(environment.getId(), test.getId(), PermissionAPI.PERMISSION_USE);

				boolean exists=false;
				for(Permission curr : permissions)
				    exists=exists || curr.getRoleId().equals(p.getRoleId());

				if(!exists)
				    permissions.add(p);
			}

        	EnvironmentAPI eAPI = APILocator.getEnvironmentAPI();
			eAPI.updateEnvironment(environment, permissions);

            //If it was updated successfully lets set the session
            if ( UtilMethods.isSet( request.getSession().getAttribute( WebKeys.SELECTED_ENVIRONMENTS + getUser().getUserId() ) ) ) {

                //Get the selected environments from the session
                List<Environment> lastSelectedEnvironments = (List<Environment>) request.getSession().getAttribute( WebKeys.SELECTED_ENVIRONMENTS + getUser().getUserId() );

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
			throw new DotRuntimeException(e.getMessage(),e);
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

	/**
	 * Tells whether a user can create or modify publishing environments: an administrator, or a user with
	 * access to the Configuration portlet, the same rule the REST endpoint resource applies.
	 *
	 * @param user The user making the request, which can be {@code null}.
	 *
	 * @return {@code true} if the user is allowed to make the change.
	 */
	private boolean hasManagementAccess(final User user) {
		if (user == null) {
			return false;
		}
		try {
			return user.isAdmin() || APILocator.getLayoutAPI()
					.doesUserHaveAccessToPortlet(PortletID.CONFIGURATION.toString(), user);
		} catch (DotDataException e) {
			Logger.error(this, "Unable to check the portlet access of the user: " + e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Answers a request that the user is not allowed to make with a 403 and leaves a log entry
	 * with the user id and nothing else from the request.
	 *
	 * @param response The current instance of the {@link HttpServletResponse}.
	 *
	 * @throws IOException The response could not be written.
	 */
	private void rejectManagementRequest(final HttpServletResponse response) throws IOException {
		final User user = getUser();
		SecurityLogger.logWarn(getClass(), () -> "Rejected a publishing environment change from a user without management access, userId: "
				+ (user != null ? user.getUserId() : "unknown"));
		response.sendError(HttpServletResponse.SC_FORBIDDEN);
	}

}
