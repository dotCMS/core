<%@page import="com.dotcms.listeners.SessionMonitor"%>
<%@page import="com.dotcms.content.elasticsearch.business.ContentletIndexAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.content.elasticsearch.business.ESContentletIndexAPI"%>
<%@page import="com.dotmarketing.portlets.structure.factories.StructureFactory"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotcms.content.elasticsearch.business.ESIndexAPI"%>
<%@page import="java.lang.management.RuntimeMXBean"%>
<%@page import="java.lang.management.ManagementFactory"%>
<%@page import="com.dotmarketing.business.DotGuavaCacheAdministratorImpl"%>
<%@page import="com.dotmarketing.cache.H2CacheLoader"%>
<%@page import="com.dotmarketing.business.DotJBCacheAdministratorImpl"%>
<%@page import="com.dotmarketing.business.CacheLocator"%>
<%@ page import="java.util.Calendar"%>
<%@ page import="javax.portlet.WindowState"%>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@ page import="com.dotmarketing.portlets.cmsmaintenance.struts.CmsMaintenanceForm"%>
<%@ page import="java.util.List"%>

<%@ include file="/html/portlet/ext/cmsmaintenance/init.jsp"%>

<%

DateFormat modDateFormat = java.text.DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT, locale);

java.util.Map params = new java.util.HashMap();
params.put("struts_action",	new String[] { "/ext/cmsmaintenance/view_cms_maintenance" });
String referer = java.net.URLEncoder.encode(com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.NORMAL.toString(), params), "UTF-8");

CmsMaintenanceForm CMF = (com.dotmarketing.portlets.cmsmaintenance.struts.CmsMaintenanceForm) request.getAttribute("CmsMaintenanceForm");
session.setAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION, true);
ContentletIndexAPI idxApi = APILocator.getContentletIndexAPI();
List<Structure> structs = StructureFactory.getStructures();
%>
<script type="text/javascript" src="/dwr/engine.js"></script>
<script type="text/javascript" src="/dwr/util.js"></script>
<script type='text/javascript' src='/dwr/interface/CMSMaintenanceAjax.js'></script>
<script type="text/javascript" src="/dwr/interface/ThreadMonitorTool.js"></script>
<script type="text/javascript" src="/dwr/interface/UserSessionAjax.js"></script>

<script language="Javascript">
dojo.require("dijit.Editor");

var view = "<%= java.net.URLEncoder.encode("(working=" + com.dotmarketing.db.DbConnectionFactory.getDBTrue() + ")","UTF-8") %>";

var isIndexTabShownOnReindexing = false;

function submitform(cacheName)
{
	form = document.getElementById('cmsMaintenanceForm');
	var action = "<portlet:actionURL>";
	action += "<portlet:param name='struts_action' value='/ext/cmsmaintenance/view_cms_maintenance' />";
	action += "<portlet:param name='cmd' value='cache' />";
	action += "</portlet:actionURL>";
	form.action = action;
	form.cmd.value="cache";
	form.cmd.value="cache";
	cacheNameID = document.getElementById('cacheName');
	cacheNameID.value = cacheName;
	form.submit();
}

var stillInReindexation = false;
function checkReindexationCallback (response) {
	var inFullReindexation = response['inFullReindexation'];
	var contentCountToIndex = response['contentCountToIndex'];
	var lastIndexationProgress = response['lastIndexationProgress'];
	var currentIndexPath = response['currentIndexPath'];
	var newIndexPath = response['newIndexPath'];
	var lastIndexationStartTime = ' ';
	var lastIndexationEndTime = ' ';

	var reindexationInProgressDiv = document.getElementById("reindexationInProgressDiv");
	if (inFullReindexation) {
		if(!isIndexTabShownOnReindexing){
			dijit.byId('mainTabContainer').selectChild('indexTabCp');
			isIndexTabShownOnReindexing = true;
		}
		dojo.query(".indexActionsDiv").style("display","none");


		reindexationInProgressDiv.style.display = "";


		var bar = dijit.byId("reindexProgressBar");
		if(bar != undefined){
		    dijit.byId("reindexProgressBar").update({
		      maximum: contentCountToIndex,
		      progress: lastIndexationProgress
		    });
		}
		stillInReindexation = true;
		var indexationProgressDiv = document.getElementById("indexationProgressDiv");
		indexationProgressDiv.innerHTML = "<%= LanguageUtil.get(pageContext,"Reindex-Progress") %>: " + lastIndexationProgress + " / " + contentCountToIndex + " ";
	} else {
		dojo.query(".indexActionsDiv").style("display","");
		reindexationInProgressDiv.style.display = "none";
		if(stillInReindexation){
			stillInReindexation = false;
			refreshIndexStats();

		}
	}
	setTimeout("checkReindexation()", 5000);
}

function checkReindexation () {

	CMSMaintenanceAjax.getReindexationProgress(checkReindexationCallback);
}

function stopReIndexing(){
	CMSMaintenanceAjax.stopReindexation(checkReindexationCallback);
}

function optimizeCallback() {
	showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext,"Optimize-Done")%>");
}

function checkFixAssetCallback (responser) {
	$("fixAssetsButton").disabled = false;

	var fixAssetInfoDiv = document.getElementById("fixAssetInfo");
	var fixAssetTimeDiv = document.getElementById("fixAssetTime");
	var infodiv = "";

	if(responser != undefined){

		for(i=0;i<responser.size();i++){
			response=responser[i];
			var total = response['total'];
			var actual = response['actual'];
			error = response['error'];
			var currentIndexPath = response['currentIndexPath'];
			var initialTime = response['initialTime'];
			var finalTime = response['finalTime'];
			var running = response['running'];
			var percentage = response['percentage'];
			var elapsed = response['elapsed'];
			var remaining = response['remaining'];
		     description = response['description'];

		     infodiv =infodiv +"<%= LanguageUtil.get(pageContext,"The-Task-perform-was") %> " + description + " .<%= LanguageUtil.get(pageContext,"The-total-of-assets-to-change-is") %> " + total + " <%= LanguageUtil.get(pageContext,"--and--") %> " + error + " <%= LanguageUtil.get(pageContext,"assets-were-succesfully-fixed") %>"+"<br />";
		     infodiv  =infodiv+"<%= LanguageUtil.get(pageContext,"The-start-time-was") %> " + initialTime + " <%= LanguageUtil.get(pageContext,"and-ended-on") %>  "+ finalTime+"<br /><br />";

			}

			fixAssetInfoDiv.innerHTML = infodiv;
			//fixAssetTimeDiv.innerHTML = timeDiv;
			document.getElementById("fixAssetsMessage").innerHTML ="";
			//$("fixAssetsButton").disabled = true;
			document.getElementById("fixAssetsButtonDiv").style.display = "";

		//	setTimeout("fixAssetsCallback()", 10000000);
	}

	else{
		fixAssetInfoDiv.innerHTML = "<%= LanguageUtil.get(pageContext,"No-Tasks-were-executed") %>"
		fixAssetTimeDiv.innerHTML = "";

		document.getElementById("fixAssetsButtonDiv").style.display = "";
		document.getElementById("fixAssetsMessage").innerHTML ="";
		//setTimeout("fixAssetsCallback()", 10000000);

	}

	setTimeout("fixAssetsCallback()", 10000000);
}


function checkFixAsset()
{
	CMSMaintenanceAjax.getFixAssetsProgress(checkFixAssetCallback);
}

function doCreateZipAjax(dataOnly)
{
	showDotCMSSystemMessage("<%= LanguageUtil.get(pageContext,"Backup-file-created-on-background") %>");
	CMSMaintenanceAjax.doBackupExport("createZip",dataOnly,showDotCMSSystemMessage);
}

function doReplace () {
  if (document.getElementById("searchString").value == "") {
  		alert ("<%= LanguageUtil.get(pageContext,"Please-specify-a-search-string") %>");
    	return;
  }
  if (confirm ("<%= LanguageUtil.get(pageContext,"Are-you-sure") %>")) {
  	form = document.getElementById('cmsMaintenanceForm');
	var action = "<portlet:actionURL>";
	action += "<portlet:param name='struts_action' value='/ext/cmsmaintenance/view_cms_maintenance' />";
	action += "<portlet:param name='cmd' value='searchandreplace' />";
	action += "</portlet:actionURL>";
	form.cmd.value="searchandreplace";
	form.action = action
	form.submit();
  }
}

function doCreateZip(dataOnly){
   form = document.getElementById('cmsMaintenanceForm');
	var action = "<portlet:actionURL>";
	action += "<portlet:param name='struts_action' value='/ext/cmsmaintenance/view_cms_maintenance' />";
	action += "<portlet:param name='cmd' value='createZip' />";
	action += "</portlet:actionURL>";
	form.dataOnly.value = dataOnly;
	form.cmd.value="createZip";
	form.action = action
	form.submit();
}


function doDownloadZip(dataOnly){
   form = document.getElementById('cmsMaintenanceForm');
	var action = "<portlet:actionURL>";
	action += "<portlet:param name='struts_action' value='/ext/cmsmaintenance/view_cms_maintenance' />";
	action += "<portlet:param name='cmd' value='downloadZip' />";
	action += "</portlet:actionURL>";
	form.dataOnly.value = dataOnly;
	form.cmd.value="downloadZip";
	form.action = action
	form.submit();
}

function doUpload(){
	form = document.getElementById('cmsMaintenanceForm');
	var action = "<portlet:actionURL>";
	action += "<portlet:param name='struts_action' value='/ext/cmsmaintenance/view_cms_maintenance' />";
	action += "</portlet:actionURL>";
	form.cmd.value="upload";
	form.action = action
	form.submit();
}


function doFixAssetsInconsistencies(){
   form = document.getElementById('cmsMaintenanceForm');

   if (confirm("<%= LanguageUtil.get(pageContext,"Do-you-want-to-fix-assets-inconsistencies") %>")) {
   		document.getElementById("fixAssetsButtonDiv").style.display = "none";
	 	$("fixAssetsButton").disabled = true;
		document.getElementById("fixAssetsMessage").innerHTML = "<font face='Arial' size='2' color='#ff0000'><b><%= LanguageUtil.get(pageContext,"Working") %></b></font>";
		CMSMaintenanceAjax.fixAssetsInconsistencies(fixAssetsCallback);
	}
}

function fixAssetsCallback(responser)
{
	$("fixAssetsButton").disabled = false;

	var fixAssetInfoDiv = document.getElementById("fixAssetInfo");
	var fixAssetTimeDiv = document.getElementById("fixAssetTime");
	var infodiv = "";
	if(responser!= null){

		for(i=0;i<responser.size();i++){
			response=responser[i];
			var total = response['total'];
			var actual = response['actual'];
			error = response['error'];
			var currentIndexPath = response['currentIndexPath'];
			var initialTime = response['initialTime'];
			var finalTime = response['finalTime'];
			var running = response['running'];
			var percentage = response['percentage'];
			var elapsed = response['elapsed'];
			var remaining = response['remaining'];
		     description = response['description'];

		     infodiv =infodiv +"<%= LanguageUtil.get(pageContext,"The-Task-perform-was") %> " + description + " .<%= LanguageUtil.get(pageContext,"The-total-of-assets-to-change-is") %> " + total + " <%= LanguageUtil.get(pageContext,"--and--") %> " + error + " <%= LanguageUtil.get(pageContext,"assets-were-succesfully-fixed") %>"+"<br />";
		     infodiv  =infodiv+"<%= LanguageUtil.get(pageContext,"The-start-time-was") %> " + initialTime + " <%= LanguageUtil.get(pageContext,"and-ended-on") %>  "+ finalTime+"<br /><br />";

			}

			fixAssetInfoDiv.innerHTML = infodiv;
			document.getElementById("fixAssetsMessage").innerHTML ="";
			document.getElementById("fixAssetsButtonDiv").style.display = "";

	}

	else{
		fixAssetInfoDiv.innerHTML = "<%= LanguageUtil.get(pageContext,"No-Tasks-were-executed") %>"
		fixAssetTimeDiv.innerHTML = "";

		document.getElementById("fixAssetsButtonDiv").style.display = "";
		document.getElementById("fixAssetsMessage").innerHTML ="";
	}
}



function doDeleteContentlets(){
	var ids= document.getElementById('contentIdsList').value;
	if(ids=="" || ids.indexOf(';') > 0 || ids.indexOf(':')>0 || ids.indexOf('.')>0 )
	{
	     alert("<%= LanguageUtil.get(pageContext,"Please-enter-a-identifiers-list") %>");
	     return false;
	}

	  if(confirm("<%= LanguageUtil.get(pageContext,"Do-you-want-to-delete-this-contentlet-s") %>")){
		 	$("deleteContentletMessage").innerHTML= '<font face="Arial" size="2" color="#ff0000><b><%= LanguageUtil.get(pageContext,"Process-in-progress-deleting-contentlets") %></b></font>';
		 	$("deleteContentletButton").disabled = true;
			CMSMaintenanceAjax.deleteContentletsFromIdList(document.getElementById('contentIdsList').value, document.getElementById('userId').value, doDeleteContentletsCallback);
		}
}

function doDeleteContentletsCallback(contentlets){

    var message="";

 	if (contentlets[0]!="")
 	{
 		var contaddedsize=contentlets[0];
 	 	/*if(contentlets[0].indexOf(",")){
 	 	 	var contadded=contentlets[0].split(',')
 	 	 	contaddedsize=contadded.length;
 	 	 	}*/
 	 	message+= contaddedsize+ ' <%= LanguageUtil.get(pageContext,"contentlets-were-succesfully-deleted") %></br>';
 	}
	if (contentlets[1]!="")
 	{
 	 	if(contentlets[1].indexOf(",")){
 	 	 	var contnotfound=contentlets[1].split(',')
 	 	 	message+=  '<%= LanguageUtil.get(pageContext,"The-following") %> ' + contnotfound.length + ' <%= LanguageUtil.get(pageContext,"contentlets-were-not-found") %>: '+ contentlets[1] +'</br>';
 	 	 	}
 	 	else message+= '<%= LanguageUtil.get(pageContext,"The-following") %> ' + ' <%= LanguageUtil.get(pageContext, "contentlet-was-not-found") %>: '+ contentlets[1] +'</br>';
 	}

	if (contentlets[2]!="")
 	{
 	 	if(contentlets[2].indexOf(",")){
 	 	 	var conthasreqrel=contentlets[2].split(',')
 	 	 	message+= '<%= LanguageUtil.get(pageContext,"The-following") %> ' + conthasreqrel.length + ' <%= LanguageUtil.get(pageContext,"contentlet-s-could-not-be-deleted-because-the-contentlet-is-required-by-another-piece-of-content") %>: '+ contentlets[2] +'</br>';
 	 	 	}
 	 	else message+= '<%= LanguageUtil.get(pageContext,"The-following") %> ' + ' <%= LanguageUtil.get(pageContext, "contentlet-s-could-not-be-deleted-because-the-contentlet-is-required-by-another-piece-of-content") %>: '+ contentlets[2] +'</br>';
 	}
	if (contentlets[3]!="")
 	{
 	 	if(contentlets[3].indexOf(",")){
 	 	 	var contnotfound=contentlets[3].split(',')
 	 	 	message+= '<%= LanguageUtil.get(pageContext,"The-following") %> ' + contnotfound.length + ' <%= LanguageUtil.get(pageContext,"contentlet-s-could-not-be-deleted-because-the-user-does-not-have-the-necessary-permissions") %>:'+ contentlets[3] +'</br>';
 	 	 	}
 	 	else message+= '<%= LanguageUtil.get(pageContext,"The-following") %> ' + ' <%= LanguageUtil.get(pageContext, "contentlet-s-could-not-be-deleted-because-the-user-does-not-have-the-necessary-permissions") %>:'+ contentlets[1] +'</br>';
 	}

	document.getElementById("deleteContentletMessage").innerHTML=message;
	document.getElementById("deleteContentletButton").disabled = false;
}

function doDropAssets(){
   var form = $('cmsMaintenanceForm');
   if(!validateDate(form.removeassetsdate)){
     alert("<%= LanguageUtil.get(pageContext,"Please,-enter-a-valid-date") %>");
     return false;
   }

  if(confirm("<%= LanguageUtil.get(pageContext,"Do-you-want-to-drop-all-old-assets") %>")){
	 	$("dropAssetsMessage").innerHTML= '<font face="Arial" size="2" color="#ff0000><b><%= LanguageUtil.get(pageContext,"Process-in-progress") %></b></font>';
	 	$("dropAssetsButton").disabled = true;
		CMSMaintenanceAjax.removeOldVersions(form.removeassetsdate.value, doDropAssetsCallback);
	}
}

function doDropAssetsCallback(removed){
 	$("dropAssetsButton").disabled = false;
	if (removed >= 0)
	 	document.getElementById("dropAssetsMessage").innerHTML= '<font face="Arial" size="2" color="#ff0000><b>' + removed + '<%= LanguageUtil.get(pageContext,"old-asset-versions-found-and-removed-from-the-system") %></b></font>';
	else if (removed == -2)
	 	document.getElementById("dropAssetsMessage").innerHTML= '<font face="Arial" size="2" color="#ff0000><b><%= LanguageUtil.get(pageContext,"Database-inconsistencies-found.-The-process-was-cancelled") %></b></font>';
	else
	 	document.getElementById("dropAssetsMessage").innerHTML= '<font face="Arial" size="2" color="#ff0000><b><%= LanguageUtil.get(pageContext,"Remove-process-failed.-Check-the-server-log") %></b></font>';
}

function validateDate(date){

  if(date == null || date.value==''){
  	return false;
  }

  var dateStr = date.value;
  var datearr = dateStr.split("/");

  var month= datearr[0];
  if(parseInt(month) > 12){
  	return false;
  }

  var day= datearr[1];
  if(parseInt(day) > 31){
  	return false;
  }

  var year= datearr[2];
  if(parseInt(year) > 9999 || parseInt(year) < 1900  ){
  	return false;
  }

  return true;
}


function indexStructureChanged(){
	if(dojo.byId('structure') ==undefined || dijit.byId('cleanReindexButton') == undefined){
		return;
	}

	if(dojo.byId('structure').value != "<%= LanguageUtil.get(pageContext,"Rebuild-Whole-Index") %>")
		dijit.byId('cleanReindexButton').attr('disabled', false);
	else
		dijit.byId('cleanReindexButton').attr('disabled', true);
}

function cleanReindexStructure(){
	var strInode = dojo.byId('structure').value;
	CMSMaintenanceAjax.cleanReindexStructure(strInode,showDotCMSSystemMessage);
}

var journalDataCellFuncs = [
                  function(data) { return data['serverId']; },
                  function(data) { return data['count']; },
                  function(data) { return data['priority']; }
                ];

var noDataCellFuncs = [];

function refreshCache(){
	var x = dijit.byId("cacheStatsCp");
	var y =Math.floor(Math.random()*1123213213);

	<%if(CacheLocator.getCacheAdministrator().getImplementationClass().equals(DotGuavaCacheAdministratorImpl.class)){%>
		if(dijit.byId("showSize").checked){
			x.attr( "href","/html/portlet/ext/cmsmaintenance/cachestats_guava.jsp?showSize=true&r=" + y  );

		}
		else{
			x.attr( "href","/html/portlet/ext/cmsmaintenance/cachestats_guava.jsp?r=" + y  );

		}
	<%}else{%>
		x.attr( "href","/html/portlet/ext/cmsmaintenance/cachestats.jsp?r=" + y  );
	<%}%>
	x.style( "height","600px"  );
	console.log(x);

}

function deleteIndex(indexName, live){

	if(live && ! confirm("<%= LanguageUtil.get(pageContext, "Delete-Live-Index") %>")){
		return;
	}
	CMSMaintenanceAjax.deleteIndex(indexName,deleteIndexCallback);

}

function deleteIndexCallback(data){

	refreshIndexStats();
}


function refreshIndexStats(){
	var x = dijit.byId("indexStatsCp");
	var y =Math.floor(Math.random()*1123213213);

	x.attr( "href","/html/portlet/ext/cmsmaintenance/index_stats.jsp?r=" + y  );



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

function doCloseIndex(indexName) {
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

function doOpenIndex(indexName) {
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

function doCreateWorking() {


	var number=prompt("<%=LanguageUtil.get(pageContext, "Number-of-Shards")%> ", <%=Config.getIntProperty("es.index.number_of_shards", 4)%>);
	if(!number){
		return;
	}

	var shards = parseInt(number);
	if(shards <1){
		return;

	}

	var xhrArgs = {
       url: "/DotAjaxDirector/com.dotmarketing.portlets.cmsmaintenance.ajax.IndexAjaxAction/cmd/createIndex/shards/" + shards,
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

function doCreateLive() {


	var number=prompt("<%=LanguageUtil.get(pageContext, "Number-of-Shards")%> ", <%=Config.getIntProperty("es.index.number_of_shards", 4)%>);
	if(!number){
		return;
	}

	var shards = parseInt(number);
	if(shards <1){
		return;
	}



	var xhrArgs = {
       url: "/DotAjaxDirector/com.dotmarketing.portlets.cmsmaintenance.ajax.IndexAjaxAction/cmd/createIndex/live/on/shards/" + shards,
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

function dohighlight(id) {
	dojo.addClass(id,"highlight");
}

function undohighlight(id) {
    dojo.removeClass(id,"highlight");
}

function getAllThreads() {
	document.getElementById('threadList').innerHTML = "";
	dojo.query('#threadProgress').style({display:"block"});
    ThreadMonitorTool.getThreads({
        callback:function(data) {
        	dojo.query('#threadProgress').style({display:"none"});
            var tempString = "";
            var parity = "odd";
            for(var i = 0; i < data.length; i++){
                tempString += "<li class='" + parity + "'>"+ data[i] +"</li>";
                if(parity == "odd"){
                    parity = "even";
                }else{
                    parity = "odd";
                }
            }

//             var editor = dijit.byId('threadList');
//             editor.set('value',tempString);
            dwr.util.setValue("threadList", tempString, { escapeHtml:false });

        },
        errorHandler:function(message) {
            alert("Oops: " + message);
            dojo.query('#threadProgress').style({display:"none"});
        }
    });
}

function hideAllThreads() {
    document.getElementById('threadList').innerHTML = "";
}

function getSysInfo() {

    var tableId = "sysInfo";
    var rowsClass = tableId + "_row";

    //Cleaning the table contents
    cleanTable(rowsClass);

    dojo.query('#sysInfoProgress').style({display:"block"});
    ThreadMonitorTool.getSysProps({
        callback:function(data) {

        	dojo.query('#sysInfoProgress').style({display:"none"});

            for(var dat in data){
                var rowData = "<td>"+ dat +"</td><td> " + data[dat] +"</td>";
                //Creating the row and adding it to the table
                createRow(tableId, rowData, rowsClass, null);
            }

        },
        errorHandler:function(message) {
        	dojo.query('#sysInfoProgress').style({display:"none"});
            alert("Oops: " + message);
        }
    });
}

/**
* Remove all the elements with a given class name
* @param rowsClass
 */
var cleanTable = function (rowsClass) {

    //First we need to remove the old rows
    var currentItems = dojo.query( "." + rowsClass );
    if ( currentItems.length ) {
        for ( i = 0; i < currentItems.length; i++ ) {
            dojo.destroy( currentItems[i] );
        }
    }
};

/**
* Creates a tr node and add it to a table
* @param tableId
* @param rowInnerHtml inner html for the tr node
* @param className
* @param id
 */
var createRow = function (tableId, rowInnerHtml, className, id) {

    if (id == null) {
        id = "";
    }

    //And building the node...
    var tableNode = dojo.byId( tableId );
    var newTr = dojo.create( "tr", {
        innerHTML:rowInnerHtml,
        className:className,
        id:id
    }, tableNode );
};

function killSession(sessionId) {
	dojo.style(dijit.byId('invalidateButton-'+sessionId).domNode,{display:"none",visibility:"hidden"});
	dojo.query('#killSessionProgress-'+sessionId).style({display:"block"});
	UserSessionAjax.invalidateSession(sessionId,{
			callback:function() {
				dojo.style(dijit.byId('invalidateButton-'+sessionId).domNode,{display:"block",visibility:"visible"});
			    dojo.query('#killSessionProgress-'+sessionId).style({display:"none"});
			    dijit.byId('invalidateButton-'+sessionId).set('disabled',true);

			    showDotCMSSystemMessage('<%=LanguageUtil.get(pageContext,"logged-users-tab-killed")%>');
			},
			errorHandler:function(message) {
				dojo.style(dijit.byId('invalidateButton-'+sessionId).domNode,{display:"block",visibility:"visible"});
			    dojo.query('#killSessionProgress-'+sessionId).style({display:"none"});

			    showDotCMSSystemMessage('<%=LanguageUtil.get(pageContext,"logged-users-tab-notkilled")%>');
			}
	});
}

function killAllSessions() {
	UserSessionAjax.invalidateAllSessions({
			callback:function() {
			    showDotCMSSystemMessage('<%=LanguageUtil.get(pageContext,"logged-users-tab-killed")%>');
			    loadUsers();
			},
			errorHandler:function(message) {
			    showDotCMSSystemMessage('<%=LanguageUtil.get(pageContext,"logged-users-tab-notkilled")%>');
			}
	});
}

function loadUsers() {

	var oldButtons=dojo.query("#sessionList .killsessionButton");
	for(var i=0;i<oldButtons.length;i++)
		dijit.byNode(oldButtons[i]).destroy();

    var tableId = "sessionList";
    var rowsClass = tableId + "_row";

    //Cleaning the table contents
    cleanTable(rowsClass);

    dojo.query('#loggedUsersProgress').style({display:"block"});
	UserSessionAjax.getSessionList({
		callback: function(sessionList) {

            dojo.query('#loggedUsersProgress').style({display:"none"});

			if(sessionList.size() > 0) {

                for(var i=0;i<sessionList.size();i++) {
					var session=sessionList[i];
					var html ="<td>"+session.sessionTime+"</td> ";
					html+="<td>"+session.address+"</td> ";
					html+="<td>"+session.userId+"</td> ";
					html+="<td>"+session.userEmail+"</td> ";
					html+="<td>"+session.userFullName+"</td> ";
					html+="<td> ";
					if("<%=session.getId()%>" !=session.sessionId ){
						html+=" <img style='display:none;' id='killSessionProgress-"+session.sessionId+"' src='/html/images/icons/round-progress-bar.gif'/> ";
						html+=" <button id='invalidateButtonNode-"+session.sessionId+"' type='button'> ";
		                html+=" </button>";
					}
	                
					html+=" </td>";

                    //Creating the row and adding it to the table
                    createRow(tableId, html, rowsClass, null)
				}

				for(var i=0;i<sessionList.size();i++) {
                    var session=sessionList[i];
                    new dijit.form.Button({
                    	id:"invalidateButton-"+session.sessionId,
                        label: "<%= LanguageUtil.get(pageContext,"logged-users-tab-killsession") %>",
                        iconClass: "deleteIcon",
                        "class": "killsessionButton",
                        sid : session.sessionId,
                        onClick: function(){
                            killSession(this.sid);
                        }
                    }, "invalidateButtonNode-"+session.sessionId);
				}
			}
		},
		errorHandler: function(message) {
			showDotCMSSystemMessage('<%=LanguageUtil.get(pageContext,"logged-users-reload-error")%>');
		}
	});

}

function selectAll(id){

	document.selection;
	 s = window.getSelection();
	 var r1 = document.createRange();
	 r1.setStartBefore(document.getElementById(id));
	 r1.setEndAfter(document.getElementById(id));
	 s.addRange(r1);
}

function showIndexClusterStatus(indexName) {
	
	var dialog = new dijit.Dialog({
		title: "Index Store",
		style: "width: 400px;",
		content: new dojox.layout.ContentPane({
			href: "/html/portlet/ext/cmsmaintenance/index_cluster_status.jsp?indexName="+indexName
		})
	});
	
	dialog.show();
}


</script>

<style>
#idxReplicasDialog{
	width:300px,height:150px;
}
.highlight td {
    background: #94BBFF;
    color: white !important;
}
#threadList li.odd{
    background-color:#ECECEC;
}

</style>

<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">

    <html:form styleId="cmsMaintenanceForm" method="POST" action="/ext/cmsmaintenance/view_cms_maintenance" enctype="multipart/form-data">
        <input type="hidden" name="userId"  id="userId" value="<%=user.getUserId()%>">
        <input type="hidden" name="referer" value="<%=referer%>">
        <input type="hidden" name="cacheName" id="cacheName">
        <input type="hidden" name="dataOnly" id="dataOnly">
        <input type="hidden" name="cmd" value="">
        <input type="hidden" name="shards" id="numberOfShards" value="<%=Config.getIntProperty("es.index.number_of_shards", 2)%>">

        <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
        <!-- START Cache TAB -->
        <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
        <div id="cache" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Cache") %>" >

            <table class="listingTable shadowBox">
                <tr>
                    <th colspan="2"><%= LanguageUtil.get(pageContext,"Cache") %></th>
                    <th style="text-align:center;white-space:nowrap;" width="350"><%= LanguageUtil.get(pageContext,"Action") %></th>
                </tr>
                <tr>
                    <td colspan="2">&nbsp;</td>
                    <td align="center">
                        <select name="cName" dojoType="dijit.form.ComboBox" autocomplete="true" value="<%= LanguageUtil.get(pageContext,"Flush-All-Caches") %>">
                            <option selected="selected" value="all"><%= LanguageUtil.get(pageContext,"Flush-All-Caches") %></option>
                            <% for(Object c : CacheLocator.getCacheIndexes()){ %>
                                <option><%= c.toString() %></option>
                            <% } %>
                        </select>
                        <button dojoType="dijit.form.Button" onClick="submitform('flushCache');" iconClass="deleteIcon">
                         <%= LanguageUtil.get(pageContext,"Flush-All-Caches") %>
                        </button>
                    </td>
                </tr>
                <tr>
                    <th colspan="2"><%= LanguageUtil.get(pageContext,"Menus-File-Store") %></th>
                    <th style="text-align:center;white-space:nowrap;" width="350"><%= LanguageUtil.get(pageContext,"Action") %></th>
                </tr>
                <tr>
                    <td colspan="2">&nbsp;</td>
                    <td align="center">
                        <button dojoType="dijit.form.Button"  onClick="submitform('<%=com.dotmarketing.util.WebKeys.Cache.CACHE_MENU_FILES%>');" iconClass="deleteIcon">
                           <%= LanguageUtil.get(pageContext,"Delete-Menu-Cache") %>
                        </button>
                    </td>
                </tr>
                <tr>
                    <th colspan="3"><%= LanguageUtil.get(pageContext,"Cache-Stats") %></th>
                </tr>
                <tr>
                    <td colspan="3">
                        <div class="buttonRow" style="text-align: right">
                        <label for="showSize">
                        <%= LanguageUtil.get(pageContext,"Show-Memory-Size") %>: <input type="checkbox" value="true" dojoType="dijit.form.CheckBox" name="showSize" id="showSize" />
                        </label>
                        <button dojoType="dijit.form.Button"  onClick="refreshCache()" iconClass="resetIcon">
                           <%= LanguageUtil.get(pageContext,"Refresh-Stats") %>
                        </button>
                        </div>
                        <div id="cacheStatsCp" dojoType="dijit.layout.ContentPane" style="text-align: center;min-height: 100px;">


                            <div style="padding-bottom:30px;">

                                <table class="listingTable shadowBox" style="width:400px">
                                    <tr>
                                        <th><%= LanguageUtil.get( pageContext, "Total-Memory-Available" ) %>
                                        </th>
                                        <td align="right"><%=UtilMethods.prettyByteify( Runtime.getRuntime().maxMemory() )%>
                                        </td>
                                    </tr>
                                    <tr>
                                        <th><%= LanguageUtil.get( pageContext, "Memory-Allocated" ) %>
                                        </th>
                                        <td align="right"><%= UtilMethods.prettyByteify( Runtime.getRuntime().totalMemory() )%>
                                        </td>
                                    </tr>
                                    <tr>
                                        <th><%= LanguageUtil.get( pageContext, "Filled-Memory" ) %>
                                        </th>
                                        <td align="right"><%= UtilMethods.prettyByteify( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() )%>
                                        </td>
                                    </tr>
                                    <tr>
                                        <th><%= LanguageUtil.get( pageContext, "Free-Memory" ) %>
                                        </th>
                                        <td align="right"><%= UtilMethods.prettyByteify( Runtime.getRuntime().freeMemory() )%>
                                        </td>
                                    </tr>
                                </table>
                                <div class="clear"></div>
                            </div>

                            <a href="#" onclick="refreshCache()"><%= LanguageUtil.get(pageContext,"Refresh-Stats") %></a>

                        </div>

                    </td>
                </tr>
            </table>
        </div>

        <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
        <!-- START Index TAB -->
        <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
        <div dojoType="dojox.layout.ContentPane" id="indexTabCp" title="<%= LanguageUtil.get(pageContext, "Index") %>" >

            <div class="indexActionsDiv" <%=(idxApi.isInFullReindex()) ? "style='display:none'" : "" %>>
                <table class="listingTable">
                    <tr>
                        <th colspan="2"><%= LanguageUtil.get(pageContext,"Content-Index-Tasks") %></th>
                    </tr>
                    <tr>
                        <td colspan="2" align="center">
                            <div id="currentIndexDirDiv"></div>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <div id="lastIndexationDiv"></div>

                                <%= LanguageUtil.get(pageContext,"Reindex") %>:
                                <select id="structure" dojoType="dijit.form.ComboBox" style="width:250px;" autocomplete="true" name="structure" onchange="indexStructureChanged();">
                                    <option><%= LanguageUtil.get(pageContext,"Rebuild-Whole-Index") %></option>
                                    <%

                                        for(Structure structure : structs){%>
                                        <option><%=structure.getVelocityVarName()%></option>
                                    <%}%>
                                </select>

                        </td>
                        <td style="text-align:center;white-space:nowrap;" width="350">
                            <button dojoType="dijit.form.Button" id="idxReindexButton" iconClass="repeatIcon" onClick="doFullReindex()">
                                <%= LanguageUtil.get(pageContext,"Reindex") %>
                            </button>
                            <button dojoType="dijit.form.Button"  iconClass="reindexIcon" onClick="cleanReindexStructure();return false;" id="cleanReindexButton" disabled="disabled">
                                <%= LanguageUtil.get(pageContext,"Delete-Reindex-Structure") %>
                            </button>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <%= LanguageUtil.get(pageContext,"Optimize-Index") %> (<%= LanguageUtil.get(pageContext,"Optimize-Index-Info") %> )
                        </td>
                        <td align="center">
                            <button dojoType="dijit.form.Button" id="idxShrinkBtn" iconClass="shrinkIcon" onClick="CMSMaintenanceAjax.optimizeIndices(optimizeCallback)">
                                <%= LanguageUtil.get(pageContext,"Optimize-Index") %>
                            </button>
                         </td>
                    </tr>
                </table>
            </div>

            <!-- START Re-Index Progress Display -->
            <div id="reindexationInProgressDiv"  <%=(idxApi.isInFullReindex()) ? "" : "style='display:none'" %>>
                <table class="listingTable">
                    <tr>
                        <th colspan="2"><%= LanguageUtil.get(pageContext,"Content-Index-Tasks") %></th>
                    </tr>
                    <tr>
                        <td colspan="2" align="center">
                            <div>
                                <%= LanguageUtil.get(pageContext,"A-reindexation-process-is-in-progress") %>
                            </div>
                            <div id="indexationStartTimeDiv"></div>
                            <div id="newIndexDirPathDiv"></div>
                            <div style="width:200px" maximum="200" id="reindexProgressBar" progress="0" dojoType="dijit.ProgressBar"></div>
                            <div id="indexationProgressDiv"></div>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="2" align="center">
                            <button dojoType="dijit.form.Button"  iconClass="reindexIcon" onClick="stopReIndexing();">
                                <%= LanguageUtil.get(pageContext,"Stop-Reindexation") %>
                            </button>
                        </td>
                    </tr>
                </table>
            </div>

            <div id="indexStatsCp"  dojoType="dijit.layout.ContentPane"></div>

        </div>

        <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
        <!-- START Tools TAB -->
        <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
        <div id="tools" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Tools") %>" >
            <div style="height:20px">&nbsp;</div>
            <table class="listingTable">
                <tr>
                    <th><%= LanguageUtil.get(pageContext,"Import/Export-dotCMS-Content") %></th>
                    <th style="text-align:center;white-space:nowrap;" width="350"><%= LanguageUtil.get(pageContext,"Action") %></th>
                </tr>
                <tr>
                    <td><%= LanguageUtil.get(pageContext,"Backup-to-Zip-file") %></td>
                    <td style="text-align:center;white-space:nowrap;">
                        <button dojoType="dijit.form.Button" onClick="doCreateZipAjax('true');" iconClass="backupIcon">
                           <%= LanguageUtil.get(pageContext,"Backup-Data-Only") %>
                        </button>
                        <button dojoType="dijit.form.Button" onClick="doCreateZipAjax('false');" iconClass="backupIcon">
                          <%= LanguageUtil.get(pageContext,"Backup-Data/Assets") %>
                        </button>
                    </td>
                </tr>
                <tr>
                    <td><%= LanguageUtil.get(pageContext,"Download-Zip-file") %></td>
                    <td style="text-align:center;white-space:nowrap;">
                        <button dojoType="dijit.form.Button" onClick="doDownloadZip('true');" iconClass="downloadIcon">
                           <%= LanguageUtil.get(pageContext,"Download-Data-Only") %>
                        </button>

                        <button dojoType="dijit.form.Button" onClick="doDownloadZip('false');" iconClass="downloadIcon">
                          <%= LanguageUtil.get(pageContext,"Download-Data/Assets") %>
                        </button>
                    </td>
                </tr>
            </table>

            <div style="height:20px">&nbsp;</div>

            <table class="listingTable">
                <tr>
                    <th><%= LanguageUtil.get(pageContext,"Search-And-Replace-Utility") %></th>
                    <th style="text-align:center;white-space:nowrap;" width="350"><%= LanguageUtil.get(pageContext,"Action") %></th>
                </tr>
                <tr>
                    <td>
                        <p><%= LanguageUtil.get(pageContext,"This-utility-will-do-a-find-and-replace") %></p>
                        <%= LanguageUtil.get(pageContext,"Please-specify-the-following-parameters-and-click-replace") %>:
                        <dl>
                            <dt><%= LanguageUtil.get(pageContext,"String-to-find") %>:</dt>
                            <dd><input type="text" dojoType="dijit.form.TextBox" name="searchString" id="searchString" size="50"></dd>

                            <dt><%= LanguageUtil.get(pageContext,"Replace-with") %>:</dt>
                            <dd><input type="text" dojoType="dijit.form.TextBox" name="replaceString" id="replaceString" size="50"></dd>
                        </dl>
                    </td>
                    <td align="center" valing="middle">
                        <button dojoType="dijit.form.Button" onclick="doReplace();" iconClass="reorderIcon">
                           <%= LanguageUtil.get(pageContext,"Replace") %>
                        </button>
                    </td>
                </tr>
            </table>

            <div style="height:20px">&nbsp;</div>

            <table class="listingTable">
                <tr>
                    <th><%= LanguageUtil.get(pageContext,"Fix-Assets-Inconsistencies") %></th>
                    <th style="text-align:center;white-space:nowrap;" width="350"><%= LanguageUtil.get(pageContext,"Action") %></th>
                </tr>
                <tr>
                    <td>
                        <p><%= LanguageUtil.get(pageContext,"This-utility-will-fix-assets-inconsistencies") %></p>
                        <p style="color:#ff0000;"><%= LanguageUtil.get(pageContext,"It's-recommended-to-have-a-fresh") %></p>
                        <div align="center" id="fixAssetsMessage"></div>
                        <%= LanguageUtil.get(pageContext,"Fix-Assets-Inconsistencies") %>
                        <div align="center" id="fixAssetInfo"></div>
                        <div align="center" id="fixAssetTime"></div>
                    </td>
                    <td align="center">
                        <div id="fixAssetsButtonDiv">
                            <button dojoType="dijit.form.Button" id="fixAssetsButton"  onClick="doFixAssetsInconsistencies();" iconClass="fixIcon">
                                <%= LanguageUtil.get(pageContext,"Execute") %>
                            </button>
                        </div>
                    </td>
                </tr>
            </table>

            <div style="height:20px">&nbsp;</div>

            <table class="listingTable">
                <tr>
                    <th><%= LanguageUtil.get(pageContext,"Drop-Old-Assets-Versions") %></th>
                    <th style="text-align:center;white-space:nowrap;" width="350"><%= LanguageUtil.get(pageContext,"Action") %></th>
                </tr>
                <tr>
                    <td>
                        <p><%= LanguageUtil.get(pageContext,"This-utility-will-remove-old-versions-of-contentlets") %></p>
                        <div align="center"  id="dropAssetsMessage">&nbsp;</div>
                        <dl>
                            <dt><%= LanguageUtil.get(pageContext,"Remove-assets-older-than") %>:</dt>
                            <dd>
                                <input type="text" name="removeassetsdate" id="removeassetsdate" constraints="{datePattern:'MM/dd/yyyy'}" invalidMessage="Invalid date."  data-dojo-type="dijit.form.DateTextBox" maxlength="10" size="8">
                            </dd>
                            <dd style="color:#ff0000;"><%= LanguageUtil.get(pageContext,"It's-recommended-to-have-a-fresh") %></dd>
                        </dl>
                    </td>
                    <td align="center">
                      <button dojoType="dijit.form.Button" onClick="doDropAssets();"  id="dropAssetsButton" iconClass="dropIcon">
                         <%= LanguageUtil.get(pageContext,"Execute") %>
                      </button>
                    </td>
                </tr>
            </table>

            <div style="height:20px">&nbsp;</div>

            <table class="listingTable">
                <tr>
                    <th><%= LanguageUtil.get(pageContext,"Delete-Contentlets") %></th>
                    <th style="text-align:center;white-space:nowrap;" width="350"><%= LanguageUtil.get(pageContext,"Action") %></th>
                </tr>
                <tr>
                    <td>
                        <p><%= LanguageUtil.get(pageContext,"This-utility-will-remove-contentlets-from-a-list-of-comma-separated-identifier") %></p>
                        <div align="center"  id="deleteContentletMessage"></div>
                        <dl>
                            <dt><%= LanguageUtil.get(pageContext,"Place-list-here") %>:</dt>
                            <dd>
                            <textarea style="width:350px" name="contentIdsList" id="contentIdsList">
                            </textarea>
                            </dd>
                        </dl>
                    </td>
                    <td align="center">
                      <button dojoType="dijit.form.Button" onClick="doDeleteContentlets();"  id="deleteContentletButton" iconClass="deleteIcon">
                         <%= LanguageUtil.get(pageContext,"Execute") %>
                      </button>
                    </td>
                </tr>

            </table>
        </div>

    </html:form>

    <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
    <!-- START Logging TAB -->
    <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
    <div id="Logging" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Log-Files") %>" >
        <div style="height:20px">&nbsp;</div>
        <div style="margin-bottom:10px;height:700px;border:0px solid red">
            <iframe style="margin-bottom:10px;height:500px;width:100%;border:0px;" id="_logFileInclude" src="/html/portlet/ext/cmsmaintenance/tail_log.jsp" style=""></iframe>
        </div>
   
    </div>

    <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
    <!-- START System Info TAB -->
    <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
    <div id="systemProps" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "System-Properties") %>" >
		<br>&nbsp;<br>
		<div style="width:80%;margin: 0 auto;">
	        <table class="listingTable shadowBox">
	            <thead>
	            <th>
	                <%= LanguageUtil.get(pageContext, "Env-Variable") %>
	            </th>
	            <th>
	                <%= LanguageUtil.get(pageContext, "Value") %>
	            </th>
	            </thead>
	
	            <%Map<String,String> s = System.getenv();%>
	            <%for(Object key : s.keySet()){ %>
	            <tr>
	                <td valign="top"><%=key %></td>
	                <td style="white-space: normal;word-wrap: break-word;"><%=s.get(key) %></td>
	            </tr>
	
	            <%} %>
	        </table>
		<br>&nbsp;<br>
	        <table class="listingTable shadowBox">
	            <thead>
	            <th>
	                <%= LanguageUtil.get(pageContext, "System-Property") %>
	            </th>
	            <th>
	                <%= LanguageUtil.get(pageContext, "Value") %>
	            </th>
	            </thead>
	
	            <%Properties p = System.getProperties();%>
	            <% RuntimeMXBean b = ManagementFactory.getRuntimeMXBean(); %>
	            <tr>
	                <td valign="top" style="vertical-align: top">Startup Args</td>
	                <td valign="top" style="vertical-align: top">
	                    <%for(Object key : b.getInputArguments()){ %>
	                    <%=key %><br>
	                    <%} %>
	                </td>
	            </tr>
	
	            <%for(Object key : p.keySet()){ %>
	
	            <tr>
	                <td valign="top"><%=key %></td>
	                <td>
	                <div  style="white-space: normal;word-wrap: break-word !important;max-width: 400px">
	                	<%=p.get(key) %>
	                </div>
	                </td>
	            </tr>
	
	            <%} %>
	        </table>
	        <br>&nbsp;<br>
		</div>
    </div>

    <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
    <!-- START Threads TAB -->
    <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
    <div id="threads" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "threads-tab-title") %>" >

        <table class="listingTable shadowBox" style="width:800px !important;">
            <thead>
            <th>System information</th>
            <th>Value</th>
            </thead>
            <tbody id="sysInfo">
            </tbody>
        </table>

        <img style="display:none;" id="sysInfoProgress" src="/html/images/icons/round-progress-bar.gif"/>
        <br/>
         <div class="buttonRow" style="text-align:right;width:98% !important;">
            <button dojoType="dijit.form.Button" onClick="getAllThreads()" iconClass="resetIcon">
                <%= LanguageUtil.get(pageContext,"thread-tab-reload") %>
            </button>
            <button dojoType="dijit.form.Button" onClick="getSysInfo()" iconClass="infoIcon">
                <%= LanguageUtil.get(pageContext,"thread-tab-reload-sysinfo") %>
            </button>
        </div>
		<div style="width: 98%; margin-left:auto; margin-right:auto; margin-top: -30px">
		<button dojoType="dijit.form.Button"  name="btn" onClick="selectAll('threadList');">
			 <%= LanguageUtil.get(pageContext,"Select-all") %>
		</button>
		</div>
<!--         <ol class="orderMe" id="threadList"></ol> -->
        <ol class="orderMe" id="threadStats"></ol>
        <div  id="threadList" style="margin-top:10px; width:98%;height:500px;overflow:auto; margin-left:auto; margin-right:auto; border:1px solid #C0C0C0;"></div>
<!--         <textarea class="tailerBody" style="width:98%; height: 800px;  display: block; margin-left: auto; margin-right: auto;" name="contentIdsList" id="threadList"></textarea> -->
        <img style="display:none;" id="threadProgress" src="/html/images/icons/round-progress-bar.gif"/>
    </div>

    <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
    <!-- START Logged Users TAB -->
    <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
    <div id="loggedusers" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "logged-users-tab-title") %>" >
        <div class="buttonRow" style="text-align:right;width:90% !important;">
            <button dojoType="dijit.form.Button" onClick="loadUsers()" iconClass="resetIcon">
                <%= LanguageUtil.get(pageContext,"logged-users-reload") %>
            </button>
        </div>


        <table class="listingTable shadowBox" style="width:90% !important;">
            <thead>
            <tr>
                <th><%= LanguageUtil.get(pageContext,"Started") %></th>
                <th><%= LanguageUtil.get(pageContext,"Remote-Address") %></th>
                <th><%= LanguageUtil.get(pageContext,"user-id") %></th>
                <th><%= LanguageUtil.get(pageContext,"Email") %></th>
                <th><%= LanguageUtil.get(pageContext,"user-name") %></th>
                <th style="text-align:center;">
                	<button dojoType="dijit.form.Button" onClick="killAllSessions();" iconClass="exclamation-red" >
                	<%= LanguageUtil.get(pageContext, "logged-users-tab-killsession-all") %>
                </button>
                </th>
            </tr>
            </thead>
            <tbody id="sessionList">
            </tbody>
        </table>

        <img style="display:none;" id="loggedUsersProgress" src="/html/images/icons/round-progress-bar.gif"/>
    </div>

</div>

<script language="Javascript">
dojo.require("dijit.form.DateTextBox");
	dojo.addOnLoad (function(){

		checkReindexation();
		checkFixAsset();
		//indexStructureChanged();

			var tab =dijit.byId("mainTabContainer");
		   	dojo.connect(tab, 'selectChild',
			 function (evt) {
			 	selectedTab = tab.selectedChildWidget;

				  	if(selectedTab.id =="indexTabCp"){
				  		refreshIndexStats();
				  	}

			});
		//getAllThreads();
		getSysInfo();
		loadUsers();
	});
</script>
