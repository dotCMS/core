<%@ include file="/html/portlet/ext/report/init.jsp" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ include file="/html/common/messages_inc.jsp" %>
<%
	int pageNumber = 1;
	if (request.getParameter("pageNumber")!=null) {
		pageNumber = Integer.parseInt(request.getParameter("pageNumber")); 
	}
	
	int perPage = com.dotmarketing.util.Config.getIntProperty("PER_PAGE");
	int minIndex = (pageNumber - 1) * perPage;
	int maxIndex = perPage * pageNumber;
	
	java.util.Hashtable params = new java.util.Hashtable ();
	params.put("struts_action", new String [] {"/ext/report/view_reports"} );
//	params.put("pageNumber",new String[] { pageNumber + "" });
	
	String referrer = com.dotmarketing.util.PortletURLUtil.getRenderURL(request, javax.portlet.WindowState.MAXIMIZED.toString(), params);

	ArrayList<PermissionAsset> permissionReports = (ArrayList<PermissionAsset>) request.getAttribute(com.dotmarketing.util.WebKeys.Report.ReportList);
	
	int totalMatchesToShow = 0;
	for (int m = 0; m < permissionReports.size(); m++) {
		boolean hasReadPermission = (Long)(permissionReports.get(m).getPermissions().get(0))>=1?true:false;
		if (hasReadPermission)
			totalMatchesToShow++;
	}
%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotmarketing.beans.PermissionAsset"%>
<%@page import="com.dotmarketing.portlets.report.model.Report"%>

<%@page import="com.liferay.portal.util.Constants"%>
<%@page import="com.dotmarketing.portlets.report.action.RunReportAction"%>
<script type="text/javascript">
<!--
	function openFormBuilder(){
		newwin = window.open("/html/dotCMS_form_builder/form_builder_2_7.html", "formwindow", "menubar=0, scrollbars=1, resizable=1, width=900, height=600");
		newwin.focus();
	}

	function deleteSelectedReports() {
		if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.report.delete.selected.reports")) %>')) {
			var reportsToDelete = "";
			
			dojo.forEach(dojo.query('input[name=reportsToDelete]'),
						function (obj) {
							if (obj.checked) {
								if (reportsToDelete != "")
									reportsToDelete += "," + obj.value;
								else
									reportsToDelete += obj.value;
							}
						});
			
			var form = document.getElementById('deleteFm');
			form.<portlet:namespace />cmd.value = '<%=Constants.DELETE%>';
			form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/report/view_reports" /></portlet:actionURL>&reportsToDelete=' + reportsToDelete;
			submitForm(form);
		}
	}
	
	function deleteReport(reportId){
		var id = reportId;
		if (confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.report.delete.selected.report.delete")) %>')) {
			window.location.href='<portlet:actionURL><portlet:param name="struts_action" value="/ext/report/view_reports"/><portlet:param name="<%= Constants.CMD %>" value="<%= Constants.DELETE %>"/></portlet:actionURL>&reportToDelete=' + id;
		}
	}

function addReport(){
	window.location ="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
				<portlet:param name="struts_action" value="/ext/report/edit_report" />
				</portlet:actionURL>";

}

function checkUncheckAll() {
		var checkAll = dijit.byId("checkAll").attr('checked');
		
		dojo.query("[name=reportsToDelete]").forEach(function(node, index, arr) {
			dijit.byId(node.id).attr('checked', checkAll);
		});
}
	//-->
</script>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Viewing-Reports")) %>' />

<div class="buttonRow" style="text-align:right;">
	<button dojoType="dijit.form.ComboButton" id="contentAddButton" optionsTitle='createOptions' onClick="addReport()" iconClass="plusIcon" title="Add New Content">
		<span><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-New-Report")) %></span>
		<div dojoType="dijit.Menu" id="createMenu" style="display: none;">
			<div dojoType="dijit.MenuItem" iconClass="plusIcon" onClick="addReport()">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-New-Report")) %>
			</div>
			<div dojoType="dijit.MenuItem" iconClass="uploadIcon" onClick="openFormBuilder();">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Add-Legacy-Form")) %>
			</div>
		</div>
	</button>
</div>

<table class="listingTable">
	<tr>
		<th width="20">
			<input dojoType="dijit.form.CheckBox" type="checkbox" name="checkAll" id="checkAll" onclick="checkUncheckAll()"/> 
			<span dojoType="dijit.Tooltip" connectId="checkAll" id="ckeckAll_tooltip"><%= LanguageUtil.get(pageContext, "Check-all") %> / <%= LanguageUtil.get(pageContext, "Uncheck-all") %></span>
		</th>						
		<th><%= LanguageUtil.get(pageContext, "Title") %></th>
		<th><%= LanguageUtil.get(pageContext, "Type") %></th>
		<th><%= LanguageUtil.get(pageContext, "Run") %></th>
		<th><%= LanguageUtil.get(pageContext, "Requires-Input") %></th>
	</tr>
	<% if (permissionReports.size() == 0) { %>
		<tr>
			<td colspan="5" align=center><%= LanguageUtil.get(pageContext, "There-are-no-Reports-to-show") %></td>
		</tr>
	<% }else { %>
		<form name="deleteFm" id="deleteFm" method="post">
		<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="">
			<% 
			int index = 0;
			
			int limit = 0;
			if (totalMatchesToShow > maxIndex)
				limit = maxIndex;
			else
				limit = permissionReports.size();
			
			for(int m=minIndex; m<limit; m++){
				PermissionAsset pa = permissionReports.get(m);
				Report report = (Report)pa.getAsset();
				String str_style =  ((index%2)==0)  ? "class=\"alternate_1\"" : "class=\"alternate_2\"";
			%>
			<% if(pa.hasReadPermission()){ %>
				<tr <%= str_style %>>
					<td align="center">
						<input type="checkbox" dojoType="dijit.form.CheckBox" name="reportsToDelete" id="reportsToDelete<%= report.getInode() %>" value="<%= report.getInode() %>" <% if(!pa.hasWritePermission()){ %>disabled<% } %>>
					</td>
					<td>
						<% if(pa.hasWritePermission()){ %>
							<a href="<portlet:actionURL><portlet:param name="struts_action" value="/ext/report/edit_report" /><portlet:param name="reportId" value="<%= String.valueOf(report.getInode()) %>" /></portlet:actionURL>">
								<%= report.getReportName()%>
							</a>
						<% }else{ %>
							<%= report.getReportName()%>
						<% } %>
					</td>
					<td>
						<%if (!report.isWebFormReport()) { %>
							<%= LanguageUtil.get(pageContext, "Jasper-Report") %>
						<%}else{ %>
							<%= LanguageUtil.get(pageContext, "Web-Form-Report") %>
						<%} %>
					</td>
					
					<td>
						<%if (!report.isWebFormReport()) {%>
							<a href="<portlet:actionURL><portlet:param name="struts_action" value="/ext/report/run_report" /><portlet:param name="<%= Constants.CMD %>" value="<%= RunReportAction.CMD_RUN %>" /><portlet:param name="reportId" value="<%= String.valueOf(report.getInode()) %>" /></portlet:actionURL>&pdf=true"><img src="/icon?i=.pdf" border="0" width="16" height="16" hspace="5" vspace="0"/></a>
						
						<%}if (!report.isWebFormReport()) {%> 
							<a href="<portlet:actionURL><portlet:param name="struts_action" value="/ext/report/run_report" /><portlet:param name="<%= Constants.CMD %>" value="<%= RunReportAction.CMD_RUN %>" /><portlet:param name="reportId" value="<%= String.valueOf(report.getInode()) %>" /></portlet:actionURL>&xls=true"><img src="/icon?i=.xls" border="0" width="16" height="16" hspace="5" vspace="0"/></a>
						
						<%} else {%>
							<a href="<portlet:actionURL><portlet:param name="struts_action" value="/ext/report/run_report" />
								<portlet:param name="<%= Constants.CMD %>" value="<%= RunReportAction.CMD_WEBFORM_RUN %>" />
								<portlet:param name="report_type" value="<%= report.getReportName() %>" />
								</portlet:actionURL>"><img src="/icon?i=.xls" border="0" width="16" height="16" hspace="5" vspace="0"/>
							</a>
						<% } %>
						
						<% if (!report.isWebFormReport()) {%>
							<a href="<portlet:actionURL><portlet:param name="struts_action" value="/ext/report/run_report" /><portlet:param name="<%= Constants.CMD %>" value="<%= RunReportAction.CMD_RUN %>" /><portlet:param name="reportId" value="<%= String.valueOf(report.getInode()) %>" /></portlet:actionURL>&rtf=true"><img src="/icon?i=.rtf" border="0" width="16" height="16" hspace="5" vspace="0"/></a>
						<%}%>
					</td>
					<td><%= (report.isRequiresInput()==true)? LanguageUtil.get(pageContext, "yes"): LanguageUtil.get(pageContext, "no")%> </td>
				</tr>
				<% 
				index++;
				} %>
			<% } %>
		</form>
	<% } %>
</table>

<div class="yui-gb buttonRow">
	<div class="yui-u first" style="text-align:left;">
		<%if(minIndex !=0 ||maxIndex < totalMatchesToShow) {  %>
			<% if (minIndex != 0) { %>
  				<button dojoType="dijit.form.Button" onClick="window.location.href='<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/report/view_reports" /><portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber - 1) %>" /></portlet:renderURL>'" iconClass="previousIcon">
					<%= LanguageUtil.get(pageContext, "Previous") %>
				</button>
			<% } %>
		<% } %>&nbsp;
	</div>
	<div class="yui-u">
		<button dojoType="dijit.form.Button"  iconClass="deleteIcon"  onClick="deleteSelectedReports()">
    		<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete-Selected")) %>
 		</button>
	</div>
	<div class="yui-u" style="text-align:right;">
		<%if(minIndex !=0 ||maxIndex < totalMatchesToShow) {  %>
			<% if (maxIndex < totalMatchesToShow) { %>
  				<button dojoType="dijit.form.Button" onClick="window.location.href='<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/report/view_reports" /><portlet:param name="pageNumber" value="<%= String.valueOf(pageNumber + 1) %>" /></portlet:renderURL>'" iconClass="nextIcon">
					<%= LanguageUtil.get(pageContext, "Next") %>
				</button>
			<% } %>
		<%} %>&nbsp;
	</div>
</div>
	
</liferay:box>
