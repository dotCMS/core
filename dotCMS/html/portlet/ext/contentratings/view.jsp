<%@ include file="/html/portlet/ext/contentratings/init.jsp" %>
<%--@ page import="com.dotmarketing.portlets.webforms.model.*" %>
<%@ page import="com.dotmarketing.portlets.webforms.struts.*" --%>
<%@ page import="com.dotmarketing.portlets.contentratings.factories.*" %>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure" %>
<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%
	List<Structure> structures = ContentRatingsFactory.getContentRatingsStructures();
%>

<script type="text/javascript">
	<!--
	 function generateReport() {
	    var structureType = document.getElementById('structureType');
	 	var structureInode = structureType.options[structureType.selectedIndex].value;
	 	window.location = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/contentratings/generate_contentratings_report" /></portlet:actionURL>&structure_type=' + structureInode;
	 }
	//-->
</script>
<table border="0" cellpadding="4" cellspacing="0" align="center">
<%
	if (0 < structures.size()) {
%>
<tr>
	<td align="center">
		<select id="structureType">
<%
		for (Structure structure : structures) {
%>
			<option value="<%= structure.getInode() %>"><%= structure.getName() %></option>
<%
		}
%>
		</select>
	</td>
</tr>
<tr>
	<td align="center">
    <button dojoType="dijit.form.Button" onClick="generateReport()"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Generate-Report")) %></button>
    </td>
</tr>
<%	
	} else {
%>
<tr>
	<td align="center"><b><%= LanguageUtil.get(pageContext, "message.contentlet.ratings") %></b></td>
</tr>
<%
	}
%>
</table>