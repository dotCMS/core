<%@ include file="/html/portlet/ext/webforms/init.jsp" %>
<%@ page import="com.dotmarketing.portlets.webforms.model.*" %>
<%@ page import="com.dotmarketing.portlets.webforms.struts.*" %>
<%@ page import="com.dotmarketing.portlets.webforms.factories.*" %>
<%
	String[] formTypes = WebFormFactory.getWebFormsTypes ();
%>

<script type="text/javascript">
	<!--
	 function generateWebFormReport() { 
	 	var reportTypeSelect = document.getElementById("webFormType");
	 	var reportType = reportTypeSelect.options[reportTypeSelect.selectedIndex].value;
	 	window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/webforms/generate_webforms_report" /></portlet:actionURL>&report_type=' + escape(reportType);
	 }
	 function removeWebFormData() { 
	 	var reportTypeSelect = document.getElementById("webFormType");
	 	var reportType = reportTypeSelect.options[reportTypeSelect.selectedIndex].value;
	 	if (!confirm('This action will delete all the data associated with the forms of type: ' + reportType + ', are you sure you want to continue?'))
	 		return false;

	 	window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/webforms/generate_webforms_report" /><portlet:param name="cmd" value="delete" /></portlet:actionURL>&report_type=' + escape(reportType);
	 }
	//-->
</script>

<table border="0" cellpadding="4" cellspacing="0" align="center">
<%
	if (formTypes.length > 0) {
%>
<tr>
	<td align="center">
		<select id="webFormType">
<%
		for (String type : formTypes) {
%>
			<option value="<%=type %>"><%=type %></option>
<%
		}
%>
		</select>
	</td>
</tr>
<tr>
	<td align="center">
      <button dojoType="dijit.form.Button" onClick="generateWebFormReport()">Generate Report</button>
    </td>
</tr>
<tr>
	<td align="center">
      <button dojoType="dijit.form.Button" onclick="removeWebFormData()">Remove Data</button>
    </td>
</tr>
<%	
	} else {
%>
<tr>
	<td align="center"><b>No forms data found to generate reports</b></td>
</tr>
<%
	}
%>
</table>



