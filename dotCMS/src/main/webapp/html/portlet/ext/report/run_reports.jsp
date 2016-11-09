<%@ include file="/html/portlet/ext/report/init.jsp" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ include file="/html/common/messages_inc.jsp" %>
<%
	int pageIndex = 0;
	int lastPageIndex = 0;
	//long reportId = (Long)request.getAttribute("reportId");
	String reportId = (String)request.getAttribute("reportId");
	HashMap<String, String> rps = (HashMap<String, String>)request.getAttribute("submittedPars");
	try{
		pageIndex = ((Integer)request.getAttribute("pageIndex")).intValue();
	}catch(Exception e){
		pageIndex = 0;
	}
	try{
		lastPageIndex = ((Integer)request.getAttribute("lastPageIndex")).intValue();
	}catch(Exception ex){
		lastPageIndex = 0;
	}

	StringBuffer sbuffer = (StringBuffer)request.getAttribute("reportSB");
%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotmarketing.portlets.report.model.Report"%>

<%@page import="com.liferay.portal.util.Constants"%>
<%@page import="com.dotmarketing.portlets.report.action.RunReportAction"%>
<%@page import="com.dotmarketing.portlets.report.model.ReportParameter"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.Map"%>
<script type="text/javascript">
<!--

   function deleteSelectedReports() {}
	
	function firstPage(){
		document.getElementById("pageIndex").value = 0;
		submitfm();
	}
	function nextPage(){
		document.getElementById("pageIndex").value = <%= pageIndex + 1 %>;
		submitfm();
	}
	function previousPage(){
		document.getElementById("pageIndex").value = <%= pageIndex - 1 %>;
		submitfm();
	}
	function lastPage(){
		document.getElementById("pageIndex").value = <%= lastPageIndex %>;
		submitfm();
	}
	function submitfm(form) {
		var form = document.getElementById('fm');
		form.<portlet:namespace /><%= Constants.CMD %>.value = '<%= RunReportAction.CMD_RUN %>';
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/report/run_report" /></portlet:actionURL>';
		submitForm(form);
 	}
	//-->	
</script>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Run-Report")) %>' />
<form method="post" id="fm">
	<input type="hidden" id="reportId" name="reportId" value="<%= reportId %>">
	<input type="hidden" id="<portlet:namespace /><%= Constants.CMD %>" name="<portlet:namespace /><%= Constants.CMD %>" value="<%= RunReportAction.CMD_RUN %>">
	<input type="hidden" id="pageIndex" name="pageIndex" value="<%= pageIndex %>">
	<input type="hidden" id="<%= RunReportAction.PARAMETERS_SUBMITTED %>" name="<%= RunReportAction.PARAMETERS_SUBMITTED %>" value="true">
	<%
		Iterator iter = rps.entrySet().iterator(); 
		while(iter.hasNext()){
			Map.Entry pairs = (Map.Entry)iter.next();
			System.out.println(pairs.getKey());
	%>
			<input type="hidden" id="<%= pairs.getKey() %>" name="<%= pairs.getKey() %>" value="<%= pairs.getValue() %>">
	<%
		}
	%>
</form>
<table align="center" width="100%" cellpadding="0" cellspacing="0"
	border="0">
	<tr align="center">
		<td colspan="3">
			<a href="<portlet:actionURL><portlet:param name="struts_action" value="/ext/report/run_report" /><portlet:param name="<%= Constants.CMD %>" value="<%= RunReportAction.CMD_RUN %>" /><portlet:param name="reportId" value="<%= String.valueOf(reportId) %>" /></portlet:actionURL>"><%= LanguageUtil.get(pageContext, "Run-Report-Again") %></a>
			<br />
			<br />
			<a href="/servlets/pdf" target="_blank"><%= LanguageUtil.get(pageContext, "PDF-export") %></a> | <a	href="/servlets/jxl" target="_blank"><%= LanguageUtil.get(pageContext, "XLS-export") %></a> | <a href="/servlets/rtf" target="_blank"><%= LanguageUtil.get(pageContext, "RTF-export") %></a>
		</td>
	</tr>
	<tr align="center">
		<td align="center">&nbsp;</td>
		<td align="center">
		<hr size="1" color="#000000">
		<table align="center" width="100%" cellpadding="0" cellspacing="0"
			border="0">
			<tr align="center">
				<%
				if (pageIndex > 0) {
				%>
					<td align="center"><a onclick="firstPage()"><img src="/portal/images/prevyear.gif" border="0"></a></td>
					<td align="center"><a onclick="previousPage()"><img	src="/portal/images/prev.gif" border="0"></a></td>
				<%
				} else {
				%>
					<td align="center"><img style="filter:alpha(opacity=50);-moz-opacity:0.5;opacity: 0.5;" src="/portal/images/prevyear.gif" border="0"></td>
					<td align="center"><img style="filter:alpha(opacity=50);-moz-opacity:0.5;opacity: 0.5;" src="/portal/images/prev.gif" border="0"></td>
				<%
					}
					if (pageIndex < lastPageIndex) {
				%>
						<td align="center"><a onclick="nextPage()"><img	src="/portal/images/next.gif" border="0"></a></td>
						<td align="center"><a onclick="lastPage()"><img	src="/portal/images/nextyear.gif" border="0"></a></td>
				<%
					} else {
				%>
						<td align="center"><img style="filter:alpha(opacity=50);-moz-opacity:0.5;opacity: 0.5;" src="/portal/images/next.gif" border="0"></td>
						<td align="center"><img style="filter:alpha(opacity=50);-moz-opacity:0.5;opacity: 0.5;" src="/portal/images/nextyear.gif" border="0"></td>
				<%
				}
				%>
			</tr>
		</table>
		<hr size="1" color="#000000">
		</td>
		<td>&nbsp;</td>
	</tr>
	<tr>
		<td>&nbsp;</td>
		<td align="center"><%=sbuffer.toString()%></td>
		<td>&nbsp;</td>
	</tr>
	<tr align="center">
		<td align="center">&nbsp;</td>
		<td align="center">
		<hr size="1" color="#000000">
		<table align="center" width="100%" cellpadding="0" cellspacing="0"
			border="0">
			<tr align="center">
				<%
				if (pageIndex > 0) {
				%>
					<td align="center"><a onclick="firstPage()"><img src="/portal/images/prevyear.gif" border="0"></a></td>
					<td align="center"><a onclick="previousPage()"><img	src="/portal/images/prev.gif" border="0"></a></td>
				<%
				} else {
				%>
					<td align="center"><img style="filter:alpha(opacity=50);-moz-opacity:0.5;opacity: 0.5;" src="/portal/images/prevyear.gif" border="0"></td>
					<td align="center"><img style="filter:alpha(opacity=50);-moz-opacity:0.5;opacity: 0.5;" src="/portal/images/prev.gif" border="0"></td>
				<%
					}

					if (pageIndex < lastPageIndex) {
				%>
						<td align="center"><a onclick="nextPage()"><img	src="/portal/images/next.gif" border="0"></a></td>
						<td align="center"><a onclick="lastPage()"><img	src="/portal/images/nextyear.gif" border="0"></a></td>
				<%
					} else {
				%>
						<td align="center"><img style="filter:alpha(opacity=50);-moz-opacity:0.5;opacity: 0.5;" src="/portal/images/next.gif" border="0"></td>
						<td align="center"><img style="filter:alpha(opacity=50);-moz-opacity:0.5;opacity: 0.5;" src="/portal/images/nextyear.gif" border="0"></td>
				<%
				}
				%>
			</tr>
		</table>
		<hr size="1" color="#000000">
		</td>
		<td>&nbsp;</td>
	</tr>
	<tr align="center">
		<td colspan="3">
			<a href="/servlets/pdf" target="_blank"><%= LanguageUtil.get(pageContext, "PDF-export") %></a> | <a	href="/servlets/jxl" target="_blank"><%= LanguageUtil.get(pageContext, "XLS-export") %></a> | <a href="/servlets/rtf" target="_blank"><%= LanguageUtil.get(pageContext, "RTF-export") %></a>
		</td>
	</tr>
</table>
	
</liferay:box>
