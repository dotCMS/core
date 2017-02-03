<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<div class=" navCalendar">
	<table class="listingTable" id="navigationCalendar">
		<tr>
			<th id="navMonthYearName" class="last" colspan="7" onclick="changeCalendarView('monthly')"></th>
		</tr>
		<tbody id="navBodyHead">
			<tr>
				<td class="navDay"><%= LanguageUtil.get(pageContext, "sunday.first.letter") %></td>
				<td class="navDay"><%= LanguageUtil.get(pageContext, "monday.first.letter") %></td>
				<td class="navDay"><%= LanguageUtil.get(pageContext, "tuesday.first.letter") %></td>
				<td class="navDay"><%= LanguageUtil.get(pageContext, "wednesday.first.letter") %></td>
				<td class="navDay"><%= LanguageUtil.get(pageContext, "thursday.first.letter") %></td>
				<td class="navDay"><%= LanguageUtil.get(pageContext, "friday.first.letter") %></td>
				<td class="navLastDay"><%= LanguageUtil.get(pageContext, "saturday.first.letter") %></td>
			</tr>
		</tbody>
		<tbody id="navBodyCalendar">
	
		</tbody>
	</table>
	
	<div id="calendarFilters" class="calendarFiltersBox">
		<%= LanguageUtil.get(pageContext, "No-filters-set") %>
	</div>
</div>

<script type="text/javascript">

	function initializeNavCalendar() {
	
		renderNavCalendar();
		
		renderFilters();
		
	}
	
	function renderFilters() {
		var strHTML = '<b><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Showing")) %>:</b><br/>';
		strHTML += '<div class="filterBox">';
		for(var i = 0; i < categories.length; i++) {
			strHTML += '<span onclick="removeCategoryFilter(\'' + categories[i] + '\')" class="removeFilter">&nbsp</span> ' + categoriesLabels[i] + '<br/>';
		}
		if(categories.length == 0)
			strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "No-categories")) %>';
		strHTML += '</div><br/>';

		strHTML += '<b><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Filtered-by")) %>:</b><br/>';
		strHTML += '<div class="filterBox">';
		for(var i = 0; i < tags.length; i++) {
			var tag = javascriptfyVar(tags[i]);
			strHTML += '<span onclick="removeTagFilter(\'' + tag + '\')" class="removeFilter">&nbsp;</span> tag: ' + tags[i] + '<br/>';
		}
		for(var i = 0; i < keywords.length; i++) {
			var keyword = javascriptfyVar(keywords[i]);
			strHTML += '<span onclick="removeKeywordFilter(\'' + keyword + '\')" class="removeFilter">&nbsp</span> ' + keywords[i] + '<br/>';
		}
		if(keywords.length == 0 && tags.length == 0)
			strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "No-filters")) %>';
		strHTML += '</div><br/>';
		$('calendarFilters').update(strHTML);
	}
	
	function changeNavCalendarDate (newDate) {
		selectedDate = newDate;
		calendarCookieProvider.set("selectedDate", selectedDate);
		
		if(fromDate != null && toDate != null && isDateInRange(newDate, fromDate, toDate)) {
			setDayView(newDate.getFullYear(), newDate.getMonth(), newDate.getDate());	
		} else {
			refreshCalendarView();
			renderNavCalendar();
		}
	}
	
	function renderNavCalendar () {
	
		$('navMonthYearName').update(dojo.date.locale.format(selectedDate, {selector: "date", datePattern: "MMMM yyyy", locale: dojo.locale}));
		
		var body = $('navBodyCalendar');
		body.update('');
		
		var firstDayOfWeek = selectedDate.getFirstDayOfMonth();
		var daysInMonth = selectedDate.getDaysInMonth();
		
		//calculating the range to highlight
		var fromDate = new Date();
		var toDate = new Date();
		if(selectedView == 'list') {
			var range =  currentRange;
			if(range == 'day') {
				fromDate = selectedDate;
				toDate = new Date(selectedDate);
				toDate.setHours(23);
				toDate.setMinutes(59);
				toDate.setSeconds(59);
			} else if(range == 'week') {
				var dayOfWeek = selectedDate.format('w');
				fromDate = new Date(selectedDate.getTime()-(1000*60*60*24*dayOfWeek));
				toDate = new Date(selectedDate.getTime()+(1000*60*60*24*(7 - dayOfWeek)));
			} else if(range == 'month') {
				var days = selectedDate.getDaysInMonth();
				fromDate = new Date(selectedDate.getFullYear(), selectedDate.getMonth(), 1);
				toDate = new Date(selectedDate.getFullYear(), selectedDate.getMonth(), days);
			} else if(range == 'year') {
				fromDate = new Date(selectedDate.getFullYear(), 0, 1);
				toDate = new Date(selectedDate.getFullYear(), 11, 31);
			} else if(range = '7days') {
				fromDate = new Date(selectedDate.getTime());
				toDate = new Date(selectedDate.getTime()+(1000*60*60*24*7));
			}
		} else if(selectedView == 'weekly') {
			var dayOfWeek = selectedDate.format('w');
			fromDate = new Date(selectedDate.getTime()-(1000*60*60*24*dayOfWeek));
			toDate = new Date(selectedDate.getTime()+(1000*60*60*24*(7 - dayOfWeek)));
		} else if(selectedView == 'monthly') {
			var days = selectedDate.getDaysInMonth();
			fromDate = new Date(selectedDate.getFullYear(), selectedDate.getMonth(), 1);
			toDate = new Date(selectedDate.getFullYear(), selectedDate.getMonth(), days);
		}
		
		var htmlBuffer = '<tr>';
		for (var i = 0; i < firstDayOfWeek; i++) {
			htmlBuffer += '<td class="navDay">&nbsp;</td>';
		}
		
		var dayOfTheWeek = firstDayOfWeek;
		for (var day = 1; day <= daysInMonth; day++) {
			var cDate = new Date(selectedDate.getTime());
			cDate.setDate(day);
			var tdClass = (isDateInRange(cDate, fromDate, toDate)?'navSelectedDay ':'');
			
			//http://jira.dotmarketing.net/browse/DOTCMS-5862
			if(selectedView == 'weekly' && ((cDate.getWeek()!=fromDate.getWeek()) 
					|| (daysBetweenDates(cDate,toDate) <= 0 || daysBetweenDates(cDate,toDate) > 7))){
				tdClass = '';
			}else if(selectedView == 'weekly'){
				tdClass = 'navSelectedDay ';
			}
			
			htmlBuffer += '<td class="' + tdClass + 
				(dayOfTheWeek == 6?'navLastDay':'navDay') + '">' +
				'<div class="">' + 
				'	<a href="javascript: changeNavCalendarDate(Date.parseDate(\'' + selectedDate.getFullYear() + '-' + (selectedDate.getMonth() + 1) + '-' + day + '\', \'Y-n-j\'));">' + 
				day + 
				'	</a>' +
				'</div></td>';
			dayOfTheWeek = (dayOfTheWeek + 1) % 7;				
			if(dayOfTheWeek == 0) {
				htmlBuffer += '</tr>';
				if(day < daysInMonth) {
					htmlBuffer += '<tr>';
				}
			}
			
		}
		
		for (var i = dayOfTheWeek; i < 7; i++) {
			htmlBuffer += '<td class="'+(i == 6?'navLastDay':'navDay')+'">&nbsp;</td>';
			if (i == 6) {
				htmlBuffer += '</tr>';
			}

		}
		
		body.update(htmlBuffer);
	
	}
	
</script>

