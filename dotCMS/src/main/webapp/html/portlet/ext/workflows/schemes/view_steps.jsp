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
	if( UtilMethods.isSet(scheme.getEntryActionId())){
		try{
			entryAction = wapi.findAction(scheme.getEntryActionId(), APILocator.getUserAPI().getSystemUser());
	
		}catch(Exception e){
			
		}
	}
%>

<script type="text/javascript">


	dragula([document.querySelectorAll('.wfStepInDrag'), document.getElementById('wfStepInDragContainer')], {
		  moves: function (el, source, handle, sibling) {

			  return handle.classList.contains('handle'); 
		  },
		  accepts: function (el, target, source, sibling) {
		    return target ==document.getElementById('wfStepInDragContainer');
		  },
		  direction: 'horizontal',             // Y axis is considered when determining where an element would be dropped
		  revertOnSpill: false,              // spilling will put the element back where it was dragged from, if this is true
		  ignoreInputTextSelection: true     // allows users to select input text, see details below
		}).on('drop', function (el) {
		  	var stepID = el.id.split("stepID").join("");
			let sibblings = el.parentNode.children
			let index = Array.from(sibblings).indexOf(el)
		  	console.log("moving: " + stepID + " to " + index);
			stepAdmin.reorderStep(stepID, index);
		 });
	
	
		var arr = new Array(<%=steps.size()%>);
		<%for(WorkflowStep step : steps){ %>
			arr.push(document.getElementById("jsNode<%=step.getId()%>"));
		<%}%>
		
		
	  dragula(arr, {
		  moves: function (el, source, handle, sibling) {

			  return el.classList.contains('wf-action-wrapper'); 
		  },
		  accepts: function (el, target, source, sibling) {

		    return true;
		  },
		  revertOnSpill: true,    
		  copy: true,                       // elements are moved by default, not copied
		  copySortSource: true,             // elements in copy-source containers can be reordered
		  
		}).on('drop', function (ele) {

			actionAdmin.copyOrReorderAction(ele);
		 });


</script>



<div class="portlet-toolbar">

	<div class="portlet-toolbar__actions-primary showPointer" onClick="schemeAdmin.showAddEdit('<%=scheme.getId()%>');">
		<input type="hidden" name="cmd" value="save">
		<input type="hidden" name="schemeId" value="<%=UtilMethods.webifyString(scheme.getId())%>">
		<div>
			<h2 style="border-bottom:dotted 1px gray;"><%=UtilMethods.webifyString(scheme.getName())%> &nbsp; &nbsp; <span class="editIcon" style="float: right;"></span></h2>
			<p><%=UtilMethods.webifyString(scheme.getDescription())%></p>
		</div>
	</div>

	<div class="portlet-toolbar__info"></div>

	<div class="portlet-toolbar__actions-secondary">
		<!-- ADD STEP -->

	   <!-- ADD STEP -->
	</div>
</div>



<!-- Workflow Steps -->
<div class="board-wrapper">
	<div class="board-main-content">
		<div class="board-canvas">
			<div class="" id="wfStepInDragContainer">
				
				<%for(WorkflowStep step : steps){ %>
					<%List<WorkflowAction> actions = wapi.findActions(step, APILocator.getUserAPI().getSystemUser());%>
					<div class="list-wrapper wfStepInDrag" id="stepID<%=step.getId()%>">	
						<div class="list-item">
							<div class="wfStepTitle">
								<div class="showPointer wfStepTitleDivs handle" onClick="stepAdmin.showStepEdit('<%=step.getId()%>')">
									<span style="border-bottom:dotted 1px #fff;"><%=step.getName() %></span>
									<span style="font-weight:normal;display:inline-block;">
										<%=step.isResolved() ? "(" +  LanguageUtil.get(pageContext, "resolved") + ")" : "" %>
									</span>
								</div>
								<div class="clear"></div>
							</div>
							<div class="wfActionList" id="jsNode<%=step.getId()  %>"  data-wfstep-id="<%=step.getId()%>">
									<%for(WorkflowAction action : actions){ %>
										<div class="wf-action-wrapper x<%=action.getId()%>" data-wfaction-id="<%=action.getId()%>">
											<div class="handles"></div>
											<div class="wf-action showPointer">
												<div class="pull-right showPointer" onclick="actionAdmin.deleteActionForStep(this)"><span class="deleteIcon"></span></div>
												<div  class="pull-left showPointer" onClick="actionAdmin.viewAction('<%=scheme.getId()%>', '<%=action.getId() %>');">
													<%=action.getName() %> <span style="color:#a6a6a6">&#8227; <%=(WorkflowAction.CURRENT_STEP.equals(action.getNextStep())) ?  WorkflowAction.CURRENT_STEP : wapi.findStep(action.getNextStep()).getName() %></span>
												</div>
											</div>
										</div>
									<%} %>
							</div>

							<div class="btn-flat-wrapper">
									<div class="btn-flat showPointer" onclick="stepAdmin.deleteStep('<%=step.getId()%>')">Delete</div>
									<div class="btn-flat btn-primary showPointer" onclick="actionAdmin.addOrAssociatedAction('<%=scheme.getId()%>', '<%=step.getId()%>', 'step-action-<%=step.getId()%>');">
										<i class="fa fa-plus" aria-hidden="true"></i> Add
									</div>
							</div>
						</div>
					</div>
				<%}%>
				<div class="list-wrapper showPointer ghostAddDiv" onclick="stepAdmin.schemeId='<%=schemeId%>';stepAdmin.showAddNewStep();" >	
					<div class="list-item">
						<div class="wfStepTitle">
							<div class="wfStepTitleDivs">
								<span style="border-bottom:dotted 0px #fff;"><%=LanguageUtil.get(pageContext, "Add-Workflow-Step")%></span>
							</div>
							<div class="clear"></div>
						</div>
						<div class="wfActionList">
						
						</div>
					</div>
				</div>
				
				
			</div>
		</div>
	</div>
</div>
<script>


</script>