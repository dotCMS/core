<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<div id="weeklyView" style="display: none;">

	<div class="yui-gb" id="monthControls" style="padding: 8px 5px;">
		<div class="yui-u first">
			<button dojoType="dijit.form.Button" onClick="previousWeek()" >
				<span class="arrowLeftIcon"></span>
			</button>
			<button dojoType="dijit.form.Button" onClick="nextWeek()" >
				<span class="arrowRightIcon"></span>
			</button>
			<button dojoType="dijit.form.Button" onClick="setTodayView()" >
	    		<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Today")) %>
			</button>
		</div>
		<div class="yui-u"style=" text-align:center;padding-top:5px;">
			<span id="monthWeeklyViewName" class="monthName"></span>
		</div>
	    <div class="yui-u">
	    	&nbsp;
		</div> 
	</div>

	<div id="weekWrapper" style="overflow-y:auto;overflow-x:hidden;">
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
			<tbody id="calendarWeeklyBody" class="weeklyCalendarBody"></tbody>
		</table>
	</div>
</div>
			
<script type="text/javascript">

	function initializeWeeklyCalendar() {
	
		renderWeeklyCalendar();
	}
	
	function previousWeek() {

		selectedDate = new Date(selectedDate.getTime()-(1000*60*60*24*7));
		calendarCookieProvider.set("selectedDate", selectedDate);
		
		renderNavCalendar();
		renderWeeklyCalendar();
	}
	
	function nextWeek() {

		selectedDate = new Date(selectedDate.getTime()+(1000*60*60*24*7));
		calendarCookieProvider.set("selectedDate", selectedDate);
				
		renderNavCalendar();
		renderWeeklyCalendar();
	}
	
	function renderWeeklyCalendar () {
	
		$('weeklyView').hide();

		showLoadingCalendar();

		$('monthWeeklyViewName').update(selectedDate.format('F Y'));
		
		var dayOfWeek = selectedDate.format('w');
		fromDate = new Date(selectedDate.getTime()-(1000*60*60*24*dayOfWeek));
		fromDate.setHours(0);
		fromDate.setMinutes(0);
		fromDate.setSeconds(0);
		toDate = new Date(selectedDate.getTime()+(1000*60*60*24*(7 - dayOfWeek)));
		toDate.setHours(23);
		toDate.setMinutes(59);
		toDate.setSeconds(59);

		while(daysBetweenDates(fromDate,toDate)>8){
			if(fromDate<toDate){
			  fromDate.setDate(fromDate.getDate()+1);
			}
		}

		var hostId = '<%= session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID) %>';

		CalendarAjax.findEventsByHostFolder(hostId, fromDate, toDate, tags, keywords, categories, live, archived, 0, -1, findEventsWeeklyCallback);

		renderNavCalendar();

	}	
	
	function findEventsWeeklyCallback (events) {
	
		for(var i = 0; i < events.length; i++) 
		{
				var event = events[i];
				event.startDate = transformTimeZone(event.startDate,event.offSet);
				event.endDate = transformTimeZone(event.endDate,event.offSet);
		}
	
		var body = $('calendarWeeklyBody');
		body.update('');
		
		var currentDate = fromDate;
		 
		var daysInMonth = selectedDate.getDaysInMonth();
		
		var htmlBuffer = '<tr>';
		
		for (var weekDayCount = 0; weekDayCount < 7; weekDayCount++) {
			
			var currentDay = currentDate.format('j');
			var currentDayOfWeek = currentDate.format('N');

			var alldayEvents = new Array();
			var dayEvents = new Array();

			//Updating global events list with the gathered data
			eventsList = new Array ();

			for(var i = 0; i < events.length; i++) {
				var event = events[i];
				eventsList[event.identifier] = event;
			    if(isDateInRange(currentDate, event.startDate, event.endDate)) {
					if(isAllDayEvent(event)) {
						alldayEvents.push(event);
					} else {
						dayEvents.push(event);
					}
				}
			}		
			
			dayEvents.sort(function(event1, event2) { return compareDateTime(event1.startDate, event2.startDate); });
		
		// START PRINT DATE
			htmlBuffer += 
				'<td class="weekDay ' + (isSameDate(selectedDate, currentDate)?'selectedDay ':'') + 
					(selectedDate.getMonth() != currentDate.getMonth()?'notInMonth ':'') +
					(currentDayOfWeek == 6 || currentDayOfWeek == 7?'weekendDay ':'') + 
					(currentDayOfWeek == 6?'lastWeekDay':'') + '">' +
				'	<div class="dayNumber" style="margin:-26px 3px 15px 0;"><a href="javascript: setDayView('+currentDate.getFullYear()+','+currentDate.getMonth()+','+currentDate.getDate()+');">' + currentDate.format('j') + '</a></div>';

				
		// START ALL DAY LOOP	
			htmlBuffer += '	<div class="allDayEvents"> ';
				for(var i = 0; i < alldayEvents.length; i++) {
					var event = alldayEvents[i];
					var img = getStatusImage(event);
					//Breaking titles with too long words
					var eventTitle = event.title.replace(/([^\s]{15})/g,"$1-<br/> ");
					htmlBuffer += '<span class="' + img + '"></span><a href="javascript: ;" id="eventRef' + event.identifier + '-' + currentDate.format('j') + 'Weekly">' + event.title + '</a>';
					htmlBuffer += 
						'\<script\>' +
						'	eventRef = Ext.get("eventRef' + event.identifier + '-' + currentDate.format('j') + 'Weekly");' +
						'	eventRef.on("click", function(jsevent) { showEventDetail(jsevent, eventsList[\'' + event.identifier + '\']); }, this, { stopPropagation: true });' +
						'\</script\>';
				}
			
			htmlBuffer += '</div>';
			
			
		// START EVENT LOOP
			if(dayEvents.length > 0)
				htmlBuffer += '	<div class="dayEventsSection">';
				
			var lastEventTime = '';
			for(var j = 0; j < dayEvents.length; j++, i++) {
				var event = dayEvents[j];
				var img = getStatusImage(event);
				var eventTime = event.startDate.format('ga');
				//Breaking titles with too long words
				var eventTitle = event.title.replace(/([^\s]{15})/g,"$1-<br/> ");
				htmlBuffer += '	<p><span class="' + img + '"></span>' +
					(eventTime != lastEventTime?'<span style="font-size:77%;"><b>' + eventTime + '</b></span>':'') +
					'	<a href="javascript: ;" id="eventRef' + event.identifier + '-' + currentDate.format('j') + 'Weekly">' + eventTitle + '</a>';
				htmlBuffer += 
					'\<script\>' +
					'	eventRef = Ext.get("eventRef' + event.identifier + '-' + currentDate.format('j') + 'Weekly");' +
					'	eventRef.on("click", function(jsevent) { showEventDetail(jsevent, eventsList[\'' + event.identifier + '\']); }, this, { stopPropagation: true });' +
					'\</script\>';
				lastEventTime = eventTime;
			}
				
			if(dayEvents.length > 0)
				htmlBuffer += '	</div>';
			
			htmlBuffer += '</td>';
				
			//http://jira.dotmarketing.net/browse/DOTCMS-5862
			//currentDate = new Date(currentDate.getTime()+(1000*60*60*24));
			currentDate.setDate(currentDate.getDate()+1);
			
		}
		htmlBuffer += '</tr>';
		
		body.update(htmlBuffer);
		
		hideLoadingCalendar();
		$('weeklyView').show({duration: .4});
	
		
	}

	function daysBetweenDates(date1, date2){
		// The number of milliseconds in one day
        var ONE_DAY = 1000 * 60 * 60 * 24;

	    // Convert both dates to milliseconds
	    var date1_ms = new Date(date1.getTime());
	    var date2_ms = new Date(date2.getTime());

	    // Calculate the difference in milliseconds
	    var difference_ms = Math.abs(date1_ms - date2_ms);
	    
	    // Convert back to days and return
	    return Math.round(difference_ms/ONE_DAY);
	}

	Date.prototype.getWeek = function() {
		var onejan = new Date(this.getFullYear(),0,1);
		return Math.ceil((((this - onejan) / 86400000) + onejan.getDay()+1)/7);
	}
	
</script>
			
