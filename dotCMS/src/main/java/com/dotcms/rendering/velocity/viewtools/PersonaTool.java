package com.dotcms.rendering.velocity.viewtools;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotcms.repackage.bsh.util.Util;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public class PersonaTool implements ViewTool {

	private HttpServletRequest request = null;

	boolean ADMIN_MODE = false;
	boolean timemachine = false;
	User user = null;
	
	
	public void init(Object obj) {
		if (obj instanceof ViewContext) {
			request = ((ViewContext) obj).getRequest();
			HttpSession session = request.getSession(false);

			if (session != null) {
				timemachine = session.getAttribute("tm_date") != null;
		        boolean ADMIN_MODE =   PageMode.get(request) .isAdmin;
				try {
					user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
				} catch (DotRuntimeException e) {
					Logger.error(this.getClass(), e.getMessage());
				}
			}
		}
	}
	

	/**
	 * Forces a specific persona into the visitor object.  If the persona is not found, 
	 * it will rm the persona from the visitor object
	 * @param id
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public Persona forcePersona(String id)  {

		if(!UtilMethods.isSet(id)){
			return null;
		}
		
		Persona persona;
		Optional<Visitor> visitor = APILocator.getVisitorAPI().getVisitor(request, true);
		try {
			persona = find(id);
			visitor.get().setPersona(persona);
		} catch (Exception  e) {
			Logger.debug(this.getClass(), e.getMessage());
			visitor.get().setPersona(null);
			return null;
		}
		
		
		
		return persona;
	}
	
	
	
	
	
	
	/**
	 * Permission based find method
	 * @param id
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public Persona find(String id) throws DotDataException, DotSecurityException {
		
		if(!UtilMethods.isSet(id)){
			return null;
		}

		return APILocator.getPersonaAPI().find(id, user, true);

	}

	/**
	 * gets personas on both the host being viewed and the system host (global personas)
	 * @return
	 */
	public List<Persona> getPersonas(){
		
		
		try {
			
			Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
			List<Persona> personas = APILocator.getPersonaAPI().getPersonas(host, false, false, user, true);

			return personas;
		} catch (DotDataException | DotSecurityException | PortalException | SystemException e) {
			Logger.error(this.getClass(), e.getMessage());
		}
		return null;
		
	}
	
	
	
	
	
}