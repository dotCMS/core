<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<%@page import="java.util.TimeZone"%>
<%@page import="java.util.Date"%>


<div id="monthlyView" style="display:none;">

	<div class="yui-gb" id="monthControls" style="padding: 8px 5px;">
		<div class="yui-u first">
			<button dojoType="dijit.form.Button" onClick="previousMonth()" >
				<span class="arrowLeftIcon"></span>
			</button>
			<button dojoType="dijit.form.Button" onClick="nextMonth()" >
				<span class="arrowRightIcon"></span>
			</button>
			<button dojoType="dijit.form.Button" onClick="setTodayView()" >
	    		<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Today")) %>
			</button>
		</div>
		<div class="yui-u"style=" text-align:center;padding-top:5px;">
			<span id="monthMonthlyViewName" class="monthName"></span>
		</div>
	    <div class="yui-u">
	    	&nbsp;
		</div> 
	</div>
	
	<div id="monthWrapper" style="overflow-y:auto;overflow-x:hidden;">
		<table class="listingTable">
			<thead> 
				<tr>
					<th width="14%"><%= LanguageUtil.get(pageContext, "SUN") %></th>
					<th width="14%"><%= LanguageUtil.get(pageContext, "MON") %></th>
					<th width="14%"><%= LanguageUtil.get(pageContext, "TUE") %></th>
					<th width="14%"><%= LanguageUtil.get(pageContext, "WED") %></th>
					<th width="14%"><%= LanguageUtil.get(pageContext, "THU") %></th>
					<th width="14%"><%= LanguageUtil.get(pageContext, "FRI") %></th>
					<th width="14%"><%= LanguageUtil.get(pageContext, "SAT") %></th>
				</tr>
			</thead>
			<tbody id="calendarMonthlyViewBody" class="calendarBody"></tbody>
		</table>
	</div>
</div>
			
<script type="text/javascript">

	function initializeMonthlyCalendar() {
	
		renderMonthlyCalendar();
	}
	
	function previousMonth() {

		var days = daysInMonth(selectedDate.getFullYear(), selectedDate.getMonth() - 1);
		var day = selectedDate.getDate() > days?days:selectedDate.getDate();
		selectedDate = new Date(selectedDate.getFullYear(), selectedDate.getMonth() - 1, day);
		calendarCookieProvider.set("selectedDate", selectedDate);
		
		renderNavCalendar();
		renderMonthlyCalendar();
	}
	
	function nextMonth() {

		var days = daysInMonth(selectedDate.getFullYear(), selectedDate.getMonth() + 1);
		var day = selectedDate.getDate() > days?days:selectedDate.getDate();
		selectedDate = new Date(selectedDate.getFullYear(), selectedDate.getMonth() + 1, day);
		calendarCookieProvider.set("selectedDate", selectedDate);
				
		renderNavCalendar();
		renderMonthlyCalendar();
	}
	
	
	function renderMonthlyCalendar () {
	
		$('monthlyView').hide();
		showLoadingCalendar();

		var days = daysInMonth(selectedDate.getFullYear(), selectedDate.getMonth());
		fromDate = new Date(selectedDate.getFullYear(), selectedDate.getMonth(), 1);
		toDate = new Date(selectedDate.getFullYear(), selectedDate.getMonth(), days,23,59,59);

		$('monthMonthlyViewName').update(selectedDate.format('F Y'));

		var hostId = '<%= session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID) %>';

		CalendarAjax.findEventsByHostFolder(hostId, fromDate, toDate, tags, keywords, categories, live, archived, 0, -1, findEventsMonthlyCallback);

		renderNavCalendar();

	}	
	
	function findEventsMonthlyCallback(data) {
		
		var events = data.reverse();
		for(var i = 0; i < events.length; i++) 
		{
				var event = events[i];
				event.startDate = transformTimeZone(event.startDate,event.offSet);
				event.endDate = transformTimeZone(event.endDate,event.offSet);
		}
		
		var body = $('calendarMonthlyViewBody');
		body.update('');
		
		
		var htmlBuffer = '<tr>';
				
		var rangeStarts = fromDate;
		if(rangeStarts.format('w') != 0) {
			rangeStarts = new Date(fromDate.getTime() - (1000*60*60*24*fromDate.format('w'))); 
		}
		var rangeEnds = fromDate;
		if(rangeEnds.format('w') != 7) {
			rangeEnds = new Date(toDate.getTime() + (1000*60*60*24*(6 - toDate.format('w')))); 
		}
		var nowDate = rangeStarts;
		while(isDateInRange(nowDate, rangeStarts, rangeEnds)) {

			if(!isDateInRange(nowDate, fromDate, toDate)) {
				htmlBuffer += '<td class="emptyDay">&nbsp;</td>';
    			if(nowDate.format('w') == 6) {
    				htmlBuffer += '</tr>';
    				if(compareDates(nowDate, rangeEnds) < 0) {
    					htmlBuffer += '<tr>';
    				}
    			}
				nowDate = nowDate.add(Date.DAY, 1);
				continue;
			}
			
			var day = nowDate.format('j');
			var dayOfTheWeek = nowDate.format('w');
			
			var alldayEvents = new Array();
			var dayEvents = new Array();

			//Updating global events list with the gathered data
			eventsList = new Array ();
			
			for(var i = 0; i < events.length; i++) {
				
				var event = events[i];
				eventsList[event.identifier] = event;				
	 			
			    if(isDateInRange(nowDate, event.startDate, event.endDate)) {
					if(isAllDayEvent(event)) {
						alldayEvents.push(event);
					} else {
						dayEvents.push(event);
					}
				}
			}		
			
			alldayEvents.sort(function(event1, event2) { return compareDateTime(event1.startDate, event2.startDate); });
			dayEvents.sort(function(event1, event2) { return compareDateTime(event1.startDate, event2.startDate); });
			
			htmlBuffer += 
				'<td class="' + (selectedDate.getDate() == day?'selectedDay ':'') + 
				(selectedDate.getMonth() != nowDate.getMonth()?'notInMonth ':'') + 
				(dayOfTheWeek == 0 || dayOfTheWeek == 6?'weekendDay ':'') + 
				(dayOfTheWeek == 6?'lastWeekDay':'') + '">' +
				'<div id="myid" class="dayNumberSection ' + (alldayEvents.length > 0?"":"") + '">' +
				'	<div class="dayNumber">' +
				'		<a href="javascript: setDayView('+nowDate.getFullYear()+','+nowDate.getMonth()+','+nowDate.getDate()+');">' + 
				day + '</a></div>';
				
			htmlBuffer += '	<div class="allDayEvents"> ';
			for(var i = 0; i < alldayEvents.length; i++) {
				var event = alldayEvents[i];
				var img = getStatusImage(event);
				var eventTitle = event.title.replace(/([^\s]{15})/g,"$1-<br/> ");
				if(i > 0)
					htmlBuffer += '';
				htmlBuffer += 
					'<div><span class="' + img + '"></span><a href="javascript: ;" id="eventRef' + event.identifier + '-' + day + '">' + eventTitle + '</a><div>';
				htmlBuffer += 
					'\<script\>' +
					'	eventRef = Ext.get("eventRef' + event.identifier + '-' + day + '");' +
					'	eventRef.on("click", function(jsevent) { showEventDetail(jsevent, eventsList[\'' + event.identifier + '\']); }, this, { stopPropagation: true });' +
					'\</script\>';
			}
			
			htmlBuffer += '	</div>';
			 
			if(dayEvents.length > 0)

				
			for(var i = 0; i < dayEvents.length; i++) {
				var event = dayEvents[i];
				var img = getStatusImage(event);
				var eventTitle = event.title.replace(/([^\s]{15})/g,"$1-<br/> ");
				var eventTime = event.startDate.format('g') + event.startDate.format('a').substring(0,1);
				htmlBuffer += '<div class="dayEventsSection"><span class="'+img+'"></span><span>' + eventTime + '</span> ' + 
					'<a href="javascript: ;" id="eventRef' + event.identifier + '-' + day + '">' + eventTitle + '</a>';
				htmlBuffer += 
					'\<script\>' +
					'	eventRef = Ext.get("eventRef' + event.identifier + '-' + day + '");' +
					'	eventRef.on("click", function(jsevent) { showEventDetail(jsevent, eventsList[\'' + event.identifier + '\']); }, this, { stopPropagation: true });' +
					'\</script\>';
			}
				
			if(dayEvents.length > 0)
				htmlBuffer += '</td>';
			
			if(nowDate.format('w') == 6) {
				htmlBuffer += '</tr>';
				if(compareDates(nowDate, rangeEnds) < 0) {
					htmlBuffer += '<tr>';
				}
			}
			nowDate = nowDate.add(Date.DAY, 1);
		}
		
		body.update(htmlBuffer);
		
		hideLoadingCalendar();
		$('monthlyView').show({duration: .4});
		 
	}
	
</script>
			
