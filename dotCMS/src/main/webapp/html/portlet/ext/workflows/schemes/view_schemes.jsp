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
	boolean showArchived = (request.getParameter("showArchived") != null);
	List<WorkflowScheme> schemes = wapi.findSchemes(showArchived);

%>


<div class="portlet-main">

	<!-- START Toolbar -->
	<form id="fm" method="post">
		<div class="portlet-toolbar" >
			<div class="showPointer" onClick="schemeAdmin.show()"  style="float: left;">
				<h2 style="border-bottom:dotted 1px white;"><%=LanguageUtil.get(pageContext, "Workflow-Schemes")%></h2>
			</div>
			<div style="float: right;padding-right:10px;">

				<input type="checkbox" id="showArchivedChk" name="showArchived"
						<%=(showArchived) ? "checked='true'" : ""%> id="system"
					   dojoType="dijit.form.CheckBox" value="1"
					   onClick="new function(){schemeAdmin.showArchived = <%=!showArchived%>;schemeAdmin.show();}" />

				<label for="showArchivedChk"><%=LanguageUtil.get(pageContext, "Show-Archived")%></label>
				&nbsp; &nbsp;

				<button dojoType="dijit.form.Button"
						onClick="schemeAdmin.showImport();return false;" iconClass="addIcon">
					<%=LanguageUtil.get(pageContext, "Import-Workflow-Scheme")%>
				</button>
				<button dojoType="dijit.form.Button"
						onClick="schemeAdmin.showAddEdit();return false;" iconClass="addIcon">
					<%=LanguageUtil.get(pageContext, "Add-Workflow-Scheme")%>
				</button>
			</div>
		</div>
	</form>
	<!-- END Toolbar -->
</div>



<!-- START Listing Results -->

<div class="board-wrapper">
	<div class="flex-container flex-wrap">
		<%if (schemes != null && schemes.size() > 0) {%>
		<%for (WorkflowScheme scheme : schemes) {%>
		<div class="flex-item editRow showPointer">
			<div id="td<%=scheme.getId()%>" onclick="stepAdmin.showViewSteps('<%=scheme.getId()%>');">
				<div class="wfSchemeTitle">
					<%if(scheme.isArchived()){ %>
					<a class="pull-right showPointer btn-flat btn-primary " href="#" onclick="schemeAdmin.deleteScheme('<%=scheme.getId()%>');"><i class="fa fa-trash" aria-hidden="true"></i></a>
					<%} %>
					<a class="pull-right showPointer btn-flat btn-primary " href="#" onclick="schemeAdmin.showAddEdit('<%=scheme.getId()%>');"><i class="fa fa-pencil" aria-hidden="true"></i></a>



					<%List<WorkflowStep> steps = wapi.findSteps(scheme);%>
					<%if(!scheme.isArchived()){ %>
					<span class="workflowIcon"></span> <%=scheme.getName()%>
					<%}else{ %>
					<strike><%=scheme.getName()%></strike>
					<%} %>



					<div style="font-weight:normal;font-size:12px;">
						<%=UtilMethods.webifyString(scheme.getDescription())%>
					</div>

				</div>
				<ol class="wfStepsList">
					<%if(steps!= null && steps.size() > 0){ %>
					<%for(WorkflowStep step : steps){ %>
					<li><%=step.getName() %></li>
					<%} %>
					<%}else{ %>
					<li><%=LanguageUtil.get(pageContext, "No-steps-have-been-created")%></li>
					<%} %>
				</ol>
			</div>
			<div style="border-top: 1px solid #e0e0e0; padding: 4px 8px 4px 16px;z-index:10;position: absolute; bottom: 0; width:100%;" onclick="stepAdmin.showViewSteps('<%=scheme.getId()%>');">
				<div class="btn-flat btn-primary showPointer" href="#" style="z-index: 10000;" ><%=LanguageUtil.get(pageContext, "View-Steps")%></div>
			</div>
		</div>
		<%}%>
		<%} else {%>
		<table class="listingTable">
			<tr>
				<td><%=LanguageUtil.get(pageContext, "No-Workflow-Schemes")%><br /></td>
			</tr>
		</table>
		<%}%>
	</div>
</div>


<script>
    dojo.ready(function(){
        mainAdmin.resetCrumbTrail();
        mainAdmin.addCrumbtrail("<%=LanguageUtil.get(pageContext, "Workflows")%>", schemeAdmin.baseJsp);
        mainAdmin.addCrumbtrail("<%=LanguageUtil.get(pageContext, "Schemes")%>", schemeAdmin.baseJsp);

        mainAdmin.refreshCrumbtrail();


    });


</script>
