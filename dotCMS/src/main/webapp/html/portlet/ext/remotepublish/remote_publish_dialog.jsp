<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.CacheLocator"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@ include file="/html/common/init.jsp" %>
<%@page import="com.dotmarketing.portlets.contentlet.util.ActionletUtil"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowAction"%>
<%

final String actionId = request.getParameter("actionId");
WorkflowAction action = null;
if(UtilMethods.isSet(actionId)) {
	action = APILocator.getWorkflowAPI().findAction(actionId, user);
}

String conPublishDateVar = request.getParameter("publishDate");
String conExpireDateVar = request.getParameter("expireDate");

final String structureInode = request.getParameter("structureInode");
if(UtilMethods.isSet(structureInode)){
    final Structure structure =  CacheLocator.getContentTypeCache().getStructureByInode(structureInode);
    if(UtilMethods.isSet(structure)){
        conPublishDateVar = structure.getPublishDateVar();
        conExpireDateVar = structure.getExpireDateVar();
    }
}

final String inode = request.getParameter("inode");// DOTCMS-7085

%>

<style type="text/css">
    .progressRow {
        padding-left: 33%;
    }
</style>

<script type="application/javascript" >

    function setDates(){
        // force dojo to parse
        var pubDay = new Date();
        var pubTime = new Date();
        var expireDate = new Date();
        var expireTime = new Date();

        if(dijit.byId("<%=conPublishDateVar%>Date")){
            pubDay = dijit.byId("<%=conPublishDateVar%>Date").getValue();
            dijit.byId("wfPublishDateAux").setValue(pubDay);
        }
        if(dijit.byId("<%=conPublishDateVar%>Time")){
            pubTime= dijit.byId("<%=conPublishDateVar%>Time").getValue();
            dijit.byId("wfPublishTimeAux").setValue(pubTime);
        }

        if(dijit.byId("<%=conExpireDateVar%>Date") && dijit.byId("<%=conExpireDateVar%>Date").getValue()){
            expireDate = dijit.byId("<%=conExpireDateVar%>Date").getValue();
            dijit.byId("wfExpireDateAux").setValue(expireDate);

        }

        if(dijit.byId("<%=conExpireDateVar%>Time")){
            expireTime= dijit.byId("<%=conExpireDateVar%>Time").getValue();
            dijit.byId("wfExpireTimeAux").setValue(expireTime);
        }
    }

</script>


<input name="assetIdentifier" id="assetIdentifier" type="hidden" value="<%=inode%>">
<input name="actionId" id="actionId" type="hidden" value="<%=actionId%>">
<input name="hasCondition" id="hasCondition" type="hidden" value="<%=(null != action && UtilMethods.isSet(action.getCondition()))%>">

<div id="pushPublish-container">
<div class="form-horizontal" dojoType="dijit.form.Form" id="publishForm">

	<%--  if we have an action then we're looking at workflow request. --%>
	<%
		if(null != action){
		   final Role role = APILocator.getRoleAPI().loadRoleById(action.getNextAssign());
	%>

        <%--  COMMENTABLE ACTION  --%>
		<%if(action.isCommentable()){ %>
		<dl>
			<dt>
				<%= LanguageUtil.get(pageContext, "Comments") %>:
			</dt>
			<dd>
				<textarea name="taskCommentsAux" id="taskCommentsAux" cols=40 rows=8 style="min-height:100px;width:260px;" dojoType="dijit.form.Textarea"></textarea>
			</dd>
		</dl>
		<%}%>

		<%-- ASSIGNABLE ACTION --%>
		<%if (action.isAssignable()) { %>

			<dl>
				<dt>
					<%= LanguageUtil.get(pageContext, "Assignee") %>:
				</dt>
				<dd>
					<%if (action.isAssignable()) { %>
					<select id="taskAssignmentAux" name="taskAssignmentAux"
							dojoType="dijit.form.FilteringSelect"
							searchDelay="300" pageSize="30" labelAttr="name"
							style="width:260px"
							invalidMessage="<%= LanguageUtil.get(pageContext, "Invalid-option-selected") %>">
					</select>
					<%} else if (UtilMethods.isSet(role) && UtilMethods.isSet(role.getId())) { %>
					<%= APILocator.getRoleAPI().loadCMSAnonymousRole().getId().equals(role.getId())
							? LanguageUtil.get(pageContext, "current-user") : role.getName()%>
					<input type="text" dojoType="dijit.form.TextBox" style="display:none"
						   name="taskAssignmentAux" id="taskAssignmentAux" value="<%=role.getId()%>">
					<%} %>
				</dd>
			</dl>

		<%} %>

		<%
		final boolean hasPushPublishActionlet = ActionletUtil.hasPushPublishActionlet(action);
		if (hasPushPublishActionlet) { %>
<%--		   <%@ include file="/html/portlet/ext/remotepublish/remote_publish_form_ui_inc.jsp" %>--%>
		<%} %>

        <div class="progressRow" style="display: none;">
            <div dojoType="dijit.ProgressBar" style="width:200px;text-align:center;" indeterminate="true" jsId="saveRemotePublishProgress" id="saveRemotePublishProgress"></div>
        </div>

		<div class="buttonRow-right">
			<button dojoType="dijit.form.Button" class="dijitButtonFlat" type="button" id="remotePublishCancelButton" >
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
			</button>
			<button dojoType="dijit.form.Button" type="button" id="remotePublishSaveButton">
				<%=  hasPushPublishActionlet ?
						UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publisher_dialog_push")) :
						UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publisher_dialog_save")) %>
			</button>
		</div>

	<%} else {%>
		<%--  if there isn't an action then we're attending a push publish request. --%>

		<%@ include file="/html/portlet/ext/remotepublish/remote_publish_form_ui_inc.jsp" %>

		<div class="progressRow" style="display: none;">
			<div dojoType="dijit.ProgressBar" style="width:200px;text-align:center;" indeterminate="true" jsId="saveRemotePublishProgress" id="saveRemotePublishProgress"></div>
		</div>

		<div class="buttonRow-right">
			<button dojoType="dijit.form.Button" class="dijitButtonFlat" type="button" id="remotePublishCancelButton" >
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
			</button>
			<button dojoType="dijit.form.Button" type="button" id="remotePublishSaveButton">
				<%=  UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publisher_dialog_push"))%>
			</button>
		</div>

	<%} %>

		<input id="whereToSendRequired" type="hidden" value="<%=LanguageUtil.get(pageContext, "publisher_dialog_environment_mandatory")%>" />
		<input id="assignToRequired" type="hidden" value="<%=LanguageUtil.get(pageContext, "Assign-To-Required")%>" />

</div>
<script>
    dojo.addOnLoad(function () {
        setDates();
	});
</script>

<%@ include file="/html/portlet/ext/remotepublish/remote_publish_form_inc.jsp" %>
</div>
