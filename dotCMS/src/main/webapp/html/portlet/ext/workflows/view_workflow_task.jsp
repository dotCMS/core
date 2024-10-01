<%@page import="org.apache.commons.codec.digest.DigestUtils"%>
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
<%@ page import="com.dotmarketing.portlets.workflows.business.WorkflowAPI" %>
<%@page import="java.util.Date" %>
<%@page import="java.util.Optional" %>
<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<%@ page import="com.dotmarketing.portlets.fileassets.business.FileAsset" %>

<%!


public String getPostedby(String postedBy){


	try {

	    return APILocator.getUserAPI().loadUserById(postedBy, APILocator.systemUser(), false).getFullName();
	} catch (Exception e) {
	    try{
	        return APILocator.getRoleAPI().loadRoleById(postedBy).getName();
	    }
	    catch(Exception ee){

	    }
	}
	return "unknown";
}

public String getGravatar(String postedBy){


    try {
        return DigestUtils.md5Hex(APILocator.getUserAPI().loadUserById(postedBy, APILocator.systemUser(), false).getEmailAddress().toLowerCase()).toString();

    } catch (Exception e) {

    }
    return "unknown";
}
%>
<%
    WorkflowTask task = APILocator.getWorkflowAPI().findTaskById(request.getParameter("taskId"));

    //Search for the contentlet (Using the same way the view_tasks_list use to find the contentlet on each WorkflowTask and show it in the list)
    Contentlet contentlet = APILocator.getContentletAPI().search( "+identifier: " + task.getWebasset() + " +languageId:" + task.getLanguageId(), 0, -1, null, APILocator.getUserAPI().getSystemUser(), true ).get( 0 );
    if ( contentlet == null ) {
        out.println( LanguageUtil.get( pageContext, "the-selected-content-cannot-be-found" ) );
        return;
    }
    Language contentletLanguage = APILocator.getLanguageAPI().getLanguage( contentlet.getLanguageId() );

    Structure structure = contentlet.getStructure();

    Role createdBy      = APILocator.getRoleAPI().loadRoleById(task.getCreatedBy());
    Role assignedTo     = APILocator.getRoleAPI().loadRoleById(task.getAssignedTo());

    WorkflowStep step   = APILocator.getWorkflowAPI().findStepByContentlet(contentlet);
    WorkflowScheme scheme = null;
    if(null != step){
        scheme = APILocator.getWorkflowAPI().findScheme(step.getSchemeId());
    }

    List<WorkflowAction> actions = APILocator.getWorkflowAPI().findAvailableActions(contentlet, user, WorkflowAPI.RenderMode.LISTING);
    List<WorkflowAction>  wfActionsAll= APILocator.getWorkflowAPI().findActions(step, user);

    boolean canEdit = APILocator.getPermissionAPI().doesUserHavePermission(contentlet, PermissionAPI.PERMISSION_EDIT, user, false);

    List<WorkflowTimelineItem> commentsHistory = APILocator.getWorkflowAPI().getCommentsAndChangeHistory(task);

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
        var customEvent = document.createEvent("CustomEvent");
        customEvent.initCustomEvent("ng-event", false, false,  {
            name: "close"
        });
        document.dispatchEvent(customEvent)
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



</script>
<style>

.gravitarThingy{

   border-radius: 50%;
   background-size: cover;
   border:1px solid #eeeeee;
   width:40px;
   height:40px;
   font-size: 24px;
   text-align:center;
   color:white;
   font-weight: bold;
   vertical-align: middle;
   margin-left:auto;
   margin-right:auto;

}
</style>



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
<!-- START Task HEADER -->

<div class="view-workflow" style="margin:0px;border:0px;">
    <table class="listingTable" style="margin:0px;border:0px;">
	    <tr>
	        <th style="display:flex; align-items:center;">
	            <div>
	                <span class="pageIcon"></span>

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
	                        <%if( wfActionsAll != null && wfActionsAll.size() > 0 ){ %>
	                            <div data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: doEdit">
	                                <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit-Content")) %>
	                            </div>
	                            <% hasAction = true; %>
	                        <%} %>
	                    <%} %>

	                    <%for(WorkflowAction a : actions){ %>

	                        <% List<WorkflowActionClass> actionlets = APILocator.getWorkflowAPI().findActionClasses(a); %>
	                        <% boolean hasPushPublishActionlet = false; %>
	                        <% for(WorkflowActionClass actionlet : actionlets){ %>
	                            <% if(actionlet.getActionlet() != null && actionlet.getActionlet().getClass().getCanonicalName().equals(PushPublishActionlet.class.getCanonicalName())){ %>
	                                <% hasPushPublishActionlet = true; %>
	                            <% } %>
	                        <% } %>

	                        <div data-dojo-type="dijit/MenuItem" onclick="contentAdmin.executeWfAction('<%=a.getId()%>', <%=a.isAssignable() || hasPushPublishActionlet%>, <%=a.isCommentable() || UtilMethods.isSet(a.getCondition())%>, '<%=contentlet.getInode()%>', <%=hasPushPublishActionlet%>, <%=(a.hasMoveActionletActionlet() && !a.hasMoveActionletHasPathActionlet())%>)">
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
	</table>
	<!-- END Task HEADER -->

	<!-- START Tabs -->
	<div style="display: flex; flex-wrap: nowrap;">
		<div id="mainTabContainer" dolayout="false" dojoType="dijit.layout.TabContainer" style="margin-top:10px;">

		    <!-- START Comments -->
		        <div id="commentsTab" dojoType="dijit.layout.ContentPane" title="Comments">
                    <div style="max-height:500px;overflow: auto;border-bottom:1px solid silver;padding:15px;">
		                <table style="width:100%;padding: 0px;">
		                    <%
		                        for(WorkflowTimelineItem comment : commentsHistory){ %>
                                  <tr>
		                            <td style="width:50px;vertical-align: top;padding: 15px 0px 15px 0px">
                                        <div class="gravitarThingy" style="z-index:100;background-image:url('https://www.gravatar.com/avatar/<%=getGravatar(comment.roleId()) %>?d=blank'"></div>
                                         <div class="gravitarThingy" style="z-index:4;color:#bbbbbb;margin-top:-40px;">
                                            <div style="margin-top:5px;">
                                                <%=getPostedby(comment.roleId()).substring(0,1) %>
                                            </div>
                                         </div>
		                            </td>
                                     <td>
                                        <div  style="border:1px solid silver;margin:10px 15px 14px 5px">
                                          <div style="background: #efefef;padding:10px;"><b><%=getPostedby(comment.roleId())%></b> <%= DateUtil.prettyDateSince(comment.createdDate()) %>

                                          <div style="float:right;"><%=UtilMethods.dateToHTMLDate(comment.createdDate()) %> - <%=UtilMethods.dateToHTMLTime(comment.createdDate()) %></div>

                                          </div>
                                          <div style="padding:15px;min-height: 70px"><%= Xss.strip(comment.commentDescription()) %></div>
                                        </div>
                                     </td>
		                        </tr>

		                    <% } %>
		                </table>
		              </div>
                      <div style="padding:20px;max-width:1024px;border-top:0px solid silver">
                         <form style="max-width: 700px; display: flex; align-items: end; margin-left: 51px" id="commentFormlet" method="post" action="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
                            <portlet:param name="struts_action" value="/ext/workflows/edit_workflow_task" />
                            <portlet:param name="inode" value="<%= String.valueOf(task.getInode()) %>" />
                            </portlet:actionURL>">
                            <input type="hidden" name="referer" value="<%= referer %>">
                            <input type="hidden" name="cmd" value="add_comment">

                            <textarea id="comment" name="comment" class="mceNoEditor" rows="4" cols="60"></textarea>

                            <div style="margin-left: 16px;">
                                <button dojoType="dijit.form.Button" class="dijitButtonAction" type="button" onClick="dojo.byId('commentFormlet').submit()">
                                <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-Comment")) %>
                                </button>
                            </div>
                            </div>
                           </form>






		            <div style="float:left;padding:10px;width:100%;">
		                <jsp:include page="/html/portlet/ext/contentlet/view_contentlet_popup_inc.jsp"></jsp:include>
		            </div>

		        </div>

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
								FileAsset fileAsset = null;
								if (file instanceof FileAsset){
									fileAsset = (FileAsset) file;
								}
		                %>
		                    <tr <%=str_style %>>

								<td>
									<a href="<%= (fileAsset != null && fileAsset.getMap().get("fileLink") != null) ? fileAsset.getMap().get("fileLink") : file.getURI() %>" target="_blank">
										<%= (fileAsset != null && fileAsset.getMap().get("fileLink") != null) ? fileAsset.getUnderlyingFileName() : file.getFileName() %>
									</a>
								</td>
								<td>

		                            <button dojoType="dijit.form.Button" type="button" class="dijitButtonDanger" style="float: right;" onClick="removeFile('<%= file.getInode() %>')"><%= LanguageUtil.get(pageContext, "remove") %></button>
		                        </td>
		                    </tr>
		                <% } %>

		                <% if (files.size() == 0 && !UtilMethods.isSet(errorRetrievingFilesMsg)) { %>
		                    <tr>
		                        <td colspan="2">
		                            <div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "None") %></div>
		                        </td>
		                    </tr>
                        <% } else if (UtilMethods.isSet(errorRetrievingFilesMsg)) { %>
		                    <tr>
		                        <td colspan="2">
		                            <div class="noResultsMessage"><%= errorRetrievingFilesMsg %></div>
		                        </td>
		                    </tr>
		                <% } %>
		            </table>
		        </div>
		    <!-- END Files Tab -->

		</div>

		<!-- START Task DETAILS -->
		<div style="width:300px;background:#fafafa;">
			<table class="listingTable">
			    <tr>
			        <td>
			            <strong><%=LanguageUtil.get(pageContext, "Type") %>:</strong>
			            <%=structure.getName()%>
			        </td>
			    </tr>
			    <tr>
			        <td>
			            <strong><%= LanguageUtil.get(pageContext, "Assigned-To") %>:</strong>
			            <%= assignedRoleName%>

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
			    </tr>
			    <tr>
			        <td>
			            <strong><%= LanguageUtil.get(pageContext, "Created-on") %>:</strong>
			            <%= UtilMethods.dateToHTMLDate(task.getCreationDate()) %>
			            <%= LanguageUtil.get(pageContext, "at") %> <%= UtilMethods.dateToHTMLTime(task.getCreationDate()) %>
			        </td>
			    </tr>

			    <tr>
			        <td>
			            <strong><%= LanguageUtil.get(pageContext, "Status") %>: </strong>
			            <%= com.dotmarketing.util.UtilHTML.getStatusIcons(contentlet) %>
			            <%=step.getName()%>
			        </td>
			       </tr>
			       <tr>
			        <td>
			            <strong><%= LanguageUtil.get(pageContext, "Updated") %>:</strong>
			            <%= DateUtil.prettyDateSince(task.getModDate(), user.getLocale()) %>
			        </td>
			    </tr>

			    <%if (contentlet.isLocked()) {%>
                    <%
                        Optional<Date> date = APILocator.getVersionableAPI().getLockedOn(contentlet);
                        Optional<String> userId = APILocator.getVersionableAPI().getLockedBy(contentlet);
                    %>
			        <tr>
			            <td>
			                <b><%= LanguageUtil.get(pageContext, "Locked") %></b>:
			                <%=userId.isPresent()?APILocator.getUserAPI().loadUserById(userId.get(), APILocator.getUserAPI().getSystemUser(), false).getFullName():"--" %>
			                <span class="lockedAgo" style="display: inline">(<%=date.isPresent()? UtilMethods.capitalize(DateUtil.prettyDateSince(date.get(), user.getLocale())): "--" %>)</span>
			            </td>
			        </tr>
			    <%} %>
			</table>
		</div>
		<!-- END Task DETAILS -->
	</div>



</div>
<%} else {%>
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
<% }%>


<div id="savingContentDialog" dojoType="dijit.Dialog" disableCloseButton="true" title="<%= LanguageUtil.get(pageContext, "Workflow") %>" style="display: none;">
    <div dojoType="dijit.ProgressBar" style="width:200px;text-align:center;" indeterminate="true" jsId="saveProgress" id="saveProgress"></div>
</div>

<div dojoAttachPoint="fileBrowser" jsId="fileBrowser" onFileSelected="attachFileCallback" onlyFiles="true" dojoType="dotcms.dijit.FileBrowserDialog">
</div>
</div>
