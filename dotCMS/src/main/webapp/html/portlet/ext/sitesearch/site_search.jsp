<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
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
<%@page import="com.dotcms.repackage.javax.portlet.WindowState"%>
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
<%if(LicenseUtil.getLevel() < LicenseLevel.STANDARD.level){ %>
	<div class="portlet-wrapper">
	
		<div class="subNavCrumbTrail">
			<ul id="subNavCrumbUl">
				<li class="lastCrumb">
					<a href="#" ><%=LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title.site-search")%></a>
				</li>

			</ul>
			<div class="clear"></div>
		</div>
	   <style>
	       .wrapper{background:url(/html/images/skin/sitesearch-promo.png) no-repeat 0 0;height:600px;margin:0 auto;}
	       .content{position:fixed;left:50%;top:50%;margin:-200px 0 0 -300px;width:600px;background:#333;opacity:.85;color:#fff;padding:20px 20px 35px 20px;-moz-border-radius: 15px;-webkit-border-radius: 15px;-moz-box-shadow:0px 0px 15px #666;-webkit-box-shadow:0px 0px 15px #666;}
	       .content h2{font-size:200%;}
	       .content p{margin:0;}
	       .content ul{margin:5px 0 25px 15px;padding:0 0 0 10px;list-style-position:outside; list-style:decimal;}
	       .content li{list-style-position:outside; list-style:disc;}
	       .content a{color:#fff;}
	       #mainTabContainer {display:none;}
	   </style>
	   <div class="greyBg"></div>
	   <div class="wrapper">
	       <div class="content">
	           <h2><%= LanguageUtil.get(pageContext, "Sitesearch") %></h2>
	           <p><%= LanguageUtil.get(pageContext, "Sitesearch-Not-Licensed") %></p>
	       </div>
	   </div>
	</div>

<%return;}%>




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
		newRow.id = hostId;
		var cell0 = newRow.insertCell(0);
		var cell1 = newRow.insertCell(1);
		var anchor = document.createElement("a");
		anchor.href= 'javascript:deleteHost('+'"'+ hostId +'"'+');';
		anchor.innerHTML = '<span class="deleteIcon"></span>';
		cell0.className = 'job-scheduler__host-list-actions';
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

function doCloseIndex(indexName){
    
    var xhrArgs = {
    
        url: "/DotAjaxDirector/com.dotmarketing.portlets.cmsmaintenance.ajax.IndexAjaxAction/cmd/closeIndex/indexName/" + indexName,

        handleAs: "text",
        handle : function(dataOrError, ioArgs) {
            if (dojo.isString(dataOrError)) {
                if (dataOrError.indexOf("FAILURE") == 0) {
                    showDotCMSSystemMessage(dataOrError, true);
                } else {
                    showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Index-Closed")%>", true);
                    refreshIndexStats();
                }
            } else {
                showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Request-Failed")%>", true);
            }
        }
    };
    dojo.xhrPost(xhrArgs);

}

function doOpenIndex(indexName){
    
    var xhrArgs = {
    
        url: "/DotAjaxDirector/com.dotmarketing.portlets.cmsmaintenance.ajax.IndexAjaxAction/cmd/openIndex/indexName/" + indexName,

        handleAs: "text",
        handle : function(dataOrError, ioArgs) {
            if (dojo.isString(dataOrError)) {
                if (dataOrError.indexOf("FAILURE") == 0) {
                    showDotCMSSystemMessage(dataOrError, true);
                } else {
                    showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext, "Index-Opened")%>", true);
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
	dialog.show();
}

function restoreUploadCompleted() {
	hideRestoreIndex();
}


dojo.ready(function() {
	/*dojo.require("dojox.form.Uploader");
    dojo.require("dojox.embed.Flash");
    if(dojox.embed.Flash.available){
      dojo.require("dojox.form.uploader.plugins.Flash");
    }else{
      dojo.require("dojox.form.uploader.plugins.IFrame");
    }*/
	if(dojo.isIE && dojo.isIE<10) dojo.require("dojox.form.uploader.plugins.IFrame");
	else dojo.require("dojox.form.uploader.plugins.HTML5");
});

function connectUploadEvents() {
	var uploader=dijit.byId('restoreIndexUploader');
	dojo.connect(uploader, "onChange", function(dataArray){
        dojo.forEach(dataArray, function(data){
               dojo.byId("uploadFileName").innerHTML=data.name;
        });
	}); 
	dojo.connect(uploader, "onComplete", function(dataArray) {
        hideRestoreIndex();
        showDotCMSSystemMessage("Upload Complete. Index Restores in background");
    });
}

function doCreateSiteSearch(alias,number) {
	
	if(!number || !alias){
		return;
	}

	var shards = parseInt(number);
	if(shards <1){
		return;
	}

	if(/[^a-zA-Z0-9-_]/.test(alias.split(/\b\s+/)[0].trim())) {
		showDotCMSErrorMessage("<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Invalid-Index-Alias")) %>");
		return;
	}

	var xhrArgs = {
       url: "/DotAjaxDirector/com.dotmarketing.sitesearch.ajax.SiteSearchAjaxAction/cmd/createSiteSearchIndex/shards/" + shards +"/alias/"+alias,
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






function runNow(action) {
	//var value = dijit.byId(action).getValue();
	let incremental = dijit.byId('incremental');
	if(action === 'now'){
		dojo.query('.showScheduler').style({display:"none"});
		dojo.query('.showRunNow').style({display:""});
		dijit.byId("QUARTZ_JOB_NAME").setValue("<%=SiteSearchAPI.ES_SITE_SEARCH_EXECUTE_JOB_NAME%>");

		incremental.attr('disabled',true);
		incremental.attr('checked',false);
	}
	else if (action === 'schedule'){
		dojo.query('.showScheduler').style({display:""});
		dojo.query('.showRunNow').style({display:"none"});
		dijit.byId("QUARTZ_JOB_NAME").setValue("");
		incremental.attr('disabled',false)
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
	
	if(dojo.query("#sitesearch input[name='langToIndex']:checked").length==0) {
		showDotCMSErrorMessage("<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Choose-a-Language")) %>");
		return;
	}

	//Based on the error invalid_alias_name_exception returned by the ES
	//Alias must not contain the following characters [ , \", *, \\, <, |, ,, >, /, ?]"}]
	let indexAlias = dojo.byId("indexAlias").value;
	if( !indexAlias || indexAlias === "" || /[^a-zA-Z0-9-_]/.test(indexAlias.split(/\b\s+/)[0].trim())) {
		showDotCMSErrorMessage("<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Invalid-Index-Alias")) %>");
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
					//This line is not necessary there is another function refreshing teh jobs stats.
					// this line throws "CancelError: Request canceled" if is enable
					//refreshJobStats();
				}
			}
		});

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
	        // We'll 404 in the demo, but that's com.dotcms.repackage.jruby.okay.  We don't have a 'postIt' service on the
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

	x.containerNode.style.height = "auto";
}

function refreshJobStats(){
	var x = dijit.byId("jobStatsCp");
	var y =Math.floor(Math.random()*1123213213);
	x.attr( "href","/html/portlet/ext/sitesearch/site_search_job_stats.jsp?r=" + y  );
	dojo.byId("crumbTitleSpan").innerHTML="<%= LanguageUtil.get(pageContext, "View-All-Jobs") %>";

	x.containerNode.style.height = "auto";
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

	x.containerNode.style.height = "auto";
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
	dojo.byId("crumbTitleSpan").innerHTML="<%= LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title.jobs") %>";
	myJobName=null;

	x.containerNode.style.height = "auto";
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

function showNewIndexDialog() {
	dijit.byId('createIndexAlias').attr('value','');
	dijit.byId('createIndexNumShards').attr('value','2');
	dijit.byId('createIndexDialog').show()
} 

function refreshAuditData(jobId,offset,limit) {
	var url="/html/portlet/ext/sitesearch/site_search_audit.jsp";
	if(jobId) {
		url+="?jobId="+jobId;
		if(offset)
			url+="&offset="+offset;
		if(limit)
			url+="&limit="+limit;
	}

	var x = dijit.byId("auditCp");
	x.attr("href",url);
	x.containerNode.style.height = "auto";
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
			  	if(selectedTab.id=="auditTabCp") {
			  		refreshAuditData();
			  	}
		});
   	refreshIndexStats();
	enableJobsProgressUpdate();
});

function enableJobsProgressUpdate() {
	setInterval(function() {
		var tab =dijit.byId("mainTabContainer");
		selectedTab = tab.selectedChildWidget;
		if(selectedTab.id =="jobTabCp")
		    jobsProgressUpdate();
	},5000);
}

function jobsProgressUpdate() {
	
	var xhrArgs = {
       url: "/DotAjaxDirector/com.dotmarketing.sitesearch.ajax.SiteSearchAjaxAction/cmd/getJobProgress/" ,
       handleAs: "json",
       load : function(dataOrError, ioArgs) {
           if (dojo.isString(dataOrError) && dataOrError.indexOf("FAILURE") == 0) {
               showDotCMSSystemMessage(dataOrError, true);
           } else {
        	   var refresh=false;
        	   if(dojo.query("div.pb").length!=dataOrError.length) {
                   refresh = true;
               } else {

                   let p = null;
                   for (let i = 0; i < dataOrError.length; ++i) {

                       p = dataOrError[i];

                       if (!refresh) {
                           if (dojo.query("tr[jobname='" + p.jobname + "']").length) {
                               if (p.progress != -1) {
                                   // job in progress. Lets show the progress bar
                                   dojo.query("tr[jobname='" + p.jobname + "'] .deleteIcon").addClass("hidden");
                                   dojo.query("tr[jobname='" + p.jobname + "'] .pb").removeClass("hidden");
                                   dijit.byNode(dojo.query("tr[jobname='" + p.jobname + "'] .pb")[0]).set("value", p.progress);
                                   dijit.byNode(dojo.query("tr[jobname='" + p.jobname + "'] .pb")[0]).set("maximum", p.max);
                               }
                               else {
                                   // job not running. Lets show the delete icon
                                   dojo.query("tr[jobname='" + p.jobname + "'] .deleteIcon").removeClass("hidden");
                                   dojo.query("tr[jobname='" + p.jobname + "'] .pb").addClass("hidden");
                               }
                           }
                           else {
                               // we don't know that job. Lets refresh the whole thing
                               refresh = true;
                           }
                       }
                   }
               }

               if(refresh)
            	   refreshJobStats();
           }
       }
    };
    dojo.xhrPost(xhrArgs);
}

</script>

<style type="text/css">
	
	.highlight td {
	    background: #94BBFF;
	    color: white !important;
	}
	.hidden {
	   display: none;
	}
</style>


<span dojoType="dotcms.dojo.data.HostReadStore" jsId="HostStore"></span>


<div class="portlet-main">

	<div class="subNavCrumbTrail">
		<ul id="subNavCrumbUl">
			<li>
				<a href="#" onclick="refreshJobsListingPane();"><%=LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title.site-search")%></a>
			</li>
			<li class="lastCrumb"><span id="crumbTitleSpan"></span></li>
		</ul>
		<div class="clear"></div>
	</div>
	


	<div id="mainTabContainer" dolayout="false" dojoType="dijit.layout.TabContainer">

		<div dojoType="dijit.layout.ContentPane" id="indexTabCp" title="<%= LanguageUtil.get(pageContext, "Indices") %>">
			<div dojoType="dojox.layout.ContentPane" id="indexStatsCp" style="height:auto"></div>
		</div>
		
		<div dojoType="dijit.layout.ContentPane" id="indexTestTabCp" title="<%= LanguageUtil.get(pageContext, "Search") %>">
			<div dojoType="dojox.layout.ContentPane" id="indexTestCp"></div>
		</div>
		<div id="jobTabCp" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "View-All-Jobs") %>">
			<div dojoType="dojox.layout.ContentPane" id="jobStatsCp" style="height:auto"></div>
		</div>
		
		<div id="scheduleTabCp" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title.jobs") %>">
			<div style="overflow-y: auto;" dojoType="dojox.layout.ContentPane" id="scheduleCp" style="height:auto"></div>
		</div>
		
		<div id="auditTabCp" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "sitesearch-audit-tab") %>">
            <div style="overflow-y: auto;" dojoType="dojox.layout.ContentPane" id="auditCp" style="height:auto"></div>
        </div>
		
	</div>

</div>

