<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
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
	wfActions = APILocator.getWorkflowAPI().findAvailableActionsEditing(contentlet, user);
	wfActionsAll= APILocator.getWorkflowAPI().findActions(wfSteps, user);
	if(null != wfSteps && !wfSteps.isEmpty() && wfSteps.size() == 1) {
		wfStep = wfSteps.get(0);
		scheme = APILocator.getWorkflowAPI().findScheme(wfStep.getSchemeId());
	}
}
catch(Exception e){
	wfActions = new ArrayList<>();
}

	Map<String, String> schemesAvailable = new HashMap<>();
	for (WorkflowAction action : wfActions) {
		if (!schemesAvailable.containsKey(action.getSchemeId())) {
			schemesAvailable.put(action.getSchemeId(),
					APILocator.getWorkflowAPI().findScheme(action.getSchemeId()).getName());
		}
	}
%>
<script>
function setMyWorkflowScheme(){
	var schemeId=dijit.byId("select-workflow-scheme-dropdown").getValue();
   document.querySelectorAll('.content-edit-actions .schemeActionsDiv').forEach(function(ele) {
        ele.style.display='none';
    });
	
    if(!schemeId || schemeId.length<1){
    	return;
    }

	
	document.querySelectorAll('.content-edit-actions .schemeId' + schemeId).forEach(function(ele) {
	    ele.style.display='block';
	});
}

</script>


<%if(schemesAvailable.size()>1){%>
<div style="margin-bottom:10px;">
	<select id="select-workflow-scheme-dropdown" dojoType="dijit.form.FilteringSelect" onchange="setMyWorkflowScheme()" style="width:100%">
	
	   <option value=""><%=LanguageUtil.get(pageContext, "dot.common.select.workflow")%></option>
		<%for(String key :schemesAvailable.keySet()) {%>
		  
		  <option value="<%=key%>"><%=schemesAvailable.get(key) %></option>
		
		<%} %>
	</select>
</div>
<%} %>

<%--check permissions to display the save and publish button or not--%> 
<%boolean canUserWriteToContentlet = conPerAPI.doesUserHavePermission(contentlet,PermissionAPI.PERMISSION_WRITE,user);%> 

<div class="content-edit-actions">

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



	<%final boolean canPublish = (InodeUtils.isSet(contentlet.getInode())?canUserPublishContentlet && isContLocked && contentEditable && !contentlet.isArchived():canUserPublishContentlet);%>
	<%if (canPublish && isHost) {%>
		<% final String savePublishButtonTitle = UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save-Activate"));%>
	
			<a onClick="saveContent(false);">
				<span class="saveIcon"></span>
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save")) %>
			</a>
			<input type="hidden" id="copyOptions" name="copyOptions" value="<%= copyOptions %>" />
			<a onClick="publishContent()">
				<span class="publishIcon"></span>
				<%= savePublishButtonTitle %>
			</a>

	<% } %>

	<%--Start workflow tasks --%>
	<%if(wfActions.size()>0) {%>

		<%for(WorkflowAction action : wfActions){ %>
			<% List<WorkflowActionClass> actionlets = APILocator.getWorkflowAPI().findActionClasses(action); %>


			
			<a 
			style="<%if(schemesAvailable.size()>1){%>display:none;<%} %>" class="schemeId<%=action.getSchemeId()%> schemeActionsDiv"
			onclick="contentAdmin.executeWfAction('<%=action.getId()%>', <%= action.doesPushPublish() || action.isAssignable() || action.isCommentable() || UtilMethods.isSet(action.getCondition()) %>)">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, action.getName())) %>   <div style="float:right"><%if(action.doesSave()){ %>(saves)<%} %></div>

			</a>
		<%} %>

	<%} %>

</div>


