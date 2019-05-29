package com.dotcms.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicyProvider;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This method takes a contentlet and fires a workflow action on it. It requires
 * the parameters (id | inode) and action optionally, you can pass a language,
 * assign (roleId of the next assignee), and comments
 *
 * @See com.dotcms.rest.api.v1.workflow.WorkflowResource
 * @Deprecated
 * @author will
 *
 */
@Deprecated
@Path("/workflow")
public class WorkflowResource {

    private final WebResource webResource = new WebResource();

    @Deprecated
    @PUT
	@Path("/fire/{params:.*}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response fireWorkflow(@Context HttpServletRequest request, @Context final HttpServletResponse response,
			String json) throws JsonProcessingException, IOException, DotDataException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonParams = mapper.readTree(json);
        String callback = null, 
                language = null, 
                id = null, 
                inode = null, 
                wfAction = null, 
                wfAssign = null, 
                wfComments = null,
                wfPublishDate = null, 
                wfPublishTime = null, 
                wfExpireDate = null,
                wfExpireTime = null,
                wfNeverExpire=null,
                whereToSend = null,
                forcePush = null;

        InitDataObject initData = webResource.init(null, request, response, false, null);

		if (jsonParams.has(RESTParams.CALLBACK.getValue())) {
			callback = jsonParams.get(RESTParams.CALLBACK.getValue()).asText();
		}
		if (jsonParams.has(RESTParams.LANGUAGE.getValue())) {
			language = jsonParams.get(RESTParams.LANGUAGE.getValue()).asText();
		}
		if (jsonParams.has(RESTParams.ID.getValue())) {
			id = jsonParams.get(RESTParams.ID.getValue()).asText();
		}
		if (jsonParams.has(RESTParams.INODE.getValue())) {
			inode = jsonParams.get(RESTParams.INODE.getValue()).asText();
		}
		if (jsonParams.has("wfAction")) {
			wfAction = jsonParams.get("wfAction").asText();
		}
		if (jsonParams.has("wfAssign")) {
			wfAssign = jsonParams.get("wfAssign").asText();
		}
		if (jsonParams.has("wfComments")) {
			wfComments = jsonParams.get("wfComments").asText();
		}
		if (jsonParams.has("wfPublishDate")) {
		    wfPublishDate = jsonParams.get("wfPublishDate").asText();
		}
		if (jsonParams.has("wfPublishTime")) {
		    wfPublishTime = jsonParams.get("wfPublishTime").asText();
		}
		if (jsonParams.has("wfExpireDate")) {
		    wfExpireDate = jsonParams.get("wfExpireDate").asText();
		}
		if (jsonParams.has("wfExpireTime")) {
		    wfExpireTime = jsonParams.get("wfExpireTime").asText();
		}
		if (jsonParams.has("wfNeverExpire")) {
		    wfNeverExpire = jsonParams.get("wfNeverExpire").asText();
		}
		if (jsonParams.has("whereToSend")) {
		    whereToSend = jsonParams.get("whereToSend").asText();
		}
		if (jsonParams.has("forcePush")) {
		    forcePush = jsonParams.get("forcePush").asText();
		}
	        
		ObjectNode jsonResponse = JsonNodeFactory.instance.objectNode();
		User user = initData.getUser();

		long lang = APILocator.getLanguageAPI().getDefaultLanguage().getId();

		if (language != null) {
			try {
				lang = Long.parseLong(language);
			} catch (Exception e) {
				Logger.warn(this.getClass(), "Invald language passed in, defaulting to, well, the default");
			}
		}

		Contentlet contentlet = null;
		try {
			contentlet =
					(inode != null) ? APILocator.getContentletAPI().find(inode, user, false)
							: APILocator.getContentletAPI()
									.findContentletByIdentifier(id, false, lang, user, false);
		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);
		}

		if (contentlet == null || contentlet.getIdentifier() == null) {
			jsonResponse.put("message", "contentlet not found");
			jsonResponse.put("return", 404);
			Response.ResponseBuilder responseBuilder = Response.status(HttpStatus.SC_NOT_FOUND);
			return responseBuilder.entity(jsonResponse).build();
		}

		WorkflowAPI wapi = APILocator.getWorkflowAPI();
		WorkflowAction action = null;
		try {
			action = wapi.findActionRespectingPermissions(wfAction, contentlet, user);
			if (action == null) {
				throw new ServletException("No such workflow action");
			}
		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			jsonResponse.put("message", "error:" + e.getMessage());
			jsonResponse.put("return", 500);

			Response.ResponseBuilder responseBuilder = Response.status(HttpStatus.SC_FAILED_DEPENDENCY);
			return responseBuilder.entity(jsonResponse).build();

		}

		try {

			contentlet.setStringProperty("wfActionId", action.getId());
			contentlet.setStringProperty("wfActionComments", wfComments);
			contentlet.setStringProperty("wfActionAssign", wfAssign);
			contentlet.setStringProperty("wfPublishDate", wfPublishDate);
			contentlet.setStringProperty("wfPublishTime", wfPublishTime);
			contentlet.setStringProperty("wfExpireDate", wfExpireDate);
			contentlet.setStringProperty("wfExpireTime", wfExpireTime);
			contentlet.setStringProperty("wfNeverExpire", wfNeverExpire);
			contentlet.setStringProperty("whereToSend", whereToSend);
			contentlet.setStringProperty("forcePush", forcePush);
			contentlet.setIndexPolicy(IndexPolicyProvider.getInstance().forSingleContent());
			contentlet = APILocator.getWorkflowAPI().fireContentWorkflow(contentlet,
					new ContentletDependencies.Builder()
							.respectAnonymousPermissions(PageMode.get(request).respectAnonPerms)
							.modUser(user).build());


			if (UtilMethods.isSet(callback)) {
				jsonResponse.put("callback", callback);
			}
			jsonResponse.put("inode", contentlet.getInode());
			jsonResponse.put("id", contentlet.getIdentifier());
			jsonResponse.put("message", "workflow action fired");
			try{
				jsonResponse.put("locked", contentlet.isLocked());
				jsonResponse.put("live", contentlet.isLive());
				jsonResponse.put("archived", contentlet.isArchived());
			}
			catch(NullPointerException npe){
				Logger.debug(this.getClass(), npe.getMessage(), npe);
			}
			jsonResponse.put("return", 200);

		} catch (DotSecurityException e) {
			throw new ForbiddenException(e);
			
		} catch (Exception e) {
			if (UtilMethods.isSet(callback)) {
				jsonResponse.put("callback", callback);
			}
			jsonResponse.put("inode", contentlet.getInode());
			jsonResponse.put("id", contentlet.getIdentifier());
			Logger.error(this.getClass(), e.getMessage(), e);
			jsonResponse.put("message", "workflow action error" + e.getMessage());
			jsonResponse.put("return", 500);
			Response.ResponseBuilder responseBuilder = Response.status(HttpStatus.SC_BAD_REQUEST);
			return responseBuilder.entity(jsonResponse).build();
		}

		Response.ResponseBuilder responseBuilder = Response.status(HttpStatus.SC_OK);
		return responseBuilder.entity(jsonResponse).build();
		
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
