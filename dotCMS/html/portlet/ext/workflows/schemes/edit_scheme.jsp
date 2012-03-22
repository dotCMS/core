<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowAction"%>
<%@page import="java.util.ArrayList"%>
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
	String schemeId = request.getParameter("schemeId");
	WorkflowScheme defaultScheme = wapi.findDefaultScheme();
	WorkflowScheme scheme = new WorkflowScheme();

	List<WorkflowStep> steps = null;
	WorkflowStep firstStep = null;
	List<WorkflowAction> actions = null;
	try {
		scheme = wapi.findScheme(schemeId);
		steps = wapi.findSteps(scheme);
		firstStep = steps.get(0);
		actions =wapi.findActions(firstStep, APILocator.getUserAPI().getSystemUser());

	} catch (Exception e) {
	}
%>
	<div dojoType="dijit.form.Form" id="addEditSchemeForm" jsId="addEditSchemeForm" encType="multipart/form-data" action="/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfSchemeAjax" method="POST">
		<input type="hidden" name="cmd" value="save">
		<input type="hidden" name="schemeId" value="<%=UtilMethods.webifyString(scheme.getId())%>">
		<!-- START Listing Results -->
		<table class="listingTable" style="width:90%">


				<tr>
					<td align="right" width="40%"><%=LanguageUtil.get(pageContext, "Name")%>:</td>
					<td><input type="text" name="schemeName" id="schemeName"
						dojoType="dijit.form.ValidationTextBox"  required="true" 
						value="<%=UtilMethods.webifyString(scheme.getName())%>"
						maxlength="255">
					</td>
				</tr>
				<tr>
					<td align="right"><%=LanguageUtil.get(pageContext, "Description")%>:</td>
					<td><input type="textarea" name="schemeDescription"
						id="schemeDescription" dojoType="dijit.form.Textarea"
						value="<%=UtilMethods.webifyString(scheme.getDescription())%>">
					</td>
				</tr>
				<tr>
					<td align="right"><%=LanguageUtil.get(pageContext, "Archived")%>:</td>
					<td><input type="checkbox" name="schemeArchived"
						id="schemeArchived" dojoType="dijit.form.CheckBox" value="true"
						<%=(scheme.isArchived()) ? "checked='true'" : ""%>>
					</td>
				</tr>
				<%if(firstStep !=null){ %>
					<tr>
						<td align="right"><%=LanguageUtil.get(pageContext, "Initial-Step")%>:</td>
						<td>
							<%if(firstStep !=null){ %>
								<%=firstStep.getName() %>
							<%}else{ %>
								<%=LanguageUtil.get(pageContext, "Save-Scheme-First") %>
							<%} %>
						</td>
					</tr>
				<%} %>
				<tr>
					<td align="right"><%=LanguageUtil.get(pageContext, "Mandatory")%>:</td>
					<td><input type="checkbox" name="schemeMandatory"
						id="schemeMandatory" dojoType="dijit.form.CheckBox" value="true"
						<%=(scheme.isMandatory()) ? "checked='true'" : ""%> onClick="schemeAdmin.toggleInitialAction">
					</td>
				</tr>
				
				
				<%if(firstStep !=null){ %>
					<tr <%=(!scheme.isMandatory()) ? "style='display:none;'" : ""%> id="forceInitialAction">
						<td nowrap="true" align="right"><%=LanguageUtil.get(pageContext, "Default-Initial-Action")%>:</td>
						<td>
							<%if(actions !=  null) {%>
								<select name="schemeEntryAction" dojoType="dijit.form.Select" style="width:250px;">
									<option value=""><%=LanguageUtil.get(pageContext, "None") %></option>
									<%for(WorkflowAction action : actions){ %>
										<option value="<%=action.getId()%>" <%=(!action.isNew() && action.getId().equals(scheme.getEntryActionId())) ? "selected='true'" :"" %>><%=action.getName() %></option>
									<%} %>
								</select>
							<%}else{ %>
								<%=LanguageUtil.get(pageContext, "Create-Actions-First") %>
							<%} %>
						</td>
					</tr>
				<%} %>
				
		</table>
				
		<div class="buttonRow">

		
		<button dojoType="dijit.form.Button" onClick='schemeAdmin.saveAddEdit()' iconClass="saveIcon"
			type="button">
			<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save"))%>
		</button> &nbsp; &nbsp;

		<button dojoType="dijit.form.Button"
			onClick='schemeAdmin.hideAddEdit()' iconClass="cancelIcon"
			type="button">
			<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel"))%>
		</button>
		</div>

	
	</div>

