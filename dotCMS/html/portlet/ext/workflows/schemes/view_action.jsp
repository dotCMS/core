<%@page import="com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowActionClass"%>
<%@page import="java.util.Set"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowAction"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.portlets.workflows.business.WorkflowAPI"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowStep"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowScheme"%>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.business.*" %>

<%
	WorkflowAPI wapi = APILocator.getWorkflowAPI();
	String stepId = request.getParameter("stepId");
	String actionId = request.getParameter("actionId");

    WorkflowAction action = new WorkflowAction();
    Role nextAssignRole = new Role();
    boolean hideHierarchayControl = true;
    try {
        action = wapi.findAction( actionId, APILocator.getUserAPI().getSystemUser() );
        nextAssignRole = APILocator.getRoleAPI().loadRoleById( action.getNextAssign() );
        if ( UtilMethods.isSet( nextAssignRole ) && UtilMethods.isSet( nextAssignRole.getId() ) ) {
            if ( !nextAssignRole.isUser() && !nextAssignRole.equals( APILocator.getRoleAPI().loadCMSAnonymousRole() ) ) {
                if ( action.isAssignable() ) {
                    hideHierarchayControl = false;
                }
            }
        }
        if ( stepId == null ) {
            stepId = action.getStepId();
        }
    } catch ( Exception e ) {
        Logger.debug( this.getClass(), "can't find action" );
    }
	WorkflowStep step = wapi.findStep(stepId);
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





		var permissionSelect = new dijit.form.FilteringSelect({
            id: "whoCanUseSelect",
            name: "whoCanUseSelect",
            store: myRoleReadStore2,

            pageSize:30,
            searchDelay:300,
            required:false,
            onClick:function(){
            	dijit.byId("whoCanUseSelect").set("displayedValue","");
            	dijit.byId("whoCanUseSelect").loadDropDown();
            }
        },
        "actionWhoCanUseSelect");

	    <%
            String assignToLabel=null;
            if ( UtilMethods.isSet( nextAssignRole ) && UtilMethods.isSet( nextAssignRole.getId() ) ) {
                assignToLabel = nextAssignRole.getName();
                if(nextAssignRole.equals(APILocator.getRoleAPI().loadCMSAnonymousRole())) {
                    assignToLabel=LanguageUtil.get(pageContext, "current-user");
                }
            }
	    %>

		var assignSelect = new dijit.form.FilteringSelect({
            id: "actionAssignToSelect",
            name: "actionAssignToSelect",
            store: myRoleReadStore,

            displayedValue : "<%=UtilMethods.webifyString(assignToLabel)%>",
            searchDelay:300,
            value:"<%=UtilMethods.webifyString(action.getNextAssign())%>",
            pageSize:30,

            onChange:function(me){
            	actionAdmin.doChange();
            },
            onClick:function(){
            	dijit.byId("actionAssignToSelect").set("displayedValue","");
            	dijit.byId("actionAssignToSelect").loadDropDown();
            }

        },
        "actionAssignToSelect");

		function setIconLabel(){
			//alert(dijit.byId('actionIconSelect').item.value)
			var x = dojo.attr("showIconSpan", "className",dijit.byId('actionIconSelect').item.value);

			//dijit.byId('actionIconSelect').displayedValue = x;
		}

		var iconSelect = new dijit.form.FilteringSelect({
            id: "actionIconSelect",
            name: "actionIconSelect",
            store: myIconStore,
            searchAttr:"value",
            labelAttr: "label",
            labelType: "html",
            searchDelay:300,
            value:"<%=UtilMethods.webifyString(action.getIcon())%>",
            pageSize:50,
            onChange:actionAdmin.doChange,
            onClick:function(){
            	dijit.byId("actionIconSelect").displayedValue="";
            },
            required:false,
            onChange:setIconLabel

        },
        "actionIconSelect");




		//assignSelect._hasBeenBlurred=false;



		actionAdmin.whoCanUse = new Array();
        <% Set<Role> roles = APILocator.getPermissionAPI().getRolesWithPermission(action, PermissionAPI.PERMISSION_USE);%>
		<%for(Role tmpRole :  roles){
			if (UtilMethods.isSet(tmpRole) && UtilMethods.isSet(tmpRole.getId()) ) {%>
                actionAdmin.addToWhoCanUse("<%=(tmpRole.isSystem()) ? tmpRole.getRoleKey() : tmpRole.getId()%>",
                "<%=(tmpRole.getName().toLowerCase().contains("anonymous")) ? LanguageUtil.get(pageContext, "current-user") + " (" + LanguageUtil.get(pageContext, "Everyone") + ")" : tmpRole.getName()+ ((tmpRole.isSystem()) ? " (" + LanguageUtil.get(pageContext, "User") + ")" : "")%>");
            <%}
        }%>


		actionAdmin.refreshWhoCanUse();



		actionClassAdmin.actionClasses = new Array(),
		<%for(WorkflowActionClass subaction : subActions){ %>
			actionClassAdmin.addToActionClassesArray("<%=subaction.getId()%>", "<%=subaction.getName()%>");
		<%} %>

		actionClassAdmin.refreshActionClasses();

		//actionAdmin.doChange();


	});








</script>


<div>
	<div dojoType="dijit.form.Form" id="addEditAction" jsId="addEditAction" encType="multipart/form-data" action="/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfActionAjax" method="POST">
		<input type="hidden" name="cmd" value="save">
		<input type="hidden" name="stepId"	value="<%=UtilMethods.webifyString(step.getId())%>">
		<input type="hidden" name="schemeId"	value="<%=UtilMethods.webifyString(scheme.getId())%>">
		<input type="hidden" name="whoCanUse"	id="whoCanUse" value="">
		<input type="hidden" name="actionId"	id="actionId" value="<%=UtilMethods.webifyString(action.getId())%>">

		<table border="0">
			<tr>
				<td width="50%" valign="top" style="padding:7px;">
					<table class="listingTable" width="100%">

						<tr>
							<th colspan="2">
								<%=LanguageUtil.get(pageContext, "About-Action")%>
							</th>
						</tr>
						<tr>
							<td nowrap="nowrap"><%=LanguageUtil.get(pageContext, "Action-Name")%>:</td>
							<td nowrap="true"><input type="text" name="actionName" id="actionName"
								dojoType="dijit.form.ValidationTextBox" required="true"
								value="<%=UtilMethods.webifyString(action.getName())%>"
								maxlength="255" style="width:390px;font-weight:bold;border:1px solid #eeeeee" onkeypress="actionAdmin.doChange()">
							</td>
						</tr>
						<tr>
							<td  width="100px" ><%=LanguageUtil.get(pageContext, "Save-content")%>: (<%=LanguageUtil.get(pageContext, "Requires-Checkout")%>)</td>
							<td nowrap="true"><input type="checkbox" name="actionRequiresCheckout"
								id="actionRequiresCheckout" dojoType="dijit.form.CheckBox" value="true"
								<%=(action.requiresCheckout()) ? "checked='true'" : ""%> onClick="actionAdmin.doChange()">
							</td>
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
										</table>
									</div>
							</td>
						</tr>
					</table>
				</td>
				<td valign="top" style="padding:7px;">
					<table class="listingTable" width="100%">
						<tr>
							<th colspan="2">
								<%=LanguageUtil.get(pageContext, "What-Action-Does")%>
							</th>
						</tr>

						<tr>
							<td nowrap="true"><%=LanguageUtil.get(pageContext, "Allow-Comments")%>:</td>
							<td><input type="checkbox" name="actionCommentable"
								id="actionCommentable" dojoType="dijit.form.CheckBox" value="true"
								<%=(action.isCommentable()) ? "checked='true'" : ""%> onClick="actionAdmin.doChange()"></td>
						</tr>
						<tr>
							<td nowrap="true"><%=LanguageUtil.get(pageContext, "User-Assignable")%>:</td>
							<td><input type="checkbox" name="actionAssignable"
								id="actionAssignable" dojoType="dijit.form.CheckBox" value="true"
								<%=(action.isAssignable()) ? "checked='true'" : ""%> onClick="actionAdmin.doChange()"></td>
						</tr>
						<tr>
							<td nowrap="true" valign="top"><%=LanguageUtil.get(pageContext, "Assign-To")%>:</td>
							<td valign="top">
								<input id="actionAssignToSelect"  />
								&nbsp;
								<%--hideHierarchayControl --%>
								<table id="divRoleHierarchyForAssign" style="visibility:<%=hideHierarchayControl ? "hidden" : "visible" %>;width:160px;float:right;margin:0px;">
									<tr>
										<td style="margin:0px;border:0px;padding:3px;padding-top:5px;" valign="top">
										<input type="checkbox" name="actionRoleHierarchyForAssign"
												id="actionRoleHierarchyForAssign" dojoType="dijit.form.CheckBox" value="true"
												<%=(action.isRoleHierarchyForAssign()) ? "checked='true'" : ""%> onClick="actionAdmin.doChange()">
										</td>
										<td style="margin:0px;border:0px;padding:0px;;padding-left:3px" valign="top">
											<label for="actionRoleHierarchyForAssign"><%=LanguageUtil.get(pageContext, "Use-Role-Hierarchy")%></label>
										</td>
									</tr>
								</table>


							</td>
						</tr>
						<tr>
							<td nowrap="true"><%=LanguageUtil.get(pageContext, "Next-Step")%>:</td>
							<td>
								<select name="actionNextStep" id="actionNextStep"  onChange="actionAdmin.doChange()"
									dojoType="dijit.form.FilteringSelect">
										<%if(steps !=null){
											for(WorkflowStep s : steps){ %>
											<option value="<%=s.getId() %>"
												<%=(action != null && s.getId().equals(action.getNextStep())) ? "selected='true'" : "" %>><%=s.getName() %></option>
											<%}%>
										<% }%>
								</select>



							</td>
						</tr>
						<tr>
							<td nowrap="true"><%=LanguageUtil.get(pageContext, "Icon")%>:</td>
							<td nowrap="nowrap">
								<div>
									<div id="showIconSpan" class="<%=UtilMethods.webifyString(action.getIcon())%>" style="width:16px;height:16px;border:1px solid silver;padding:1px;margin-right:10px;display: inline-block;"></div>

									<input id="actionIconSelect" name="actionIconSelect" />
								</div>
							</td>
						</tr>

						<tr>
							<td valign="top"><%=LanguageUtil.get(pageContext, "Custom-Code")%>:</td>


							<%
							String textValue = action.getCondition();
					        if(textValue != null){
					            textValue = textValue.replaceAll("&", "&amp;");
					            textValue = textValue.replaceAll("<", "&lt;");
					            textValue = textValue.replaceAll(">", "&gt;");
					        }
					        else{
					        	textValue="";
					        }
							%>


							<td>
								<textarea id="actionCondition" style="min-width:300px;min-height:100px;font-family: monospace;" name="actionCondition"><%=textValue %></textarea>
							</td>
						</tr>


					</table>
				</td>
			</tr>





		</table>
	</div>








	<div class="wfAddActionButtonRow">
		<%if(action!=null && !action.isNew()) {%>
		<span id="deleteButtonDiv" class="saveButtonHide">
			<button dojoType="dijit.form.Button"
				onClick="actionAdmin.deleteAction('<%=action.getId() %>');" iconClass="deleteIcon">
				<%=LanguageUtil.get(pageContext, "Delete")%>
			</button>
		</span>
		&nbsp; 	&nbsp; 	&nbsp;
		<%} %>


		<span id="saveButtonDiv" class="saveButtonHide">
			<button dojoType="dijit.form.Button"
				onClick="actionAdmin.saveAction('<%=stepId %>');" iconClass="saveIcon">
				<%=LanguageUtil.get(pageContext, "Save")%>
			</button>
		</span>&nbsp;
		<button dojoType="dijit.form.Button"
			onClick='mainAdmin.show(stepAdmin.baseJsp + "?schemeId=<%=schemeId%>")'
			iconClass="cancelIcon">
			<%=LanguageUtil.get(pageContext, "Cancel")%>
		</button>
	</div>
	<%if(!action.isNew()){ %>
		<div class="clear"></div>
		<div>
			<div style="margin:40px;" >
				<div class="wfAddActionButtonRow">
					<%=LanguageUtil.get(pageContext, "Workflow-SubAction")%>: <select name="wfActionlets" id="wfActionlets" style="width:300px;" dojoType="dijit.form.FilteringSelect">
						<option value=""></option>
						<%for(WorkFlowActionlet a : wapi.findActionlets()){%>
							<option value="<%=a.getClass().getCanonicalName()%>"><%=a.getName() %></option>
						<%} %>
					</select>
					<button dojoType="dijit.form.Button"
					 onClick="actionClassAdmin.addSelectedToActionClasses();" iconClass="addIcon">
					<%=LanguageUtil.get(pageContext, "Add-Workflow-SubAction")%>
					</button>
				</div>
				<table id="actionletsTbl" class="listingTable">

				</table>
			</div>
		</div>
	<%} %>

</div>






