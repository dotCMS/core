<%@ include file="/html/common/init.jsp" %>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowAction"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="java.util.Set"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%
String actionId=request.getParameter("actionId");
String inode=request.getParameter("inode");// DOTCMS-7085
WorkflowAction action = APILocator.getWorkflowAPI().findAction(actionId, user);
Role role = APILocator.getRoleAPI().loadRoleById(action.getNextAssign());
%>
<script>
dojo.require("dojox.data.QueryReadStore");


var myRoleReadStore = new dojox.data.QueryReadStore({url: '/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfRoleStoreAjax?cmd=assignable&actionId=<%=actionId%>'});



</script>
<!--  DOTCMS-7085 -->
<input name="wfConId" id="wfConId" type="hidden" value="<%=inode%>"> 

<div style="margin:auto;width:300px;">
		<div style="margin:10px;">
			<b><%= LanguageUtil.get(pageContext, "Perform-Workflow") %></b>: <%=action.getName() %>
		</div>
	<%if(action.isCommentable()){ %>
		<div style="margin:10px;margin-top:0px">
				<b><%= LanguageUtil.get(pageContext, "Comments") %>: </b><br />
				<textarea name="taskCommentsAux" id="taskCommentsAux" cols=40 rows=8 style="min-height:100px;" dojoType="dijit.form.Textarea"></textarea>
		</div>
	<%}else{ %>
		<div style="margin:10px;margin-top:0px">
				<b><%= LanguageUtil.get(pageContext, "Comments") %>: </b><%= LanguageUtil.get(pageContext, "None") %>
		</div>
	<%} %>
		<div style="margin:10px;">
			<b><%= LanguageUtil.get(pageContext, "Assignee") %>: </b> 
		
			<%if(action.isAssignable()){ %>
				<select id="taskAssignmentAux" name="taskAssignmentAux" dojoType="dijit.form.FilteringSelect" 
						store="myRoleReadStore" searchDelay="300" pageSize="30" labelAttr="name" 
						invalidMessage="<%= LanguageUtil.get(pageContext, "Invalid-option-selected") %>">
				</select>
			<%}else{ %>
				<%=role.getName() %>
				<input type="text" dojoType="dijit.form.TextBox" style="display:none" name="taskAssignmentAux" id="taskAssignmentAux" value="<%=role.getId()%>">
			<%} %>
		</div>
	
	<div class="buttonRow">
		<button dojoType="dijit.form.Button" iconClass="saveAssignIcon" onClick="contentAdmin.saveAssign()" type="button">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %>
		</button>
		<button dojoType="dijit.form.Button" iconClass="cancelIcon" onClick="dijit.byId('contentletWfDialog').hide()" type="button">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
		</button>
	
	</div>
</div>