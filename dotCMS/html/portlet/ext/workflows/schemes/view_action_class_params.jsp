<%@page import="java.util.Map"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter"%>
<%@page
	import="com.dotmarketing.osgi.actionlet.WorkFlowActionlet"%>
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
		<div id="wacMessage" class="callOutBox2" style="font-size:88%;"><%=actionlet.getHowTo() %></div>
		<div class="clear"></div>
	</div>
	<%if(params != null && params.size() > 0) {%>
		<table class="listingTable" style="width:80%">
		<tr>
			<th colspan="2"><%=LanguageUtil.get(pageContext, "Parameters") %></th>
		</tr>
	
			
			<%for(WorkflowActionletParameter x : params){ %>
				<%WorkflowActionClassParameter enteredParam = enteredParams.get(x.getKey());
				enteredParam = (enteredParam == null) ? new WorkflowActionClassParameter() : enteredParam;
				String value = (enteredParam.getValue() != null) ? enteredParam.getValue() : x.getDefaultValue();%>
				

				<tr>
					<td nowrap="true" valign="top" style="text-align: right;"><%if(x.isRequired()){ %><span class="required"></span><%} %><%=x.getDisplayName() %>:</td>
					<td>
						<textarea class="wfParamTextArea" dojoType="dijit.form.Textarea" id="acp-<%=x.getKey() %>"  name="acp-<%=x.getKey() %>" /><%=UtilMethods.webifyString(value) %></textarea>
					</td>
				</tr>
			<%}%>
	
	
		</table>
	<%}%>
	<div class="wfAddActionButtonRow">
	<%if(params != null && params.size() > 0) {%>
		<button dojoType="dijit.form.Button"
			onClick="actionClassAdmin.saveActionParameters();" iconClass="saveIcon">
			<%=LanguageUtil.get(pageContext, "Save")%>
		</button>
			&nbsp;
	<%} %>
		<button dojoType="dijit.form.Button"
			onClick='dijit.byId("actionClassParamsDia").destroyRecursive()'
			iconClass="cancelIcon">
			<%=LanguageUtil.get(pageContext, "Cancel")%>
		</button>
	</div>

</div>