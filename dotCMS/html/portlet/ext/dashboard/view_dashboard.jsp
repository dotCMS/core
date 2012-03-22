<%@ include file="/html/portlet/ext/dashboard/init.jsp" %>
<%@page import="com.dotmarketing.portlets.structure.model.Field"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.portlets.categories.model.Category"%>
<%@page import="com.dotmarketing.portlets.categories.business.CategoryAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.dashboard.business.DashboardAPI"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.NoSuchUserException"%>
<%@page import="java.util.Date"%>
<%@page import="com.dotmarketing.util.UtilHTML"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.InodeUtils"%>
<%@page import="com.dotmarketing.portlets.contentlet.util.ContentletUtil"%>
<%@page import="com.dotmarketing.exception.DotSecurityException"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotmarketing.portlets.structure.business.FieldAPI"%>
<%@page import="com.dotmarketing.business.IdentifierCache"%>
<%@page import="com.dotmarketing.util.VelocityUtil"%>
<%@page import="com.dotmarketing.business.RoleAPI"%>
<%@page import="com.dotmarketing.business.Versionable"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.HostAPI"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="java.util.Map"%>
<%@page import="com.dotmarketing.util.Parameter"%>
<%@page import="com.dotmarketing.portlets.dashboard.model.HostWrapper"%>
<%@page import="com.dotmarketing.portlets.dashboard.model.DashboardWorkStream"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.cache.StructureCache"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>




<% 
java.util.Map params = new java.util.HashMap();
params.put("struts_action",new String[] {"/ext/dashboard/view_dashboard"});
String referer = java.net.URLDecoder.decode(com.dotmarketing.util.PortletURLUtil.getRenderURL(request,javax.portlet.WindowState.MAXIMIZED.toString(),params));
Structure hostStructure = StructureCache.getStructureByVelocityVarName("Host");
Language lang = APILocator.getLanguageAPI().getDefaultLanguage();

int pageNumber = 1;
if (request.getParameter("pageNumber") != null && !request.getParameter("pageNumber").equals("")) {
	pageNumber = Integer.parseInt(request.getParameter("pageNumber"));
}
int perPage = 5;
int span = 5;
int minIndex = (pageNumber - 1) * perPage;
int maxIndex = perPage * pageNumber;
DashboardAPI dAPI = APILocator.getDashboardAPI();
RoleAPI rAPI = APILocator.getRoleAPI();
String hostId = (String) session.getAttribute(com.dotmarketing.util.WebKeys.CMS_SELECTED_HOST_ID);
HostAPI hAPI = APILocator.getHostAPI();
Host myHost =hAPI.find(hostId, user, false);
List<com.dotmarketing.portlets.structure.model.Field> fields = com.dotmarketing.cache.FieldsCache.getFieldsByStructureVariableName("Host");
for(Field f:fields){
	if(f.isListed()){
		span++;
	}
}
String orderByWs = (request.getParameter("orderByWs") != null) ? request.getParameter("orderByWs"): "";
String orderByHost = (request.getParameter("orderByHost") != null) ? request.getParameter("orderByHost"): "";
List<Layout> layoutList=null;
String browserURL ="";
try {	
	layoutList=APILocator.getLayoutAPI().findAllLayouts();
	 for(Layout layoutObj:layoutList) {
		List<String> portletIdsForLayout=layoutObj.getPortletIds();
		for(String portletId : portletIdsForLayout){
		if (portletId.equals("EXT_BROWSER")) {
			browserURL = "/c/portal/layout?p_l_id=" + layoutObj.getId() +"&p_p_id=EXT_BROWSER&p_p_action=0";
			break;
		}
	  }
	}
} catch (Exception e) {}


String selectedHost =request.getParameter("dahboardHostSelector");
if(!UtilMethods.isSet(selectedHost)){
	selectedHost = (String)request.getAttribute("dahboardHostSelector");
}

int periodData = dAPI.checkPeriodData(0,0);


%>
<%@ include file="/html/portlet/ext/contentlet/field/edit_field_js.jsp" %>  
<script type='text/javascript' src='/dwr/engine.js'></script>
<script type='text/javascript' src='/dwr/util.js'></script>
<script type='text/javascript' src='/dwr/interface/TagAjax.js'></script>
<script type='text/javascript' src='/dwr/interface/DashboardAjax.js'></script>
<script type="text/javascript">

      var orderByHost = '';
      var orderByHostDir = '';
      var maxCount = 20;
      var globalPageNumber = 1;
      dojo.require('dotcms.dojo.data.HostReadStore');
      dojo.require('dijit.ProgressBar');

      function getFormData(formId,nameValueSeparator){ // Returns form data as name value pairs with nameValueSeparator.
  		
  		var formData = new Array();

  		//Taking the text from all the textareas
  		var k = 0;
  		$(document.getElementById(formId)).getElementsBySelector('textarea').each(
  				function (textareaObj) {
  					if ((textareaObj.id != "") && (codeMirrorEditors[textareaObj.id] != null)) {
  						try {
  							document.getElementById(textareaObj.id).value=codeMirrorEditors[textareaObj.id].getCode();
  						} catch (e) {
  						}
  					}
  				}
  			);

  		var formElements = document.getElementById(formId).elements;
  		
  		var formDataIndex = 0; // To collect name/values from multi-select,text-areas. 
   
  		for(var formElementsIndex = 0; formElementsIndex < formElements.length; formElementsIndex++,formDataIndex++){
  			
  			
  			// Collecting only checked radio and checkboxes
  			if((formElements[formElementsIndex].type == "radio") && (formElements[formElementsIndex].checked == false)){
  				continue;									
  			}
  			
  			if((formElements[formElementsIndex].type == "checkbox") && (formElements[formElementsIndex].checked == false)){
  				continue;									
  			}

  			// Collecting selected values from multi select 
  			if(formElements[formElementsIndex].type == "select-multiple") {				
  				for(var multiSelectIndex = 0; multiSelectIndex < formElements[formElementsIndex].length; multiSelectIndex++){

  					if(formElements[formElementsIndex].options[multiSelectIndex].selected == true){
  						formData[formDataIndex] = formElements[formElementsIndex].name+nameValueSeparator+formElements[formElementsIndex].options[multiSelectIndex].value;						
  						formDataIndex++;											
  					}					
  				}
  				continue;
  			}
  			
  			// Getting values from text areas
  			if(formElements[formElementsIndex].type == "textarea" && formElements[formElementsIndex].id != '') {				
  				if(tinyMCE.get(formElements[formElementsIndex].id) != null){											
  					textAreaData = tinyMCE.get(formElements[formElementsIndex].id).getContent();					
  					formData[formDataIndex] = formElements[formElementsIndex].name+nameValueSeparator+textAreaData;			
  					continue;
  				}				
  				if(tinyMCE.get(formElements[formElementsIndex].id) == null){		
  						textAreaData = document.getElementById(formElements[formElementsIndex].id).value;
  						formData[formDataIndex] = formElements[formElementsIndex].name+nameValueSeparator+textAreaData;
  						continue;					
  				}														
  			}
  						
  			formData[formDataIndex] = formElements[formElementsIndex].name+nameValueSeparator+formElements[formElementsIndex].value;				
  		}		
  		return formData;
  	}



        

     function submitfm() {
     	form = document.getElementById('fm');
     	form.pageNumber.value = 1;
     	form.action = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/dashboard/view_dashboard" /></portlet:renderURL>';
     	//submitForm(form);
     	DashboardAjax.getHosts(getFormData("fm","<%= com.dotmarketing.util.WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR %>"),10,1,'',dojo.hitch(this, fillHostsTable));
     }

     function updateDateField(varName, timeField) {
 		var field = $(varName);
 		var dateValue = "01/01/1900 ";
        if(!timeField){
        	dateValue = document.getElementById(varName+'Date').value + " ";
        }
 		if(timeField || (dateValue!=null && dateValue!=' ')){
 		if (document.getElementById(varName + 'Hour').value != null && document.getElementById(varName + 'Hour').value != '') {
 			var hour = document.getElementById(varName + 'Hour').value;
 			
 			if(hour < 10) hour = "0" + hour;
 			var min = document.getElementById(varName + 'Minute').value;
 			if(min!=''){
 			   dateValue += hour + ":" + min; 
 			}else{
 				dateValue += hour + ":00" ;
 			}
 		} else {
 			dateValue += "00:00";
 		}
 		  field.value = dateValue;
 		}else{
 			field.value = ''; 
 			if (document.getElementById(varName + 'Hour').value != null && document.getElementById(varName + 'Hour').value != '') {
 	 			dijit.byId(varName + 'Hour').reset();
 	 			dijit.byId(varName + 'Minute').reset();
 			}
 			
 	 	}
 	} 

   	function viewHostReport(id){
   		var URL = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">\
		<portlet:param name="struts_action" value="/ext/dashboard/view_dashboard" />\
		<portlet:param name="cmd" value="<%=Constants.VIEW_HOST_REPORT %>" />\
		<portlet:param name="lang" value="<%= Long.toString(lang.getId()) %>" />\
		<portlet:param name="referer" value="<%=referer%>" />\
        </portlet:actionURL>&hostId={hostIdentifier}';
        var href = dojo.replace(URL, { hostIdentifier: id})
   		window.location=href;	
   	 }


	 function viewWorkStream(id){
		var URL = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">\
		<portlet:param name="struts_action" value="/ext/dashboard/view_dashboard" />\
		<portlet:param name="cmd" value="<%=Constants.VIEW_ACTIVITY_STREAM %>" />\
		<portlet:param name="lang" value="<%= Long.toString(lang.getId()) %>" />\
		<portlet:param name="referer" value="<%=referer%>" />\
		</portlet:actionURL>&hostId={hostIdentifier}';
		var href = dojo.replace(URL, { hostIdentifier: id})
		window.location=href;	
	 }

	 function viewBrowser(id){
		    var URL = '<%=browserURL%>&hostId={hostIdentifier}';
		    var href = dojo.replace(URL, { hostIdentifier: id})
		    window.location=href;	
	 }        

   	function viewWorkStreams(pageNumber, orderBy){
   	   	var hostId = dijit.byId("dahboardHostSelectorWorkStream").value;
   	    var userId = dijit.byId("dashboardUserSelector").value;
   		var fromDateStr = document.getElementById("workStreamFromDate").value;
   	    var toDateStr = document.getElementById("workStreamToDate").value;
   		DashboardAjax.getWorkStreams(hostId,userId,fromDateStr,toDateStr,5,1,orderBy,dojo.hitch(this, fillWorkStreamTable));
   	}

	var noRecordsTemplate = '<tr id="rowNoResults"><td colspan="5"><div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "No-Records-Found") %></div></td></tr>';
		 

   	function viewHosts(pageNumber, orderBy){  
   		globalPageNumber = pageNumber;
   		DashboardAjax.getHosts(getFormData("fm","<%= com.dotmarketing.util.WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR %>"),15,pageNumber,orderBy,dojo.hitch(this, fillHostsTable));
   	}

   	function orderHosts(orderBy, dir){
   	   	if(orderByHost==''){
   	   	  orderByHost = orderBy;
   	      orderByHostDir = dir;
   	   	}else{
   	   	   	if(orderByHost == orderBy){
   	   	   	   	if(orderByHostDir == 'desc'){
   	   	        	dir = 'asc';
   	   	   	   	}else{
   	   	   	        dir = 'desc';
   	   	   	   	}
   	   	   	}
   	   	    orderByHost = orderBy;
   	   	    orderByHostDir = dir;
   	   	}
   	   viewHosts(globalPageNumber,orderBy + ' ' + dir);
   	}

   	function clearHostSearch(){
   		document.getElementById('fm').reset();
   	   	dijit.byId("dahboardHostSelector").displayedValue = "";
 	   	dijit.byId("dahboardHostSelector").value = "";
 		dojo.query("input[type='hidden']",'fm').forEach(function(node, index, arr){
   	   		node.value="";
   	   	});
   	    dojo.query(".dijitCheckBoxInput",'fm').forEach(function(node, index, arr){
   	    	dijit.byId(node.id).reset();
	   	 });

 		DashboardAjax.getHosts(getFormData("fm","<%= com.dotmarketing.util.WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR %>"),15,1,'',dojo.hitch(this, fillHostsTable));
   	}

   	function clearValue(select){
   	   	if(dijit.byId(select).displayedValue == ''){
   	   	 console.log(dijit.byId(select).displayedValue);
   	      dijit.byId(select).displayedValue = "";
	   	  dijit.byId(select).value = "";
	   	  dijit.byId(select).selectedItem = null;
   	   	}
   	}

   	var noRecordsHostTemplate = '<tr class="alternate_1" id="rowNoResults"><td colspan="${fields}"><div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "No-Records-Found") %></div></td></tr>';
   	var hostTemplate1 = '<tr id="${inode}" onmouseover="changeBgOver(\'${inode}\');" style="cursor:pointer" onclick="viewHostReport(\'${hostId}\');" onmouseout="changeBgOut(\'${inode}\');"><td><a href="javascript:viewHostReport(\'${hostId}\');">${name}</a></td><td>${status}</td>';
    var hostTemplate2 = '<td>${pageViews}</td></tr>';
    var hostTemplate3 = '<td>${vfield}</td>';
    var contextMenuTemplate='<div dojoType="dijit.Menu" class="dotContextMenu" contextMenuForWindow="false" style="display: none;" targetNodeIds="${hostInode}">\
                               ${menuesHTML}\
                             </div>';
   
    var contextMenuTemplate2 =  '<div dojoType="dijit.MenuItem" iconClass="appMonitorIcon" onClick="javascript:viewHostReport(\'${hostId}\');">\<%= LanguageUtil.get(pageContext, "Host-Report") %>\</div>';
    var contextMenuTemplate3 =  '<div dojoType="dijit.MenuItem" iconClass="bowserIcon" onClick="javascript:viewBrowser(\'${hostId}\');">\<%= LanguageUtil.get(pageContext, "View-Host-Browser") %>\</div>';
    var contextMenuTemplate4 =  '<div dojoType="dijit.MenuItem" iconClass="workStreamIcon" onClick="javascript:viewWorkStream(\'${hostId}\');">\<%= LanguageUtil.get(pageContext, "View-Activity-Stream") %>\</div>';


   	function fillHostsTable(data){
		DWRUtil.removeAllRows(dojo.byId('hosts'));
   		var pageNumber = data.pageNumber;
		var orderBy = "'" + data.orderBy + "'";
		var minIndex = (pageNumber - 1) * maxCount;
        var totalCount = data.hostCount;
        var maxIndex = maxCount * pageNumber;
        if((minIndex + maxCount) >= totalCount){
        	maxIndex = totalCount;
        }
        var leftPageStr = '';
        if (minIndex != 0) { 
            leftPageStr += '<a href="javascript:viewHosts('+(pageNumber-1)+','+orderBy+')"> << </a>'; 
	    } 
        var rightPageStr = '';
        if (maxIndex < totalCount) { 
        	rightPageStr += '<a href="javascript:viewHosts('+(pageNumber + 1)+','+orderBy+')"> >> </a>';  
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
   		var hosts = data.hosts;
		if(hosts.length == 0) {
			dojo.place(dojo.string.substitute(noRecordsHostTemplate, { fields:<%=span%>}), 'hosts', 'only');
		} else {
			var tableHTML = "";
			for(var i = 0; i < hosts.length; i++) {
				var host = hosts[i];
				var fields = host.fields;
				var trClassName = (i%2==0)?'alternate_1':'alternate_2';
				var html = dojo.string.substitute(hostTemplate1, { inode:host.inode, csn:trClassName, hostId: host.identifier, name:host.hostName, status:host.status });
				for(var j = 0; j<fields.length;j++){
					var field = fields[j];
					html+= dojo.string.substitute(hostTemplate3, { vfield:field});
				}
				html+= dojo.string.substitute(hostTemplate2, { pageViews:host.pageViews});
				tableHTML += html;	


				var contextMenuHTML = dojo.string.substitute(contextMenuTemplate2, { hostId:host.identifier });
				contextMenuHTML +=dojo.string.substitute(contextMenuTemplate3, { hostId:host.identifier  });
				contextMenuHTML += dojo.string.substitute(contextMenuTemplate4, { hostId:host.identifier  });
				
				var contextHtml = dojo.string.substitute(contextMenuTemplate, {menuesHTML: contextMenuHTML,hostInode:host.inode });
                dojo.place(contextHtml, 'hostContextMenues', 'last');
			}
			dojo.place(tableHTML, 'hosts', 'only');
            dojo.parser.parse('hostContextMenues');
			
		}
   	}
	
	function changeBgOver(inode){
		dojo.style(inode, "background", "#CDEAFD"); 
	}
	function changeBgOut(inode){
		dojo.style(inode, "background", "#ffffff"); 
	}

    <% if(LicenseUtil.getLevel() > 199){ %>		
    dojo.addOnLoad(function(){
    	dojo.place('<br/><br/><br/><br/><img src="/html/images/icons/round-progress-bar.gif" /><br/>&nbsp;&nbsp;&nbsp;<b><%= LanguageUtil.get(pageContext, "Loading") %>...</b>', 'hosts', 'only');    	
   		DashboardAjax.getHosts(getFormData("fm","<%= com.dotmarketing.util.WebKeys.CONTENTLET_FORM_NAME_VALUE_SEPARATOR %>"),20,1,'',dojo.hitch(this, fillHostsTable));
	});
	<%}%>
    

	    
</script>
<style media="all" type="text/css">
	@import url(/html/css/widget.css);
	@import url(/html/portlet/ext/contentlet/field/edit_field.css);
	@import url(/html/portlet/ext/browser/browser.css);
</style>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= LanguageUtil.get(pageContext, "view-dashboard") %>' />

<style>
	.searchTable{width:100%;}
	.searchTable td{vertical-align:top;}
	.searchTable td.tdLabel{text-align:right;padding:2px 7px 2px 5px;width:5%;white-space:nowrap;font-weight:bold;}
	.searchTable td.tdField{padding:2px 40px 2px 0;width:20%;}
</style>
<% if(LicenseUtil.getLevel() > 199){ %>		
<form id="fm" method="post">
<% if(periodData<=0){ %>
<br />
<br />
<%} %>

<input type="hidden" name="pageNumber" value="<%=pageNumber%>">
<span dojoType="dotcms.dojo.data.HostReadStore" jsId="HostStore"></span>

<div class="roundBox" style="margin:0 0 15px 0;background:#f1f1f1;">
	<table class="searchTable">
		<tr>
			<td class="tdLabel"><%= LanguageUtil.get(pageContext, "Host") %>:</td>
			<td class="tdField">
				<select id="dahboardHostSelector" name="dahboardHostSelector" dojoType="dijit.form.FilteringSelect" 
					store="HostStore"  pageSize="30" labelAttr="hostname"  searchAttr="hostname" 
					searchDelay="400"  <%= UtilMethods.isSet(selectedHost)?"value=\"" + selectedHost+ "\"":""  %> invalidMessage="<%= LanguageUtil.get(pageContext, "Invalid-option-selected")%>">
				</select>
	    	</td>
		    	
		    	
		    	
			<% int count = 2;
			   int numTd = 8;
			for(com.dotmarketing.portlets.structure.model.Field f : fields){
				//ignore non-searchable and host/alias
				if(!f.isSearchable() || "hostName".equals(f.getVelocityVarName()) || "aliases".equals(f.getVelocityVarName())){
					continue;
				}
				count++;
				%>
				
				<%-- Always print the field name in its own td box --%>
				<td class="tdLabel"><%= f.getFieldName()%>:</td>
				<td class="tdField">
					<%String defaultValue = f.getDefaultValue() != null ? f.getDefaultValue().trim() : "";
					String fieldValues = f.getValues() == null ? "" : f.getValues().trim();
					Object value = request.getParameter(f.getFieldContentlet())==null?request.getAttribute(f.getFieldContentlet()):request.getParameter(f.getFieldContentlet());
		 
		 			//-------  Start the BIG FRIGGEN IF FieldType Statement  ----------

			 		if (f.getFieldType().equals(Field.FieldType.TEXT.toString()) || 
								 f.getFieldType().equals(Field.FieldType.TEXT_AREA.toString()) ||
								 f.getFieldType().equals(Field.FieldType.WYSIWYG.toString()) ||
								 f.getFieldType().equals(Field.FieldType.CUSTOM_FIELD.toString()) ) {      
						boolean isNumber = (f.getFieldContentlet().startsWith(Field.DataType.INTEGER.toString()) || f.getFieldContentlet().startsWith(Field.DataType.FLOAT.toString()));
						String textValue = UtilMethods.isSet(value) ? value.toString() : (UtilMethods.isSet(defaultValue) ? defaultValue : "");
						if(textValue != null){
							textValue = textValue.replaceAll("&", "&amp;");
							textValue = textValue.replaceAll("<", "&lt;");
							textValue = textValue.replaceAll(">", "&gt;");
						} 
						count++;%>
						
							<input type="text" name="<%=f.getFieldContentlet()%>" id="<%=f.getVelocityVarName()%>" <%=(isNumber) ? "dojoType='dijit.form.NumberTextBox' style='width:200px;'" : "dojoType='dijit.form.TextBox' style='width:200px'" %> value="<%= UtilMethods.htmlifyString(textValue) %>"  />
							
						<%} else if (f.getFieldType().equals(Field.FieldType.RADIO.toString())) {%>
							
		           				
							<% 
								String radio = f.getFieldContentlet();
								count++;
								String[] pairs = fieldValues.split("\r\n");
								for (int j = 0; j < pairs.length; j++) {
									String pair = pairs[j];
									String[] tokens = pair.split("\\|");            
									String name = (tokens.length > 0 ? tokens[0] : "");
									Object pairValue = (tokens.length > 1 ? tokens[1] : name);
									if (value instanceof Boolean)
										pairValue = Parameter
										.getBooleanFromString((String) pairValue);
									else if (value instanceof Long)
										pairValue = Parameter.getLong((String) pairValue);
									else if (value instanceof Double)
										pairValue = Parameter.getDouble((String) pairValue);
										String checked = "";
									if ((UtilMethods.isSet(value) && pairValue.toString().equals(value.toString()))|| (!UtilMethods.isSet(value) && UtilMethods.isSet(defaultValue) && defaultValue.toString().equals(pairValue.toString()))) {
										checked = "checked";
									}%>
				  	               <div style="height:20px;vertical-align:middle;float:left;white-space:nowrap;margin-right:10px;">
										<input type="radio" dojoType="dijit.form.RadioButton" name="<%=radio%>" id="<%=f.getVelocityVarName() + j %>" value="<%=pairValue%>" <%=checked%>>
										<label for="<%=f.getVelocityVarName() + j %>"><%=name%></label>
				  	               </div>
		           				<%}%>
							
	
	            		<%}else if (f.getFieldType().equals(Field.FieldType.SELECT.toString())) {%>
	
			               		<select dojoType="dijit.form.FilteringSelect" autocomplete="true" name="<%=f.getFieldContentlet()%>">
				               		<option value=""></option>
						            <%
						               String[] pairs = fieldValues.split("\r\n");
						               count++;
						               for (int j = 0; j < pairs.length; j++) {    
						            	   String pair = pairs[j];   
						            	   String[] tokens = pair.split("\\|");              
						            	   String name = (tokens.length > 0 ? tokens[0] : "");
						            	   if(UtilMethods.isSet(name)){
						            	   String pairvalue = (tokens.length > 1 ? tokens[1].trim() : name.trim());
						            	   String selected = "";
						                   String compareValue = (UtilMethods.isSet(value) ? value.toString() : (UtilMethods.isSet(defaultValue) ? defaultValue : ""));            
						                   if (compareValue != null && (compareValue.equals(pairvalue))) 
						                   {
						                       selected = "SELECTED";
						                   }
						                   //Added to support boolean values with: true/false - 1/0  - t/f
						                   else if((compareValue.equalsIgnoreCase("true") && (pairvalue.equalsIgnoreCase("true") || pairvalue.equalsIgnoreCase("1") || pairvalue.equalsIgnoreCase("t"))) ||
						                   		(compareValue.equalsIgnoreCase("false") && (pairvalue.equalsIgnoreCase("false") || pairvalue.equalsIgnoreCase("0") || pairvalue.equalsIgnoreCase("f"))))
						                   {
						                   	selected = "SELECTED";
						                   }
				                  		%>
											<option value="<%=pairvalue%>" <%=selected%>><%=name%></option>
					                 	<%}%>
									<%}%>
				              	</select>
							
							 
	           			<%}else if (f.getFieldType().equals(Field.FieldType.MULTI_SELECT.toString())) { %>
	           
	
			                <% 
								count++;
				                String[] pairs = fieldValues.split("\r\n");
							%>
					   
								<select multiple="multiple" size="<%= pairs.length %>" name="<%=f.getFieldContentlet()%>MultiSelect" id="<%=f.getVelocityVarName()%>MultiSelect" onchange="update<%=f.getVelocityVarName()%>MultiSelect()" style="width: 200px;">
								  <%
								     for(int j = 0; j < pairs.length; j++) {
								       String pair = pairs[j];
								       String[] tokens = pair.split("\\|");                
								       String name = (tokens.length > 0 ? tokens[0] : "");
								       String pairvalue = (tokens.length > 1 ? tokens[1] : name);
								       String selected = "";
								       String separator = (j<pairs.length-1)?",":"";
								       String compareValue = (UtilMethods.isSet(value) ? value.toString() : (UtilMethods.isSet(defaultValue) ? defaultValue : ""));
								       if (UtilMethods.isSet(compareValue) && UtilMethods.isSet(pairvalue) && ((((String) compareValue).contains(pairvalue + separator) || 
								    		   ((String) compareValue).equals(pairvalue)))){
								           selected = "SELECTED";
								       } %>
								       <option value="<%=pairvalue%>" <%=selected%>><%=name%></option>
								  <%}%>
								</select>
		
								<input type="hidden" name="<%=f.getFieldContentlet()%>" id="<%=f.getVelocityVarName()%>" value=" "/> 
								<script type="text/javascript">
									function update<%=f.getVelocityVarName()%>MultiSelect() {
										var valuesList = "";
										var multiselect = $('<%=f.getVelocityVarName()%>MultiSelect');
										for(var i = 0; i < multiselect.options.length; i++) {
											if(multiselect.options[i].selected) {
												valuesList += multiselect.options[i].value + ",";
											}
										}
										$('<%=f.getVelocityVarName()%>').value = valuesList;
									}
									update<%=f.getVelocityVarName()%>MultiSelect();
								</script>
		              		
								
	          			<%}else if (f.getFieldType().equals(Field.FieldType.CHECKBOX.toString())) { %>
	
	      				
								 
						         <% 
					              count++;
					        	  String fieldName = f.getFieldContentlet();
					        	  String[] pairs = fieldValues.split("\r\n");
					        	  for (int j = 0; j < pairs.length; j++) {
					        		  String pair = pairs[j];
					                  String[] tokens = pair.split("\\|");            
					                  String name = (tokens.length > 0 ? tokens[0] : "");
					                  String pairValue = (tokens.length > 1 ? tokens[1] : name);
					                  String checked = "";
					                  if (UtilMethods.isSet(value)) {
					                      if ((((String) value).contains(pairValue + ",") || ((String) value).contains(pairValue)) && UtilMethods.isSet(pairValue)) {
					                          checked = "CHECKED";
					                      }
					                  } else {
					                      if (UtilMethods.isSet(defaultValue)
					                              && (defaultValue.contains("|" + pairValue)
					                                      || defaultValue.contains(pairValue + "|") || defaultValue
					                                      .equals(pairValue))) {
					                          checked = "CHECKED";
					                      }
					                  }
						          %> 
						          <div style="height:20px;vertical-align:middle;float:left;white-space:nowrap;margin-right:10px;">
						              <input type="checkbox" dojoType="dijit.form.CheckBox" name="<%=fieldName%>Checkbox" id="<%=fieldName + j%>Checkbox" value="<%=pairValue%>" <%=checked%>  onchange="update<%=f.getVelocityVarName()%>Checkbox()">
						              <label for="<%=fieldName + j%>Checkbox"><%=name%></label>
						          </div>
								<%}%>
								
	         					<input type="hidden" name="<%=fieldName%>" id="<%=f.getVelocityVarName()%>" value="<%=value%>">
	      
								<script type="text/javascript">
									function update<%=f.getVelocityVarName()%>Checkbox() {
										var valuesList = "";
										var checkbox = null;
										<% for (int j = 0; j < pairs.length; j++) { %>
											checkbox = $('<%=fieldName + j%>Checkbox');
											if(checkbox.checked) {
												valuesList += checkbox.value + ",";
											}
										<%}%>
										$('<%=f.getVelocityVarName()%>').value = valuesList;
									}
									update<%=f.getVelocityVarName()%>Checkbox();
								</script>
	         				
	     				<%}else if (f.getFieldType().equals(Field.FieldType.TAG.toString())) { %>
							 <% 
						    	 count++;
						         String tagJSFunction = "suggestTagsForSearch(this, '" + f.getVelocityVarName() + "suggestedTagsDiv');";
						         String textValue = UtilMethods.isSet(value) ? (String) value : (UtilMethods.isSet(defaultValue) ? defaultValue : "");     
						     %>
						 
			         	
						         
						         <!-- display -->
						           <div id="<%=f.getVelocityVarName()%>Wrapper">
						             <div style="float:left;">
						                <textarea dojoType="dijit.form.Textarea" name="<%=f.getFieldContentlet()%>" id="<%=f.getVelocityVarName()%>" onkeyup="<%= tagJSFunction %>" style="width:250px;min-height:100px;"><%=textValue%></textarea>
						             </div>
						             <div class="suggestedTagsWrapper" id="<%=f.getVelocityVarName()%>suggestedTagsWrapper">
						                <%= LanguageUtil.get(pageContext, "Suggested-Tags") %>:<br />
						                <div id="<%=f.getVelocityVarName()%>suggestedTagsDiv" class="suggestedTags"></div>
						            </div>
						            <div class="clear"></div>
						          </div>
						        <!-- end display --> 
			        		
					
		     			<%}else if (f.getFieldType().equals(Field.FieldType.DATE.toString()) || 
							f.getFieldType().equals(Field.FieldType.TIME.toString()) || 
							f.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
							
							SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm");
							SimpleDateFormat df2 = new SimpleDateFormat("MM/dd/yyyy");
							Date dateValue = null;
							Date auxDate = new Date();
							if(value instanceof String && value!=null && !value.equals("")) {
							dateValue = df.parse((String) value);
							auxDate = dateValue;
							} else if( value!=null && !value.equals("")) {
							dateValue = (Date)value;
							auxDate = dateValue;
							}
							
							int[] monthIds = CalendarUtil.getMonthIds();
							String[] months = CalendarUtil.getMonths(locale);
							GregorianCalendar cal = new GregorianCalendar();
							cal.setTime((Date) auxDate);
							int dayOfMonth = cal.get(GregorianCalendar.DAY_OF_MONTH);
							int month = cal.get(GregorianCalendar.MONTH);
							int year = cal.get(GregorianCalendar.YEAR);
							int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH); %>
	
			 
						      <input type="hidden" id="<%=f.getFieldContentlet()%>" name="<%=f.getFieldContentlet()%>" value="<%= dateValue!=null?df.format(dateValue):"" %>" /> 
						        <%if (f.getFieldType().equals(Field.FieldType.DATE.toString())
							 			    || f.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) { %> 
							    <input type="text" 
								  value="<%= dateValue!=null?df2.format(dateValue):""  %>"
							 	  onChange="updateDate('<%=f.getFieldContentlet()%>');"
							 	  dojoType="dijit.form.DateTextBox" 
							 	  name="<%=f.getFieldContentlet()%>Date" 
							 	  id="<%=f.getFieldContentlet()%>Date" 
							 	  style="width:100px;">
						       <%}%>
						 
						        <%if (f.getFieldType().equals(Field.FieldType.TIME.toString())
								             || f.getFieldType().equals(Field.FieldType.DATE_TIME.toString())) {
							
							    String hour = (cal.get(GregorianCalendar.HOUR_OF_DAY) < 10) ? "0"+cal.get(GregorianCalendar.HOUR_OF_DAY) : ""+cal.get(GregorianCalendar.HOUR_OF_DAY);
					            String min = (cal.get(GregorianCalendar.MINUTE) < 10) ? "0"+cal.get(GregorianCalendar.MINUTE) : ""+cal.get(GregorianCalendar.MINUTE);%>
							    <input type="text" id='<%=f.getFieldContentlet()%>Time'
								  name='<%=f.getFieldContentlet()%>Time'
								  onChange="updateDate('<%=f.getFieldContentlet()%>');"
								  value='T<%=hour+":"+min%>:00'
					        	  
								  dojoType="dijit.form.TimeTextBox" style="width: 90px;" <%=f.isReadOnly()?"disabled=\"disabled\"":""%>/>
						
						        <%}%> 
							
								
	     
						<%}else if(f.getFieldType().equals(Field.FieldType.CATEGORY.toString())) { 
	   						CategoryAPI catAPI = APILocator.getCategoryAPI();
	       					String[] selectedCategories = null;
	       					Object catValue = request.getParameterValues("categories");
	       					if(catValue==null){
	     	 					catValue = request.getAttribute("categories");
	       					}
	       					if (UtilMethods.isSet(catValue)) {
	     	  					String[] categoriesArr = (String[])catValue;
	           					selectedCategories =  new String[categoriesArr.length];
	           					int i = 0;
	           					for(String cat: categoriesArr){
	                				if(cat != null){
	                    				selectedCategories[i] = cat;
	                				}
	                				i++;
	           					}
	         				} else {
	     						selectedCategories = f.getDefaultValue().split("\\|");
								int i = 0;
								for(String selectedCat: selectedCategories){
									if(selectedCat!=null && !selectedCat.equals("")){
											Category selectedCategory = catAPI.findByName(selectedCat, user, false);
											selectedCategories[i] = String.valueOf(selectedCategory.getInode());
									}
								}
								i++; 
							}
	       					try { 
								Category category = catAPI.find(f.getValues(), user, false); 
								if(category != null) { 
									List<Category> children = catAPI.getChildren(category, false, user, false); 
									if (children.size() >= 1 && catAPI.canUseCategory(category, user, false)) { 
										String catOptions = UtilHTML.getSelectCategories(category, 1, selectedCategories, user, false); 
										if(catOptions.length() > 1) { 
											count++; %>
											<span style="padding:0 15px 0 0;">
												<select multiple="true" name="categories" id="categoriesSelect" style="width:200px;visibility=visible;">
													<%= catOptions %>
												</select>
											</span>
										<%}
									}
								}
	         					}catch (DotSecurityException e) { 
	 								Logger.debug(this, "User don't have permissions to edit this category");     	
								}      
							}%> <%-- THE END OF THE BIG FRIGGEN IF FIELD TYPE --%>
						</td>
				<% if(count>0 && count % numTd ==0){ %>
					</tr><tr>
				<% } %>
			<% } %><%-- THE END OF THE BIG FRIGGEN FOR LOOP--%>
					
			<% if(count % numTd != 0){ %>
				<td colspan=8>&nbsp;</td>
			<% } %>
		</tr>
	</table>

				
	<div class="buttonRow">
		<button dojoType="dijit.form.Button" onClick="submitfm();" iconClass="searchIcon">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Search")) %>
		</button>
		<button dojoType="dijit.form.Button" id="clearButtonHost" onClick="clearHostSearch();" iconClass="resetIcon">
			<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Clear-Search")) %>
		</button>
	</div>
</div>
</form>

	<table class="listingTable">
		<thead>
			<tr>
				<th nowrap>
					<a href="javascript:orderHosts('contentlet.title','desc');"><%= LanguageUtil.get(pageContext, "Host") %></a>
				</th>
				<th nowrap>
					<a href="javascript:orderHosts('status','desc');"><%= LanguageUtil.get(pageContext, "Status") %></a>
				</th>
				<% for(com.dotmarketing.portlets.structure.model.Field field: fields){ %>
					<%if(field.isListed() ){ %>
					   	<%String fieldVelName = "contentlet."+field.getFieldContentlet(); %>
						<th nowrap>
							<a href="javascript:orderHosts('<%= fieldVelName%> ','desc');"><%= field.getFieldName() %></a>
						</th>	
				    <% } %>
				<% } %>   
				<th nowrap>
					<a href="javascript:orderHosts('totalpageviews','desc');"><%= LanguageUtil.get(pageContext, "Page-Views") %></a>
				</th>
			</tr>
		</thead>
		<tbody id="hosts"></tbody>
	</table>
	<div id="footer" style="text-align:center;"></div>
	<div id="popups"></div>
	<div id="hostContextMenues"></div>
</div>
<%}else{ %>
<%@ include file="/html/portlet/ext/dashboard/not_licensed.jsp" %>
<% }%>
</liferay:box>