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
String submitURL = com.dotmarketing.util.PortletURLUtil.getRenderURL(request,null,null,"EXT_SITESEARCH");
List<Host> selectedHosts = new ArrayList<Host>();
String error = "";
String cronExpression = "";
String pathsToIgnore  = "";
String extToIgnore    = "";
String port           = "";
boolean followQueryString = false;
boolean indexAll = false;
boolean showBlankCronExp = false;
boolean success = false;
String successMsg = LanguageUtil.get(pageContext, "schedule-site-search-success") ;
String[] indexHosts;
CronScheduledTask task = null;
boolean execute = false;
boolean resetSiteSearch = false;

if (request.getMethod().equalsIgnoreCase("POST") ) {
resetSiteSearch = !UtilMethods.isSet(request.getParameter("resetSiteSearch"))?false:Boolean.valueOf((String)request.getParameter("resetSiteSearch"));
if(!resetSiteSearch){
cronExpression = !UtilMethods.isSet(request.getParameter("cronExpression"))?"":request.getParameter("cronExpression");
port = !UtilMethods.isSet(request.getParameter("port"))?"":request.getParameter("port");
pathsToIgnore  = !UtilMethods.isSet(request.getParameter("pathsToIgnore"))?"":request.getParameter("pathsToIgnore");
extToIgnore    = !UtilMethods.isSet(request.getParameter("extToIgnore"))?"":request.getParameter("extToIgnore");
followQueryString = !UtilMethods.isSet(request.getParameter("followQueryString"))?false:Boolean.valueOf((String)request.getParameter("followQueryString"));
execute = !UtilMethods.isSet(request.getParameter("saveAndExecute"))?false:Boolean.valueOf((String)request.getParameter("saveAndExecute"));
indexAll = !UtilMethods.isSet(request.getParameter("indexAll"))?false:Boolean.valueOf((String)request.getParameter("indexAll"));
indexHosts = request.getParameterValues("indexhost");


if(pathsToIgnore.contains("*")){
	error = "invalid.pathname";
}
if(extToIgnore.contains("*")){
	error = "invalid.extensions";	
}

if(UtilMethods.isSet(port)){

	try{
	    Integer.valueOf(port);
	}catch(NumberFormatException e){
		error = "invalid.port.number";	
	}
	
	String patternStr = "^0-9"; 
	java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(patternStr); 
	java.util.regex.Matcher matcher = pattern.matcher(port);
	if(matcher.matches()){
		error = "invalid.port.number";	
	}
	
	
}
if(UtilMethods.isSet(indexHosts) || indexAll){
if(!UtilMethods.isSet(error)){
Map<String, Object> parameters = new HashMap<String, Object>();
parameters.put("cronExpression", cronExpression);
parameters.put("pathsToIgnore", pathsToIgnore);
parameters.put("extToIgnore", extToIgnore);
parameters.put("followQueryString", followQueryString);
parameters.put("indexAll", indexAll);
parameters.put("squentiallyScheduled", false);
parameters.put("runJobAfterSeq", true);
parameters.put("port", port);
List<String> indexHostsList = null;
if(!indexAll){
indexHostsList = Arrays.asList(indexHosts);
parameters.put("hosts", indexHostsList);
}
parameters.put("defaultCronExpression", false);
if(indexAll || (indexHostsList!=null && !indexHostsList.isEmpty())){
List<ScheduledTask> tasks = QuartzUtils.getScheduledTask("site-search", "site-search-group");
CronScheduledTask prevTask = null;

if(tasks!=null && !tasks.isEmpty()){
	
	prevTask = (CronScheduledTask)tasks.get(0);
	//Pause and remove any current jobs in the group
	QuartzUtils.pauseJob("site-search", "site-search-group");
	QuartzUtils.removeTaskRuntimeValues("site-search", "site-search-group");
	QuartzUtils.removeJob("site-search", "site-search-group");	
	successMsg = LanguageUtil.get(pageContext, "schedule-site-search-success-updated") ;
	
}
	Calendar startTime = Calendar.getInstance();
	String exp = cronExpression;
    if(!UtilMethods.isSet(cronExpression)){
       //This is so the task can be saved, the job should be paused afterwards 
	   exp="0 0/10 * * * ?";
	   parameters.put("cronExpression", exp);
	   parameters.put("defaultCronExpression", true);
	   showBlankCronExp = true;
	   parameters.put("runJobAfterSeq", false);

    }
    
  

	
    //Create new task with updated cron expression and properties
	task = new CronScheduledTask("site-search", "site-search-group", "Site Search ", SiteSearchJobProxy.class.getCanonicalName(), false,
			"site-search-trigger", "site-search-trigger-group", startTime.getTime(), null,
			SimpleTrigger.INSTRUCTION_SET_TRIGGER_COMPLETE, 5, true, parameters, exp);
if(task!=null){

	
try{
	 QuartzUtils.scheduleTask(task);
	 if(!UtilMethods.isSet(cronExpression)||execute){
			//Pausing the job if no cron expression
			QuartzUtils.pauseJob("site-search", "site-search-group");
		}

}catch(Exception e){
	QuartzUtils.pauseJob("site-search", "site-search-group");
	if(e instanceof java.text.ParseException){
		error = "invalid.cron.expression";
		if(prevTask!=null){
			cronExpression = prevTask.getCronExpression();
		}
		parameters.put("cronExpression", cronExpression);
		if(prevTask!=null && (Boolean)prevTask.getProperties().get("defaultCronExpression")){
			showBlankCronExp= true;
            parameters.put("defaultCronExpression", true);
     	    parameters.put("runJobAfterSeq", false);	
		}else{
			parameters.put("runJobAfterSeq", true);	
		}
		task = new CronScheduledTask("site-search", "site-search-group", "Site Search ", SiteSearchJobProxy.class.getCanonicalName(), false,
				"site-search-trigger", "site-search-trigger-group", startTime.getTime(), null,
				SimpleTrigger.INSTRUCTION_SET_TRIGGER_COMPLETE, 5, true, parameters, cronExpression);
		 QuartzUtils.scheduleTask(task);
		 if((prevTask!=null && (Boolean)prevTask.getProperties().get("defaultCronExpression"))||execute){
			 QuartzUtils.pauseJob("site-search", "site-search-group");
		 }
	}else{
		error = e.getMessage();
   }	
 }

if(execute){
	if (!QuartzUtils.isJobSequentiallyScheduled("site-search-execute-once", "site-search-execute-once-group")) {
		startTime = Calendar.getInstance();
		parameters.put("squentiallyScheduled", true);
		SimpleScheduledTask scheduledTask = new SimpleScheduledTask("site-search-execute-once", "site-search-execute-once-group", "Site Search ",SiteSearchJobProxy.class.getCanonicalName(), false,
				"site-search-execute-once-trigger", "site-search-execute-once-trigger-group", startTime.getTime(), null,
				SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT, 5, true, parameters, 0, 0);
		try{
		   QuartzUtils.scheduleTask(scheduledTask);
		}catch(Exception e){
			error = "error.executing.job";
			success = true;
			QuartzUtils.removeJob("site-search-execute-once", "site-search-execute-once-group");	
		}
	}else{
		error = "job.already.inprogress" ;
		success = true;
	}
	
	
}

}
selectedHosts = new ArrayList<Host>();
if(!indexAll){
for(String hostId : indexHostsList){
  try {
	Host host =  APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(), false);
	if(host!=null){
		selectedHosts.add(host);
	}
  }catch (Exception e) {
	  error = e.getMessage();
   }
  }
 }
}
}
}else{
	List<ScheduledTask> tasks = QuartzUtils.getScheduledTask("site-search", "site-search-group");
	if(tasks!=null && !tasks.isEmpty()){
	try {
		if (QuartzUtils.isJobSequentiallyScheduled("site-search-execute-once", "site-search-execute-once-group")) {
			QuartzUtils.removeJob("site-search-execute-once", "site-search-execute-once-group");	
		}
		QuartzUtils.pauseJob("site-search", "site-search-group");
		QuartzUtils.removeTaskRuntimeValues("site-search", "site-search-group");
		QuartzUtils.removeJob("site-search", "site-search-group");	
	} catch (SchedulerException e) {
		error = e.getMessage();
	}
	if(!UtilMethods.isSet(error)){
		successMsg = LanguageUtil.get(pageContext, "sitesearch-successfully-deleted") ;
		success = true;
	}
	
	}else{
		error = "select.host";
	}

}

if(!UtilMethods.isSet(error) && !success){
	success = true;
}
}else{
	

	List<ScheduledTask> tasks = QuartzUtils.getScheduledTask("site-search", "site-search-group");
	try {
		if (QuartzUtils.isJobSequentiallyScheduled("site-search-execute-once", "site-search-execute-once-group")) {
			QuartzUtils.removeJob("site-search-execute-once", "site-search-execute-once-group");	
		}else if(tasks==null || tasks.isEmpty()){
			error = "reset-sitesearch-no-scheduled-jobs";
		}
		
		if(tasks!=null && !tasks.isEmpty()){
			QuartzUtils.pauseJob("site-search", "site-search-group");
			QuartzUtils.removeTaskRuntimeValues("site-search", "site-search-group");
			QuartzUtils.removeJob("site-search", "site-search-group");	
		}
	} catch (SchedulerException e) {
		error = e.getMessage();
	}
	
	if(!UtilMethods.isSet(error) && !success){
		successMsg = LanguageUtil.get(pageContext, "reset-sitesearch-success") ;
		success = true;
	}
	
}
}else{
	List<ScheduledTask> tasks = QuartzUtils.getScheduledTask("site-search", "site-search-group");
	
	if(tasks!=null && !tasks.isEmpty()){
	 task = (CronScheduledTask) tasks.get(0);
	 
	if((Boolean)task.getProperties().get("defaultCronExpression")){
	   showBlankCronExp = true;
    }
	
	Map<String, Object> parameters = task.getProperties();
	cronExpression = !UtilMethods.isSet((String)parameters.get("cronExpression"))||showBlankCronExp?"":(String)parameters.get("cronExpression");
	pathsToIgnore  = !UtilMethods.isSet((String)parameters.get("pathsToIgnore"))?"":(String)parameters.get("pathsToIgnore");
	extToIgnore    = !UtilMethods.isSet((String)parameters.get("extToIgnore"))?"":(String)parameters.get("extToIgnore");
	followQueryString = !UtilMethods.isSet(parameters.get("followQueryString"))?false:(Boolean)parameters.get("followQueryString");
	indexAll = !UtilMethods.isSet(parameters.get("indexAll"))?false:(Boolean)parameters.get("indexAll");
	List<String> indexHostsList = (List<String>)parameters.get("hosts");
	port = !UtilMethods.isSet((String)parameters.get("port"))?"":(String)parameters.get("port");
	
    if(!indexAll && indexHostsList!=null && !indexHostsList.isEmpty()){
    	selectedHosts = new ArrayList<Host>();
    	for(String hostId : indexHostsList){
    	  try {
    		Host host =  APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(), false);
			if(host!=null){
				selectedHosts.add(host);
			}
    	  }catch (Exception e) {
    		  error = e.getMessage();
    	  }
    	}
    }
}
}

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


</script>
<style type="text/css">
.listingTable {
	width: 42.5%;
	font-size: 100%;
	border-top: 1px solid #d0d0d0;
}
</style>
<div class="portlet-wrapper">
<div style="min-height: 400px;" id="borderContainer" class="shadowBox headerBox">
<div style="padding: 7px;">
<div>
<h3><%= LanguageUtil.get(pageContext, "javax.portlet.title.EXT_SITESEARCH") %></h3>
</div>
<br clear="all">
</div>

<form name="sitesearch" id="sitesearch" action="<%= submitURL %>"
	method="post">
<dl>
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
	<dd>
	<dt><strong><%= LanguageUtil.get(pageContext, "use-port") %>
	: </strong> <a href="javascript: ;" id="portHintHook">?</a> <span
		dojoType="dijit.Tooltip" connectId="portHintHook" id="portHint"
		class="fieldHint"><%=LanguageUtil.get(pageContext, "port-hint") %></span>
	</dt>
	<dd><input name="port" id="port" type="text"
		dojoType='dijit.form.NumberTextBox' constraints="{pattern: '###'}" style='width: 60px'
		" value="<%=port %>" size="10" maxlength="5"/></dd>
	<dt><strong><%= LanguageUtil.get(pageContext, "cron-expression") %>
	: </strong> <a href="javascript: ;" id="cronHintHook">?</a> <span
		dojoType="dijit.Tooltip" connectId="cronHintHook" id="cronHint"
		class="fieldHint"><%=LanguageUtil.get(pageContext, "cron-hint") %></span>
	</dt>
	<dd><input name="cronExpression" id="cronExpression" type="text"
		dojoType='dijit.form.TextBox' style='width: 200px'
		" value="<%=showBlankCronExp?"":cronExpression %>" size="10" />
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
	<dt><strong><%= LanguageUtil.get(pageContext, "ignore-extensions") %>
	: </strong> <a href="javascript: ;" id="extHintHook">?</a> <span
		dojoType="dijit.Tooltip" connectId="extHintHook" id="extHint"
		class="fieldHint"><%=LanguageUtil.get(pageContext, "ext-hint") %></span>
	</dt>
	<dd><input name="extToIgnore" id="extToIgnore" type="text"
		dojoType='dijit.form.TextBox' style='width: 400px'
		" value="<%=extToIgnore %>" size="200" /></dd>
	<dt><strong><%= LanguageUtil.get(pageContext, "follow-query-string") %>
	: </strong> <a href="javascript: ;" id="queryStrHintHook">?</a> <span
		dojoType="dijit.Tooltip" connectId="queryStrHintHook"
		id="queryStrHint" class="fieldHint"><%=LanguageUtil.get(pageContext, "query-string-hint") %></span>
	</dt>
	<dd><input name="followQueryString" id="followQueryString"
		dojoType="dijit.form.CheckBox" type="checkbox" value="true"
		<%=!followQueryString?"":"checked"%> /></dd>

	<dt></dt>
	<dd>
	
	<input type="hidden" name="saveAndExecute" id="saveAndExecute" value="false" />
	<input type="hidden" name="resetSiteSearch" id="resetSiteSearch" value="false" />

	<button dojoType="dijit.form.Button"
		id="saveButton" onClick="submitfm(document.getElementById('sitesearch'))"
		iconClass="saveIcon"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save")) %>
	</button>
	<button dojoType="dijit.form.Button"
		id="saveAndExecuteButton" onClick="saveAndExecute();"
		iconClass="saveIcon"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Save-and-execute")) %>
	</button>
    <button dojoType="dijit.form.Button"
		id="resetSitesearchButton" onClick="resetSiteSearch();"
		iconClass="deleteIcon"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "reset-sitesearch")) %>
	</button>
	</dd>

</dl>
</form>
</div>
</div>

