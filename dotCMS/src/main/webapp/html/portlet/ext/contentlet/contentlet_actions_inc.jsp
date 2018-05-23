<%@page import="java.util.stream.Collectors"%>
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
	wfActions = new ArrayList<>();
}

boolean showScheme = false;
String changeMe=null;
for(WorkflowAction action : wfActions){
    if(changeMe!=null && !changeMe.equals(action.getSchemeId())){
        showScheme=true;
        break;
    }
    changeMe=action.getSchemeId();
}
%>

<%--check permissions to display the save and publish button or not--%> 
<%boolean canUserWriteToContentlet = conPerAPI.doesUserHavePermission(contentlet,PermissionAPI.PERMISSION_WRITE,user);%> 

<div class="content-edit-actions">
	<div style="margin-bottom:-1px">
		<%if(isContLocked && (contentEditable || isUserCMSAdmin)) {%>
			<%if(contentEditable){ %>
			    <a onClick="unlockContent('<%=contentlet.getInode() %>');" id="unlockContentButton">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Release-Lock")) %>
				</a>
			<%}else{ %>
			    <a onClick="stealLock('<%=contentlet.getInode() %>');" id="stealLockContentButton">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Steal-Lock")) %>
				</a>
			<%} %>
		<%} %>

		
		<%if ((InodeUtils.isSet(contentlet.getInode())) && (canUserWriteToContentlet) && (!contentlet.isArchived()) && isContLocked && contentEditable) { %> 
			<%if (!InodeUtils.isSet(contentlet.getInode()) || contentlet.isLive() || contentlet.isWorking()) { %> 
				<%if (contentlet.isLive() && !contentlet.isWorking()) {%>
					<a onClick="selectVersion('<%=contentlet.getInode()%>');">
						<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Revert-Working-Changes")) %>
					</a>
				<%}%>
			<%} else if (InodeUtils.isSet(contentlet.getInode())) {%>
				<a  onClick="selectVersion('<%=contentlet.getInode()%>');">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Bring-Back-Version")) %>
				</a>
			<%} %>
		<%}else if(!isContLocked &&  !contentlet.isNew()) {%>
		
			<%if((null != scheme ) || ( wfActionsAll != null && wfActionsAll.size() > 0)){ %>
			    <a onClick="makeEditable('<%=contentlet.getInode() %>');" id="lockContentButton">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Make-Editable")) %>
				</a>
			<%} %>
		<%}%>
	</div>


	<%final boolean canPublish = (InodeUtils.isSet(contentlet.getInode())?canUserPublishContentlet && isContLocked && contentEditable && !contentlet.isArchived():canUserPublishContentlet);%>
	<%if (canPublish && isHost) {%>
		<% final String savePublishButtonTitle = UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save-Activate"));%>
		<div style="margin-top:-1px">
			<a onClick="saveContent(false);">
				<span class="saveIcon"></span>
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save")) %>
			</a>
			<input type="hidden" id="copyOptions" name="copyOptions" value="<%= copyOptions %>" />
			<a onClick="publishContent()">
				<span class="publishIcon"></span>
				<%= savePublishButtonTitle %>
			</a>
		</div>
	<% } %>
		
	<%--Start workflow tasks --%>
	<%if(wfActions.size()>0) {%>
		<%String wfSchemeIdStr=null; %>
		<div style="margin-top:-1px">
			<%for(WorkflowAction action : wfActions){ %>
				<% List<WorkflowActionClass> actionlets = APILocator.getWorkflowAPI().findActionClasses(action); %>
				<% boolean hasPushPublishActionlet = false; %>
				<% for(WorkflowActionClass actionlet : actionlets){ %>
					<% if(actionlet.getActionlet() != null && actionlet.getActionlet().getClass().getCanonicalName().equals(PushPublishActionlet.class.getCanonicalName())){ %>
						<% hasPushPublishActionlet = true; %>
					<% } %>
				<% } %>
				<% if(wfSchemeIdStr!=null && !wfSchemeIdStr.equals(action.getSchemeId())){%>
					</div><div style="margin-top:-1px">
				<%} %>
				<% wfSchemeIdStr=action.getSchemeId();%>
				<a onclick="contentAdmin.executeWfAction('<%=action.getId()%>', <%= hasPushPublishActionlet || action.isAssignable() || action.isCommentable() || UtilMethods.isSet(action.getCondition()) %>)">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, action.getName())) %>
					<%if(showScheme){ %>
						<div style="padding-left:8px;font-size:x-small"><%=APILocator.getWorkflowAPI().findScheme(action.getSchemeId()).getName() %></div>
					<%} %>
				</a>
			<%} %>
		</div>
	<%} %>



	<div style="margin-top:20px">
		<a onClick="cancelEdit();">
			<span class="cancelIcon"></span>
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Close")) %>
		</a>
	</div>
</div>


