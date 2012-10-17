<%@page import="com.liferay.util.cal.CalendarUtil"%>
<%@page import="java.util.GregorianCalendar"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<%

	Date recurrenceStarts = contentletForm.getRecurrenceStartsDate();
	Date recurrenceEnds = contentletForm.getRecurrenceEndsDate();
	
	int[] monthIds = CalendarUtil.getMonthIds();
	String[] months = CalendarUtil.getMonths(locale);
	
	GregorianCalendar cal = new GregorianCalendar();
	cal.setTime((Date) recurrenceEnds);
	int dayOfMonth = cal.get(GregorianCalendar.DAY_OF_MONTH);
	int month = cal.get(GregorianCalendar.MONTH);
	int year = cal.get(GregorianCalendar.YEAR);
	
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	if (UtilMethods.isSet(contentletForm.getRecurrenceDaysOfWeek())) {
		java.util.Arrays.sort(contentletForm.getRecurrenceDaysOfWeek());
	}
%>	
<html:hidden property="disconnectedFrom" styleId="disconnectedFrom" /> 
<html:hidden property="originalStartDate" styleId="originalStartDate" /> 
<html:hidden property="originalEndDate" styleId="originalEndDate" /> 
<html:hidden property="recurrenceChanged" styleId="recurrenceChanged" />
<html:hidden property="recurrenceWeekOfMonth" styleId="recurrenceWeekOfMonth" />
<html:hidden property="recurrenceDayOfWeek" styleId="recurrenceDayOfWeek"  />
<html:hidden property="recurrenceMonthOfYear" styleId="recurrenceMonthOfYear"  />
<input type="hidden" id="isSpecificDate" name="isSpecificDate" value="<%= contentletForm.isSpecificDate() %>"/>
	<div class="fieldWrapper">	
		<div class="fieldName"><%= LanguageUtil.get(pageContext, "Repeats") %>:</div>
		<div class="fieldValue">
			<input dojoType="dijit.form.RadioButton" type="radio" onclick="recurrenceHasChanged()" name="recurrenceOccurs" id="recursNever" value="never" <%= (UtilMethods.isSet(contentletForm.getRecurrenceOccurs()) && contentletForm.getRecurrenceOccurs().equals("never")) ? "checked" : "" %> /><label for="recursNever"><%= LanguageUtil.get(pageContext, "never") %></label>&nbsp;&nbsp;
			<input dojoType="dijit.form.RadioButton" type="radio" onclick="recurrenceHasChanged()" name="recurrenceOccurs" id="recursDaily" value="daily" <%= (UtilMethods.isSet(contentletForm.getRecurrenceOccurs()) && contentletForm.getRecurrenceOccurs().equals("daily")) ? "checked" : "" %> /><label for="recursDaily"><%= LanguageUtil.get(pageContext, "daily") %></label>&nbsp;
			<input dojoType="dijit.form.RadioButton" type="radio" onclick="recurrenceHasChanged()" name="recurrenceOccurs" id="recursWeekly" value="weekly" <%= (UtilMethods.isSet(contentletForm.getRecurrenceOccurs()) && contentletForm.getRecurrenceOccurs().equals("weekly")) ? "checked" : "" %> /><label for="recursWeekly"><%= LanguageUtil.get(pageContext, "weekly") %></label>&nbsp;
			<input dojoType="dijit.form.RadioButton" type="radio" onclick="recurrenceHasChanged()" name="recurrenceOccurs" id="recursMonthly" value="monthly" <%= (UtilMethods.isSet(contentletForm.getRecurrenceOccurs()) && contentletForm.getRecurrenceOccurs().equals("monthly")) ? "checked" : "" %> /><label for="recursMonthly"><%= LanguageUtil.get(pageContext, "monthly") %></label>&nbsp;
			<input dojoType="dijit.form.RadioButton" type="radio" onclick="recurrenceHasChanged()" name="recurrenceOccurs" id="recursAnnually" value="annually" <%= (UtilMethods.isSet(contentletForm.getRecurrenceOccurs()) && contentletForm.getRecurrenceOccurs().equals("annually")) ? "checked" : "" %> /><label for="recursAnnually"><%= LanguageUtil.get(pageContext, "annually") %></label>&nbsp;
			<br/>
			<div id="recurence" class="recurenceWrapper roundBox" style="display: none;width:600px;padding:10px">
				<html:hidden property="recurrenceInterval" styleId="recurrenceInterval" /> 
				
				
				
				
				
				
				<%--       DAILY RECURRANCE        --%>
				<div id="dailyRecurrence" class="eventRecurrence">
					<%= LanguageUtil.get(pageContext, "Every") %>: 
						<input type="text" dojoType="dijit.form.TextBox" name="recurrenceIntervalDaily" id="recurrenceIntervalDaily" onchange="setRecurrenceInterval('recurrenceIntervalDaily');" style="width: 30px;" value="<%= UtilMethods.isSet(contentletForm.getRecurrenceInterval()) ? contentletForm.getRecurrenceInterval() : "" %>" /> 
						<%= LanguageUtil.get(pageContext, "day-s-") %>&nbsp; 
				</div> 
				
				
				
				
				
				
				
				<%--       WEEKLY RECURRANCE        --%>
				<div id="weeklyRecurrence"  style="border:0px solid red;padding:3px;white-space:nowrap">
					<%= LanguageUtil.get(pageContext, "Every") %>:  
					<input type="text" dojoType="dijit.form.TextBox" name="recurrenceIntervalWeekly" id="recurrenceIntervalWeekly" onchange="setRecurrenceInterval('recurrenceIntervalWeekly');" style="width: 30px;" value="<%= UtilMethods.isSet(contentletForm.getRecurrenceInterval()) ? contentletForm.getRecurrenceInterval() : "" %>" /> 
					<%= LanguageUtil.get(pageContext, "week-s-on") %>&nbsp;: &nbsp;

				
				    <input type="checkbox" dojoType="dijit.form.CheckBox" name="recurrenceDaysOfWeek" id="Sunday" value="<%= String.valueOf(Calendar.SUNDAY) %>" <%= (UtilMethods.isSet(contentletForm.getRecurrenceDaysOfWeek()) && (-1 < java.util.Arrays.binarySearch(contentletForm.getRecurrenceDaysOfWeek(), String.valueOf(Calendar.SUNDAY)))) ? "checked" : "" %> />
					<label for="Sunday"><%= LanguageUtil.get(pageContext, "Sunday").charAt(0) %></label>
					&nbsp;
				    <input type="checkbox" dojoType="dijit.form.CheckBox" name="recurrenceDaysOfWeek" id="Monday" value="<%= String.valueOf(Calendar.MONDAY) %>" <%= (UtilMethods.isSet(contentletForm.getRecurrenceDaysOfWeek()) && (-1 < java.util.Arrays.binarySearch(contentletForm.getRecurrenceDaysOfWeek(), String.valueOf(Calendar.MONDAY)))) ? "checked" : "" %> />
					<label for="Monday"><%= LanguageUtil.get(pageContext, "Monday").charAt(0) %></label>
					&nbsp;
				    <input type="checkbox" dojoType="dijit.form.CheckBox" name="recurrenceDaysOfWeek" id="Tuesday" value="<%= String.valueOf(Calendar.TUESDAY) %>" <%= (UtilMethods.isSet(contentletForm.getRecurrenceDaysOfWeek()) && (-1 < java.util.Arrays.binarySearch(contentletForm.getRecurrenceDaysOfWeek(), String.valueOf(Calendar.TUESDAY)))) ? "checked" : "" %> />
					<label for="Tuesday"><%= LanguageUtil.get(pageContext, "Tuesday").charAt(0) %></label>
					&nbsp;
				    <input type="checkbox" dojoType="dijit.form.CheckBox" name="recurrenceDaysOfWeek" id="Wednesday" value="<%= String.valueOf(Calendar.WEDNESDAY) %>" <%= (UtilMethods.isSet(contentletForm.getRecurrenceDaysOfWeek()) && (-1 < java.util.Arrays.binarySearch(contentletForm.getRecurrenceDaysOfWeek(), String.valueOf(Calendar.WEDNESDAY)))) ? "checked" : "" %> />
					<label for="Wednesday"><%= LanguageUtil.get(pageContext, "Wednesday").charAt(0) %></label>
					&nbsp;
				    <input type="checkbox" dojoType="dijit.form.CheckBox" name="recurrenceDaysOfWeek" id="Thursday" value="<%= String.valueOf(Calendar.THURSDAY) %>" <%= (UtilMethods.isSet(contentletForm.getRecurrenceDaysOfWeek()) && (-1 < java.util.Arrays.binarySearch(contentletForm.getRecurrenceDaysOfWeek(), String.valueOf(Calendar.THURSDAY)))) ? "checked" : "" %> />
					<label for="Thursday"><%= LanguageUtil.get(pageContext, "Thursday").charAt(0) %></label>
					&nbsp;
				    <input type="checkbox" dojoType="dijit.form.CheckBox" name="recurrenceDaysOfWeek" id="Friday" value="<%= String.valueOf(Calendar.FRIDAY) %>" <%= (UtilMethods.isSet(contentletForm.getRecurrenceDaysOfWeek()) && (-1 < java.util.Arrays.binarySearch(contentletForm.getRecurrenceDaysOfWeek(), String.valueOf(Calendar.FRIDAY)))) ? "checked" : "" %> />
					<label for="Friday"><%= LanguageUtil.get(pageContext, "Friday").charAt(0) %></label>
					&nbsp;
				    <input type="checkbox" dojoType="dijit.form.CheckBox" name="recurrenceDaysOfWeek" id="Saturday" value="<%= String.valueOf(Calendar.SATURDAY) %>" <%= (UtilMethods.isSet(contentletForm.getRecurrenceDaysOfWeek()) && (-1 < java.util.Arrays.binarySearch(contentletForm.getRecurrenceDaysOfWeek(), String.valueOf(Calendar.SATURDAY)))) ? "checked" : "" %> />
					<label for="Saturday"><%= LanguageUtil.get(pageContext, "Saturday").charAt(0) %></label>
					
					<div style="clear: both;"></div>
				</div> 
				
				
				
				
				
				
				<%--       MONTHLY RECURRANCE        --%>
				<div id="monthlyRecurrence">
					<style>
						#monthRecurrTable td{
							padding:3px;
							font-size:100%;
						}
						#yearRecurrTable td{
							padding:3px;
							font-size:100%;
						}
					</style>
					<table id="monthRecurrTable">
						<tr>
							<td><%= LanguageUtil.get(pageContext, "Every") %>:</td>
							<td>
								<input type="text" dojoType="dijit.form.TextBox" name="recurrenceIntervalMonthly" id="recurrenceIntervalMonthly" onchange="setRecurrenceInterval('recurrenceIntervalMonthly');" style="width: 30px;" value="<%= UtilMethods.isSet(contentletForm.getRecurrenceInterval()) ? contentletForm.getRecurrenceInterval() : "" %>" /> <%= LanguageUtil.get(pageContext, "month-s-") %>
							</td>
						</tr>
						<tr>
						    <td>
						        <input dojoType="dijit.form.RadioButton" type="radio" onclick="specificDateChanged('monthly', false)" name="isSpecificDateMonth" id="noSpecificDateMonth" value="false" <%= (!contentletForm.isSpecificDate()) ? "checked" : "" %> />
						        <label for="noSpecificDateMonth"><%= LanguageUtil.get(pageContext, "On-The") %>:</label>
						    </td>
							
							<td>
							    <select id="recurrenceWeekOfMonthM" name="recurrenceWeekOfMonthM" dojoType="dijit.form.FilteringSelect" style="width: 90px;" onchange="setRecurrenceWeekOfMonth('month');" <%= (!contentletForm.isSpecificDate()) ? "" : "disabled" %>>
								  <option value="1" <%=contentletForm.getRecurrenceWeekOfMonth()==1?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "1st") %></option>
							      <option value="2" <%=contentletForm.getRecurrenceWeekOfMonth()==2?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "2nd") %></option>
							      <option value="3" <%=contentletForm.getRecurrenceWeekOfMonth()==3?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "3rd") %></option>
								  <option value="4" <%=contentletForm.getRecurrenceWeekOfMonth()==4?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "4th") %></option>
								  <option value="5" <%=contentletForm.getRecurrenceWeekOfMonth()==5?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "Last") %></option>
								</select>
								&nbsp;
								<select id="recurrenceDayOfWeekM" name="recurrenceDayOfWeekM" dojoType="dijit.form.FilteringSelect" style="width: 125px;" onchange="setRecurrenceDayOfWeek('month');" <%= (!contentletForm.isSpecificDate()) ? "" : "disabled" %>>
								  <option value="1" <%=contentletForm.getRecurrenceDayOfWeek()==1?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "Sunday") %></option>
								  <option value="2" <%=contentletForm.getRecurrenceDayOfWeek()==2?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "Monday") %></option>
								  <option value="3" <%=contentletForm.getRecurrenceDayOfWeek()==3?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "Tuesday") %></option>
								  <option value="4" <%=contentletForm.getRecurrenceDayOfWeek()==4?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "Wednesday") %></option>
								  <option value="5" <%=contentletForm.getRecurrenceDayOfWeek()==5?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "Thursday") %></option>
								  <option value="6" <%=contentletForm.getRecurrenceDayOfWeek()==6?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "Friday") %></option>
								  <option value="7" <%=contentletForm.getRecurrenceDayOfWeek()==7?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "Saturday") %></option>
								</select>	
								
								
								<%= LanguageUtil.get(pageContext, "of-the-month") %>
							</td>
						</tr>
						<tr>
							<td><%= LanguageUtil.get(pageContext, "or") %></td>
							<td>
								&nbsp;
							</td>
						</tr>
						<tr>
							<td>
							 <input dojoType="dijit.form.RadioButton" type="radio" onclick="specificDateChanged('monthly', true)" name="isSpecificDateMonth" id="specificDateMonth" value="true" <%= (contentletForm.isSpecificDate()) ? "checked" : "" %> />
						     <label for="specificDateMonth"><%= LanguageUtil.get(pageContext, "Specific-Day") %>:</label>
							</td>
							<td>
						        <input type="text" dojoType="dijit.form.TextBox" name="recurrenceDayOfMonth"  id="recurrenceDayOfMonthM" style="width: 30px;" 
						           value="<%= UtilMethods.isSet((String)contentletForm.getRecurrenceDayOfMonth()) ? (String)contentletForm.getRecurrenceDayOfMonth() : "" %>" 
						           <%= (contentletForm.isSpecificDate()) ? "" : "disabled" %>
						           /> 
							</td>
						</tr>
					</table>

					
				</div> 
				
				
				
				
				
				<%--       YEARLY RECURRANCE        --%>
				<div id="annualRecurrence">
				
					<table id="yearRecurrTable">
						<tr>
							<td><%= LanguageUtil.get(pageContext, "Every") %>:</td>
							<td>
								<input type="text" dojoType="dijit.form.TextBox" name="recurrenceIntervalYearly" id="recurrenceIntervalYearly" onchange="setRecurrenceInterval('recurrenceIntervalYearly');" style="width: 30px;" value="<%= UtilMethods.isSet(contentletForm.getRecurrenceInterval()) ? contentletForm.getRecurrenceInterval() : "" %>" /> <%= LanguageUtil.get(pageContext, "year-s-") %>
							</td>
						</tr>
						<tr>
						     <td>
								<input dojoType="dijit.form.RadioButton" type="radio" onclick="specificDateChanged('annually', false)" name="isSpecificDateYear" id="noSpecificDateYear" value="false" <%= (!contentletForm.isSpecificDate()) ? "checked" : "" %> />
						        <label for="noSpecificDateYear"><%= LanguageUtil.get(pageContext, "On-The") %>:</label>
							</td>
							<td>
								<select id="recurrenceWeekOfMonthY" name="recurrenceWeekOfMonthY" dojoType="dijit.form.FilteringSelect" style="width: 70px;" onchange="setRecurrenceWeekOfMonth('year');" <%= (!contentletForm.isSpecificDate()) ? "" : "disabled" %>>
								  <option value="1" <%=contentletForm.getRecurrenceWeekOfMonth()==1?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "1st") %></option>
								  <option value="2" <%=contentletForm.getRecurrenceWeekOfMonth()==2?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "2nd") %></option>
								  <option value="3" <%=contentletForm.getRecurrenceWeekOfMonth()==3?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "3rd") %></option>
								  <option value="4" <%=contentletForm.getRecurrenceWeekOfMonth()==4?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "4th") %></option>
								  <option value="5" <%=contentletForm.getRecurrenceWeekOfMonth()==5?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "Last") %></option>
								</select>
								&nbsp;
								<select id="recurrenceDayOfWeekY" name="recurrenceDayOfWeekY" dojoType="dijit.form.FilteringSelect" style="width: 125px;" onchange="setRecurrenceDayOfWeek('year');" <%= (!contentletForm.isSpecificDate()) ? "" : "disabled" %>>
								  <option value="1" <%=contentletForm.getRecurrenceDayOfWeek()==1?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "Sunday") %></option>
								  <option value="2" <%=contentletForm.getRecurrenceDayOfWeek()==2?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "Monday") %></option>
								  <option value="3" <%=contentletForm.getRecurrenceDayOfWeek()==3?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "Tuesday") %></option>
								  <option value="4" <%=contentletForm.getRecurrenceDayOfWeek()==4?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "Wednesday") %></option>
								  <option value="5" <%=contentletForm.getRecurrenceDayOfWeek()==5?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "Thursday") %></option>
								  <option value="6" <%=contentletForm.getRecurrenceDayOfWeek()==6?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "Friday") %></option>
								  <option value="7" <%=contentletForm.getRecurrenceDayOfWeek()==7?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "Saturday") %></option>
								</select>
								&nbsp;
								<%= LanguageUtil.get(pageContext, "of") %>
								&nbsp;
								<select id="recurrenceMonthOfYear" name="recurrenceMonthOfYearY" dojoType="dijit.form.FilteringSelect" style="width: 125px;" onchange="setRecurrenceMonthOfYear();" <%= (!contentletForm.isSpecificDate()) ? "" : "disabled" %>>
								  <option value="1" <%=contentletForm.getRecurrenceMonthOfYear()==1?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "January") %></option>
								  <option value="2" <%=contentletForm.getRecurrenceMonthOfYear()==2?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "February") %></option>
								  <option value="3" <%=contentletForm.getRecurrenceMonthOfYear()==3?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "March") %></option>
								  <option value="4" <%=contentletForm.getRecurrenceMonthOfYear()==4?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "April") %></option>
								  <option value="5" <%=contentletForm.getRecurrenceMonthOfYear()==5?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "May") %></option>
								  <option value="6" <%=contentletForm.getRecurrenceMonthOfYear()==6?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "June") %></option>
								  <option value="7" <%=contentletForm.getRecurrenceMonthOfYear()==7?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "July") %></option>
								  <option value="8" <%=contentletForm.getRecurrenceMonthOfYear()==8?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "August") %></option>
								  <option value="9" <%=contentletForm.getRecurrenceMonthOfYear()==9?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "September") %></option>
								  <option value="10" <%=contentletForm.getRecurrenceMonthOfYear()==10?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "October") %></option>
								  <option value="11" <%=contentletForm.getRecurrenceMonthOfYear()==11?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "November") %></option>
								  <option value="12" <%=contentletForm.getRecurrenceMonthOfYear()==12?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "December") %></option>
								</select>
								
								
							</td>
							
						</tr>
						<tr>
							<td><%= LanguageUtil.get(pageContext, "or") %></td>
							<td>
								&nbsp;
							</td>
						</tr>
						
						<tr>					
							<td>
							  <input dojoType="dijit.form.RadioButton" type="radio" onclick="specificDateChanged('annually', true)" name="isSpecificDateYear" id="specificDateYear" value="true" <%= (contentletForm.isSpecificDate()) ? "checked" : "" %> />
						      <label for="specificDateYear"><%= LanguageUtil.get(pageContext, "Specific-Day") %>:</label>
							<td>
							
							  <select id="specificDayOfMonthRecY" name="specificDayOfMonthRecY" dojoType="dijit.form.FilteringSelect" style="width: 125px;" <%= (contentletForm.isSpecificDate()) ? "" : "disabled" %>>
							   <%for(int i =1; i<32; i++){ %>
							      <option value="<%=i %>" <%=contentletForm.getSpecificDayOfMonthRecY().equals(String.valueOf(i))?"selected=\"selected\"":"" %>><%= i %></option>
							   <%} %>
							   </select>
							   	
							   <%= LanguageUtil.get(pageContext, "of") %>
								&nbsp;
								<select id="specificMonthOfYearRecY" name="specificMonthOfYearRecY" dojoType="dijit.form.FilteringSelect" style="width: 125px;" <%= (contentletForm.isSpecificDate()) ? "" : "disabled" %>>
								  <option value="1" <%=contentletForm.getSpecificMonthOfYearRecY().equals("1")?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "January") %></option>
								  <option value="2" <%=contentletForm.getSpecificMonthOfYearRecY().equals("2")?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "February") %></option>
								  <option value="3" <%=contentletForm.getSpecificMonthOfYearRecY().equals("3")?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "March") %></option>
								  <option value="4" <%=contentletForm.getSpecificMonthOfYearRecY().equals("4")?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "April") %></option>
								  <option value="5" <%=contentletForm.getSpecificMonthOfYearRecY().equals("5")?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "May") %></option>
								  <option value="6" <%=contentletForm.getSpecificMonthOfYearRecY().equals("6")?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "June") %></option>
								  <option value="7" <%=contentletForm.getSpecificMonthOfYearRecY().equals("7")?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "July") %></option>
								  <option value="8" <%=contentletForm.getSpecificMonthOfYearRecY().equals("8")?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "August") %></option>
								  <option value="9" <%=contentletForm.getSpecificMonthOfYearRecY().equals("9")?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "September") %></option>
								  <option value="10" <%=contentletForm.getSpecificMonthOfYearRecY().equals("10")?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "October") %></option>
								  <option value="11" <%=contentletForm.getSpecificMonthOfYearRecY().equals("11")?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "November") %></option>
								  <option value="12" <%=contentletForm.getSpecificMonthOfYearRecY().equals("12")?"selected=\"selected\"":"" %>><%= LanguageUtil.get(pageContext, "December") %></option>
								</select>

							</td>
						</tr>

						
					  </table>
						
						

						
			
						
				</div> 
				<div>
				    <br/>		
				    <input type="checkbox" dojoType="dijit.form.CheckBox" name="noEndDate" id="noEndDate" value="<%= contentletForm.isNoEndDate() %>" <%= contentletForm.isNoEndDate()?"checked" : "" %> onclick="toggleEndDate();"/>
					<label for="noEndDate"><%= LanguageUtil.get(pageContext, "No End Date") %></label>
				</div>	
				<div id="recurrenceEndDate" class="recurrenceEndDate">
					<br/>
					<input type="hidden" id="startRecurrenceDate" name="recurrenceStarts" value="<%= df.format(recurrenceStarts) %>" /> 
					<input type="hidden" id="endRecurrenceDate" name="recurrenceEnds" value="<%= df.format(recurrenceEnds) %>" /> 
					<%= LanguageUtil.get(pageContext, "Ends") %>: 
					<%SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");%>
					
						<input type="text" value="<%=df2.format(recurrenceEnds) %>" onChange="updateRecurrenceEndDate('endRecurrenceDate');" 
					 	       dojoType="dijit.form.DateTextBox"  name="endRecurrenceDateDate"  id="endRecurrenceDateDate" style="width:100px;">
					
				</div>				
			</div>
			
		</div>
		<div class="clear"></div>
	</div>
	
		<div id="recurrenceEndDate_calendarDialog" dojoType="dijit.Dialog" title="" style="width: 200px; height: 230px;">
			<div id="recurrenceEndDate_calendar" dojoType="dojox.widget.Calendar">
				<script type="dojo/connect" event="onValueSelected" args="date">
					dijit.byId('endRecurrenceDateMonth').attr('value',date.getMonth());
					dijit.byId('endRecurrenceDateYear').attr('value',date.getFullYear());
	    			dijit.byId('endRecurrenceDateDay').attr('value',date.getDate());
 					dijit.byId('recurrenceEndDate_calendarDialog').hide(); 	
	  			</script>
			</div>        
		</div>
		<style type="text/css">
			#recurrenceEndDate_calendarDialog_underlay { background-color:transparent; }
		</style>
	<script type="text/javascript">

	  function setRecurrenceWeekOfMonth(type){

		  var recurrenceWeekOfMonthSelect = 1;
		  if(type=='month'){
			  recurrenceWeekOfMonthSelect = dijit.byId('recurrenceWeekOfMonthM').value;
		  }else if(type=='year'){
			  recurrenceWeekOfMonthSelect = dijit.byId('recurrenceWeekOfMonthY').value;
		  }

		 document.getElementById('recurrenceWeekOfMonth').value = recurrenceWeekOfMonthSelect;

	  }

	  function setRecurrenceDayOfWeek(type){

		  var recurrenceDayOfWeekSelect = 1;
		  if(type=='month'){
			  recurrenceDayOfWeekSelect = dijit.byId('recurrenceDayOfWeekM').value;
		  }else if(type=='year'){
			  recurrenceDayOfWeekSelect = dijit.byId('recurrenceDayOfWeekY').value;
		  }

		 document.getElementById('recurrenceDayOfWeek').value = recurrenceDayOfWeekSelect;
	  }


	  function setRecurrenceMonthOfYear(){
		  var recurrenceMonthOfYear = document.getElementById('recurrenceMonthOfYear');
		  recurrenceMonthOfYear.value = dijit.byId('recurrenceMonthOfYear').value;
	  }
	  


	    function toggleEndDate(){
            if($('recurrenceEndDate').style.display=='none'){
            	$('recurrenceEndDate').show();
            	$('noEndDate').value='false';
            }else{
            	$('recurrenceEndDate').hide();
            	$('noEndDate').value='true';
            }

		    
	    }
		
	    function updateStartRecurrenceDate(varName) {
			var field = $('startRecurrenceDate');
			var dateValue ="";
			var myDate = dijit.byId(varName + "Date");
			if(myDate == null){
				myDate = new Date();
			}
			var x = myDate.getValue();
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
		
		function recurrenceHasChanged() {
			$('dailyRecurrence').hide();
			$('weeklyRecurrence').hide();
			$('monthlyRecurrence').hide();
			$('annualRecurrence').hide();
			if($('recursNever').checked) {
				$('recurence').hide();
			} else if($('recursDaily').checked) {
				$('dailyRecurrence').show();
				$('recurence').show();
			} else if($('recursWeekly').checked) {
				$('weeklyRecurrence').show();
				$('recurence').show();
			} else if($('recursMonthly').checked) {
				$('monthlyRecurrence').show();
				$('recurence').show();
			}else if($('recursAnnually').checked) {
				$('annualRecurrence').show();
				$('recurence').show();
			}
			
			
			if(!dijit.byId('recursNever').checked){
				var startDate 	= dojo.byId('<%=startDateField.getVelocityVarName()%>');
				var endDate 	= dojo.byId('<%=endDateField.getVelocityVarName()%>');
				var endDateDate 	= dijit.byId('<%=endDateField.getVelocityVarName()%>Date');
				var startDateD = dojo.date.locale.parse(startDate.value, { datePattern: 'yyyy-MM-dd HH:mm', selector: "date" });
				var endDateD = dojo.date.locale.parse(endDate.value, { datePattern: 'yyyy-MM-dd HH:mm', selector: "date" });
				shownRecurrenceMessage = false;

				startDateD.setHours(0);
				startDateD.setMinutes(0);
				startDateD.setSeconds(0);
				endDateDate.setValue(startDateD);
				endDateDate.setValue(startDateD);
				updateDate('<%=endDateField.getVelocityVarName()%>');
				endDateDate.readOnly=true;
				
			}
			else{
				var endDateDate 	= dijit.byId('<%=endDateField.getVelocityVarName()%>Date');
				endDateDate.readOnly=false;
			}
			
			$('recurrenceChanged').value = "true";
		}
		
		function getEndRecurrenceDate() {
			var date = new Date(dijit.byId('endRecurrenceDateYear').attr('value'), 
				dijit.byId('endRecurrenceDateMonth').attr('value'),
				dijit.byId('endRecurrenceDateDay').attr('value'));
			return date;
		}
		
		function <portlet:namespace />setCalendarDate_endRecurrenceDate(year, month, day) {
			var dateStr = "" + year + '-' + month + '-' + day;
			dijit.byId('endRecurrenceDateYear').attr('value', year);
			dijit.byId('endRecurrenceDateMonth').attr('value', (parseInt(month) - 1));
			dijit.byId('endRecurrenceDateDay').attr('value', day);
			
			$('endRecurrenceDate').value = dateStr;
		}
		
		function setRecurrenceInterval(recurrence){
		   $('recurrenceInterval').value = $(recurrence).value;
		}
		dojo.addOnLoad( function(){
			recurrenceHasChanged();
		});
		
        function <portlet:namespace />dojoCalendarOnClick_endRecurrenceDate(date){            
        	dijit.byId('recurrenceEndDate_calendarDialog').show();
        	dijit.byId('recurrenceEndDate_calendar').attr('value',date);            	
        }

		<% if(contentletForm.isNoEndDate()){%>
	       toggleEndDate();
	    <%}%>


	    function updateRecurrenceEndDate(varName) {
			var field = $(varName);
			var dateValue = "";
			var myDate = dijit.byId(varName + "Date");
			if(myDate == null){
				myDate = new Date();
			}
			var x = myDate.getValue();
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


	    function specificDateChanged(recurType, isSpecificDate){

			if(recurType == 'monthly'){
				if(isSpecificDate){
					document.getElementById('isSpecificDate').value = true;
					dijit.byId('noSpecificDateMonth').attr('checked',false);
					dijit.byId('specificDateMonth').attr('checked',true);
					dijit.byId('recurrenceDayOfMonthM').attr('disabled',false);
					dijit.byId('recurrenceWeekOfMonthM').attr('disabled',true);
					dijit.byId('recurrenceDayOfWeekM').attr('disabled',true);
				}else{
					document.getElementById('isSpecificDate').value = false;
					dijit.byId('noSpecificDateMonth').attr('checked',true);
					dijit.byId('specificDateMonth').attr('checked',false);
					dijit.byId('recurrenceDayOfMonthM').attr('disabled',true);
					dijit.byId('recurrenceWeekOfMonthM').attr('disabled',false);
					dijit.byId('recurrenceDayOfWeekM').attr('disabled',false);
				}

			}else{
				if(isSpecificDate){
					document.getElementById('isSpecificDate').value = true;
					dijit.byId('noSpecificDateYear').attr('checked',false);
					dijit.byId('specificDateYear').attr('checked',true);
					dijit.byId('specificDayOfMonthRecY').attr('disabled',false);
					dijit.byId('specificMonthOfYearRecY').attr('disabled',false);
					dijit.byId('recurrenceWeekOfMonthY').attr('disabled',true);
					dijit.byId('recurrenceDayOfWeekY').attr('disabled',true);
					dijit.byId('recurrenceMonthOfYear').attr('disabled',true);
				}else{
					document.getElementById('isSpecificDate').value = false;
					dijit.byId('noSpecificDateYear').attr('checked',true);
					dijit.byId('specificDateYear').attr('checked',false);
					dijit.byId('specificDayOfMonthRecY').attr('disabled',true);
					dijit.byId('specificMonthOfYearRecY').attr('disabled',true);
					dijit.byId('recurrenceWeekOfMonthY').attr('disabled',false);
					dijit.byId('recurrenceDayOfWeekY').attr('disabled',false);
					dijit.byId('recurrenceMonthOfYear').attr('disabled',false);
				}
			}

		}
		
		
	</script>