<%@ include file="/html/portlet/ext/scheduler/init.jsp" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.util.InodeUtils" %>

<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@page import="com.dotmarketing.util.UtilMethods"%>

<%@page import="com.dotmarketing.portlets.report.struts.ReportForm"%>
<%@page import="com.dotmarketing.portlets.report.model.Report"%>
<%@page import="com.dotmarketing.portlets.report.struts.ReportForm.DataSource"%>
<%
	ReportForm reportForm = null;

	if (request.getAttribute("ReportForm") != null) {
		reportForm = (ReportForm) request.getAttribute("ReportForm");
	}
	
	java.util.Hashtable params = new java.util.Hashtable ();
	params.put("struts_action", new String [] {"/ext/report/edit_report"} );
	//params.put("reportId", new String[] {new Long(UtilMethods.parseLong(request.getParameter("reportId"),0)).toString()});
	if(InodeUtils.isSet(request.getParameter("reportId"))){
		params.put("reportId", new String[] {request.getParameter("reportId")});
	}else {
		params.put("reportId", new String[] {""});
	}
	String referrer = com.dotmarketing.util.PortletURLUtil.getRenderURL(request, javax.portlet.WindowState.MAXIMIZED.toString(), params);
	Report report = (Report)request.getAttribute(com.dotmarketing.util.WebKeys.REPORT_EDIT);
	
	List<DataSource> dataSources = (List<DataSource>) request.getAttribute("dataSources");
%>


<script language="Javascript">

	
	function submitfm(form) {
		var form = document.getElementById('fm');
		
		form.<portlet:namespace />cmd.value = '<%=Constants.EDIT%>';
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/report/edit_report" /></portlet:actionURL>';
		submitForm(form);
 	}
	
	function cancelEdit() {
		self.location = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/report/view_reports" /></portlet:renderURL>';
	}
	
	function deleteSelectedReports() {
   	}
   	
   	function downloadReportSource(reportId) {
   		var href = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
		href += "<portlet:param name='struts_action' value='/ext/report/edit_report' />";
		href += "<portlet:param name='cmd' value='downloadReportSource' />";		
		href += "</portlet:actionURL>";
		href += "&reportId=" + reportId;
		window.location.href=href;
   	}
	function hideEditButtonsRow() {
		dojo.style('editReportButtonRow', { display: 'none' });
		}

	function showEditButtonsRow() {
		if( typeof changesMadeToPermissions!= "undefined"){
			if(changesMadeToPermissions == true){
				dijit.byId('applyPermissionsChangesDialog').show();
			}
		}
		dojo.style('editReportButtonRow', { display: '' });
		changesMadeToPermissions = false;
	}
</script>
<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value= '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit-Report")) %>' />
<html:form method="POST" action='/ext/report/edit_report' enctype="multipart/form-data" styleId="fm">
<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="">
<input name="<portlet:namespace />redirect" type="hidden" value="">
<input name="referrer" type="hidden" value="<%= referrer %>">
<html:hidden property="reportId"></html:hidden>
<html:hidden property="webFormReport"></html:hidden>

<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false" >
     
	<!-- Basic Properties -->    
	<div id="fileBasicTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Basic-Properties") %>" onShow="showEditButtonsRow()">
		<dl>
			<dt>
				<span class="required"></span> <%= LanguageUtil.get(pageContext, "Name") %>:
			</dt>
			<dd><input type="text" dojoType="dijit.form.TextBox" name="reportName" style="width:250px;" value="<%= UtilMethods.isSet(reportForm.getReportName()) ? reportForm.getReportName() : "" %>" /></dd>
			
			<dt>
				<span class="required"></span> <%= LanguageUtil.get(pageContext, "Description") %>:
			</dt>
			<dd>
				<textarea dojoType="dijit.form.Textarea" name="reportDescription" rows="4" cols="90" style="width:400px;padding:5px;min-height:100px;"><%= UtilMethods.isSet(reportForm.getReportDescription()) ? reportForm.getReportDescription() : "" %></textarea>
			</dd>
		
			<%if (!reportForm.isWebFormReport()) {%>
				<dt>
					<span class="required"></span> <%= LanguageUtil.get(pageContext, "Datasource") %>:
				</dt>
				<dd>
					<select dojoType="dijit.form.FilteringSelect" name="selectedDataSource" value="<%= UtilMethods.isSet(request.getAttribute("selectedDS")) ? String.valueOf(request.getAttribute("selectedDS")) : "" %>">
<%
					for (DataSource dataSource: dataSources) {
%>
						<option value="<%= dataSource.getDsName() %>"><%= dataSource.getDsName() %></option>
<%
					}
%>
					</select>
				</dd>
		
				<dt>
					<span class="required"></span> <%= LanguageUtil.get(pageContext, "JRML-File") %>:
				</dt>
				<dd><input type="file" name="jrxmlFile" id="jrxmlFile" /></dd>
				<%if(report !=null && InodeUtils.isSet(report.getInode())) {%>
					<dd class="inputCaption"><a href="javascript:downloadReportSource('<%= reportForm.getReportId() %>')"><%= LanguageUtil.get(pageContext, "Download-Report-Source") %></a></dd>
				<%} %>
		
				<dt>&nbsp;</dt>
				<dd><%= LanguageUtil.get(pageContext, "You-Need-iReport") %>: <a target="_blank" href="http://www.google.com/search?q=ireport+jasper">iReport</a></dd>
			<%}%>
		</dl>
	</div>
	<!-- END Properties Tab -->
		
	<!-- START Permissions Tab -->
<%
	PermissionAPI perAPI = APILocator.getPermissionAPI();
	boolean canEditAsset = perAPI.doesUserHavePermission(report, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user);
	if (canEditAsset) {
%>
	<div id="permissionTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Permissions") %>" onShow="hideEditButtonsRow()">
		<%
			request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, report);
			request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT_BASE, null);
		%>
		<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>					
	</div>
<%
	}
%>
	<!-- END Permissions Tab -->

</div>
<div class="clear"></div>
<div class="buttonRow" id="editReportButtonRow">
        <button dojoType="dijit.form.Button" iconClass="saveIcon" onClick="submitfm(document.getElementById('fm'))" type="button">
		    <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %>
        </button> 
        <button dojoType="dijit.form.Button" iconClass="cancelIcon" onClick="cancelEdit()" type="button">
        	<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
		</button>
</div>
</html:form>
</liferay:box>

