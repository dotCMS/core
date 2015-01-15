package com.dotcms.rest;

import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.repackage.javax.ws.rs.Consumes;
import com.dotcms.repackage.javax.ws.rs.PUT;
import com.dotcms.repackage.javax.ws.rs.Path;
import com.dotcms.repackage.javax.ws.rs.PathParam;
import com.dotcms.repackage.javax.ws.rs.Produces;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * This method takes a contentlet and fires a workflow action on it. It requires
 * the parameters (id | inode) and action optionally, you can pass a language,
 * assign (roleId of the next assignee), and comments
 * 
 * @author will
 *
 */
@Path("/workflow")
public class WorkflowResource extends WebResource {

	@PUT
	@Path("/fire/{params:.*}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response fireWorkflow(@Context HttpServletRequest request, JSONObject json) throws DotDataException, DotSecurityException,
			JSONException {
		String callback = null, 
				language = null, 
				id = null, 
				inode = null, 
				wfAction = null, 
				wfAssign = null, 
				wfPublishDate = null, 
				wfExpireDate = null, 
				wfComments = null,
				whereToSend =null,
				forcePush=null;

		InitDataObject initData = init(null, true, request, false);

		if (json.has(RESTParams.CALLBACK.getValue()))
			callback = json.getString(RESTParams.CALLBACK.getValue());

		if (json.has(RESTParams.LANGUAGE.getValue()))
			language = json.getString(RESTParams.LANGUAGE.getValue());

		if (json.has(RESTParams.ID.getValue()))
			id = json.getString(RESTParams.ID.getValue());

		if (json.has(RESTParams.INODE.getValue()))
			inode = json.getString(RESTParams.INODE.getValue());

		if (json.has("wfAction"))
			wfAction = json.getString("wfAction");

		if (json.has("wfAssign"))
			wfAssign = json.getString("wfAssign");

		if (json.has("wfComments"))
			wfComments = json.getString("wfComments");

		
		if (json.has("wfPublishDate"))
			wfPublishDate = json.getString("wfPublishDate");

		if (json.has("wfExpireDate"))
			wfExpireDate = json.getString("wfExpireDate");
		
		JSONObject jo = new JSONObject();
		User user = initData.getUser();

		long lang = APILocator.getLanguageAPI().getDefaultLanguage().getId();

		if (language != null) {
			try {
				lang = Long.parseLong(language);
			} catch (Exception e) {
				Logger.warn(this.getClass(), "Invald language passed in, defaulting to, well, the default");
			}
		}

		Contentlet contentlet = (inode != null) ? APILocator.getContentletAPI().find(inode, user, false) : APILocator.getContentletAPI()
				.findContentletByIdentifier(id, false, lang, user, false);

		if (contentlet == null || contentlet.getIdentifier() == null) {
			jo.append("message", "contentlet not found");
			jo.append("return", 404);
			Response.ResponseBuilder responseBuilder = Response.status(HttpStatus.SC_NOT_FOUND);
			return responseBuilder.entity(jo).build();
		}

		WorkflowAPI wapi = APILocator.getWorkflowAPI();
		WorkflowAction action = null;
		try {
			action = wapi.findAction(wfAction, user);
			if (action == null) {
				throw new ServletException("No such workflow action");
			}
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			jo.append("message", "error:" + e.getMessage());
			jo.append("return", 500);

			Response.ResponseBuilder responseBuilder = Response.status(HttpStatus.SC_FAILED_DEPENDENCY);
			return responseBuilder.entity(jo).build();

		}

		try {
			if (action.requiresCheckout()) {

				Contentlet c = APILocator.getContentletAPI().checkout(contentlet.getInode(), user, false);

				c.setStringProperty("wfActionId", action.getId());
				c.setStringProperty("wfActionComments", wfComments);
				c.setStringProperty("wfActionAssign", wfAssign);

				contentlet = APILocator.getContentletAPI().checkin(c, user, false);
			} else {
				contentlet.setStringProperty("wfActionId", action.getId());
				contentlet.setStringProperty("wfActionComments", wfComments);
				contentlet.setStringProperty("wfActionAssign", wfAssign);
				wapi.fireWorkflowNoCheckin(contentlet, user);
			}
			if (UtilMethods.isSet(callback)) {
				jo.put("callback", callback);
			}
			jo.put("inode", contentlet.getInode());
			jo.put("id", contentlet.getIdentifier());
			jo.put("message", "workflow action fired");
			try{
				jo.put("locked", contentlet.isLocked());
				jo.put("live", contentlet.isLive());
				jo.put("archived", contentlet.isArchived());
			}
			catch(NullPointerException npe){
				Logger.debug(this.getClass(), npe.getMessage(), npe);
			}
			jo.put("return", 200);
			
			
			
		} catch (Exception e) {
			if (UtilMethods.isSet(callback)) {
				jo.put("callback", callback);
			}
			jo.put("inode", contentlet.getInode());
			jo.put("id", contentlet.getIdentifier());
			Logger.error(this.getClass(), e.getMessage(), e);
			jo.put("message", "workflow action error" + e.getMessage());
			jo.put("return", 500);
			Response.ResponseBuilder responseBuilder = Response.status(HttpStatus.SC_BAD_REQUEST);
			return responseBuilder.entity(jo).build();
		}

		Response.ResponseBuilder responseBuilder = Response.status(HttpStatus.SC_OK);
		return responseBuilder.entity(jo).build();

	}

	class WorkFlowExecutor {
		String callback, language, id, inode, wfAction, wfAssign, wfComments;

		public String getCallback() {
			return callback;
		}

		public void setCallback(String callback) {
			this.callback = callback;
		}

		public String getLanguage() {
			return language;
		}

		public void setLanguage(String language) {
			this.language = language;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getInode() {
			return inode;
		}

		public void setInode(String inode) {
			this.inode = inode;
		}

		public String getWfAction() {
			return wfAction;
		}

		public void setWfAction(String wfAction) {
			this.wfAction = wfAction;
		}

		public String getWfAssign() {
			return wfAssign;
		}

		public void setWfAssign(String wfAssign) {
			this.wfAssign = wfAssign;
		}

		public String getWfComments() {
			return wfComments;
		}

		public void setWfComments(String wfComments) {
			this.wfComments = wfComments;
		}

	}

	class WorkFlowResult {
		String language, id, inode, message;
		int status = 200;

	}

}
