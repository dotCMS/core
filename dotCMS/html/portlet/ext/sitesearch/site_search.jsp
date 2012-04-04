<%@page import="java.net.URLEncoder"%>
<%@page import="com.dotmarketing.sitesearch.business.SiteSearchAPI"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@ include file="/html/common/init.jsp"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.util.PortletURLUtil"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="java.util.Calendar"%>
<%@page import="com.dotmarketing.util.DateUtil"%>
<%@page import="com.liferay.util.cal.CalendarUtil"%>
<%@page import="java.util.ArrayList"%>
<%@page import="javax.portlet.WindowState"%>
<%@page import="com.dotmarketing.business.Layout"%>
<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.beans.Inode"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.business.Versionable"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.quartz.QuartzUtils"%>
<%@page import="com.dotmarketing.quartz.CronScheduledTask"%>
<%@page import="com.dotmarketing.quartz.ScheduledTask"%>
<%@page import="com.dotmarketing.quartz.SimpleScheduledTask"%>
<%@page import="org.quartz.SchedulerException"%>
<%@page import="org.quartz.SimpleTrigger"%>
<%@page import="com.dotmarketing.sitesearch.job.SiteSearchJobProxy"%>

<%

SiteSearchAPI ssapi = APILocator.getSiteSearchAPI();
String submitURL = com.dotmarketing.util.PortletURLUtil.getRenderURL(request,null,null,"EXT_SITESEARCH");
List<Host> selectedHosts = new ArrayList<Host>();
String error = "";
String CRON_EXPRESSION = "";
String pathsToIgnore  = "";
String pathsToFollow  = "";

String extToIgnore    = "";
String port           = "";
boolean followQueryString = false;
boolean indexAll = false;
boolean showBlankCronExp = false;
boolean success = false;
String successMsg = LanguageUtil.get(pageContext, "schedule-site-search-success") ;
String[] indexHosts;

String QUARTZ_JOB_NAME =  "SiteSearch_" + APILocator.getContentletIndexAPI().timestampFormatter.format(new Date());


CRON_EXPRESSION = UtilMethods.webifyString(request.getParameter("CRON_EXPRESSION"));
pathsToIgnore  = UtilMethods.webifyString(request.getParameter("pathsToIgnore"));
extToIgnore    = UtilMethods.webifyString(request.getParameter("extToIgnore"));
indexAll = !UtilMethods.isSet(request.getParameter("indexAll"))?false:Boolean.valueOf((String)request.getParameter("indexAll"));
indexHosts = request.getParameterValues("indexhost");


List<String> indexes = ssapi.listIndices();



%>




<script type="text/javascript">
dojo.require("dijit.form.TextBox");
dojo.require("dijit.form.NumberTextBox");
dojo.require('dotcms.dojo.data.HostReadStore');

function checkAll() {
	var check = dijit.byId("checkAll").checked;
	dojo.query('input[type=checkbox]', document).forEach(function(tag){
		var id = tag.id;
		if(id != undefined && id.indexOf("indexhost") >-1){
			dijit.byId(id).setChecked(check);
		}
	});
}

function addNewHost() {

	if(dijit.byId('hostSelector').attr('value') == '') {
		return;
	}

	var hostId = dijit.byId('hostSelector').attr('value');
	var hostName = dijit.byId('hostSelector').attr('displayedValue');
	var table = document.getElementById('hostTable');
	var rowCount = table.rows.length;
	var row  = document.getElementById(hostId);

	if(row!=null){
	   alert('<%= LanguageUtil.get(pageContext, "host-already-selected") %>');
	}else{

    var nohosts = document.getElementById("nohosts");
	if(nohosts!=null){
		table.deleteRow(1);
    }


	var newRow = table.insertRow(table.rows.length);
	if((table.rows.length%2)==0){
        newRow.className = "alternate_1";
	}else{
		newRow.className = "alternate_2";
	}
	newRow.id = hostId;
	var cell0 = newRow.insertCell(0);
	var cell1 = newRow.insertCell(1);
	var anchor = document.createElement("a");
	anchor.href= 'javascript:deleteHost('+'"'+ hostId +'"'+');';
	anchor.innerHTML = '<span class="deleteIcon"></span>';
	cell0.appendChild(anchor);
	cell1.innerHTML = hostName;
	var input = document.createElement("input");
	input.type="hidden";
	input.name="indexhost";
	input.id="indexhost"+hostId;
	input.value=hostId;
	newRow.appendChild(input);
	
	}
	
}

function deleteHost(hostId) {

	var table = document.getElementById('hostTable');
	var row  = document.getElementById(hostId);
	if(row){
		try {
			 var rowCount = table.rows.length;
			 for(var i=0; i<rowCount; i++) {
				if(row.id==table.rows[i].id) {
					table.deleteRow(i);
					rowCount--;
					i--;
					if(rowCount <= 1) {
						var newRow = table.insertRow(rowCount);
						newRow.id="nohosts";
						newRow.innerHTML = '<td colspan="2"><div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "no-hosts-selected") %></div></td>';
						break;
					}
				}
			 }
		 }catch(e) {}
	}
}

function deleteAll(indexAll){
	var table = document.getElementById('hostTable');
	try {
		var rowCount = table.rows.length;
		 for(var i=rowCount-1; i>=0; i--) {
			table.deleteRow(i);
			rowCount--;
			if(rowCount <= 1) {
				 var newRow = table.insertRow(rowCount);
				 newRow.id="nohosts";
				 if(indexAll){
				     newRow.innerHTML = '<td colspan="2"><div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "all-hosts-selected") %></div></td>';
				 }else{
					 newRow.innerHTML = '<td colspan="2"><div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "no-hosts-selected") %></div></td>'; 
				 }
			     break;
		}
	  }
	 }catch(e) {}
}

function indexAll(checked){
	if(checked){
      if(confirm('<%= LanguageUtil.get(pageContext, "index-all-warning") %>')){
    	cleanUpIndexAll(checked);
	  }else{
		dijit.byId('indexAll').attr('checked',false);
	  }
	}else{
		cleanUpIndexAll(checked);
	}
}

function cleanUpIndexAll(checked){
	deleteAll(checked);
	if(checked){
		dijit.byId('hostSelector').attr('disabled',true);
		dijit.byId('addHostButton').attr('disabled',true);
	}else{
		dijit.byId('hostSelector').attr('disabled',false);
		dijit.byId('addHostButton').attr('disabled',false);
	}

	
}

<%if(UtilMethods.isSet(error)){ %>
showDotCMSErrorMessage("<%=LanguageUtil.get(pageContext, error ) %>");
<%}
if(success){ %>
showDotCMSSystemMessage("<%=successMsg %>");
<%} %>

function submitfm(form) {

	var port = dijit.byId("port");
	if(port.isValid()){
	   submitForm(form);
    }else{
    	showDotCMSErrorMessage("<%=LanguageUtil.get(pageContext, "invalid.port.number" ) %>");
	}
}

function saveAndExecute(){

	var port = dijit.byId("port");
	if(port.isValid()){
	var form = document.getElementById('sitesearch');
	var saveAndExecute = document.getElementById('saveAndExecute');
	saveAndExecute.value="true";
	submitForm(form);	
	}else{
		showDotCMSErrorMessage("<%=LanguageUtil.get(pageContext, "invalid.port.number" ) %>");
	}
}


function resetSiteSearch(){

	if(confirm('<%= LanguageUtil.get(pageContext, "reset-sitesearch-warning") %>')){

	   var form = document.getElementById('sitesearch');
	   var resetSiteSearch = document.getElementById('resetSiteSearch');
	   resetSiteSearch.value="true";
	   submitForm(form);	
	}
}




function deleteIndex(indexName, live){
	
	if(!confirm("<%=LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-delete-this-index")%>")){
		return;
		
	}

	var xhrArgs = {
	
		url: "/DotAjaxDirector/com.dotmarketing.portlets.cmsmaintenance.ajax.IndexAjaxAction/cmd/deleteIndex/indexName/" + indexName,

		handleAs: "text",
		handle : function(dataOrError, ioArgs) {
			if (dojo.isString(dataOrError)) {
				if (dataOrError.indexOf("FAILURE") == 0) {
					showDotCMSSystemMessage(dataOrError, true);
				} else {
					showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Index-Deleted")%>", true);
					refreshIndexStats();
				}
			} else {
				showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Request-Failed")%>", true);
			}
		}
	};
	dojo.xhrPost(xhrArgs);

}

function deleteIndexCallback(data){

	refreshIndexStats();
}


function refreshIndexStats(){
	var x = dijit.byId("indexStatsCp");
	var y =Math.floor(Math.random()*1123213213);
	x.attr( "href","/html/portlet/ext/sitesearch/site_search_index_stats.jsp?r=" + y  );
}

function refreshJobStats(){
	var x = dijit.byId("jobStatsCp");
	var y =Math.floor(Math.random()*1123213213);
	x.attr( "href","/html/portlet/ext/sitesearch/site_search_job_stats.jsp?r=" + y  );
}
function doDownloadIndex(indexName){
	

	window.location="/DotAjaxDirector/com.dotmarketing.portlets.cmsmaintenance.ajax.IndexAjaxAction/cmd/downloadIndex/indexName/" + indexName;
	
}

function doFullReindex(){
	
	
	var number=prompt("<%=LanguageUtil.get(pageContext, "Number-of-Shards")%> ", <%=Config.getIntProperty("es.index.number_of_shards", 4)%>);
	if(!number){
		return;
	}
	
	var shards = parseInt(number);
	if(shards <1){
		return;	
	}
	dojo.byId("numberOfShards").value = shards;
	
	dijit.byId('idxReindexButton').setDisabled(true);
	dijit.byId('idxShrinkBtn').setDisabled(true);
	submitform('<%=com.dotmarketing.util.WebKeys.Cache.CACHE_CONTENTS_INDEX%>');
	return false;
	
}

function doClearIndex(indexName){
	
	if(!confirm("<%=LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-clear-this-index")%>")){
		return;
		
	}

	var xhrArgs = {
	
		url: "/DotAjaxDirector/com.dotmarketing.portlets.cmsmaintenance.ajax.IndexAjaxAction/cmd/clearIndex/indexName/" + indexName,

		handleAs: "text",
		handle : function(dataOrError, ioArgs) {
			if (dojo.isString(dataOrError)) {
				if (dataOrError.indexOf("FAILURE") == 0) {
					showDotCMSSystemMessage(dataOrError, true);
				} else {
					showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Index-Cleared")%>", true);
					refreshIndexStats();
				}
			} else {
				showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Request-Failed")%>", true);
			}
		}
	};
	dojo.xhrPost(xhrArgs);

}
function doActivateIndex(indexName){
	
	if(!confirm("<%=LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-activate-this-index")%>")){
		return;
		
	}

	var xhrArgs = {
	
		url: "/DotAjaxDirector/com.dotmarketing.portlets.cmsmaintenance.ajax.IndexAjaxAction/cmd/activateIndex/indexName/" + indexName,

		handleAs: "text",
		handle : function(dataOrError, ioArgs) {
			if (dojo.isString(dataOrError)) {
				if (dataOrError.indexOf("FAILURE") == 0) {
					showDotCMSSystemMessage(dataOrError, true);
				} else {
					showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Index-Activated")%>", true);
					refreshIndexStats();
				}
			} else {
				showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Request-Failed")%>", true);
			}
		}
	};
	dojo.xhrPost(xhrArgs);
}
function doDeactivateIndex(indexName){
	
	if(!confirm("<%=LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-deactivate-this-index")%>")){
		return;
		
	}

	var xhrArgs = {
	
		url: "/DotAjaxDirector/com.dotmarketing.portlets.cmsmaintenance.ajax.IndexAjaxAction/cmd/deactivateIndex/indexName/" + indexName,

		handleAs: "text",
		handle : function(dataOrError, ioArgs) {
			if (dojo.isString(dataOrError)) {
				if (dataOrError.indexOf("FAILURE") == 0) {
					showDotCMSSystemMessage(dataOrError, true);
				} else {
					showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Index-Deactivated")%>", true);
					refreshIndexStats();
				}
			} else {
				showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Request-Failed")%>", true);
			}
		}
	};
	dojo.xhrPost(xhrArgs);

}

function hideRestoreIndex() {
    dijit.byId("restoreIndexDialog").hide();
}

function showRestoreIndexDialog(indexName) {
	dojo.byId("indexToRestore").value=indexName;
	var dialog=dijit.byId("restoreIndexDialog");
	dialog.set('title','Restore index '+indexName);
	dojo.byId("uploadFileName").innerHTML='';
	dijit.byId('uploadSubmit').set('disabled',false);
	dojo.query('#uploadProgress').style({display:"none"});
	connectUploadEvents();
	dojo.byId("uploadWarningLive").style.display="none";
	dojo.byId("uploadWarningWorking").style.display="none";
	dialog.show();
}

function doRestoreIndex() {
	if(dojo.byId("uploadFileName").innerHTML=='') {
		showDotCMSErrorMessage("<%=LanguageUtil.get(pageContext, "No-File-Selected")%>");
	}
	else {
		dijit.byId('uploadSubmit').set('disabled',true);
	    dojo.query('#uploadProgress').style({display:"block"});
	    dijit.byId("restoreIndexUploader").submit();
	}
}

function restoreUploadCompleted() {
	hideRestoreIndex();
}

dojo.ready(function() {
	dojo.require("dojox.form.Uploader");
    dojo.require("dojox.embed.Flash");
    if(dojox.embed.Flash.available){
      dojo.require("dojox.form.uploader.plugins.Flash");
    }else{
      dojo.require("dojox.form.uploader.plugins.IFrame");
    }
});

function connectUploadEvents() {
	var uploader=dijit.byId("restoreIndexUploader");
	dojo.connect(uploader, "onChange", function(dataArray){
		 dojo.forEach(dataArray, function(data){
			    dojo.byId("uploadFileName").innerHTML=data.name;
			    var uploadName=data.name;
			    var indexName=dojo.byId("indexToRestore").value;
			    
			    if(indexName.indexOf("working")==0 && uploadName.indexOf("working")!=0)
			    	dojo.byId("uploadWarningWorking").style.display="block";
			    else
			    	dojo.byId("uploadWarningWorking").style.display="none";
			    
			    if(indexName.indexOf("live")==0 && uploadName.indexOf("live")!=0)
			    	dojo.byId("uploadWarningLive").style.display="block";
                else
                	dojo.byId("uploadWarningLive").style.display="none";
		 });
	});
	dojo.connect(uploader, "onComplete", function(dataArray) {
           hideRestoreIndex();
           showDotCMSSystemMessage("Upload Complete. Index Restores in background");
    });
}

function doCreateSiteSearch() {
	
	
	var number=prompt("<%=LanguageUtil.get(pageContext, "Number-of-Shards")%> ", <%=Config.getIntProperty("es.index.number_of_shards", 4)%>);
	if(!number){
		return;
	}
	
	var shards = parseInt(number);
	if(shards <1){
		return;	
	
	}
	
	var xhrArgs = {
       url: "/DotAjaxDirector/com.dotmarketing.sitesearch.ajax.SiteSearchAjaxAction/cmd/createSiteSearchIndex/shards/" + shards ,
       handleAs: "text",
       handle : function(dataOrError, ioArgs) {
           if (dojo.isString(dataOrError)) {
               if (dataOrError.indexOf("FAILURE") == 0) {
                   showDotCMSSystemMessage(dataOrError, true);
               } else {
                   showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Index-Created")%>", true);
                   refreshIndexStats();

               }
           } else {
               showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Request-Failed")%>", true);
           }
       }
    };
    dojo.xhrPost(xhrArgs);
}

function scheduleJob() {
	
	
	var myForm = dijit.byId("sitesearch");

	if (myForm.validate()) {
		dojo.xhrPost({
			form : "sitesearch",
			preventCache:true,
			
			timeout : 30000,
			handle : function(dataOrError, ioArgs) {
				alert(dataOrError);
				if (dojo.isString(dataOrError) && dataOrError) {
					
					if (dataOrError.indexOf("FAILURE") == 0) {

						actionAdmin.saveError(dataOrError);
					} else {
					
						actionAdmin.saveSuccess(dataOrError);
					}
				} else {

					actionAdmin.saveError("<%=LanguageUtil.get(pageContext, "Unable-to-save-action")%>");

				}
			}
		});

	}
	
}


function updateReplicas(indexName,currentNum){

	var number=prompt("<%=LanguageUtil.get(pageContext, "Update-Replicas-Index")%> for index:\n\n" + indexName, currentNum);
	
	if(!number){
		return;
	}
	
	
	var replicas = parseInt(number);
	if(currentNum != replicas){
		
		var xhrArgs = {
				
				url: "/DotAjaxDirector/com.dotmarketing.portlets.cmsmaintenance.ajax.IndexAjaxAction/cmd/updateReplicas/indexName/" + indexName + "/replicas/" + replicas,
			
				handleAs: "text",
				handle : function(dataOrError, ioArgs) {
					if (dojo.isString(dataOrError)) {
						if (dataOrError.indexOf("FAILURE") == 0) {
							showDotCMSSystemMessage(dataOrError, true);
						} else {
							showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Replicas-Updated")%>", true);
							refreshIndexStats();
						}
					} else {
						showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Request-Failed")%>", true);
					}
				}
			};
			dojo.xhrPost(xhrArgs);
	}
}

function deleteJob(taskName){
	var xhrArgs = {
		       url: "/DotAjaxDirector/com.dotmarketing.sitesearch.ajax.SiteSearchAjaxAction/cmd/deleteJob/taskName/" + taskName ,
		       handleAs: "text",
		       handle : function(dataOrError, ioArgs) {
		           if (dojo.isString(dataOrError)) {
		               if (dataOrError.indexOf("FAILURE") == 0) {
		                   showDotCMSSystemMessage(dataOrError, true);
		               } else {
		                   showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Task-Deleted")%>", true);
		                   refreshIndexStats();

		               }
		           } else {
		               showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Request-Failed")%>", true);
		           }
		       }
		    };
		    dojo.xhrPost(xhrArgs);
	
}



function dohighlight(id) {
	dojo.addClass(id,"highlight");
}

function undohighlight(id) {
    dojo.removeClass(id,"highlight");
}


dojo.addOnLoad (function(){
		var tab =dijit.byId("mainTabContainer");
	   	dojo.connect(tab, 'selectChild',
		 function (evt) {
		 	selectedTab = tab.selectedChildWidget;
			  	if(selectedTab.id =="indexTabCp"){
			  		refreshIndexStats();
			  	}
			  	if(selectedTab.id =="jobTabCp"){
			  		refreshJobStats();
			  	}
		});
	
});	





</script>
<style type="text/css">
.listingTable {
	width: 42.5%;
	font-size: 100%;
	border-top: 1px solid #d0d0d0;
}
</style>
<div class="portlet-wrapper">
	<div id="mainTabContainer" dolayout="false" dojoType="dijit.layout.TabContainer">
		<div id="TabOne" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Schedule-New-Job") %>">
			<form dojoType="dijit.form.Form"  name="sitesearch" id="sitesearch" action="/DotAjaxDirector/com.dotmarketing.sitesearch.ajax.SiteSearchAjaxAction/cmd/scheduleJob" method="post">
			
			<dl>
				<dt><strong><%= LanguageUtil.get(pageContext, "job-name") %></strong>
				: 
				</dt>
				<dd><input name="QUARTZ_JOB_NAME" id="QUARTZ_JOB_NAME" type="text"
					dojoType='dijit.form.TextBox' style='width: 400px' 
					" value="<%=QUARTZ_JOB_NAME %>" size="200" /></dd>
				<dd>
				<dt><span class="required"></span><strong><%= LanguageUtil.get(pageContext, "select-hosts-to-index") %>:
				</strong> <a href="javascript: ;" id="hostsHintHook">?</a> <span
					dojoType="dijit.Tooltip" connectId="hostsHintHook" id="hostsHint"
					class="fieldHint"><%=LanguageUtil.get(pageContext, "hosts-hint") %></span>
				</dt>
			<dd>
			<span dojoType="dotcms.dojo.data.HostReadStore" jsId="HostStore"></span>
			<div class="selectHostIcon"></div>
			<select id="hostSelector" name=hostSelector" dojoType="dijit.form.FilteringSelect" 
				store="HostStore"  pageSize="30" labelAttr="hostname"  searchAttr="hostname" 
				invalidMessage="<%= LanguageUtil.get(pageContext, "Invalid-option-selected")%>" <%=indexAll?"disabled":"" %>>>
			</select>
			<button id="addHostButton" dojoType="dijit.form.Button" type="button" iconClass="plusIcon" onclick="addNewHost()" <%=indexAll?"disabled":"" %>><%= LanguageUtil.get(pageContext, "Add-Host") %></button>
			</dd>
				
				<table class="listingTable" id="hostTable">
					<tr>
					    <th nowrap style="width:60px;"><span><%= LanguageUtil.get(pageContext, "Delete") %></span></th>
						<th nowrap><%= LanguageUtil.get(pageContext, "Host") %></th>
					</tr>
			
			  <% 
			  if(!indexAll){
			    for (int k=0;k<selectedHosts.size();k++) {
			    	
			    	Host host = selectedHosts.get(k);
			    	
			    	boolean checked =  false;
			    	
			    	if(!host.isSystemHost()){
			   
			    String str_style = "";
			      if ((k%2)==0) {
			        str_style = "class=\"alternate_1\"";
			        }
			        else
			        {
			        str_style = "class=\"alternate_2\"";
			        }
			   %>
					<tr id="<%=host.getIdentifier()%>" <%=str_style %>>
					    <td nowrap>
					       	<a href="javascript:deleteHost('<%=host.getIdentifier()%>');"><span class="deleteIcon"></span></a>
					    </td>
						<td nowrap><%= host.getHostname() %></td>
						<td nowrap="nowrap" style="overflow:hidden; display:none; "> <input type="hidden"
							name="indexhost" id="indexhost<%= host.getIdentifier() %>"
							value="<%= host.getIdentifier() %>" /></td>
			
					</tr>
					<%} %>
					<%}
			        } %>
					<% if (indexAll || selectedHosts.size()==0) { %>
					<tr id= "nohosts">
						<td colspan="2">
						<div class="noResultsMessage"><%= indexAll?LanguageUtil.get(pageContext, "all-hosts-selected"):LanguageUtil.get(pageContext, "no-hosts-selected") %></div>
						</td>
					</tr>
					<% } %>
				</table>
				<br />
				<dd>
			<strong><%= LanguageUtil.get(pageContext, "index-all-hosts") %>
				: </strong>
			<input name="indexAll" id="indexAll"
					dojoType="dijit.form.CheckBox" type="checkbox" value="true"
					<%=!indexAll?"":"checked"%> onclick="indexAll(this.checked)" />
				</dd>
				
				<dt><strong><%= LanguageUtil.get(pageContext, "Index-Name") %>
				: </strong>
				</dt>
				<dd>
					<select id="indexName" name="indexName" dojoType="dijit.form.FilteringSelect">
					<option value=""><%= LanguageUtil.get(pageContext, "New-Index") %></option>
					<%for(String x : indexes){ %>
						<option value="<%=x%>"><%=x%> <%=(x.equals(APILocator.getIndiciesAPI().loadIndicies().site_search)) ? "(" +LanguageUtil.get(pageContext, "active") +") " : ""  %></option>
					<%} %>
					</select>
				</dd>
				
				<dt><strong><%= LanguageUtil.get(pageContext, "cron-expression") %>
				: </strong> <a href="javascript: ;" id="cronHintHook">?</a>
				</dt>
				<dd><input name="CRON_EXPRESSION" id="cronExpression" type="text"
					dojoType='dijit.form.TextBox' style='width: 200px'
					" value="<%=showBlankCronExp?"":CRON_EXPRESSION %>" size="10" />
					<p></p>
					 <div style="width: 350px; text-align: left;" id="cronHelpDiv" class="callOutBox2">
						<h3><%= LanguageUtil.get(pageContext, "cron-examples") %></h3>
						<span style="font-size: 88%;">
						<p></p>
				        <p><b><%= LanguageUtil.get(pageContext, "cron-once-an-hour") %>:</b> 0 0/60 * * * ?</p> 	
				        <p><b><%= LanguageUtil.get(pageContext, "cron-twice-a-day") %>:</b> 0 0 10-11 ? * *</p> 	
				        <p><b><%= LanguageUtil.get(pageContext, "cron-once-a-day-1am")%>:</b> 0 0 1 * * ?</p> 
						</span>
					</div>
				</dd>
				 
			
				<dt><strong><%= LanguageUtil.get(pageContext, "paths-to-ignore") %>
				: </strong> <a href="javascript: ;" id="pathsHintHook">?</a> <span
					dojoType="dijit.Tooltip" connectId="pathsHintHook" id="pathsHint"
					class="fieldHint"><%=LanguageUtil.get(pageContext, "paths-hint") %></span>
				</dt>
				<dd><input name="pathsToIgnore" id="pathsToIgnore" type="text"
					dojoType='dijit.form.TextBox' style='width: 400px'
					" value="<%=pathsToIgnore %>" size="200" /></dd>
					
				<dt><strong><%= LanguageUtil.get(pageContext, "paths-to-follow") %>
				: </strong> <a href="javascript: ;" id="pathsHintHook1">?</a> <span
					dojoType="dijit.Tooltip" connectId="pathsHintHook1" id="pathsHint1"
					class="fieldHint"><%=LanguageUtil.get(pageContext, "paths-hint") %></span>
				</dt>
				<dd><input name="pathsToIgnore" id="pathsToFollow" type="text"
					dojoType='dijit.form.TextBox' style='width: 400px'
					" value="<%=pathsToFollow %>" size="200" /></dd>
				<dd>

				
				<input type="hidden" name="saveAndExecute" id="saveAndExecute" value="false" />
				<input type="hidden" name="resetSiteSearch" id="resetSiteSearch" value="false" />
			
				<button dojoType="dijit.form.Button"
					id="saveButton" onClick="scheduleJob()"
					iconClass="saveIcon"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Schedule")) %>
				</button>
				<button dojoType="dijit.form.Button"
					id="saveAndExecuteButton" onClick="scheduleJob();"
					iconClass="saveIcon"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Execute")) %>
				</button>
				</dd>
			
			</dl>
			</form>
		</div>
		
		<div id="jobTabCp" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Current-Jobs") %>">
			<div dojoType="dijit.layout.ContentPane" id="jobStatsCp" style="height:600px"></div>
		</div>
		
		
		<div dojoType="dijit.layout.ContentPane" id="indexTabCp" title="<%= LanguageUtil.get(pageContext, "Index") %>">
			<div dojoType="dijit.layout.ContentPane" id="indexStatsCp" style="height:600px"></div>
		</div>
	</div>
	

		
</div>

