<%@page
	import="com.dotmarketing.portlets.workflows.business.WorkflowAPI"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowStep"%>
<%@page
	import="com.dotmarketing.portlets.workflows.model.WorkflowScheme"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%



	WorkflowAPI wapi = APILocator.getWorkflowAPI();
	WorkflowScheme defaultScheme = wapi.findDefaultScheme();
	boolean showArchived = (request.getParameter("showArchived") != null);

	List<WorkflowScheme> schemes = wapi.findSchemes(showArchived);
	
	
	
%>



<div class="yui-g portlet-toolbar" style="margin:0 0 5px 10px;">
	<form id="fm" method="post">
		<div class="yui-u first" style="white-space: nowrap">
			<b><%=LanguageUtil.get(pageContext, "Workflow-Schemes")%></b>
		</div>
		<div class="yui-u" style="text-align: right;">
		
			<input type="checkbox" id="showArchivedChk" name="showArchived"
				<%=(showArchived) ? "checked='true'" : ""%> id="system"
				dojoType="dijit.form.CheckBox" value="1"
				onClick="new function(){schemeAdmin.showArchived = <%=!showArchived%>;schemeAdmin.show();}" />
			<label font-size:85%; for="showArchivedChk"><%=LanguageUtil.get(pageContext, "Show-Archived")%></label>

			&nbsp;&nbsp;&nbsp;
			<button dojoType="dijit.form.Button"
				onClick="schemeAdmin.showAddEdit()" iconClass="addIcon">
				<%=LanguageUtil.get(pageContext, "Add-Workflow-Scheme")%>
			</button>
		</div>
	</form>
</div>

<!-- END Toolbar -->

<!-- START Listing Results -->


	<%if (schemes != null && schemes.size() > 0) {%>


		<%for (WorkflowScheme scheme : schemes) {%>
			<div class="editRow showPointer">
			<table class="listingTable" id="td<%=scheme.getId()%>" style="margin:0 0 25px 0;" onclick="stepAdmin.showViewSteps('<%=scheme.getId()%>');">
				<%List<WorkflowStep> steps = wapi.findSteps(scheme);%>
				<tr>
					<th>
						<%if(!scheme.isArchived()){ %>
							<span class="workflowIcon"></span> <%=scheme.getName()%>
						<%}else{ %>
							<strike><%=scheme.getName()%></strike>
						<%} %>
						<%if(scheme.isMandatory()){ %>(<%=LanguageUtil.get(pageContext, "Mandatory")%>)<%} %>
						<div style="font-weight:normal;margin-left:25px;font-size:85%;"><%=UtilMethods.webifyString(scheme.getDescription())%></div>
					</th>
				</tr>	
				<tr>
					<td>	
						<ol class="wfStepsList">
							<%if(steps!= null && steps.size() > 0){ %>

								<%for(WorkflowStep step : steps){ %>
									<li><%=step.getName() %></li>
								<%} %>
									
							<%}else{ %>
								<li><%=LanguageUtil.get(pageContext, "No-steps-have-been-created")%></li>
							<%} %>
						</ol>	
					</td>
				</tr>
			</table>
			</div>
			
			<div dojoType="dijit.Menu" class="dotContextMenu"  style="display: none;" targetNodeIds="td<%=scheme.getId()%>">
				<div dojoType="dijit.MenuItem" iconClass="previewIcon" onClick="stepAdmin.showViewSteps('<%=scheme.getId()%>');"><%=LanguageUtil.get(pageContext, "View-Steps")%></div>
				<div dojoType="dijit.MenuItem" iconClass="editIcon" onClick="schemeAdmin.showAddEdit('<%=scheme.getId()%>');"><%=LanguageUtil.get(pageContext, "Edit-Workflow")%></div>
			</div>
			
			
			
		<%}%>

	<%} else {%>
		<table class="listingTable">
			<tr>
				<td><%=LanguageUtil.get(pageContext, "No-Workflow-Schemes")%><br /></td>
			</tr>
		</table>
	<%}%>


<script>
dojo.ready(function(){
	mainAdmin.resetCrumbTrail();
	mainAdmin.addCrumbtrail("<%=LanguageUtil.get(pageContext, "Workflows")%>", schemeAdmin.baseJsp);
	mainAdmin.addCrumbtrail("<%=LanguageUtil.get(pageContext, "Schemes")%>", schemeAdmin.baseJsp);
	
	mainAdmin.refreshCrumbtrail();
	
	
});


</script>
