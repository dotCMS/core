<%@page import="com.dotmarketing.portlets.workflows.actionlet.PushPublishActionlet"%>
<%@page import="com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.DateUtil"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.portlets.workflows.model.*"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%

if(user == null){
	return;	
}
boolean isUserCMSAdmin = APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole());
boolean isHost = ("Host".equals(structure.getVelocityVarName()));

boolean isContLocked=(request.getParameter("sibbling") != null) ? false : contentlet.isLocked();

WorkflowScheme scheme = APILocator.getWorkflowAPI().findSchemeForStruct(structure);
WorkflowTask wfTask = APILocator.getWorkflowAPI().findTaskByContentlet(contentlet); 

WorkflowStep wfStep = null;
List<WorkflowAction> wfActions = null;
List<WorkflowAction> wfActionsAll = null;
try{
	wfStep = APILocator.getWorkflowAPI().findStepByContentlet(contentlet); 
	wfActions = APILocator.getWorkflowAPI().findAvailableActions(contentlet, user); 
	wfActionsAll= APILocator.getWorkflowAPI().findActions(wfStep, user); 
}
catch(Exception e){
	wfActions = new ArrayList();
}


%>

<%--check permissions to display the save and publish button or not--%> 
<%boolean canUserWriteToContentlet = conPerAPI.doesUserHavePermission(contentlet,PermissionAPI.PERMISSION_WRITE,user);%> 






<%if(isContLocked && (contentEditable || isUserCMSAdmin)) {%>

		<%if(contentEditable){ %>
		    <a onClick="unlockContent('<%=contentlet.getInode() %>');" id="unlockContentButton">
				<span class="unlockIcon"></span>
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Release-Lock")) %>
			</a>
		<%}else{ %>
		    <a onClick="stealLock('<%=contentlet.getInode() %>');" id="stealLockContentButton">
				<span class="unlockIcon"></span>
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Steal-Lock")) %>
			</a>
		<%} %>


<%} %>



<%if ((InodeUtils.isSet(contentlet.getInode())) && (canUserWriteToContentlet) && (!contentlet.isArchived()) && isContLocked && contentEditable) { %> 
	<%if (!InodeUtils.isSet(contentlet.getInode()) || contentlet.isLive() || contentlet.isWorking()) { %> 
		<%if (contentlet.isLive() && !contentlet.isWorking()) {%>
			<a onClick="selectVersion('<%=contentlet.getInode()%>');">
				<span class="reorderIcon"></span>
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Revert-Working-Changes")) %>
			</a>
		<%} else { %>
			<%if(!scheme.isMandatory()){ %>
			<a onClick="saveContent(false);">
				<span class="saveIcon"></span>
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save")) %>
			</a>
			<%} %>
		<%}%>
	<%} else if (InodeUtils.isSet(contentlet.getInode())) {%>
		<a  onClick="selectVersion('<%=contentlet.getInode()%>');">
			<span class="reorderIcon"></span>
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Bring-Back-Version")) %>
		</a>
	<%} %>
<%}else if(!InodeUtils.isSet(contentlet.getInode())) {%>
	<%if(!scheme.isMandatory()){ %>
			<a onClick="saveContent(false);">
			<span class="saveIcon"></span>
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save")) %>
		</a>
	<%} %>
<%}else if(!isContLocked) {%>


	<%if(!scheme.isMandatory() || ( wfActionsAll != null && wfActionsAll.size() > 0)){ %>




	    <a onClick="makeEditable('<%=contentlet.getInode() %>');" id="lockContentButton">
			<span class="lockIcon"></span>
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Make-Editable")) %>
		</a>
	<%} %>
<%}%>


<%if(!scheme.isMandatory()){ %>
	<%
	boolean canPublish = (InodeUtils.isSet(contentlet.getInode())?canUserPublishContentlet && isContLocked && contentEditable && !contentlet.isArchived():canUserPublishContentlet);
	if (canPublish) {
		String savePublishButtonTitle = UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save-Publish"));
		if(isHost){
			savePublishButtonTitle = UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save-Activate"));
		}%>
		<input type="hidden" id="copyOptions" name="copyOptions" value="<%= copyOptions %>" />
		<a onClick="publishContent()">
			<span class="publishIcon"></span>
			<%= savePublishButtonTitle %>
		</a>
	<% } %>
<% } %>

<%--Start workflow tasks --%>
<%for(WorkflowAction a : wfActions){ %>
	<% List<WorkflowActionClass> actionlets = APILocator.getWorkflowAPI().findActionClasses(a); %>
	<% boolean hasPushPublishActionlet = false; %>
	<% for(WorkflowActionClass actionlet : actionlets){ %>
		<% if(actionlet.getActionlet().getClass().getCanonicalName().equals(PushPublishActionlet.class.getCanonicalName())){ %>
			<% hasPushPublishActionlet = true; %>
		<% } %>
	<% } %>
	
	<%if(a.requiresCheckout() && ! contentEditable) {%>
		<a onclick="contentAdmin.executeWfAction('<%=a.getId()%>', <%= hasPushPublishActionlet || a.isAssignable() || a.isCommentable() || UtilMethods.isSet(a.getCondition()) %>)">
		<span class="<%=a.getIcon()%>"></span>
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, a.getName())) %>
		</a>
	<%} else if(!a.requiresCheckout() && ! contentEditable && InodeUtils.isSet(contentlet.getInode())) {%>
		<a onclick="contentAdmin.executeWfAction('<%=a.getId()%>', <%= hasPushPublishActionlet || a.isAssignable() || a.isCommentable() || UtilMethods.isSet(a.getCondition()) %>)">
		<span class="<%=a.getIcon()%>"></span>
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, a.getName())) %>
		</a>
	<%} else if(a.requiresCheckout() &&  contentEditable ) {%>
		<a onclick="contentAdmin.executeWfAction('<%=a.getId()%>', <%= hasPushPublishActionlet || a.isAssignable() || a.isCommentable() || UtilMethods.isSet(a.getCondition()) %>)">
		<span class="<%=a.getIcon()%>"></span>
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, a.getName())) %>
		</a>
	<%} %>
<%} %>


<a onClick="cancelEdit();">
	<span class="cancelIcon"></span>
	<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cancel")) %>
</a>



