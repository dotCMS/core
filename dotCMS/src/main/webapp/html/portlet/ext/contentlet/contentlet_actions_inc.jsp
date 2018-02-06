<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.workflows.actionlet.PushPublishActionlet"%>
<%@page import="com.dotmarketing.portlets.workflows.model.*"%>
<%@page import="com.dotmarketing.util.DateUtil"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="java.util.List"%>
<%

if(user == null){
	return;	
}
boolean isUserCMSAdmin = APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole());
boolean isHost = ("Host".equals(structure.getVelocityVarName()));

boolean isContLocked=(request.getParameter("sibbling") != null) ? false : contentlet.isLocked();
List<WorkflowScheme> schemes = APILocator.getWorkflowAPI().findSchemesForStruct(structure);
WorkflowTask wfTask = APILocator.getWorkflowAPI().findTaskByContentlet(contentlet); 

List<WorkflowStep> wfSteps = null;
WorkflowStep wfStep = null;
WorkflowScheme scheme = null;
List<WorkflowAction> wfActions = null;
List<WorkflowAction> wfActionsAll = null;
try{
	wfSteps = APILocator.getWorkflowAPI().findStepsByContentlet(contentlet);
	wfActions = APILocator.getWorkflowAPI().findAvailableActions(contentlet, user);
	wfActionsAll= APILocator.getWorkflowAPI().findActions(wfSteps, user);
	if(null != wfSteps && !wfSteps.isEmpty() && wfSteps.size() == 1) {
		wfStep = wfSteps.get(0);
		scheme = APILocator.getWorkflowAPI().findScheme(wfStep.getSchemeId());
	}
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
			<a onClick="saveContent(false);">
				<span class="saveIcon"></span>
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save")) %>
			</a>
		<%}%>
	<%} else if (InodeUtils.isSet(contentlet.getInode())) {%>
		<a  onClick="selectVersion('<%=contentlet.getInode()%>');">
			<span class="reorderIcon"></span>
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Bring-Back-Version")) %>
		</a>
	<%} %>
<%}else if(!InodeUtils.isSet(contentlet.getInode())) {%>
			<a onClick="saveContent(false);">
			<span class="saveIcon"></span>
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save")) %>
		</a>
<%}else if(!isContLocked) {%>


	<%if((null != scheme ) || ( wfActionsAll != null && wfActionsAll.size() > 0)){ %>




	    <a onClick="makeEditable('<%=contentlet.getInode() %>');" id="lockContentButton">
			<span class="lockIcon"></span>
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Make-Editable")) %>
		</a>
	<%} %>
<%}%>



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


<%--Start workflow tasks --%>
<%for(WorkflowAction action : wfActions){ %>
	<% List<WorkflowActionClass> actionlets = APILocator.getWorkflowAPI().findActionClasses(action); %>
	<% boolean hasPushPublishActionlet = false; %>
	<% for(WorkflowActionClass actionlet : actionlets){ %>
		<% if(actionlet.getActionlet() != null && actionlet.getActionlet().getClass().getCanonicalName().equals(PushPublishActionlet.class.getCanonicalName())){ %>
			<% hasPushPublishActionlet = true; %>
		<% } %>
	<% } %>
	
	<a onclick="contentAdmin.executeWfAction('<%=action.getId()%>', <%= hasPushPublishActionlet || action.isAssignable() || action.isCommentable() || UtilMethods.isSet(action.getCondition()) %>)">
	<span class="<%=action.getIcon()%>"></span>
		<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, action.getName())) +"<br/><small>( "+
				APILocator.getWorkflowAPI().findScheme(action.getSchemeId()).getName()
				+" )</small>"%>
	</a>

<%} %>


<a onClick="cancelEdit();">
	<span class="cancelIcon"></span>
	<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cancel")) %>
</a>



