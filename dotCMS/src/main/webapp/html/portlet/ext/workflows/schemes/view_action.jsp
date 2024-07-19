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
<%@ page import="java.util.Collections" %>
<%@ page import="com.dotmarketing.portlets.contentlet.util.ActionletUtil" %>

<%
	WorkflowAPI wapi = APILocator.getWorkflowAPI();
	String schemeId  = request.getParameter("schemeId");
	String stepId    = request.getParameter("stepId");
	String actionId  = request.getParameter("actionId");
    final String NEW_ACTION ="new";
	WorkflowAction action = new WorkflowAction();
	List<WorkflowActionClass> subActions = Collections.emptyList();
	Role nextAssignRole = new Role();
	boolean hideHierarchayControl = true;
	final Role cmsAnonRole=APILocator.getRoleAPI().loadCMSAnonymousRole();
	if(UtilMethods.isSet(actionId) && !NEW_ACTION.equals(actionId)) {
		try {
			action = wapi.findAction(actionId, APILocator.getUserAPI().getSystemUser());
			nextAssignRole = APILocator.getRoleAPI().loadRoleById(action.getNextAssign());
			if (UtilMethods.isSet(nextAssignRole) && UtilMethods.isSet(nextAssignRole.getId())) {
				if (!nextAssignRole.isUser() && !nextAssignRole
						.equals(cmsAnonRole)) {
					if (action.isAssignable()) {
						hideHierarchayControl = false;
					}
				}
			}

			if (schemeId == null) {
				schemeId = action.getSchemeId();
			}
		} catch (Exception e) {
			Logger.debug(this.getClass(), "can't find action");
		}

		subActions = wapi.findActionClasses(action);
	}else{
	    action.setNextAssign(cmsAnonRole.getId());
	}

	final WorkflowScheme scheme = wapi.findScheme(schemeId);

	List<WorkflowStep> steps = wapi.findSteps(scheme);

	boolean showLocked      = false;
	boolean showUnLocked    = false;
	boolean showNew		    = false;
	boolean showPublished   = false;
	boolean showUnpublished = false;
	boolean showArchive		= false;
    boolean showListing     = false;
    boolean showEditing     = false;
	if (null != action) {

		showLocked      = action.shouldShowOnLock();
		showUnLocked    = action.shouldShowOnUnlock();
		showNew 	    = action.shouldShowOnNew();
		showPublished   = action.shouldShowOnPublished();
		showUnpublished = action.shouldShowOnUnpublished();
		showArchive		= action.shouldShowOnArchived();
		showListing     = action.shouldShowOnListing();
		showEditing     = action.shouldShowOnEdit();
	}

	final boolean showAll	= showNew && showPublished && showUnpublished && showArchive;
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

        dojo.addOnLoad(function(){
            dojo.connect(dijit.byId("whoCanUseSelect"), 'onChange', function(event){

                actionAdmin.addSelectedToWhoCanUse();
            });
        });
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
        <% Set<Role> roles = APILocator.getPermissionAPI().getRolesWithPermission(action, PermissionAPI.PERMISSION_USE);
           List<Role> workflowRoles = APILocator.getRoleAPI().findWorkflowSpecialRoles();
        %>
        <%for(Role tmpRole :  roles){
            if (UtilMethods.isSet(tmpRole) && UtilMethods.isSet(tmpRole.getId()) ) {%>
        actionAdmin.addToWhoCanUse("<%=(tmpRole.isSystem()) ? tmpRole.getRoleKey() : tmpRole.getId()%>",
            "<%=(tmpRole.getName().toLowerCase().contains("anonymous")) ? LanguageUtil.get(pageContext, "current-user") + " (" + LanguageUtil.get(pageContext, "Everyone") + ")" : tmpRole.getName()+ ((tmpRole.isSystem() && !workflowRoles.contains(tmpRole) ) ? " (" + LanguageUtil.get(pageContext, "User") + ")" : "")%>");
        <%}
    }%>

        actionAdmin.refreshWhoCanUse();

        // Load action classes into array
        actionClassAdmin.actionClasses = [];
        <%

        boolean hasOnlyBatch = false;
        for(WorkflowActionClass subaction : subActions) {

            boolean isOnlyBatch = ActionletUtil.isOnlyBatch(subaction.getClazz());
			hasOnlyBatch |= isOnlyBatch;
        %>
        actionClassAdmin.actionClasses.push({id:"<%=subaction.getId()%>",name:"<%=subaction.getName()%>", isOnlyBatch:<%=isOnlyBatch%>});
        <%}
		if (hasOnlyBatch) {
        %>

		setTimeout(() => {
			actionClassAdmin.disableShowOnEditing();
		}, "2000");

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
			<%if(UtilMethods.isSet(actionId) && (!"new".equals(actionId))) {%>
			<div class="row">
				<div class="col-md-12">
					<p>
						<%=LanguageUtil.get(pageContext, "Action")%> <%=LanguageUtil.get(pageContext, "Id")%>:

						<strong>
							<a onclick="this.parentNode.innerHTML='<%=actionId%>'; return false;" href="#"><%=APILocator.getShortyAPI().shortify(actionId) %></a>
						</strong>

						(<a href="/api/v1/workflow/actions/<%=actionId%>" target="_blank">json</a>)
					</p>
				</div>
			</div>
			<%} %>

			<main class="row" >
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
							<label for="actionName"><%=LanguageUtil.get(pageContext, "Name")%>:</label>
						</dt>
						<dd>
							<input type="text" name="actionName" id="actionName" style="width: 80%;" 
								   dojoType="dijit.form.ValidationTextBox" required="true"
								   value="<%=UtilMethods.webifyString(action.getName())%>"
								   maxlength="255" onkeypress="actionAdmin.doChange()" <%if(action.isNew()){ %>onchange="actionAdmin.saveAction('<%=schemeId %>');"<%} %>>
						</dd>
					</dl>
					
					<dl class="vertical">
						<dt>
							<label for="actionNextStep"><%=LanguageUtil.get(pageContext, "Next-Step")%>:</label>
						</dt>
						<dd>
							<select name="actionNextStep" id="actionNextStep"  onChange="actionAdmin.doChange()" style="width: 50%;"  labelType="html"
									dojoType="dijit.form.FilteringSelect">

								<option value="<%=WorkflowAction.CURRENT_STEP %>"
										<%=(action != null && action.isNextStepCurrentStep()) ? "selected='true'" : "" %>>
									<%=LanguageUtil.get(pageContext, "Current-Step")%>
								</option>

								<%if(steps !=null){

									for(WorkflowStep s : steps){ %>
										<option value="<%=s.getId() %>"
										<%=(action != null && s.getId().equals(action.getNextStep())) ? "selected='true'" : "" %>><%=s.getName() %></option>
								<%}%>
								<% }%>
							</select>
						</dd>
					</dl>
					<br> <br> 
					
					<dl class="vertical">
						<fieldset style="width:80%">
							<legend><%=LanguageUtil.get(pageContext, "Who-can-use-action")%></legend>
							<dt></dt>
							<dd>
								<input id="actionWhoCanUseSelect"/>
								<button dojoType="dijit.form.Button"
										onClick='actionAdmin.addSelectedToWhoCanUse'
										iconClass="addIcon">
									<%=LanguageUtil.get(pageContext, "add")%>
								</button>
							</dd>
		
							<dt></dt>
							<dd>
								
								<table class="who-can-use__list" id="whoCanUseTbl">
								</table>
								
							</dd>
						
						</fieldset>
					</dl>
					<dl class="vertical">
						<fieldset style="width:80%">
							<legend><%=LanguageUtil.get(pageContext, "show-when")%></legend>
							<div class="checkbox">
                                <input type="checkbox" name="showOn" id="showOnEDITING" dojoType="dijit.form.CheckBox"   value="EDITING"        onclick="actionAdmin.doChange()"   <%=(showEditing)?     "checked" : "" %>/>
                                <label for="showOnEDITING"><%=LanguageUtil.get(pageContext, "Editing") %></label>
                                &nbsp; &nbsp;
                                <input type="checkbox" name="showOn" id="showOnLISTING"  dojoType="dijit.form.CheckBox"   value="LISTING"      onclick="actionAdmin.doChange()"   <%=(showListing)?   "checked" : "" %>/>
                                <label for="showOnLISTING"><%=LanguageUtil.get(pageContext, "Listing") %></label>
                            </div>
                                <div style="padding:10px;font-style: italic;">AND</div>
							
							
							
							<div class="checkbox">
								<input type="checkbox" name="showOn" id="showOnLOCKED" dojoType="dijit.form.CheckBox"   value="LOCKED"        onclick="actionAdmin.doChange()"   <%=(showLocked)?     "checked" : "" %>/>
								<label for="showOnLOCKED"><%=LanguageUtil.get(pageContext, "Requires-Checkout-Locked") %></label>
								&nbsp; &nbsp;
								<input type="checkbox" name="showOn" id="showOnUNLOCKED"  dojoType="dijit.form.CheckBox"   value="UNLOCKED"      onclick="actionAdmin.doChange()"   <%=(showUnLocked)?   "checked" : "" %>/>
								<label for="showOnUNLOCKED"><%=LanguageUtil.get(pageContext, "Requires-Checkout-Unlocked") %></label>
							</div>
								<div style="padding:10px;font-style: italic;">AND</div>
							<div class="checkbox">
								<input type="checkbox" name="showOn" id="showOnNEW"   dojoType="dijit.form.CheckBox"   value="NEW"         onclick="actionAdmin.doChange()"   <%=(showNew)? 		 "checked" : "" %>/>
								<label for="showOnNEW"><%=LanguageUtil.get(pageContext, "new") %></label>
								&nbsp; &nbsp;
								<input type="checkbox" name="showOn" id="showOnPUBLISHED"  dojoType="dijit.form.CheckBox"   value="PUBLISHED"   onclick="actionAdmin.doChange()"   <%=(showPublished)?   "checked" : "" %>/>
								<label for="showOnPUBLISHED"><%=LanguageUtil.get(pageContext, "Published") %></label>
								&nbsp; &nbsp;
								<input type="checkbox" name="showOn" id="showOnUNPUBLISHED"  dojoType="dijit.form.CheckBox"   value="UNPUBLISHED" onclick="actionAdmin.doChange()"   <%=(showUnpublished)? "checked" : "" %>/>
								<label for="showOnUNPUBLISHED"><%=LanguageUtil.get(pageContext, "Unpublished") %></label>
								&nbsp; &nbsp;
								<input type="checkbox" name="showOn" id="showOnARCHIVED"  dojoType="dijit.form.CheckBox"   value="ARCHIVED"    onclick="actionAdmin.doChange()"   <%=(showArchive)? 	 "checked" : "" %>/>
								<label for="showOnARCHIVED"><%=LanguageUtil.get(pageContext, "Archived") %></label>
								&nbsp; &nbsp;
								<input type="checkbox" name="showOnAll" id="showOnALL" dojoType="dijit.form.CheckBox"  value="ALL"         onChange="var check=this.checked; document.getElementsByName('showOn').forEach(function(checkbox) {dijit.byId(checkbox.id).set('checked', check);}); return true;"   <%=(showAll)? 		 "checked" : "" %>/>
								<label for="showOnALL"><%=LanguageUtil.get(pageContext, "All") %></label>
	
							</div>
						</fieldset>
					</dl>
					<dl class="vertical">
						<fieldset style="width:80%">
							<legend><%=LanguageUtil.get(pageContext, "Comments-and-Assigns")%></legend>

							<dt></dt>
							<dd>
	
								<div class="checkbox">
									<input type="checkbox" name="actionAssignable"
										   id="actionAssignable" dojoType="dijit.form.CheckBox" value="true"
										<%=(action.isAssignable()) ? "checked='true'" : ""%> onClick="actionAdmin.doChange()">
									<label for="actionAssignable"><%=LanguageUtil.get(pageContext, "User-Assignable")%></label>
									
									&nbsp; &nbsp; &nbsp;
									<input type="checkbox" name="actionCommentable"
										   id="actionCommentable" dojoType="dijit.form.CheckBox" value="true"
										<%=(action.isCommentable()) ? "checked='true'" : ""%> onClick="actionAdmin.doChange()">
									<label for="actionCommentable"><%=LanguageUtil.get(pageContext, "Allow-Comments")%></label>
				
								</div>
							</dd>
					
							<dt>
								<label for=""><%=LanguageUtil.get(pageContext, "Assign-To")%>:</label>
							</dt>
							<dd>
								<input id="actionAssignToSelect"  />
								<%--hideHierarchayControl --%>
								<div class="checkbox" id="divRoleHierarchyForAssign" style="padding:10px;display:<%=hideHierarchayControl ? "none" : "block" %>;">
									<input type="checkbox" name="actionRoleHierarchyForAssign" id="actionRoleHierarchyForAssign" dojoType="dijit.form.CheckBox" value="true"
										<%=(action.isRoleHierarchyForAssign()) ? "checked='true'" : ""%> onClick="actionAdmin.doChange()">
									<label for="actionRoleHierarchyForAssign"><%=LanguageUtil.get(pageContext, "Use-Role-Hierarchy")%></label>
								</div>
							</dd>
						</fieldset>
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
							<select name="wfActionlets" id="wfActionlets" 
							dojoType="dijit.form.FilteringSelect" 
                            autocomplete="false"
                            onChange="actionClassAdmin.addSelectedToActionClasses()">
								<option value=""></option>
								<%for(WorkFlowActionlet a : wapi.findActionlets()){%>
								<option value="<%=a.getClass().getCanonicalName()%>"><%=a.getName()%></option>
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
							<a id="saveButtonDiv" class="saveButtonHide" onClick="actionAdmin.saveAction('<%=schemeId %>');">
								<%=LanguageUtil.get(pageContext, "Save")%>
							</a>
							<a onClick='mainAdmin.show(stepAdmin.baseJsp + "?schemeId=<%=schemeId%>")'>
								<%=LanguageUtil.get(pageContext, "Cancel")%>
							</a>
						</div>
					</div>
				</div>
			</main>
		</div>
	</div>
</div>


