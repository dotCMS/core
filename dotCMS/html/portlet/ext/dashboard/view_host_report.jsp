<%@ include file="/html/portlet/ext/dashboard/init.jsp" %>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.dashboard.business.DashboardAPI"%>
<%@page import="com.dotmarketing.portlets.dashboard.model.TopAsset"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotmarketing.portlets.structure.factories.RelationshipFactory"%>
<%@page import="com.dotmarketing.portlets.structure.model.Relationship"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.business.*"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>



<%
String hostId = request.getParameter("hostId");
java.util.Map params = new java.util.HashMap();
params.put("struts_action",new String[] {"/ext/dashboard/view_dashboard"});
params.put("cmd",new String[] {Constants.VIEW_HOST_REPORT});
String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params)+"&hostId="+hostId;
DateFormat modDateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
modDateFormat.setTimeZone(timeZone);
DashboardAPI dashboardAPI = APILocator.getDashboardAPI();
Host currentHost = APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(),false);
request.getSession().setAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID, hostId);
Contentlet content = (Contentlet)currentHost;
PermissionAPI perAPI = APILocator.getPermissionAPI();
int userCount = perAPI.getUserCount(currentHost.getInode(), perAPI.PERMISSION_READ,"");
List<TopAsset> topAssets = dashboardAPI.getTopAssets(user, hostId);
java.text.NumberFormat numberFormat = java.text.NumberFormat.getInstance();
long totalPages = 0;
long totalContent = 0;
long totalFiles = 0;
for(TopAsset ta : topAssets){

	if(ta.getAssetType().equalsIgnoreCase("htmlpage")){
		totalPages = ta.getCount();
		
	}else if(ta.getAssetType().equalsIgnoreCase("contentlet")){
		totalContent = ta.getCount();
		
	}else if(ta.getAssetType().equalsIgnoreCase("file_asset")){
		totalFiles = ta.getCount();
		
	}
}
List<Relationship> relationships = new ArrayList<Relationship>();
if(content!=null){
  relationships.addAll(RelationshipFactory.getRelationshipsByParent(content.getStructure()));
  relationships.addAll(RelationshipFactory.getRelationshipsByChild(content.getStructure()));
}
LanguageAPI langAPI = APILocator.getLanguageAPI();
List<Language> langs = langAPI.getLanguages();
Language defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();

List<Layout> layoutList=null;
String userURL ="";
String pagesURL ="";
String filesURL ="";
String contentURL ="";
try {	
	layoutList=APILocator.getLayoutAPI().findAllLayouts();
	 for(Layout layoutObj:layoutList) {
		List<String> portletIdsForLayout=layoutObj.getPortletIds();
		for(String portletId : portletIdsForLayout){
		if (portletId.equals("EXT_USER_ADMIN")) {
			userURL = "/c/portal/layout?p_l_id=" + layoutObj.getId() +"&p_p_id=EXT_USER_ADMIN&p_p_action=0";
		}else if (portletId.equals("EXT_15")) {
			pagesURL = "/c/portal/layout?p_l_id=" + layoutObj.getId() +"&p_p_id=EXT_15&p_p_action=0";
		}else if (portletId.equals("EXT_3")) {
			filesURL = "/c/portal/layout?p_l_id=" + layoutObj.getId() +"&p_p_id=EXT_3&p_p_action=0";
		}else if (portletId.equals("EXT_11")) {
			contentURL = "/c/portal/layout?p_l_id=" + layoutObj.getId() +"&p_p_id=EXT_11";
		}
	  }
	}
} catch (Exception e) {}	 
%>



<script type='text/javascript' src='/dwr/interface/DashboardAjax.js'></script>
<script language="Javascript">

	dojo.require("dojox.charting.Chart2D");
	dojo.require("dojox.charting.themes.PlotKit.blue");
	dojo.require("dojox.charting.widget.Legend");
    dojo.require("dojox.charting.plot2d.Pie");
    dojo.require("dojox.charting.action2d.Highlight");
    dojo.require("dojox.charting.action2d.MoveSlice");
    dojo.require("dojox.charting.action2d.Tooltip");
	
	dojo.require("dojo.date.locale");
	dojo.require("dojo.number");	
	dojo.require("dojox.charting.action2d.Magnify");
	dojo.require("dijit.Tooltip");

    var view = 'day';

    var maxCount = 10;

    var contentPageNumber = 1;
    var pagesPageNumber = 1;
    var refererPageNumber = 1;
    var summary404PageNumber = 1;

    var globalOrderBy = '';
    var globalOrderByDir = '';

	function order(orderBy, dir, type){
	   	if(globalOrderBy==''){
	   		globalOrderBy = orderBy;
	   		globalOrderByDir = dir;
	   	}else{
	   	   	if(globalOrderBy == orderBy){
	   	   	   	if(globalOrderByDir == 'desc'){
	   	        	dir = 'asc';
	   	   	   	}else{
	   	   	        dir = 'desc';
	   	   	   	}
	   	   	}
	   	    globalOrderBy = orderBy;
	   	    globalOrderByDir = dir;
	   	}

	   	if(type=='pages'){
	   		viewTopPages(pagesPageNumber,orderBy + ' ' + dir);
	   	}else if(type=='content'){
	   		viewTopContent(contentPageNumber, orderBy + ' ' + dir);
	   	}else if(type=='referers'){
	   		viewTopReferers(refererPageNumber, orderBy + ' ' + dir);
	   	}else if(type=='404'){
	   		view404(summary404PageNumber, orderBy + ' ' + dir);
	   	}
	  
	}
    

    function setStatsView(viewType){
        var showIgnored = dijit.byId('showIgnoredCheckbox').checked?true:false;
        view = viewType;
        var dayButton = "<%=LanguageUtil.get(pageContext, "Day")%>";
        var weekButton = "<%=LanguageUtil.get(pageContext, "Week")%>";
        var monthButton ="<%=LanguageUtil.get(pageContext, "Month")%>";
        if(viewType == 'day'){
			dijit.byId('dayButton').setAttribute("disabled", true);
			dijit.byId('dayButton').attr('iconClass','calDayIconDis');
			dijit.byId("weekButton").setAttribute("disabled", false);
			dijit.byId('weekButton').attr('iconClass','calWeekIcon');
			dijit.byId("monthButton").setAttribute("disabled", false);
			dijit.byId('monthButton').attr('iconClass','calMonthIcon');
        }else if(viewType=='week'){
        	dijit.byId('dayButton').setAttribute("disabled", false);
			dijit.byId('dayButton').attr('iconClass','calDayIcon');
			dijit.byId("weekButton").setAttribute("disabled", true);
			dijit.byId('weekButton').attr('iconClass','calWeekIconDis');
			dijit.byId("monthButton").setAttribute("disabled", false);
			dijit.byId('monthButton').attr('iconClass','calMonthIcon');
        }else if(viewType=='month'){
        	dijit.byId('dayButton').setAttribute("disabled", false);
			dijit.byId('dayButton').attr('iconClass','calDayIcon');
			dijit.byId("weekButton").setAttribute("disabled", false);
			dijit.byId('weekButton').attr('iconClass','calWeekIcon');
			dijit.byId("monthButton").setAttribute("disabled", true);
			dijit.byId('monthButton').attr('iconClass','calMonthIconDis');
        }
        dojo.place('<div style="text-align:center;height:140px;padding-top:60px;"><img src="/html/images/icons/round-progress-bar.gif" /><br/><b><%= LanguageUtil.get(pageContext, "Loading") %>...</b>', 'lineWrapper', 'only');
    	DashboardAjax.getHostStatistics('<%=hostId%>', viewType, showIgnored, dojo.hitch(this, fillStatistics));
    }

    function refresh404List(){
    	var showIgnored = dijit.byId('showIgnoredCheckbox').checked?true:false;
    	DashboardAjax.getSummary404s('<%=hostId%>', view, showIgnored, 5, 0, '',  dojo.hitch(this, fillsummary404sTable));
    }

    function setIgnore(summaryId, ignore){
        	 var tbody = dojo.byId('summary404s');
        	 var animArgs = {node: "summary404"+summaryId,duration: 1000, delay: 50,onEnd:function(node){
                	             DashboardAjax.setIgnore(summaryId, ignore, dojo.hitch(this, refresh404List));
            	               }
                            };
             dojo.fadeOut(animArgs).play();	     
    }



    function createLineChart(data){

      var visits = data.summaryVisits;
		if(visits.length ==0){
			dojo.place('<div id="lineChart" class="noChart" style="opacity:0;"><span><%=LanguageUtil.get(pageContext, "Not-enough-data-to-create-chart")%></span></div>', 'lineWrapper', 'only');
			dojo.fadeIn({node:dojo.byId("lineChart"),duration:2500}).play();
			return;
		}
		
		
       	try{
	     	var dataArr = [];
			for(var i = 0; i < visits.length; i++) {
				var visitData = visits[i].visits;
				dataArr.push(visitData);
			 }
	
	    	 dojo.place('<div id="lineChart" style="width: 100%; height: 200px;"></div>', 'lineWrapper', 'only');		
	    	 var lineChart = new dojox.charting.Chart2D("lineChart");
	         lineChart.addPlot("default", {type: "Lines", markers: true,
	             tension:"S", shadows: {dx: 2, dy: 2, dw: 2}});
	         lineChart.addPlot("other", {type: "Areas", vAxis: "other y", tension:"S"});
	         lineChart.addPlot("Grid", {type: "Grid",
	             hAxis: "x",
	             vAxis: "y",
	             hMajorLines: true,     
	             hMinorLines: false,     
	             vMajorLines: true,     
	             vMinorLines: false  
	          });
	
	          var labelsArr = [];
	
	         
	    	  for(var i = 0; i < visits.length; i++) {
	    	    var visit = visits[i];
	    	    labelsArr.push({value: i + 1, text: visit.formattedTime});
	    	  }
	         lineChart.addAxis("x", {labels: labelsArr});
	         lineChart.addAxis("y", {vertical: true, fixUpper: "major", fixLower:"minor", includeZero: false,
	             labelFunc : function(o) {
	                   return dojo.number.format(o);
	                 }
	               });
	         lineChart.addAxis("other y", {vertical: true, leftBottom: false, fixUpper: "major", fixLower:"minor", includeZero: false,
	                           labelFunc : function(o) {
	                             return dojo.number.format(o);
	                          }
	               });
	         lineChart.addSeries("Series 1", dataArr);
	         lineChart.addSeries("Series 2", dataArr,
	                 {plot: "other", stroke: {color:"blue"}, fill: "lightblue"});
	         var tip = new dojox.charting.action2d.Tooltip(lineChart, "default", {
	        	    text : function(o) {
	        	        return (dojo.number.format(o.y));
	        	    }
	        	});
	         var magnify = new dojox.charting.action2d.Magnify(lineChart, "default");
	         lineChart.render();	
    	}
       	catch(e){
       		dojo.place('<div id="lineChart" class="noChart" style="opacity:0;"><span><%=LanguageUtil.get(pageContext, "Not-enough-data-to-create-chart")%></span></div>', 'lineWrapper', 'only');
        }			
	}


    function createPieChart(data){

    	var referringSitesData = data.referringSites;
    	var directTrafficData = data.directTraffic;
    	var searchEnginesData = data.searchEngines;
		
    	try{
    		var totaltraffic = 0;
        	var referringSitesPercentage = 0;
        	var directTrafficPercentage = 0;
        	var searchEnginesPercentage = 0; 	

       	 totaltraffic = referringSitesData + directTrafficData + searchEnginesData;
    	 if(totaltraffic > 0){  
    	   referringSitesPercentage = ((referringSitesData/totaltraffic)*100).toFixed(2);
    	   directTrafficPercentage = ((directTrafficData/totaltraffic)*100).toFixed(2);
    	   searchEnginesPercentage = ((searchEnginesData/totaltraffic)*100).toFixed(2);
    	 }else{
			dojo.place('<div id="pieChart" class="noPie" style="opacity:0;"><span><%=LanguageUtil.get(pageContext, "Not-enough-data-to-create-chart")%></span></div>', 'pieWrapper', 'only');		       
			dojo.fadeIn({node:dojo.byId("pieChart"),duration:2500}).play();
			return;
		 }

    	 dojo.place('<div id="pieChart" style="width:190px; height:190px;"></div>', 'pieWrapper', 'only');	
    	 var dc = dojox.charting;
         var pieChart = new dojox.charting.Chart2D("pieChart");
         pieChart.addPlot("default", {type: "Pie", font: "normal normal 8pt Tahoma",
             fontColor: "black",
             labelOffset: -20,
             radius: 65});

         var series = [];
         if(referringSitesData>0){
        	 series.push({y: referringSitesData,
            	         legend: "<b>Referring Sites</b><br /> "+  dojo.number.format(referringSitesData) + " (" + referringSitesPercentage + '%)' ,
            	         color: "green",
            	         tooltip: referringSitesPercentage + '%'})
         }
         if(searchEnginesData>0){
        	 series.push({y: searchEnginesData,
        		         legend: "<b>Search Engines</b> <br />" +  dojo.number.format(searchEnginesData) + " (" + searchEnginesPercentage + '%)',
            	         color: "red",
            	         tooltip: referringSitesPercentage + '%'})
         }
         if(directTrafficData>0){
        	 series.push({y: directTrafficData,
        		         legend: "<b>Direct Traffic</b> <br />" +  dojo.number.format(directTrafficData) +" (" + directTrafficPercentage + '%)' ,
        		         color: "blue",
        	             tooltip: directTrafficPercentage + '%'})
         }
         pieChart.addSeries("Series A", series);
         var anim_a = new dc.action2d.MoveSlice(pieChart, "default");
         var anim_b = new dc.action2d.Highlight(pieChart, "default");
         var anim_c = new dc.action2d.Tooltip(pieChart, "default");
         pieChart.render();

         
         dijit.registry.remove('pieChartLegend');
         var legend = new dojox.charting.widget.Legend({
             chart: pieChart, horizontal:false
         },
         "pieChartLegend");
         
    	}catch(e){
        	dojo.place('<div id="pieChart" style="width:190px; height:190px;"></div>', 'pieWrapper', 'only');	
        }
    }

    function fillStatistics(data) {
		fillUsageStatistics(data);
		fillTopContentTable(data);
		fillTopPagesTable(data);
		fillTopReferersTable(data);
		fillsummary404sTable(data);
	}

	function fillUsageStatistics(data){
		dojo.byId('visits').innerHTML = data.visits;
		dojo.byId('uniqueVisitors').innerHTML = data.uniqueVisits;
		dojo.byId('newVisits').innerHTML = data.newVisits;		
		dojo.byId('bounceRate').innerHTML = data.bounceRate.toFixed(2) + "%";
		dojo.byId('pageViews').innerHTML = data.pageViews;
		dojo.byId('timeOnSite').innerHTML = data.timeOnSite;
		createPieChart(data);
		createLineChart(data);
	}



	var noRecordsTemplate = '<tr class="alternate_1" id="rowNoResults"><td colspan="5"><div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "No-Records-Found") %></div></td></tr>';
		 
	var topContentTemplate = '<tr class="${className}"><td><div class="urlWrapper" onmouseover="tip(this)"><span><a href="${uri}" target="_blank">${title}</a></span></div></td><td width="70" align="center">${hits}</td></tr>';
	   
	function fillTopContentTable(data){

		DWRUtil.removeAllRows(dojo.byId('topContent'));
		var topContent = data.topContent;
		if(topContent.length == 0) {
			dojo.place(dojo.string.substitute(noRecordsTemplate, { }), 'topContent', 'last');
		} else {
			var tableHTML = "";
			for(var i = 0; i < topContent.length; i++) {
				var content = topContent[i];
				var trClassName = (i%2==0)?'alternate_1':'alternate_2';
				var html = dojo.string.substitute(topContentTemplate, { className:trClassName, uri:content.uri,  title: content.title, hits: content.hits });
				tableHTML += html;
				
			}
			dojo.place(tableHTML, 'topContent', 'last');
		}
		
	}

	var topPagesTemplate = '<tr class="${className}">\
		<td><div class="urlWrapper" onmouseover="tip(this)"><span>${uri}</span</div></td>\
		<td width="75" align="center">${hits}</td>\
	   </tr>';

	function fillTopPagesTable(data){

		DWRUtil.removeAllRows(dojo.byId('topPages'));
		var topPages = data.topPages;
		if(topPages.length == 0) {
			dojo.place(dojo.string.substitute(noRecordsTemplate, { }), 'topPages', 'last');
		} else {
			var tableHTML = "";
			for(var i = 0; i < topPages.length; i++) {
				var page = topPages[i];
				var trClassName = (i%2==0)?'alternate_1':'alternate_2';
				var html = dojo.string.substitute(topPagesTemplate, { className:trClassName,  uri: page.uri, hits: page.hits });
				tableHTML += html;	
			}
			dojo.place(tableHTML, 'topPages', 'last');
		}
		
	}

	var topRefererTemplate = '<tr class="${className}">\
		<td><div class="urlWrapper" onmouseover="tip(this)"><span>${uri}</span></div></td>\
		<td width="75" align="center">${hits}</td>\
	   </tr>';

	function fillTopReferersTable(data){

          
		DWRUtil.removeAllRows(dojo.byId('topReferers'));
		var topReferers = data.topReferers;
		if(topReferers.length == 0) {
			dojo.place(dojo.string.substitute(noRecordsTemplate, { }), 'topReferers', 'last');
		} else {
			var tableHTML = "";
			for(var i = 0; i < topReferers.length; i++) {
				var referer = topReferers[i];
				var trClassName = (i%2==0)?'alternate_1':'alternate_2';
				var html = dojo.string.substitute(topRefererTemplate, { className:trClassName,  uri: referer.uri, hits: referer.hits });
				tableHTML += html;	
			}
			dojo.place(tableHTML, 'topReferers', 'last');
	
		}
		
	}

	var summary404sTemplate = '<tr id="summary404${id}" class="${className}">\
		<td><div class="urlWrapper" onmouseover="tip(this)"><span>${uri}</span</div></td>\
		<td><div class="urlWrapper" onmouseover="tip(this)"><span>${referer}</span></div></td>\
		<td align="center">${ignored}</td>\
	   </tr>';

	function fillsummary404sTable(data){

		DWRUtil.removeAllRows(dojo.byId('summary404s'));
		var summary404s = data.summary404s;
		if(summary404s.length == 0) {
			dojo.place(dojo.string.substitute(noRecordsTemplate, { }), 'summary404s', 'last');
		} else {
			var tableHTML = "";
			for(var i = 0; i < summary404s.length; i++) {
				var summary = summary404s[i];
				var trClassName = (i%2==0)?'alternate_1':'alternate_2';
				var deleteIconTemplate = '<a href="javascript:setIgnore(\'${id}\',true);"><span class="deleteIcon"></span></a>';
                var deleteIcon =  dojo.string.substitute(deleteIconTemplate, {id: summary.id});
				if(summary.ignored=='true'){
					deleteIconTemplate = '<a href="javascript:setIgnore(\'${id}\',false);"><span class="resolveIcon"></span></a>';
	                deleteIcon =  dojo.string.substitute(deleteIconTemplate, {id: summary.id});
				}
				var html = dojo.string.substitute(summary404sTemplate, { className:trClassName,  id:summary.id, uri: summary.uri, referer: summary.referer, ignored: deleteIcon});
				tableHTML += html;	
			}
			dojo.place(tableHTML, 'summary404s', 'last');
		}
		
	}

	function donwloadToExcel(trending){

		var href = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
		href += "<portlet:param name='struts_action' value='/ext/dashboard/view_dashboard' />";
		href += "<portlet:param name='cmd' value='export' />";		
		href += "<portlet:param name='referer' value='<%=java.net.URLDecoder.decode(referer, "UTF-8")%>' />";		
		href += "</portlet:actionURL>";
		href += "&hostId=<%=hostId%>&trending="+trending+"&viewType="+view;
		window.location.href=href;	

	}

	function fillTopContentTablePaginated(data){

		DWRUtil.removeAllRows(dojo.byId('summaryList'));
		var pageNumber = data.pageNumber;
		var orderBy = "'" + data.orderBy + "'";
		if(orderBy==''){
           orderBy = "'sum(summaryContent.hits) desc'";
		}
		var minIndex = (pageNumber - 1) * maxCount;
        var totalCount = data.summaryCount;
        var maxIndex = maxCount * pageNumber;
        if((minIndex + maxCount) >= totalCount){
        	maxIndex = totalCount;
        }
        var leftPageStr = '';
        if (minIndex != 0) { 
            leftPageStr += '<a href="javascript:viewTopContent('+(pageNumber-1)+','+orderBy+')"> << </a>'; 
	    } 
        var rightPageStr = '';
        if (maxIndex < totalCount) { 
        	rightPageStr += '<a href="javascript:viewTopContent('+(pageNumber + 1)+','+orderBy+')"> >> </a>';  
	    } 
		var index = 0;
		if(totalCount>0){
			index = (minIndex+1);
		}
		var str = '';
		if(leftPageStr!=''){
			str += leftPageStr + ' ';
		}
		str += '<%= LanguageUtil.get(pageContext, "Viewing") %>';
		str += ' ' + index + ' - '; 
		if (maxIndex > (minIndex + totalCount)) { 
	    	str +=' ' + (minIndex + totalCount); 
		}else{ 
			str += ' ' + (maxIndex); 
		} 
		str += ' ' + '<%= LanguageUtil.get(pageContext, "of1") %>'+ ' '  + totalCount;
		if(rightPageStr!=''){
			str +=  ' ' + rightPageStr;
		}
		
		var footer =  '<a align="center" href="javascript:donwloadToExcel(\'content\');"><%= LanguageUtil.get(pageContext, "Export") %></a><a align="center" href="javascript:donwloadToExcel(\'content\');"><img src="/icon?i=csv.xls" border="0" alt="export results" align="absbottom"></a></div><br />'+str;
		var title = '<b><%= LanguageUtil.get(pageContext, "Top-Content") %></b>';
		var header  ='<tr><th nowrap width="50%"><a href="javascript:order(\'title\',\'desc\',\'content\');"><%= LanguageUtil.get(pageContext, "Title") %></a></th><th nowrap width="50%"><a href="javascript:order(\'sum(summaryContent.hits)\',\'desc\',\'content\');"><%= LanguageUtil.get(pageContext, "Hits") %></a> </th></tr>';
		dojo.place(footer, 'footer', 'only');
		dojo.place(title, 'listTitle', 'only');
		dojo.place(header, 'listingTableHeader', 'only');
		var topContent = data.topContent;
		if(topContent.length == 0) {
			dojo.place(dojo.string.substitute(noRecordsTemplate, { }), 'summaryList', 'last');
		} else {
			var tableHTML = "";
			for(var i = 0; i < topContent.length; i++) {
				var content = topContent[i];
				var trClassName = (i%2==0)?'alternate_1':'alternate_2';
				var html = dojo.string.substitute(topContentTemplate, { className:trClassName,  uri:content.uri, title: content.title, hits: content.hits });
				tableHTML += html;	
			}
			dojo.place(tableHTML, 'summaryList', 'last');
		}
		dijit.byId('listingWrapper').show();
		
	}


	function viewTopContent(pageNumber, orderBy){
		contentPageNumber = pageNumber;
		DashboardAjax.getTopContent('<%=hostId%>', view, maxCount, pageNumber, orderBy,  dojo.hitch(this, fillTopContentTablePaginated));
	}

	function fillTopPagesTablePaginated(data){


		DWRUtil.removeAllRows(dojo.byId('summaryList'));
		var pageNumber = data.pageNumber;
		var orderBy = "'" + data.orderBy + "'";
		if(orderBy==''){
           orderBy = "'sum(summaryPage.hits) desc'";
		}
		var minIndex = (pageNumber - 1) * maxCount;
        var totalCount = data.summaryCount;
        var maxIndex = maxCount * pageNumber;
        if((minIndex + maxCount) >= totalCount){
        	maxIndex = totalCount;
        }
        var leftPageStr = '';
        if (minIndex != 0) { 
            leftPageStr += '<a href="javascript:viewTopPages('+(pageNumber-1)+','+orderBy+')"> << </a>'; 
	    } 
        var rightPageStr = '';
        if (maxIndex < totalCount) { 
        	rightPageStr += '<a href="javascript:viewTopPages('+(pageNumber + 1)+','+orderBy+')"> >> </a>';  
	    } 
		var index = 0;
		if(totalCount>0){
			index = (minIndex+1);
		}
		var str = '';
		if(leftPageStr!=''){
			str += leftPageStr + ' ';
		}
		str += '<%= LanguageUtil.get(pageContext, "Viewing") %>';
		str += ' ' + index + ' - '; 
		if (maxIndex > (minIndex + totalCount)) { 
	    	str +=' ' + (minIndex + totalCount); 
		}else{ 
			str += ' ' + (maxIndex); 
		} 
		str += ' ' + '<%= LanguageUtil.get(pageContext, "of1") %>'+ ' '  + totalCount;
		if(rightPageStr!=''){
			str +=  ' ' + rightPageStr;
		}
		var footer =  '<a align="center" href="javascript:donwloadToExcel(\'pages\');"><%= LanguageUtil.get(pageContext, "Export") %></a><a align="center" href="javascript:donwloadToExcel(\'pages\');"><img src="/icon?i=csv.xls" border="0" alt="export results" align="absbottom"></a></div><br />'+str;
		var title = '<b><%= LanguageUtil.get(pageContext, "Top-Pages") %></b>';
		var header  = '<tr><th nowrap width="50%"><a href="javascript:order(\'uri\',\'desc\',\'pages\');"><%= LanguageUtil.get(pageContext, "Uri") %></a></th><th nowrap width="50%"><a href="javascript:order(\'sum(summaryPage.hits)\',\'desc\',\'pages\');"><%= LanguageUtil.get(pageContext, "Hits") %></a> </th></tr>';
		dojo.place(footer, 'footer', 'only');
		dojo.place(title, 'listTitle', 'only');
		dojo.place(header, 'listingTableHeader', 'only');
		var topPages = data.topPages;
		if(topPages.length == 0) {
			dojo.place(dojo.string.substitute(noRecordsTemplate, { }), 'summaryList', 'last');
		} else {
			var tableHTML = "";
			for(var i = 0; i < topPages.length; i++) {
				var page = topPages[i];
				var trClassName = (i%2==0)?'alternate_1':'alternate_2';
				var html = dojo.string.substitute(topPagesTemplate, { className:trClassName, uri: page.uri, hits: page.hits });
				tableHTML += html;	
			}
			dojo.place(tableHTML, 'summaryList', 'last');
		}
		dijit.byId('listingWrapper').show();
		
	}

	function viewTopPages(pageNumber, orderBy){
		pagesPageNumber = pageNumber;
		DashboardAjax.getTopPages('<%=hostId%>', view, maxCount, pageNumber, orderBy,  dojo.hitch(this, fillTopPagesTablePaginated));
	}


	function fillReferersTablePaginated(data){


		DWRUtil.removeAllRows(dojo.byId('summaryList'));
		var pageNumber = data.pageNumber;
		var orderBy = "'" + data.orderBy + "'";
		if(orderBy==''){
           orderBy = "'sum(summaryRef.hits) desc'";
		}
		var minIndex = (pageNumber - 1) * maxCount;
        var totalCount = data.summaryCount;
        var maxIndex = maxCount * pageNumber;
        if((minIndex + maxCount) >= totalCount){
        	maxIndex = totalCount;
        }
        var leftPageStr = '';
        if (minIndex != 0) { 
            leftPageStr += '<a href="javascript:viewTopReferers('+(pageNumber-1)+','+orderBy+')"> << </a>'; 
	    } 
        var rightPageStr = '';
        if (maxIndex < totalCount) { 
        	rightPageStr += '<a href="javascript:viewTopReferers('+(pageNumber + 1)+','+orderBy+')"> >> </a>';  
	    } 
		var index = 0;
		if(totalCount>0){
			index = (minIndex+1);
		}
		var str = '';
		if(leftPageStr!=''){
			str += leftPageStr + ' ';
		}
		str += '<%= LanguageUtil.get(pageContext, "Viewing") %>';
		str += ' ' + index + ' - '; 
		if (maxIndex > (minIndex + totalCount)) { 
	    	str +=' ' + (minIndex + totalCount); 
		}else{ 
			str += ' ' + (maxIndex); 
		} 
		str += ' ' + '<%= LanguageUtil.get(pageContext, "of1") %>'+ ' '  + totalCount;
		if(rightPageStr!=''){
			str +=  ' ' + rightPageStr;
		}
		var footer = '<a align="center" href="javascript:donwloadToExcel(\'referers\');"><%= LanguageUtil.get(pageContext, "Export") %></a><a align="center" href="javascript:donwloadToExcel(\'referers\');"><img src="/icon?i=csv.xls" border="0" alt="export results" align="absbottom"></a></div><br />'+str;
		var title = '<b><%= LanguageUtil.get(pageContext, "Top-Referers") %></b>';
		var header ='<tr><th nowrap width="50%"><a href="javascript:order(\'uri\',\'desc\',\'referers\');"><%= LanguageUtil.get(pageContext, "Referer") %></a></th><th nowrap width="50%"><a href="javascript:order(\'sum(summaryRef.hits)\',\'desc\',\'referers\');"><%= LanguageUtil.get(pageContext, "Hits") %></a> </th></tr>';
		dojo.place(footer, 'footer', 'only');
		dojo.place(title, 'listTitle', 'only');
		dojo.place(header, 'listingTableHeader', 'only');
		var referers = data.topReferers;
		if(referers.length == 0) {
			dojo.place(dojo.string.substitute(noRecordsTemplate, { }), 'summaryList', 'last');
		} else {
			var tableHTML = "";
			for(var i = 0; i < referers.length; i++) {
				var referer = referers[i];
				var trClassName = (i%2==0)?'alternate_1':'alternate_2';
				var html = dojo.string.substitute(topRefererTemplate, { className:trClassName,  uri: referer.uri, hits: referer.hits });
				tableHTML += html;	
			}
			dojo.place(tableHTML, 'summaryList', 'last');
		}
		dijit.byId('listingWrapper').show();
		
	}

	function viewTopReferers(pageNumber, orderBy){
		refererPageNumber = pageNumber;
		DashboardAjax.getTopReferers('<%=hostId%>', view, maxCount, pageNumber, orderBy,  dojo.hitch(this, fillReferersTablePaginated));
	}

	function fillsummary404sTablePaginated(data){


		DWRUtil.removeAllRows(dojo.byId('summaryList'));
		var pageNumber = data.pageNumber;
		var orderBy = "'" + data.orderBy + "'";
		if(orderBy==''){
           orderBy = "'uri'";
		}
		var minIndex = (pageNumber - 1) * maxCount;
        var totalCount = data.summaryCount;
        var maxIndex = maxCount * pageNumber;
        if((minIndex + maxCount) >= totalCount){
        	maxIndex = totalCount;
        }
        var leftPageStr = '';
        if (minIndex != 0) { 
            leftPageStr += '<a href="javascript:view404('+(pageNumber-1)+','+orderBy+')"> << </a>'; 
	    } 
        var rightPageStr = '';
        if (maxIndex < totalCount) { 
        	rightPageStr += '<a href="javascript:view404('+(pageNumber + 1)+','+orderBy+')"> >> </a>';  
	    } 
		var index = 0;
		if(totalCount>0){
			index = (minIndex+1);
		}
		var str = '';
		if(leftPageStr!=''){
		   str += leftPageStr + ' ';
		}
		str += '<%= LanguageUtil.get(pageContext, "Viewing") %>';
		str += ' ' + index + ' - '; 
		if (maxIndex > (minIndex + totalCount)) { 
	    	str +=' ' + (minIndex + totalCount); 
		}else{ 
			str += ' ' + (maxIndex); 
		} 
		str += ' ' + '<%= LanguageUtil.get(pageContext, "of1") %>'+ ' '  + totalCount;
		if(rightPageStr!=''){
        	str +=  ' ' + rightPageStr;
		}
		var footer = '<a align="center" href="javascript:donwloadToExcel(\'404\');"><%= LanguageUtil.get(pageContext, "Export") %></a><a align="center" href="javascript:donwloadToExcel(\'404\');"><img src="/icon?i=csv.xls" border="0" alt="export results" align="absbottom"></a></div><br />'+str;
		var title = '<b><%= LanguageUtil.get(pageContext, "404") %></b>';
		var header ='<tr><th nowrap width="40%"><a href="javascript:order(\'uri\',\'desc\',\'404\');"><%= LanguageUtil.get(pageContext, "Page/File") %></a></th><th nowrap width="50%"><a href="javascript:order(\'referer_uri\',\'desc\',\'404\');"><%= LanguageUtil.get(pageContext, "Referer") %></a> </th><th nowrap width="10%"><a href="javascript:order(\'userPreferences.ignored\',\'desc\',\'404\');"><%= LanguageUtil.get(pageContext, "Ignored") %></a></th></tr>';
		dojo.place(footer, 'footer', 'only');
		dojo.place(title, 'listTitle', 'only');
		dojo.place(header, 'listingTableHeader', 'only');
		var summary404s = data.summary404s;
		if(summary404s.length == 0) {
			dojo.place(dojo.string.substitute(noRecordsTemplate, { }), 'summaryList', 'last');
		} else {
			var tableHTML = "";
			for(var i = 0; i < summary404s.length; i++) {
				var summary = summary404s[i];
				var trClassName = (i%2==0)?'alternate_1':'alternate_2';
				var html = dojo.string.substitute(summary404sTemplate, { className:trClassName,  id: summary.id, uri: summary.uri, referer: summary.referer, ignored: summary.ignored});
				tableHTML += html;	
			}
			DWRUtil.removeAllRows(dojo.byId('summaryList'));
			dojo.place(tableHTML, 'summaryList', 'last');
		}
		dijit.byId('listingWrapper').show();
		
	}
	
	function view404(pageNumber, orderBy){
		summary404PageNumber = pageNumber;
		var showIgnored = dijit.byId('showIgnoredCheckbox').checked?true:false;
    	DashboardAjax.getSummary404s('<%=hostId%>', view, showIgnored, maxCount, pageNumber, orderBy,  dojo.hitch(this, fillsummary404sTablePaginated));
	}

	function viewPages(){
		var URL = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">\
		          <portlet:param name="struts_action" value="/ext/htmlpages/view_htmlpages" />\
			      </portlet:actionURL>&hostId={hostIdentifier}';
		var href = dojo.replace(URL, { hostIdentifier: '<%=hostId%>'})
	    window.location=href;	
	}

	function viewPages(){
		var URL = '<%=pagesURL%>&hostId={hostIdentifier}';
		var href = dojo.replace(URL, { hostIdentifier: '<%=hostId%>'})
	    window.location=href;	
	}

	function viewFiles(){
	    var URL = '<%=filesURL%>&hostId={hostIdentifier}';
	    var href = dojo.replace(URL, { hostIdentifier: '<%=hostId%>'})
	    window.location=href;	
	}

	function viewUsers(){
	     var URL = '<%=userURL%>&hostId={hostIdentifier}';
	     var href = dojo.replace(URL, { hostIdentifier: '<%=hostId%>'})
		 window.location=href;	
					 
	}

	function viewContent(){
		var URL = '<%=contentURL%>&p_p_action=0&hostId={hostIdentifier}';
	    var href = dojo.replace(URL, { hostIdentifier: '<%=hostId%>'})
		window.location=href;	
	}

	function editContent(contInode,structureInode){
		 var URL = '<%=contentURL%>&p_p_action=1&p_p_state=maximized&p_p_mode=view&_EXT_11_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet&_EXT_11_cmd=edit&_EXT_11_selectedStructure={stInode}&inode={inode}&referer=<%=referer%>';
	     var href = dojo.replace(URL, { stInode:structureInode, inode: contInode})
		 window.location=href;	
	}

	function tip(el){
	    new dijit.Tooltip({
            connectId: [el],
            position: "above",
            label: "<b>"+el.innerHTML+"</b>"
        });
	}
	

	<% if(LicenseUtil.getLevel() > 199){ %>	
	dojo.addOnLoad(function(){
		setStatsView('month');
	});
	<%}%>
	
	 function viewWorkStream(id){
			var URL = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">\
			<portlet:param name="struts_action" value="/ext/dashboard/view_dashboard" />\
			<portlet:param name="cmd" value="<%=Constants.VIEW_ACTIVITY_STREAM %>" />\
			<portlet:param name="lang" value="<%= Long.toString(APILocator.getLanguageAPI().getDefaultLanguage().getId()) %>" />\
			<portlet:param name="referer" value="<%=referer%>" />\
			</portlet:actionURL>&hostId={hostIdentifier}';
			var href = dojo.replace(URL, { hostIdentifier: id})
			window.location=href;	
		 }
	
</script>

<style>
	.listingTable{margin-bottom:10px;}
	.urlWrapper{position:relative;}
	.urlWrapper span{display:block;position:absolute;top:-9px;left:0;width:95%;overflow:hidden;white-space:nowrap;cursor:pointer;}
</style>
<% if(LicenseUtil.getLevel() > 199){ %>	
<form id="fm" method="post">
<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= LanguageUtil.get(pageContext, "view-dashboard") %>' />

<input name="<portlet:namespace />referer" type="hidden" value="<%=referer%>">

<div class="buttonBoxRight" id="viewControle">
	<button id="dayButton" iconClass="calDayIcon" dojoType="dijit.form.Button"  onClick="setStatsView('day');"><%=LanguageUtil.get(pageContext, "Day")%></button>
	<button id="weekButton" iconClass="calWeekIcon" dojoType="dijit.form.Button"  onClick="setStatsView('week');"><%=LanguageUtil.get(pageContext, "Week")%></button>
	<button id="monthButton" iconClass="calMonthIcon" dojoType="dijit.form.Button"  onClick="setStatsView('month');"><%=LanguageUtil.get(pageContext, "Month")%></button> 
</div>
			

			
<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">

<!-- Statistics Tabs -->
<div id="hostStatisticsTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Statistics") %>">
	<div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" id="borderContainer" style="height:1000px;">
	
	<!-- START Left Column Stats -->
		<div dojoType="dijit.layout.ContentPane" splitter="true" region="center">
		
		
			<div style="margin:0 10px 0 0;">
			<div class="dijitLayoutContainer roundBox" style="margin-bottom:10px;">
					<div style="float:right">
						<button id="workstreamBtn" iconClass="workStreamIcon" dojoType="dijit.form.Button"  onclick="viewWorkStream('<%=content.getIdentifier()%>');">
							<%=LanguageUtil.get(pageContext, "Work-Stream")%>
						</button> 
					</div>
				<div style="margin:5px 10px 15px 10px;"><b><%=content.get("title") %></b></div>

				<!-- Graph -->
				<div id="lineWrapper" style="border:0px solid #ccc;margin-bottom:10px;"></div> 
				
				<!-- Overview -->
				<div class="yui-gb">
					<div class="yui-u first">
						<table class="listingTable">
							<tr>
								<td width="15%" bgcolor="#ECECEC"><span><b><%= LanguageUtil.get(pageContext, "Visits") %></b></span></td>
								<td width="15%" id="visits"> </td>
							</tr>
							<tr>
								<td bgcolor="#ECECEC"><span><b><%= LanguageUtil.get(pageContext, "Page-Views") %></b></span></td>
								<td id="pageViews"> </td>
							</tr>
						</table>
					</div>
					<div class="yui-u">
						<table class="listingTable">
							<tr>	
								<td width="15%" bgcolor="#ECECEC"><span><b><%= LanguageUtil.get(pageContext, "Unique-Visitors") %></b></span></td>
								<td width="15%" id="uniqueVisitors"> </td>
							</tr>
							<tr>
								<td bgcolor="#ECECEC"><span><b><%= LanguageUtil.get(pageContext, "New-Visits") %></b></span></td>
								<td id="newVisits"> </td>
							</tr>
						</table>
					</div>
					<div class="yui-u">
						<table class="listingTable">
							<tr>	
								<td width="15%" bgcolor="#ECECEC"><span><b><%= LanguageUtil.get(pageContext, "Bounce-Rate") %></b></span></td>
								<td width="15%" id="bounceRate"> </td>
							</tr>
							<tr>
								<td bgcolor="#ECECEC"><span><b><%= LanguageUtil.get(pageContext, "Time-on-Site") %></b></span></td>
								<td id="timeOnSite"> </td>
							</tr>
						</table>
					</div>
				</div>

					<div class="yui-g" style="margin-top:15px;">
						<div class="yui-u first">
							
							<!-- Top Pages -->
							<table class="listingTable">
								<thead>
									<tr>
										<th nowrap colspan="2"><b><%= LanguageUtil.get(pageContext, "Top-Pages") %> </b>&nbsp; <b><a href="javascript:viewTopPages(1, '');"><%= LanguageUtil.get(pageContext, "view") %></a></b></th>
									</tr>
								</thead>   
								<tbody id="topPages"></tbody>
							</table>
							
							<!-- Top Content -->
							<table class="listingTable" style="margin-top:15px;">
								<thead>
									<tr>
										<th nowrap colspan="2"><b><%= LanguageUtil.get(pageContext, "Top-Content") %></b>&nbsp; <b><a href="javascript:viewTopContent(1, '');"><%= LanguageUtil.get(pageContext, "view") %></a></b></th>
									</tr>
								</thead>   
								<tbody id="topContent"></tbody>
							</table>
							
						</div>
						<div class="yui-u" id="topReferersDiv">
							
							<!-- Top Referers -->
							<table class="listingTable">
								<thead>
									<tr>
										<th nowrap colspan="2"><b><%= LanguageUtil.get(pageContext, "Top-Referers") %></b>&nbsp; <b><a href="javascript:viewTopReferers(1,'');"><%= LanguageUtil.get(pageContext, "view") %></a></b></th>
									</tr>
								</thead>   
								<tbody id="topReferers"></tbody>
							</table>
							
						</div>
					</div>

				
					<table class="listingTable" style="margin-top:15px;">
						<thead>
							<tr>
								<th nowrap width="50%"><b><%= LanguageUtil.get(pageContext, "404") %>&nbsp; <a href="javascript:view404(1,'');"><%= LanguageUtil.get(pageContext, "view") %></a></th>
								<th nowrap width="50%"><%= LanguageUtil.get(pageContext, "Referer") %> </th>
								<th nowrap align="right" width="150">
									<input type="checkbox" dojoType="dijit.form.CheckBox" name="showIgnoredCheckbox" id="showIgnoredCheckbox" onchange="refresh404List();">
									<%= LanguageUtil.get(pageContext, "Show-Ignored") %>
								</th>
							</tr>
						</thead> 
						<tbody id="summary404s"></tbody>
					</table>
				</div>
				
			</div>
		</div>
	<!-- END Left Column Stats -->
	
	
	<!-- START Right Column stats -->
		<div dojoType="dijit.layout.ContentPane" splitter="false" region="right" style="width: 200px;overflow:auto;">
			
			<div id="rightColumnDiv">
				<div class="dijitLayoutContainer roundBox">
					<div style="margin:5px 10px 15px 10px;"><b><%=LanguageUtil.get(pageContext, "Numbers")%></b></div>
					<div class="siteOverview">
						<span><%= numberFormat.format(totalPages) %></span>
						<a href="javascript:viewPages();"><%=LanguageUtil.get(pageContext, "Pages")%></a>
					</div>
					<div class="siteOverview">
						<span><%= numberFormat.format(totalFiles) %></span>
						<a href="javascript:viewFiles();"><%=LanguageUtil.get(pageContext, "Files")%></a>
					</div>
					<div class="siteOverview">
						<span><%= numberFormat.format(totalContent) %></span>
						<a href="javascript:viewContent();"><%=LanguageUtil.get(pageContext, "Content-Items")%></a>
					</div>
					<div class="siteOverview">	
						<span><%= numberFormat.format(userCount) %></span>
						<a href="javascript:viewUsers();"><%=LanguageUtil.get(pageContext, "Users")%></a>
					</div>

				</div>
				<div class="dijitLayoutContainer roundBox" style="margin-top:20px;">
					<div style="margin:5px 10px 15px 10px;"><b><%= LanguageUtil.get(pageContext, "Referers") %></b></div>
					<div id="pieWrapper"></div>
					<div id="pieChartLegend" class="chartLegend"></div>
				</div>
			</div>
		</div>
	<!-- END Right Column stats -->
	
	</div>	
</div>
<!-- Statistics Tabs -->
	
<div id="listingWrapper" dojoType="dijit.Dialog" style="display:none;height:420px;width:700px;vertical-align: middle;overflow: auto;padding-top:15px\9;" draggable="true" >
	<div style="margin:-34px 0 15px 0;">
		<span id="listTitle"></span>
	</div>
	<div style="height:340px;width:660px;overflow-x:hidden;overflow-y:auto;">
		<table class="listingTable">
			<thead id="listingTableHeader"></thead> 
			<tbody id="summaryList"></tbody>
		</table>
	</div>
	<div id="footer" style="text-align:center;"></div>
</div>  


<!-- Properties Tabs -->
<div id="hostPropertiesTab" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Properties") %>">
 <% if(content != null){ %>
      <%@ include file="/html/portlet/ext/dashboard/host_properties_tab_inc.jsp" %>
 <%} %>	
</div>
<!-- Properties Tabs -->

<!-- Related content tabs -->
     <% if(!relationships.isEmpty()){
    	Structure structure = null;
    	boolean isParent = false;
    	ContentletAPI contentletService = APILocator.getContentletAPI();
    	Contentlet cont = content;
    	com.dotmarketing.portlets.structure.model.ContentletRelationships cRelationships = contentletService.getAllRelationships(cont); 
    	List<com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords> relationshipRecords = cRelationships.getRelationshipsRecords();
        for(Relationship rel : relationships){
        	  if(rel.getChildStructure().getInode().equals(cont.getStructure().getInode())){
	        		structure = rel.getParentStructure();
	        		isParent = true;
	        	}else if(rel.getParentStructure().getInode().equals(cont.getStructure().getInode())){
	        		structure = rel.getChildStructure();
	        		isParent = false;
	        	}
			  
        	

			boolean noRecords = true;
			int nFields = 3;
			if(!relationshipRecords.isEmpty()){%>
			<%for(com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords contRecords : relationshipRecords){%>
			<% if(contRecords.getRelationship().equals(rel)){ %>
			<%if(!contRecords.getRecords().isEmpty()){ 
			  noRecords = false;
			  if(contRecords.getRecords().size()==1){
				 content = contRecords.getRecords().get(0); 
				 if(content != null){ 			 
				 %>
				    <div id="relationship<%= rel.getInode()%>" dojoType="dijit.layout.ContentPane" title="<%= content.getStructure().getName() %>">
        	          <div>
                      <%@ include file="/html/portlet/ext/dashboard/rel_content_properties_tab_inc.jsp" %>
                <%} 	
				  
			  }else{
				  if(UtilMethods.isSet(structure.getName())){%>
        	
        	 <div id="relationship<%= rel.getInode()%>" dojoType="dijit.layout.ContentPane" title="<%= structure.getName() %>">
        	 <div>
        	   <table style="width: 100%;" class="listingTable" >
				 <thead>
					<tr class="beta">
					<% 
						if(langs.size() > 1) {
					%>
						<th width="20"><B><font class="beta" size="2"><%= LanguageUtil.get(pageContext, "Language") %></font></B></th>
						<th width="20"><B><font class="beta" size="2"></font></B></th>
					<% 
						}
			           boolean indexed = false;
			           boolean hasListedFields = false;
			           List<Field> targetFields = structure.getFields();
			           for (Field f : targetFields) {
				         if (f.isListed()) {
				    	   hasListedFields = true;
					       indexed = true;
					       nFields++;
	                 %>
						<th colspan="<%= nFields %>"><B><font class="beta" size="2"><%= f.getFieldName() %> </font></B>
						</th>
	                 <%
				      }
			       }
		
			       if (!indexed) {%>
						<th><B><font class="beta" size="2"> <%= LanguageUtil.get(pageContext, "No-Searchable-Fields-Found-Showing-the-Identity-Number") %> </font></B></th>
	              <%}%>
						
				</tr>
			</thead>
			<tbody id="<%= rel.getInode()%>Table">
			<%for(Contentlet con : contRecords.getRecords()){ %>
			<% Language language = langAPI.getLanguage(cont.getLanguageId()); %>
			 <tr> 
			   <td>
			     <img style="vertical-align: middle;" src="/html/images/languages/<%= langAPI.getLanguageCodeAndCountry(con.getLanguageId(),null) %>.gif" alt="'<%= language.getLanguage() %>'">
			   </td>
			   <td>
			    <%if(con.isLive()){ %>
			       <span class="liveIcon"></span>
			    <%}else if(con.isArchived()){ %>
			       <span class="archivedIcon"></span>
			    <%}else if(con.isWorking()){ %>
			      <span class="workingIcon"></span>
			    <%} %>
			   </td>
				<%  
				   int nFields2= 3;
				   boolean isFirst = true;
				   for (Field f : targetFields) {
					String fieldValue = "";
					if (f.isListed()) {
						nFields2++;
						String fieldName = f.getFieldName();
						ContentletAPI rconAPI = APILocator.getContentletAPI(); 
					    Object fieldValueObj = rconAPI.getFieldValue(con, f);
						if (fieldValueObj != null) {
							if (fieldValueObj instanceof java.util.Date) {
								 fieldValue = modDateFormat.format(fieldValueObj);
							} else if (fieldValueObj instanceof java.sql.Timestamp){
								 java.util.Date fieldDate = new java.util.Date(((java.sql.Timestamp)fieldValueObj).getTime());
								 fieldValue = modDateFormat.format(fieldDate);
							} else {
								 fieldValue = fieldValueObj.toString();
								}
							}
						fieldValue = fieldValue.replaceAll("'","\\\\'").replaceAll("\n","").replaceAll("\r","").trim();
						%>
						<%if(isFirst){ %>
						   <td colspan="<%= nFields2 %>">
				              <a href="javascript:editContent('<%= con.getInode()%>','<%= con.getStructure().getInode() %>')"><%= fieldValue %></a>
				           </td>	
						<%}else{ %>
						   <td colspan="<%= nFields2 %>">
				              <%= fieldValue %>
				           </td>	
						<%} %>
				     
				    <% isFirst = false;  
				    }%>			
				<%
				 } %>
		    </tr>
			<% } }
			   }
			  }
			 } %>
			</tbody>
			<%} %>
			<% if(noRecords){ %>
			 <div id="relationship<%= rel.getInode()%>" dojoType="dijit.layout.ContentPane" title="<%= structure.getName() %>">
        	 <div>
			<tbody id="<%= rel.getInode()%>TableNoResults">
			   <tr class="alternate_1">
				  <td colspan="<%= nFields %>" align="center"><div class="noResultsMessage"><b><%= LanguageUtil.get(pageContext, "No-Related-Content") %></b></div></td>
			   </tr>
			</tbody>
			<%} %>
		</table>
		</div>
      </div>
     <%  } 
        }
      } %>
<!-- Related content tabs -->

</div>






</liferay:box>
</form>
<%}else{ %>
<%@ include file="/html/portlet/ext/dashboard/not_licensed.jsp" %>
<% }%>
