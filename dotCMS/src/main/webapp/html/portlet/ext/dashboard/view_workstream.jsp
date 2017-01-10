<%@ include file="/html/portlet/ext/dashboard/init.jsp" %>

<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.portlets.workflows.model.WorkflowTask"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.dashboard.business.DashboardAPI"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.NoSuchUserException"%>
<%@page import="java.util.Date"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.business.RoleAPI"%>
<%@page import="com.dotmarketing.business.Versionable"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.HostAPI"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="java.util.Map"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%
String hostId = request.getParameter("hostId");
java.util.Map params = new java.util.HashMap();
params.put("struts_action",new String[] {"/ext/dashboard/view_dashboard"});
params.put("cmd",new String[] {Constants.VIEW_ACTIVITY_STREAM});
String referer = com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.MAXIMIZED.toString(),params)+"&hostId="+hostId;
List<Layout> layoutList=null;
String templateURL ="";
String containerURL ="";
String linkURL ="";
String pagesURL ="";
String filesURL ="";
String contentURL ="";
try {	
	layoutList=APILocator.getLayoutAPI().findAllLayouts();
	 for(Layout layoutObj:layoutList) {
		List<String> portletIdsForLayout=layoutObj.getPortletIds();
		for(String portletId : portletIdsForLayout){
		if (portletId.equals("EXT_13")) {
			templateURL = "/c/portal/layout?p_l_id=" + layoutObj.getId() +"&p_p_id=EXT_13&p_p_action=1";
		}else if (portletId.equals("EXT_12")) {
			containerURL = "/c/portal/layout?p_l_id=" + layoutObj.getId() +"&p_p_id=EXT_12&p_p_action=1";
		}else if (portletId.equals("EXT_1")) {
			linkURL = "/c/portal/layout?p_l_id=" + layoutObj.getId() +"&p_p_id=EXT_1&p_p_action=1";
		}else if (portletId.equals("EXT_15")) {
			pagesURL = "/c/portal/layout?p_l_id=" + layoutObj.getId() +"&p_p_id=EXT_15&p_p_action=1";
		}else if (portletId.equals("EXT_3")) {
			filesURL = "/c/portal/layout?p_l_id=" + layoutObj.getId() +"&p_p_id=EXT_3&p_p_action=1";
		}else if (portletId.equals("EXT_11")) {
			contentURL = "/c/portal/layout?p_l_id=" + layoutObj.getId() +"&p_p_id=EXT_11&p_p_action=1";
		}
	  }
	}
} catch (Exception e) {}	 
%>
<script type='text/javascript' src='/dwr/interface/DashboardAjax.js'></script>
<script type="text/javascript">

      var orderByWs = '';
      var orderByWsDir = '';
      var maxCount = 20;
      var globalPageNumber = 1;
      dojo.require('dotcms.dojo.data.HostReadStore');
        
      <liferay:include page="/html/js/calendar/calendar_js_box_ext.jsp" flush="true">
        <liferay:param name="calendar_num" value="1" />
      </liferay:include>

      function <portlet:namespace />setCalendarDate_0(year, month, day) {
   	     date = document.getElementById("workStreamFromDate");
   	     var monthStr = ''+month;
   	     var dayStr = ''+day;
   	     if(month < 10){
   	        monthStr = '0'+month;
   	     }
         if(day < 10){
   	       dayStr = '0'+day;
         }
   	   date.value=monthStr+"/"+dayStr+"/"+year;
   	}

     <liferay:include page="/html/js/calendar/calendar_js_box_ext.jsp" flush="true">
      <liferay:param name="calendar_num" value="2" />
     </liferay:include>
 	 
     function <portlet:namespace />setCalendarDate_1(year, month, day) {
      	date = document.getElementById("workStreamToDate");
      	var monthStr = ''+month;
      	var dayStr = ''+day;
      	if(month < 10){
      	   monthStr = '0'+month;
      	}
        if(day < 10){
      	   dayStr = '0'+day;
        }
      date.value=monthStr+"/"+dayStr+"/"+year;
    }

     function viewWorkStreams(pageNumber, orderBy){
    	    globalPageNumber = pageNumber;
    	   	var hostId = dijit.byId("dahboardHostSelectorWorkStream").value;
    	    var userId = dijit.byId("dashboardUserSelector").value;
    	    if(userId.indexOf('user-') == 0){
    	    	userId = userId.substring(5, userId.length);
        	}
    		var fromDateStr = document.getElementById("workStreamFromDate").value;
    	    var toDateStr = document.getElementById("workStreamToDate").value;
    		DashboardAjax.getWorkStreams(hostId,userId,fromDateStr,toDateStr,20,pageNumber,orderBy,dojo.hitch(this, fillWorkStreamTable));
    	}

 	var noRecordsTemplate = '<tr class="alternate_1" id="rowNoResults"><td colspan="5"><div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "No-Records-Found") %></div></td></tr>';
 		 
 	var workStreamsTemplate = '<tr class="${className}"><td><a href="javascript:editAsset(\'${assetType}\',\'${assetInode}\',\'${structureInode}\');">${name}</a></td>\
 		<td>${host}</td>\
 		<td>${user}</td>\
 		<td>${action}</td>\
 		<td>${date}</td>\
 	   </tr>';

    	function fillWorkStreamTable(data){
            DWRUtil.removeAllRows(dojo.byId('workStreams'));
    		var pageNumber = data.pageNumber;
    		var orderBy = "'" + data.orderBy + "'";
    		var minIndex = (pageNumber - 1) * maxCount;
            var totalCount = data.wsCount;
            var maxIndex = maxCount * pageNumber;
            if((minIndex + maxCount) >= totalCount){
            	maxIndex = totalCount;
            }
            var leftPageStr = '';
            if (minIndex != 0) { 
                leftPageStr += '<a href="javascript:viewWorkStreams('+(pageNumber-1)+','+orderBy+')"> << </a>'; 
    	    } 
            var rightPageStr = '';
            if (maxIndex < totalCount) { 
            	rightPageStr += '<a href="javascript:viewWorkStreams('+(pageNumber + 1)+','+orderBy+')"> >> </a>';  
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
    		dojo.byId('footer').innerHTML = str; 	
 		    var workStreams = data.workStreams;
 		    if(workStreams.length == 0) {
 			   dojo.place(dojo.string.substitute(noRecordsTemplate, { }), 'workStreams', 'last');
 		    } else {
 			    var tableHTML = "";
 			    for(var i = 0; i < workStreams.length; i++) {
 				   var ws = workStreams[i];
 				   var trClassName = (i%2==0)?'alternate_1':'alternate_2';
 				   var html = dojo.string.substitute(workStreamsTemplate, { className:trClassName, assetType:ws.assetType, assetInode:ws.inode, structureInode:ws.structureInode, name: ws.title, host: ws.hostname ,user: ws.username, action:ws.action, date:ws.mod_date  });
 				   tableHTML += html;	
 			   }
 			   dojo.place(tableHTML, 'workStreams', 'last');
 		   }
    	}


    	function orderWs(orderBy, dir){
    	   	if(orderByWs==''){
    	   	   orderByWs = orderBy;
    	   	   orderByWsDir = dir;
    	   	}else{
    	   	   	if(orderByWs == orderBy){
    	   	   	   	if(orderByWsDir == 'desc'){
    	   	        	dir = 'asc';
    	   	   	   	}else{
    	   	   	        dir = 'desc';
    	   	   	   	}
    	   	   	}
    	   	    orderByWs = orderBy;
    	   	    orderByWsDir = dir;
    	   	}
    	  viewWorkStreams(globalPageNumber,orderBy + ' ' + dir);
    	}

    	function clearWsSearch(){
    		document.getElementById('fm').reset();
    	   	dijit.byId("dahboardHostSelectorWorkStream").displayedValue = "";
    	    dijit.byId("dashboardUserSelector").displayedValue = "";
  	      	dijit.byId("dahboardHostSelectorWorkStream").value = "";
    	    dijit.byId("dashboardUserSelector").value = "";
    		DashboardAjax.getWorkStreams('','','','',20,1,'',dojo.hitch(this, fillWorkStreamTable));
    	}


   	 function editContent(contInode,structureInode){
   	     var URL = '<%=contentURL%>&p_p_state=maximized&p_p_mode=view&_EXT_11_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet&_EXT_11_cmd=edit&_EXT_11_selectedStructure={stInode}&host_id={host_id}&inode={inode}&referer=<%=referer%>';
   	     var hostId = dijit.byId("dahboardHostSelectorWorkStream").value;
 	     var href = dojo.replace(URL, { stInode:structureInode, inode: contInode, host_id: hostId })
   		 window.location=href;	
   	   }

       function editHtmlPage(contInode){
      	     var URL = '<%=pagesURL%>&p_p_state=maximized&p_p_mode=view&_EXT_15_struts_action=%2Fext%2Fhtmlpages%2Fedit_htmlpage&_EXT_15_cmd=edit&host_id={host_id}&inode={inode}&referer=<%=referer%>';	   	     	        
      	     var hostId = dijit.byId("dahboardHostSelectorWorkStream").value;
      	     var href = dojo.replace(URL, {inode: contInode, host_id: hostId})
      		 window.location=href;	
      	}

       function editFile(contInode){
    	     var URL = '<%=filesURL%>&p_p_state=maximized&p_p_mode=view&_EXT_3_struts_action=%2Fext%2Ffiles%2Fedit_file&_EXT_3_cmd=edit&host_id={host_id}&inode={inode}&referer=<%=referer%>';	   	     	              	     	        
    	     var hostId = dijit.byId("dahboardHostSelectorWorkStream").value;
    	     var href = dojo.replace(URL, {inode: contInode, host_id: hostId})
    		 window.location=href;	
    	}

       function editTemplate(contInode){
  	     var URL = '<%=templateURL%>&p_p_state=maximized&p_p_mode=view&_EXT_13_struts_action=%2Fext%2Ftemplates%2Fedit_template&_EXT_13_cmd=edit&host_id={host_id}&inode={inode}&referer=<%=referer%>';	   	     	              	     	           	     	        
  	     var hostId = dijit.byId("dahboardHostSelectorWorkStream").value;
   	     var href = dojo.replace(URL, {inode: contInode, host_id: hostId})
  		 window.location=href;	
     	}

       function editContainer(contInode){
    	     var URL = '<%=containerURL%>&p_p_state=maximized&p_p_mode=view&_EXT_12_struts_action=%2Fext%2Fcontainers%2Fedit_container&_EXT_12_cmd=edit&host_id={host_id}&inode={inode}&referer=<%=referer%>';	   	     	              	     	           	     	        	  	     	        
    	     var hostId = dijit.byId("dahboardHostSelectorWorkStream").value;
    	     var href = dojo.replace(URL, {inode: contInode, host_id: hostId})
    		 window.location=href;	
       	}

       function editLink(contInode){
  	     var URL = '<%=linkURL%>&p_p_state=maximized&p_p_mode=view&_EXT_1_struts_action=%2Fext%2Flinks%2Fedit_link&_EXT_1_cmd=edit&host_id={host_id}&inode={inode}&referer=<%=referer%>';	   	     	              	     	           	     	        	  	     	           	     
  	     var hostId = dijit.byId("dahboardHostSelectorWorkStream").value;
  	     var href = dojo.replace(URL, {inode: contInode, host_id: hostId})
  		 window.location=href;	
     	}

      	

    	function editAsset(assetType, inode, structureInode){
    		if(assetType=='htmlpage'){
         		editHtmlPage(inode);
         	}else if(assetType =='file_asset'){
         		editFile(inode);
         	}else if(assetType=='contentlet'){
         		editContent(inode,structureInode);
         	}else if(assetType=='container'){
         		editContainer(inode);
         	}else if(assetType=='template'){
         		editTemplate(inode);
         	}else if(assetType=='link'){
         		editLink(inode);
         	} 

    	}
    	<% if(LicenseUtil.getLevel() > 199){ %>	
    	dojo.addOnLoad(function(){
       		DashboardAjax.getWorkStreams('<%=request.getParameter("hostId")%>','','','',20,1,'',dojo.hitch(this, fillWorkStreamTable));
    	});
    	<%}%>

 </script>    

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= LanguageUtil.get(pageContext, "view-workstream") %>' />
<% if(LicenseUtil.getLevel() > 199){ %>	
<form id="fm" method="post">
	<div class="portlet-toolbar" style="white-space: nowrap">
		<span dojoType="dotcms.dojo.data.HostReadStore" jsId="HostStore"></span>
		<span dojoType="dotcms.dojo.data.UsersReadStore" jsId="usersStore" includeRoles="false"></span>
		<% String selectedHost =request.getParameter("hostId"); %>
		
		<span style="margin:0 15px 0 0;">
			<%= LanguageUtil.get(pageContext, "Host") %>: 
			<select id="dahboardHostSelectorWorkStream" name="dahboardHostSelectorWorkStream" dojoType="dijit.form.FilteringSelect" 
				store="HostStore"  pageSize="30" labelAttr="hostname"  searchAttr="hostname" 
				searchDelay="400"  <%= UtilMethods.isSet(selectedHost)?"value=\"" + selectedHost+ "\"":""  %> invalidMessage="<%= LanguageUtil.get(pageContext, "Invalid-option-selected")%>">
			</select>
		</span>

		<span style="margin:0 15px 0 0;">
		    <%= LanguageUtil.get(pageContext, "User") %>: 
			<select id="dashboardUserSelector" dojoType="dijit.form.FilteringSelect" store="usersStore" 
			        searchDelay="300" pageSize="30" labelAttr="name" invalidMessage="<%= LanguageUtil.get(pageContext, "Invalid-option-selected") %>">
			</select>
		</span>
			
		<span style="margin:0 15px 0 0;">
			<%= LanguageUtil.get(pageContext, "From") %>: 
	 		<input dojoType="dijit.form.DateTextBox" type="text" id="workStreamFromDate" name="workStreamFromDate" value="" style="width:110px;" iconClass="calDayIcon">
	 
			<%= LanguageUtil.get(pageContext, "To") %>: 
	 		<input dojoType="dijit.form.DateTextBox" type="text" id="workStreamToDate" name="workStreamToDate" value="" style="width:110px;">
		</span>
	
		
		<button dojoType="dijit.form.Button" onClick="viewWorkStreams(1,'')" iconClass="searchIcon">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Search")) %>
		</button>
		<button dojoType="dijit.form.Button" id="clearButtonWs" onClick="clearWsSearch();" iconClass="resetIcon">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Clear-Search")) %>
		</button>            
		
	</div>
</form>



	<table class="listingTable">
	    <thead>
		<tr>
		<th nowrap>
			<a href="javascript:orderWs('name','desc');">
			   <%= LanguageUtil.get(pageContext, "Name") %>
			</a>
		</th>
		<th nowrap>
			<a href="javascript:orderWs('hostname','desc');">
				<%= LanguageUtil.get(pageContext, "Host") %>
			</a>
		</th>
		<th nowrap>
			<a href="javascript:orderWs('username','desc');">
			    <%= LanguageUtil.get(pageContext, "User") %>
			</a>
		</th>
		<th nowrap>
			<a href="javascript:orderWs('action','desc');">
			    <%= LanguageUtil.get(pageContext, "Action") %>
			</a>
		</th>
		<th nowrap>
			<a href="javascript:orderWs('analytic_summary_workstream.mod_date','desc');">
			    <%= LanguageUtil.get(pageContext, "Date") %>
			</a>
		</th>
	</tr>
	</thead>
    <tbody id="workStreams"></tbody>
</table>

<br></br>

<div id="footer" class="yui-u" style="text-align:center;"></div>
<%}else{ %>
<%@ include file="/html/portlet/ext/dashboard/not_licensed.jsp" %>
<% }%>
</liferay:box>