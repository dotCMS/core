<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<div id="listView" style="display: none;">

	<div class="yui-gb" id="monthControls" style="padding: 8px 5px;">
		<div class="yui-u first">
			<button dojoType="dijit.form.Button" onClick="previousRangeListView()" >
				<span class="arrowLeftIcon"></span>
			</button>
			<button dojoType="dijit.form.Button" onClick="nextRangeListView()" >
				<span class="arrowRightIcon"></span>
			</button>
			<button dojoType="dijit.form.Button" onClick="setTodayView()" >
	    		<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Today")) %>
			</button>
	    	<!-- <select name="rangeSelector" onchange="changeDatesRange()" id="rangeSelector"">
				<option value="day"><%= LanguageUtil.get(pageContext, "Day") %></option>
				<option value="7days"><%= LanguageUtil.get(pageContext, "Next-7-Days") %></option>
				<option value="week"><%= LanguageUtil.get(pageContext, "Week") %></option>
				<option value="month"><%= LanguageUtil.get(pageContext, "Month") %></option>
				<option value="year"><%= LanguageUtil.get(pageContext, "Year") %></option>
			</select>
			-->
		</div>
		<div class="yui-u"style=" text-align:center;padding-top:5px;">
			<span id="monthListViewName" class="monthName" style="font-size:108%;"></span>
		</div>
	    <div class="yui-u">
	    	&nbsp;
		</div> 
	</div>
	
	<div id="dayWrapper" style="overflow-y:auto;overflow-x:hidden;">
		<div id="calendarListBody" class="yui-ge">
			<div id="dailyEvents" class="yui-u first"></div>
			<div id="ongoingEvents" class="yui-u">
				<h3 style="margin-top:4px;"><%= LanguageUtil.get(pageContext, "Ongoing-Events") %></h3>
				<div id="ongoingEventsList"></div>
			</div>	
		</div>
		
		<div id="eventsNavigation" class=""></div>
	</div>
	
</div>
			
<script type="text/javascript">

	var fromDate;
	var toDate;

	function initializeListCalendar() {
	
		renderListCalendar();
		
		var range = currentRange;
		
		dwr.util.setValue('rangeSelector', range);
		
	}
	
	function changeDatesRange () {
		newRange= $F('rangeSelector');
		if(newRange == '7days') {
			selectedDate = new Date();
			calendarCookieProvider.set('selectedDate', selectedDate);
		}
		currentRange = newRange;
		calendarCookieProvider.set('currentRange', newRange);
		renderListCalendar();
	}
	
	function previousRangeListView() {
	
		var range = currentRange;
		
		if(range == 'day') {
			selectedDate = new Date(selectedDate.getTime()-(1000*60*60*24*1));
		} else if(range == 'week') {
			selectedDate = new Date(selectedDate.getTime()-(1000*60*60*24*7));
		} else if(range == 'month') {
			var days = daysInMonth(selectedDate.getFullYear(), selectedDate.getMonth() - 1);
			var day = selectedDate.getDate() > days?days:selectedDate.getDate();
			selectedDate = new Date(selectedDate.getFullYear(), selectedDate.getMonth() - 1, day);
		} else if(range == 'year') {
			var days = daysInMonth(selectedDate.getFullYear() - 1, selectedDate.getMonth());
			var day = selectedDate.getDate() > days?days:selectedDate.getDate();
			selectedDate = new Date(selectedDate.getFullYear() - 1, selectedDate.getMonth(), day);
		} else if(range = '7days') {
			selectedDate = new Date(selectedDate.getTime()-(1000*60*60*24*7));
		}
		
		calendarCookieProvider.set("selectedDate", selectedDate);
		
		renderNavCalendar();
		renderListCalendar();
	}
	
	function nextRangeListView() {
		var range = currentRange;
		
		if(range == 'day') {
			selectedDate = new Date(selectedDate.getTime()+(1000*60*60*24*1));
		} else if(range == 'week') {
			selectedDate = new Date(selectedDate.getTime()+(1000*60*60*24*7));
		} else if(range == 'month') {
			var days = daysInMonth(selectedDate.getFullYear(), selectedDate.getMonth() + 1);
			var day = selectedDate.getDate() > days?days:selectedDate.getDate();
			selectedDate = new Date(selectedDate.getFullYear(), selectedDate.getMonth() + 1, day);
		} else if(range == 'year') {
			var days = daysInMonth(selectedDate.getFullYear() + 1, selectedDate.getMonth());
			var day = selectedDate.getDate() > days?days:selectedDate.getDate();
			selectedDate = new Date(selectedDate.getFullYear() + 1, selectedDate.getMonth(), day);
		} else if(range = '7days') {
			selectedDate = new Date(selectedDate.getTime()+(1000*60*60*24*7));
		}
				
		calendarCookieProvider.set("selectedDate", selectedDate);
				
		renderNavCalendar();
		renderListCalendar();
	}
	
	function renderListCalendar (dontResetPage) {
	
		if(!dontResetPage) {
			offset = 0;
		}
		
		$('listView').hide();
		showLoadingCalendar();
	
		var range =  currentRange;
		
		if(range == 'day') {
			fromDate = selectedDate;
			toDate = new Date(selectedDate);
		} else if(range == 'week') {
			var dayOfWeek = selectedDate.format('w');
			fromDate = new Date(selectedDate.getTime()-(1000*60*60*24*dayOfWeek));
			toDate = new Date(selectedDate.getTime()+(1000*60*60*24*(6 - dayOfWeek)));
		} else if(range == 'month') {
			var days = daysInMonth(selectedDate.getFullYear(), selectedDate.getMonth());
			fromDate = new Date(selectedDate.getFullYear(), selectedDate.getMonth(), 1);
			toDate = new Date(selectedDate.getFullYear(), selectedDate.getMonth(), days);
		} else if(range == 'year') {
			fromDate = new Date(selectedDate.getFullYear(), 0, 1);
			toDate = new Date(selectedDate.getFullYear(), 11, 31);
		} else if(range = '7days') {
			fromDate = new Date(selectedDate.getTime());
			toDate = new Date(selectedDate.getTime()+(1000*60*60*24*7));
		}
		fromDate.setHours(0);
		fromDate.setMinutes(0);
		fromDate.setSeconds(0);
		toDate.setHours(23);
		toDate.setMinutes(59);
		toDate.setSeconds(59);

		$('monthListViewName').update(isSameDate(fromDate, toDate)?fromDate.format('F d, Y'):fromDate.format('F d, Y') + ' - ' + toDate.format('F d, Y'));

		var hostId = '<%= session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID) %>';

		if(range == 'day'){
			CalendarAjax.findEventsForDay(hostId, fromDate.format('m/d/Y'), tags, keywords, categories, live, archived, offset, perPage + 1, findEventsListCallback);
		}else{
	 	    CalendarAjax.findEventsByHostFolder(hostId, fromDate, toDate, tags, keywords, categories, live, archived, offset, perPage + 1, findEventsListCallback);
		}
		renderNavCalendar();

	}
	
	function findEventsListCallback (data) {
	
		for(var i = 0; i < data.length; i++) 
		{
				var event = data[i];
				event.startDate = transformTimeZone(event.startDate,event.offSet);
				event.endDate = transformTimeZone(event.endDate,event.offSet);
		}
	
		//Loading daily events		
		var body = $('dailyEvents');
		body.update('');
		
		var htmlBuffer = "";
		var currentDate = null;
		
		var dailyEventsCount = 0;
		
		//Updating global events list with the gathered data
		eventsList = new Array ();
		
		var lastIndex = perPage < data.length ? perPage : data.length;		
		for(var i = 0; i < lastIndex; i++) {
			var event = data[i];
			eventsList[event.identifier] = event;
			
			if(!isMultiDayEvent(event)) {
				dailyEventsCount++;
				if(!isSameDate(event.startDate, currentDate)) {
					if(currentDate != null)
						htmlBuffer += '</ul>';				
					htmlBuffer += 
						'<h3>' + event.startDate.format('F j, Y') + '</h3>' +
						'<ul>';
					currentDate = event.startDate;
				}		
				var startTime = event.startDate.format('h:i A');
				var endTime = event.endDate.format('h:i A');
				var img = getStatusImage(event);
				htmlBuffer += 
					'	<li><span class="' + img + '"></span><a href="javascript: ;" id="eventRef' + event.identifier + 'List"><b>' + event.title + '</b></a>' +
					'	<div class="whenListView"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "when")) %>: ' + startTime + (startTime != endTime?' - ' + endTime:'') + '</div>';
				if(event.rating > 0) {
					htmlBuffer += 
						'		<div class="ratingListView"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "rating")) %>: ' + event.rating + '</div>';
				}
				if(event.tags && event.tags.length > 0) {
					htmlBuffer += 
						'		<div class="tagsListView"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "tags")) %>: ' + event.tags + '</div>';
				}
				htmlBuffer += '</li>'; 
			}
		}		
		if(dailyEventsCount > 0)
			htmlBuffer += '</ul>';
		else
			htmlBuffer += 
				'<h3>' + (isSameDate(fromDate, toDate)?fromDate.format('F j, Y'):fromDate.format('F j, Y') + " - " + toDate.format('F j, Y')) + '</h3>' +
				'<ul><li><div class="noResultsMessage"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "No-Events")) %></div></li><br/></ul>';		
		body.update(htmlBuffer);

		//Loading ongoing events
		body = $('ongoingEventsList');		
		body.update('');
		
		htmlBuffer = "<ul>";
		
		var ongoingEventsCount = 0;
		
		for(var i = 0; i < lastIndex; i++) {
			var event = data[i];
			if(isMultiDayEvent(event)) {
				var img = getStatusImage(event);
				ongoingEventsCount++;
				htmlBuffer += 
					'	<li><span class="' + img + '"></span><a href="javascript: ;" id="eventRef' + event.identifier + 'List"><b>' + event.title + '</b></a>' +
					'	<div class="whenListView"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Through")) %>: ' + event.endDate.format('F j, Y') + '<br/>';
				if(event.rating > 0) {
					htmlBuffer += 
						' <hr/><span style="font-size:77%;"><b><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "rating")) %>:</b> ' + event.rating + '</span><br/>';
				}
				if(event.tags && event.tags.length > 0) {
					htmlBuffer += 
						' <hr/><span style="font-size:77%;"><b><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "tags")) %>:</b> ' + event.tags + '</span><br/>';
				}
				htmlBuffer += '</li>';		
			}
		}
		
		if(ongoingEventsCount == 0)
			htmlBuffer += '<li><div class="noResultsMessage"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "No-Events")) %></div></li>';
		
		htmlBuffer += "</ul>";
				
		for(var i = 0; i < lastIndex; i++) {			
			var event = data[i];
			htmlBuffer += '\<script\>' +
				'	eventRef = Ext.get("eventRef' + event.identifier + 'List");' +
				'	eventRef.on("click", function (jsevent) { showEventDetail(jsevent, eventsList[\'' + event.identifier + '\']); }, this, { stopPropagation: true });' +
				'\</script\>';			
		}		
				
		body.update(htmlBuffer);
		
		//Loading navigation 
		htmlBuffer = "";
		body = $('eventsNavigation');
		body.update('');
		htmlBuffer += '<div id="pageNavigation" class="pageNavigation">';
		if(data.length > perPage) {
			htmlBuffer += '<a class="nextPageLink" href="javascript: nextListViewPage()"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Next-Page")) %> &gt;</a>';
		}
		if(offset > 0) {
			htmlBuffer += '<a class="previousPageLink" href="javascript: previousListViewPage()">&lt; <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Previous-Page")) %></a>';
		}
		htmlBuffer += '</div>';		
		body.update(htmlBuffer);		
			
		
		
		hideLoadingCalendar();
		$('listView').show({duration: .4});
		
	}
	
	function nextListViewPage() {
		offset += perPage;
		renderListCalendar(true);
	}

	function previousListViewPage() {
		offset -= perPage;
		renderListCalendar(true);
	}
	
</script>
			
