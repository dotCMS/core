<%@page import="com.dotcms.contenttype.model.type.ContentType"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotcms.listeners.SessionMonitor"%>
<%@page import="com.dotcms.content.elasticsearch.business.ContentletIndexAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotcms.content.elasticsearch.business.ContentletIndexAPIImpl"%>
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
<%@ page import="com.dotmarketing.db.DbConnectionFactory" %>


<%@ include file="/html/portlet/ext/cmsmaintenance/init.jsp"%>

<%
int rollingRestartDelay = Config.getIntProperty("ROLLING_RESTART_DELAY_SECONDS", 60);

DateFormat modDateFormat = java.text.DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT, locale);

java.util.Map params = new java.util.HashMap();
params.put("struts_action",	new String[] { "/ext/cmsmaintenance/view_cms_maintenance" });
String referer = java.net.URLEncoder.encode(com.dotmarketing.util.PortletURLUtil.getActionURL(request,WindowState.NORMAL.toString(), params), "UTF-8");

CmsMaintenanceForm CMF = (com.dotmarketing.portlets.cmsmaintenance.struts.CmsMaintenanceForm) request.getAttribute("CmsMaintenanceForm");

ContentletIndexAPI idxApi = APILocator.getContentletIndexAPI();
List<ContentType> structs = APILocator.getContentTypeAPI(APILocator.systemUser()).findAll("name");
%>
<script type="text/javascript" src="/dwr/engine.js"></script>
<script type="text/javascript" src="/dwr/util.js"></script>
<script type='text/javascript' src='/dwr/interface/CMSMaintenanceAjax.js'></script>
<script type="text/javascript" src="/dwr/interface/ThreadMonitorTool.js"></script>
<script type="text/javascript" src="/dwr/interface/UserSessionAjax.js"></script>

<script language="Javascript">
dojo.require("dijit.Editor");
dojo.require("dijit.form.MultiSelect");
dojo.require("dijit.form.ValidationTextBox")


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
	var inFullReindexation = response.entity.inFullReindexation;
	var contentCountToIndex = response.entity.contentCountToIndex;
	var lastIndexationProgress = response.entity.lastIndexationProgress;
	var currentIndexPath = response.entity.currentIndexPath;
	var newIndexPath = response.entity.newIndexPath;
	var reindexTimeElapsed = response.entity.reindexTimeElapsed;
    var failedRecords = response.entity.errorCount;

    if(failedRecords > 0){
        dojo.byId("failedReindexRecordsDiv").style.display="";
        dojo.byId("failedReindexRecordsMessage").innerHTML= failedRecords;

    }else{
        dojo.byId("failedReindexRecordsDiv").style.display="none";
    }


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
		    bar.update({
		      maximum: contentCountToIndex,
		      progress: lastIndexationProgress
		    });
		    bar.attr("style", "width:500px;margin:10px;");
		}
		stillInReindexation = true;
		var indexationProgressDiv = document.getElementById("indexationProgressDiv");
		indexationProgressDiv.innerHTML = "<div style='text-align:center;'><%= LanguageUtil.get(pageContext,"Reindex-Progress") %> : " + lastIndexationProgress + " / " + contentCountToIndex + "<br><%= LanguageUtil.get(pageContext,"Time") %> : "  + reindexTimeElapsed ;
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

    fetch('/api/v1/esindex/reindex',{cache: 'no-cache'})
    .then(response => response.json())
    .then(data =>checkReindexationCallback(data));

}
function deleteFailedRecords () {

    fetch('/api/v1/esindex/failed', {method:'DELETE',cache: 'no-cache'} )
    .then(response => response.json())
    .then(data =>checkReindexationCallback(data));

}


/** Stops de re-indexation process and clears the database table that contains
    the remaining non re-indexed records. */
function stopReIndexing(){

    fetch('/api/v1/esindex/reindex?switch=false', {method:'DELETE',cache: 'no-cache'} )
    .then(response => response.json())
    .then(data =>checkReindexationCallback(data));


}

/** Stops de re-indexation process and clears the database table that contains
    the remaining non re-indexed records. Moreover, switches the current index
    to point to the new one. */
function stopReIndexingAndSwitchover() {
    fetch('/api/v1/esindex/reindex?switch=true', {method:'DELETE',cache: 'no-cache'} )
    .then(response => response.json())
    .then(data =>checkReindexationCallback(data));

}

/** Downloads the main information of the records that could not be re-indexed
    as a .CSV file*/
function downloadFailedAsJson() {
	var href = "/api/v1/esindex/failed";
	window.open(href);
}


function optimizeIndices(){
    fetch('/api/v1/esindex/optimize', {method:'POST',cache: 'no-cache'} )
    .then(response => response.json());

}


function flushIndiciesCache(){
    fetch('/api/v1/esindex/cache', {method:'DELETE',cache: 'no-cache'} )
    .then(response => response.json());

}

function deleteIndex(indexName, live){

    if(live && ! confirm("<%= UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Delete-Live-Index")) %>")){
        return;
    }

    fetch('/api/v1/esindex/' + indexName, {method:'DELETE',cache: 'no-cache'} )
    .then(response => response.json())
    .then(()=>refreshIndexStats());


}

function refreshIndexStats(){
    var x = dijit.byId("indexStatsCp");
    var y =Math.floor(Math.random()*1123213213);


    x.attr( "href","/html/portlet/ext/cmsmaintenance/index_stats.jsp?r=" + y  );

    /**
    fetch('/api/v1/index', {cache: 'no-cache'})
    .then(response => response.json())
    .then(data =>paintStatusTable(data))
    **/
}


function doReindex(){
    var shards =1
    var contentType = dijit.byId('structure').item != null ? dijit.byId('structure').item.id : "DOTALL";
    if("DOTALL"=== contentType){
        var number=prompt("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Number-of-Shards"))%> ", <%=Config.getIntProperty("es.index.number_of_shards", 2)%>);
        if(!number){
            return;
        }
        shards = parseInt(number);
        if(shards == null || shards <1){
            return;
        }
    }
    dijit.byId('structure').reset();

    fetch('/api/v1/esindex/reindex?shards=' + shards + '&contentType=' + contentType, {method:'POST'})
    .then(response => response.json())
    .then(data =>checkReindexationCallback(data))
    .then(()=>refreshIndexStats());
}

function doCloseIndex(indexName) {

    fetch('/api/v1/esindex/' + indexName + '?action=close', {method:'PUT'} )
    .then(response => response.json())
    .then(()=>refreshIndexStats());
}

function doOpenIndex(indexName) {


    fetch('/api/v1/esindex/' + indexName + '?action=open', {method:'PUT'} )
    .then(response => response.json())
    .then(()=>refreshIndexStats());
}

function doClearIndex(indexName){

    if(!confirm("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-clear-this-index"))%>")){
        return;

    }

    fetch('/api/v1/esindex/' + indexName + '?action=clear', {method:'PUT'} )
    .then(response => response.json())
    .then(()=>refreshIndexStats());
}

function doActivateIndex(indexName){

    if(!confirm("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-activate-this-index"))%>")){
        return;
    }

    fetch('/api/v1/esindex/' + indexName, {method:'PUT'} )
    .then(response => response.json())

    .then(()=>refreshIndexStats());
}


function doDeactivateIndex(indexName){

    if(!confirm("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Are-you-sure-you-want-to-deactivate-this-index"))%>")){
        return;

    }

    fetch('/api/v1/esindex/' + indexName + '?action=deactivate', {method:'PUT'} )
    .then(response => response.json())
    .then(()=>refreshIndexStats());

}


function toggleRollingShutdown(){
    let show = document.getElementById("rollingShutdownInfo").style.display;
    document.getElementById("rollingShutdownInfo").style.display=(show=='none') ? '':'none';

}

function doShutdownDotcms(){

    if(dijit.byId("agreeToShutdown") && !dijit.byId("agreeToShutdown").checked){
        alert("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Please agree with the disclaimer"))%>")
        return;
    }

    if(dijit.byId("agreeToShutdown") && !confirm("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "shutdown.dotcms.confirmation"))%>")){
        return;
    }

    let rollingShutdown = false;

    if(dijit.byId("rollingShutdown")){
        rollingShutdown = dijit.byId("rollingShutdown").getValue();
    }


    rollingShutdown = (rollingShutdown === 'true');




    if(!rollingShutdown){
        fetch('/api/v1/maintenance/_shutdown', {method:'DELETE'} )
        .then(response => response.json())
        .then(()=>{
            alert('shutdown started');
            dijit.byId("rollingRestartDelay").setDisabled(true);
        });
    }

    if(rollingShutdown && !(dijit.byId("rollingRestartDelay").isValid())){
        alert("<%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Please enter a number for the restart delay"))%>")
        return;
    }

    let rollingRestartDelay = dijit.byId("rollingRestartDelay").getValue();

    if(isNaN(parseInt( rollingRestartDelay ))){
        rollingRestartDelay = <%=rollingRestartDelay%> ;
    }else{
        rollingRestartDelay=parseInt( rollingRestartDelay );
    }



    fetch('/api/v1/maintenance/_shutdownCluster?rollingDelay=' + rollingRestartDelay, {method:'DELETE'} )
    .then(response => response.json())
    .then(()=>{
        alert('shutdown started');
        dijit.byId("rollingRestartDelay").setDisabled(true);
    });


}








const indexTableRowTmpl = (data) => `<tr class="${data.rowClass} showPointer" id="${data.indexName}Row">
    <td  align="center" class="showPointer">${data.state}</td>
    <td  class="showPointer" >${data.indexName}</td>
    <td>${data.created}</td>
    <td align="center">${data.documentCount}</td>
    <td align="center">${data.numberOfShards}</td>
    <td align="center">${data.numberOfReplicas}</td>
    <td align="center">${data.size}</td>
    <td align="center">
          <div onclick="showIndexClusterStatus('${data.indexName}')"  style='cursor:pointer;background:${data.indexColor}; width:20px;height:20px;'></div>
    </td>
</tr>`;

/**
 *
 */
function paintStatusTable(data){

    const indexTable = document.getElementById("indexStatsCp");

    indexTable.innerHTML="";

    console.log("found rows:" + indexTable.rows.length);
    data.entity.forEach(function (item, index) {
        const data = {};
        data.rowClass= item.active ? "trIdxActive" : item.building ? "trIdxBuilding" : "trIdxNothing";
        data.state = item.active ? "<%= LanguageUtil.get(pageContext,"active") %>" : item.building ? "<%= LanguageUtil.get(pageContext,"building") %>" : "";
        data.indexName = item.indexName;
        data.created = item.created;
        data.indexColor=item.health.status;
        data.numberOfReplicas=item.health.numberOfReplicas;
        data.numberOfShards=item.health.numberOfShards;
        data.documentCount=(item.status === undefined) ? "n/a"  : item.status.documentCount;
        data.size=(item.status === undefined) ? "n/a"  : item.status.size;

        let row  = indexTable.insertRow();
        row.outerHTML = indexTableRowTmpl(data);
      });

}




function checkFixAsset()
{
	CMSMaintenanceAjax.getFixAssetsProgress(fixAssetsCallback);
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

<% if (Config.getBooleanProperty("ALLOW_STARTER_ZIP_GENERATION_ON_DISK", false)) { %>
function doCreateZip(dataOnly) {
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
<% } %>

function downloadDb(downloadLocation) {
    fetch('/api/v1/maintenance/_pgDumpAvailable', {method:'GET'} )
    .then(response => response.text())
    .then((data) => {
        if (data !== 'true') {
            alert("<%= LanguageUtil.get(pageContext,"PG-Dump-Unavailable") %>");
            return;
        }
        location.href = downloadLocation;
    });
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
		var strInode = dijit.byId('structure').item.id;
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




function dohighlight(id) {
	dojo.addClass(id,"highlight");
}

function undohighlight(id) {
    dojo.removeClass(id,"highlight");
}

function getAllThreads() {
	document.getElementById('threadList').innerHTML = "";
	dojo.query('#threadProgress').style({display:"block"});
    const hideSystemThreads = document.getElementById("hideSystemThreads").checked;
    ThreadMonitorTool.getThreads(hideSystemThreads, {
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

/**
* Remove all the elements with a given class name
* @param rowsClass
 */
const cleanTable = function (rowsClass) {

    //First we need to remove the old rows
    var currentItems = dojo.query( "." + rowsClass );
    if ( currentItems.length ) {
        for ( i = 0; i < currentItems.length; i++ ) {
            dojo.destroy( currentItems[i] );
        }
    }
};


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

function killSessionById(obfSessionId) {
    var invalidateButtonElem = dijit.byId("invalidateButtonNode-"+obfSessionId);

	dojo.style(invalidateButtonElem.domNode,{display:"none",visibility:"hidden"});
	dojo.query('#killSessionProgress-'+obfSessionId).style({display:"block"});
	UserSessionAjax.invalidateSession(obfSessionId,{
			callback:function() {
				dojo.style(invalidateButtonElem.domNode,{display:"block",visibility:"visible"});
			    dojo.query('#killSessionProgress-'+obfSessionId).style({display:"none"});
			    invalidateButtonElem.set('disabled',true);

			    showDotCMSSystemMessage('<%=UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext,"logged-users-tab-killed"))%>');
			},
			errorHandler:function(message) {
				dojo.style(invalidateButtonElem.domNode,{display:"block",visibility:"visible"});
			    dojo.query('#killSessionProgress-'+obfSessionId).style({display:"none"});

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
					if( "true" !== session.isCurrent ){
						html+=" <img style='display:none;' id='killSessionProgress-"+session.obfSession+"' src='/html/images/icons/round-progress-bar.gif'/> ";
						html+=" <button id='" + invalidateButtonIdPrefix + session.obfSession + "'></button>";
					}

					html+=" </td>";

                    //Creating the row and adding it to the table
                    createRow(tableId, html, rowsClass, "loggedUser-"+session.obfSession)
				}

				for(var i=0;i<sessionList.length;i++) {
                    var session=sessionList[i];

                    var id = invalidateButtonIdPrefix + session.obfSession;

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
                            obsid : session.obfSession,
                            onClick: function(){
                                killSessionById(this.obsid);
                            }
                        }, invalidateButtonIdPrefix + session.obfSession);
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


    /**
     * When the User needs to download dotCMS assets, this function will display a dialog asking them whether they want
     * to include old versions of the assets or not.
     */
    function createAssetExportDialog(downloadUrl) {
        // Create the dialog
        assetExportDialog = new dijit.Dialog({
            title: "<%= LanguageUtil.get(pageContext, "Import/Export-dotCMS-Content") %>",
            content: "<%= LanguageUtil.get(pageContext, "download-assets-include-old-versions") %>",
            style: "maxWidth: 400px;",
            onBlur: () => {
                if (assetExportDialog.open) {
                    assetExportDialog.hide();
                }
            }
        });

        if(dijit.byId("oldAssetsRadioIdTrue") != null){
            dijit.byId("oldAssetsRadioIdTrue").destroy();
            dijit.byId("oldAssetsRadioIdFalse").destroy();
            dijit.byId("maxFileSize").destroy();
        }

        let btnContainer = document.createElement("div");
        btnContainer.style.padding = "10px";
        btnContainer.style.textAlign = "center";
        let radioButton = new dijit.form.RadioButton({
            name: "oldAssetsRadio",
            id:"oldAssetsRadioIdTrue",
            value: "true",
            checked: false
        });
        let label = document.createElement("label");
        label.style.paddingRight = "30px";
        label.style.paddingLeft = "5px";
        label.htmlFor = "oldAssetsRadioIdTrue";
        label.innerHTML = "<%= LanguageUtil.get(pageContext, "Yes") %>";
        btnContainer.appendChild(radioButton.domNode);
        btnContainer.appendChild(label);


        radioButton = new dijit.form.RadioButton({
            name: "oldAssetsRadio",
            id:"oldAssetsRadioIdFalse",
            value: "false",
            checked: true
        });
        label = document.createElement("label");
        label.style.paddingRight = "10px";
        label.style.paddingLeft = "5px";

        label.htmlFor = "oldAssetsRadioIdFalse";
        label.innerHTML = "<%= LanguageUtil.get(pageContext, "No") %>";
        btnContainer.appendChild(radioButton.domNode);
        btnContainer.appendChild(label);
        assetExportDialog.containerNode.appendChild(btnContainer);


        btnContainer = document.createElement("div");
        btnContainer.style.textAlign = "center";


        let input = new dijit.form.ValidationTextBox({
            name: "maxFileSize",
            id:"maxFileSize",
            required: false,
            label: "E.g: 1mb, 512k, 2gb",
            placeHolder: "512k, 1mb, 2gb...",
            regExp: "([0-9]+)\\s?(b|kb|mb|gb|k|m|g)?",
            invalidMessage: "<%= LanguageUtil.get(pageContext, "Invalid-Size") %>",

        });
        input.domNode.style.width = "150px";
        input.domNode.style.margin = "auto";
        let labelDiv = document.createElement("div");
        labelDiv.style.paddingTop = "10px";
        labelDiv.style.paddingBottom = "10px";
        labelDiv.innerHTML = "Max file size for included assets? Leave empty for no limit.";
        btnContainer.appendChild(labelDiv);
        btnContainer.appendChild(input.domNode);
        labelDiv = document.createElement("div");
        labelDiv.style.padding = "10px";
        labelDiv.innerHTML="Leave empty for no limit"
        //btnContainer.appendChild(labelDiv);
        assetExportDialog.containerNode.appendChild(btnContainer);




        let buttonDiv = document.createElement("div");
        buttonDiv.style.padding = "20px";


        buttonDiv.style.textAlign = "center";

        let button = new dijit.form.Button({
            label: "Download Now",
            class: "dialogButton",
            onClick: () => {
                if(!dijit.byId("maxFileSize").isValid()){
                    alert("Invalid file size");
                    return;
                }
                let oldAssets = dijit.byId("oldAssetsRadioIdTrue").checked;
                let maxFileSize = dijit.byId("maxFileSize").get("value");
                location.href = downloadUrl + "?oldAssets=" + oldAssets + "&maxSize=" + maxFileSize;
                assetExportDialog.hide();
            }
        });
        button.domNode.style.width = "130px";

        buttonDiv.appendChild(button.domNode);

        assetExportDialog.containerNode.appendChild(buttonDiv);


        assetExportDialog.show();
    }

    function createDialogBtn(label, href) {
        return new dijit.form.Button({
            label,
            class: "dialogButton",
            onClick: () => {
                location.href = href;
                assetExportDialog.hide();
            }
        });
    }

</script>

<style>

    .dialogBtnContainer {
        text-align: center; /* Horizontally center the content */
        width: auto;
        display: flex;
        justify-content: center;
        padding: .5rem 0;
        gap: 25px; /* flex | grid */
    }

    .dialogButton {
        width: 30%;
    }

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

.hideSizer{
    font: white;
    font-size: 1%;
}



</style>

<div class="portlet-main" style="height: 100%;">
	<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">

    <html:form styleId="cmsMaintenanceForm" method="POST" action="/ext/cmsmaintenance/view_cms_maintenance" enctype="multipart/form-data">
        <input type="hidden" name="userId"  id="userId" value="<%=user.getUserId()%>">
        <input type="hidden" name="referer" value="<%=referer%>">
        <input type="hidden" name="cacheName" id="cacheName">
        <input type="hidden" name="systemJobName" id="systemJobName">
        <input type="hidden" name="systemJobGroup" id="systemJobGroup">
        <input type="hidden" name="dataOnly" id="dataOnly">
        <input type="hidden" name="cmd" value="">
        <input type="hidden"  name="defaultStructure" id="defaultStructure" value="">
        <input type="hidden" name="shards" id="numberOfShards" value="<%=Config.getIntProperty("es.index.number_of_shards", 2)%>">

        <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
        <!-- START Cache TAB -->
        <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
        <div id="cache" dojoType="dijit.layout.ContentPane" title="<%= UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Cache")) %>" style="padding-bottom: 0;" >

            <table class="listingTable shadowBox">
                <tr>
                    <th><%= LanguageUtil.get(pageContext,"Cache") %></th>
                    <th></th>
                </tr>
                <tr>
                    <td>&nbsp;</td>
                    <td align="right" style="white-space: nowrap;">
                        <select id="cache_list" name="cName" dojoType="dijit.form.ComboBox" autocomplete="true" value="<%= LanguageUtil.get(pageContext,"Flush-All-Caches") %>">
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
                    <th><%= LanguageUtil.get(pageContext,"Menus-File-Store") %></th>
                    <th></th>
                </tr>
                <tr>
                    <td>&nbsp;</td>
                    <td align="right">
                        <button dojoType="dijit.form.Button"  onClick="submitform('<%=com.dotmarketing.util.WebKeys.Cache.CACHE_MENU_FILES%>');" iconClass="deleteIcon">
                           <%= LanguageUtil.get(pageContext,"Delete-Menu-Cache") %>
                        </button>
                    </td>
                </tr>
                <tr>
                    <th colspan="2"><%= LanguageUtil.get(pageContext,"Cache-Stats") %></th>
                </tr>
                <tr>
                    <td colspan="2">
                        <div class="buttonRow" style="text-align: right">
                        <button dojoType="dijit.form.Button"  onClick="refreshCache()" iconClass="resetIcon">
                           <%= LanguageUtil.get(pageContext,"Refresh-Stats") %>
                        </button>
                        </div>
                        <div id="cacheStatsCp" dojoType="dojox.layout.ContentPane" parseOnLoad="true" style="text-align: center;min-height: 100px;">


                            <%

                                long maxMemory = Runtime.getRuntime().maxMemory();
                                long totalMemoryInUse = Runtime.getRuntime().totalMemory();
                                long freeMemory = Runtime.getRuntime().freeMemory();
                                long usedMemory = totalMemoryInUse - freeMemory;
                                long availableMemory = maxMemory - usedMemory;

                            %>
                            <div style="padding-bottom:30px;">

                                <table class="listingTable shadowBox" style="width:400px">
                                    <tr>
                                        <th><%= LanguageUtil.get( pageContext, "Total-Memory-Available" ) %> / Xmx
                                        </th>
                                        <td align="right"><%=UtilMethods.prettyByteify( maxMemory )%>
                                        </td>
                                    </tr>
                                    <tr>
                                        <th><%= LanguageUtil.get( pageContext, "Memory-Allocated" ) %>
                                        </th>
                                        <td align="right"><%= UtilMethods.prettyByteify( totalMemoryInUse )%>
                                        </td>
                                    </tr>
                                    <tr>
                                        <th><%= LanguageUtil.get( pageContext, "Filled-Memory" ) %>
                                        </th>
                                        <td align="right"><%= UtilMethods.prettyByteify( usedMemory )%>
                                        </td>
                                    </tr>
                                    <tr>
                                        <th><%= LanguageUtil.get( pageContext, "Free-Memory" ) %>
                                        </th>
                                        <td align="right"><%= UtilMethods.prettyByteify( availableMemory )%>
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
                        <td colspan="2" align="right">
                            <div id="currentIndexDirDiv"></div>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <div id="lastIndexationDiv"></div>

                                <%= LanguageUtil.get(pageContext,"Reindex") %> :
                                <select id="structure" dojoType="dijit.form.ComboBox" style="width:250px;" autocomplete="true" name="structure" onchange="indexStructureChanged();">
                                    <option value="<%= LanguageUtil.get(pageContext,"Rebuild-Whole-Index") %>"><%= LanguageUtil.get(pageContext,"Rebuild-Whole-Index") %></option>
                                    <%

                                        for(ContentType type : structs){%>
                                        <option value="<%=type.variable()%>"><%=type.name()%></option>
                                    <%}%>
                                </select>

                        </td>
                        <td style="text-align:right;white-space:nowrap;" width="350">
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
                            <%= LanguageUtil.get(pageContext,"Optimize-Index-Info") %>
                        </td>
                        <td align="right">
                            <button dojoType="dijit.form.Button" id="idxShrinkBtn" onClick="optimizeIndices()">
                                <%= LanguageUtil.get(pageContext,"Optimize-Index") %>
                            </button>
                         </td>
                    </tr>
                    <tr>
                        <td>
                            <%= LanguageUtil.get(pageContext,"maintenance.index.cache.flush.info") %>
                        </td>
                        <td align="right">
                            <button dojoType="dijit.form.Button"  onClick="flushIndiciesCache()">
                                <%= LanguageUtil.get(pageContext,"maintenance.index.cache.flush") %>
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
                        </td>
                    </tr>
                </table>
            </div>
            <div id="failedReindexRecordsDiv" style="display:none;border:1px solid silver;background-color:#ffdddd;border-radius:10px;width:50%;padding-bottom:15px;margin:auto;text-align: center">

                <div style="padding:15px;margin:auto;text-align: center;font-weight: bold;">
                    <span id="failedReindexRecordsMessage"></span> <%= LanguageUtil.get(pageContext,"Contents-Failed-Reindex") %>
                </div>

                   <button dojoType="dijit.form.Button"  onClick="downloadFailedAsJson();">
                       <%= LanguageUtil.get(pageContext,"Download-Failed-Records-As-JSON") %>
                   </button>
                    &nbsp; &nbsp;
                   <button dojoType="dijit.form.Button"  onClick="deleteFailedRecords();">
                       <%= LanguageUtil.get(pageContext,"Delete-Failed-Reindex-Records") %>
                   </button>

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
                <% if (Config.getBooleanProperty("ALLOW_STARTER_ZIP_GENERATION_ON_DISK", false)) { %>
                    <tr>
                        <td><%= LanguageUtil.get(pageContext,"Backup-to-Zip-file") %></td>
                        <td style="text-align:center;white-space:nowrap;">
                            <div class="inline-form">
                                <button dojoType="dijit.form.Button" onClick="doCreateZip('true');" iconClass="backupIcon">
                                   <%= LanguageUtil.get(pageContext,"Backup-Data-Only") %>
                                </button>
                                <button dojoType="dijit.form.Button" onClick="doCreateZip('false');" iconClass="backupIcon">
                                  <%= LanguageUtil.get(pageContext,"Backup-Data/Assets") %>
                                </button>
                            </div>
                        </td>
                    </tr>
                <% } %>
                <tr>
                    <td><%= LanguageUtil.get(pageContext,"Download-Assets") %></td>
                    <td style="text-align:center;white-space:nowrap;">
                        <div class="inline-form">
                            <button dojoType="dijit.form.Button" onclick="createAssetExportDialog('/api/v1/maintenance/_downloadAssets')" iconClass="downloadIcon">
                                <%= LanguageUtil.get(pageContext,"Download-Assets") %>
                            </button>
                        </div>
                    </td>
                </tr>
                <% if (DbConnectionFactory.isPostgres()) { %>
                    <tr>
                        <td><%= LanguageUtil.get(pageContext,"Download-DB-Dump") %></td>
                        <td style="text-align:center;white-space:nowrap;">
                            <div class="inline-form">
                                <button dojoType="dijit.form.Button" onclick="downloadDb('/api/v1/maintenance/_downloadDb')" iconClass="backupIcon">
                                    <%= LanguageUtil.get(pageContext,"Download-DB-Dump") %>
                                </button>
                            </div>
                        </td>
                    </tr>
                <% } %>
                <tr>
                    <td><%= LanguageUtil.get(pageContext,"Download-Starter-ZIP") %></td>
                    <td style="text-align:center;white-space:nowrap;">
                        <div class="inline-form">
                            <button dojoType="dijit.form.Button" onClick="location.href='/api/v1/maintenance/_downloadStarter'" iconClass="downloadIcon">
                                <%= LanguageUtil.get(pageContext,"Download-Data-Only") %>
                            </button>

                            <button dojoType="dijit.form.Button" onclick="createAssetExportDialog('/api/v1/maintenance/_downloadStarterWithAssets')" iconClass="downloadIcon">
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


            <table class="listingTable">
                <tr>
                    <th><%= LanguageUtil.get(pageContext,"shutdown.dotcms.button") %></th>
                </tr>
                <tr>
                    <td align="center" class="warning">

                    <%if(System.getProperty("DOTCMS_CLUSTER_RESTART")!=null){ %>

                        <div style="margin:auto;width:50%;background-color:pink;padding:50px;border-radius:20px;">
                             System restarting @ <%=System.getProperty("DOTCMS_CLUSTER_RESTART")%>
                             <br>&nbsp;<br>
                            <button dojoType="dijit.form.Button" onClick="doShutdownDotcms();"  id="doShutdownDotcms">
                               <%= LanguageUtil.get(pageContext,"shutdown.dotcms.button.force") %>
                            </button>


                        </div>

                    <%} else { %>

                       <div style="margin:auto;width:55%;padding:50px;text-align: justify;line-height:1.5">
                           <%= LanguageUtil.get(pageContext,"shutdown.dotcms.disclaimer") %>
                       </div>

                       <div style="margin:auto;width:50%;background-color:pink;padding:50px;border-radius:20px;">
                          <div style="display:grid;grid-template-columns: 50% 50%;width:100%">
                             <div style="text-align: right;padding:10px 10px">
                                <%= LanguageUtil.get(pageContext,"shutdown.dotcms.consent") %>:
                             </div>
                             <div style="text-align: left;padding:10px 10px">
                                  <input dojoType="dijit.form.CheckBox" type="checkbox" id="agreeToShutdown" name="agreeToShutdown" value="true"><label for="agreeToShutdown">
                             </div>
                             <div style="text-align: right;padding:10px 10px">
                                   <%= LanguageUtil.get(pageContext,"shutdown.dotcms.cluster") %>:
                             </div>
                             <%if(System.getProperty("DOTCMS_CLUSTER_RESTART")==null){ %>
                                <div style="text-align: left;padding:10px 10px">
                                     <input dojoType="dijit.form.CheckBox" type="checkbox" id="rollingShutdown" value="true" name="rollingShutdown" onclick="toggleRollingShutdown()">
                                </div>
                             <%} %>

                          </div>
                          <%if(System.getProperty("DOTCMS_CLUSTER_RESTART")==null){ %>
                          <div id="rollingShutdownInfo" style="display:none">
                          <div style="display:grid;grid-template-columns: 50% 50%;width:100%">
                             <div style="text-align: right;padding:15px;vertical-align: middle;">

                                     Rolling Delay in Seconds:
                             </div>
                            <div style="text-align:left;align:left;padding-top:15px">

                                <input dojoType="dijit.form.NumberTextBox"
                                    type="text"
                                    id="rollingRestartDelay"
                                    name="rollingRestartDelay"
                                    value="<%=rollingRestartDelay %>"
                                    style="width:100px"
                                    invalidMessage="Please enter only numbers"
                                    constraints="{ min:0,max:6000,places:0,pattern:'####'}">
                                   <div style="padding:10px;font-size:95%">
                                       <%= LanguageUtil.get(pageContext,"shutdown.dotcms.rolling.message") %>
                                  </div>
                              </div>
                            </div>
                            </div>
                            <%} %>
                           <div style="padding:15 15 0 15">
                            <button dojoType="dijit.form.Button" onClick="doShutdownDotcms();"  id="doShutdownDotcms">
                               <%= LanguageUtil.get(pageContext,"shutdown.dotcms.button") %>
                            </button>
                         </div>

                      <% } %>
                    </td>
                </tr>

            </table>


            <%
            	  List<Host> hosts = new ArrayList<Host>();
            	try{
            		hosts = APILocator.getHostAPI().getHostsWithPermission(PermissionAPI.PERMISSION_READ,user,false);
            	}
            	catch(Exception e){
            		Logger.error(this.getClass(), "Unable to list Hosts: " + e.getMessage());
            	}
            %>

        </div>

    </html:form>

    <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
    <!-- START Logging TAB -->
    <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
    <div id="Logging" style="height: calc(100% - 42px);" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "Log-Files") %>" >
		<%@ include file="/html/portlet/ext/cmsmaintenance/tail_log.jsp"%>
    </div>

    <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
    <!-- START System Info TAB -->
    <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
    <div id="systemProps" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "System-Properties") %>" >
        <div style="margin-left:30%;max-width:400px;padding:1em;text-align: center;cursor: pointer"
             onclick="top.location='/dotAdmin/#/c/configuration?mId=1a87&systemProps=1'">
            This tab is moving to the <a href="/dotAdmin/#/c/configuration?mId=1a87&systemProps=1" target="_top">Configuration</a>
            screen.
        </div>

        <%@ include file="/html/portlet/ext/cmsconfig/system_config.jsp" %>
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
             <checkbox id="hideSystemThreads" name="hideSystemThreads" value="true" dojoType="dijit.form.CheckBox"
                       checked="true"></checkbox>&nbsp;&nbsp;
             <label for="hideSystemThreads"><%= LanguageUtil.get(pageContext, "Hide-System-Threads")
                     .replaceAll("-", " ") %>
             </label>&nbsp;&nbsp;
            <button dojoType="dijit.form.Button" onClick="getAllThreads()" iconClass="resetIcon">
                <%= LanguageUtil.get(pageContext,"thread-tab-reload") %>
            </button>
            <button dojoType="dijit.form.Button" onClick="getSysInfo()" iconClass="infoIcon">
                <%= LanguageUtil.get(pageContext,"thread-tab-reload-sysinfo") %>
            </button>
        </div>
		<div style="width: 98%; margin-left:auto; margin-right:auto; margin-top: -55px">
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




    <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
    <!-- START SYSTEM_JOBS -->
    <!-- ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ -->
    <div id="system_jobs" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "scheduler.system.jobs") %>">


        <%@ include file="/html/portlet/ext/cmsmaintenance/system_jobs.jsp" %>



    </div>




</div>
</div>

<div dojoType="dijit.Dialog" id="addIndex" title="Add Index" style="height:150px;width:400px;">

 	<div align="center" style="padding-top: 10px;">
		<label name="index"><%=UtilMethods.escapeDoubleQuotes(LanguageUtil.get(pageContext, "Number-of-Shards"))%></label>
  		<input type="text" id="shards" name="shards" value="<%=Config.getIntProperty("es.index.number_of_shards", 2)%>">
  	</div><br />
  	<div class="buttonRow" align="center">
		<button dojoType="dijit.form.Button" iconClass="cancelIcon" onClick="javascript:dijit.byId('addIndex').hide();"><%= LanguageUtil.get(pageContext, "Cancel") %></button>&nbsp; &nbsp;
		<button id="addButton" dojoType="dijit.form.Button" iconClass="addIcon" onClick="shardCreating()"><%= LanguageUtil.get(pageContext, "Add") %></button>&nbsp; &nbsp;
	</div>

</div>

<script language="Javascript">
dojo.require("dijit.form.DateTextBox");
	dojo.addOnLoad (function(){

		checkReindexation();
		checkFixAsset();
        var countdownId;
        var tab = dijit.byId("mainTabContainer");
		   	dojo.connect(tab, 'selectChild',
			 function (evt) {
			 	selectedTab = tab.selectedChildWidget;
                 if (countdownId) {
                     clearTimeout(countdownId);
                     countdownId = null
                 }

                 if (selectedTab.id == "indexTabCp") {
                     refreshIndexStats();
                 }
                 if (selectedTab.id == "systemProps") {
                     initLoadConfigsAPI();
                     /*
                     document.getElementById("countdownSpan").innerHTML="5";
                     countdownId = setInterval(()=>{
                         let x=5;
                         try{
                             x = parseInt(document.getElementById("countdownSpan").innerHTML);
                         }
                         catch(e){
                             console.error(e);
                         }
                         if(!x || isNaN(x)){
                             x=5;
                             document.getElementById("countdownSpan").innerHTML=x+"";
                         }
                         document.getElementById("countdownSpan").innerHTML=(--x+"");
                         if(x <= 0){
                             top.location="/dotAdmin/#/c/configuration?mId=1a87&systemProps=" + new Date().getTime();
                         }

                     }, 1000);

 */

                 }

			});

		getSysInfo();
		loadUsers();
	});
</script>
