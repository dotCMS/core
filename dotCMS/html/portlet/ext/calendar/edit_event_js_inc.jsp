<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@ page import="com.liferay.portal.language.LanguageUtil" %>
<script language='javascript' type='text/javascript'>

function submitfmEvent(form, subcmd) 
	{
	
	
	var isAjaxFileUploading = false; 
   	dojo.query(".fileAjaxUploader").forEach(function(node, index, arr){
   		FileAjax.getFileUploadStatus(node.id,{async:false, callback: function(fileStats){
   	   		if(fileStats!=null){
   	   		  isAjaxFileUploading = true;
   	   		}
   	   	}});
   	 });

  	if(isAjaxFileUploading){
  	   alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Please-wait-until-all-files-are-uploaded")) %>');
       return false;
   	}
	



	if(doesUserCancelledEdit){
	return false;
	}
	
		if(subcmd == 'assignto') {
			if (dijit.byId('taskAssignment').value == '') {
				alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Please-select-a-user-or-role-to-assign-the-task")) %>');
				return;
			}
		}
		
	<%
		boolean found = false;
		for (Field f : fields) {
			if (f.isListed()) {
	%>
	form.title.value = form.<%=f.getFieldContentlet()%>.value;
	<%			
				found = true;
				break;
			}
		}
		
		if (!found) {
	%>
	form.title.value = 'no title';
	<%
		}
	%>
	
	// http://jira.dotmarketing.net/browse/DOTCMS-2273
	//if (subcmd != ''){
		$('subcmd').value = subcmd;
	//}
	if(isContentAutoSaving){ // To avoid concurrent auto and normal saving.
		return;
	}				
	window.scrollTo(0,0);	// To show lightbox effect(IE) and save content errors. 	
	dijit.byId('savingContentDialog').show();
 	var isAutoSave = false;
 	saveEvent(isAutoSave);	 	
 	return;
}
function saveEvent(isAutoSave){

	if(isAutoSave && isContentSaving)
		return;
	
	var textAreaData = "";	
	var fmData = new Array();

	if (dijit.byId('alldayevent').attr('checked')) {
		setAllDayEvent();
	}
	
	fmData = getFormData("fm","<%= com.dotmarketing.util.WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR %>");
	
	if(isInodeSet(currentContentletInode)){
		isCheckin = false;
	}
	
	if(isAutoSave){
		isContentAutoSaving = true;
	}

	if(!isAutoSave) {
		isContentSaving = true;
	}
			
	CalendarAjax.saveEvent(fmData,isAutoSave,isCheckin,saveContentCallback);			
}
</script>

<%

	//Calculating if it is an all day event 
	//An all day event is the one that starts at 00:00:00
	// and ends at of the same day at the 23:59:59
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	
	//String startDateStr = (String)contentletForm.getStringProperty(startDateField.getFieldContentlet());
	//Date startDate = startDateStr != null?df.parse(startDateStr):new Date();
	Date startDate = contentletForm.getDateProperty(startDateField.getVelocityVarName());
	if (startDate == null)
		startDate = new Date();

	//String endDateStr = (String)contentletForm.getStringProperty(endDateField.getFieldContentlet());
	//Date endDate = endDateStr != null?df.parse(endDateStr):new Date();
	Date endDate = contentletForm.getDateProperty(endDateField.getVelocityVarName());
	if (endDate == null)
		endDate = new Date();
	
	Calendar calStartDate = new GregorianCalendar();
	calStartDate.setTime(startDate);
	Calendar calEndDate = new GregorianCalendar();
	calEndDate.setTime(endDate);
	
	boolean isAllDayEvent = 
		(calStartDate.get(Calendar.YEAR) == calEndDate.get(Calendar.YEAR) &&
			calStartDate.get(Calendar.MONTH) == calEndDate.get(Calendar.MONTH) &&
			calStartDate.get(Calendar.DAY_OF_MONTH) == calEndDate.get(Calendar.DAY_OF_MONTH) &&
			calStartDate.get(Calendar.HOUR_OF_DAY) == 0 &&
			calStartDate.get(Calendar.MINUTE) == 0 &&
			calStartDate.get(Calendar.SECOND) == 0 &&
			calEndDate.get(Calendar.HOUR_OF_DAY) == 23 &&
			calEndDate.get(Calendar.MINUTE) == 59 &&
			calEndDate.get(Calendar.SECOND) == 0);

	String googleKey = Config.getStringProperty("GOOGLE_MAPS_KEY");
%>

<script type='text/javascript' src='/dwr/interface/CalendarAjax.js'></script>
<script type='text/javascript' src='/dwr/engine.js'></script>
<script type='text/javascript' src='/dwr/util.js'></script>

<%
 	if(UtilMethods.isSet(googleKey)) {
%>
<script src="http://maps.google.com/maps?file=api&amp;v=2.x&amp;key=<%= googleKey %>" type="text/javascript"></script>
<%
 	}
%>

<script type="text/javascript">
	var startDate;
	var startDateYear;
	var startDateMonth;
	var startDateDay;
	var startDateTime;
	
	var endDate;
	var endDateYear;
	var endDateMonth;
	var endDateDay;
	var endDateTime;
		
	dojo.addOnLoad(function () {
		startDate = dojo.byId('<%=startDateField.getVelocityVarName()%>');
		startDateDate = dijit.byId('<%=startDateField.getVelocityVarName()%>Date');	
		startDateTime = dijit.byId('<%=startDateField.getVelocityVarName()%>Time');		
		
		endDate = dojo.byId('<%=endDateField.getVelocityVarName()%>');
		endDateTime = dijit.byId('<%=endDateField.getVelocityVarName()%>Time');
		endDateDate = dijit.byId('<%=endDateField.getVelocityVarName()%>Date');		
		
		dojo.connect(startDateDate, 'onChange', 'updateStartDate');
		dojo.connect(startDateTime, 'onChange', 'updateStartDate');
		dojo.connect(endDateDate, 'onChange', 'updateEndDate');
		dojo.connect(endDateTime, 'onChange', 'updateEndDate');
		dojo.connect(endDateDate, 'onMouseOver', 'endDateMessage');
		<%if(isAllDayEvent) {%>
				dijit.byId('alldayevent').attr('checked', true);
				setAllDayEvent();
		<%}%>

		updateStartRecurrenceDate('<%=startDateField.getVelocityVarName()%>');
	});
	var shownRecurrenceMessage = false;
	function endDateMessage(){
		var endDateDate = dijit.byId('<%=endDateField.getVelocityVarName()%>Date');	
		
		if(endDateDate.readOnly && !shownRecurrenceMessage){
			shownRecurrenceMessage = true;
			showDotCMSSystemMessage("<%= LanguageUtil.get(pageContext, "Multiday-recurring-events-not-supported") %>", true);
		}
		
	}
	
	
	
	
	var endDateCalendarEvent;
	
	function setAllDayEvent() {
		
		var x = "startDate";

		var y = "endDate";
		
		var startDate = dijit.byId(x + "Date");
		var endDate = dijit.byId(y + "Date");
		var startTime = dijit.byId(x + "Time");
		var endTime = dijit.byId(y + "Time");
		var start = dojo.byId(x);
		var end = dojo.byId(y);
		var startDateD = dojo.date.locale.parse(start.value, { datePattern: 'yyyy-MM-dd HH:mm', selector: "date" });
		var endDateD = dojo.date.locale.parse(end.value, { datePattern: 'yyyy-MM-dd HH:mm', selector: "date" });
	
		if(dijit.byId('alldayevent').attr('checked')) {		


			startDateD.setHours(0);
			startDateD.setMinutes(0);
			startDateD.setSeconds(0);
			startTime.setValue(startDateD);
			startTime.readOnly = true;
			startDateD.setHours(23);
			startDateD.setMinutes(59);
			startDateD.setSeconds(59);
		
			
			
			endTime.setValue(startDateD);
			endTime.readOnly = true;
			endDate.setValue(startDateD);
			endDate.readOnly = true;
			updateDate(x);			
			updateDate(y);


		} else {
			startDateTime.setValue(new Date());	
			endDateTime.setValue(new Date());			
			endDateDate.readOnly = false;
			endDateTime.readOnly = false;
			startDateTime.readOnly = false;		
			
			updateDate('<%=endDateField.getVelocityVarName()%>');
			
		}	
	}
	
	//Ensures the allday event check is handled correctly when the date is changed
	function updateStartDate() {
		var startDate 	= dojo.byId('<%=startDateField.getVelocityVarName()%>');
		var endDate 	= dojo.byId('<%=endDateField.getVelocityVarName()%>');

		
		var startDateD = dojo.date.locale.parse(startDate.value, { datePattern: 'yyyy-MM-dd HH:mm', selector: "date" });
		var endDateD = dojo.date.locale.parse(endDate.value, { datePattern: 'yyyy-MM-dd HH:mm', selector: "date" });
		if(dijit.byId('alldayevent').attr('checked')) {
			dijit.byId("<%=endDateField.getVelocityVarName()%>Date").setValue(startDateD);
			updateDate('<%=endDateField.getVelocityVarName()%>');
		}
		else if(startDateD > endDateD) {
			dijit.byId("<%=endDateField.getVelocityVarName()%>Date").setValue(startDateD);
			dijit.byId("<%=endDateField.getVelocityVarName()%>Time").setValue( new Date(0,0,0,startDateTime.value.getHours(),startDateTime.value.getMinutes(),00));			
			updateDate('<%=endDateField.getVelocityVarName()%>');

		}

	}

	function updateEndDate() {
		var startDate 	= dojo.byId('<%=startDateField.getVelocityVarName()%>');
		var endDate 	= dojo.byId('<%=endDateField.getVelocityVarName()%>');
		var startDateD = dojo.date.locale.parse(startDate.value, { datePattern: 'yyyy-MM-dd HH:mm', selector: "date" });
		var endDateD = dojo.date.locale.parse(endDate.value, { datePattern: 'yyyy-MM-dd HH:mm', selector: "date" });

		if(startDateD > endDateD) {

			startDateDate.setValue(endDateD);	

			startDateTime.setValue(new Date(0,0,0,endDateTime.value.getHours(),endDateTime.value.getMinutes(),00));			
			updateDate('<%=startDateField.getVelocityVarName()%>');
		}


	}


	
	//Locations and maps management
	
	function suggestLocations () {
		var currentValue = $F('<%=locationField.getFieldContentlet()%>');
		if(currentValue.length > 1) {
			CalendarAjax.findLocations(currentValue, suggestLocationsCallback);
		} else if(currentValue.length == 0) {
			$('locationSuggestions').hide({ duration: .4 });
		}
		showMapLink();
	}

	var isShowAllLocations = true;//DOTCMS-
	function showAllLocations () {		
		if(isShowAllLocations){
			CalendarAjax.findLocations('', suggestLocationsCallback);
			isShowAllLocations = false;
		}else{
			isShowAllLocations = true;
			$('locationSuggestions').hide({ duration: .4 });
		}
		dojo.toggleClass('showAllLocationsImg','plusIcon');
		dojo.toggleClass('showAllLocationsImg','minusIcon');
	}
	
	var buildingsCache = new Array();
	var facilitiesCache = new Array();
	
	function suggestLocationsCallback (buildings) {
		if(buildings.length > 0) {
			$('locationSuggestions').update('');
			var htmlBuffer = "";
			for (var i = 0; i < buildings.length; i++) {
				var building = buildings[i];
				buildingsCache[building.identifier] = building;
				htmlBuffer += '<a href=\"javascript: setBuilding(\'' + building.identifier + '\');\">' + building.title + '</a><br/>';		
				var facilities = building.facilities;
				for (var j = 0; j < facilities.length; j++) {
					var facility = facilities[j];
					facilitiesCache[facility.identifier] = facility;
					htmlBuffer += '&nbsp;&nbsp;&nbsp;- <a href=\"javascript: setFacility(\'' + building.identifier + '\', \'' + facility.identifier + '\');\">' + facility.title + '</a><br/>';		
				}
			}
			$('locationSuggestions').update(htmlBuffer);
			$('locationSuggestions').show({ duration: .4 });
			dojo.attr('locationSuggestions','style','position:top');
		} else {
			$('locationSuggestions').hide({ duration: .4 });
		}
	}
	<%if(UtilMethods.isSet(locationField.getFieldContentlet())){%>
		$('<%= locationField.getFieldContentlet() %>').observe('keyup', suggestLocations);	
	<%}%>
	function setBuilding(id) {
		var building = buildingsCache[id];
		var buildingInfo = building.title;
		if(building.address != null && building.address != "")
		{
			buildingInfo += " @ " + building.address;
		}		
		$('<%=locationField.getFieldContentlet()%>').value = buildingInfo;
		$('locationSuggestions').hide({ duration: .4 });
		isShowAllLocations = true;		
		dojo.toggleClass('showAllLocationsImg','plusIcon');
		dojo.toggleClass('showAllLocationsImg','minusIcon');
		showMapLink();
	}			
	
	function setFacility(buildingId, facilityId) {
		var building = buildingsCache[buildingId];
		var facility = facilitiesCache[facilityId];
		var fieldValue = building.title + ", " + facility.title;
		if(facility.roomId != null && facility.roomId != "")
			fieldValue += ", Room " + facility.roomId;
		fieldValue += " @ " + building.address;
		$('<%=locationField.getFieldContentlet()%>').value = fieldValue;
		$('locationSuggestions').hide({ duration: .4 });
		showMapLink();
	}	
	
	function hideLocationSuggestions () {
		if($('locationSuggestions') != undefined)
			$('locationSuggestions').hide({ duration: .4 });
	}
	
<%
	//Initializing all mapping functions 
 	if(UtilMethods.isSet(googleKey)) {
%>
	
	var mapAddress = "";
	function showMapLink() {
		var currentValue = $F('<%=locationField.getFieldContentlet()%>');
		var address = '';
		if(currentValue.indexOf("@") > -1) {
			var arr = currentValue.split("@");
			if(arr.length == 2) {
				address = arr[1];
				currentFullLocation = arr[0] + "<br/>" + arr[1]; 	
			}	
		} else if (currentValue.length > 0) {
			address = currentValue;
			currentFullLocation = currentValue; 	
		}
		if(address != '') {
			$('locationMapLink').show({ duration: .4 });
		} else { 
			$('locationMapLink').hide();
		}
		mapAddress = address;
		hideMap();
	}		

	function showMap() {
		changeMapLocation(mapAddress);
		$('locationMap').show({ duration: .4 });
	}
	

	
	function hideMap() {
		$('locationMap').hide({ duration: .4 });
	}
	
    var map;
    var geocoder;

    function initializeMap() {
      map = new GMap2(document.getElementById("locationMapCanvas"));
	  map.setCenter(new GLatLng(37.4419, -122.1419), 15);
      geocoder = new GClientGeocoder();
    }

	var currentFullLocation = ""; 
	var currentAddress = ""; 
	
    // showLocation() is called when you click on the Search button
    // in the form.  It geocodes the address entered into the form
    // and adds a marker to the map at that location.
    function changeMapLocation(address) {
      currentAddress = address;
      $('locationMapMessage').update('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Loading")) %>' + '...');
      $('locationMapCanvas').hide();
      geocoder.getLatLng(address, addAddressToMap);
    }	

    // addAddressToMap() is called when the geocoder returns an
    // answer.  It adds a marker to the map with an open info window
    // showing the nicely formatted version of the address and the country code.
    function addAddressToMap(point) {
      $('locationMapMessage').update('');
      $('locationMapCanvas').show();
      map.clearOverlays();
      if (!point) {
        $('locationMapMessage').update('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Unable-to-map-your-location-check-your-address")) %>');
        $('locationMapCanvas').hide();
      } else {
        map.setCenter(point, 15);
        marker = new GMarker(point);
        map.addOverlay(marker);
        marker.openInfoWindowHtml(currentFullLocation);
      }
    }	
	initializeMap();
	showMapLink();
<%
 	} else {
%>
	function showMapLink() { }

	function hideMap() {
		$('locationMap').hide({ duration: .4 });
	}
<%
 	}
%>
	
	function checkEscapeKey(jsevent) {
		if(jsevent.keyCode == 27) {
			hideLocationSuggestions();
			hideMap();
		}
	}
	
	$(document.body).observe("keyup", checkEscapeKey);	
	

	function hideEditButtonsRow() {
		
		dojo.style('editEventButtonRow', { display: 'none' });
	}
	
	function showEditButtonsRow() {
		if( typeof changesMadeToPermissions!= "undefined"){
			if(changesMadeToPermissions == true){
				dijit.byId('applyPermissionsChangesDialog').show();
			}
		}
		dojo.style('editEventButtonRow', { display: '' });
		changesMadeToPermissions = false;
	}
	
	function  resizeBrowser(){
        var viewport = dijit.getViewport();
        var viewport_height = viewport.h;
        
        var  e =  dojo.byId(editButtonRow);
        dojo.style(e, "height", viewport_height -185+ "px");

         dojo.query(".wrapperRight").forEach(function(node, index, arr){
		      dojo.style(node, "height", viewport_height -185+ "px");
		 });
    }

    dojo.addOnLoad(resizeBrowser);
    dojo.connect(window, "onresize", this, "resizeBrowser");
</script>

	
