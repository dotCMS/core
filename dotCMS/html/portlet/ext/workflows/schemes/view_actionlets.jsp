<%@page import="com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowActionClass"%>
<%@page import="java.util.Set"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.business.RoleAPI"%>
<%@page import="com.dotmarketing.beans.Permission"%>
<%@page import="com.dotmarketing.business.PermissionSummary"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page
	import="com.dotmarketing.portlets.workflows.model.WorkflowAction"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page
	import="com.dotmarketing.portlets.workflows.business.WorkflowAPI"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowStep"%>
<%@page
	import="com.dotmarketing.portlets.workflows.model.WorkflowScheme"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>

<%
	WorkflowAPI wapi = APILocator.getWorkflowAPI();
	String stepId = request.getParameter("stepId");
	String actionId = request.getParameter("actionId");
	WorkflowStep step = wapi.findStep(stepId);
	WorkflowAction action = new WorkflowAction();
	Role r = new Role();
	try{
		action =wapi.findAction(actionId, APILocator.getUserAPI().getSystemUser());
		r = APILocator.getRoleAPI().loadRoleById(action.getNextAssign());
	}
	catch(Exception e){
		Logger.debug(this.getClass(), "can't find action");
	}

	String schemeId = step.getSchemeId();

	WorkflowScheme scheme = new WorkflowScheme();
	scheme = wapi.findScheme(schemeId);
	List<WorkflowStep> steps = wapi.findSteps(scheme);
	
	List<WorkflowActionClass> subActions = wapi.findActionClasses(action);
	
%>
<script>
	dojo.ready(function(){
		actionAdmin.actionlets = new Array();
		mainAdmin.resetCrumbTrail();
		mainAdmin.addCrumbtrail("<%=LanguageUtil.get(pageContext, "Workflows")%>", "/html/portlet/ext/workflows/schemes/view_schemes.jsp");
		mainAdmin.addCrumbtrail("<%=LanguageUtil.get(pageContext, "Scheme")%> : <%=(scheme.isArchived()) ? "<strike>" :""%><%=scheme.getName()%><%=(scheme.isArchived()) ? "</strike>" :""%>", stepAdmin.baseJsp  + "?schemeId=<%=schemeId%>");
		
		mainAdmin.addCrumbtrail("<%=LanguageUtil.get(pageContext, "Step")%> : <%=step.getName()%>", stepAdmin.baseJsp + "?schemeId=<%=schemeId%>");
		mainAdmin.addCrumbtrail("<%=LanguageUtil.get(pageContext, "Action")%>", actionAdmin.baseJsp + "?stepId=<%=stepId%>" );
		mainAdmin.refreshCrumbtrail();	

		
		
		var roleStore=new dotcms.dojo.data.UsersReadStore({
			includeRoles:true,
			hideSystemRoles:true,
			includeUsers:false,
			
		});
		
		
		var userRoleStore=new dotcms.dojo.data.UsersReadStore({
			includeRoles:true,
			hideSystemRoles:true,
			includeUsers:true,
			
		});

		
		var permissionSelect = new dijit.form.FilteringSelect({
            id: "whoCanUseSelect",
            name: "whoCanUseSelect",
            store: userRoleStore,
            searchAttr: "name",
            labelAttr: "name",
            pageSize:30,
            searchDelay:300,
            required:false
            
        },
        "actionWhoCanUseSelect");
		
		var assignSelect = new dijit.form.FilteringSelect({
            id: "actionAssignToSelect",
            name: "actionAssignToSelect",
            store: roleStore,
            searchAttr: "name",
            labelAttr: "name",
            displayedValue : "<%=UtilMethods.webifyString(r.getName())%>",
            searchDelay:300,
            value:"<%=UtilMethods.webifyString("role-" + action.getNextAssign())%>",
            pageSize:30,
            required:true

            
        },
        "actionAssignToSelect");
		
		
		actionAdmin.whoCanUse = new Array();
		<% Set<Role> roles = APILocator.getPermissionAPI().getReadRoles(action);%>


		<%for(Role tmpRole :  roles){%>
			actionAdmin.addToWhoCanUse("<%=(tmpRole.isSystem()) ? tmpRole.getRoleKey() : tmpRole.getId()%>", "<%=tmpRole.getName()%> <%=(tmpRole.isSystem()) ? " (" + LanguageUtil.get(pageContext, "User") + ")" : ""%>");
		<% }%>

		actionAdmin.refreshWhoCanUse();
	});
	

	

	

	
	
	
</script>



<div>
	<div dojoType="dijit.form.Form" id="addEditAction" jsId="addEditAction" encType="multipart/form-data" action="/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfActionAjax" method="POST">
		<input type="hidden" name="cmd" value="save">
		<input type="hidden" name="stepId"	value="<%=UtilMethods.webifyString(step.getId())%>">
		<input type="hidden" name="schemeId"	value="<%=UtilMethods.webifyString(scheme.getId())%>">
		<input type="hidden" name="whoCanUse"	id="whoCanUse" value="">
		<div>
			<table class="listingTable" style="width:49%;float:left;">
				
				<tr>
					<th colspan="2">
						<%=LanguageUtil.get(pageContext, "About-Action")%>
					</th>
				</tr>
				<tr>
					<td><%=LanguageUtil.get(pageContext, "Action-Name")%>:</td>
					<td><input type="text" name="actionName" id="actionName"
						dojoType="dijit.form.ValidationTextBox" required="true"
						value="<%=UtilMethods.webifyString(action.getName())%>"
						maxlength="255" style="width:390px;font-weight:bold;border:1px solid #eeeeee"></td>
				</tr>
				<tr>
					<td style="vertical-align: top;"><%=LanguageUtil.get(pageContext, "Who-can-use-action")%>:</td>
					<td>
						<input id="actionWhoCanUseSelect" />
							<button dojoType="dijit.form.Button"
								onClick='actionAdmin.addSelectedToWhoCanUse'
								iconClass="addIcon">
								<%=LanguageUtil.get(pageContext, "add")%>
							</button>
							<div class="wfWhoCanUseDiv">
								<table class="listingTable" id="whoCanUseTbl">
							</div>	
							
							</table>
						</td>
				</tr>
			</table>
			<table class="listingTable" style="width:49%;float:right;">
				<tr>
					<th colspan="2">
						<%=LanguageUtil.get(pageContext, "What-Action-Does")%>
					</th>
				</tr>
				
				<tr>
					<td><%=LanguageUtil.get(pageContext, "Allow-Comments")%>:</td>
					<td><input type="checkbox" name="actionCommentable"
						id="actionCommentable" dojoType="dijit.form.CheckBox" value="true"
						<%=(action.isCommentable()) ? "checked='true'" : ""%>></td>
				</tr>
				<tr>
					<td><%=LanguageUtil.get(pageContext, "User-Assignable")%>:</td>
					<td><input type="checkbox" name="actionAssignable"
						id="actionAssignable" dojoType="dijit.form.CheckBox" value="true"
						<%=(action.isAssignable()) ? "checked='true'" : ""%>></td>
				</tr>
				<tr>
					<td><%=LanguageUtil.get(pageContext, "Assign-To")%>:</td>
					<td>
						<input id="actionAssignToSelect" />
					</td>
				</tr>
				<tr>
					<td><%=LanguageUtil.get(pageContext, "Next-Step")%>:</td>
					<td><select name="actionNextStep" id="actionNextStep"
						dojoType="dijit.form.FilteringSelect">
							<%if(steps !=null){
								for(WorkflowStep s : steps){ %>
								<option value="<%=s.getId() %>"
									<%=(action != null && s.getId().equals(action.getNextStep())) ? "selected='true'" : "" %>><%=s.getName() %></option>
								<%}%>
							<% }%>
					</select></td>
				</tr>
				
			</table>
			<div class="clear"></div>
		</div>
		
	</div>
	<%if(!action.isNew()){ %>
		<div class="clear"></div>
		<div>
			<div class="wfStepBoundingBox" style="width:80%;margin:auto;margin-top:20px;margin-bottom:20px;" >
				<div class="wfStepTitle">
					<%=LanguageUtil.get(pageContext, "Workflow-SubAction")%>
				</div>
								<div class="wfAddActionButtonRow">
					<select name="wfActionlets" id="wfActionlets" style="width:300px;" dojoType="dijit.form.FilteringSelect">
						<%for(WorkFlowActionlet a : wapi.findActionlets()){%>
							<option value="<%=a.getClass()%>"><%=a.getName() %></option>
						<%} %>
					</select>
					<button dojoType="dijit.form.Button"
					 onClick="actionAdmin.addSelectedToActionlets();" iconClass="addIcon">
					<%=LanguageUtil.get(pageContext, "Add-Workflow-SubAction")%>
					</button>
				</div>
				<table class="listingTable" id="actionletsTbl">
					<%for(WorkflowActionClass subaction : subActions){ %>
						<tr class="dojoDndItem" id="id_<%=subaction.getId()%>">
							<td class="wfXBox showPointer" onclick="actionAdmin.deleteAction('<%=subaction.getId()%>')"><span class="deleteIcon"></span></td>
							<td onClick="actionAdmin.viewAction('<%=subaction.getId() %>');" class="showPointer"><%=subaction.getName() %></td>
						</tr>
					<%} %>
				</table>
			</div>
		</div>
	<%} %>
	<div class="wfAddActionButtonRow">
		<button dojoType="dijit.form.Button"
			onClick="actionAdmin.saveAction();" iconClass="saveIcon">
			<%=LanguageUtil.get(pageContext, "Save")%>
		</button>
			&nbsp;
		<button dojoType="dijit.form.Button"
			onClick='mainAdmin.show(stepAdmin.baseJsp + "?schemeId=<%=schemeId%>")'
			iconClass="cancelIcon">
			<%=LanguageUtil.get(pageContext, "Cancel")%>
		</button>
	</div>
</div>






