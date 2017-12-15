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
	String schemeId  = request.getParameter("schemeId");
	String stepId    = request.getParameter("stepId");
	String actionId  = request.getParameter("actionId");

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

        if ( schemeId == null ) {
			schemeId = action.getSchemeId();
        }
    } catch ( Exception e ) {
        Logger.debug( this.getClass(), "can't find action" );
    }

	final WorkflowScheme scheme = wapi.findScheme(schemeId);

	List<WorkflowStep> steps = wapi.findSteps(scheme);
	List<WorkflowActionClass> subActions = wapi.findActionClasses(action);

	boolean showOnBoth   = true;
	boolean showLocked   = false;
	boolean showUnLocked = false;
	if (null != action) {
		if (action.shouldShowOnLock() && !action.shouldShowOnUnlock()) {
			showOnBoth   = showUnLocked = false;
			showLocked   = true;
		} else if (!action.shouldShowOnLock() && action.shouldShowOnUnlock()) {
			showOnBoth   = showLocked = false;
			showUnLocked = true;
		}
	}

%>
<script type="text/javascript">

	dojo.ready(function(){
		
		
		actionAdmin.actionlets = new Array();
		mainAdmin.resetCrumbTrail();
		mainAdmin.addCrumbtrail("<%=LanguageUtil.get(pageContext, "Workflows")%>", "/html/portlet/ext/workflows/schemes/view_schemes.jsp");
		mainAdmin.addCrumbtrail("<%=LanguageUtil.get(pageContext, "Scheme")%> : <%=(scheme.isArchived()) ? "<strike>" :""%><%=scheme.getName()%><%=(scheme.isArchived()) ? "</strike>" :""%>", stepAdmin.baseJsp  + "?schemeId=<%=schemeId%>");
		mainAdmin.addCrumbtrail("<%=LanguageUtil.get(pageContext, "Action")%>", actionAdmin.baseJsp + "?stepId=<%=stepId%>" );
		mainAdmin.refreshCrumbtrail();
		
		var permissionSelect = new dijit.form.FilteringSelect({
            id: "whoCanUseSelect",
            name: "whoCanUseSelect",
            store: myRoleReadStore2,
            pageSize:30,
            searchDelay:300,
			style: "width: 80%",
            required:false,
            onClick:function(){
            	dijit.byId("whoCanUseSelect").set("displayedValue","");
            	dijit.byId("whoCanUseSelect").loadDropDown();            	
            }
        },
        "actionWhoCanUseSelect");
		
	    <%
            String assignToLabel=null;
            if ( UtilMethods.isSet( nextAssignRole ) && UtilMethods.isSet( nextAssignRole.getId())) {
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
		

		actionAdmin.whoCanUse = new Array();
        <% Set<Role> roles = APILocator.getPermissionAPI().getRolesWithPermission(action, PermissionAPI.PERMISSION_USE);%>
		<%for(Role tmpRole :  roles){
			if (UtilMethods.isSet(tmpRole) && UtilMethods.isSet(tmpRole.getId()) ) {%>
                actionAdmin.addToWhoCanUse("<%=(tmpRole.isSystem()) ? tmpRole.getRoleKey() : tmpRole.getId()%>",
                "<%=(tmpRole.getName().toLowerCase().contains("anonymous")) ? LanguageUtil.get(pageContext, "current-user") + " (" + LanguageUtil.get(pageContext, "Everyone") + ")" : tmpRole.getName()+ ((tmpRole.isSystem()) ? " (" + LanguageUtil.get(pageContext, "User") + ")" : "")%>");
            <%}
        }%>
        
		actionAdmin.refreshWhoCanUse();
		
	    // Load action classes into array
        actionClassAdmin.actionClasses = [];
        <%for(WorkflowActionClass subaction : subActions){ %>
            actionClassAdmin.actionClasses.push({id:"<%=subaction.getId()%>",name:"<%=subaction.getName()%>"});
        <%} %>

        // Refresh action classes table
        actionClassAdmin.refreshActionClasses();
	});
</script>


<div class="portlet-main view-actions">
	
	<div dojoType="dijit.form.Form" id="addEditAction" jsId="addEditAction" encType="multipart/form-data" action="/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfActionAjax" method="POST">
		<input type="hidden" name="cmd" value="save">
		<input type="hidden" name="schemeId"	value="<%=UtilMethods.webifyString(scheme.getId())%>">
		<input type="hidden" name="stepId"	    value="<%=UtilMethods.webifyString(stepId)%>">
		<input type="hidden" name="whoCanUse"	id="whoCanUse" value="">
		<input type="hidden" name="actionId"	id="actionId" value="<%=UtilMethods.webifyString(action.getId())%>">

		<div class="container-fluid">
			<%if(UtilMethods.isSet(actionId)) {%>
			<div class="row">
				<div class="col-md-12">
					<p><%=LanguageUtil.get(pageContext, "Action")%> <%=LanguageUtil.get(pageContext, "Id")%>: <strong><%=actionId %></strong></p>
				</div>
			</div>
			<%} %>

			<div class="row">
				<div class="col-xs-5 view-actions__permissions">
					<button dojoType="dijit.form.Button" class="view-actions__back-btn" onClick='mainAdmin.show(stepAdmin.baseJsp + "?schemeId=<%=schemeId%>")'>
						<i class="fa fa-level-up" aria-hidden="true"></i>
					</button>
					<dl class="vertical">
						<dt>
							<h3><%=LanguageUtil.get(pageContext, "About-Action")%></h3>
						</dt>
						
					</dl>
					<dl class="vertical">
						<dt>
							<label for=""><%=LanguageUtil.get(pageContext, "Action-Name")%>:</label>
						</dt>
						<dd>
							<input type="text" name="actionName" id="actionName" style="width: 80%;"
								dojoType="dijit.form.ValidationTextBox" required="true"
								value="<%=UtilMethods.webifyString(action.getName())%>"
								maxlength="255" onkeypress="actionAdmin.doChange()">
						</dd>
					</dl>
					<dl class="vertical">
						<dt>
							<label for=""><%=LanguageUtil.get(pageContext, "Who-can-use-action")%>:</label>
						</dt>
						<dd>
							<input id="actionWhoCanUseSelect" />
							<button dojoType="dijit.form.Button"
								onClick='actionAdmin.addSelectedToWhoCanUse'
								iconClass="addIcon">
								<%=LanguageUtil.get(pageContext, "add")%>
							</button>
						</dd>
					</dl>
					<dl class="vertical">
						<dt></dt>
						<dd>
							<div class="who-can-use view-actions__who-can-use">
								<table class="who-can-use__list" id="whoCanUseTbl">
								</table>
							</div>
						</dd>
					</dl>
					<dl class="vertical">
						<dt></dt>
						<dd>
							<div class="checkbox">
								<input type="checkbox" name="actionRequiresCheckout"
									   id="actionRequiresCheckout" dojoType="dijit.form.CheckBox" value="true"
									<%=(action.requiresCheckout()) ? "checked='true'" : ""%> onClick="actionAdmin.doChange()">
								<label for=""><%=LanguageUtil.get(pageContext, "Requires-Checkout")%></label>
							</div>

							<div class="checkbox">
								<input type="checkbox" name="actionCommentable"
									id="actionCommentable" dojoType="dijit.form.CheckBox" value="true"
									<%=(action.isCommentable()) ? "checked='true'" : ""%> onClick="actionAdmin.doChange()">
								<label for=""><%=LanguageUtil.get(pageContext, "Allow-Comments")%></label>
							</div>
							<div class="checkbox">
								<input type="checkbox" name="actionAssignable"
									id="actionAssignable" dojoType="dijit.form.CheckBox" value="true"
									<%=(action.isAssignable()) ? "checked='true'" : ""%> onClick="actionAdmin.doChange()">
								<label for=""><%=LanguageUtil.get(pageContext, "User-Assignable")%></label>
							</div>
						</dd>
					</dl>
					<dl class="vertical">
						<dt>
							<label for=""><%=LanguageUtil.get(pageContext, "show-on")%>:</label>
						</dt>
						<dd>
							<select name="showOn" id="showOn"  onChange="actionAdmin.doChange()"
									dojoType="dijit.form.FilteringSelect">
								<option value="LOCKED"
										<%=(showLocked) ? "selected='true'" : "" %>>
									<%=LanguageUtil.get(pageContext, "Requires-Checkout-Locked") %>
								</option>
								<option value="UNLOCKED"
										<%=(showUnLocked) ? "selected='true'" : "" %>>
									<%=LanguageUtil.get(pageContext, "Requires-Checkout-Unlocked") %>
								</option>
								<option value="LOCKED,UNLOCKED"
										<%=(showOnBoth) ? "selected='true'" : "" %>>
									<%=LanguageUtil.get(pageContext, "Requires-Checkout-Both") %>
								</option>
							</select>
						</dd>
					</dl>
					<dl class="vertical">
						<dt>
							<label for=""><%=LanguageUtil.get(pageContext, "Assign-To")%>:</label>
						</dt>
						<dd>
							<input id="actionAssignToSelect"  />
							<%--hideHierarchayControl --%>
							<div class="checkbox" id="divRoleHierarchyForAssign" style="display:<%=hideHierarchayControl ? "none" : "block" %>;">
								<input type="checkbox" name="actionRoleHierarchyForAssign" id="actionRoleHierarchyForAssign" dojoType="dijit.form.CheckBox" value="true"
								<%=(action.isRoleHierarchyForAssign()) ? "checked='true'" : ""%> onClick="actionAdmin.doChange()">
								<label for="actionRoleHierarchyForAssign"><%=LanguageUtil.get(pageContext, "Use-Role-Hierarchy")%></label>
							</div>
						</dd>
					</dl>
					<dl class="vertical">
						<dt>
							<label for=""><%=LanguageUtil.get(pageContext, "Next-Step")%>:</label>
						</dt>
						<dd>
							<select name="actionNextStep" id="actionNextStep"  onChange="actionAdmin.doChange()"
								dojoType="dijit.form.FilteringSelect">
									<%if(steps !=null){
										for(WorkflowStep s : steps){ %>
										<option value="<%=s.getId() %>"
											<%=(action != null && s.getId().equals(action.getNextStep())) ? "selected='true'" : "" %>><%=s.getName() %></option>
										<%}%>
									<% }%>
							</select>
						</dd>
					</dl>
					<dl class="vertical">
						<dt>
							<label for=""><%=LanguageUtil.get(pageContext, "Custom-Code")%>:</label>
						</dt>
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
						<dd>
							<textarea id="actionCondition" dojoType="dijit.form.Textarea" style="width:80%;min-height:100px;max-height:100px;height:100px;" name="actionCondition"><%=textValue %></textarea>
						</dd>
					</dl>
				</div>

				<div class="col-xs-5 view-actions__sub-actions">
					<div class="view-actions__arrow-right"> 
						<i class="fa fa-arrow-circle-o-right" aria-hidden="true"></i>
					</div>
					<%if(!action.isNew()){ %>
					<dl class="vertical">
						<dt>
							<h3><%=LanguageUtil.get(pageContext, "Workflow-SubAction")%></h3>
						</dt>
					</dl>
					<dl class="vertical">
						<dt>
							<select name="wfActionlets" id="wfActionlets" dojoType="dijit.form.FilteringSelect">
								<option value=""></option>
								<%for(WorkFlowActionlet a : wapi.findActionlets()){%>
									<option value="<%=a.getClass().getCanonicalName()%>"><%=a.getName() %></option>
								<%} %>
							</select>
							<button dojoType="dijit.form.Button"
							onClick="actionClassAdmin.addSelectedToActionClasses();" style="width:auto;" iconClass="addIcon">
							<%=LanguageUtil.get(pageContext, "Add-Workflow-SubAction")%>
							</button>
						</dt>
					</dl>
					<dl class="vertical">
						<dt>
							<table id="actionletsTbl" class="listingTable">
								<tbody id="actionletsTblBody">
								</tbody>
							</table>
						</dt>
					</dl>
					<%} %>
				</div>
				<div class="col-xs-2 view-actions__workflow-actions">
					<div class="view-actions__arrow-right"> 
						<i class="fa fa-arrow-circle-o-right" aria-hidden="true"></i>
					</div>
					<div class="content-edit-actions">
						<div>
							<%if(action!=null && !action.isNew()) {%>
							<a id="deleteButtonDiv" class="saveButtonHide" onClick="actionAdmin.deleteAction('<%=action.getId() %>');">
								<%=LanguageUtil.get(pageContext, "Delete")%>
							</a>
							<%} %>
							<a id="saveButtonDiv" class="saveButtonHide" onClick="actionAdmin.saveAction('<%=schemeId %>');">
								<%=LanguageUtil.get(pageContext, "Save")%>
							</a>
							<a onClick='mainAdmin.show(stepAdmin.baseJsp + "?schemeId=<%=schemeId%>")'>
								<%=LanguageUtil.get(pageContext, "Cancel")%>
							</a>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>






