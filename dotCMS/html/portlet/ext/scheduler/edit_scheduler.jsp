<%@ include file="/html/portlet/ext/scheduler/init.jsp" %>

<%@ page import="com.dotmarketing.portlets.scheduler.struts.SchedulerForm" %>
<%@ page import="com.dotmarketing.util.SchedulerJobLocator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="javax.servlet.jsp.PageContext" %>

<%
	SchedulerForm schedulerForm = null;

	if (request.getAttribute("SchedulerForm") != null) {
		schedulerForm = (SchedulerForm) request.getAttribute("SchedulerForm");
	}

	java.util.Hashtable params = new java.util.Hashtable();
	params.put("struts_action", new String [] {"/ext/scheduler/view_schedulers"} );

	String referrer = com.dotmarketing.util.PortletURLUtil.getRenderURL(request, javax.portlet.WindowState.MAXIMIZED.toString(), params);

	List<String> listJobClasses = SchedulerJobLocator.getJobClassess();

%>

<%@page import="com.dotmarketing.util.UtilMethods"%>
<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit-Job")) %>' />
<style>
.aligncenter{
	margin-left: auto;
    margin-right: auto;
    width: 600px;
}
</style>
<script language="Javascript">

function checkDate(element, fieldName) {
	if (element.checked) {
	  	eval("document.getElementById('" + fieldName + "Div').style.visibility = ''");
	} else {
		eval("document.getElementById('" + fieldName + "Div').style.visibility = 'hidden'");
	}
}



function submitfm(form) {
	if (validate()) {
		form.<portlet:namespace />cmd.value = '<%=Constants.ADD%>';
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/scheduler/edit_scheduler" /></portlet:actionURL>';
		form.<portlet:namespace />redirect.value = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/scheduler/edit_scheduler" /></portlet:renderURL>';
		form.referrer.value = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/scheduler/view_schedulers" /><portlet:param name="group" value="user_jobs" /></portlet:renderURL>';
		submitForm(form);
	}

}

function cancelEdit() {
	self.location = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/scheduler/view_schedulers" /></portlet:renderURL>';
}


function deleteSchedule(form) {
	if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.Scheduler.confirm.delete")) %>')){
		form.<portlet:namespace />cmd.value = '<%=Constants.DELETE%>';
		form.<portlet:namespace />redirect.value = '<%= referrer %>';
		form.referrer.value = '<%= referrer %>';
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/scheduler/edit_scheduler" /></portlet:actionURL>';
		submitForm(form);
	}
}
	 function validate() {
		   return dijit.byId("cronExpression").required=true;
		 }


	 function updateDate(varName) {
			var field = $(varName);
			var dateValue ="";
			var myDate = dijit.byId(varName + "Date");
			var x = new Date();
			if(myDate != null){
				x = myDate.getValue();
			}
			var month = (x.getMonth() +1) + "";
			month = (month.length < 2) ? "0" + month : month;
			var day = (x.getDate() ) + "";
			day = (day.length < 2) ? "0" + day : day;
			year = x.getFullYear();
			dateValue= year + "-" + month + "-" + day + " ";

			if (dijit.byId(varName + 'Time') != null) {
				var time = dijit.byId(varName + 'Time').value;
				var hour = time.getHours();
				if(hour < 10) hour = "0" + hour;
				var min = time.getMinutes();
				if(min < 10) min = "0" + min;
				dateValue += hour + ":" + min;
			} else {
				dateValue += "00:00";
			}

			field.value = dateValue;
		}
</script>
<html:form action="/ext/scheduler/edit_scheduler" styleId="fm">

	<div  id="mainTabContainer"  dolayout="false" dojoType="dijit.layout.TabContainer">
		  <div id="main" class="aligncenter" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Main") %>">
			<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="">
			<input name="<portlet:namespace />redirect" type="hidden" value="">
			<input name="referrer" type="hidden" value="">
			<input type="hidden" name="jobGroup" id="jobGroup" value="User Job">
			<dl>
					<dt>
						<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Job-Name") %>:</b></font>
					</dt>
					<dd>
<%
	if (((schedulerForm.getJobGroup() == null) ||
		 (schedulerForm.getJobGroup().equals("User Job"))) &&
		(!schedulerForm.isEditMode())) {
%>
						<input class="form-text" dojoType="dijit.form.TextBox" name="jobName" id="jobName" value="<%= UtilMethods.isSet(schedulerForm.getJobName()) ? schedulerForm.getJobName() : "" %>" style="width: 300px;" type="text" >
<%
	} else {
%>
						<%= schedulerForm.getJobGroup().equals("Recurrent Campaign") ? schedulerForm.getJobDescription() : schedulerForm.getJobName() %>
						<input class="form-text" dojoType="dijit.form.TextBox" name="jobName" id="jobName" value="<%= UtilMethods.isSet(schedulerForm.getJobName()) ? schedulerForm.getJobName() : "" %>" type="hidden" >
						<input class="form-text" dojoType="dijit.form.TextBox" name="editMode" id="editMode" value="<%= schedulerForm.isEditMode()? "true" : "false" %>" type="hidden" >
<%
	}
%>
					</dd>
					<dt>
						<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Job-Description") %>:</b></font>
					</dt>
					<dd>
<%
	if ((schedulerForm.getJobGroup() == null) || (schedulerForm.getJobGroup().equals("User Job"))) {
%>
						<input class="form-text" dojoType="dijit.form.TextBox" name="jobDescription" id="jobDescription" value="<%= UtilMethods.isSet(schedulerForm.getJobDescription()) ? schedulerForm.getJobDescription() : "" %>"  style="width: 300px;" type="text" >
<%
	} else {
%>
						<%= schedulerForm.getJobGroup().equals("Recurrent Campaign") ? schedulerForm.getJobName() : schedulerForm.getJobDescription() %>
<%
	}
%>
					</dd>
					<dt>
						<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Execute") %>:</b></font>
					</dt>
					<dd>
						<div id="startDateDiv">

<%
	SimpleDateFormat sdf = new SimpleDateFormat(com.dotmarketing.util.WebKeys.DateFormats.DOTSCHEDULER_DATE);

	int[] monthIds = CalendarUtil.getMonthIds();
	String[] months = CalendarUtil.getMonths(locale);

	int currentYear = GregorianCalendar.getInstance().get(Calendar.YEAR);
	int previous = 100;

	if ((schedulerForm.getJobGroup() == null) || (schedulerForm.getJobGroup().equals("User Job"))) {
%>

							    	<font><%= LanguageUtil.get(pageContext, "From1") %></font>

<%
	Calendar startDateCalendar = null;
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	Date startDate;
	try {
		startDate = sdf.parse(schedulerForm.getStartDate());
	} catch (Exception e) {
		try {
			SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			startDate = sdf2.parse(schedulerForm.getStartDate());
		} catch (Exception ex) {
			startDate = new Date();
		}
	}

	if (schedulerForm.isHaveStartDate() ||
		!UtilMethods.isSet(schedulerForm.getJobGroup()) ||
		(UtilMethods.isSet(schedulerForm.getJobGroup()) &&
		 !schedulerForm.isHaveStartDate())) {
		startDateCalendar = GregorianCalendar.getInstance();
		startDateCalendar.setTime(startDate);
	}
	 String hour = (startDateCalendar.get(GregorianCalendar.HOUR_OF_DAY) < 10) ? "0"+startDateCalendar.get(GregorianCalendar.HOUR_OF_DAY) : ""+startDateCalendar.get(GregorianCalendar.HOUR_OF_DAY);
     String min = (startDateCalendar.get(GregorianCalendar.MINUTE) < 10) ? "0"+startDateCalendar.get(GregorianCalendar.MINUTE) : ""+startDateCalendar.get(GregorianCalendar.MINUTE);
%>
							<input type="checkbox" dojoType="dijit.form.CheckBox" <%=schedulerForm.isHaveStartDate()?"checked":""  %> id="haveStartDate" name="haveStartDate" />
							<input type="text" value="<%= df.format(startDate) %>" onChange="updateDate('startDate');" dojoType="dijit.form.DateTextBox" name="startDateDate"
                                            id="startDateDate" style="width:150px;" />
                            <input type="text" id="startDateTime" name="startDateTime" value='T<%=hour+":"+min%>:00' onChange="updateDate('startDate');"
                                            dojoType="dijit.form.TimeTextBox" style="width: 100px;" />
							<input type="hidden" name="startDate" value="" id="startDate">
							<script language="javascript">
							dojo.addOnLoad (function(){
<%
	if (!UtilMethods.isSet(schedulerForm.getJobGroup())) {
%>
								document.getElementById('haveStartDate').checked = true;
<%
	}
%>   							checkDate(document.forms[0].haveStartDate, 'startDate');
								updateDate('startDate');
							});
							</script>
<%
	} else {%>

							    	<font><%= LanguageUtil.get(pageContext, "From1") %></font>

<%
	if (schedulerForm.isHaveStartDate()) {
		Calendar startDateCalendar = null;
		Date startDate;
		try {
			startDate = sdf.parse(schedulerForm.getStartDate());
		} catch (Exception e) {
			try {
				SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				startDate = sdf2.parse(schedulerForm.getStartDate());
			} catch (Exception ex) {
				startDate = new Date();
			}
		}

		SimpleDateFormat sdf2 = new SimpleDateFormat("MMMM/dd/yyyy hh:mm:ss a");
%>
									&nbsp;&nbsp;&nbsp;<%= sdf2.format(startDate) %>
<%
	} else {
%>
									&nbsp;&nbsp;&nbsp;<%= LanguageUtil.get(pageContext, "Not-Specified") %>
<%
	}
%>
						</div>
					</dd>

<%
	}
%>
<%
	if ((schedulerForm.getJobGroup() == null) || (schedulerForm.getJobGroup().equals("User Job"))) {
%>
					<dd>
						<div id="endDateDiv">
							&nbsp;&nbsp;&nbsp;&nbsp;<font><%= LanguageUtil.get(pageContext, "To1") %></font>

<%
	Calendar endDateCalendar = null;
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	Date endDate;
	try {
		endDate = sdf.parse(schedulerForm.getEndDate());
	} catch (Exception e) {
		try {
			SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			endDate = sdf2.parse(schedulerForm.getEndDate());
		} catch (Exception ex) {
			endDate = new Date();
		}
	}

	if (schedulerForm.isHaveEndDate() ||
		!UtilMethods.isSet(schedulerForm.getJobGroup()) ||
		(UtilMethods.isSet(schedulerForm.getJobGroup()) &&
		 !schedulerForm.isHaveEndDate())) {
		endDateCalendar = GregorianCalendar.getInstance();
		endDateCalendar.setTime(endDate);
	}
	 String hour = (endDateCalendar.get(GregorianCalendar.HOUR_OF_DAY) < 10) ? "0"+endDateCalendar.get(GregorianCalendar.HOUR_OF_DAY) : ""+endDateCalendar.get(GregorianCalendar.HOUR_OF_DAY);
     String min = (endDateCalendar.get(GregorianCalendar.MINUTE) < 10) ? "0"+endDateCalendar.get(GregorianCalendar.MINUTE) : ""+endDateCalendar.get(GregorianCalendar.MINUTE);

%>
						 	<input type="checkbox" dojoType="dijit.form.CheckBox" <%=schedulerForm.isHaveEndDate()?"checked":""  %> id="haveEndDate" name="haveEndDate" />
							<input type="text" value="<%= df.format(endDate) %>" onChange="updateDate('endDate');" dojoType="dijit.form.DateTextBox" name="endDateDate"
                                            id="endDateDate" style="width:150px;" />
                            <input type="text" id="endDateTime" name="endDateTime" value='T<%=hour+":"+min%>:00' onChange="updateDate('endDate');"
                                            dojoType="dijit.form.TimeTextBox" style="width: 100px;" />
							<input type="hidden" name="endDate" value="" id="endDate">
							<script language="javascript">
							dojo.addOnLoad (function(){
								<%
									if (!UtilMethods.isSet(schedulerForm.getJobGroup())) {
								%>
								    document.getElementById('haveEndDate').checked = true;
								<%
									}
								%>
								checkDate(document.forms[0].haveEndDate, 'endDate');
								updateDate('endDate');
							});
							</script>
<%
	} else {
%>
							<font><%= LanguageUtil.get(pageContext, "To1") %></font>

<%
	if (schedulerForm.isHaveEndDate()) {
	Calendar endDateCalendar = null;
		Date endDate;
		try {
			endDate = sdf.parse(schedulerForm.getEndDate());
		} catch (Exception e) {
			try {
				SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				endDate = sdf2.parse(schedulerForm.getEndDate());
			} catch (Exception ex) {
				endDate = new Date();
			}
		}

		SimpleDateFormat sdf2 = new SimpleDateFormat("MMMM/dd/yyyy hh:mm:ss a");
%>
									&nbsp;&nbsp;&nbsp;<%= sdf2.format(endDate) %>
<%
	} else {
%>
									&nbsp;&nbsp;&nbsp;<%= LanguageUtil.get(pageContext, "Not-Specified") %>
<%
	}
%>

<%
	}
%>

						</div>
					</dd>

					<dt>
						<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Class-to-be-executed") %>:</b></font>
					</dt>
					<dd>
<%
	if ((schedulerForm.getJobGroup() == null) || (schedulerForm.getJobGroup().equals("User Job"))) {
%>
						<select class="form-text" id="javaClass" name="javaClass" dojoType="dijit.form.FilteringSelect" required="true" value="<%= UtilMethods.isSet(schedulerForm.getJavaClass()) ? schedulerForm.getJavaClass() : "" %>" style="width: 300px;">
						 <% for(String c : listJobClasses){ %>
							<option><%= c %></option>
						 <% } %>
						</select>
<%
	} else {
%>
						<%= schedulerForm.getJavaClass() %>
<%
	}
%>
					</dd>

			<dt>
				<span class="required"></span> <font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "cron-expression") %>: </b></font> <br>
			</dt>
			<dd>
				<input name="cronExpression" id="cronExpression" type="text" dojoType='dijit.form.ValidationTextBox' style='width: 200px'" value="<%=schedulerForm.getCronExpression() %>" size="10" />
			</dd>
			<dt><span ></span> <font class="bg" size="2"><b></b></font> <br></dt>
			<dd>
				<div style="width: 350px;  text-align: left;" id="cronHelpDiv" class="callOutBox2" >
					<h3><%= LanguageUtil.get(pageContext, "cron-examples") %></h3>
					<span style="font-size: 88%;">
						<p></p>
		        		<p><b><%= LanguageUtil.get(pageContext, "cron-once-an-hour") %>:</b> 0 0/60 * * * ?</p>
		       	 		<p><b><%= LanguageUtil.get(pageContext, "cron-twice-a-day") %>:</b> 0 0 10-11 ? * *</p>
			    		<p><b><%= LanguageUtil.get(pageContext, "cron-once-a-day-1am")%>:</b> 0 0 1 * * ?</p>
					</span>
				</div>
			</dd>
			</dl>
		</div>

		<div id="properties" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Parameters") %>">
<%
	java.util.Map<String, String> properties = schedulerForm.getMap();
	Iterator<String> keys = null;
	if (properties != null)
		keys = properties.keySet().iterator();

	boolean parameterShowed = false;
	String key;
	String value;

	if ((keys != null) && keys.hasNext()) {
		key = keys.next();
		value = properties.get(key);
	} else {
		key = "";
		value = "";
	}
%>
<%
	if ((schedulerForm.getJobGroup() == null) || (schedulerForm.getJobGroup().equals("User Job"))) {
%>
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Name") %>: </b></font><input type="text" dojoType="dijit.form.TextBox" name="propertyName0" id="propertyName0" value="<%= key %>">&nbsp;&nbsp;
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Value") %>: </b></font><input type="text" dojoType="dijit.form.TextBox" name="propertyValue0" id="propertyValue0" value="<%= value %>">
				<br>
				<br>
<%
	} else {
		if (UtilMethods.isSet(key)) {
			parameterShowed = true;
%>
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Name") %>: </b></font><%= key %>&nbsp;&nbsp;
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Value") %>: </b></font><%= value %>
				<br>
				<br>
<%
		}
	}
%>
<%
	if ((keys != null) && keys.hasNext()) {
		key = keys.next();
		value = properties.get(key);
	} else {
		key = "";
		value = "";
	}
%>
<%
	if ((schedulerForm.getJobGroup() == null) || (schedulerForm.getJobGroup().equals("User Job"))) {
%>
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Name") %>: </b></font><input type="text" dojoType="dijit.form.TextBox" name="propertyName1" id="propertyName1" value="<%= key %>">&nbsp;&nbsp;
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Value") %>: </b></font><input type="text" dojoType="dijit.form.TextBox" name="propertyValue1" id="propertyValue1" value="<%= value %>">
				<br>
				<br>
<%
	} else {
		if (UtilMethods.isSet(key)) {
			parameterShowed = true;
%>
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Name") %>: </b></font><%= key %>&nbsp;&nbsp;
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Value") %>: </b></font><%= value %>
				<br>
				<br>
<%
		}
	}
%>
<%
	if ((keys != null) && keys.hasNext()) {
		key = keys.next();
		value = properties.get(key);
	} else {
		key = "";
		value = "";
	}
%>
<%
	if ((schedulerForm.getJobGroup() == null) || (schedulerForm.getJobGroup().equals("User Job"))) {
%>
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Name") %>: </b></font><input type="text" dojoType="dijit.form.TextBox" name="propertyName2" id="propertyName2" value="<%= key %>">&nbsp;&nbsp;
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Value") %>: </b></font><input type="text" dojoType="dijit.form.TextBox" name="propertyValue2" id="propertyValue2" value="<%= value %>">
				<br>
				<br>
<%
	} else {
		if (UtilMethods.isSet(key)) {
			parameterShowed = true;
%>
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Name") %>: </b></font><%= key %>&nbsp;&nbsp;
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Value") %>: </b></font><%= value %>
				<br>
				<br>
<%
		}
	}
%>
<%
	if ((keys != null) && keys.hasNext()) {
		key = keys.next();
		value = properties.get(key);
	} else {
		key = "";
		value = "";
	}
%>
<%
	if ((schedulerForm.getJobGroup() == null) || (schedulerForm.getJobGroup().equals("User Job"))) {
%>
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Name") %>: </b></font><input type="text" dojoType="dijit.form.TextBox" name="propertyName3" id="propertyName3" value="<%= key %>">&nbsp;&nbsp;
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Value") %>: </b></font><input type="text" dojoType="dijit.form.TextBox" name="propertyValue3" id="propertyValue3" value="<%= value %>">
				<br>
				<br>
<%
	} else {
		if (UtilMethods.isSet(key)) {
			parameterShowed = true;
%>
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Name") %>: </b></font><%= key %>&nbsp;&nbsp;
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Value") %>: </b></font><%= value %>
				<br>
				<br>
<%
		}
	}
%>

<%
	if ((keys != null) && keys.hasNext()) {
		key = keys.next();
		value = properties.get(key);
	} else {
		key = "";
		value = "";
	}
%>
<%
	if ((schedulerForm.getJobGroup() == null) || (schedulerForm.getJobGroup().equals("User Job"))) {
%>
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Name") %>: </b></font><input type="text" dojoType="dijit.form.TextBox" name="propertyName4" id="propertyName4" value="<%= key %>">&nbsp;&nbsp;
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Value") %>:  </b></font><input type="text" dojoType="dijit.form.TextBox" name="propertyValue4" id="propertyValue4" value="<%= value %>">
				<br>
				<br>
<%
	} else {
		if (UtilMethods.isSet(key)) {
			parameterShowed = true;
%>
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Name") %>: </b></font><%= key %>&nbsp;&nbsp;
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Value") %>: </b></font><%= value %>
				<br>
				<br>
<%
		}
	}
%>
<%
	if ((keys != null) && keys.hasNext()) {
		key = keys.next();
		value = properties.get(key);
	} else {
		key = "";
		value = "";
	}
%>
<%
	if ((schedulerForm.getJobGroup() == null) || (schedulerForm.getJobGroup().equals("User Job"))) {
%>
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Name") %>: </b></font><input type="text" dojoType="dijit.form.TextBox" name="propertyName5" id="propertyName5" value="<%= key %>">&nbsp;&nbsp;
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Value") %>: </b></font><input type="text" dojoType="dijit.form.TextBox" name="propertyValue5" id="propertyValue5" value="<%= value %>">
				<br>
				<br>
<%
	} else {
		if (UtilMethods.isSet(key)) {
			parameterShowed = true;
%>
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Name") %>: </b></font><%= key %>&nbsp;&nbsp;
				<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "Parameter-Value") %>: </b></font><%= value %>
				<br>
				<br>
<%
		}
	}
%>
<%
	if ((schedulerForm.getJobGroup() != null) && (schedulerForm.getJobGroup().equals("Recurrent Campaign")) && !parameterShowed) {
%>
			<font class="bg" size="2"><%= LanguageUtil.get(pageContext, "There-are-no-Parameters-to-show") %></font>
<%
	}
%>
		</div>
	</div>
<%
	if ((schedulerForm.getJobGroup() == null) || (schedulerForm.getJobGroup().equals("User Job"))) {
%>
	<div class="buttonRow">
	<% if ((schedulerForm != null) && (UtilMethods.isSet(schedulerForm.getJobGroup()))) { %>
		<button dojoType="dijit.form.Button" onClick="deleteSchedule(document.getElementById('fm'))" iconClass="deleteIcon">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Delete")) %>
		</button>
	<% } %>
		<button dojoType="dijit.form.Button"  onClick="cancelEdit();return false;" iconClass="cancelIcon">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cancel")) %>
		</button>
		<button dojoType="dijit.form.Button" onClick="submitfm(document.getElementById('fm'))" iconClass="saveIcon">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save")) %>
		</button>
	</div>
<%
	}
%>
</html:form>
</liferay:box>