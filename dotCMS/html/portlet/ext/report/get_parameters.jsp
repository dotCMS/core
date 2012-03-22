<%@ include file="/html/portlet/ext/report/init.jsp" %>
<%@ include file="/html/common/messages_inc.jsp"%>
<%@ page import="com.dotmarketing.util.UtilMethods" %>

<%
	ArrayList<ReportParameter> reportParameters = (ArrayList<ReportParameter>)request.getAttribute("reportParameters");
	ReportParameter rp = reportParameters.get(0);
	//Long reportId = rp.getReportInode();
	String reportId = rp.getReportInode();
	Report report = ReportFactory.getReport(reportId);
	boolean pdf = (Boolean)request.getAttribute("pdf");
	boolean xls = (Boolean)request.getAttribute("xls");
	boolean rtf = (Boolean)request.getAttribute("rtf");
	boolean html = (Boolean)request.getAttribute("html");
%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotmarketing.portlets.report.model.ReportParameter"%>
<%@page import="com.dotmarketing.portlets.report.businessrule.ReportParamterBR"%>
<%@page import="com.dotmarketing.db.DbConnectionFactory"%>

<%@page import="com.dotmarketing.portlets.report.action.RunReportAction"%>
<%@page import="java.util.Calendar"%>
<%@page import="bsh.Interpreter"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.SelectParameter"%>
<%@page import="java.util.Date"%>
<%@page import="com.dotmarketing.portlets.report.factories.ReportFactory"%>
<%@page import="com.dotmarketing.portlets.report.model.Report"%>
<script type="text/javascript">
<!--

   function deleteSelectedReports() {}
	
	//-->	
</script>

<script type="text/javascript"> 
   <!-- ### Set the number of calendars to use ### -->
   <liferay:include page="/html/js/calendar/calendar_js_box_ext.jsp" flush="true">
  	<liferay:param name="calendar_num" value="<%= String.valueOf(ReportParamterBR.getCalendarNumber(reportParameters)) %>" />
   </liferay:include>
   <!-- ### END Set the number of calendars to use ### -->   
   
   	function submitfm(form) {
		var form = document.getElementById('fm');
		
		form.<portlet:namespace />cmd.value = 'run';
		<% if(pdf){ %>
			form.target='_blank';
			form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/report/run_report" /></portlet:actionURL>&pdf=true';
			submitForm(form);
			setTimeout("window.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/report/view_reports" /></portlet:actionURL>'", 1000);
		<%}else if(xls){ %>
			form.target='_blank';
			form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/report/run_report" /></portlet:actionURL>&xls=true';
			submitForm(form);
			setTimeout("window.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/report/view_reports" /></portlet:actionURL>'", 1000);
		<%}else if(rtf){ %>
			form.target='_blank';
			form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/report/run_report" /></portlet:actionURL>&rtf=true';
			submitForm(form);
			setTimeout("window.location = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/report/view_reports" /></portlet:actionURL>'", 1000);
		<% }else{ %>
			form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/report/run_report" /></portlet:actionURL>';
			submitForm(form);
		<% } %>
 	}
	
	function cancelEdit() {
		self.location = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/report/view_reports" /></portlet:renderURL>';
	}
   
 </script>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value= '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Fill-Report-Parameters")) %>'/>
<h3><%= report.getReportName() %></h3>
<form id="fm" method="post">
<input type="hidden" id="<%= RunReportAction.PARAMETERS_SUBMITTED %>" name="<%= RunReportAction.PARAMETERS_SUBMITTED %>" value="true">
<input type="hidden" id="reportId" name="reportId" value ="<%= reportId %>"> 
<input type="hidden" id="<portlet:namespace />cmd" name="<portlet:namespace />cmd" > 

<table align="center" width="90%" cellpadding="3" cellspacing="0" border="0" align="center" class="portletBox">
	
	<tr><td colspan="4">&nbsp;</td></tr>
	
	<% 
		long calIndex = 0;
		for(ReportParameter par: reportParameters){ %>
			<tr>	
				<td align="right"><strong><%= par.getName() %></strong></td>
				<% 
					if(ReportParamterBR.isNumberParameter(par.getClassType()) || ReportParamterBR.isStringParameter(par.getClassType())){
						String defaultValue = par.getDefaultValue();
						defaultValue = (UtilMethods.isSet(defaultValue) ? defaultValue : "");
						defaultValue = (String) new bsh.Interpreter().eval(defaultValue);
						defaultValue = (UtilMethods.isSet(defaultValue) ? defaultValue : "");
				%>						
						<td align="left"><input type="text" name="<%= par.getName() %>" id="<%= par.getName() %>" value="<%=defaultValue%>"></td>
				<%	}else if(ReportParamterBR.isDataSourceParameter(par.getClassType())){ 
				%>
					<td>
						<select name="<%= par.getName() %>" id="<%= par.getName() %>">
							<%
								ArrayList<String> dataSources = DbConnectionFactory.getAllDataSources();
								for(String ds: dataSources){
							%>
									<option name="<%= ds %>" id="<%= ds %>" value="<%= ds %>"><%= ds %></option>
							<%
								}
							%>
						</select>
					</td>
				<%	}else if(ReportParamterBR.isDateParameter(par.getClassType())){
						String date;
						String hour;
						String min;
						String sec;
					
						Calendar c = Calendar.getInstance();
						if(par.getDefaultValue() != null && !par.getDefaultValue().equals("")){
							 c.setTime((Date)new bsh.Interpreter().eval(par.getDefaultValue()));
						}
						date = String.valueOf((c.get(Calendar.MONTH) + 1) + "/" + c.get(Calendar.DATE) + "/" + c.get(Calendar.YEAR));
						hour = String.valueOf(c.get(Calendar.HOUR) == 0 ? 12 : c.get(Calendar.HOUR));
						min = String.valueOf(c.get(Calendar.MINUTE));
						sec = String.valueOf(c.get(Calendar.SECOND));

				%>
					<td>
						<input name="<%= par.getName() + "date" %>" id="<%= par.getName() + "date" %>" value="<%= date %>">
						<img id="<portlet:namespace />calendar_input_<%= calIndex %>_button" name="<portlet:namespace />calendar_input_<%= calIndex %>_button" src="/html/skin/image/common/calendar/calendar.gif" onclick="<portlet:namespace />calendarOnClick_<%= String.valueOf(calIndex) %>()">  
						<%= LanguageUtil.get(pageContext, "Hours") %>: <input size="2" name="<%= par.getName() + "hour" %>" id ="<%= par.getName() + "hour" %>" value="<%= hour %>"> 
						<%= LanguageUtil.get(pageContext, "Mins") %>: <input size="2" name="<%= par.getName() + "min" %>" id="<%= par.getName() + "min" %>" value="<%= min %>" > 
						<%= LanguageUtil.get(pageContext, "Sec") %>: <input size="2" name="<%= par.getName() + "sec" %>" id="<%= par.getName() + "sec" %>" value="<%= sec %>" > 
						<select id="<%= par.getName() + "dayPart" %>" name="<%= par.getName() + "dayPart" %>">
							<option value="am" <% if(c.get(Calendar.AM_PM) == Calendar.AM){ %>selected<%} %>>AM</option>
							<option value="pm" <% if(c.get(Calendar.AM_PM) == Calendar.PM){ %>selected<%} %>>PM</option>
						</select>
						
						<script type="text/javascript">
							function <portlet:namespace />setCalendarDate_<%= calIndex %>(year, month, day){
								document.getElementById('<%= String.valueOf(par.getName() + "date") %>').value = month + "/" + day + "/" + year;
								document.getElementById('<%= String.valueOf(par.getName() + "year") %>').value = year;
								document.getElementById('<%= String.valueOf(par.getName() + "month") %>').value = month;
								document.getElementById('<%= String.valueOf(par.getName() + "day") %>').value = day;
							}
						</script>
					</td>
					<input type="hidden" id="<%= par.getName() + "year"%>" name="<%= par.getName() + "year"%>" value="<%= c.get(Calendar.YEAR) %>" >
					<input type="hidden" id="<%= par.getName() + "month"%>" name="<%= par.getName() + "month"%>" value="<%= c.get(Calendar.MONTH) + 1  %>">
					<input type="hidden" id="<%= par.getName() + "day"%>" name="<%= par.getName() + "day"%>" value="<%= c.get(Calendar.DATE) %>">
					<%	
						calIndex++;
						}else if(ReportParamterBR.isBooleanParameter(par.getClassType())){
					%>
						<td>
							<%= LanguageUtil.get(pageContext, "True") %> <input <% if(((Boolean)new bsh.Interpreter().eval(par.getDefaultValue())).booleanValue()){ %>checked="checked"<%} %> type="radio" id="<%= par.getName() %>" name="<%= par.getName() %>" value="true" >
							<%= LanguageUtil.get(pageContext, "False") %> <input <% if(!((Boolean)new bsh.Interpreter().eval(par.getDefaultValue())).booleanValue()){ %>checked="checked"<%} %> type="radio" id="<%= par.getName() %>" name="<%= par.getName() %>" value="false" >
						</td>
					<%	
						}else if(ReportParamterBR.isObjectParameter(par.getClassType())){
					%>
						<td>
							<%
								java.lang.Object obj = new bsh.Interpreter().eval(par.getDefaultValue());

								if (obj instanceof com.dotmarketing.util.SelectParameter) {
									SelectParameter sp = (SelectParameter) obj;
							%>
 
									<select name="<%= par.getName() %>" id="<%= par.getName() %>" <%= sp.isMultiple()?"multiple":""%> >
										<% 
											for (int i=0; i < sp.getNumOptions(); i++){
										%>
												<option value="<%= sp.getOption(i) %>" <%= sp.isSelected(i)?"selected":"" %> ><%= sp.getOption(i) %></option>
										<%
											}
										%>
									</select>
							<%
								} else {
							%>
								<textarea rows="1" cols="25" name="<%= par.getName() %>" id="<%= par.getName() %>" readonly><%= LanguageUtil.get(pageContext, "Parameter-class-unkown") %></textarea>
							<%
								}

							%>
						</td>
					<%
						}
					%>
			</tr>
		<% } %>
		
			<tr><td colspan="4">&nbsp;</td></tr>

</table>     
            <br/>
            <button dojoType="dijit.form.Button" onClick="submitfm(document.getElementById('fm'))">
			   <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Run-Report")) %>
            </button>&nbsp; &nbsp;
            <button dojoType="dijit.form.Button" onClick="cancelEdit()"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cancel")) %></button>
            
</form>
</liferay:box>
