<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowAction"%>
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
	final StringBuilder actionsDropDownOptions
			= new StringBuilder("<option value='new'>New Action</option>"); // todo: i18n

	WorkflowAPI wapi = APILocator.getWorkflowAPI();
	String schemeId  = request.getParameter("schemeId");
	WorkflowScheme defaultScheme   = wapi.findDefaultScheme();
	WorkflowScheme scheme          = wapi.findScheme(schemeId);
	final List<WorkflowStep> steps = wapi.findSteps(scheme);
	final List<WorkflowAction> schemaActions =
			wapi.findActions(scheme, APILocator.getUserAPI().getSystemUser());
	WorkflowAction entryAction = null;
	if(scheme.isMandatory() && UtilMethods.isSet(scheme.getEntryActionId())){
		try{
			entryAction = wapi.findAction(scheme.getEntryActionId(), APILocator.getUserAPI().getSystemUser());
	
		}catch(Exception e){
			
		}
	}
	
	
	
%>


<script type="text/javascript">





	dojo.ready(function(){
		mainAdmin.resetCrumbTrail();
		mainAdmin.addCrumbtrail("<%=LanguageUtil.get(pageContext, "Workflows")%>", "/html/portlet/ext/workflows/schemes/view_schemes.jsp");
		mainAdmin.addCrumbtrail("<%=LanguageUtil.get(pageContext, "Scheme")%> : <%=(scheme.isArchived()) ? "<strike>" :""%><%=scheme.getName()%><%=(scheme.isArchived()) ? "</strike>" :""%>", "/html/portlet/ext/workflows/schemes/view_schemes.jsp<%=(scheme.isArchived()) ? "?showArchived=1" : ""%>");
		
		mainAdmin.addCrumbtrail("<%=LanguageUtil.get(pageContext, "Steps")%>", stepAdmin.baseJsp + "?schemeId=<%=schemeId%>");
		mainAdmin.refreshCrumbtrail();	
		

		dojo.query(".wfActionList").forEach(
			function(selectedTag){
				var source = new dojo.dnd.Source(selectedTag);
				dojo.connect(source, "onDropInternal",this, actionAdmin.reorderAction);

			}
		);
		
		// the action class reordering was interferring with this reordering 
		if(actionClassAdmin.dndHandle){
			dojo.disconnect(actionClassAdmin.dndHandle);
			actionClassAdmin.dndHandle = null;
			
		
		}
		
	});
	
	
	

</script>

<div class="portlet-main">
	<!-- START Toolbar -->
	<div class="portlet-toolbar">
		<div class="portlet-toolbar__actions-primary">
			<strong><%=LanguageUtil.get(pageContext, "Workflow-Scheme")%></strong>
		</div>

    	<div class="portlet-toolbar__actions-secondary">
    		<button dojoType="dijit.form.Button" iconClass="editIcon" onClick="schemeAdmin.showAddEdit('<%=scheme.getId()%>');">
				<%=LanguageUtil.get(pageContext, "Edit-Workflow-Scheme")%>
			</button>
		</div>
	</div>
	<!-- END Toolbar -->
		
	<table class="listingTable showPointer" onClick="schemeAdmin.showAddEdit('<%=scheme.getId()%>');">
		<input type="hidden" name="cmd" value="save">
		<input type="hidden" name="schemeId" value="<%=UtilMethods.webifyString(scheme.getId())%>">
		<tr>
			<th style="width:150px;"><%=LanguageUtil.get(pageContext, "Name")%>:</th>
			<th>
				<%=UtilMethods.webifyString(scheme.getName())%>
			</th>
		</tr>
		<tr>
			<td><%=LanguageUtil.get(pageContext, "Description")%>:</td>
			<td><%=UtilMethods.webifyString(scheme.getDescription())%>
			</td>
		</tr>
		<tr>
			<td><%=LanguageUtil.get(pageContext, "Archived")%>:</td>
			<td><%=(scheme.isArchived()) ? LanguageUtil.get(pageContext, "Yes") : LanguageUtil.get(pageContext, "No")%>
			</td>
		</tr>
		<tr>
			<td><%=LanguageUtil.get(pageContext, "Mandatory")%>:</td>
			<td>
				<%=(scheme.isMandatory()) ? LanguageUtil.get(pageContext, "Yes") : LanguageUtil.get(pageContext, "No")%>

			</td>
		</tr>
		<%if(scheme.isMandatory() && entryAction !=null){ %>
			<tr>
				<td><%=LanguageUtil.get(pageContext, "Default-Initial-Action")%>:</td>
				<td>
					&quot;<%=entryAction.getName() %>&quot;

				</td>
			</tr>
		<%} %>
		
	</table>
</div>

<div class="portlet-main">
	<!-- START Toolbar -->
	<div class="portlet-toolbar">
		<div class="portlet-toolbar__actions-primary"></div>

    	<div class="portlet-toolbar__actions-secondary">
    		<form id="fm" method="post">
	    		<div id="dropdownButtonContainer"></div>
					<script>
			            dojo.addOnLoad(function() {
			                var dialog = new dijit.TooltipDialog({
			                    content: '<div class="inline-form"><%=LanguageUtil.get(pageContext, "Name")%>:&nbsp;<input type="text" name="stepName" id="stepName" dojoType="dijit.form.ValidationTextBox"  required="true" value="" maxlength="255">&nbsp;<button dojoType="dijit.form.Button" onClick="stepAdmin.addStep()" iconClass="addIcon" id="Save-new-step"><%=LanguageUtil.get(pageContext, "Add")%></button></div>',
			                    onKeyPress:function(e){
			                    	if(e.keyCode==13){
			                    		stepAdmin.addStep();
			                    	}
		                    	}
			                });
		
			                var button = new dijit.form.DropDownButton({
			                    label: "<%=LanguageUtil.get(pageContext, "Add-Workflow-Step")%>",
			                    dropDown: dialog,
			                    iconClass:"addIcon",
			                    onClick:function(){
			                    	stepAdmin.schemeId = '<%=schemeId%>';
			                    	
			                    },
			                    
			                    
			                });
			                dojo.byId("dropdownButtonContainer").appendChild(button.domNode);
			              
			            });
					</script>
				</div>
			</form>
    	</div>
   </div>
   <!-- END Toolbar -->
</div>

<!-- schema actions -->
<div id="wfStepsBoundingBoxMain" >

	<div class="wfStepBoundingBox" >
		<div class="wfStepTitle ">
			<div  style="float:left;width:89%;" class="showPointer wfStepTitleDivs">
				<span style="border-bottom:dotted 1px gray;">Schema Workflow Actions</span>
			</div>
			<div class="clear"></div>
		</div>
		<table class="wfActionList" id="<%= "jsNode" + scheme.getId()  %>" dojoType="dojo.dnd.Source" class="dndContainer container" accept="actionOrderClass<%=scheme.getId()%>">
			<tbody>

			<%for(WorkflowAction action : schemaActions){

				actionsDropDownOptions.append("<option value='").append(action.getId()).append("'>").append(action.getName()).append("</option>");
			%>
			<tr class="dojoDndItem actionOrderClass<%=scheme.getId()%> actionOrderClass" id="id_<%=action.getId()%>_<%=scheme.getId()%>">
				<td class="wfXBox showPointer" onclick="actionAdmin.deleteAction('<%=action.getId()%>')"><span class="deleteIcon"></span></td>
				<td onClick="actionAdmin.viewAction('<%=scheme.getId()%>', '<%=action.getId() %>');" class="showPointer">
					<span class="<%=action.getIcon()%>"></span>
					<%=action.getName() %>
					<span style="color:#a6a6a6">&#8227; <%=wapi.findStep(action.getNextStep()).getName() %></span>
					<%if(action.requiresCheckout()){ %><div title="<%=LanguageUtil.get(pageContext, "Save-content")%>: (<%=LanguageUtil.get(pageContext, "Requires-Checkout")%>)" style="float:right;opacity:0.45;"><span class="saveIcon"></span></div><%} %>
				</td>
			</tr>
			<%} %>
			</tbody>
		</table>

	</div>

</div>

<hr/>

<div id="wfStepsBoundingBoxMain" >
	
	<%for(WorkflowStep step : steps){ %>
		
		<%List<WorkflowAction> actions = wapi.findActions(step, APILocator.getUserAPI().getSystemUser());%>
		<div class="wfStepBoundingBox" >
			<div class="wfStepTitle ">
				<div  style="float:left;width:89%;" class="showPointer wfStepTitleDivs" onClick="stepAdmin.showStepEdit('<%=step.getId()%>')">
					<span style="border-bottom:dotted 1px gray;"><%=step.getName() %></span>
					<span style="font-weight:normal;padding:5px;display:inline-block;">
						<%=step.isResolved() ? "(" +  LanguageUtil.get(pageContext, "resolved") + ")" : "" %>
					</span>
				</div>
				<div style="float:right; width:10%;text-align: right" class="wfStepTitleDivs showPointer" onclick="stepAdmin.deleteStep('<%=step.getId()%>')"><span class="deleteIcon"></span></div>
				<div class="clear"></div>
			</div>
			<table class="wfActionList" id="<%= "jsNode" + step.getId()  %>" dojoType="dojo.dnd.Source" class="dndContainer container" accept="actionOrderClass<%=step.getId()%>">
				<tbody>

					<%for(WorkflowAction action : actions){ %>
						<tr class="dojoDndItem actionOrderClass<%=step.getId()%> actionOrderClass" id="id_<%=action.getId()%>_<%=step.getId()%>">
							<td class="wfXBox showPointer" onclick="actionAdmin.deleteActionForStep('<%=action.getId()%>','<%=step.getId()%>')"><span class="deleteIcon"></span></td>
							<td onClick="actionAdmin.viewAction('<%=scheme.getId()%>', '<%=action.getId() %>');" class="showPointer">
								<span class="<%=action.getIcon()%>"></span>
								<%=action.getName() %> 
								<span style="color:#a6a6a6">&#8227; <%=wapi.findStep(action.getNextStep()).getName() %></span>
								<%if(action.requiresCheckout()){ %><div title="<%=LanguageUtil.get(pageContext, "Save-content")%>: (<%=LanguageUtil.get(pageContext, "Requires-Checkout")%>)" style="float:right;opacity:0.45;"><span class="saveIcon"></span></div><%} %>
							</td>
						</tr>
					<%} %>
				</tbody>
			</table>


			<div class="wfAddActionButtonRow">
				<select name="step-action-<%=step.getId()%>" id="step-action-<%=step.getId()%>">
					<%=actionsDropDownOptions.toString()%>
				</select>

				<button dojoType="dijit.form.Button"
				 onClick="actionAdmin.addOrAssociatedAction('<%=scheme.getId()%>', '<%=step.getId()%>', 'step-action-<%=step.getId()%>');" iconClass="addIcon">
				<%=LanguageUtil.get(pageContext, "Add-Workflow-Action")%>
				</button>
			</div>
		</div>

	<%} %>
</div>



