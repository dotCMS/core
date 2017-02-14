<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowAction"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.portlets.workflows.business.WorkflowAPI"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowStep"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowScheme"%>
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
	<div class="form-horizontal">
		<dl>
			<dt>
				<label for=""><%=LanguageUtil.get(pageContext, "Name")%>:</label>
			</dt>
			<dd>
				<input type="text" name="schemeName" id="schemeName"
					dojoType="dijit.form.ValidationTextBox"  required="true" 
					value="<%=UtilMethods.webifyString(scheme.getName())%>"
					maxlength="255" style="width:250px">
			</dd>
		</dl>
		<dl>
			<dt>
				<label for=""><%=LanguageUtil.get(pageContext, "Description")%>:</label>
			</dt>
			<dd>
				<input type="textarea" name="schemeDescription"
					id="schemeDescription" dojoType="dijit.form.Textarea"
					value="<%=UtilMethods.webifyString(scheme.getDescription())%>" style="width:250px; height:100px;min-height:100px;max-height:100px;">
			</dd>
		</dl>
		<dl>
			<dt>
				<label for=""><%=LanguageUtil.get(pageContext, "Archived")%>:</label>
			</dt>
			<dd>
				<input type="checkbox" name="schemeArchived"
					id="schemeArchived" dojoType="dijit.form.CheckBox" value="true"
					<%=(scheme.isArchived()) ? "checked='true'" : ""%>>
			</dd>
		</dl>
		<%if(firstStep !=null){ %>
			<dl>
				<dt>
					<label for=""><%=LanguageUtil.get(pageContext, "Initial-Step")%>:</label>
				</dt>
				<dd>
					<%if(firstStep !=null){ %>
						<%=firstStep.getName() %>
					<%}else{ %>
						<%=LanguageUtil.get(pageContext, "Save-Scheme-First") %>
					<%} %>
				</dd>
			</dl>
		<%} %>
		<dl>
			<dt>
				<label for=""><%=LanguageUtil.get(pageContext, "Mandatory")%>:</label>
			</dt>
			<dd>
				<%if(firstStep !=null){ %>
					<input type="checkbox" name="schemeMandatory"
					id="schemeMandatory" dojoType="dijit.form.CheckBox" value="true"
					<%=(firstStep !=null) ? "disabled ='false'" : "disabled ='true'"%> onClick="schemeAdmin.toggleInitialAction"
					<%=(scheme.isMandatory()) ? "checked='true'" : ""%>>
				<%}else{ %>
						<%=LanguageUtil.get(pageContext, "Add-Workflow-Step") %>
				<%} %>	
			</dd>
		</dl>
		<%if(firstStep !=null){ %>
			<dl <%=(!scheme.isMandatory()) ? "style='display:none;'" : ""%> id="forceInitialAction">
				<dt>
					<label for=""><%=LanguageUtil.get(pageContext, "Default-Initial-Action")%>:</label>
				</dt>
				<dd>
					<%if(!actions.isEmpty()) {%>
						<select name="schemeEntryAction" dojoType="dijit.form.FilteringSelect" style="width:250px;">
							<option value=""><%=LanguageUtil.get(pageContext, "None") %></option>
							<%for(WorkflowAction action : actions){ %>
								<option value="<%=action.getId()%>" <%=(!action.isNew() && action.getId().equals(scheme.getEntryActionId())) ? "selected='true'" :"" %>><%=action.getName() %></option>
							<%} %>
						</select>
					<%}else{ %>
						<%=LanguageUtil.get(pageContext, "Create-Actions-First") %>
					<%} %>
				</dd>
			</dl>
		<%} %>
	</div>
			
	<div class="buttonRow" style="margin-top: 20px;">
		<button dojoType="dijit.form.Button" onClick='schemeAdmin.saveAddEdit()' iconClass="saveIcon" type="button">
			<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save"))%>
		</button>
		<button dojoType="dijit.form.Button"
			onClick='schemeAdmin.hideAddEdit()' class="dijitButtonFlat" type="button">
			<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel"))%>
		</button>
	</div>

</div>
