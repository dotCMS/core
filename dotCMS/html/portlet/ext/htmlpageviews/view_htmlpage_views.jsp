<%@ include file="/html/portlet/ext/htmlpageviews/init.jsp" %>
<%@ page import="com.dotmarketing.util.Config" %>
<%@ page import="com.dotmarketing.portlets.mailinglists.model.*" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="com.dotmarketing.portlets.contentlet.model.Contentlet" %>
<%@ page import="com.dotmarketing.factories.InodeFactory" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.util.InodeUtils" %>
<%@ page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@ page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.util.PortletURLUtil"%>
<%@page import="com.dotmarketing.business.LayoutAPI"%>

<%
	com.dotmarketing.portlets.htmlpages.model.HTMLPage myHTMLPage = (com.dotmarketing.portlets.htmlpages.model.HTMLPage) request.getAttribute("htmlPage");
	Host host = APILocator.getHostAPI().findParentHost(myHTMLPage, APILocator.getUserAPI().getSystemUser(), false);
	String pageUrl = request.getParameter("pageURL");
	//Mailing list parameters
	List mailingLists = (List)request.getAttribute(com.dotmarketing.util.WebKeys.MAILING_LIST_VIEW);
%>

<script type='text/javascript' src='/dwr/interface/HTMLPageViewAjax.js'></script>
<script language="Javascript">

	dojo.require("dojox.charting.Chart2D");
	dojo.require("dojox.charting.themes.PlotKit.blue");
	dojo.require("dojox.charting.widget.Legend");
	dojo.require("dojo.date.locale");	

	//Global
	var startDate = new Date();
	startDate.setUTCDate(startDate.getUTCDate() - 7);	
	var endDate = new Date();
	endDate.setUTCDate(endDate.getUTCDate() - 1);
	endDate.setHours(23, 59, 59);

	dojo.addOnLoad(function(){
		dijit.byId('startDate').attr('value', startDate);
		dijit.byId('endDate').attr('value', endDate);
	});

	function setDates() {
		startDate = dijit.byId('startDate').attr('value');
		endDate = dijit.byId('endDate').attr('value');
		dijit.byId('fetchingDataDialog').show();
		HTMLPageViewAjax.getPageStatistics("<%=myHTMLPage.getIdentifier()%>", startDate, endDate, dojo.hitch(this, fillStatistics));		
	}
	
	//START GRAPH FUNCTIONS

	function fillStatistics(data) {
		fillUsageStatistics(data);
		fillSourcesStatistics(data);
		fillTopUsersTable(data);
		fillStatisticsTables(data);
		dijit.byId('fetchingDataDialog').hide();
	}
	
	function fillUsageStatistics(data){

		dojo.byId('totalPageViews').innerHTML = data.totalPageViews;
		dojo.byId('uniqueVisitors').innerHTML = data.uniqueVisitors;
		dojo.byId('pagesVisit').innerHTML = data.pagesVisit;

		
		var seconds = data.timeOnPage;
		var minutes = Math.floor(seconds / 60);
		seconds = seconds - (minutes * 60);
		var hours = Math.floor(minutes / 60);
		minutes = minutes - (hours * 60);
		hours = hours < 10?"0"+hours:hours;
		minutes = minutes < 10?"0"+minutes:minutes;
		seconds = seconds < 10?"0"+seconds:seconds;
		dojo.byId('timeOnPage').innerHTML = hours + ":" + minutes + ":" + seconds;
		
		dojo.byId('pageBounceRate').innerHTML = data.pageBounceRate + "%";
		dojo.byId('pageExitRate').innerHTML = data.pageExitRate + "%";		

		var viewBy = data.viewBy;
		
		if(viewBy == 'DAY')
			graphSiteUsagePerDay(data);
		else if(viewBy == 'WEEK')
			graphSiteUsagePerWeek(data);
		else 
			graphSiteUsagePerMonth(data);		
		
	}

	function graphSiteUsagePerDay(data){
		
		var totalViewsPerDayData = data.totalPageViewsByDay;
		var uniqueVisitorsPerDayData = data.uniqueVisitorsByDay;
		
		//Total visits per month chart
		dojo.byId('siteUsageChartWrapper').innerHTML = '<div id="siteUsageChart" style="width: 99%; height: 250px;"></div>';
		var chart1 = new dojox.charting.Chart2D("siteUsageChart");
		chart1.addPlot("default", {
			type: "Lines",
			grid: true,
			areas: true			
		});
		chart1.setTheme(dojox.charting.themes.PlotKit.blue);
		var labels = [];

		var dayLabels = [];
		for(var i = 0; i < totalViewsPerDayData.length; i++) {
			var dayData = totalViewsPerDayData[i];
			if(i == 0){
				dayLabels.push({value: i + 1, text: dojo.date.locale.format(dayData.startDate, {datePattern: "MMM", selector: "date"} )});
			}else if(dayData.startDate.getDate() == 1){
				dayLabels.push({value: i + 1, text: dojo.date.locale.format(dayData.startDate, {datePattern: "MMM", selector: "date"} )});
			}else{
				dayLabels.push({value: i + 1, text: dojo.date.locale.format(dayData.startDate, {datePattern: "d", selector: "date"} )});
			}				
		}	
	
		chart1.addAxis("x", { 
			majorTick: {color: "grey", length: 1},
			minorLabels: false,
			minorTicks: false,
	        labels: dayLabels
	    });
		chart1.addAxis("y", {
			vertical: true, 
	        leftBottom: true, 
			includeZero: true,
	        majorTick: {color: "grey", length: 1},
			minorTick: {color: "grey", length: 0},
		});
		
		var pageViewsSeries = [];
		var uniqueVisitorsSeries = [];
		for(var i = 0; i < totalViewsPerDayData.length; i++) {
			pageViewsSeries.push(totalViewsPerDayData[i].data);
			uniqueVisitorsSeries.push(uniqueVisitorsPerDayData[i].data);
		}
		
		chart1.addSeries("<%= LanguageUtil.get(pageContext, "Page-Views") %>", pageViewsSeries);
		chart1.addSeries("<%= LanguageUtil.get(pageContext, "Unique-Visitors") %>", uniqueVisitorsSeries);
		chart1.render();

		dijit.registry.remove('legend1');
		var legend1 = new dojox.charting.widget.Legend({chart: chart1, horizontal: true}, "legend1"); 
				
	}

	function graphSiteUsagePerWeek(data){
		
		var totalViewsPerWeekData = data.totalPageViewsByWeek;
		var uniqueVisitorsPerWeekData = data.uniqueVisitorsByWeek;
		
		//Total visits per month chart
		dojo.byId('siteUsageChartWrapper').innerHTML = '<div id="siteUsageChart" style="width: 99%; height: 250px;"></div>';
		var chart1 = new dojox.charting.Chart2D("siteUsageChart");
		chart1.addPlot("default", {
			type: "Lines",
			grid: true,
			areas: true			
		});
		chart1.setTheme(dojox.charting.themes.PlotKit.blue);
		var labels = [];

		var weekLabels = [];
		for(var i = 0; i < totalViewsPerWeekData.length; i++) {
			var weekData = totalViewsPerWeekData[i];
			if(i == 0){
				weekLabels.push({value: i + 1, text: dojo.date.locale.format(weekData.startDate, {datePattern: "MMM", selector: "date"} )});
			}else if(weekData.startDate.getDate() < 8){
				weekLabels.push({value: i + 1, text: dojo.date.locale.format(weekData.startDate, {datePattern: "MMM", selector: "date"} )});
			}else {				
				weekLabels.push({value: i + 1, text: dojo.date.locale.format(weekData.startDate, {datePattern: "d", selector: "date"} )});				
			}			
		}
		
		chart1.addAxis("x", { 
			majorTick: {color: "grey", length: 1},
			minorLabels: false,
			minorTicks: false,
	        labels: weekLabels
	    });
		chart1.addAxis("y", {
			vertical: true, 
	        leftBottom: true, 
			includeZero: true,
	        majorTick: {color: "grey", length: 1},
			minorTick: {color: "grey", length: 0},
		});
		
		var pageViewsSeries = [];
		var uniqueVisitorsSeries = [];
		for(var i = 0; i < totalViewsPerWeekData.length; i++) {
			pageViewsSeries.push(totalViewsPerWeekData[i].data);
			uniqueVisitorsSeries.push(uniqueVisitorsPerWeekData[i].data);
		}
		
		chart1.addSeries("<%= LanguageUtil.get(pageContext, "Page-Views") %>", pageViewsSeries);
		chart1.addSeries("<%= LanguageUtil.get(pageContext, "Unique-Visitors") %>", uniqueVisitorsSeries);
		chart1.render();

		dijit.registry.remove('legend1');
		var legend1 = new dojox.charting.widget.Legend({chart: chart1, horizontal: true}, "legend1"); 
					
	}
		
	function graphSiteUsagePerMonth(data){


			
			var totalViewsPerMonthData = data.totalPageViewsByMonth;
			var uniqueVisitorsPerMonthData = data.uniqueVisitorsByMonth;
			
			//Total visits per month chart
			dojo.byId('siteUsageChartWrapper').innerHTML = '<div id="siteUsageChart" style="width: 99%; height: 250px;"></div>';
			var chart1 = new dojox.charting.Chart2D("siteUsageChart");
			chart1.addPlot("default", {
				type: "Lines",
				grid: true,
				areas: true			
			});
			chart1.setTheme(dojox.charting.themes.PlotKit.blue);
			var labels = [];
	
			var monthLabels = [];
			for(var i = 0; i < totalViewsPerMonthData.length; i++) {
				var monthData = totalViewsPerMonthData[i];
				monthLabels.push({value: i + 1, text: dojo.date.locale.format(monthData.startDate, {datePattern: "MMM", selector: "date"} )});
			}
			chart1.addAxis("x", { 
				majorTick: {color: "grey", length: 1},
				minorLabels: false,
				minorTicks: false,
		        labels: monthLabels
		    });
			chart1.addAxis("y", {
				vertical: true, 
		        leftBottom: true, 
				includeZero: true,
		        majorTick: {color: "grey", length: 1},
				minorTick: {color: "grey", length: 0},
			});
			
			var pageViewsSeries = [];
			var uniqueVisitorsSeries = [];
			for(var i = 0; i < totalViewsPerMonthData.length; i++) {
				pageViewsSeries.push(totalViewsPerMonthData[i].data);
				uniqueVisitorsSeries.push(uniqueVisitorsPerMonthData[i].data);
			}
			
			chart1.addSeries("<%= LanguageUtil.get(pageContext, "Page-Views") %>", pageViewsSeries);
			chart1.addSeries("<%= LanguageUtil.get(pageContext, "Unique-Visitors") %>", uniqueVisitorsSeries);
			chart1.render();
	
			dijit.registry.remove('legend1');
			var legend1 = new dojox.charting.widget.Legend({chart: chart1, horizontal: true}, "legend1"); 

		
					
	}

	function fillSourcesStatistics (data) {
		fillSourcesTable(data);
		graphTrafficSources(data);
	}
	
	var topUsersNoRecordsTemplate = '<tr><td>&nbsp;</td>\
			<td bgcolor="#eeeeee" align="center"><%= LanguageUtil.get(pageContext, "No-Records-Found") %></td>\
			<td>&nbsp;</td>\
		</tr>';

	var topUsersTemplate = '<tr><td><a href="javascript:viewUser(\'${userId}\');">${userFullName}</a></td>\
			<td bgcolor="#eeeeee" align="center">${numViews}</td>\
			<td><a href="javascript:viewUser(\'${userId}\');">${emailAddress}</a></td>\
		</tr>';

	var topUsersUnregisteredTemplate = '<tr>\
			<td><b><%= LanguageUtil.get(pageContext, "Unregistered-Users") %></b></td>\
			<td bgcolor="#eeeeee" align="center">${unregisteredViews}</td>\
			<td>&nbsp</td>\
		</tr>';

	function fillTopUsersTable (data) {

		var topUsers = data.topUsers;
		if(topUsers.length == 0) {
			dojo.style('topUserActions', { display: "none" });
			dojo.byId('topUsers').innerHTML =  dojo.string.substitute(topUsersNoRecordsTemplate, { });
		} else {
				
			var unregisteredCount = 0;
			var tableHTML = "";
			for(var i = 0; i < topUsers.length; i++) {
				var user = topUsers[i];
				if(!user.user_id || user.user_id == '') {
					unregisteredCount += parseInt(user.num_views);
				} else {
					var html = dojo.string.substitute(topUsersTemplate, { userId: user.user_id, userFullName: user.user_full_name, numViews: user.num_views, emailAddress: user.user_email });
					tableHTML += html;
				}
			}
			if(unregisteredCount > 0) {
				var html = dojo.string.substitute(topUsersUnregisteredTemplate, { unregisteredViews: unregisteredCount });
				tableHTML += html;
			}
			dojo.byId('topUsers').innerHTML = tableHTML;
			dojo.style('topUserActions', { display: "" });
		}
	}

	function fillSourcesTable (data) {
		var searchEngineVisits = data.searchEngineVisits;
		var referringSitesVisits = data.referringSitesVisits;
		var directTrafficVisits = data.directTrafficVisits;
		var totalVisits = searchEngineVisits + referringSitesVisits + directTrafficVisits;
		dojo.byId('searchEngineVisits').innerHTML = searchEngineVisits;
		var searchEngineVisitsPercentage = totalVisits == 0?0:Math.round(parseInt(searchEngineVisits) * 100 / totalVisits);
		dojo.byId('searchEngineVisitsPercentage').innerHTML = searchEngineVisitsPercentage + "%";
		dojo.byId('referringSitesVisits').innerHTML = referringSitesVisits;
		var referringSitesVisits = totalVisits == 0?0:Math.round(parseInt(referringSitesVisits) * 100 / totalVisits);
		dojo.byId('referringSitesVisitsPercentage').innerHTML = referringSitesVisits + "%";
		dojo.byId('directTrafficVisits').innerHTML = directTrafficVisits;
		dojo.byId('directTrafficVisitsPercentage').innerHTML = totalVisits == 0?0+'%':(100 - searchEngineVisitsPercentage - referringSitesVisits) + "%";
	}

	function graphTrafficSources(data) {


			var searchEngineVisits = data.searchEngineVisits;
			var referringSitesVisits = data.referringSitesVisits;
			var directTrafficVisits = data.directTrafficVisits;
			var totalVisits = searchEngineVisits + referringSitesVisits + directTrafficVisits;
	
			var searchEngineVisitsPercentage = totalVisits == 0?0:Math.round(parseInt(searchEngineVisits) * 100 / totalVisits);
			var referringSitesVisitsPercentage = totalVisits == 0?0:Math.round(parseInt(referringSitesVisits) * 100 / totalVisits);
			var directTrafficVisitsPercentage = totalVisits == 0?0:(100 - searchEngineVisitsPercentage - referringSitesVisitsPercentage);
	
			// START PIE CHART
			dojo.byId('trafficSourcesPieWrapper').innerHTML = '<div id="trafficSourcesPie" style="width: 98%; height: 250px;"></div>';
			
			var chart2 = new dojox.charting.Chart2D("trafficSourcesPie");
				chart2.addPlot("default", {
					type: "Pie",
					fontColor: "grey",
					labels: false,
					radius: 100
				}
			);
			chart2.setTheme(dojox.charting.themes.PlotKit.blue);
			chart2.addSeries("Series 1", [
				{y: searchEngineVisitsPercentage, text: "Search Engines"},
				{y: referringSitesVisitsPercentage, text: "Referring Sites"},
				{y: directTrafficVisitsPercentage, text: "Direct Traffic"}
			]);
			chart2.render();
			dijit.registry.remove('legend2');
			try {
				var legend2 = new dojox.charting.widget.Legend({chart: chart2, horizontal: true}, "legend2");
			} catch (err) { } 

	}


	var statisticsRowTemplate = 
		'<tr>\
			<td><b>${statisticTitle}</b></td>\
			<td bgcolor="#eeeeee" align="center">${statisticValue}</td>\
			<td>\
				<a href="<portlet:renderURL>\
					<portlet:param name="struts_action" value="/ext/htmlpageviews/view_htmlpage_views" />\
				</portlet:renderURL>&pageIdentifier=${pageIdentifier}">${pageURI}</a>\
			</td>\
		</tr>';

	var statisticsRowTemplate2 = 
			'<tr>\
				<td><b>${statisticTitle}</b></td>\
				<td bgcolor="#eeeeee" align="center">${statisticValue}</td>\
				<td>\
					${pageURI}\
				</td>\
			</tr>';

	function fillStatisticsTables (data) {

		var referers = data.externalReferers;
		var table = dojo.byId('externalIncomingStatisticsTable');
		fillStatisticsTable(table, referers, '<%= LanguageUtil.get(pageContext, "External-Incoming") %>');
		
		var referers = data.internalReferers;
		var table = dojo.byId('internalReferersStatisticsTable');
		fillStatisticsTable(table, referers, '<%= LanguageUtil.get(pageContext, "Incoming") %>');
		
		var referers = data.internalOutgoing;
		var table = dojo.byId('internalOutgoingStatisticsTable');
		fillStatisticsTable(table, referers, '<%= LanguageUtil.get(pageContext, "Outgoing") %>');
		
	}

	function fillStatisticsTable(table, referers, statisticsTitle) {
		if(referers.length == 0)
			return;
		var tableRows = '';
		var j = 0;
		for(var i = 0; i < referers.length; i++) {
			var referer = referers[i];
			if(referer.associated_identifier) {
				tableRows += dojo.string.substitute(statisticsRowTemplate, { statisticTitle:(j++ == 0?statisticsTitle:''), 
					statisticValue:referer.num_referring, pageIdentifier:referer.associated_identifier, pageURI:referer.request_uri });
			} else if(referer.referer != '') {
				tableRows += dojo.string.substitute(statisticsRowTemplate2, { statisticTitle:(j++ == 0?statisticsTitle:''), 
					statisticValue:referer.num_referring, pageURI:referer.referer });
			}
		}
		table.innerHTML = tableRows;
	}
		
	dojo.addOnLoad(function(){
		dijit.byId('fetchingDataDialog').show();
		HTMLPageViewAjax.getPageStatistics("<%=myHTMLPage.getIdentifier()%>", startDate, endDate, dojo.hitch(this, fillStatistics));

	});

	//END of graph functions
	
	//Mailing list management functions
	
	function saveNewMailingList() {

		if(!dijit.byId('addNewMailingListForm').validate()) {
			return;
		}

		HTMLPageViewAjax.createMailingList('<%=myHTMLPage.getIdentifier()%>', startDate, endDate, dijit.byId('mailingListTitle').attr('value'), 
				dijit.byId('allowPublicToSubscribeChk').attr('value') != false, { callback:dojo.hitch(this, saveMailingListComplete), errorHandler:failedToSaveNewMailingList });
		
	}	

	var mailingList;
	function saveNewMailingListComplete(list) {
		mailingList = list;
		dijit.byId('mailingListPopup').hide();
		dijit.byId('saveFinishedDialog').show();
	}

	function failedToSaveNewMailingList(error, exception) {
		showDotCMSErrorMessage('<%= LanguageUtil.get(pageContext, "Error-saving-mailing") %>');
	}

	function addToMailingList() {

		if(!dijit.byId('modifyMailingListForm').validate()) {
			return;
		}

		HTMLPageViewAjax.addToMailingList('<%=myHTMLPage.getIdentifier()%>', startDate, endDate, dijit.byId('mailingListInode').attr('value'), 
				{ callback:dojo.hitch(this, saveMailingListComplete), errorHandler:failedToSaveNewMailingList });
		
	}	

	function removeFromMailingList() {

		if(!dijit.byId('modifyMailingListForm').validate()) {
			return;
		}

		HTMLPageViewAjax.removeFromMailingList('<%=myHTMLPage.getIdentifier()%>', startDate, endDate, dijit.byId('mailingListInode').attr('value'), 
				{ callback:dojo.hitch(this, saveMailingListComplete), errorHandler:failedToSaveNewMailingList });
		
	}
		
	var mailingList;
	function saveMailingListComplete(list) {
		mailingList = list;
		dijit.byId('mailingListPopup').hide();
		dijit.byId('saveFinishedDialog').show();
	}

	function failedToSaveNewMailingList(error, exception) {
		dijit.byId('mailingListPopup').hide();
		showDotCMSErrorMessage('<%= LanguageUtil.get(pageContext, "Error-saving-mailing") %>');
	}	

	function goToMailingList() {
		var url = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/mailinglists/edit_mailinglist" /><portlet:param name="<%= Constants.CMD %>" value="<%= Constants.EDIT %>" /></portlet:actionURL>&inode=' + mailingList.inode;
		window.location = url;
	}

	function closeFinishedDialog() {
		dijit.byId('saveFinishedDialog').hide();
	}

	function downloadList() {
		var searchStartDate = dojo.date.locale.format(startDate, {datePattern: "MM/dd/yyyy"});
		var searchEndDate = dojo.date.locale.format(endDate, {datePattern: "MM/dd/yyyy"});
		var url = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">
			<portlet:param name="struts_action" value="/ext/htmlpageviews/html_page_report" />
			<portlet:param name="cmd" value="userReport" />
		</portlet:actionURL>&htmlpage=<%= myHTMLPage.getInode() %>&searchStartDate=' + searchStartDate + '&searchEndDate=' + searchEndDate;
		document.location = url;
	}	

	function viewUser(userId) {
		<%
			LayoutAPI lapi = APILocator.getLayoutAPI();
			if(lapi.doesUserHaveAccessToPortlet("EXT_USER_ADMIN", user)) {
				Map<String, String[]> paramsMap = new HashMap<String, String[]>();
				paramsMap.put("struts_action", new String[] { "/ext/useradmin/view_users" });
				String viewUserURL = PortletURLUtil.getRenderURL(request, WindowState.MAXIMIZED.toString(), paramsMap, "EXT_USER_ADMIN");
		%>
		var url = '<%=viewUserURL%>&userId=' + userId;
		window.location = url;
		<%
			}
		%>
	}
	
</script>

<form id="fm" method="post">

<style>
	h3{color:#2C548D;}
	.dojoxLegendHorizontal{margin: 0 0 20px 35px;}
	.dojoxLegendText{padding: 0 15px 0 3px;}
	.listingTable{margin-bottom:10px;}
</style>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= LanguageUtil.get(pageContext, "view-htmlpage-views-all") %>' />

<div class="shadowBox headerBox" style="padding:10px;">
	
	<!-- TITLE BOX -->
	<div style="margin-bottom:10px;">
		<h3><%= LanguageUtil.get(pageContext, "Page-View-Statistics-for") %>&nbsp;:&nbsp;
		<%if (!InodeUtils.isSet(host.getInode())) {%>
			<%=pageUrl%> (<%= myHTMLPage.getTitle() %>)
		<%} else {%>
			<A style="font-weight:normal;" href="http://<%=host.getHostname()%><%=UtilMethods.encodeURIComponent(request.getAttribute("uri").toString())%>?host_id=<%=host.getIdentifier()%>"><%=host.getHostname()%>:<%=request.getAttribute("uri")%></a> (<%= myHTMLPage.getTitle() %>)
		<% } %>
		</h3>
	</div>
	<!-- END TITLE BOX -->
	
	<!-- DATE FILTER BOX -->
	<div class="callOutBox" style="background-color:#f1f1f1;">
		<b><%= LanguageUtil.get(pageContext, "From") %>:</b>
		<input dojoType="dijit.form.DateTextBox" type="text" id="startDate" name="startDate" value="" >
	
		&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;

		<b><%= LanguageUtil.get(pageContext, "End-Date") %>:</b>
		<input dojoType="dijit.form.DateTextBox" type="text" id="endDate" name="endDate" value="" >

		&nbsp;&nbsp;
		<button dojoType="dijit.form.Button" onClick="setDates()" iconClass="previewIcon">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "filter")) %>
		</button>
	</div>

<!-- END DATE FILTER BOX -->
	
	
<div class="yui-g">
		
<!-- SITE USAGE -->
	<div class="yui-u first">
			
		<div class="shadowBoxLine">
			<h3 style="margin:-35px 0 10px 10px;"><%= LanguageUtil.get(pageContext, "Site-Usage") %></h3>
			<div id="siteUsageChartWrapper">
				<div id="siteUsageChart" style="width: 99%; height: 250px;"></div>
			</div>
			<div id="legend1"></div>
			<div style="margin:0 20px;">
				<table class="listingTable" style="font-weight:bold;font-size:108%;">
					<tr>
						<td width="50%"><span id="totalPageViews">...</span> <span style="color:#2C548D;"><%= LanguageUtil.get(pageContext, "Page-Views") %></span></td>
						<td width="50%"><span id="uniqueVisitors">...</span> <span style="color:#2C548D;"><%= LanguageUtil.get(pageContext, "Unique-Visitors") %></span></td>
					</tr>
					<tr>
						<td><span id="timeOnPage">...</span> <span style="color:#2C548D;">Time on Page</span></td>
						<td><span id="pageBounceRate">...</span> <span style="color:#2C548D;">Bounce Rate</span></td>
					</tr>
					<tr>
						<td><span id="pagesVisit">...</span> <span style="color:#2C548D;">Pages/Visit</span></td>
						<td><span id="pageExitRate">...</span> <span style="color:#2C548D;">Exit</span></td>
					</tr>
				</table>
			</div>
		</div>
		
		<div class="shadowBoxLine">

			<div style="margin:-40px 0 15px 10px;">
				<div style="float:left;">
					<h3 style="margin:4px 0 0 0;"><%= LanguageUtil.get(pageContext, "Top-Users") %></h3>
				</div>
				<div style="float:right;">
					<div id="topUserActions" style="display: none;">
						<button dojoType="dijit.form.Button" onClick="dijit.byId('mailingListPopup').show();" iconClass="mailListIcon">
							<%= LanguageUtil.get(pageContext, "Create Mailing List") %>
						</button>
						<button dojoType="dijit.form.Button" onClick="downloadList();" iconClass="downloadIcon">
							<%= LanguageUtil.get(pageContext, "Download-List") %>
						</button>
					</div>
				</div>
				<div class="clear">&nbsp;</div>
			</div>
			


			<div style="margin:0 20px;">
				<table class="listingTable">
					<thead>
						<tr>
							<th width="170"><%= LanguageUtil.get(pageContext, "Top-Users") %></th>
							<th width="70" style="text-align:center;"><%= LanguageUtil.get(pageContext, "Views") %></th>
							<th><%= LanguageUtil.get(pageContext, "Detail") %></th>
						</tr>
					</thead>
					<tbody id="topUsers">
					</tbody>
				</table>
			</div>
		</div>

	</div>
		
	<div class="yui-u">
			
		<div class="shadowBoxLine">
			<h3 style="margin:-35px 0 10px 10px;"><%= LanguageUtil.get(pageContext, "Traffic-Flow") %></h3>
			<div id="trafficSourcesPieWrapper">
				<div id="trafficSourcesPie" style="width: 98%; height: 250px;"></div>
			</div>
			<div id="legend2"></div>
			
			<div style="margin:0 20px;">
				<table class="listingTable">
					<tr>
						<th>Sources</th>
						<th style="text-align:center;" width="100"><%= LanguageUtil.get(pageContext, "Visits") %></th>
						<th style="text-align:center;" width="100">% <%= LanguageUtil.get(pageContext, "visits") %></th>
					</tr>
					<tr>
						<td><b>Search Engines</b></td>
						<td align="center"><span id="searchEngineVisits">...</span></td>
						<td align="center"><span id="searchEngineVisitsPercentage">...</span></td>
					</tr>
					<tr>
						<td><b>Referring Sites</b></td>
						<td align="center"><span id="referringSitesVisits">...</span></td>
						<td align="center"><span id="referringSitesVisitsPercentage">...</span></td>
					</tr>
					<tr>
						<td><b>Direct Traffic</b></td>
						<td align="center"><span id="directTrafficVisits">...</span></td>
						<td align="center"><span id="directTrafficVisitsPercentage">...</span></td>
					</tr>
				</table>
				
				<table class="listingTable">
					<thead>
						<tr>
							<th width="170"><%= LanguageUtil.get(pageContext, "Statistic") %></th>
							<th style="text-align:center;" width="70"><%= LanguageUtil.get(pageContext, "Total") %></th>
							<th><%= LanguageUtil.get(pageContext, "Detail") %></th>
						</tr>
					</thead>
					<tbody id="externalIncomingStatisticsTable">
						<tr>
							<td><b><%= LanguageUtil.get(pageContext, "External-Incoming") %></b></td>
							<td align="center" bgcolor="#eeeeee">0</td>
							<td>&nbsp;</td>
						</tr>					
					</tbody>
					<tbody id="internalReferersStatisticsTable">
						<tr>
							<td><b><%= LanguageUtil.get(pageContext, "Incoming") %></b></td>
							<td align="center" bgcolor="#eeeeee">0</td>
							<td>&nbsp;</td>
						</tr>					
					</tbody>
					<tbody id="internalOutgoingStatisticsTable">
						<tr>
							<td><b><%= LanguageUtil.get(pageContext, "Outgoing") %></b></td>
							<td align="center" bgcolor="#eeeeee">0</td>
							<td>&nbsp;</td>
						</tr>					
					</tbody>
				</table>
			</div>
		</div>
	</div>
</div>
</div>

<!-- END MAILING LIST POPUP -->

</liferay:box>
</form>

<!-- MAILING LIST POPUP -->

<div id="mailingListPopup" dojoType="dijit.Dialog" style="display: none; width:600px;">
	<h3 style="margin:-33px 0 10px 10px"><%= LanguageUtil.get(pageContext, "Mailing-List-Manipulation") %></h3>

	<p style="color:#990000;margin:20px 0 0 10px;font-weight:bold;">Create a new mailing list from the list of page visitors.</p>
	<form id="addNewMailingListForm" dojoType="dijit.form.Form">
	<dl>
		<dt><%= LanguageUtil.get(pageContext, "List Name") %>:</dt>
		<dd>
			<input type="hidden" name="allowPublicToSubscribe" id="allowPublicToSubscribe" size="20" value=""/>
			<input type="text" required="true" invalidMessage="<%= LanguageUtil.get(pageContext, "Required") %>" dojoType="dijit.form.ValidationTextBox" style="width:175px;" id="mailingListTitle" name="mailingListTitle" />
			<button dojoType="dijit.form.Button" onClick="saveNewMailingList()" iconClass="plusIcon">
	            <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Create-List")) %>
	        </button>
		</dd>
		<dd class="inputCaption">
			<input type="checkbox" dojoType="dijit.form.CheckBox" name="allowPublicToSubscribeChk" id="allowPublicToSubscribeChk"/>
			<%= LanguageUtil.get(pageContext, "Allow-Public-to-Subscribe") %>
		</dd>
	</dl>
	</form>
	
	
	<hr style="margin:20px 10px;" />
		
	<p style="color:#990000;margin:20px 0 0 10px;font-weight:bold;">Modify an existing mailing list from the list of page visitors.</p>
	<form id="modifyMailingListForm" dojoType="dijit.form.Form">
	<dl>
		<dt><%= LanguageUtil.get(pageContext, "Modify-Existing-List") %>:</dt>
		<dd>
			<select id="mailingListInode" dojoType="dijit.form.FilteringSelect" required="true" invalidMessage="<%= LanguageUtil.get(pageContext, "Required") %>" style="width:175px;">
				<option value=""></option>
				<% 
					Iterator it = mailingLists.iterator ();
					while (it.hasNext()) { 
						MailingList list = (MailingList)it.next();
				%>
					<option value="<%=list.getInode()%>"><%=list.getTitle().equals("Do Not Send List")? LanguageUtil.get(pageContext, "message.mailinglists.do_not_send_list_title"): list.getTitle() %></option>
				<% } %>
			</select>
		</dd>
		<dd>
			<button dojoType="dijit.form.Button" onClick="removeFromMailingList()" iconClass="deleteIcon">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Remove-From-List")) %>
			</button>
			
			<button dojoType="dijit.form.Button" onClick="addToMailingList()" iconClass="plusIcon">
				<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Append-To-List")) %>
			</button>
		</dd>
	</dl>
	</form>

</div>

<div id="saveFinishedDialog" dojoType="dijit.Dialog" title="<%= LanguageUtil.get(pageContext, "Saved") %>" style="display: none;">
	<div></div>
	<div class="buttonRow">
		<button dojoType="dijit.form.Button" type="button" onclick="goToMailingList()"><%= LanguageUtil.get(pageContext, "Go-To-Mailing-List") %></button>
		<button dojoType="dijit.form.Button" type="button" onclick="closeFinishedDialog()"><%= LanguageUtil.get(pageContext, "Go-Back-To-This-Page") %></button>
	</div>	
</div>
<!-- To show progress bar while fetching data  -->
<div id="fetchingDataDialog" dojoType="dijit.Dialog" title="<%= LanguageUtil.get(pageContext, "Loading") %> ..." style="display: none;">
	<div dojoType="dijit.ProgressBar" style="width:200px;text-align:center;" indeterminate="true" jsId="fetchProgress" id="fetchProgress"></div>
</div>
<script type="text/javascript">
	dojo.addOnLoad(function () {
		dojo.style(dijit.byId('fetchingDataDialog').closeButtonNode, 'visibility', 'hidden');
	});	
</script>