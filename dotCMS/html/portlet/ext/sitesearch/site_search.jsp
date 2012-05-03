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


<%


String successMsg = LanguageUtil.get(pageContext, "schedule-site-search-success") ;
String error = "";

boolean success = false;
%>




<script type="text/javascript">
dojo.require("dijit.form.TextBox");
dojo.require("dijit.form.NumberTextBox");
dojo.require('dotcms.dojo.data.HostReadStore');
dojo.require("dijit.form.Form");
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






function doDownloadIndex(indexName){
	window.location="/DotAjaxDirector/com.dotmarketing.portlets.cmsmaintenance.ajax.IndexAjaxAction/cmd/downloadIndex/indexName/" + indexName;
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
	
	if(!confirm("<%=LanguageUtil.get(pageContext, "Make-this-index-default")%>")){
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
					showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Index-Made-Default")%>", true);
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
	
	if(!confirm("<%=LanguageUtil.get(pageContext, "Make-this-index-not-default")%>")){
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






function runNow() {
	var runNow = dijit.byId("whenToRunNow").getValue();

	if(runNow){
		
		dojo.query('.showScheduler').style({display:"none"});
		dojo.query('.showRunNow').style({display:""});
		dijit.byId("QUARTZ_JOB_NAME").setValue("<%=SiteSearchAPI.ES_SITE_SEARCH_EXECUTE_JOB_NAME%>");
	}
	else{
		dojo.query('.showScheduler').style({display:""});
		dojo.query('.showRunNow').style({display:"none"});
		dijit.byId("QUARTZ_JOB_NAME").setValue("");
	}


}

function scheduleJob() {

	submitSchedule();

}



function submitSchedule() {
	var runNow = dijit.byId("whenToRunNow").getValue();
	if(runNow){
		dijit.byId("cronExpression").required=false;
		
		
		
	}else{
		
		dijit.byId("cronExpression").required=true;
	}
	
	var myForm = dijit.byId("sitesearch");

	
	var hosts = dojo.query("[name$=\"indexhost\"]");
	if(hosts ==undefined || hosts =="" && ! (dijit.byId("indexAll").checked)){
		dijit.byId('hostSelector').focus();
		
		
		
		showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "select-hosts-to-index")%>", true);
		return;
	}
	
	
	
	if (myForm.validate()) {
		dojo.xhrPost({
			form : "sitesearch",
			preventCache:true,
			
			timeout : 30000,
			handle : function(dataOrError, ioArgs) {
				if (dojo.isString(dataOrError) && dataOrError) {
					
					if (dataOrError.indexOf("FAILURE") == 0) {

						showDotCMSSystemMessage(dataOrError, true);
					} else {
						
					}
				} else {

					showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "message.Scheduler.saved")%>", false);
					var tabs =dijit.byId("mainTabContainer");
					var pane = dijit.byId("jobTabCp");
					tabs.selectChild(pane);
					refreshJobStats()
				}
			}
		});

	}
}






function updateReplicas(indexName,currentNum){
	var number=prompt("<%=LanguageUtil.get(pageContext, "Update-Replicas-Index")%> for index:\n\n" + indexName, currentNum);
	if(!number){return;}
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
	if(confirm("<%=LanguageUtil.get(pageContext, "message.Scheduler.confirm.delete")%>")){
	var xhrArgs = {
       url: "/DotAjaxDirector/com.dotmarketing.sitesearch.ajax.SiteSearchAjaxAction/cmd/deleteJob/taskName/" + taskName ,
       handleAs: "text",
       handle : function(dataOrError, ioArgs) {
           if (dojo.isString(dataOrError)) {
               if (dataOrError.indexOf("FAILURE") == 0) {
                   showDotCMSSystemMessage(dataOrError, true);
               } else {
                   showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Task-Deleted")%>", true);
                   refreshJobStats();

               }
           } else {
               showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Request-Failed")%>", true);
           }
       }
    };
    dojo.xhrPost(xhrArgs);
	}
	
}


function dohighlight(id) {
	dojo.addClass(id,"highlight");
}

function undohighlight(id) {
    dojo.removeClass(id,"highlight");
}


function doTestSearch(){

	var x = dijit.byId("indexTestCp");
	var y =Math.floor(Math.random()*1123213213);

	var xhrArgs = {
	      form: dojo.byId("testSiteForm"),
	      handleAs: "text",
	      load: function(data){
	    	  //alert(data);
	    	  dojo.byId("siteSearchResults").innerHTML = data;
	        //dojo.byId("response").innerHTML = "Form posted.";
	      },
	      error: function(error){
	        // We'll 404 in the demo, but that's okay.  We don't have a 'postIt' service on the
	        // docs server.
	       dojo.byId("siteSearchResults").innerHTML = error;
	        //dojo.byId("response").innerHTML = "Form posted.";
	      }
	    }
	    var deferred = dojo.xhrPost(xhrArgs);
}

function changeIncludeExclude(){

		dijit.byId("paths").setDisabled(dijit.byId("includeAll").checked);
		if(!dijit.byId("includeAll").checked && dijit.byId("paths").getValue() =="/*"){
			dijit.byId("paths").setValue("");
			
		}
		else if(dijit.byId("includeAll").checked){
				
				dijit.byId("paths").setValue("/*");
		
		}
}


function changeIncremental(){
		var disable = dijit.byId("incremental").getValue();

		dijit.byId("startDateDate").setDisabled(disable);
		dijit.byId("startDateTime").setDisabled(disable);
		dijit.byId("endDateDate").setDisabled(disable);
		dijit.byId("endDateTime").setDisabled(disable);
}




function refreshIndexStats(){
	var x = dijit.byId("indexStatsCp");
	var y =Math.floor(Math.random()*1123213213);
	x.attr( "href","/html/portlet/ext/sitesearch/site_search_index_stats.jsp?r=" + y  );
	dojo.byId("crumbTitleSpan").innerHTML="<%= LanguageUtil.get(pageContext, "Indices") %>";
}

function refreshJobStats(){
	var x = dijit.byId("jobStatsCp");
	var y =Math.floor(Math.random()*1123213213);
	x.attr( "href","/html/portlet/ext/sitesearch/site_search_job_stats.jsp?r=" + y  );
	dojo.byId("crumbTitleSpan").innerHTML="<%= LanguageUtil.get(pageContext, "View-All-Jobs") %>";
}

var myJobName;
var myIndexName;

function refreshTestSearch(){
	var x = dijit.byId("indexTestCp");
	var y =Math.floor(Math.random()*1123213213);
	if(myIndexName == undefined || myIndexName.trim().length ==0){
		x.attr( "href","/html/portlet/ext/sitesearch/test_site_search.jsp?r=" + y  );
	}
	else{
		x.attr( "href","/html/portlet/ext/sitesearch/test_site_search.jsp?testIndex=" + myIndexName +"&r=" + y  );
	}
	
	dojo.byId("crumbTitleSpan").innerHTML="<%= LanguageUtil.get(pageContext, "Search") %>";
	myIndexName=null;
}

function refreshJobSchedule(){
	var x = dijit.byId("scheduleCp");
	var y =Math.floor(Math.random()*1123213213);
	
	if(myJobName == undefined || myJobName.trim().length ==0){
		
		x.attr( "href","/html/portlet/ext/sitesearch/site_search_job_schedule.jsp?r=" + y  );
	}
	else{
		x.attr( "href","/html/portlet/ext/sitesearch/site_search_job_schedule.jsp?jobName=" +myJobName +"&r=" + y  );
	}
	dojo.byId("crumbTitleSpan").innerHTML="<%= LanguageUtil.get(pageContext, "javax.portlet.title.EXT_SCHEDULER") %>";
	myJobName=null;
}

function showJobsListingPane(){

	var tabs =dijit.byId("mainTabContainer");
	var pane = dijit.byId("jobTabCp");
	tabs.selectChild(pane);

}


function showJobSchedulePane(jobName){
	myJobName=jobName;
	var tabs =dijit.byId("mainTabContainer");
	var pane = dijit.byId("scheduleTabCp");
	tabs.selectChild(pane);

}

function showSiteSearchPane(indexName){
	myIndexName=indexName;
	var tabs =dijit.byId("mainTabContainer");
	var pane = dijit.byId("indexTestTabCp");
	tabs.selectChild(pane);

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
			  	if(selectedTab.id =="indexTestTabCp"){
			  		refreshTestSearch();
			  	}
			  	if(selectedTab.id =="scheduleTabCp"){
			  		refreshJobSchedule();
			  	}
		});
   	refreshJobStats();
	
});	





</script>
<style type="text/css">
	.listingTable {
		width: 42.5%;
		font-size: 100%;
		border-top: 1px solid #d0d0d0;
	}
	.trIdxBuilding{
	background:#F8ECE0;
	}
	.trIdxActive{
	background:#D8F6CE;
	}
	.trIdxNothing td{
	color:#aaaaaa;
	
	}
	.trIdxNothing:hover,.trIdxActive:hover,.trIdxBuilding:hover {background:#e0e9f6 !important;}
	 #restoreIndexUploader {
	   width:200px !important;
	 }
	 #uploadProgress {
	   float: right;
	   display: none;
	 }
</style>


<span dojoType="dotcms.dojo.data.HostReadStore" jsId="HostStore"></span>


<div class="portlet-wrapper">

	<div class="subNavCrumbTrail">
		<ul id="subNavCrumbUl">
			<li>
				<a href="#" onclick="refreshJobsListingPane();"><%=LanguageUtil.get(pageContext, "javax.portlet.title.EXT_SCHEDULER")%></a>
			</li>
			<li class="lastCrumb"><span id="crumbTitleSpan"><%=LanguageUtil.get(pageContext, "javax.portlet.title.EXT_SCHEDULER")%></span></li>
		</ul>
		<div class="clear"></div>
	</div>
	


	<div id="mainTabContainer" dolayout="false" dojoType="dijit.layout.TabContainer">

		
		<div id="jobTabCp" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "View-All-Jobs") %>">
			<div dojoType="dojox.layout.ContentPane" id="jobStatsCp" style="min-height:700px"></div>
		</div>
		
		<div id="scheduleTabCp" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "javax.portlet.title.EXT_SCHEDULER") %>">
			<div dojoType="dojox.layout.ContentPane" id="scheduleCp" style="min-height:800px"></div>
		</div>
		
		<div dojoType="dijit.layout.ContentPane" id="indexTabCp" title="<%= LanguageUtil.get(pageContext, "Indices") %>">
			<div dojoType="dojox.layout.ContentPane" id="indexStatsCp" style="min-height:700px"></div>
		</div>
		
		<div dojoType="dijit.layout.ContentPane" id="indexTestTabCp" title="<%= LanguageUtil.get(pageContext, "Search") %>">
			<div dojoType="dojox.layout.ContentPane" id="indexTestCp" style="min-height:700px"></div>
		</div>
		
		
		
	</div>
	

		
</div>

