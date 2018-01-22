<%@page import="com.dotmarketing.beans.WebAsset"%>
<%@include file="/html/portlet/ext/workflows/init.jsp"%>

<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.exception.DotDataException"%>
<%@page import="com.dotmarketing.exception.DotSecurityException"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.portlets.fileassets.business.IFileAsset"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@page import="com.dotmarketing.portlets.workflows.actionlet.PushPublishActionlet"%>
<%@page import="com.dotmarketing.portlets.workflows.model.*"%>
<%@page import="com.dotmarketing.util.DateUtil" %>
<%@page import="com.dotmarketing.util.Logger" %>

<%
	WorkflowTask task = APILocator.getWorkflowAPI().findTaskById(request.getParameter("taskId"));

    //Search for the contentlet (Using the same way the view_tasks_list use to find the contentlet on each WorkflowTask and show it in the list)
    Contentlet contentlet = APILocator.getContentletAPI().search( "+identifier: " + task.getWebasset(), 0, -1, null, APILocator.getUserAPI().getSystemUser(), true ).get( 0 );
    if ( contentlet == null ) {
        out.println( LanguageUtil.get( pageContext, "the-selected-content-cannot-be-found" ) );
        return;
    }
    Language contentletLanguage = APILocator.getLanguageAPI().getLanguage( contentlet.getLanguageId() );

	Structure structure = contentlet.getStructure();

	Role createdBy 		= APILocator.getRoleAPI().loadRoleById(task.getCreatedBy());
	Role assignedTo 	= APILocator.getRoleAPI().loadRoleById(task.getAssignedTo());

	WorkflowStep step 	= APILocator.getWorkflowAPI().findStepByContentlet(contentlet);
	WorkflowScheme scheme = null;
	if(null != step){
		scheme = APILocator.getWorkflowAPI().findScheme(step.getSchemeId());
	}

	List<WorkflowAction> actions = APILocator.getWorkflowAPI().findAvailableActions(contentlet, user);
	List<WorkflowAction>  wfActionsAll= APILocator.getWorkflowAPI().findActions(step, user);

	boolean canEdit = APILocator.getPermissionAPI().doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT, user, false);

	List<WorkflowComment> comments = APILocator.getWorkflowAPI().findWorkFlowComments(task);
	List<WorkflowHistory> history = APILocator.getWorkflowAPI().findWorkflowHistory(task);
	Collections.reverse(history);
	List<IFileAsset> files = Collections.EMPTY_LIST;
	String errorRetrievingFilesMsg = null;
	try {
		files = APILocator.getWorkflowAPI().findWorkflowTaskFilesAsContent(task, user);
	} catch (DotDataException e) {
		errorRetrievingFilesMsg = LanguageUtil.get(pageContext,
				"workflows.task.attachedfiles.permissionerror") + " " + e.getMessage();
		Logger.error(this, "An error occurred when retrieving the files attached to workflow [" +
						task.getTitle() + "] with ID [" + task.getId() + "]: " + e.getMessage());
	}

	java.util.Map params = new java.util.HashMap();
	params.put("struts_action",new String[] {"/ext/workflows/edit_workflow_task"});
	params.put("cmd",new String[] {"view"});
	params.put("taskId",new String[] {String.valueOf(task.getInode())});
    params.put( "langId", new String[]{String.valueOf( contentletLanguage.getId() )} );
	String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params);

	List<User> users = APILocator.getUserAPI().findAllUsers();
	PermissionAPI permAPI = APILocator.getPermissionAPI();
	WebAsset asset = null;
	request.setAttribute("contentletId", contentlet.getInode());

    String assignedRoleName = "";
    if (UtilMethods.isSet( assignedTo ) && UtilMethods.isSet( assignedTo.getId() )) {
        assignedRoleName = assignedTo.getName();
    }

%>

<!-- Include the associated contentlet action scripts -->
<%@ include file="/html/portlet/ext/workflows/workflows_js_inc.jsp" %>


<script language="javascript">


	dojo.require('dijit.form.FilteringSelect');
	dojo.require('dotcms.dijit.FileBrowserDialog');
	dojo.require("dotcms.dijit.ContentPreviewDialog");
	var contentPreview = new dotcms.dijit.ContentPreviewDialog({contentletId:"<%=contentlet.getInode()%>"});

	function showAssign () {
		document.getElementById("assignSelect").selectedIndex = -1;
		document.getElementById("assignDiv").style.display = "";
		document.getElementById("assignSelect").focus();
	}
	function hideAssign () {
		document.getElementById("assignSelect").selectedIndex = -1;
		document.getElementById("assignDiv").style.display = "none";
	}
	function assign () {
		var select = dijit.byId("assignSelect");
		//var value = select.options[select.selectedIndex].value;
		var value = select.getValue();
		document.getElementById("assignDiv").style.display = "none";
		document.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
									<portlet:param name="struts_action" value="/ext/workflows/edit_workflow_task" />
									<portlet:param name="inode" value="<%= String.valueOf(task.getInode()) %>" />
									<portlet:param name="cmd" value="assign_task" />
									<portlet:param name="referer" value="<%= referer %>" />
								</portlet:actionURL>&user_id='+value;
	}
	function assignToMe () {
		document.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
									<portlet:param name="struts_action" value="/ext/workflows/edit_workflow_task" />
									<portlet:param name="inode" value="<%= String.valueOf(task.getInode()) %>" />
									<portlet:param name="cmd" value="assign_task" />
									<portlet:param name="referer" value="<%= referer %>" />
								</portlet:actionURL>&user_id=user-<%= user.getUserId() %>';
	}
	function changeStatus (newStatus) {
		if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-change-the-task-status")) %>')) {
			document.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
									<portlet:param name="struts_action" value="/ext/workflows/edit_workflow_task" />
									<portlet:param name="inode" value="<%= String.valueOf(task.getInode()) %>" />
									<portlet:param name="cmd" value="change_status" />
									<portlet:param name="referer" value="<%= referer %>" />
								</portlet:actionURL>&new_status='+newStatus;
		}
	}
	function attachFile(contentlet,popup) {
		fileBrowser.show();
	}
	function attachFileCallback(file) {
		var fileInode = file.inode;
		document.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
								<portlet:param name="struts_action" value="/ext/workflows/edit_workflow_task" />
								<portlet:param name="inode" value="<%= String.valueOf(task.getInode()) %>" />
								<portlet:param name="cmd" value="add_file" />
								<portlet:param name="referer" value="<%= referer %>" />
							</portlet:actionURL>&file_inode='+fileInode;
	}

	function setImage(inode,name)
	{
	   document.getElementById("attachedFileInode").value = inode;
	   submitParent();
	}

	function removeFile(fileInode) {
		document.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
								<portlet:param name="struts_action" value="/ext/workflows/edit_workflow_task" />
								<portlet:param name="inode" value="<%= String.valueOf(task.getInode()) %>" />
								<portlet:param name="cmd" value="remove_file" />
								<portlet:param name="referer" value="<%= referer %>" />
							</portlet:actionURL>&file_inode='+fileInode;
	}

	function cancel () {
		document.location = "<portlet:actionURL windowState="<%=WindowState.MAXIMIZED.toString()%>">
		<portlet:param name="struts_action" value="/ext/workflows/view_workflow_tasks" />
	    </portlet:actionURL>";
	}

	function doEdit(){
		window.location="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
		<%if(contentlet.getStructure().getName().equals("Event")){%>s
			<portlet:param name="struts_action" value="/ext/calendar/edit_event" />
		<%}else{%>
			<portlet:param name="struts_action" value="/ext/contentlet/edit_contentlet" />
		<%}%>
		<portlet:param name="inode" value="<%= String.valueOf(contentlet.getInode()) %>" />
		<portlet:param name="cmd" value="edit" />
		<portlet:param name="referer" value="<%= referer %>" />
		</portlet:actionURL>";



	}


	 function serveFile(doStuff,conInode,velVarNm){

         if(doStuff != ''){
         window.open('/contentAsset/' + doStuff + '/' + conInode + '/' + velVarNm + "?byInode=true",'fileWin','toolbar=no,resizable=yes,width=400,height=300');
         }else{
         window.open('/contentAsset/raw-data/' + conInode + '/' + velVarNm + "?byInode=true",'fileWin','toolbar=no,resizable=yes,width=400,height=300');
         }
     }


</script>


<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Workflow-Task-Detail")) %>' />




<%
	boolean hasPermission = false;
	ContentletAPI conApi = APILocator.getContentletAPI();
	try {
		Contentlet content = conApi.find(contentlet.getInode(),
				user, false);
		hasPermission = true;
	} catch (DotSecurityException dse) {
		hasPermission = false;
	}
	if (hasPermission) {
%>
<!-- START Task Overview -->

<div class="view-workflow">
	<table class="listingTable">
	<tr>
		<th style="display:flex; align-items:center;">
			<div>
				<% if(structure.getStructureType() ==1){ %>
					<span class="structureIcon"></span>
				<%}else if(structure.getStructureType() ==2){ %>
					<span class="gearIcon"></span>
				<%}else if(structure.getStructureType() ==3){ %>
					<span class="formIcon"></span>
				<%}else if(structure.getStructureType() ==4){ %>
					<span class="documentIcon"></span>
				<%}else if(structure.getStructureType() ==5){ %>
					<span class="pageIcon"></span>
			    <%} %>
			</div>
			<h1 style="margin:15px 0 15px 10px;"><a href="javascript:doEdit()"><%= contentlet.getTitle() %></a></h1>
		</th>
		<th>
		<!-- START Actions -->
			<div id="archiveDropDownButton" data-dojo-type="dijit/form/DropDownButton" data-dojo-props='iconClass:"actionIcon", class:"dijitDropDownActionButton"'>
				<span></span>

				<div data-dojo-type="dijit/Menu" class="contentlet-menu-actions">
					<div id="cancel" data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: cancel">
	                    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cancel")) %>
	                </div>

	                <%--Start workflow tasks --%>
					<%boolean hasAction = false; %>
					<%if(canEdit) {%>
						<%if((null != scheme && !scheme.isMandatory()) || ( wfActionsAll != null && wfActionsAll.size() > 0)){ %>
							<div data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: doEdit">
								<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit-Content")) %>
							</div>
							<% hasAction = true; %>
						<%} %>
					<%} %>

					<%for(WorkflowAction a : actions){ %>
						<%if(a.requiresCheckout())continue; %>

						<% List<WorkflowActionClass> actionlets = APILocator.getWorkflowAPI().findActionClasses(a); %>
						<% boolean hasPushPublishActionlet = false; %>
						<% for(WorkflowActionClass actionlet : actionlets){ %>
							<% if(actionlet.getActionlet() != null && actionlet.getActionlet().getClass().getCanonicalName().equals(PushPublishActionlet.class.getCanonicalName())){ %>
								<% hasPushPublishActionlet = true; %>
							<% } %>
						<% } %>

						<div data-dojo-type="dijit/MenuItem" onclick="contentAdmin.executeWfAction('<%=a.getId()%>', <%=a.isAssignable() || hasPushPublishActionlet%>, <%=a.isCommentable() || UtilMethods.isSet(a.getCondition())%>, '<%=contentlet.getInode()%>', <%=hasPushPublishActionlet%>)">
							<!-- <span class="<%=a.getIcon()%>"></span> -->
							<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, a.getName())) %>
							<% hasAction = true; %>
						</div>
					<%}%>

					<%if(!hasAction){ %>
						<div data-dojo-type="dijit/MenuItem" data-dojo-props="">
							<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "No-Actions")) %>
						</div>
					<%} %>

				</div>
			</div>
		<!-- END Actions -->
		</th>
	</tr>
	<tr>
		<td>
			<strong><%=LanguageUtil.get(pageContext, "Type") %>:</strong>
			<%=structure.getName()%>
		</td>
		<td>
			<strong><%= LanguageUtil.get(pageContext, "Status") %>: </strong>
			<%if (contentlet.isLive()) {%>
	            <span class="liveIcon"></span>
	        <%} else if (contentlet.isArchived()) {%>
	        	<span class="archivedIcon"></span>
	        <%} else if (contentlet.isWorking()) {%>
	            <span class="workingIcon"></span>
	        <%}%>
	        <%if (contentlet.isLocked()) {
	  		  	User u = APILocator.getUserAPI().loadUserById(APILocator.getVersionableAPI().getLockedBy(contentlet), APILocator.getUserAPI().getSystemUser(), false); %>
	        	<span class="lockIcon"  title="<%=UtilMethods.javaScriptify(u.getFullName()) %>"></span>
	   		<%} %>
	   		<%=step.getName()%>
		</td>
	</tr>
	<tr>
		<td>
			<strong><%= LanguageUtil.get(pageContext, "by") %>:</strong>
			<% if(createdBy != null){%>
				<%= createdBy.getName() %>
			<% } else  { %>
				<%= LanguageUtil.get(pageContext, "Nobody") %>
			<% } %>
		</td>

		<td>
			<strong><%= LanguageUtil.get(pageContext, "Created-on") %>:</strong>
			<%= UtilMethods.dateToHTMLDate(task.getCreationDate()) %>
			<%= LanguageUtil.get(pageContext, "at") %> <%= UtilMethods.dateToHTMLTime(task.getCreationDate()) %>
		</td>
	</tr>

	<tr>
		<td>
			<strong><%= LanguageUtil.get(pageContext, "Assigned-To") %>:</strong>
			<%= assignedRoleName%>

		</td>
		<td>
			<strong><%= LanguageUtil.get(pageContext, "Updated") %>:</strong>
			<%= DateUtil.prettyDateSince(task.getModDate(), user.getLocale()) %>
		</td>
	</tr>

	<%String latestComment = (comments != null && comments.size()>0) ? comments.get(0).getComment() :task.getDescription();  %>
	<%if(UtilMethods.isSet(latestComment)){ %>
		<tr>
			<td colspan="2">
				<strong><%= LanguageUtil.get(pageContext, "Latest-Comment") %>:</strong>

					<%=latestComment%>

			</td>
		</tr>
	<%} %>

	<%if (contentlet.isLocked()) {%>
		<tr>
			<td colspan=2>
				<b><%= LanguageUtil.get(pageContext, "Locked") %></b>:


				<%=APILocator.getUserAPI().loadUserById(APILocator.getVersionableAPI().getLockedBy(contentlet), APILocator.getUserAPI().getSystemUser(), false).getFullName() %>
				<span class="lockedAgo" style="display: inline">(<%=UtilMethods.capitalize( DateUtil.prettyDateSince(APILocator.getVersionableAPI().getLockedOn(contentlet), user.getLocale())) %>)</span>


			</td>

		</tr>
	<%} %>

</table>
<!-- END Task Overview -->


<div style="margin:40px 0;">


<!-- START Tabs -->
	<div id="mainTabContainer" dolayout="false" dojoType="dijit.layout.TabContainer">
		<div id="TabZero" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Preview") %>">

			<jsp:include page="/html/portlet/ext/contentlet/view_contentlet_popup_inc.jsp"></jsp:include>


			<div style="border:1px solid #ddd;margin: 60px 0;">

				<table class="listingTable">
					<tr>
						<th><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Comments")) %></th>
						<th>
							<% if (!step.isResolved()) { %>
								<div dojoType="dijit.form.DropDownButton" iconClass="plusIcon" class="view-workflow__add-comment">
									<span><%= LanguageUtil.get(pageContext, "Add-a-Comment") %></span>
									<div dojoType="dijit.TooltipDialog" id="dialog1" title="Login Form" execute="addComment();">
										<form id="commentFormlet" method="post" action="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
												<portlet:param name="struts_action" value="/ext/workflows/edit_workflow_task" />
												<portlet:param name="inode" value="<%= String.valueOf(task.getInode()) %>" />
											</portlet:actionURL>">
										<input type="hidden" name="referer" value="<%= referer %>">
										<input type="hidden" name="cmd" value="add_comment">

										<textarea id="comment" name="comment" class="mceNoEditor" rows="4" cols="60"></textarea>
										<div class="buttonRow">
			                                <button dojoType="dijit.form.Button" type="button" onClick="dojo.byId('commentFormlet').submit()" iconClass="infoIcon">
											    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-Comment")) %>
			                                </button>
										</div>
										</form>
									</div>
								</div>
							<%}%>
						</th>
					</tr>

					<%
					    String str_style2="";
						int y =0;

						Iterator<WorkflowComment> commentsIt = comments.iterator();
						while (commentsIt.hasNext()) {
							WorkflowComment comment = commentsIt.next();

							if(y%2==0){
							  str_style2="class=\"alternate_1\"";
							}
							else{
							  str_style2="class=\"alternate_2\"";
							}
							y++;
					%>
						<tr <%=str_style2 %>>
							<td colspan="2">
								<p>
									<%= APILocator.getRoleAPI().loadRoleById(comment.getPostedBy()) == null ? "": APILocator.getRoleAPI().loadRoleById(comment.getPostedBy()).getName() %> &nbsp;(<%= DateUtil.prettyDateSince(comment.getCreationDate()) %>)<br/>

									<div style="font-size: 10pt;margin:5px;margin-top:0px;"><%= comment.getComment() %><%if (commentsIt.hasNext()) { %><% } %></div>
								</p>
							</td>
						</tr>
					<% } %>

					<%	if (comments.size() == 0) { %>
						<tr>
							<td>
								<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "None") %></div>
							</td>
						</tr>
					<% } %>

				</table>
			</div>

			<!-- END Comments -->

		</div>
	<!-- END Description Tab -->

	<!-- START Files Tab -->
		<div id="TabTwo" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Attached-Files") %>">

			<% if (!step.isResolved()) { %>
				<div class="buttonRow" style="Text-align:right;">
					<button dojoType="dijit.form.Button" onClick="attachFile();" iconClass="browseIcon"><%= LanguageUtil.get(pageContext, "Attach-File") %></button>
				</div>
			<% } else { %>
				<div class="buttonRow" style="Text-align:right;">
					<%= LanguageUtil.get(pageContext, "Attached-Files") %>
				</div>
			<% } %>

			<table class="listingTable">
				<%
					int x=0;
					String str_style="";

					for (IFileAsset file : files) {
						if(x%2==0){
						  str_style="class=\"alternate_1\"";
						}
						else{
						  str_style="class=\"alternate_2\"";
						}
						x++;
				%>
					<tr <%=str_style %>>
						<td>
							<img border="0" src="/icon?i=<%= UtilMethods.encodeURIComponent(file.getFileName()) %>"> &nbsp;
							<a href="#" onclick="javascript: serveFile('','<%= file.getInode()%>','fileAsset');">
								<%= file.getFileName() %>
							</a>
						</td>
						<td>
							<button dojoType="dijit.form.Button" type="button" class="dijitButtonDanger" style="float: right;" href="javascript:removeFile('<%= file.getInode() %>')"><%= LanguageUtil.get(pageContext, "remove") %></button>
						</td>
					</tr>
				<% } %>

				<% if (files.size() == 0 && !UtilMethods.isSet(errorRetrievingFilesMsg)) { %>
					<tr>
						<td colspan="2">
							<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "None") %></div>
						</td>
					</tr>
				<% } else { %>
					<tr>
						<td colspan="2">
							<div class="noResultsMessage"><%= errorRetrievingFilesMsg %></div>
						</td>
					</tr>
				<% } %>
			</table>
		</div>
	<!-- END Files Tab -->



	<!-- START History Tab -->
		<div id="TabThree" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Change-History") %>">
			<table class="listingTable">
				<%
				    int z=0;
					String str_style3="";

					for (WorkflowHistory histItem : history) {

					  if(z%2==0){
					    str_style3="class=\"alternate_1\"";
			          }
					  else{
					    str_style3="class=\"alternate_2\"";
					  }
					  z++;
				%>
					<tr <%=str_style3 %>>
						<td>
							<strong><%= LanguageUtil.get(pageContext, "Change-by") %>:</strong> <%= UtilMethods.getUserFullName(histItem.getMadeBy()) %> (<%= UtilMethods.dateToHTMLDate(histItem.getCreationDate()) %> <%= UtilMethods.dateToHTMLTime(histItem.getCreationDate()) %>)<br>
							<%= UtilMethods.webifyString( histItem.getChangeDescription()).replaceAll(";", "<br />") %>
						</td>
					</tr>
				<% } %>

				<% if (history.size() == 0) { %>
					<tr>
						<td>
							<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "There-are-no-history-yet-on-this-task.") %></div>
						</td>
					</tr>
				<% } %>
			</table>
		</div>
	<!-- END History Tab -->
</div>

<%
	} else {
%>
	<table border="0" cellpadding="4" cellspacing="0" width="100%" height="300">
	    <tr>
	        <td align="center">
	            <table border="0" cellpadding="8" cellspacing="0">
	                <tr>
	                    <td>
	                        <center>
	                            <%= LanguageUtil.get(pageContext, "dont-have-permissions-msg") %>
	                            <br>&nbsp;<br>
	                            <button dojoType="dijit.form.Button" onclick="history.back();" iconClass="cancelIcon">
	                                <%= LanguageUtil.get(pageContext, "try-again") %>
	                        </button>
	                    </td>
	                </tr>
	            </table>
	        </td>
	    </tr>
	</table>
<%
	}
%>
</liferay:box>

<div id="savingContentDialog" dojoType="dijit.Dialog" disableCloseButton="true" title="<%= LanguageUtil.get(pageContext, "Workflow") %>" style="display: none;">
	<div dojoType="dijit.ProgressBar" style="width:200px;text-align:center;" indeterminate="true" jsId="saveProgress" id="saveProgress"></div>
</div>

<div dojoAttachPoint="fileBrowser" jsId="fileBrowser" onFileSelected="attachFileCallback" onlyFiles="true" dojoType="dotcms.dijit.FileBrowserDialog">
</div>

<form id="submitWorkflowTaskFrm" action="/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfTaskAjax?cmd=executeAction">
	<input name="wfActionAssign" id="wfActionAssign" type="hidden" value="">
	<input name="wfActionComments" id="wfActionComments" type="hidden" value="">
	<input name="wfActionId" id="wfActionId" type="hidden" value="">
	<input name="wfContentletId" id="wfContentletId" type="hidden" value="<%=contentlet.getInode()%>">

	<!-- PUSH PUBLISHING ACTIONLET -->
	<input name="wfPublishDate" id="wfPublishDate" type="hidden" value="">
	<input name="wfPublishTime" id="wfPublishTime" type="hidden" value="">
	<input name="wfExpireDate" id="wfExpireDate" type="hidden" value="">
	<input name="wfExpireTime" id="wfExpireTime" type="hidden" value="">
	<input name="wfNeverExpire" id="wfNeverExpire" type="hidden" value="">
	<input name="whereToSend" id="whereToSend" type="hidden" value="">
</form>
</div>