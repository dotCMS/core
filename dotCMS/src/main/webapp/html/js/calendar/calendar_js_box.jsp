<%@ include file="/html/common/init.jsp" %>
<%
Calendar curCal = new GregorianCalendar(timeZone, locale);
int curMonth = curCal.get(Calendar.MONTH);
int curDay = curCal.get(Calendar.DATE);
int curYear = curCal.get(Calendar.YEAR);

String calendar_num = request.getParameter("calendar_num");
int cal_num = Integer.parseInt(calendar_num);

	for (int i=0;i<cal_num;i++) {
%>
	// Calendar Stuff
	var <portlet:namespace />calObj_<%=i%> = new Calendar(false, null, <portlet:namespace />calendarOnSelect_<%=i%>, <portlet:namespace />calendarOnClose);
	<portlet:namespace />calObj_<%=i%>.weekNumbers = false;
	<portlet:namespace />calObj_<%=i%>.firstDayOfWeek = <%= curCal.getFirstDayOfWeek() - 1 %>;
	<portlet:namespace />calObj_<%=i%>.setTtDateFormat("%A, %B %e, %Y");
	<portlet:namespace />calObj_<%=i%>.setRange(<%= curYear %>, <%= curYear + 10 %>);

	function <portlet:namespace />calendarOnClick_<%=i%>(id) {
		if (id == "<portlet:namespace />calObj_<%=i%>") {
			<portlet:namespace />calObj_<%=i%>.create();
			//<portlet:namespace />calObj_<%=i%>.setDate(new Date(myForm.calendar_<%=i%>_year.value, myForm.calendar_<%=i%>_month.value, myForm.calendar_<%=i%>_day.value));
			<portlet:namespace />calObj_<%=i%>.showAtElement(document.getElementById('<portlet:namespace />calendar_input_<%=i%>_button'), 'br');
		}
	}
	function <portlet:namespace />calendarOnClose(cal) {
		cal.hide();
	};

	function <portlet:namespace />calendarOnSelect_<%=i%>(cal) {
		if (cal.dateClicked) {
			var month = cal.date.getMonth() + 1;
			var day = cal.date.getDate();
			var year = cal.date.getFullYear();
			var new_date = month + "/" + day + "/" + year;
			if (<%=i%> == 0) {
				document.getElementById("start_date").value = new_date;
			}
			else {
				document.getElementById("end_date").value = new_date;
			}
			cal.callCloseHandler();
		}
	};
<% } %>
