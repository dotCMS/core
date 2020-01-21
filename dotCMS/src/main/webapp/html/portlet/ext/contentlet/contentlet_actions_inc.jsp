<%@page import="java.util.ArrayList"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.workflows.model.*"%>
<%@page import="com.dotmarketing.util.DateUtil"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@ page import="com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo" %>
<%@ page import="com.dotmarketing.util.Logger" %>
<%

if(user == null){
	return;
}
boolean isUserCMSAdmin = APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadCMSAdminRole());
boolean isHost = ("Host".equals(structure.getVelocityVarName()));

boolean isContLocked=(request.getParameter("sibbling") != null) ? false : contentlet.isLocked();
WorkflowTask wfTask = APILocator.getWorkflowAPI().findTaskByContentlet(contentlet);

List<WorkflowStep> wfSteps = null;
WorkflowStep wfStep = null;
WorkflowScheme scheme = null;
List<WorkflowAction> wfActions = new ArrayList<>();
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
	Logger.error(getClass(),"Exception calculating actions", e);
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
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<% com.dotmarketing.beans.Host myHost =  WebAPILocator.getHostWebAPI().getCurrentHost(request); %>

<script>

var myHostId = '<%= (myHost != null) ? myHost.getIdentifier() : "" %>';

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

function editPage(url, languageId) {
    var customEvent = document.createEvent("CustomEvent");
    customEvent.initCustomEvent("ng-event", false, false,  {
        name: 'edit-page',
		data: {
            url,
            languageId,
            hostId: myHostId
        }
    });
    document.dispatchEvent(customEvent);
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
<%}%>

<%--check permissions to display the save and publish button or not--%>
<%boolean canUserWriteToContentlet = conPerAPI.doesUserHavePermission(contentlet,PermissionAPI.PERMISSION_WRITE,user);%>

<%if(!"edit-page".equals(request.getParameter("angularCurrentPortlet")) && contentlet.isHTMLPage() && contentlet.getIdentifier() != "" && (canUserWriteToContentlet)) {%>
   <div class="content-edit-actions" >
       <a style="border:0px;" onClick="editPage('<%= APILocator.getIdentifierAPI().find(contentlet.getIdentifier()).getURI() %>', '<%= contentlet.getLanguageId() %>')">
           <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "editpage.toolbar.preview.page")) %>
           <div style="display:inline-block;float:right;">&rarr;</div>
       </a>
   </div>
<%} %>




<%if(!wfActionsAll.isEmpty()){%>
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
			<%} else if (InodeUtils.isSet(contentlet.getInode())) {

				final ContentletVersionInfo contentletVersionInfo = APILocator.getVersionableAPI().getContentletVersionInfo(contentlet.getIdentifier(), contentlet.getLanguageId());
				final String 				latestInode		  	  = contentletVersionInfo.getWorkingInode();
			%>
				<a  onClick="editVersion('<%=latestInode%>');">
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "See-Latest-Version")) %>
				</a>
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
			onclick="contentAdmin.executeWfAction('<%=action.getId()%>', <%= action.hasPushPublishActionlet() || action.isAssignable() || action.isCommentable() || UtilMethods.isSet(action.getCondition()) %>)">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, action.getName())) %>   
				<%if(action.hasSaveActionlet()){ %>
                    <i class="fa fa-save" style="opacity:.35;float:right"></i>
                <%} %>

			</a>
		<%} %>

	<%}  %>

</div>
<%} %>

<div class="content-edit-workflow">
	<% if (!isHost) { %>
    <h3><%= LanguageUtil.get(pageContext, "Workflow") %></h3>
    <table>
     <tr>
            <th style="vertical-align: top"><%= LanguageUtil.get(pageContext, "Content-Type") %>:</th>
            <td><%=contentlet!=null && contentlet.getContentType()!=null ? contentlet.getContentType().name() : LanguageUtil.get(pageContext, "not-available") %></td>
        </tr>
        <tr>
            <th style="vertical-align: top"><%= LanguageUtil.get(pageContext, "Workflow") %>:</th>
            <td><%=(scheme==null) ? LanguageUtil.get(pageContext, "not-available") : scheme.getName() %></td>
        </tr>
        <tr>
            <th style="vertical-align: top"><%= LanguageUtil.get(pageContext, "Step") %>:</th>
            <td><%=(wfStep==null) ? LanguageUtil.get(pageContext, "New") : wfStep.getName() %></td>
        </tr>
        <tr>
            <th style="vertical-align: top"><%= LanguageUtil.get(pageContext, "Assignee") %>:</th>
            <td><%=(wfTask == null || wfTask.isNew() || !UtilMethods.isSet(wfTask.getAssignedTo()) || APILocator.getRoleAPI().loadRoleById(wfTask.getAssignedTo()) == null) ? LanguageUtil.get(pageContext, "Nobody") : APILocator.getRoleAPI().loadRoleById(wfTask.getAssignedTo()).getName() %></td>
        </tr>

        <tr id="contentLockedInfo" <%=(!isContLocked) ? "style='height:0px;'" : "" %>>
            <%if(contentlet != null && InodeUtils.isSet(contentlet.getInode()) && isContLocked){ %>
                <th style="vertical-align: top"><%= LanguageUtil.get(pageContext, "Locked") %>:</th>
                <td id="lockedTextInfoDiv">
                    <%=APILocator.getUserAPI().loadUserById(APILocator.getVersionableAPI().getLockedBy(contentlet), APILocator.getUserAPI().getSystemUser(), false).getFullName() %>
                    <span class="lockedAgo">(<%=UtilMethods.capitalize( DateUtil.prettyDateSince(APILocator.getVersionableAPI().getLockedOn(contentlet), user.getLocale())) %>)</span>
                </td>
            <%} %>
        </tr>
    </table>
    <% } %>
</div>


<%if(wfActionsAll.isEmpty() && contentlet!=null && contentlet.getContentType()!=null){ %>
    <div style="padding:5px;"><%=LanguageUtil.get(pageContext, "dot.common.message.no.workflow.schemes") %></div>
<%} %>

