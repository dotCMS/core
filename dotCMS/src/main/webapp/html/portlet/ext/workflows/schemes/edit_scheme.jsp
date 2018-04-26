<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.workflows.business.WorkflowAPI"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowScheme"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowStep"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="java.util.List"%>

<%
	WorkflowAPI wapi = APILocator.getWorkflowAPI();
	String schemeId = request.getParameter("schemeId");
	WorkflowScheme scheme = new WorkflowScheme();

	List<WorkflowStep> steps;
	WorkflowStep firstStep = null;
	try {
		scheme = wapi.findScheme(schemeId);
		steps = wapi.findSteps(scheme);
		firstStep = steps.get(0);
	} catch (Exception e) {
	}
%>


<div dojoType="dijit.form.Form" id="addEditSchemeForm" jsId="addEditSchemeForm" encType="multipart/form-data" action="/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfSchemeAjax" method="POST">
	<input type="hidden" id="cmd" name="cmd" value="save">
	<input type="hidden" id="schemeId" name="schemeId" value="<%=UtilMethods.webifyString(scheme.getId())%>">
	<!-- START Listing Results -->
	<div class="form-horizontal">

		<%if(!scheme.isNew()){%>
		<dl>
			<dt>
				<label for=""><%=LanguageUtil.get(pageContext, "Scheme")%> <%=LanguageUtil.get(pageContext, "Id")%>:</label>
			</dt>
			<dd>
				<strong>
					<a onclick="this.parentNode.innerHTML='<%=scheme.getId()%>'; return false;" href="#"><%=APILocator.getShortyAPI().shortify(scheme.getId()) %></a>
				</strong>
			</dd>
		</dl>
		<%}%>

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

	</div>
			
	<div class="buttonRow" style="margin-top: 20px;">
		<button dojoType="dijit.form.Button" onClick='schemeAdmin.saveAddEdit()' iconClass="saveIcon" type="button">
			<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save"))%>
		</button>
		<%if(!scheme.isNew()){%>
		<button dojoType="dijit.form.Button" onClick='schemeAdmin.copyScheme("<%=UtilMethods.webifyString(scheme.getId())%>", "<%=UtilMethods.webifyString(scheme.getName())%>")' iconClass="saveIcon" type="button">
			<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Copy"))%>
		</button>
		<%}%>
		<%if(scheme.isArchived()){%>
		<button dojoType="dijit.form.Button" onClick='schemeAdmin.deleteScheme("<%=UtilMethods.webifyString(scheme.getId())%>")' iconClass="deleteIcon" type="button">
			<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "delete"))%>
		</button>
		<%}%>
		<button dojoType="dijit.form.Button"
			onClick='schemeAdmin.hideAddEdit()' class="dijitButtonFlat" type="button">
			<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel"))%>
		</button>
	</div>

</div>
