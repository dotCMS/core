<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<%@page import="java.util.TimeZone"%>
	<script src="/html/js/scriptaculous/prototype.js" type="text/javascript"></script>
	<script src="/html/js/scriptaculous/scriptaculous.js" type="text/javascript"></script>     
<script type="text/javascript"
	src="/html/portlet/ext/calendar/calendar-ext.js"></script>

<script type="text/javascript">

    dojo.require("dijit.dijit"); 
    dojo.require("dijit.Dialog");
	
	//Global variables
	var selectedDate = new Date();
	var tags = new Array();
	var keywords = new Array();
	var categories = new Array();
	var categoriesLabels = new Array();
	var live = false;
	var archived = false;
	var offset = 0;
	var perPage = 50;
	var selectedView = 'list';
	var currentRange = 'day';
	
	//Cookie handler
	var calendarCookieProvider = new Ext.state.CookieProvider({
       path: "/",
       expires: new Date(new Date().getTime()+(1000*60*60*24*1000)) 
   	});
   
   //Dates utility functions
	function daysInMonth (year, month) {
	     return 32 - new Date(year, month, 32).getDate();
	}
	
	function dayOfWeek(day,month,year) {
	    var a = Math.floor((14 - month)/12);
	    var y = year - a;
	    var m = month + 12*a - 2;
	    var d = (day + y + Math.floor(y/4) - Math.floor(y/100) +
	             Math.floor(y/400) + Math.floor((31*m)/12)) % 7;
	    return d;
	}	
	
	function isAllDayEvent (event) {
		var startDate = event.startDate;
		var endDate = event.endDate;
		if(startDate.format('Y-m-d') != endDate.format('Y-m-d'))
			return true;
		else if (startDate.getHours() == 0 && startDate.getMinutes() == 0 && 
			endDate.getHours() == 23 && endDate.getMinutes() == 59)
			return true;
		return false;
	}
	
	function isMultiDayEvent (event) {
		var startDate = event.startDate;
		var endDate = event.endDate;
		return startDate.format('Y-m-d') != endDate.format('Y-m-d');
	}
	
	function isDateInRange(nowDate, startDate, endDate) {
		return compareDates(startDate, nowDate) <= 0 && 
			compareDates(endDate, nowDate) >=0; 
	}
	
	/**
	 Compares to dates no taking in count the time
	 returns -1 if date1 < date2, 1 if date1 > date2, 0 if equals
	 */
	function compareDateTime(date1, date2){
	    var date1Str = date1.format('YmdHis');
	    var date2Str = date2.format('YmdHis');
	    return parseInt(date1Str) - parseInt(date2Str);
	}

	/**
		Compares to dates no taking in count the time
	*/
	function isSameDate (date1, date2) {
		if(date1 == null || date2 == null) return false;
		return date1.format('Y-m-d') == date2.format('Y-m-d');
	}
	
	/**
		Compares to dates no taking in count the time
		returns -1 if date1 < date2, 1 if date1 > date2, 0 if equals
	*/
	function compareDates(date1, date2) {
		var date1Str = date1.format('Ymd');
		var date2Str = date2.format('Ymd');
		if(date1Str > date2Str)
			return 1;
		else if(date1Str < date2Str)
			return -1;
		else return 0;
	}	
	
	function arrayToString(array) {
		var arrayStr = '';
		for(var i = 0; i < array.length; i++) {
			arrayStr += array[i];
			if(i < array.length - 1)
				arrayStr += ",";
		}
		return arrayStr;
	}
	
	//Utility Functions
	
    function javascriptfyVar (variable)
    {
    	return variable.replace(/'/g, '\\\'').replace(/"/g, '');
    }
    
    	
	//Calendar Functions
	
	//Calendar Initialization
	function initializeCalendar() {
		selectedDate = calendarCookieProvider.get("selectedDate", new Date());
		var tagsSt = calendarCookieProvider.get("tags", '');
		if(tagsSt){
			tags = tagsSt.split(',');
		}
		else{
			tags = new Array();
		}
		var keywordsSt = calendarCookieProvider.get("keywords", '');
		keywords = keywordsSt != ""?keywordsSt.split(','):new Array();
		var categoriesSt = calendarCookieProvider.get("categories", '');
		categories = categoriesSt != ""?categoriesSt.split(','):new Array();
		var categoriesLabelsSt = calendarCookieProvider.get("categoriesLabels", '');
		categoriesLabels = categoriesLabelsSt != ""?categoriesLabelsSt.split(','):new Array();
		selectedView = calendarCookieProvider.get("selectedView", 'list');
		currentRange = calendarCookieProvider.get('currentRange', '7days');
		
		initializeNavCalendar();

		if(selectedView == 'list')
			initializeListCalendar();
		else if (selectedView == 'weekly')
			initializeWeeklyCalendar();
		else if (selectedView == 'monthly')
			initializeMonthlyCalendar();
		 
		initializeFilters();
		
 		$(document.body).observe("keyup", checkEscapeKey);	
		
	}
	
	//Retrieves the status img path
	function getStatusImage(event) { 
		var img = 'workingIcon';
		if(event.live)
			img = 'liveIcon';
		if(event.archived)
			img = 'archivedIcon';
		return img;
	}	
	
	//Change between weekly/monthly/list view
	function changeCalendarView(newView) {
		
		if(newView == selectedView)
			return;
			
		calendarCookieProvider.set("selectedView", newView);
		selectedView = newView;
		
		refreshCalendarView();			
	}
	
	//Refreshes the calendar in the actual selected view
	function refreshCalendarView() {
		
		hideCalendarViews();
		
		if(selectedView == 'list') {
			initializeListCalendar();
		} else if (selectedView == 'weekly') {
			initializeWeeklyCalendar();
		} else if (selectedView == 'monthly') {
			initializeMonthlyCalendar();
		}
			
	}
	
	function showLoadingCalendar() {
		$('loadingView').show();
	}
	
	function hideLoadingCalendar() {
		$('loadingView').hide();
	}
	
	function hideCalendarViews () {
		$('weeklyView').hide();
		$('monthlyView').hide();
		$('listView').hide();
	    hideCalendarPopups();
	}
	
	function hideCalendarPopups (jsevent) {
		if(jsevent == null || !jsevent.within(Ext.get('eventDetail'))) {
	    	$('eventDetail').hide();
    	}
    	$('filtersBox').hide();
    	closeFiltersBox();	
	}	
	
	//Categories, keywords and tags filters functions
	
	function initializeFilters () {
	
 		var keywordBox = $("keywordBox");
 		keywordBox.observe("keypress", keywordsCheckEnter);		
 		keywordBox.observe("keyup", keywordsCheckKeys);		
 		
 		var moreOptionsButton = Ext.get("moreOptionsButton");
 		moreOptionsButton.on("click", showFiltersBox, moreOptionsButton, { stopPropagation: true });		
			
	}
	
	//Loads the filter box using the keyword filled and showing a maximum of N categories 
	filterBoxPositioned = false;
	
	function showFiltersBox () {
	
		var keyword = $('keywordBox').value;

		//Cleaning all the old categories before repulling them
		$('categoriesFilterBox').update('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Loading")) %>...');
		showAllCategories = false;
		StructureAjax.getCategoriesTree('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Event")) %>', "(?i).*" + keyword + ".*", loadCategoriesFilterCallback);
		
		
		$('tagsFilterBox').update('<b><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Filter-by-tag")) %>:</b><br/><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Loading")) %>...');
		if(keyword == '')
			TagAjax.getAllTags(loadTagsFilterCallback);
		else
			TagAjax.getSuggestedTag(keyword, "", loadTagsFilterCallback);
			
		var filtersBox = Ext.get('filtersBox');
		if(!filterBoxPositioned) {
			var posX = 275;
			var posY = 165;
			filtersBox.moveTo(posX, posY);
			filterBoxPositioned = true;
		}
		$('filtersBox').show({ duration: .4 });
	}
	
	function hideFiltersBox () {
		closeFiltersBox();
	}
	
	function closeFiltersBox () {
		var filtersBox = $('filtersBox');
		filtersBox.hide({ duration: .4 });
	}
	
	//Keyword filtering functions
	
	function addKeyword() {
		if($('keywordBox').value != '') {
			addKeywordFilter($('keywordBox').value);
			$('keywordBox').value = '';
		}
	}
	
	function keywordsCheckEnter(event) {
		if(Event.keyCode == 13) {
			addKeyword();
		}
	}
	
	function keywordsCheckKeys(event) {
		if($('keywordBox').value.length == 0) {
			hideFiltersBox();
		} else if($('keywordBox').value.length >= 3 || event.keyCode == 8) {
			showFiltersBox();
		}
	}
	
	function addKeywordFilter(keyword) {
		if(keywords.indexOf(keyword) < 0) {
			keyword = keyword.replace(/"/g, "");
			keywords.push(keyword);
			calendarCookieProvider.set("keywords", arrayToString(keywords));
			renderFilters();
			refreshCalendarView();
		}
	}
	
	function removeKeywordFilter(keyword) {
		if(keywords.indexOf(keyword) >= 0) {
			keywords.remove(keyword);
			calendarCookieProvider.set("keywords", arrayToString(keywords));
			renderFilters();
			refreshCalendarView();
		}
	}
	
	//Category filter functions
	
	//Same as the last function but loads all the categories 
	function loadAllCategoriesFiltersBox () {
		var keyword = $('keywordBox').value;

		//Cleaning all the old categories before repulling them
		$('categoriesFilterBox').update('Loading...');
		showAllCategories = true;
		StructureAjax.getCategoriesTree('Event', "(?i).*" + keyword + ".*", loadCategoriesFilterCallback);
		
	}
	 
	var showAllCategories = false;
	function loadCategoriesFilterCallback(data) {
	
		var keyword = $('keywordBox').value;
		
		var currentLevel = 0;
		var secondLevelCount = 0;
		var showAllShowed = false;
		var strHTML = '';
		for(var i = 0; i < data.length; i++) {
			var cat = data[i];
			currentLevel = cat.categoryLevel;
			if(currentLevel == 0) {
				if(i > 0 && secondLevelCount == 0)
					strHTML += '<div class="categoryFilterLink" style="margin-left: ' + marginLeft + 'px" ><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "No-categories-match")) %></div>';
					
				secondLevelCount = 0;	
				showAllShowed = false;
				if(i > 0)
					strHTML += '<br/>';
				strHTML += '<b>' + cat.categoryOrigName + ':</b>'
				if(countLevelCategories(data, i + 1, 1) > 10 && !showAllCategories)
					strHTML += 
						' (<a href="javascript: loadAllCategoriesFiltersBox(\'' + cat.inode + '\')"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "show-all")) %></a>)' +
						'<br/>';
				else
					strHTML += '<br/>';
			} else {
				secondLevelCount++;
				if(showAllCategories || secondLevelCount <= 10) {
				
					var catClass = "";
					if(keyword != "" && cat.categoryOrigName.toLowerCase().indexOf(keyword.toLowerCase()) > -1) catClass = "category_higlighted";
						
					var marginLeft = currentLevel * 10;
					strHTML += '<div class="categoryFilterLink" style="margin-left: ' + marginLeft + 'px" >' +
						'- <a class="' + catClass + '" href="javascript: addCategoryFilter(\'' + cat.inode + '\', \'' + javascriptfyVar(cat.categoryOrigName) + '\')">' + cat.categoryOrigName + '</a>' +
						'</div>';
				} else if(!showAllCategories && secondLevelCount > 10 && !showAllShowed) {
					strHTML += '<div class="categoryFilterLink" style="margin-left: ' + marginLeft + 'px" >...</div>';
					showAllShowed = true;
				}
			}
		}
		if(secondLevelCount == 0)
			strHTML += '<div class="categoryFilterLink" style="margin-left: ' + marginLeft + 'px" ><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "No-categories-match")) %></div>';
		$('categoriesFilterBox').update(strHTML);
	}
	
	function countLevelCategories(data, startIndex, level) {
		var count = 0;
		for(var i = startIndex; i < data.length; i++) {
			var cat = data[i];
			if(cat.categoryLevel != level)
				break;
			count++;
		}
		return count;
	}
	
	function addCategoryFilter(catInode, catLabel) {
		$('keywordBox').value= '';
		if(categories.indexOf(catInode) < 0) {
			categories.push(catInode);
			categoriesLabels.push(catLabel);
			calendarCookieProvider.set("categories", arrayToString(categories));
			calendarCookieProvider.set("categoriesLabels", arrayToString(categoriesLabels));
			renderFilters();
			refreshCalendarView();
		}
		closeFiltersBox ();
	}
	
	function removeCategoryFilter(catInode) {
		if(categories.indexOf(catInode) >= 0) {
			categoriesLabels.remove(categoriesLabels[categories.indexOf(catInode)]);
			categories.remove(catInode);
			calendarCookieProvider.set("categories", arrayToString(categories));
			calendarCookieProvider.set("categoriesLabels", arrayToString(categoriesLabels));
			renderFilters();
			refreshCalendarView();
		}
	}
	
	//Tags filter functions
	function loadTagsFilterCallback(data) {
		var keyword = $('keywordBox').value;
		var strHTML = '<b><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Filter-by-tag")) %>:</b><br/>';
		for(var i = 0; i < data.length; i++) {
			var tag = data[i];
			if(tag.tagName == '') continue;
			
			var tagClass = "";
			if(keyword != "" && tag.tagName.toLowerCase().indexOf(keyword.toLowerCase()) > -1) tagClass = "tag_higlighted";
			
			if(i < 50) {
				var tagEscaped = javascriptfyVar(tag.tagName);
				strHTML += '<div class="tagFilterLink">' +
					'	- <a class="' + tagClass + '" href="javascript: addTagFilter(\'' + tagEscaped + '\')">' + tag.tagName + '</a>' +
					'</div>';
			} else {
				strHTML += '<div class="tagFilterLink">...</div>';
				break;
			}
		}
		if(data.length == 0)
			strHTML += '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "No-tags-found")) %>';
		$('tagsFilterBox').update(strHTML);
	}
	
	
	function addTagFilter(tag) {
		$('keywordBox').value= '';
		if(tags.indexOf(tag) < 0) {
			tags.push(tag);
			calendarCookieProvider.set("tags", arrayToString(tags));
			renderFilters();
			refreshCalendarView();
		}
		closeFiltersBox ();
	}
	
	function removeTagFilter(tag) {
		if(tags.indexOf(tag) >= 0) {
			tags.remove(tag);
			calendarCookieProvider.set("tags", arrayToString(tags));
			renderFilters();
			refreshCalendarView();
		}
	}
	
	//Calendar Navigation buttons
	 function setTodayView() {
	 	var refresh = false;
		if(!isSameDate(selectedDate, new Date())) {
			selectedDate = new Date();
			calendarCookieProvider.set("selectedDate", selectedDate);
			refresh = true;
		}
		
		refreshCalendarView();
			
	}
	
	 function setDayView(year, month, day) {
	 	var newDate = new Date();
	 	newDate.setYear(year);
	 	newDate.setMonth(month);
	 	newDate.setDate(day);
	 	var refresh = false;
		if(!isSameDate(selectedDate, newDate)) {
			selectedDate = newDate;
			calendarCookieProvider.set("selectedDate", selectedDate);
			refresh = true;
		}
		
		if(currentRange != 'day') {
			currentRange = 'day';
			calendarCookieProvider.set('currentRange', 'day');
			refresh = true;
		}
		
		if(selectedView != 'list')
			changeCalendarView('list');
		else if(refresh)
			refreshCalendarView();
			
	}
	 
	
	//Event detail popup functions
	var eventsList = new Array();
	
	function checkEscapeKey(jsevent) {
		if(jsevent.keyCode == 27) {
			hideEventDetail();
			hideFiltersBox();
		}
	}
	
	var glEvent = null;
	function showEventDetail(jsevent, event) {

		glEvent = event;
		var doc = Ext.get(document.body);
		var detailDiv = Ext.get("eventDetail");
		
		Element.update($("eventDetailTitle"), event.title);
		
		var eventDates = '';
		if(isSameDate(event.startDate, event.endDate)) {
			var startTime = event.startDate.format('h:i A');
			var endTime = event.endDate.format('h:i A');
			if(startTime != endTime) {
				eventDates = event.startDate.format('m/d/Y h:i A') + ' - ' + event.endDate.format('h:i A');
			} else {
				eventDates = event.startDate.format('m/d/Y h:i A');
			}
		} else {
			eventDates = event.startDate.format('m/d/Y h:i A') + ' - ' + event.endDate.format('m/d/Y h:i A');
		}
		Element.update($("eventDetailDate"), "<span class='calLabel'><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "When")) %>:</span>" + eventDates);

		if (event.description != null) {
			Element.update($("eventDetailDescription"), event.description);
		} else {
			Element.update($("eventDetailDescription"), "");
		}

		if(event.location != undefined){
			document.getElementById("showLocation").style.display="block";
			Element.update($("eventDetailLocation"), (event.location != ""?"<span class='calLabel'>Location:</span> " + event.location + "<br/><hr class=\"blue\"/>":""));
	    }else{
			document.getElementById("showLocation").style.display="none";
		}
		
		if(event.allowRating == "yes") {
			Element.update($("eventDetailRating"), "<span class='calLabel'><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Average-Rating")) %>:</span> " + event.rating + " (" + event.votes + " <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "votes")) %>)<br/><br/>");
		}
		if(event.allowComments == "yes") {
			Element.update($("eventDetailComments"), event.commentsCount + " <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Comments")) %><br/><br/>");
		}
		if(event.tags != '' || event.tags != null) {
			Element.update($("eventDetailTags"), "<span class='calLabel'><%= UtilMethods.escapeSingleQuotes( LanguageUtil.get(pageContext, "Tags") )%>:</span> " + ((event.tags == '' || event.tags == null) ?'<%= UtilMethods.escapeSingleQuotes( LanguageUtil.get(pageContext, "No-tags") )%>':event.tags));
		}
		if(event.categories != undefined && event.categories.length > 0){
			var cats = "<span class='calLabel'><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Categories")) %>:</span> ";
			for(i=0;i<event.categories.length;i++){
				cats+=event.categories[i].categoryName;
				if(i+1< event.categories.length)
					cats+=", "
			}
			Element.update($("eventDetailCategories"), cats);

		}
		
		var eventDetailActions = '';
		var startDate = event.startDate.format('m/d/Y');
		var endDate = event.endDate.format('m/d/Y');

		if(!event.archived && event.writePermission){
			if(event.recurs){
		      eventDetailActions += '<a class="fakeDojoButton" href="javascript: recurrentEventDetail(\'' + event.inode + '\',\'' + event.identifier + '\',\'' + startDate + '\',\'' + endDate + '\',\'copy\');">' +
			  '<span class="copyIcon"></span> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "copy")) %></a>'; 
			}else{
				eventDetailActions += '<a class="fakeDojoButton" href="javascript: copyEvent(\'' + event.inode + '\',\'<%= referer %>\');">' +
				  '<span class="copyIcon"></span> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "copy")) %></a>'; 
			}	
		}
		if(event.writePermission){
			if(event.recurs){
			  eventDetailActions += '<a class="fakeDojoButton" href="javascript: recurrentEventDetail(\'' + event.inode + '\',\'' + event.identifier + '\',\'' + startDate + '\',\'' + endDate+ '\',\'edit\');">' + 
			  '<span class="editIcon"></span> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "edit")) %></a>';
			}else{
				eventDetailActions += '<a class="fakeDojoButton" href="javascript: editEvent(\'' + event.inode + '\',\'<%= referer %>\');">' + 
				  '<span class="editIcon"></span> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "edit")) %></a>';
			}
		}
		if(event.live && event.publishPermission) {		
		    eventDetailActions += '<a class="fakeDojoButton" href="javascript: publishEvent(\'' + event.identifier + '\');">' +
					'<span class="republishIcon"></span> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "republish")) %></a>'; 	
		  if(event.recurs){
			eventDetailActions += '<a class="fakeDojoButton" href="javascript: recurrentEventDetail(\'' + event.inode + '\',\'' + event.identifier + '\',\'' + startDate + '\',\'' + endDate + '\',\'unpublish\');">' + 
			       '<span class="unpublishIcon"></span> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "unpublish")) %></a>';
		  }else{
			  eventDetailActions += '<a class="fakeDojoButton" href="javascript: unpublishEvent(\'' + event.identifier + '\',\'false\');">' + 
				'<span class="unpublishIcon"></span> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "unpublish")) %></a>';    	
		  }
		} 
		if(!event.live && !event.archived && event.publishPermission){
		     eventDetailActions += '<a class="fakeDojoButton" href="javascript: publishEvent(\'' + event.identifier + '\');">' +
				'<span class="publishIcon"></span> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publish")) %></a>'; 
		}
		if(!event.archived && !event.live && event.publishPermission){
		    	eventDetailActions += '<a class="fakeDojoButton" href="javascript: archiveEvent(\'' + event.identifier + '\');">' +
				'<span class="archiveIcon"></span> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "archive")) %></a>'; 		
		}
		if(event.archived && event.publishPermission) {
			eventDetailActions += '<a class="fakeDojoButton" href="javascript: unarchiveEvent(\'' + event.identifier + '\');">' +
				'<span class="unarchiveIcon"></span> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "unarchive")) %></a>'; 
		}

		if(!event.archived  && event.publishPermission){
			if(event.recurs){
				eventDetailActions += '<a class="fakeDojoButton" href="javascript: recurrentEventDetail(\'' + event.inode + '\',\'' + event.identifier + '\',\'' + startDate + '\',\'' + endDate + '\',\'delete\');">' +
				'<span class="deleteIcon"></span> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "delete")) %></a>'; 
			}else{
			  eventDetailActions += '<a class="fakeDojoButton" href="javascript: deleteEvent(\'' + event.identifier + '\',\'false\');">' +
				'<span class="deleteIcon"></span> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "delete")) %></a>'; 
			}
		}
		
		Element.update($("eventDetailActions"), eventDetailActions);
		
		dijit.byId('eventDetail').show();
	}
	
	function hideEventDetail () {
		closeEventDetail();
	}
	
	function closeEventDetail() {
		dijit.byId('eventDetail').hide({ duration: .7 });
	}

	function recurrentEventDetail(inode, identifier, startDate, endDate, action){
		var actions = '';
		if(action=="edit"){
			    actions += '<a class="fakeDojoButton" href="javascript: editRecurrentEvent(\'' + inode + '\',\'' + startDate + '\',\'' + endDate + '\',\'<%= referer %>\');">' +
				'<span></span> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Only-this-instance")) %></a>'; 
				actions += '<a class="fakeDojoButton" href="javascript: editEvent(\'' + inode + '\',\'<%= referer %>\');">' +
					'<span></span> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "All-events-in-the-series")) %></a>'; 
		}else if(action=="unpublish"){
			 actions += '<a class="fakeDojoButton" href="javascript: unpublishRecurrentEvent(\'' + inode + '\',\'' + identifier + '\',\'' + startDate + '\',\'' + endDate + '\',\'<%= referer %>\');">' +
				'<span></span> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Only-this-instance")) %></a>'; 
			  actions += '<a class="fakeDojoButton" href="javascript: unpublishEvent(\'' + identifier + '\',\'true\');">' +
					'<span></span> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "All-events-in-the-series")) %></a>'; 
		}else if(action=="delete"){
			 actions += '<a class="fakeDojoButton" href="javascript: deleteRecurrentEvent(\'' + inode + '\',\'' + identifier + '\',\'' + startDate + '\',\'' + endDate + '\',\'<%= referer %>\');">' +
				'<span></span> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Only-this-instance")) %></a>'; 
			  actions += '<a class="fakeDojoButton" href="javascript: deleteEvent(\'' + identifier + '\',\'true\');">' +
					'<span></span> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "All-events-in-the-series")) %></a>'; 
		}else if(action=="copy"){
			  actions += '<a class="fakeDojoButton" href="javascript: copyRecurrentEvent(\'' + inode + '\',\'' + startDate + '\',\'' + endDate + '\',\'<%= referer %>\');">' +
				'<span></span> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Only-this-instance")) %></a>'; 
			  actions += '<a class="fakeDojoButton" href="javascript: copyEvent(\'' + inode + '\',\'<%= referer %>\');">' +
					'<span></span> <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "All-events-in-the-series")) %></a>'; 
		}

		closeEventDetail(); 
		Element.update($("recEventDetailActions"), actions);
		dijit.byId('recEventDetail').show();
		var dialog = dijit.byId('recEventDetail');
		dialog.connect(dialog,"hide",function(e){
			dojo.hitch(this, setTimeout(function(){
				showEventDetail(e,glEvent);
            }, 500))
		});
				
	}


	
	function hideRecEventDetail() {
		closeRecEventDetail();
	}
	
	function closeRecEventDetail() {
		dijit.byId('recEventDetail').hide({ duration: .7 });
	}
	
	//Event actions functions
	function editEvent(inode, referer) {
			var loc = '';
			loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/calendar/edit_event" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&inode=' + inode + '&referer=' + referer;
			top.location = loc;
	}

	function editRecurrentEvent(inode, startDate, endDate, referer) {
		CalendarAjax.disconnectEvent(inode, startDate, endDate, function(event){
			if(event["disconnectEventErrors"] != null ){	// To show DotContentletValidationExceptions.		
				handleError(event);
			}else{	
			  var loc = '';
			  loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/calendar/edit_event" /><portlet:param name="cmd" value="edit" /></portlet:actionURL>&inode=' + event.inode + '&referer=' + referer;
			  top.location = loc;
			}
		});         
    }

	function copyEvent(inode, referer) {
		var loc = '';
		loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/calendar/edit_event" /><portlet:param name="cmd" value="copy" /></portlet:actionURL>&inode=' + inode + '&referer=' + referer;
		top.location = loc;
    }

	function copyRecurrentEvent(inode, startDate, endDate, referer) {
		CalendarAjax.disconnectEvent(inode, startDate, endDate, function(event){
			if(event["disconnectEventErrors"] != null ){	// To show DotContentletValidationExceptions.		
				handleError(event);
			}else{	
			  var loc = '';
			  loc += '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/calendar/edit_event" /><portlet:param name="cmd" value="copy" /></portlet:actionURL>&inode=' + event.inode + '&referer=' + referer;
			  top.location = loc;
		  }
		});        
    }

	function publishEvent(identifier) {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-publish-this-event")) %>')) {
			CalendarAjax.publishEvent(identifier, eventActionCallback);
			hideEventDetail();
		}
	}	

	function unpublishEvent(identifier, isRecurrent) {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-un-publish-this-event")) %>')) {
			CalendarAjax.unpublishEvent(identifier, unpublishEventCallback);
			if(isRecurrent=='false'){
				hideEventDetail();
			}else{
			   hideRecEventDetail();
			   dojo.hitch(this, setTimeout(function(){
				   hideEventDetail();
	            }, 505));
			}
		}
	}

	function unpublishRecurrentEvent(inode, identifier, startDate, endDate, referer){
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-un-publish-this-event")) %>')) {
		   CalendarAjax.disconnectEvent(inode, startDate, endDate, function(event){
			 if(event["disconnectEventErrors"] != null ){	// To show DotContentletValidationExceptions.		
				handleError(event);
			}else{	
			  CalendarAjax.unpublishEvent(event.identifier, unpublishEventCallback);
			  hideRecEventDetail();
			   dojo.hitch(this, setTimeout(function(){
				   hideEventDetail();
	            }, 505));
			 }
		  });  
		}      
	}

	function unpublishEventCallback(data){//DOTCMS-5199			
		if(data["eventUnpublishErrors"] != null ){	// To show DotContentletValidationExceptions.		
			var errorDisplayElement = dijit.byId('eventUnpublishErrors');
			var exceptionData = data["eventUnpublishErrors"];			
			var errorList = "";			
				for (var i = 0; i < exceptionData.length; i++) {
					var error = exceptionData[i];				
					errorList = errorList+"<li>"+error+"</li>";					
				}			
			dojo.byId('eventUnpublishExceptionData').innerHTML = "<ul>"+errorList+"</ul>";
			//dijit.byId('savingContentDialog').hide();
			errorDisplayElement.show();					
		}else{
			eventActionCallback();
		}		
	}

	function archiveEvent(identifier) {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-archive-this-event")) %>')) {
			CalendarAjax.archiveEvent(identifier, eventActionCallback);
			hideEventDetail();
		}
	}	


	function archiveDisconnectedEvent(identifier, putBack){
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-archive-this-event")) %>')) {
			CalendarAjax.archiveDisconnectedEvent(identifier, putBack, eventActionCallback);
			hideEventDetail();
		}

	}	

	function unarchiveEvent(identifier) {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-un-archive-this-event")) %>')) {
			CalendarAjax.unarchiveEvent(identifier, eventActionCallback);
			hideEventDetail ();
		}
	}	

	function deleteEvent(identifier,isRecurrent) {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-delete-this-event")) %>')) {
			CalendarAjax.deleteEvent(identifier, unpublishEventCallback);
			if(isRecurrent=='false'){
			   hideEventDetail();
			}else{
			   hideRecEventDetail();
			   dojo.hitch(this, setTimeout(function(){
				   hideEventDetail();
	            }, 505));
			}
		}
	}

	function deleteRecurrentEvent(inode, identifier, startDate, endDate, referer) {
		if(confirm('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-delete-this-event")) %>')) {
			 CalendarAjax.disconnectEvent(inode, startDate, endDate, function(event){
				if(event["disconnectEventErrors"] != null ){	// To show DotContentletValidationExceptions.		
					handleError(event);
				}else{	
				  CalendarAjax.deleteEvent(event.identifier, unpublishEventCallback);
				  hideRecEventDetail();
				   dojo.hitch(this, setTimeout(function(){
					   hideEventDetail();
		            }, 505));
				 }
			 });  
		}
	}

	function handleError(data){
		var errorDisplayElement = dijit.byId('eventUnpublishErrors');
		var exceptionData = data["disconnectEventErrors"];			
		var errorList = "";			
			for (var i = 0; i < exceptionData.length; i++) {
				var error = exceptionData[i];				
				errorList = errorList+"<li>"+error+"</li>";					
			}			
		dojo.byId('eventUnpublishExceptionData').innerHTML = "<ul>"+errorList+"</ul>";
		errorDisplayElement.show();	
	}
	
	function eventActionCallback() {
		refreshCalendarView();
	}
	
	function addEvent() {
		var startDate = selectedDate.getFullYear() + '-' + (selectedDate.getMonth() + 1) + '-' + selectedDate.getDate() + " 00:00";
		var endDate = selectedDate.getFullYear() + '-' + (selectedDate.getMonth() + 1) + '-' + selectedDate.getDate() + " 00:00";
		var addURL = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
					<portlet:param name="cmd" value="new" />
					<portlet:param name="struts_action" value="/ext/calendar/edit_event" />
					<portlet:param name="selectedStructure" value="<%= eventStructure.getInode() %>" />
					<portlet:param name="referer" value="<%= referer %>" />
					<portlet:param name="inode" value="" />
				</portlet:actionURL>&date1=' + startDate + '&date2=' + endDate;
		window.location = addURL;	
	}
	
	function transformTimeZone(date,offset)
	{
		var localOffset = date.getTimezoneOffset();	 	
	 	var serverOffset = offset / 1000 / 60;	 	
	 	date.setMinutes(date.getMinutes() + localOffset + serverOffset);	 	
	 	return date;
	}
		
</script>
