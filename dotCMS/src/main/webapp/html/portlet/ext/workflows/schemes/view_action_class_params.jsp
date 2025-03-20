<%@page import="java.util.Map"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter"%>
<%@page
		import="com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet"%>
<%@page
		import="com.dotmarketing.portlets.workflows.model.WorkflowActionClass"%>
<%@page import="java.util.Set"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.business.RoleAPI"%>
<%@page import="com.dotmarketing.beans.Permission"%>
<%@page import="com.dotmarketing.business.PermissionSummary"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page
		import="com.dotmarketing.portlets.workflows.model.WorkflowAction"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page
		import="com.dotmarketing.portlets.workflows.business.WorkflowAPI"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowStep"%>
<%@page
		import="com.dotmarketing.portlets.workflows.model.WorkflowScheme"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotmarketing.portlets.workflows.model.MultiSelectionWorkflowActionletParameter" %>
<%@ page import="com.dotmarketing.portlets.workflows.model.MultiKeyValue" %>
<%@ page import="java.util.Collection" %>
<%@ page import="com.dotmarketing.portlets.workflows.model.CheckboxWorkflowActionletParameter" %>

<%
	WorkflowAPI wapi = APILocator.getWorkflowAPI();
	String actionClassId = request.getParameter("actionClassId");
	WorkflowActionClass wac = wapi.findActionClass(actionClassId);
	WorkFlowActionlet actionlet= wapi.findActionlet(wac.getClazz());
	List<WorkflowActionletParameter> params = actionlet.getParameters();
	Map<String,WorkflowActionClassParameter > enteredParams  =wapi.findParamsForActionClass(wac);
%>

<div dojoType="dijit.form.Form" id="ActionClassParamsFrm" jsId="ActionClassParamsFrm" encType="multipart/form-data" action="/DotAjaxDirector/com.dotmarketing.portlets.workflows.ajax.WfActionClassAjax" method="POST">
	<input type="hidden" name="actionClassId" value="<%=UtilMethods.webifyString(actionClassId)%>">
	<input type="hidden" name="actionlet" value="<%=actionlet.getClass().getCanonicalName()%>">
	<input type="hidden" name="cmd" value="save">




	<div id="wacHeader">
		<div id="wacTitle"><h2><%=actionlet.getName() %></h2></div>
		<div id="wacMessage" class="callOutBox2" ><%=actionlet.getHowTo() %></div>
		<div class="clear"></div>
	</div>
	<%if(params != null && params.size() > 0) {%>
	<table class="listingTable" style="width:80%">
		<tr>
			<th colspan="2"><%=LanguageUtil.get(pageContext, "Parameters") %></th>
		</tr>


		<%for(WorkflowActionletParameter workflowActionletParameter : params){ %>
		<%WorkflowActionClassParameter enteredParam = enteredParams.get(workflowActionletParameter.getKey());
			enteredParam = (enteredParam == null) ? new WorkflowActionClassParameter() : enteredParam;
			String value = (enteredParam.getValue() != null) ? enteredParam.getValue() : workflowActionletParameter.getDefaultValue();%>

		<%if (workflowActionletParameter instanceof MultiSelectionWorkflowActionletParameter) { %>
		<tr>
			<td nowrap="true" valign="top" style="text-align: right;"><%if(workflowActionletParameter.isRequired()){ %><span class="required"></span><%} %><%=workflowActionletParameter.getDisplayName() %>:</td>
			<td>
				<select  id="acp-<%=workflowActionletParameter.getKey() %>"  name="acp-<%=workflowActionletParameter.getKey() %>" dojoType="dijit.form.FilteringSelect" style="width:100%">
					<%
						for(MultiKeyValue multiKeyValue : ((MultiSelectionWorkflowActionletParameter) workflowActionletParameter).getMultiValues()) { %>

					<option value="<%=multiKeyValue.getKey()%>" <%= multiKeyValue.getKey().equals(value)? "selected":""%> >
						<%=multiKeyValue.getValue()%>
					</option>

					<% } %>
				</select>
			</td>
		</tr>
		<% } else if (workflowActionletParameter instanceof CheckboxWorkflowActionletParameter) { %>
		<tr>
			<td nowrap="true" valign="top" style="text-align: right;"><%if(workflowActionletParameter.isRequired()){ %><span class="required"></span><%} %><%=workflowActionletParameter.getDisplayName() %>:</td>
			<td>
				<input  id="acp-<%=workflowActionletParameter.getKey() %>" name="acp-<%=workflowActionletParameter.getKey() %>" value="true"
						type="checkbox"
						<%=!workflowActionletParameter.getDefaultValue().equals(value)? "checked":""%>  dojoType="dijit.form.CheckBox" />
			</td>
		</tr>
		<% } else { %>
		<tr>
			<td valign="top" style="text-align: right;max-width: 700px;"><%if(workflowActionletParameter.isRequired()){ %><span class="required"></span><%} %><%=workflowActionletParameter.getDisplayName() %>:</td>
			<td>
				<textarea class="wfParamTextArea" dojoType="ValidationTextarea" required="<%=workflowActionletParameter.isRequired() %>" id="acp-<%=workflowActionletParameter.getKey() %>"  name="acp-<%=workflowActionletParameter.getKey() %>" ><%=UtilMethods.webifyString(value) %></textarea>
			</td>
		</tr>
		<% } %>
		<%}%>


	</table>
	<%}%>
	<div class="wfAddActionButtonRow">
		<button dojoType="dijit.form.Button" class="dijitButtonFlat"
				onClick='dijit.byId("actionClassParamsDia").destroyRecursive()' >
			<%=LanguageUtil.get(pageContext, "Cancel")%>
		</button>
		<%if(params != null && params.size() > 0) {%>
		<button dojoType="dijit.form.Button"
				onClick="actionClassAdmin.saveActionParameters();" iconClass="saveIcon">
			<%=LanguageUtil.get(pageContext, "Save")%>
		</button>
		&nbsp;
		<%} %>

	</div>

</div>
