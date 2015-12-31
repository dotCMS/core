package com.dotmarketing.viewtools;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.util.Logger;
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
				ADMIN_MODE = !timemachine && session != null
						&& (session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
				try {
					user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
				} catch (DotRuntimeException | PortalException | SystemException e) {
					Logger.error(this.getClass(), e.getMessage());
				}
			}
		}
	}
	
	public Persona persona() throws DotDataException, DotSecurityException {
		if(ADMIN_MODE){
			if(request.getParameter(WebKeys.CMS_PERSONA_ID)!=null){
				return find(request.getParameter(WebKeys.CMS_PERSONA_ID));
			}
		}
		
		Optional<Visitor> v = APILocator.getVisitorAPI().getVisitor(request);
		Visitor visitor = (v.isPresent()) ? v.get() : new Visitor();
		if (visitor.getPersona() != null) {
			return (Persona) visitor.getPersona();
		}
		return null;
	}
	
	
	
	
	public Persona find(String id) throws DotDataException, DotSecurityException {
		
		if(!UtilMethods.isSet(id)){
			return null;
		}

		return APILocator.getPersonaAPI().find(id, user, false);

	}

}