<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotcms.listeners.SessionMonitor"%>
<%@page import="com.dotcms.content.elasticsearch.business.ContentletIndexAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.content.elasticsearch.business.ESContentletIndexAPI"%>
<%@page import="com.dotmarketing.portlets.structure.factories.StructureFactory"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotcms.content.elasticsearch.business.ESIndexAPI"%>
<%@page import="java.lang.management.RuntimeMXBean"%>
<%@page import="java.lang.management.ManagementFactory"%>
<%@page import="com.dotmarketing.business.ChainableCacheAdministratorImpl"%>
<%@page import="com.dotmarketing.business.CacheLocator"%>
<%@ page import="java.util.Calendar"%>
<%@ page import="com.dotcms.repackage.javax.portlet.WindowState"%>
<%@ page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@ page import="com.dotmarketing.portlets.cmsmaintenance.struts.CmsMaintenanceForm"%>
<%@ page import="java.util.List"%>
<%@page import="java.util.regex.Matcher"%>
<%@page import="java.util.regex.Pattern"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.util.ReleaseInfo"%>


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
dojo.require("dijit.form.MultiSelect");


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

/** Stops de re-indexation process and clears the database table that contains 
    the remaining non re-indexed records. */
function stopReIndexing(){
	CMSMaintenanceAjax.stopReindexation(checkReindexationCallback);
}

/** Stops de re-indexation process and clears the database table that contains 
    the remaining non re-indexed records. Moreover, switches the current index 
    to point to the new one. */
function stopReIndexingAndSwitchover() {
	dijit.byId('stopReindexAndSwitch').set('label', '<%= LanguageUtil.get(pageContext,"Switching-To-New-Index") %>');
	dijit.byId('stopReindexAndSwitch').set('disabled', true);
	CMSMaintenanceAjax.stopReindexationAndSwitchover(checkReindexationCallback);
}

/** Downloads the main information of the records that could not be re-indexed 
    as a .CSV file*/
function downloadFailedAsCsv() {
	var href = "<portlet:actionURL windowState='<%= WindowState.MAXIMIZED.toString() %>'>";
    href += "<portlet:param name='struts_action' value='/ext/cmsmaintenance/view_cms_maintenance' />";
    href += "<portlet:param name='cmd' value='export-failed-as-csv' />";      
    href += "<portlet:param name='referer' value='<%= java.net.URLDecoder.decode(referer, "UTF-8") %>' />";       
    href += "</portlet:actionURL>";
    window.location.href=href;
}

function optimizeCallback() {
	showDotCMSSystemMessage("<%=LanguageUtil.get(pageContext,"Optimize-Done")%>");
}

function checkFixAsset()
{
	CMSMaintenanceAjax.getFixAssetsProgress(fixAssetsCallback);
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

	if(responser != undefined){

		for(i = 0; i < responser.length; i++){
			response = responser[i];
			var total = response['total'];
			var errorsFixed = response['errorsFixed'];
			var initialTime = response['initialTime'];
			var finalTime = response['finalTime'];
			var description = response['description'];

			infodiv = infodiv + "<%= LanguageUtil.get(pageContext,"The-Task-perform-was") %> " + description
                                + " .<%= LanguageUtil.get(pageContext,"The-total-of-assets-to-change-is") %> " + total
                                + " <%= LanguageUtil.get(pageContext,"--and--") %> " + errorsFixed
                                + " <%= LanguageUtil.get(pageContext,"assets-were-succesfully-fixed") %>"+"<br />";

            infodiv = infodiv + "<%= LanguageUtil.get(pageContext,"The-start-time-was") %> " + initialTime
                                + " <%= LanguageUtil.get(pageContext,"and-ended-on") %>  "+ finalTime+"<br /><br />";
			}

			fixAssetInfoDiv.innerHTML = infodiv;
	} else {
		fixAssetInfoDiv.innerHTML = "<%= LanguageUtil.get(pageContext,"No-Tasks-were-executed") %>"
		fixAssetTimeDiv.innerHTML = "";
	}

    document.getElementById("fixAssetsMessage").innerHTML ="";
    document.getElementById("fixAssetsButtonDiv").style.display = "";
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

function doDeleteContentletsCallback(message){

	document.getElementById("deleteContentletMessage").innerHTML=message;
	document.getElementById("deleteContentletButton").disabled = false;
	document.getElementById('contentIdsList').value = "";
}

function doDeletePushedAssets(){
	  if(confirm("<%= LanguageUtil.get(pageContext,"Do-you-want-to-delete-assets") %>")){
		 	$("deletePushedAssetsMessage").innerHTML= '<font face="Arial" size="2" color="#ff0000><b><%= LanguageUtil.get(pageContext,"Process-in-progress-deleting-pushed-assets") %></b></font>';
		 	$("deletePushAssetsButton").disabled = true;
			CMSMaintenanceAjax.deletePushedAssets(doDeletePushedAssetsCallback);
		}
}

function doDeletePushedAssetsCallback(result) {
	var deletePushedAssetsMessage = document.getElementById("deletePushedAssetsMessage");
	deletePushedAssetsMessage.style.display = block;
	if(result!=null && result=="success") {
		deletePushedAssetsMessage.innerHTML='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext,"pushed-assets-were-succesfully-deleted")) %>';
	} else {
		deletePushedAssetsMessage.innerHTML='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext,"push-assets-could-not-be-deleted")) %>';
	}
}

function doDropAssets(){
   var dateInput = dijit.byId('removeassetsdate');

   if(dateInput.get('value')==null || !dateInput.validate()){
     alert("<%= LanguageUtil.get(pageContext,"Please,-enter-a-valid-date") %>");
     return false;
   }

  if(confirm("<%= UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext,"Do-you-want-to-drop-all-old-assets")) %>")){
	    $("dropAssetsMessage").innerHTML = '<spanstyle="font-family: Arial; font-size: x-small; color: #ff0000><b><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext,"Process-in-progress")) %></b></spanstyle>';
        dijit.byId('dropAssetsButton').attr('disabled', true);
	 	var dateStr=dojo.date.locale.format(dijit.byId("removeassetsdate").get('value'),{selector: "date", datePattern:"MM/dd/yyyy"});
		CMSMaintenanceAjax.removeOldVersions(dateStr, doDropAssetsCallback);
	}
}

function doDropAssetsCallback(removed){
	dijit.byId('dropAssetsButton').attr('disabled', false);
    if (removed >= 0)
        document.getElementById("dropAssetsMessage").innerHTML= '<spanstyle="font-family: Arial; font-size: x-small; color: #ff0000><b>' + removed + ' <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext,"old-asset-versions-found-and-removed-from-the-system")) %></b></spanstyle>';
    else if (removed == -2)
        document.getElementById("dropAssetsMessage").innerHTML= '<spanstyle="font-family: Arial; font-size: x-small; color: #ff0000><b><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext,"Database-inconsistencies-found.-The-process-was-cancelled")) %></b></spanstyle>';
    else
        document.getElementById("dropAssetsMessage").innerHTML= '<spanstyle="font-family: Arial; font-size: x-small; color: #ff0000><b><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext,"Remove-process-failed.-Check-the-server-log")) %></b></spanstyle>';
}

/**
 * Call to clean assets deleting assets that are no longer in the File asset table and the Contentlet table
 * where the structure type is <b>File Asset<b/>
 */
var doCleanAssets = function () {

    if (confirm("<%= UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext,"cms.maintenance.clean.assets.button.confirmation")) %>")) {
        $("cleanAssetsMessage").innerHTML = '<span style="font-family: Arial; font-size: x-small; color: #ff0000><b><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext,"cms.maintenance.clean.assets.process.in.progress")) %></b></span>';
        dijit.byId('cleanAssetsButton').attr('disabled', true);

        CMSMaintenanceAjax.cleanAssets(doCleanAssetsCallback);
    }
};

dojo.ready(function() {
	doCleanAssetsCallback();
});

function doCleanAssetsCallback() {
    CMSMaintenanceAjax.getCleanAssetsStatus(getCleanAssetsStatusCallback);
}

function getCleanAssetsStatusCallback(status) {
	if(!dijit.byId('cleanAssetsButton').attr('disabled') && status['running']=='false')
		return;

	var cleanAssetsMessage = document.getElementById("cleanAssetsMessage");
	cleanAssetsMessage.style.display = 'block';
	cleanAssetsMessage.innerHTML =
     	                      '<table> <tr><td>Deleted</td><td>'+status['deleted']+'</td></tr>'+
     	                              '<tr><td>Analyzed</td><td>'+status['currentFiles']+'</td></tr>'+
     	                              '<tr><td>Total Files</td><td>'+status['totalFiles']+'</td></tr>'+
     	                              ((status['running']=='true' && parseInt(status['totalFiles'])>0) ?
     	                              ('<tr><td>Progress</td><td>'+Math.round(100*(parseInt(status['currentFiles'])/parseInt(status['totalFiles'])))+'%</td></tr>') : '')+
     	                              '<tr><td>Status</td><td>'+status['status']+'</td></tr></table>';
    if (status['running']=='true')
       setTimeout("doCleanAssetsCallback()", 1000);
    else
       dijit.byId('cleanAssetsButton').attr('disabled',false);
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
	 if(confirm("<%= LanguageUtil.get(pageContext,"are-you-sure-delete-reindex") %>")){
		var strInode = dojo.byId('structure').value;
		CMSMaintenanceAjax.cleanReindexStructure(strInode,showDotCMSSystemMessage);
	 }
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

	<%if(CacheLocator.getCacheAdministrator().getImplementationClass().equals(ChainableCacheAdministratorImpl.class)){%>
		x.attr( "href","/html/portlet/ext/cmsmaintenance/cachestats_guava.jsp?r=" + y  );
	<%}else{%>
		x.attr( "href","/html/portlet/ext/cmsmaintenance/cachestats.jsp?r=" + y  );
	<%}%>
	x.style =  "height:600px";

}

function deleteIndex(indexName, live){

	if(live && ! confirm("<%= UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Delete-Live-Index")) %>")){
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

function doSnapshotIndex(indexName){
	  window.location="/api/v1/esindex/snapshot/index/" + indexName;
}

function doReindex(){
	var shards;
    if(dojo.byId('structure').value == "<%= LanguageUtil.get(pageContext,"Rebuild-Whole-Index") %>"){
    	//document.getElementById('defaultStructure').value = "Rebuild Whole Index";
    	dojo.byId('defaultStructure').value = "Rebuild Whole Index";

		var number=prompt("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Number-of-Shards"))%> ", <%=Config.getIntProperty("es.index.number_of_shards", 4)%>);

		if(!number){
			return;
		}
			shards = parseInt(number);
    	}
    else{
    		shards =1
     	 }
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
                    showDotCMSSystemMessage("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Index-Closed"))%>", true);
                    refreshIndexStats();
                }
            } else {
                showDotCMSSystemMessage("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Request-Failed"))%>", true);
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
                    showDotCMSSystemMessage("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Index-Opened"))%>", true);
                    refreshIndexStats();
                }
            } else {
                showDotCMSSystemMessage("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Request-Failed"))%>", true);
            }
        }
    };
    dojo.xhrPost(xhrArgs);
}

function doClearIndex(indexName){

	if(!confirm("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-clear-this-index"))%>")){
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
					showDotCMSSystemMessage("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Index-Cleared"))%>", true);
					refreshIndexStats();
				}
			} else {
				showDotCMSSystemMessage("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Request-Failed"))%>", true);
			}
		}
	};
	dojo.xhrPost(xhrArgs);

}
function doActivateIndex(indexName){

	if(!confirm("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-activate-this-index"))%>")){
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
					showDotCMSSystemMessage("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Index-Activated"))%>", true);
					refreshIndexStats();
				}
			} else {
				showDotCMSSystemMessage("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Request-Failed"))%>", true);
			}
		}
	};
	dojo.xhrPost(xhrArgs);
}
function doDeactivateIndex(indexName){

	if(!confirm("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-deactivate-this-index"))%>")){
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
					showDotCMSSystemMessage("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Index-Deactivated"))%>", true);
					refreshIndexStats();
				}
			} else {
				showDotCMSSystemMessage("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Request-Failed"))%>", true);
			}
		}
	};
	dojo.xhrPost(xhrArgs);

}

function hideRestoreIndex() {
    dijit.byId("restoreIndexDialog").hide();
}

function hideRestoreSnapshotIndex() {
    dijit.byId("restoreSnapshotDialog").hide();
    refreshIndexStats();
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

function showRestoreSnapshotDialog() {
	  var dialog=dijit.byId("restoreSnapshotDialog");
	  dojo.byId("uploadSnapshotFileName").innerHTML='';
	  dijit.byId('uploadSnapshotSubmit').set('disabled',false);
	  dojo.query('#uploadSnapshotProgress').style({display:"none"});
	  dialog.show();
	}

function doRestoreIndex() {
	if(dojo.byId("uploadFileName").innerHTML=='') {
		showDotCMSErrorMessage("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "No-File-Selected"))%>");
	}
	else {
		dijit.byId('uploadSubmit').set('disabled',true);
	    dojo.query('#uploadProgress').style({display:"block"});
	    dijit.byId("restoreIndexUploader").submit();
	}
}

function doRestoreIndexSnapshot(evt){
	if(!document.getElementById("restoreSnapshotUploader").value) {
	    showDotCMSErrorMessage("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "no.snapshot.selected"))%>");
	}
	else{
		let formData = new FormData();
	  let oReq = new XMLHttpRequest();
	  dijit.byId('uploadSnapshotSubmit').set('disabled',true);
	  dojo.query('#uploadSnapshotProgress').style({display:"block"});
		formData.append('file',document.getElementById("restoreSnapshotUploader").files[0]);
		oReq.onreadystatechange = function(){
			if (oReq.readyState === 4) {
		     var msgJson = JSON.parse(oReq.response)
				 if (oReq.status === 200) {
		      	 showDotCMSErrorMessage(msgJson.message,true);
		     } else {
		     	 showDotCMSErrorMessage(msgJson.errors[0].message);
		     }
		     restoreSnapshotUploadCompleted();
		  }
		}
	   oReq.open('POST','/api/v1/esindex/restoresnapshot/',true);
	   oReq.send(formData);
	}
}

function restoreUploadCompleted() {
	hideRestoreIndex();
}

function restoreSnapshotUploadCompleted() {
	hideRestoreSnapshotIndex();
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
	dijit.byId('addIndex').show();
	document.getElementById('shards').value = <%=Config.getIntProperty("es.index.number_of_shards", 4)%>;
	shardsUrl = "/DotAjaxDirector/com.dotmarketing.portlets.cmsmaintenance.ajax.IndexAjaxAction/cmd/createIndex/shards/";
}

function doCreateLive() {
	dijit.byId('addIndex').show();
	document.getElementById('shards').value = <%=Config.getIntProperty("es.index.number_of_shards", 4)%>;
	shardsUrl = "/DotAjaxDirector/com.dotmarketing.portlets.cmsmaintenance.ajax.IndexAjaxAction/cmd/createIndex/live/on/shards/";
}

function shardCreating(){	
	dijit.byId('addIndex').hide();
	var shards = document.getElementById('shards').value;
	if(shards <1){
		return;
	}
	
	var xhrArgs = {
       url: shardsUrl + shards,
       handleAs: "text",
       handle : function(dataOrError, ioArgs) {
           if (dojo.isString(dataOrError)) {
               if (dataOrError.indexOf("FAILURE") == 0) {
                   showDotCMSSystemMessage(dataOrError, true);
               } else {
                   showDotCMSSystemMessage("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Index-Created"))%>", true);
                   refreshIndexStats();

               }
           } else {
               showDotCMSSystemMessage("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Request-Failed"))%>", true);
           }
       }
    };
    dojo.xhrPost(xhrArgs);
}

function updateReplicas(indexName,currentNum){

	var number=prompt("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Update-Replicas-Index"))%> for index:\n\n" + indexName, currentNum);

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
							showDotCMSSystemMessage("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Replicas-Updated"))%>", true);
							refreshIndexStats();
						}
					} else {
						showDotCMSSystemMessage("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Request-Failed"))%>", true);
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
            alert("Error: " + message);
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
            alert("Error: " + message);
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

function killSessionById(sessionId) {
    var invalidateButtonElem = dijit.byId("invalidateButtonNode-"+sessionId);

	dojo.style(invalidateButtonElem.domNode,{display:"none",visibility:"hidden"});
	dojo.query('#killSessionProgress-'+sessionId).style({display:"block"});
	UserSessionAjax.invalidateSession(sessionId,{
			callback:function() {
				dojo.style(invalidateButtonElem.domNode,{display:"block",visibility:"visible"});
			    dojo.query('#killSessionProgress-'+sessionId).style({display:"none"});
			    invalidateButtonElem.set('disabled',true);

			    showDotCMSSystemMessage('<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext,"logged-users-tab-killed"))%>');
			},
			errorHandler:function(message) {
				dojo.style(invalidateButtonElem.domNode,{display:"block",visibility:"visible"});
			    dojo.query('#killSessionProgress-'+sessionId).style({display:"none"});

			    showDotCMSSystemMessage('<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext,"logged-users-tab-notkilled"))%>');
			}
	});
}

function killAllSessions() {
	UserSessionAjax.invalidateAllSessions({
			callback:function() {
			    showDotCMSSystemMessage('<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext,"logged-users-tab-killed"))%>');
			    loadUsers();
			},
			errorHandler:function(message) {
			    showDotCMSSystemMessage('<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext,"logged-users-tab-notkilled"))%>');
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
		    // Append prefix to invalidate button id
			var invalidateButtonIdPrefix = "invalidateButtonNode-";
		    
            dojo.query('#loggedUsersProgress').style({display:"none"});

			if(sessionList.length > 0) {

                for(var i=0;i<sessionList.length;i++) {
					var session=sessionList[i];
					var html ="<td>"+session.sessionTime+"</td> ";
					html+="<td>"+session.address+"</td> ";
					html+="<td>"+session.userId+"</td> ";
					html+="<td>"+session.userEmail+"</td> ";
					html+="<td>"+session.userFullName+"</td> ";
					html+="<td> ";
					if("<%=session.getId()%>" !=session.sessionId ){
						html+=" <img style='display:none;' id='killSessionProgress-"+session.sessionId+"' src='/html/images/icons/round-progress-bar.gif'/> ";
						html+=" <button id='" + invalidateButtonIdPrefix + session.sessionId + "'></button>";
					}

					html+=" </td>";

                    //Creating the row and adding it to the table
                    createRow(tableId, html, rowsClass, "loggedUser-"+session.sessionId)
				}

				for(var i=0;i<sessionList.length;i++) {
                    var session=sessionList[i];

                    var id = invalidateButtonIdPrefix + session.sessionId;

                    //Verify if a button widget with this id exist, if it exist we must delete firts before to try to create a new one
                    if (dojo.byId(id)) {
                    	var button = dijit.byId(id);
                    	if(button) {
                    		button.destroyRecursive();
                    	}

                        new dijit.form.Button({
                            id: id,
                            label: "<%= UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext,"logged-users-tab-killsession")) %>",
                            iconClass: "deleteIcon",
                            "class": "killsessionButton",
                            sid : session.sessionId,
                            onClick: function(){
                                killSessionById(this.sid);
                            }
                        }, invalidateButtonIdPrefix + session.sessionId);
                    }
				}
			}
		},
		errorHandler: function(message) {
			showDotCMSSystemMessage('<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext,"logged-users-reload-error"))%>');
		}
	});

}

function selectAll(id){
	if(window.getSelection){
	 	document.selection;
	 	s = window.getSelection();
	    var r1 = document.createRange();
	 	r1.setStartBefore(document.getElementById(id));
	 	r1.setEndAfter(document.getElementById(id));
	 	s.removeAllRanges();
	 	s.addRange(r1);
	}
	if (document.selection){
		var rangeToSelect = document.selection.createRange ();
        rangeToSelect.moveToElementText(document.getElementById(id));
        rangeToSelect.select ();
    }
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

var user = "<%=user.getUserId()%>";

function enableDisableRadio(elem){
	if(elem.checked && elem.value=='assetType'){
		dijit.byId('assetType').setDisabled(false);
		dijit.byId('assetIdentifier').setDisabled(true);
		dijit.byId('assetIdentifier').set('value','');
		dijit.byId('selectAssetHostInode').setDisabled(false);
	}else if(elem.checked && elem.value=='assetIdentifier'){
		dijit.byId('assetIdentifier').setDisabled(false);
		dijit.byId('assetType').setDisabled(true);
		dijit.byId('assetType').set('value','');
		dijit.byId('selectAssetHostInode').setDisabled(true);
	}
}

function enableDisableCheckbox(elem){
	if(elem.checked){
		dijit.byId('autoPublish').setDisabled(false);
	}else{
		dijit.byId('autoPublish').setDisabled(true);
		dijit.byId('autoPublish').set('checked',false);
	}
}
function validateSearchAndReplace(){
	var assetSearchType = dijit.byId('assetType').attr('value');
	var assetIdentifier = dijit.byId('assetIdentifier').get('value');
	var assetHost = dojo.byId('assetHost').value;
	var searchString = dijit.byId('assetSearchString').get('value');
	var replaceString = dijit.byId('assetReplaceString').get('value');
	var newAssetVersion = dijit.byId('newAssetVersion').checked;
	var autoPublish = dijit.byId('autoPublish').checked;
	if( (assetSearchType != '' || assetIdentifier != '') && assetHost != '' && searchString != '' && replaceString != ''){
		//alert("is Here:"+assetSearchType+" - "+assetIdentifier+" - "+assetHost+" - "+searchString+" - "+replaceString+" - "+newAssetVersion+autoPublish);
		if(dijit.byId('assetSearchIdentifier').checked){
			var fileAssetsIds = assetIdentifier.split(',');
			assetsSearchAndReplace(fileAssetsIds.length);
		}else{
	    	var urlPath = "/DotAjaxDirector/com.dotmarketing.portlets.cmsmaintenance.ajax.AssetsSearchAndReplaceAjax/assetCountByFiles/true";
	    	if(dijit.byId('assetSearchType').checked){
	    		urlPath = urlPath+"/replaceByFiles/"+assetSearchType;
	    	}else{
	    		urlPath = urlPath+"/replaceByIds/"+assetIdentifier;
	    	}
	    	urlPath = urlPath+"/hosts/"+assetHost+"/user/"+user+"/generateNewAssetVersion/"+newAssetVersion+"/publish/"+autoPublish;

	    	var xhrArgs = {
	    	    url: urlPath,
	    	    handleAs: "text",
	    	    load: function(response, ioArgs) {
	    	    	var results = response;
	    	    	assetsSearchAndReplace(response);
	    	    },
	    	    error: function(response, ioArgs) {
	    	          console.error("HTTP status code: ", ioArgs.xhr.status);
	    	          document.getElementById("asar_message").innerHTML = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "ASSETS_SEARCH_AND_REPLACE_Error")) %>'+response; //
	    	    }
	    	};
    		dojo.xhrPost( xhrArgs );
		}
	}else{
		alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "ASSETS_SEARCH_AND_REPLACE_Required")) %>');
	}
}
function assetsSearchAndReplace(assetsToProcess){
	if(confirm('<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "ASSETS_SEARCH_AND_REPLACE_JS_Confirmation1"))%> '+assetsToProcess+' <%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "ASSETS_SEARCH_AND_REPLACE_JS_Confirmation2"))%>')){
		var assetSearchType = dijit.byId('assetType').attr('value');
    	var assetIdentifier = dijit.byId('assetIdentifier').get('value');
    	var assetHost = dojo.byId('assetHost').value;
    	var searchString = dijit.byId('assetSearchString').get('value');
    	var replaceString = dijit.byId('assetReplaceString').get('value');
    	var newAssetVersion = dijit.byId('newAssetVersion').checked;
    	var autoPublish = dijit.byId('autoPublish').checked;
    	if( (assetSearchType != '' || assetIdentifier != '') && assetHost != '' && searchString != '' && replaceString != ''){

	    	//alert("is Here:"+assetSearchType+" - "+assetIdentifier+" - "+assetHost+" - "+searchString+" - "+replaceString+" - "+newAssetVersion+autoPublish);
	    	var urlPath = "/DotAjaxDirector/com.dotmarketing.portlets.cmsmaintenance.ajax.AssetsSearchAndReplaceAjax/searchText/"+searchString+"/replaceText/"+replaceString;
	    	if(dijit.byId('assetSearchType').checked){
	    		urlPath = urlPath+"/replaceByFiles/"+assetSearchType;
	    	}else{
	    		urlPath = urlPath+"/replaceByIds/"+assetIdentifier;
	    	}
	    	urlPath = urlPath+"/hosts/"+assetHost+"/user/"+user+"/generateNewAssetVersion/"+newAssetVersion+"/publish/"+autoPublish;
	    	var xhrArgs = {
	    	    url: urlPath,
	    	    handleAs: "text",
	    	    load: function(response, ioArgs) {
	    	    	var results = response.split('|');
	    	    	document.getElementById("asar_message").innerHTML = '<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "ASSETS_SEARCH_AND_REPLACE_Assets_to_process"))%> '+results[0]+'<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "ASSETS_SEARCH_AND_REPLACE_Assets_processed"))%> '+results[1]+'<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext,"ASSETS_SEARCH_AND_REPLACE_Assets_modified"))%> '+results[2]+'<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "ASSETS_SEARCH_AND_REPLACE_Assets_not_processed"))%> '+results[3]+'<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "ASSETS_SEARCH_AND_REPLACE_Assets_error_messages"))%> '+results[4];
	    	    },
	    	    error: function(response, ioArgs) {
	    	          console.error("HTTP status code: ", ioArgs.xhr.status);
	    	          document.getElementById("asar_message").innerHTML = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "ASSETS_SEARCH_AND_REPLACE_Error")) %>'+response; //
	    	    }
	    	};
    		dojo.xhrPost( xhrArgs );
    		document.getElementById("asar_message").innerHTML = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "ASSETS_SEARCH_AND_REPLACE_Wait")) %>';
    	}else{
    		alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "ASSETS_SEARCH_AND_REPLACE_Required")) %>');
    	}
	}
}

function updateHostList(inode, name, selectval){

	if(dijit.byId('selectAssetHostInode').attr('value') == '') {
		return;
	}

	var hostId = dijit.byId('selectAssetHostInode').attr('value');
	var hostName = dijit.byId('selectAssetHostInode').attr('displayedValue');
	var table = document.getElementById('assetHostListTable');
	var rowCount = table.rows.length;
	var row  = document.getElementById("assetHostListTable_"+hostId);

	if(row!=null){
	   alert('<%= LanguageUtil.get(pageContext, "host-already-selected") %>');
	}else{
		
		if(hostId == 'all'){
			var existingHostIds=dojo.byId("assetHost").value.split(",");
			for(var i = 0; i < existingHostIds.length; i++){
				assetHostListTable_deleteHost(existingHostIds[i]);
			}
		}else{
			if(dojo.byId("assetHost").value == 'all'){
				assetHostListTable_deleteHost('all');
			}
		}

	    var nohosts = document.getElementById("assetHostListTable_nohosts");
		if(nohosts!=null){
			table.deleteRow(1);
	    }
	
	
		var newRow = table.insertRow(table.rows.length);
		if((table.rows.length%2)==0){
	        newRow.className = "alternate_1";
		}else{
			newRow.className = "alternate_2";
		}
		newRow.id = "assetHostListTable_"+hostId;
		var cell0 = newRow.insertCell(0);
		var cell1 = newRow.insertCell(1);
		var anchor = document.createElement("a");
		anchor.href= 'javascript:assetHostListTable_deleteHost('+'"'+ hostId +'"'+');';
		anchor.innerHTML = '<span class="deleteIcon"></span>';
		cell0.appendChild(anchor);
		cell1.innerHTML = hostName;
		
		if((dojo.byId(inode).value == '') || (dojo.byId(inode).value == 'all')){
			dojo.byId(inode).value = hostId;
		}else if(hostId == 'all'){
			dojo.byId(inode).value = hostId;
		}else{
			dojo.byId(inode).value = dojo.byId(inode).value + "," + hostId;
		}
	}
}
function assetHostListTable_deleteHost(hostId){

	var table = document.getElementById('assetHostListTable');
	var row  = document.getElementById("assetHostListTable_"+hostId);
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
						newRow.id="assetHostListTable_nohosts";
						newRow.innerHTML = '<td colspan="2"><div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "no-hosts-selected") %></div></td>';
						break;
					}
				}
			 }
		 }catch(e) {}
	}

	if(hostId == 'all'){
		dojo.byId("assetHost").value='';
		return;
	}else{
		var hostids=dojo.byId("assetHost").value.split(",");
		var ids="";
		for(var i = 0; i < hostids.length; i++){
			if(hostids[i] != hostId){
				if(ids == ''){
					ids = hostids[i];
				}else{
					ids=ids + "," +hostids[i];
				}
			}
		}
		dojo.byId("assetHost").value=ids;
	}
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

dt.rightdl {
   	width:300px;
   	margin:0px 10px 10px;
}
dd.leftdl {
  	width:400px;
}
.asarTextarea{
  	width:300px;
   	min-height:100px;
   	max-height: 600px;
}
.asarMultiSelect{
   	width: 300px;
   	min-height:100px;
   	max-height: 600px;
}
</style>

<div class="portlet-main">
	<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">

    <html:form styleId="cmsMaintenanceForm" method="POST" action="/ext/cmsmaintenance/view_cms_maintenance" enctype="multipart/form-data">
        <input type="hidden" name="userId"  id="userId" value="<%=user.getUserId()%>">
        <input type="hidden" name="referer" value="<%=referer%>">
        <input type="hidden" name="cacheName" id="cacheName">
        <input type="hidden" name="dataOnly" id="dataOnly">
        <input type="hidden" name="cmd" value="">
        <input type="hidden"  name="defaultStructure" id="defaultStructure" value="">
        <input type="hidden" name="shards" id="numberOfShards" value="<%=Config.getIntProperty("es.index.number_of_shards", 2)%>">

        <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
        <!-- START Cache TAB -->
        <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
        <div id="cache" dojoType="dijit.layout.ContentPane" title="<%= UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Cache")) %>" >

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
                            <% Object[] caches = (Object[])CacheLocator.getCacheIndexes();
                            String[] indexValue = new String[caches.length];
                            for (int i = 0; i<caches.length; i++) {
                            	indexValue[i] = caches[i].toString();
                            }
                            java.util.Arrays.sort(indexValue); 
                            
                            for(String c : indexValue){ %>
                                <option><%= c %></option>
                            <% } %>
                        </select>
                        <button dojoType="dijit.form.Button" onClick="submitform('flushCache');" iconClass="deleteIcon">
                         <%= LanguageUtil.get(pageContext,"Flush-Cache-Button") %>
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
                        <button dojoType="dijit.form.Button"  onClick="refreshCache()" iconClass="resetIcon">
                           <%= LanguageUtil.get(pageContext,"Refresh-Stats") %>
                        </button>
                        </div>
                        <div id="cacheStatsCp" dojoType="dojox.layout.ContentPane" parseOnLoad="true" style="text-align: center;min-height: 100px;">


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
                            <button dojoType="dijit.form.Button" id="idxReindexButton" iconClass="repeatIcon" onClick="doReindex()">
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
                            <button dojoType="dijit.form.Button"  iconClass="resolveIcon" id="stopReindexAndSwitch" onClick="stopReIndexingAndSwitchover();">
                                <%= LanguageUtil.get(pageContext,"Stop-Reindexation-And-Make-Active") %>
                            </button>
                            <button dojoType="dijit.form.Button"  iconClass="downloadIcon" onClick="downloadFailedAsCsv();">
                                <%= LanguageUtil.get(pageContext,"Download-Failed-Records-As-CSV") %>
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
						<div class="inline-form">
							<button dojoType="dijit.form.Button" onClick="doCreateZipAjax('true');" iconClass="backupIcon">
							   <%= LanguageUtil.get(pageContext,"Backup-Data-Only") %>
							</button>
							<button dojoType="dijit.form.Button" onClick="doCreateZipAjax('false');" iconClass="backupIcon">
							  <%= LanguageUtil.get(pageContext,"Backup-Data/Assets") %>
							</button>
						</div>
                    </td>
                </tr>
                <tr>
                    <td><%= LanguageUtil.get(pageContext,"Download-Zip-file") %></td>
                    <td style="text-align:center;white-space:nowrap;">
						<div class="inline-form">
							<button dojoType="dijit.form.Button" onClick="doDownloadZip('true');" iconClass="downloadIcon">
							   <%= LanguageUtil.get(pageContext,"Download-Data-Only") %>
							</button>

							<button dojoType="dijit.form.Button" onClick="doDownloadZip('false');" iconClass="downloadIcon">
							  <%= LanguageUtil.get(pageContext,"Download-Data/Assets") %>
							</button>
						</div>
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
                    <td colspan="2">
                        <p><%= LanguageUtil.get(pageContext,"This-utility-will-do-a-find-and-replace") %></p>
					</td>
				</tr>
				<tr>
					<td>
						<p><%= LanguageUtil.get(pageContext,"Please-specify-the-following-parameters-and-click-replace") %></p>
						<div class="form-horizontal">
							<dl>
								<dt><%= LanguageUtil.get(pageContext,"String-to-find") %>:</dt>
								<dd><input type="text" dojoType="dijit.form.TextBox" name="searchString" id="searchString" size="50"></dd>
							</dl>
							<dl>
								<dt><%= LanguageUtil.get(pageContext,"Replace-with") %>:</dt>
								<dd><input type="text" dojoType="dijit.form.TextBox" name="replaceString" id="replaceString" size="50"></dd>
							</dl>
						</div>
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
                    <td colspan="2">
                        <p><%= LanguageUtil.get(pageContext,"This-utility-will-remove-old-versions-of-contentlets") %></p>
					</td>
				</tr>
				<tr>
					<td>
                        <div align="center" id="dropAssetsMessage"></div>
						<div class="inline-form">
							<label form="removeassetsdate"><%= LanguageUtil.get(pageContext,"Remove-assets-older-than") %>:</label>
							<input type="text" name="removeassetsdate" id="removeassetsdate" constraints="{datePattern:'MM/dd/yyyy'}" invalidMessage="Invalid date."  data-dojo-type="dijit.form.DateTextBox" maxlength="10">
						</div>
						<br />
						<p style="color:#ff0000;"><%= LanguageUtil.get(pageContext,"It's-recommended-to-have-a-fresh") %></p>
                    </td>
                    <td align="center">
                      <button dojoType="dijit.form.Button" onClick="doDropAssets();"  id="dropAssetsButton" iconClass="dropIcon">
                         <%= LanguageUtil.get(pageContext,"Execute") %>
                      </button>
                    </td>
                </tr>

				<tr>
					<td>
						<div align="center" id="cleanAssetsMessage" style="display:none"></div>
						<p><%= LanguageUtil.get(pageContext,"cms.maintenance.clean.assets.button.explanation") %></p>
					</td>
					<td align="center">
						<div class="inline-form">
							<select id="whatClean" name="whatClean" dojoType="dijit.form.FilteringSelect">
								<option selected="selected" value="all"><%= LanguageUtil.get(pageContext,"Clean-bin-and-file") %></option>
								<option value="binary"><%= LanguageUtil.get(pageContext,"Clean-only-bin") %></option>
								<option value="file_asset"><%= LanguageUtil.get(pageContext,"Clean-only-fileasset") %></option>
							</select>
							<button dojoType="dijit.form.Button" onClick="doCleanAssets();"  id="cleanAssetsButton" iconClass="dropIcon">
								<%= LanguageUtil.get(pageContext,"cms.maintenance.clean.assets.button.label") %>
							</button>
						</div>
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
                        <div class="inline-form">
                            <label><%= LanguageUtil.get(pageContext,"Place-list-here") %>:</label>
							<textarea dojoType="dijit.form.Textarea" style="width:350px; min-height:80px" name="contentIdsList" id="contentIdsList"></textarea>
                        </div>
                    </td>
                    <td align="center">
                      <button dojoType="dijit.form.Button" onClick="doDeleteContentlets();"  id="deleteContentletButton" iconClass="deleteIcon">
                         <%= LanguageUtil.get(pageContext,"Execute") %>
                      </button>
                    </td>
                </tr>

            </table>

            <div style="height:20px">&nbsp;</div>

            <table class="listingTable">
                <tr>
                    <th><%= LanguageUtil.get(pageContext,"Delete-Pushed-Assets") %></th>
                    <th style="text-align:center;white-space:nowrap;" width="350"><%= LanguageUtil.get(pageContext,"Action") %></th>
                </tr>
                <tr>
                    <td>
						<div align="center" id="deletePushedAssetsMessage" style="display:none"></div>
                        <p><%= LanguageUtil.get(pageContext,"This-utility-will-remove-all-pushed-assets") %></p>
                    </td>
                    <td align="center">
                      <button dojoType="dijit.form.Button" onClick="doDeletePushedAssets();"  id="deletePushAssetsButton" iconClass="deleteIcon">
                         <%= LanguageUtil.get(pageContext,"Execute") %>
                      </button>
                    </td>
                </tr>

            </table>

            <div style="height:20px">&nbsp;</div>
            <%
            	  List<Host> hosts = new ArrayList<Host>();
            	try{
            		hosts = APILocator.getHostAPI().getHostsWithPermission(PermissionAPI.PERMISSION_READ,user,false);
            	}
            	catch(Exception e){
            		Logger.error(this.getClass(), "Unable to list Hosts: " + e.getMessage());
            	}
                  String validFileExtensions = Config.getStringProperty("ASSETS_SEARCH_AND_REPLACE_ALLOWED_FILE_TYPES");
            %>
           <table class="listingTable shadowBox">
	        	<tr>
	            	<th colspan="2"><%= LanguageUtil.get(pageContext, "ASSETS_SEARCH_AND_REPLACE_Manager") %></th>
	               	<th style="text-align:center;white-space:nowrap;" width="350"><%= LanguageUtil.get(pageContext,"Action") %></th>
	            </tr>
	            <tr>
	               	<td colspan="2">
	               		<p><%= LanguageUtil.get(pageContext,"ASSETS_SEARCH_AND_REPLACE_description") %></p>
		               	<table class="listingTable">
			               	<tr>
			               		<td>
									<div class="inline-form">
										<label><%= LanguageUtil.get(pageContext,"ASSETS_SEARCH_AND_REPLACE_Host") %></label>
										<select dojoType="dijit.form.FilteringSelect"  multiple="true" name="selectAssetHostInode" id="selectAssetHostInode" autocomplete="false"  invalidMessage="Invalid site name" style="width: 175px;">
											<option selected="selected" value="all"><%= LanguageUtil.get(pageContext,"ASSETS_SEARCH_AND_REPLACE_All") %></option>
												<%	String hostNames="";
													String hostIdentifier="";
													for(Host h : hosts){
													if(!h.isSystemHost()){
														hostNames=hostNames+","+h.getHostname();
														hostIdentifier=hostIdentifier+","+h.getIdentifier();
												%>
												<option value="<%=h.getIdentifier()%>"><%= h.getHostname() %></option>
												<%  }
													}
												%>
											</select>
										<button id="addHostButton" dojoType="dijit.form.Button" type="button" iconClass="plusIcon" onclick='updateHostList("assetHost","assetHostNames","selectAssetHostInode")'><%= LanguageUtil.get(pageContext, "Add-Host") %></button>
										<input type="hidden" name="assetHost" id="assetHost" value=""/>
										<input type="hidden" name="assetHostNames" id="assetHostNames" value=""/>
									</div>


									<br/><br/>
                        			<div name="assetHostlist" id="assetHostlist"></div>
									<table class="listingTable" id="assetHostListTable" style="margin:10px;width:90%">
										<tr>
										    <th nowrap style="width:60px;"><span><%= LanguageUtil.get(pageContext, "Delete") %></span></th>
											<th nowrap><%= LanguageUtil.get(pageContext, "Host") %></th>
										</tr>
										<tr id= "assetHostListTable_nohosts">
											<td colspan="2">
												<div class="noResultsMessage"><%= LanguageUtil.get(pageContext, "no-hosts-selected") %></div>
											</td>
										</tr>
									</table>                        			
                  			    </td>
			               		<td>
									<div class="checkbox">
										<input type="radio" onclick="enableDisableRadio(this)" checked="checked" value="assetType" dojoType="dijit.form.RadioButton" name="assetSearch" id="assetSearchType" />
										<label for=""><%= LanguageUtil.get(pageContext,"ASSETS_SEARCH_AND_REPLACE_Search_by_type_of_asset") %>:</label>
									</div>
			               			<br/>
	                                <select name="assetType" id="assetType" dojoType="dijit.form.MultiSelect" multiple="multiple" size="scrollable" class="asarMultiSelect">
	                                <% for(String fileType : validFileExtensions.split(",")){%>
	                                	<option value="<%=fileType%>"><%=fileType%></option>
	                               <% }%>
	                               </select></br/>
	                                <em><%=LanguageUtil.get(pageContext,"ASSETS_SEARCH_AND_REPLACE_Search_by_type_of_asset_hint") %></em>
	                           </td>
			               	</tr>
			               	<tr>
			               		<td>&nbsp;</td>
			               		<td>
									   <div class="checkbox">
										   <input type="radio" onclick="enableDisableRadio(this)" value="assetIdentifier" dojoType="dijit.form.RadioButton" name="assetSearch" id="assetSearchIdentifier" />
										   <label for=""><%= LanguageUtil.get(pageContext,"ASSETS_SEARCH_AND_REPLACE_Search_by_asset_identifier") %>:</label>
									   </div>
			               		<br/>
	                                <input type="text" disabled="disabled" dojoType="dijit.form.Textarea" class="asarTextarea" rows="3" name="assetIdentifier" id="assetIdentifier"></br/>
	                                <em><%=LanguageUtil.get(pageContext,"ASSETS_SEARCH_AND_REPLACE_Search_by_asset_identifier_hint") %></em>
	                            </td>
			               	</tr>
			               	<tr>
			               		<td>
									<div class="inline-form">
										<label for="assetSearchString"><%= LanguageUtil.get(pageContext,"ASSETS_SEARCH_AND_REPLACE_String_to_find") %></label>
										<input type="text" dojoType="dijit.form.TextBox" name="assetSearchString" id="assetSearchString" />
									</div>
			               		</td>
			               		<td>
									<div class="checkbox">
			               				<input type="checkbox" onclick="enableDisableCheckbox(this)" value="true" dojoType="dijit.form.CheckBox" name="newAssetVersion" id="newAssetVersion" />
										<label for="newAssetVersion"><%= LanguageUtil.get(pageContext,"ASSETS_SEARCH_AND_REPLACE_Generate_new_asset_version") %></label>
									</div>
			               		</td>
			               	</tr>
			               	<tr>
			               		<td>
									<div class="inline-form">
										<label for="assetReplaceString">
			               					<%= LanguageUtil.get(pageContext,"ASSETS_SEARCH_AND_REPLACE_Replace_with") %>
										</label>
										<input type="text" dojoType="dijit.form.TextBox" name="assetReplaceString" id="assetReplaceString" />
									</div>
			               		</td>
			               		<td>
									<div class="checkbox">
										<input type="checkbox" disabled="true" dojoType="dijit.form.CheckBox" name="autoPublish" id="autoPublish" />
										<label for="autoPublish"><%= LanguageUtil.get(pageContext,"ASSETS_SEARCH_AND_REPLACE_Automatically_publish_new_asset_version") %></label>
									</div>
			               		</td>
			               	</tr>
		               	</table>
	               	</td>
	               	<td style="text-align:center;white-space:nowrap;">
	               		<button dojoType="dijit.form.Button" onclick="validateSearchAndReplace();" iconClass="reorderIcon">
	               		     <%= LanguageUtil.get(pageContext,"ASSETS_SEARCH_AND_REPLACE_Execute") %>
	                    </button>
					</td>
	            </tr>
	            <tr>
	               	<td colspan="3" style="text-align:center">
						<span id="asar_message"><%= LanguageUtil.get(pageContext,"ASSETS_SEARCH_AND_REPLACE_Warning") %></span>
					</td>
	            </tr>
	        </table>
        </div>

    </html:form>

    <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
    <!-- START Logging TAB -->
    <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
    <div id="Logging" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Log-Files") %>" >
		<%@ include file="/html/portlet/ext/cmsmaintenance/tail_log.jsp"%>
    </div>

    <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
    <!-- START System Info TAB -->
    <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
    <div id="systemProps" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "System-Properties") %>" >
		<br>&nbsp;<br>
		<div style="width:80%;margin: 0 auto;">
		    <h2 id="version-info"><%=LanguageUtil.get(pageContext, "version") %>&#58;&nbsp;<%=ReleaseInfo.getVersion() %>&nbsp;&#40;<%=ReleaseInfo.getBuildDateString() %>&#41;</h2>
            <br>&nbsp;<br>
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
	            <%TreeSet<Object> keys = new TreeSet(s.keySet()); %>
	            <%for(Object key : keys){ %>
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
				<%keys = new TreeSet(p.keySet()); %>
	            <%for(Object key : keys){ %>

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
        <div  id="threadList" style="margin-top:10px; width:98%;min-height:500px;overflow:visible; ; margin-left:auto; margin-right:auto; border:1px solid #C0C0C0;"></div>
    	<img style="display:none;" id="threadProgress" src="/html/images/icons/round-progress-bar.gif"/>
    </div>

    <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
    <!-- START Logged Users TAB -->
    <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
    <div id="loggedusers" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "logged-users-tab-title") %>" >
		<div class="portlet-toolbar">
			<div class="portlet-toolbar__actions-primary">
			</div>
			<div class="portlet-toolbar__actions-secondary">
				<button dojoType="dijit.form.Button" onClick="loadUsers()" iconClass="resetIcon">
					<%= LanguageUtil.get(pageContext,"logged-users-reload") %>
				</button>
			</div>
		</div>

        <table class="listingTable">
            <thead>
            <tr>
                <th><%= LanguageUtil.get(pageContext,"Started") %></th>
                <th><%= LanguageUtil.get(pageContext,"Remote-Address") %></th>
                <th><%= LanguageUtil.get(pageContext,"user-id") %></th>
                <th><%= LanguageUtil.get(pageContext,"Email") %></th>
                <th><%= LanguageUtil.get(pageContext,"user-name") %></th>
                <th style="text-align:right;">
                	<button dojoType="dijit.form.Button" onClick="killAllSessions();" class="dijitButtonDanger">
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
</div>

<div dojoType="dijit.Dialog" id="addIndex" title="Add Index" style="height:150px;width:400px;">

 	<div align="center" style="padding-top: 10px;">
		<label name="index"><%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Number-of-Shards"))%></label>
  		<input type="text" id="shards" name="shards" value="<%=Config.getIntProperty("es.index.number_of_shards", 4)%>">
  	</div><br />
  	<div class="buttonRow" align="center">
	           <button id="addButton" dojoType="dijit.form.Button" iconClass="addIcon" onClick="shardCreating()"><%= LanguageUtil.get(pageContext, "Add") %></button>&nbsp; &nbsp; 
	           <button dojoType="dijit.form.Button" iconClass="cancelIcon" onClick="javascript:dijit.byId('addIndex').hide();"><%= LanguageUtil.get(pageContext, "Cancel") %></button>&nbsp; &nbsp; 
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
