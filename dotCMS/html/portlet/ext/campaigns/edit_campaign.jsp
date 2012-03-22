
<%@ page import="com.dotmarketing.portlets.mailinglists.factories.MailingListFactory" %>
<%@ page import="com.dotmarketing.portlets.communications.factories.CommunicationsFactory" %>
<%@ page import="com.dotmarketing.portlets.communications.model.Communication" %>
<%@ page import="com.dotmarketing.util.Config" %>
<%@ page import="com.dotmarketing.util.InodeUtils" %>
<%@ page import="com.dotmarketing.beans.Permission" %>
<%@ page import="com.dotmarketing.portlets.campaigns.model.Campaign" %>
<%@ page import="com.dotmarketing.factories.InodeFactory" %>
<%@ page import="com.dotmarketing.portlets.campaigns.struts.CampaignForm" %>
<%@ page import="com.dotmarketing.portlets.userfilter.model.UserFilter" %>
<%@ page import="com.dotmarketing.business.APILocator"%>
<%@ page import="com.dotmarketing.business.PermissionAPI"%>
<%@ include file="/html/portlet/ext/campaigns/init.jsp" %>

<%
	// pull all the user's mailing lists and stick them in the request
	java.util.List<MailingList> mlList = new ArrayList<MailingList>();
	
	PermissionAPI perAPI = APILocator.getPermissionAPI();	//Added Again
	
	boolean isMailingListAdmin = MailingListFactory.isMailingListAdmin(user);
	if (!isMailingListAdmin) {
		mlList = MailingListFactory.getMailingListsByUser(user);
		mlList.add(com.dotmarketing.portlets.mailinglists.factories.MailingListFactory.getUnsubscribersMailingList());
	}
	else {
		mlList = MailingListFactory.getAllMailingLists();		
	}
	for(MailingList mailingList : mlList)
	{			
		if(!UtilMethods.isSet(mailingList.getTitle()) ||mailingList.getTitle().trim().equalsIgnoreCase(MailingListFactory.getUnsubscribersMailingList().getTitle()))
		{
			mlList.remove(mailingList);
			break;
		}
	}

	
	request.setAttribute(com.dotmarketing.util.WebKeys.MAILING_LIST_VIEW, mlList);
	
	// pull all the communication lists and stick them in the request
	java.util.List<Communication> commList = (java.util.List<Communication>) request.getAttribute(com.dotmarketing.util.WebKeys.COMMUNICATION_LIST_VIEW);

	Campaign c;
	if (request.getAttribute(com.dotmarketing.util.WebKeys.CAMPAIGN_EDIT)!=null) {
		c = (Campaign) request.getAttribute(com.dotmarketing.util.WebKeys.CAMPAIGN_EDIT);
	}
	else {
		c = (Campaign) InodeFactory.getInode(request.getParameter("inode"),Campaign.class);
	}
	CampaignForm form = (CampaignForm)  request.getAttribute("CampaignForm");

	boolean isRecurrent = false;
//	if (c != null) {
//		isRecurrent = c.getIsRecurrent();
//	}

	if (form != null) {
		isRecurrent = form.getIsRecurrent();
	}

	int[] monthIds = CalendarUtil.getMonthIds();
	String[] months = CalendarUtil.getMonths(locale);
	String[] days = CalendarUtil.getDays(locale);
	java.util.Date startDate = c.getCStartDate();
	java.util.Date expirationDate = c.getExpirationDate();
	
	//java.util.List<Role> all_roles = PublicRoleFactory.getAllNoSystemRoles();

	Role[] userRoles = (Role[]) APILocator.getRoleAPI().loadRolesForUser(user.getUserId()).toArray(new Role[0]);

	String userRolesStr = "";
	for (int j = 0; j < userRoles.length; j++) {
		Role userRole = (Role) userRoles[j]; 
		userRolesStr += userRole.getId()+",";
	}

	boolean hasReport = false;
	String reportURL = "";
	if (c.getWasSent() && (InodeUtils.isSet(c.getInode()))) {
		hasReport = true;
		java.util.Map params = new java.util.HashMap();
		params.put("struts_action",new String[] {"/ext/campaigns/view_report"});
		params.put("inode",new String[] { c.getInode() + "" });
		reportURL = com.dotmarketing.util.PortletURLUtil.getActionURL(request, WindowState.MAXIMIZED.toString(), params);
	}

	String activeTab = "";
	
	if (form.isDisplayRecurrence())
		activeTab = "campaignRecurrenceTab";
	else
		activeTab = (request.getParameter("tab") == null?"campaignInfoTab":request.getParameter("tab"));
	
	boolean canEditAsset = perAPI.doesUserHavePermission(c, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user);
%>
<%@page import="com.dotmarketing.portlets.mailinglists.model.MailingList"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>

<%@page import="com.dotmarketing.business.Role"%><liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext,\"Edit-Email-Campaign\") %>" />
	
<html:form action='/ext/campaigns/edit_campaign' styleId="fm">
	<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="add">
    <input type="hidden" name="<portlet:namespace />redirect" value="<portlet:renderURL><portlet:param name="struts_action" value="/ext/campaigns/view_campaigns" /></portlet:renderURL>">
	<input type="hidden" name="userId" value="<%= user.getUserId() %>">
	<input type="hidden" name="webStartDate" value="">
	<input type="hidden" name="webExpirationDate" value="">
	<input type="hidden" name="inode" value="<%= c.getInode() %>" />
	
	
<script language="Javascript">
	function submitfm(form) {
	
		if (document.getElementById("title").value == "") {
			alert('<%= LanguageUtil.get(pageContext,"The campaign title is required") %>');
			document.getElementById("title").focus();
			dijit.byId('campaignTabContainer').selectChild('campaignInfoTab');
			return false;
		}
		
		// set the date
		var sdMonth = parseFloat(document.getElementById('calendar_month').value) + 1;
		var sdDay = document.getElementById('calendar_day').value;
		var sdYear = document.getElementById('calendar_year').value;
		var sdHour = dijit.byId('calendar_Hour').value;
		var sdMinute = dijit.byId('calendar_minute').value;
	
		form.webStartDate.value = sdMonth + "/" + sdDay + "/" + sdYear + " " + sdHour + ":" + sdMinute + " EST";
		copyStartDate();
		
		// set the expiration date
		form.webExpirationDate.value = "";
		var edMonth = document.getElementById('calendar_emonth').value != "" ? parseFloat(document.getElementById('calendar_emonth').value) + 1 : "x";
		var edDay = document.getElementById('calendar_eday').value != "" ? parseFloat(document.getElementById('calendar_eday').value) : "x";
		var edYear = document.getElementById('calendar_eyear').value != "" ? parseFloat(document.getElementById('calendar_eyear').value) : "x";
		var edHour = dijit.byId('calendar_eHour').value != "" ? parseFloat(dijit.byId('calendar_eHour').value) : "x";
		var edMinute = dijit.byId('calendar_eminute').value != "" ? parseFloat(dijit.byId('calendar_eminute').value) : "x";
	
		if (!isNaN(edMonth) && !isNaN(edDay) && !isNaN(edYear) && !isNaN(edHour) && !isNaN(edMinute) && dijit.byId('expirationDateEnabled').checked) {
			form.webExpirationDate.value = edMonth + "/" + edDay + "/" + edYear + " " + edHour + ":" + edMinute + " EST";
			copyEndDate();
		}
		else {
			if (dijit.byId('isRecurrent').value == "true") {
				alert("<%= LanguageUtil.get(pageContext,"Expiration date is required in recurrent campaigns") %>");
				dijit.byId('campaignTabContainer').selectChild('campaignInfoTab');
				return false;
			}
		}
	
		if (!isInodeSet(dijit.byId('communicationInode').value)) {
			alert("<%= LanguageUtil.get(pageContext,"Please-select-a-Communication") %>");
			document.getElementById("communicationInode").focus();
			dijit.byId('campaignTabContainer').selectChild('campaignInfoTab');
			return false;
		}
		
		var sendToMailingList = document.getElementById('sendToMailingList');
		var sendToUserFilter = document.getElementById('sendToUserFilter');
		if (sendToMailingList.checked) {
			var mailingList = document.getElementById('mailingList');
			if (!isInodeSet(dijit.byId('mailingList').value)) {
				alert("<%= LanguageUtil.get(pageContext,"Please-select-a-Mailing-List") %>");
				dijit.byId('campaignTabContainer').selectChild('campaignInfoTab');
				mailingList.focus();
				return false;
			}
		}
		else if (sendToUserFilter.checked) {
			var userFilter = document.getElementById('userFilterInode');
			if (!isInodeSet(dijit.byId('userFilterInode').value)) {
				alert("<%= LanguageUtil.get(pageContext,"Please-select-a-User-Filter") %>");
				dijit.byId('campaignTabContainer').selectChild('campaignInfoTab');
				userFilter.focus();
				return false;
			}
		}
		
		if (dijit.byId('isRecurrent').value == "true") {
			if (dijit.byId("atInfo").checked) {
				if (!dijit.byId('atTime').checked &&
					!dijit.byId('atBetween').checked) {
					alert("<%= LanguageUtil.get(pageContext,"At-least-one-AT-option-mus-be-selected") %>");
					dijit.byId('campaignTabContainer').selectChild('campaignRecurrenceTab');
					return false;
				}
			}
			
			if (document.getElementById("everyInfo").checked) {
				if (!dijit.byId('everyDates').checked &&
					!dijit.byId('everyDays').checked) {
					alert("<%= LanguageUtil.get(pageContext,"At-least-one-EVERY-option-must-be-selected") %>");
					dijit.byId('campaignTabContainer').selectChild('campaignRecurrenceTab');
					return false;
				} else if (dijit.byId('everyDays').checked) {
					var selected = false;
					for (var i = 1; i < 8; i++) {
						if (dijit.byId('everyDay'+i).checked) {
							selected = true;
							break;
						}
					}
					
					if (!selected) {
						alert("<%= LanguageUtil.get(pageContext,"At-least-one-week-day-must-be-selected") %>");
						dijit.byId('campaignTabContainer').selectChild('campaignRecurrenceTab');
						return false;
					}
				}
			}
	
			var atInfo = dijit.byId("atInfo");
			if (atInfo.checked) {				
				if (dijit.byId('atBetween').checked) {					
					var betweenFromHour = dijit.byId('betweenFromHour').value					
					var betweenToHour = dijit.byId('betweenToHour').value;
	
					if (parseInt(betweenToHour) < parseInt(betweenFromHour) ) {
						alert("<%= LanguageUtil.get(pageContext,"The-'From-Hour'-must-be-lesser-than-'To-Hour'") %>");
						dijit.byId('campaignTabContainer').selectChild('campaignRecurrenceTab');
						return false;
					}
					
					if (document.getElementById("eachInfo").checked) {
						var hours = parseInt(document.getElementById("eachHours").value);
						var minutes = parseInt(document.getElementById("eachMinutes").value);
						
						if ((isNaN(hours) &&
							 isNaN(minutes)) ||
							((hours == 0) &&
							 (minutes == 0)) ||
							(isNaN(hours) &&
							 (minutes == 0)) ||
							((hours == 0) &&
							 isNaN(minutes))) {
							alert("<%= LanguageUtil.get(pageContext,"Must-specify-each-hours-and/or-minutes-with-a-value-bigger-than-0") %>");
							dijit.byId('campaignTabContainer').selectChild('campaignRecurrenceTab');
							return false;
						}
					}
				} else {
					document.getElementById("eachHours").value = "";
					document.getElementById("eachMinutes").value = "";
				}
			}
		}
		
		form.<portlet:namespace />cmd.value = '<%=Constants.ADD%>';
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/campaigns/edit_campaign" /></portlet:actionURL>';
		submitForm(form);
	
	}	
	function cancelEdit() {
		self.location = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/campaigns/view_campaigns" /></portlet:renderURL>';
	}
	
	function deleteCampaign() {
		var form = document.getElementById('fm');
		if(confirm("<%= LanguageUtil.get(pageContext,"Must-specify-each-hours-and/or-minutes-with-a-value-bigger-than-0") %>")){
				form.<portlet:namespace />cmd.value = '<%=Constants.DELETE%>';
				form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/campaigns/edit_campaign" /></portlet:actionURL>';
				submitForm(form);
		}
	}

	function copyCampaign() {
		if(confirm("<%= LanguageUtil.get(pageContext,"Are-you-sure-you-want-to-copy-this-campaign") %>")){
			var form = document.getElementById('fm');
			form.<portlet:namespace />cmd.value = 'copy';
			form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/campaigns/edit_campaign" /></portlet:actionURL>';
			submitForm(form);
		}
	}
	
	function showHideHtml(){
		var ele = document.getElementById("htmlOrText");
	
		if(ele.checked ){
			document.getElementById("htmlDiv").style.display="";
			document.getElementById("textDiv").style.display="none";
		}
		else{
			document.getElementById("htmlDiv").style.display="none";
			document.getElementById("textDiv").style.display="";
	
		}
	}

	function displayProperties(id) {
		
		if (id == "campaignInfoTab") {

			//display basic campaignInfoTab
			document.getElementById("campaignInfoTab").style.display = "";			
			document.getElementById("campaignRecurrenceTab").style.display = "none";
						
		<% if (canEditAsset) { %>
			document.getElementById("campaignPermissionsTab").style.display = "none";
		<% } %>			
						
		<% if (hasReport) { %>
			document.getElementById("reports_link").className = "beta";
		<% } %>
		}
		else if (id == "campaignPermissionsTab") {

			//display basic campaignInfoTab
			document.getElementById("campaignInfoTab").style.display = "none";			
			document.getElementById("campaignRecurrenceTab").style.display = "none";
			
		<% if (canEditAsset) { %>
			document.getElementById("campaignPermissionsTab").style.display = "";
		<% } %>	
			
		<% if (hasReport) { %>
			document.getElementById("reports_link").className = "beta";
		<% } %>

		}
		else if (id == "campaignRecurrenceTab") {
			
			if(dijit.byId('isRecurrent').value == "true")  {
				
				copyStartDate();
				copyEndDate();
				
				//display basic campaignInfoTab
				document.getElementById("campaignInfoTab").style.display = "none";				
				document.getElementById("campaignRecurrenceTab").style.display = "";
				
			<% if (canEditAsset) { %>
				document.getElementById("campaignPermissionsTab").style.display = "none";
			<% } %>	
				
		<% if (hasReport) { %>
				document.getElementById("reports_link").className = "beta";
		<% } %>

			}			
			return false;
		}
	}

		
	var userRoles = "<%=userRolesStr%>";
	function checkRead(checkId) {
		var writeRole = document.getElementsByName('writeRole');
		var readRole = document.getElementsByName('readRole');
		var size = readRole.length;
	
		for (var i = 0;i < size; i++)
		{
			if (readRole[i].value == checkId)
			{
				if (!readRole[i].checked) {
					if (userRoles.indexOf(checkId) >= 0) {
						alert("<%= LanguageUtil.get(pageContext,"Warning-You-Are-Deselecting-one-of-your-roles") %>");
					}
					writeRole[i].checked = false;
				}
				break;
			}
		}
	}
	
	function checkWrite(checkId) {
		var writeRole = document.getElementsByName('writeRole');
		var readRole = document.getElementsByName('readRole');
		var size = writeRole.length;
	
		for (var i = 0;i < size; i++)
		{
			if (writeRole[i].value == checkId)
			{
				if (writeRole[i].checked)
					readRole[i].checked = true;
				else {
					if (userRoles.indexOf(checkId) >= 0) {
						alert("<%= LanguageUtil.get(pageContext,"Warning-You-Are-Deselecting-one-of-your-roles") %>");
					}
				}
				break;
			}
		}
	}

	function checkAllWrite() {
		var allReadCheck = document.getElementById('allReadCheck');
		var allWriteCheck = document.getElementById('allWriteCheck');
		var writeRole = document.getElementsByName('writeRole');
		var readRole = document.getElementsByName('readRole');
		var size = writeRole.length;
	
		if (allWriteCheck.checked) {
			allReadCheck.checked = true;
		}
		else {
			alert("<%= LanguageUtil.get(pageContext,"Warning-You-Are-Deselecting-one-of-your-roles") %>");
		}	
	
		for (var i = 0;i < size; i++)
		{
			if (!allWriteCheck.checked) {
				writeRole[i].checked = false;
			}
			else {
				writeRole[i].checked = true;
				readRole[i].checked = true;
			}
		}
	}
	
	function checkAllRead() {
		var allReadCheck = document.getElementById('allReadCheck');
		var allWriteCheck = document.getElementById('allWriteCheck');
		var writeRole = document.getElementsByName('writeRole');
		var readRole = document.getElementsByName('readRole');
		var size = readRole.length;
	
		if (!allReadCheck.checked) {
			alert("<%= LanguageUtil.get(pageContext,"Warning-You-Are-Deselecting-one-of-your-roles") %>");
			allWriteCheck.checked = false;
		}
	
		for (var i = 0;i < size; i++)
		{
			if (!allReadCheck.checked) {
				readRole[i].checked = false;
				writeRole[i].checked = false;
			}
			else {
				readRole[i].checked = true;
			}
		}
	}
	function enableDisable() {		
		var sendToMailingList = document.getElementById('sendToMailingList');
		var sendToUserFilter = document.getElementById('sendToUserFilter');
		var mailingList = document.getElementById('mailingList');
		var userFilter = document.getElementById('userFilterInode');
		if (sendToMailingList.checked) {
			if(dijit.byId('userFilterInode')){
				dijit.byId('userFilterInode').value="";
				document.getElementById('userFilterInode').value="";
		    	dijit.byId('userFilterInode').disabled = true;		    		
			}			
			dijit.byId('mailingList').disabled = false;
	    	dijit.byId('mailingList').focus();			
		}
		else if (sendToUserFilter.checked) {
			if(dijit.byId('mailingList')){
				dijit.byId('mailingList').value="";
				document.getElementById('mailingList').value="";
				dijit.byId('mailingList').disabled = true;
			}				    	
	    	dijit.byId('userFilterInode').disabled = false;
	    	dijit.byId('userFilterInode').focus();			
		}
	}

	function checkDate(element, fieldName) {
		/*if (element.checked) {
			eval("document.getElementById('" + fieldName + "Div').style.visibility = ''");
		} else {
			eval("document.getElementById('" + fieldName + "Div').style.visibility = 'hidden'");
		}*/
	}
	
	function <portlet:namespace />setCalendarDate_0(year, month, day) {	  
		document.forms[0].startDateYear.value = year;
		document.forms[0].startDateMonth.value = --month;
		document.forms[0].startDateDay.value = day;
		updateDate('startDate');
	}
	
	function <portlet:namespace />setCalendarDate_1(year, month, day) {	  
		document.forms[0].endDateYear.value = year;
		document.forms[0].endDateMonth.value = --month;
		document.forms[0].endDateDay.value = day;
		updateDate('endDate');
	}

	function <portlet:namespace />setCalendarDate_2(year, month, day) {	  
		dijit.byId('everyDateYear').attr('value',year);
		dijit.byId('everyDateMonth').attr('value',--month);
		dijit.byId('everyDateDay').attr('value',day);
		updateDateOnly('everyDate');
	}
	
	function setCalendarEveryDate() {
		var calendarEveryDate = dijit.byId('everyDate').attr('value');

		if (calendarEveryDate != null) {
			document.getElementById('everyDateYear').value = calendarEveryDate.getFullYear();
			document.getElementById('everyDateMonth').value = calendarEveryDate.getMonth();
			document.getElementById('everyDateDay').value = calendarEveryDate.getDate();
		}
	}
	
	function setCalendarStartDate() {
		var calendarStartDate = dijit.byId('calendar').attr('value');
		document.getElementById('calendar_year').value = calendarStartDate.getFullYear();
		document.getElementById('calendar_month').value = calendarStartDate.getMonth();
		document.getElementById('calendar_day').value = calendarStartDate.getDate();
	}
	
	function setCalendarExpirationDate() {
		var calendarExpirationDate = dijit.byId('calendar_e').attr('value');
		document.getElementById('calendar_eyear').value = calendarExpirationDate.getFullYear();
		document.getElementById('calendar_emonth').value = calendarExpirationDate.getMonth();
		document.getElementById('calendar_eday').value = calendarExpirationDate.getDate();
	}

	function checkCalendarExpirationDate() {

		var sdMonth = parseInt(document.getElementById('calendar_month').value);
		var sdDay = document.getElementById('calendar_day').value;
		var sdYear = document.getElementById('calendar_year').value;
		var sdHour = parseInt(dijit.byId('calendar_Hour').value);
		var sdMinute = dijit.byId('calendar_minute').value;
		var startDate = new Date(sdYear, sdMonth, sdDay, sdHour, sdMinute);

		var edMonth = parseInt(document.getElementById('calendar_emonth').value);
		var edDay = document.getElementById('calendar_eday').value;
		var edYear = document.getElementById('calendar_eyear').value;
		var edHour = parseInt(dijit.byId('calendar_eHour').value);
		var edMinute = dijit.byId('calendar_eminute').value;
		var expirationDate = new Date(edYear, edMonth, edDay, edHour, edMinute);

		if(startDate > expirationDate) {
			var newExpirationDate = new Date(sdYear, sdMonth, sdDay, sdHour + 1, sdMinute);
			dijit.byId('calendar_e').attr('value', newExpirationDate);
			dijit.byId('calendar_eHour').attr('value', newExpirationDate.getHours());
			dijit.byId('calendar_eminute').attr('value', newExpirationDate.getMinutes());
			amPm('calendar_e');
		}

	}
	
	function enableExpirationDate() {
		
		checkCalendarExpirationDate();
		
		if(dijit.byId('expirationDateEnabled').checked) {
			dijit.byId('calendar_e').setDisabled(false);
			dijit.byId('calendar_eHour').setDisabled(false);
			dijit.byId('calendar_eminute').setDisabled(false);
		} else {
			dijit.byId('calendar_e').setDisabled(true);
			dijit.byId('calendar_eHour').setDisabled(true);
			dijit.byId('calendar_eminute').setDisabled(true);
		}
	}		
		

	function setCalendars(fieldName) {
		eval("var month = document.forms[0]." + fieldName + "Month.value");
		eval("var day =   document.forms[0]." + fieldName + "Day.value");
		eval("var year =  document.forms[0]." + fieldName + "Year.value");
		var date= month + "/" + day + "/" + year;
		eval("document.forms[0]." + fieldName + ".value = date");
	}
	
	  function updateDate(field)
	  {
	  	eval("var year  = document.forms[0]." + field + "Year.value");
	  	eval("var month = document.forms[0]." + field + "Month.value");
	  	month = parseInt(month) + 1;
	  	eval("var day = document.forms[0]." + field + "Day.value");
	  	eval("var hour = document.forms[0]." + field + "Hour.value");
	  	eval("var minute = document.forms[0]." + field + "Minute.value");
	  	//eval("var second = document.forms[0]." + field + "Second.value");
	  	
	  	//var date = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second;
	  	var date = year + "-" + month + "-" + day + " " + hour + ":" + minute + ":00";
	  	eval("document.forms[0]." + field + ".value = date");
	  }

	  function updateDateOnly(field)
	  {
	  	eval("var year  = document.getElementById('" + field + "Year').value");
	  	eval("var month = document.getElementById('" + field + "Month').value");
	  	month = parseInt(month) + 1;
	  	eval("var day = document.getElementById('" + field + "Day').value");
	  	
	  	var date = year + "-" + month + "-" + day;
	  	eval("document.getElementById('" + field +"').value = date");
	  }
	  
	  function updateFieldFromDate(field)
	  {	  		
	  	eval("var date = document.forms[0]." + field + ".value");
	  	var dateAux = date.split(" ")[0];
	  	var timeAux = date.split(" ")[1];
		
		if(dateAux != null)
		{
		  	var dateArray = dateAux.split("-");
		  	if(dateArray.length >= 3)
		  	{	  
			 	var year = dateArray[0];
			 	var month = dateArray[1];
			 	month = parseInt(trimZero(month)) - 1;
		  		var day = dateArray[2];
		  		day = parseInt(trimZero(day));
	  		  
			  	eval("document.forms[0]." + field + "Day.value = day");
			 	eval("document.forms[0]." + field + "Month.value = month");
				eval("document.forms[0]." + field + "Year.value = year");	
	  		}
	  	}
	  	
	  	if(timeAux != null)
	  	{
		  	var timeArray = timeAux.split(":");
		  	if (timeArray.length >= 2)
		  	{
		  		var hour = timeArray[0];
		  		hour = parseInt(trimZero(hour));
			  	var minute = timeArray[1];
			  	var second = timeArray[2];
	  		  
			  	eval("document.forms[0]." + field + "Hour.value = hour");
			  	eval("document.forms[0]." + field + "Minute.value = minute");
			  	eval("document.forms[0]." + field + "Second.value = second");
		  	}	  	
	  	}
	  	amPm(field);
	  }
	  
	  function amPm(fieldName)
	  {	 
		var ele = document.getElementById(fieldName + "PM");

		var val = 0;
		
		if(dijit.byId(fieldName+'Hour')){		
			eval("val = dijit.byId('" + fieldName + "Hour').value");
		}
		
		if(val > 11)
		{
			ele.innerHTML = "PM";
		}
		else
		{
			ele.innerHTML = "AM";
		}
	}
		
	function copyStartDate() {
		
		document.getElementById('startDateMonthText').value = Calendar._MN[document.getElementById('calendar_month').value];
		document.getElementById('startDateMonth').value = document.getElementById('calendar_month').value;
		
		document.getElementById('startDateDay').value = document.getElementById('calendar_day').value;
		
		document.getElementById('startDateYear').value = document.getElementById('calendar_year').value;
		
		document.getElementById('startDateHourText').value = dijit.byId('calendar_Hour').attr('displayedValue');
		document.getElementById('startDateHour').value = dijit.byId('calendar_Hour').value;
		
		document.getElementById('startDateMinute').value = dijit.byId('calendar_minute').value;

		document.getElementById('startDatePM').innerHTML = document.getElementById('calendar_PM').innerHTML

		updateDate('startDate');
	}

	function copyEndDate() {
		
		document.getElementById('endDateMonthText').value = Calendar._MN[document.getElementById('calendar_emonth').value];
		document.getElementById('endDateMonth').value = document.getElementById('calendar_emonth').value;
		
		document.getElementById('endDateDay').value = document.getElementById('calendar_eday').value;
		
		document.getElementById('endDateYear').value = document.getElementById('calendar_eyear').value;
		
		document.getElementById('endDateHourText').value = dijit.byId('calendar_eHour').attr('displayedValue');
		document.getElementById('endDateHour').value = dijit.byId('calendar_eHour').value;
		
		document.getElementById('endDateMinute').value = dijit.byId('calendar_eminute').value;

		document.getElementById('endDatePM').innerHTML = document.getElementById('calendar_ePM').innerHTML

		updateDate('endDate');
	}

	function uploadNewList(){
		var href = "<portlet:renderURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
		href = href + "<portlet:param name='struts_action' value='/ext/usermanager/view_usermanagerlist' />";
		href = href + "<portlet:param name='cmd' value='load' />";
		href = href + "<portlet:param name='page' value='1' />";
		href = href + "</portlet:renderURL>";
		document.location.href = href;
	}

	function hideEditButtonsRow() {
		
		dojo.style('editCampaignButtonRow', { display: 'none' });
	}
	
	function showEditButtonsRow() {
		if( typeof changesMadeToPermissions!= "undefined"){
			if(changesMadeToPermissions == true){
				dijit.byId('applyPermissionsChangesDialog').show();
			}
		}
		dojo.style('editCampaignButtonRow', { display: '' });
		changesMadeToPermissions = false;
	}
				
	

//<liferay:include page="/html/js/calendar/calendar_js_box_ext.jsp" flush="true">
	//<liferay:param name="calendar_num" value="<%= Integer.toString(5) %>" />
//</liferay:include>
 
</script>



<% if (hasReport) { %>
	<div class="buttonBoxRight">
		<button dojoType="dijit.form.Button" onClick="document.location.href = '<%= reportURL %>';" iconClass="statisticsIcon" type="button">
			<%= LanguageUtil.get(pageContext,"Reports") %>
		</button>
	</div>
<%}%>

<!-- START Tabs -->
<div id="campaignTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">

<!-- START Campaign Info Tab -->
	<div id="campaignInfoTab" dojoType="dijit.layout.ContentPane" onShow="showEditButtonsRow()" title="<%= LanguageUtil.get(pageContext,"Campaign-Info") %>">
		<dl>
			<dt><%= LanguageUtil.get(pageContext,"Title") %>:</dt>
			<dd><input dojoType="dijit.form.TextBox" type="text" styleClass="form-text" style="width:300" name="title" id="title" value="<%=form.getTitle()!=null?form.getTitle():"" %>" /></dd>
			
			<dt><%= LanguageUtil.get(pageContext,"active") %>:</dt>
			<dd>
				<input dojoType="dijit.form.CheckBox" type="checkbox" name="active" id="active" <%if(form.isActive()){ %> checked="checked" <% } %>	/>
			</dd>
			
			<dt><%= LanguageUtil.get(pageContext,"Start-Date") %>:</dt>
			<dd>
				<%
					Calendar startDateCal = new GregorianCalendar();
					startDateCal.setTime(startDate);
				%>
				<input type="text" dojoType="dijit.form.DateTextBox" validate='return false;' invalidMessage="" id="calendar" name="calendar" value="<%= startDateCal.get(Calendar.YEAR) + "-" + (startDateCal.get(Calendar.MONTH) < 9 ? "0" : "") + (startDateCal.get(Calendar.MONTH) + 1) + "-" + (startDateCal.get(Calendar.DAY_OF_MONTH) < 10 ? "0" : "") + startDateCal.get(Calendar.DAY_OF_MONTH) %>" onchange="setCalendarStartDate(); checkCalendarExpirationDate();" />
				<input type="hidden" name="calendar_month" id="calendar_month" value="<%= startDateCal.get(Calendar.MONTH) %>" />
				<input type="hidden" name="calendar_day" id="calendar_day" value="<%= startDateCal.get(Calendar.DATE) %>" />
				<input type="hidden" name="calendar_year" id="calendar_year" value="<%= startDateCal.get(Calendar.YEAR) %>" />

				<select dojoType="dijit.form.FilteringSelect"  style="width:6em" name="calendar_Hour" id="calendar_Hour" onChange="checkCalendarExpirationDate(); amPm('calendar_');" style="visibility:visible">
					<%
						int sdHour = startDateCal.get(Calendar.HOUR_OF_DAY);
						for (int i = 0; i < 24; i++) {
						
						int val = i > 12 ?  i - 12: i;
						if(val ==0) val = 12;
					%>
						<option <%= (sdHour == i) ? "selected" : "" %> value="<%= i %>"><%= val %></option>
					<% } %>
				</select> : 
				
				<select dojoType="dijit.form.FilteringSelect"  style="width:6em" name="calendar_minute" id="calendar_minute" onchange="checkCalendarExpirationDate();" style="visibility:visible">
					<%
						int currentMin  = startDateCal.get(Calendar.MINUTE);
						boolean selected = false;
						for (int i = 0; i < 60; i= (i+5)) {
							String val = (i< 10) ? "0" + i: String.valueOf(i);
					%>
								<option <%= (i >= currentMin && ! selected) ? "selected" : "" %> value="<%= val %>"><%= val %></option>
					<%
						if(i >= currentMin) selected = true;
						}
					%>
				</select>
				
				<span id="calendar_PM"><%=(sdHour > 11) ? "PM" : "AM"%></span>
			</dd>

			<dt><%= LanguageUtil.get(pageContext,"Expiration-Date") %>:</dt>
			<dd>
				<%
					Calendar expirationDateCal = new GregorianCalendar();
					String noValueSelected = "";
					if (UtilMethods.isSet(expirationDate)) {
						expirationDateCal.setTime(expirationDate);
					} else {
						noValueSelected = "selected";
						expirationDateCal.setTime(startDate);
						expirationDateCal.add(Calendar.HOUR, 1);
					}
				%>
				<input dojoType="dijit.form.CheckBox" type="checkbox" name="expirationDateEnabled" id="expirationDateEnabled" onchange="enableExpirationDate" <%= UtilMethods.isSet(expirationDate)?"checked=\"checked\"":"" %> />
				<input type="text" <% if(!UtilMethods.isSet(expirationDate)){ %>disabled="disabled"<% } %> dojoType="dijit.form.DateTextBox" validate='return false;' invalidMessage="" id="calendar_e" name="calendar_e" value="<%= expirationDateCal.get(Calendar.YEAR) + "-" + (expirationDateCal.get(Calendar.MONTH) < 9 ? "0" : "") + (expirationDateCal.get(Calendar.MONTH) + 1) + "-" + (expirationDateCal.get(Calendar.DAY_OF_MONTH) < 10 ? "0" : "") + expirationDateCal.get(Calendar.DAY_OF_MONTH) %>" onchange="setCalendarExpirationDate();" />
				<input type="hidden" name="calendar_emonth" id="calendar_emonth" value="<%= expirationDateCal.get(Calendar.MONTH) %>" />
				<input type="hidden" name="calendar_eday" id="calendar_eday" value="<%= expirationDateCal.get(Calendar.DATE) %>" />
				<input type="hidden" name="calendar_eyear" id="calendar_eyear" value="<%= expirationDateCal.get(Calendar.YEAR) %>" />
					
				<select <% if(!UtilMethods.isSet(expirationDate)){ %>disabled="disabled"<% } %> dojoType="dijit.form.FilteringSelect"  style="width:6em" name="calendar_eHour" id="calendar_eHour" onChange="amPm('calendar_e');" style="visibility:visible">
						<%
							int edHour = 25;
							edHour = expirationDateCal.get(Calendar.HOUR_OF_DAY);
						%>
							<option <%= noValueSelected %> value=""></option>
						<%
							for (int i = 0; i < 24; i++) {
								int val = i > 12 ?  i - 12: i;
								if(val ==0) val = 12;
						%>
							<option <%= (edHour == i) ? "selected" : "" %> value="<%= i %>"><%= val %></option>
						<% } %>
				</select> :
					 
				<select <% if(!UtilMethods.isSet(expirationDate)){ %>disabled="disabled"<% } %> dojoType="dijit.form.FilteringSelect"  style="width:6em" name="calendar_eminute" id="calendar_eminute" style="visibility:visible">
					<%
						int currentEMin = 65;
						noValueSelected = "";
						currentEMin = expirationDateCal.get(Calendar.MINUTE);
					%>
						<option <%= noValueSelected %> value=""></option>
					<%
						boolean eSelected = false;
						for (int i = 0; i < 60; i= (i+5)) {
						
							String val = (i< 10) ? "0" + i: String.valueOf(i);
							%>
								<option <%= (i >= currentEMin && ! eSelected) ? "selected" : "" %> value="<%= val %>"><%= val %></option>
							<%
							if(i >= currentEMin) eSelected = true;
						}
					%>
				</select>
					
				<span id="calendar_ePM"><%=(edHour > 11) ? "PM" : "AM"%></span>
			</dd>
		
		<% if(c.getCompletedDate() != null) { %>
		<dt><%= LanguageUtil.get(pageContext,"Completed-Date") %>:</dt>
		<dd><%=com.dotmarketing.util.UtilMethods.webifyString(c.getCompletedDate() + "")%>&nbsp;</dd>
		<% } %>
		
		<dt><%= LanguageUtil.get(pageContext,"Mails-Send-Per-Hour") %>:</dt>
		<dd>
			<select dojoType="dijit.form.FilteringSelect"  style="width:10em" style="width:100;visibility:visible" name="sendsPerHour" id="sendsPerHour">
			
				<option value="unlimited"
				<%if(form.getSendsPerHour() != null && form.getSendsPerHour().equalsIgnoreCase("unlimited")){%>
					selected="selected"
				<% } %> ><%= LanguageUtil.get(pageContext,"Unlimited") %></option>
				
				<option value="50" 
				<%if(form.getSendsPerHour() != null && form.getSendsPerHour().equalsIgnoreCase("50")){%>
					selected="selected"
				<% } %> >50</option>
				
				<option value="100"
				<%if(form.getSendsPerHour() != null && form.getSendsPerHour().equalsIgnoreCase("100")){%>
					selected="selected"
				<% } %> >100</option>
				
				<option value="250"
				<%if(form.getSendsPerHour() != null && form.getSendsPerHour().equalsIgnoreCase("250")){%>
					selected="selected"
				<% } %> >250</option>
				
				<option value="500"
				<%if(form.getSendsPerHour() != null && form.getSendsPerHour().equalsIgnoreCase("500")){%>
					selected="selected"
				<% } %> >500</option>
				
				<option value="1000"
				<%if(form.getSendsPerHour() != null && form.getSendsPerHour().equalsIgnoreCase("1000")){%>
					selected="selected"
				<% } %> >1000</option>
				
			</select>
		</dd>
			
		<dt><%= LanguageUtil.get(pageContext,"Communication") %>:</dt>
		<dd>
			<select dojoType="dijit.form.FilteringSelect" style="width:14em"  name="communicationInode" id="communicationInode" >
				<option value=""></option>
				<% 	for(Communication com : commList){ %>
					<option value="<%= com.getInode() %>" <%if(com.getInode().equalsIgnoreCase(form.getCommunicationInode())){ %> selected="selected" <%} %> ><%= com.getTitle() %></option> 
				<% } %>					
			</select>				
		</dd>
			
		<dt><%= LanguageUtil.get(pageContext,"Allow-duplicates") %>:</dt>
		<dd>
			<input dojoType="dijit.form.CheckBox" type="checkbox" name="sendEmail" id="sendEmail"  
			<%if(form.isSendEmail()){ %>
				checked="checked"
			<% } %>
			/>
			<%= LanguageUtil.get(pageContext,"If-a-user-has-already-received-this-communication") %>
		</dd>
			
		<dt><%= LanguageUtil.get(pageContext,"Send-to") %>:</dt>
		<dd>
			<input dojoType="dijit.form.RadioButton" type="radio" id="sendToMailingList" name="sendTo"  value="mailingList" onclick="enableDisable();" 
			<%if(form.getSendTo() != null && form.getSendTo().equalsIgnoreCase("mailingList")){ %>
				checked="checked"
			<% } %>
			/>&nbsp;
			<font class="bg" size="2"><%= LanguageUtil.get(pageContext,"Mailing-List") %>:</font>&nbsp;
			<select dojoType="dijit.form.FilteringSelect"  style="width:14em" style="width:300;visibility:visible" name="mailingList" id="mailingList">
				<option value=""></option>
				<% 	for(MailingList mail : mlList){ %>
					<option value="<%= mail.getInode() %>" <%if(mail.getInode().equalsIgnoreCase(form.getMailingList())){ %> selected="selected" <%} %> ><%= mail.getTitle() %></option> 
				<% } %>
			</select>
		</dd>
			
		<dd>
			<input dojoType="dijit.form.RadioButton" type="radio" name="sendTo" id="sendToUserFilter" value="userFilter" onclick="enableDisable();" 
			<%if(form.getSendTo() != null && form.getSendTo().equalsIgnoreCase("userFilter")){ %>
				checked="checked"
			<% } %>				
			/>&nbsp;
			<font class="bg" size="2"><%= LanguageUtil.get(pageContext,"User-Filter") %>:</font>&nbsp;&nbsp;
			
			<select dojoType="dijit.form.FilteringSelect" style="width:14em;visibility:visible" name="userFilterInode" id="userFilterInode">
				<option value=""></option>
				<% 	
				List<UserFilter> userFilterList = (List)request.getAttribute("com.dotmarketing.userfilter.view.portlet");	
				for(UserFilter userFilter : userFilterList){					
				%>
					<option value="<%= userFilter.getInode() %>" <%if(userFilter.getInode().equalsIgnoreCase(form.getUserFilterInode())){ %> selected="selected" <%} %> ><%= userFilter.getUserFilterTitle() %></option> 
				<%
				}
				%>					
			</select>
		</dd>
			
			<dt><%= LanguageUtil.get(pageContext,"Campaign-Occurs") %>:</dt>
			<dd>
				<select dojoType="dijit.form.FilteringSelect"  style="width:10em" name="isRecurrent" id="isRecurrent" style="visibility:visible">
					<option <%= !isRecurrent ? "selected" : "" %> value="false"><%= LanguageUtil.get(pageContext, "Once") %></option>
					<option <%= isRecurrent ? "selected" : "" %> value="true"><%= LanguageUtil.get(pageContext, "Recurrently") %></option>
				</select>
			</dd>
		</dl>
	</div>
<!-- End Campaign Info Tab -->

<!-- Start Permissions Tab -->

<%	if (canEditAsset) { %>
	<div id="campaignPermissionsTab" dojoType="dijit.layout.ContentPane"  onShow="hideEditButtonsRow()" title="<%= LanguageUtil.get(pageContext,"permissions") %>">
		<%
			request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT, c);
			request.setAttribute(com.dotmarketing.util.WebKeys.PERMISSIONABLE_EDIT_BASE, null);
		%>
		<%@ include file="/html/portlet/ext/common/edit_permissions_tab_inc.jsp" %>
	</div>
<% } %>

<!-- End Permissions Tab -->

<!-- Start Recurrence Tab -->
<div id="campaignRecurrenceTab" dojoType="dijit.layout.ContentPane" onShow="showEditButtonsRow()" title="<%= LanguageUtil.get(pageContext,"Recurrence") %>">

	<%
		String campaignTitle = c.getTitle();
		if (!com.dotmarketing.util.UtilMethods.isSet(campaignTitle)) {
			campaignTitle = "";
		}
	%>
	<h2><%= LanguageUtil.get(pageContext,"Recurrent-Campaign") %>: <%= campaignTitle %></h2>

	<!-- begin recurrent scheduler form -->

	<input type="hidden" name="schedulerEditable" value="false" id="schedulerEditable">
	<input type="hidden" name="type" id="type" value="Recurrent Campaign">
	
	<h3><%= LanguageUtil.get(pageContext,"Execute") %>:</h3>
	
	<dl>
		<dt><%= LanguageUtil.get(pageContext,"From") %></dt>
		<dd>
			<%
				Calendar startDateCalendar = null;
				SimpleDateFormat sdf = new SimpleDateFormat(com.dotmarketing.util.WebKeys.DateFormats.DOTSCHEDULER_DATE2);
				Date schedulerStartDate;
				try {
					schedulerStartDate = sdf.parse(form.getStartDate());
				} catch (Exception e) {
					schedulerStartDate = new Date();
				}
			
				startDateCalendar = GregorianCalendar.getInstance();
				startDateCalendar.setTime(schedulerStartDate);
			%>
			<input type="hidden" name="haveStartDate" id="haveStartDate" value="true" />
			
			<div id="startDateDiv">
				<input type="text" class="form-text" style="border:none;text-align:right" name="startDateMonthText" id="startDateMonthText" maxlength="15" size="10" readonly>
				<input type="hidden" name="startDateMonth" id="startDateMonth">
				
				<input type="text" align="center" class="form-text" style="border:none;text-align:right" name="startDateDay" id="startDateDay" maxlength="2" size="2" readonly>
				
				<input type="text" align="center" class="form-text" style="border:none;text-align:right" name="startDateYear" id="startDateYear" maxlength="4" size="4" readonly>
				
				<input type="text" align="center" class="form-text" style="border:none;text-align:right" name="startDateHourText" id="startDateHourText" maxlength="2" size="2" readonly> :
				<input type="hidden" name="startDateHour" id="startDateHour">
				
				<input type="text" align="center" class="form-text" style="border:none;text-align:left" name="startDateMinute" id="startDateMinute" maxlength="2" size="2" readonly>
				
				<span id="startDatePM" style="vertical-align:top"><font class="bg" size="2">AM</font></span>
			</div>
			
			<input type="hidden" name="startDate" value="" id="startDate">
		</dd>

		<dt><%= LanguageUtil.get(pageContext,"To") %></dt>
		<dd>
			<%
				Calendar endDateCalendar = null;
				Date endDate;
				try {
					endDate = sdf.parse(form.getEndDate());
				} catch (Exception e) {
					endDate = new Date();
				}
			
			//	if (form.isHaveEndDate()) {
					endDateCalendar = GregorianCalendar.getInstance();
					endDateCalendar.setTime(endDate);
			//	}
			%>
			<input type="hidden" name="haveEndDate" id="haveEndDate" value="true" />
			
			<div id="endDateDiv">
				<input type="text" class="form-text" style="border:none;text-align:right" name="endDateMonthText" id="endDateMonthText" maxlength="15" size="10" readonly>
				<input type="hidden" name="endDateMonth" id="endDateMonth">

				<input type="text" align="center" style="border:none;text-align:right" class="form-text" name="endDateDay" id="endDateDay" maxlength="2" size="2" readonly>

				<input type="text" align="center" style="border:none;text-align:right" class="form-text" name="endDateYear" id="endDateYear" maxlength="4" size="4" readonly>

				<input type="text" align="center" style="border:none;text-align:right" class="form-text" name="endDateHourText" id="endDateHourText" maxlength="2" size="2" readonly> :
				<input type="hidden" name="endDateHour" id="endDateHour">

				<input type="text" align="center" style="border:none;text-align:left" class="form-text" name="endDateMinute" id="endDateMinute" maxlength="2" size="2" readonly>

				<span id="endDatePM" style="vertical-align:top" ><font class="bg" size="2">AM</font></span>
			</div>

			<input type="hidden" name="endDate" value="" id="endDate">

		</dd>
		
		<dt>
			<% form.setAtInfo(true); %>
			<div style="display: none;">
				<input dojoType="dijit.form.CheckBox" type="checkbox" name="atInfo" id="atInfo" checked="checked" />
			</div>
		</dt>
		<dd>&nbsp;</dd>
		
		<dt><input dojoType="dijit.form.RadioButton" type="radio" name="at" id="atTime" value="isTime" <%= com.dotmarketing.util.UtilMethods.isSet(form.getAt()) && form.getAt().equals("isTime") ? "checked" : "" %> ></dt>
		<dd>
			<%= LanguageUtil.get(pageContext,"atmessage") %>&nbsp;
			<select dojoType="dijit.form.FilteringSelect"  style="width:6em" name="atTimeHour" id="atTimeHour" onChange="amPm('atTime');" style="visibility:visible">
				<% for (int i = 0 ; i < 24; i++) 
					{
						int val = i > 12 ?  i - 12: i;
						if (val == 0)
							val = 12;
				%>
					<option <%= form.getAtTimeHour() == i ? "selected" : "" %> value="<%= i %>"><%= val %></option>
				<% } %>
			</select> :

			<select dojoType="dijit.form.FilteringSelect"  style="width:6em" name="atTimeMinute" id="atTimeMinute" style="visibility:visible">
				<% for (int i = 0; i < 60; ++i) {
					String val = (i < 10) ? "0" + i: String.valueOf(i);
				%>
					<option <%= form.getAtTimeMinute() == i ? "selected" : "" %> value="<%= i %>"><%= val %></option>
				<% } %>
			</select> :

			<select dojoType="dijit.form.FilteringSelect"  style="width:6em" name="atTimeSecond" id="atTimeSecond" style="visibility:visible">
				<% for (int i = 0; i < 60; ++i) {
					String val = (i < 10) ? "0" + i: String.valueOf(i);
				%>
					<option <%= form.getAtTimeSecond() == i ? "selected" : "" %> value="<%= i %>"><%= val %></option>
				<% } %>
			</select>

			<span id="atTimePM"><font class="bg" size="2">AM</font></span>

		</dd>
		
		<dt>
			<input dojoType="dijit.form.RadioButton" type="radio" name="at" id="atBetween" value="isBetween" <%= com.dotmarketing.util.UtilMethods.isSet(form.getAt()) && form.getAt().equals("isBetween") ? "checked" : "" %> >
		</dt>
		<dd>
			<%= LanguageUtil.get(pageContext,"between") %>&nbsp;
			<select dojoType="dijit.form.FilteringSelect"  style="width:6em" name="betweenFromHour" id="betweenFromHour" onChange="amPm('betweenFrom');" style="visibility:visible">
				<%
					for (int i = 0; i < 24; i++) 
					{
						int val = i > 12 ?  i - 12: i;
						if (val == 0)
							val = 12;
				%>
					<option <%= form.getBetweenFromHour() == i ? "selected" : "" %> value="<%= i %>"><%= val %></option>
				<% } %>
			</select> :

			<span id="betweenFromPM"><font class="bg" size="2">AM</font></span>

			<select dojoType="dijit.form.FilteringSelect"  style="width:6em" name="betweenToHour" id="betweenToHour" onChange="amPm('betweenTo');" style="visibility:visible">
				<% for (int i = 0; i < 24; i++) 
					{
						int val = i > 12 ?  i - 12: i;
						if (val == 0)
							val = 12;
				%>
					<option <%= form.getBetweenToHour() == i ? "selected" : "" %> value="<%= i %>"><%= val %></option>
				<% } %>
			</select> :

			<span id="betweenToPM"><font class="bg" size="2">AM</font></span>

		</dd>

		<dt>
			<% form.setEachInfo(true); %>
			<div style="display: none;">
				<input type="checkbox" name="eachInfo" id="eachInfo" 
				<%if(form.isEachInfo()){ %>
					checked="checked"
				<%} %>
				/> 
			</div>
		</dt>
		<dd>
			<%= LanguageUtil.get(pageContext,"each") %> <input dojoType="dijit.form.TextBox"  style="width:3em" type="text" class="form-text" name="eachHours" id="eachHours" maxlength="3" size="3" <%= 0 < form.getEachHours() ? "value=\"" + form.getEachHours() + "\"" : "" %> >&nbsp;<%= LanguageUtil.get(pageContext,"hours-and") %>&nbsp;<input dojoType="dijit.form.TextBox"  style="width:3em" type="text" class="form-text" name="eachMinutes" id="eachMinutes" maxlength="3" size="3" <%= 0 < form.getEachMinutes() ? "value=\"" + form.getEachMinutes() + "\"" : "" %> >&nbsp;<%= LanguageUtil.get(pageContext,"minutes") %>
		</dd>
		
		<dt>
			<%
				if (!form.isEveryInfo())
					form.setEvery("");
			%>
			<input dojoType="dijit.form.CheckBox" type="checkbox" name="everyInfo" id="everyInfo" 
			<%if(form.isEveryInfo()){ %>
				checked="checked"
			<%} %>
			/>		
		</dt>
		<dd>
			<%= LanguageUtil.get(pageContext,"every") %>
		</dd>
		
		<dt><input dojoType="dijit.form.RadioButton" type="radio" name="every" id="everyDates" value="isDate" <%= com.dotmarketing.util.UtilMethods.isSet(form.getEvery()) && form.getEvery().equals("isDate") ? "checked" : "" %> ></dt>
		<dd>
			<input type="text"  dojoType="dijit.form.DateTextBox" id="everyDate" name="everyDate" value="<%= form.getEveryDateYear() + "-" + (form.getEveryDateMonth()-1 < 9 ? "0" : "") + (form.getEveryDateMonth()) + "-" + (form.getEveryDateDay() < 10 ? "0" : "") + form.getEveryDateDay() %>" style="visibility:visible" onchange="setCalendarEveryDate();" />
			<input type="hidden" name="everyDateMonth" id="everyDateMonth" value="<%= form.getEveryDateMonth()-1 %>" />
			<input type="hidden" name="everyDateDay" id="everyDateDay" value="<%= form.getEveryDateDay() %>" />
			<input type="hidden" name="everyDateYear" id="everyDateYear" value="<%= form.getEveryDateYear() %>" />
		</dd>
		
		<dt>
			<input dojoType="dijit.form.RadioButton" type="radio" name="every" id="everyDays" value="isDays" <%= com.dotmarketing.util.UtilMethods.isSet(form.getEvery()) && form.getEvery().equals("isDays") ? "checked" : "" %> >
		</dt>
		<dd>
			<table>
					<td><%= LanguageUtil.get(pageContext,"Mon") %></td>
					<td><%= LanguageUtil.get(pageContext,"Tue") %></td>
					<td><%= LanguageUtil.get(pageContext,"Wed") %></td>
					<td><%= LanguageUtil.get(pageContext,"Thu") %></td>
					<td><%= LanguageUtil.get(pageContext,"Fri") %></td>
					<td><%= LanguageUtil.get(pageContext,"Sat") %></td>
					<td><%= LanguageUtil.get(pageContext,"Sun") %></td>
				</tr>
				<tr>
					<td>&nbsp;<input dojoType="dijit.form.CheckBox" type="checkbox" name="everyDay" id="everyDay1" value="MON" <%= form.isMonday() ? "checked" : "" %> ></td>
					<td>&nbsp;<input dojoType="dijit.form.CheckBox" type="checkbox" name="everyDay" id="everyDay2" value="TUE" <%= form.isTuesday() ? "checked" : "" %> ></td>
					<td>&nbsp;<input dojoType="dijit.form.CheckBox" type="checkbox" name="everyDay" id="everyDay3" value="WED" <%= form.isWednesday() ? "checked" : "" %> ></td>
					<td>&nbsp;<input dojoType="dijit.form.CheckBox" type="checkbox" name="everyDay" id="everyDay4" value="THU" <%= form.isThusday() ? "checked" : "" %> ></td>
					<td>&nbsp;<input dojoType="dijit.form.CheckBox" type="checkbox" name="everyDay" id="everyDay5" value="FRI" <%= form.isFriday() ? "checked" : "" %> ></td>
					<td>&nbsp;<input dojoType="dijit.form.CheckBox" type="checkbox" name="everyDay" id="everyDay6" value="SAT" <%= form.isSaturday() ? "checked" : "" %> ></td>
					<td>&nbsp;<input dojoType="dijit.form.CheckBox" type="checkbox" name="everyDay" id="everyDay7" value="SUN" <%= form.isSunday() ? "checked" : "" %> ></td>
				</tr>
			</table>
		</dd>
	</dl>
</div>
<!-- END Recurrence Tab -->

</div>
<!-- END Tabs -->

<div class="clear"></div>

<!-- START Buttons -->
<div class="buttonRow" id ="editCampaignButtonRow" style="">
<%
	boolean isCMSAdmin = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user, com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole());
	boolean isCampaignManagerAdmin = false;
	boolean isCampaignManagerEditor = false;
	
	List<com.dotmarketing.business.Role> roles = com.dotmarketing.business.APILocator.getRoleAPI().loadRolesForUser(user.getUserId());
	Iterator<com.dotmarketing.business.Role> rolesIt = roles.iterator();
	while (rolesIt.hasNext()) {
		com.dotmarketing.business.Role role = (com.dotmarketing.business.Role) rolesIt.next();
		if (role.getName().equals(com.dotmarketing.util.Config.getStringProperty("CAMPAIGN_MANAGER_ADMIN"))) {
			isCampaignManagerAdmin = true;
			
			if (isCampaignManagerEditor)
				break;
		} else if (role.getName().equals(com.dotmarketing.util.Config.getStringProperty("CAMPAIGN_MANAGER_EDITOR"))) {
			isCampaignManagerEditor = true;
			
			if (isCampaignManagerAdmin)
				break;
		}
	}
%>
	<% if ((InodeUtils.isSet(c.getInode())) && !c.isLocked() &&
			(perAPI.doesUserHavePermission(c,PermissionAPI.PERMISSION_WRITE,user) ||
				isCMSAdmin || isCampaignManagerAdmin || isCampaignManagerEditor)) { %>
        <button dojoType="dijit.form.Button" onClick="deleteCampaign()" iconClass="deleteIcon" type="button">
            <%= LanguageUtil.get(pageContext,"delete-campaign") %>
        </button>
	<% } %>
	
	<% if ((InodeUtils.isSet(c.getInode())) && (isCMSAdmin || isCampaignManagerAdmin || isCampaignManagerEditor)) { %>
        <button dojoType="dijit.form.Button"  onClick="copyCampaign()" iconClass="copyIcon" type="button">
            <%= LanguageUtil.get(pageContext,"copy-campaign") %>
        </button>        
	<% } %>
	
	<% if (isCMSAdmin || isCampaignManagerAdmin || isCampaignManagerEditor) { %>
    <button dojoType="dijit.form.Button" onClick="submitfm(document.getElementById('fm'))" iconClass="saveIcon" type="button">
       <%= LanguageUtil.get(pageContext,"save") %>
    </button>
    <% } %>
    
    <button dojoType="dijit.form.Button" onClick="cancelEdit()" iconClass="cancelIcon" type="button">
        <%= LanguageUtil.get(pageContext,"cancel") %>
    </button>     

</div>
<!-- END Buttons -->

<script>
dojo.require("dijit.form.DateTextBox");
	// DOTCMS - 3897
	dojo.addOnLoad(
			
			function(){			
				dijit.byId('campaignTabContainer').selectChild('<%= activeTab %>');
				
				amPm('betweenTo');
				amPm('betweenFrom');
				amPm('atTime');				
				amPm('endDate');
				amPm('startDate');
				
				checkDate(document.forms[0].haveStartDate, 'startDate');			
				checkDate(document.forms[0].haveEndDate, 'endDate');
				updateDate('startDate');
				updateDate('endDate');
				enableDisable();
				
				dojo.connect(dijit.byId('campaignRecurrenceTab'), "onClick", function(){
					
					if(dijit.byId('isRecurrent').value == 'false'){
						alert("<%= LanguageUtil.get(pageContext,"The-'Campaign-Occurs'-must-be-'Recurrently'-to-edit-recurrent-fields") %>");
						dijit.byId('campaignTabContainer').selectChild('campaignInfoTab');
						document.getElementById('isRecurrent').focus();
					}		
				});

				dojo.connect(dijit.byId('campaignRecurrenceTab'), "onShow", function(){					
					copyStartDate();
					copyEndDate();		
				});
				
				
			}
	);
			
</script>

</html:form>
</liferay:box>
