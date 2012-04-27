<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="com.dotmarketing.quartz.ScheduledTask"%>
<%@page import="com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo"%>
<%@page import="com.dotmarketing.sitesearch.business.SiteSearchAPI"%>
<%@page import="com.dotcms.content.elasticsearch.business.ContentletIndexAPI"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotmarketing.exception.DotSecurityException"%>
<%@page import="org.elasticsearch.action.admin.cluster.health.ClusterIndexHealth"%>
<%@page import="com.dotcms.content.elasticsearch.util.ESClient"%>
<%@page import="org.elasticsearch.action.admin.indices.status.IndexStatus"%>
<%@page import="com.dotcms.content.elasticsearch.util.ESUtils"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="com.dotmarketing.portlets.contentlet.model.Contentlet"%>
<%@page import="com.dotcms.content.elasticsearch.business.ESIndexAPI"%>
<%@page import="com.dotmarketing.portlets.cmsmaintenance.factories.CMSMaintenanceFactory"%>
<%@page import="com.dotmarketing.portlets.structure.factories.StructureFactory"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="org.jboss.cache.Cache"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.dotmarketing.business.CacheLocator"%>
<%@page import="com.dotmarketing.business.DotJBCacheAdministratorImpl"%>
<%@page import="java.util.Map"%>
<%@ include file="/html/common/init.jsp"%>
<%@page import="java.util.List"%>
<%
SiteSearchAPI ssapi = APILocator.getSiteSearchAPI();
Map<String, Object> props = new HashMap<String, Object>();
if(request.getParameter("jobName") != null){
	try{
		props = ssapi.getTask(request.getParameter("jobName")).getProperties();
	}
	catch(Exception e){
		
	}
}

List<String> indexes = ssapi.listIndices();

SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
SimpleDateFormat tdf = new SimpleDateFormat("HH:mm:ss");

Date startDate = new Date(0);
Date endDate = new Date();
String startDateDate = (props.get("startDateDate") != null) ? (String) props.get("startDateDate"): "" ;//sdf.format(startDate);
String endDateDate = (props.get("endDateDate") != null) ? (String) props.get("endDateDate"): "" ;//ssdf.format(new Date());
String startDateTime = (props.get("startDateTime") != null) ? (String) props.get("startDateTime"): "" ;//s"T" + tdf.format(startDate);
String endDateTime = (props.get("endDateTime") != null) ? (String) props.get("endDateTime"): "" ;//s"T" + tdf.format(endDate);

String QUARTZ_JOB_NAME =  UtilMethods.isSet((String) props.get("QUARTZ_JOB_NAME")) ? (String) props.get("QUARTZ_JOB_NAME"): "" ;
String CRON_EXPRESSION = UtilMethods.webifyString((String) props.get("CRON_EXPRESSION"));

boolean runNow = false;

try{
	runNow =new Boolean((String)props.get("RUN_NOW")) ;
}
catch(Exception e){
}

String indexName = UtilMethods.webifyString((String) props.get("indexName"));
boolean incremental = UtilMethods.isSet((String) props.get("incremental"));
List<Host> selectedHosts = new ArrayList<Host>();
boolean indexAll = UtilMethods.isSet((String) props.get("indexAll")) ? true : false;
String[] indexHosts = null;
Object obj = (props.get("indexhost") != null) ?props.get("indexhost") : new String[0];
if(obj instanceof String){
	indexHosts = new String[] {(String) obj};
}
else{
	indexHosts = (String[]) obj;
}


for(String x : indexHosts){
	try{
		selectedHosts.add(APILocator.getHostAPI().find(x, user, true));
	}
	catch(Exception e){}
}

boolean hasDefaultIndex = APILocator.getIndiciesAPI().loadIndicies().site_search != null;




String paths = UtilMethods.webifyString((String) props.get("paths"));
String includeExclude = (String) props.get("includeExclude") ==null ? "all": (String) props.get("includeExclude");


boolean hasPath = false;


%>


<style>
	.showScheduler:<%=(runNow) ? "display: none; " : "display: ; " %>;
</style>
<form dojoType="dijit.form.Form"  name="sitesearch" id="sitesearch" action="/DotAjaxDirector/com.dotmarketing.sitesearch.ajax.SiteSearchAjaxAction/cmd/scheduleJob" method="post">
	<table style="align:center;width:800px;" class="listingTable">
	
	
		<tr>
			<td align="right" valign="top" nowrap="true">
				<b><%= LanguageUtil.get(pageContext, "Run") %></b>
			</td>
			<td>
				<input type="radio" onclick="runNow()" <%=(runNow) ? "checked='true'" : "" %> dojoType="dijit.form.RadioButton" name="RUN_NOW" id="whenToRunNow" value="true"><label for="whenToRunNow"><%= LanguageUtil.get(pageContext, "Now") %></label>&nbsp; &nbsp;
				<input type="radio" onclick="runNow()" <%=(!runNow) ? "checked='true'" : "" %> dojoType="dijit.form.RadioButton" name="RUN_NOW" id="whenToRunCron" value="false"><label for="whenToRunCron"><%= LanguageUtil.get(pageContext, "Scheduled") %></label>
			</td>
		</tr>
		
		
		<tr class="showScheduler" >
			<td align="right" valign="top" nowrap="true">
				<span class="required"></span> <strong><%= LanguageUtil.get(pageContext, "name") %></strong>: 
			</td>
			<td>
				<input name="QUARTZ_JOB_NAME" id="QUARTZ_JOB_NAME" type="text" dojoType='dijit.form.ValidationTextBox' regExp="[\w -]+" required="true" style='width: 400px' value="<%=QUARTZ_JOB_NAME %>" size="200" />
			</td>
		</tr>
		
		
		
		
		<tr>
			<td align="right" valign="top" nowrap="true">
				<span class="required"></span> <strong><%= LanguageUtil.get(pageContext, "select-hosts-to-index") %>:</strong> <a href="javascript: ;" id="hostsHintHook">?</a> <span dojoType="dijit.Tooltip" connectId="hostsHintHook" id="hostsHint" class="fieldHint"><%=LanguageUtil.get(pageContext, "hosts-hint") %></span>
			</td>
			<td>
				<select id="hostSelector" name="hostSelector" dojoType="dijit.form.FilteringSelect"  store="HostStore"  pageSize="30" labelAttr="hostname"  searchAttr="hostname"  invalidMessage="<%= LanguageUtil.get(pageContext, "Invalid-option-selected")%>" <%=indexAll?"disabled=true":"" %> required="false"></select>
					<button id="addHostButton" dojoType="dijit.form.Button" type="button" iconClass="plusIcon" onclick="addNewHost()" <%=indexAll?"disabled":"" %>><%= LanguageUtil.get(pageContext, "Add-Host") %></button>
					<br />
				
					<table class="listingTable" id="hostTable" style="margin:10px;width:90%">
						<tr>
						    <th nowrap style="width:60px;"><span><%= LanguageUtil.get(pageContext, "Delete") %></span></th>
							<th nowrap><%= LanguageUtil.get(pageContext, "Host") %></th>
						</tr>
				
						<%if(!indexAll){ %>
					  		<% for (int k=0;k<selectedHosts.size();k++) { %>
								<%Host host = selectedHosts.get(k); %>
								<%boolean checked =  false; %>
								<%if(!host.isSystemHost()){ %>
						   
							    	<%String str_style = ((k%2)==0)  ? "class=\"alternate_1\"" :  "class=\"alternate_2\""; %>
									<tr id="<%=host.getIdentifier()%>" <%=str_style %>>
									    <td nowrap>
									       	<a href="javascript:deleteHost('<%=host.getIdentifier()%>');"><span class="deleteIcon"></span></a>
									    </td>
										<td nowrap><%= host.getHostname() %></td>
										<td nowrap="nowrap" style="overflow:hidden; display:none; "> <input type="hidden" name="indexhost" id="indexhost<%= host.getIdentifier() %>" value="<%= host.getIdentifier() %>" /></td>
							
									</tr>
								<%} %>
							<%}%>
				        <%} %>
						<% if (indexAll || selectedHosts.size()==0) { %>
						<tr id= "nohosts">
							<td colspan="2">
								<div class="noResultsMessage"><%= indexAll?LanguageUtil.get(pageContext, "all-hosts-selected"):LanguageUtil.get(pageContext, "no-hosts-selected") %></div>
							</td>
						</tr>
						<% } %>
					</table>
				<br />
				<strong><%= LanguageUtil.get(pageContext, "index-all-hosts") %>: </strong><input name="indexAll" id="indexAll" dojoType="dijit.form.CheckBox" type="checkbox" value="true" <%=!indexAll?"":"checked='true'"%> onclick="indexAll(this.checked)" />
			</td>
		</tr>
		
		
		
		<tr>
			<td align="right" valign="top" nowrap="true">
				<span class="required"></span> <strong><%= LanguageUtil.get(pageContext, "Index-Name") %>: </strong>
			</td>
			<td>
				<select id="indexName" name="indexName" dojoType="dijit.form.FilteringSelect">
				<%if(hasDefaultIndex){ %><option value="DEFAULT" <%=("DEFAULT".equals(indexName)) ? "selected='true'":"" %>><%= LanguageUtil.get(pageContext, "Default") %></option><%} %>
				<option value="NEWINDEX" <%=("NEWINDEX".equals(indexName)) ? "selected='true'": ""%>><%= LanguageUtil.get(pageContext, "New-Index-Create") %></option>
					<%for(String x : indexes){ %>
						<option value="<%=x%>" <%=(x.equals(indexName)) ? "selected='true'": ""%>><%=x%> <%=(x.equals(APILocator.getIndiciesAPI().loadIndicies().site_search)) ? "(" +LanguageUtil.get(pageContext, "Default") +") " : ""  %></option>
					<%} %>
				</select>
			</td>
		</tr>
		
	
	
		<tr>
			<td align="right" valign="top" nowrap="true">
				<strong><%= LanguageUtil.get(pageContext, "Include-Date-Range") %>: </strong> <a href="javascript: ;" id="dateRangeHintHook1">?</a> <span dojoType="dijit.Tooltip" connectId="dateRangeHintHook1" class="fieldHint"><%=LanguageUtil.get(pageContext, "date-range-hint") %></span>
			</td>
			<td>
				<div style="padding:5px;">
					<input  type="checkbox" dojoType="dijit.form.CheckBox" id="incremental" name="incremental" value="true" <%=(incremental) ? "checked='true'": "" %>><label for="incremental">&nbsp;<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Incremental")) %></label> &nbsp; &nbsp; &nbsp; 
				</div>
				<!--  
				<div style="padding:4px;">
					<div style="width:50px;float:left;display: block-inline">
						<%= LanguageUtil.get(pageContext, "Start:") %>
					</div>
					<input type="text" id="startDateDate" name="startDateDate" value="<%=startDateDate %>" dojoType="dijit.form.DateTextBox" disabled="<%=(incremental)%>">  
					<input type="text" id="startDateTime" name="startDateTime" value="<%=startDateTime %>" dojoType="dijit.form.TimeTextBox" disabled="<%=(incremental)%>">
				</div>
				<div style="padding:4px;">
					<div style="width:50px;float:left;display: block-inline">
						<%= LanguageUtil.get(pageContext, "End:") %>
					</div>
					<input type="text" id="endDateDate" name="endDateDate" value="<%=endDateDate %>" dojoType="dijit.form.DateTextBox" disabled="<%=(incremental)%>">  
					<input type="text" id="endDateTime" name="endDateTime" value="<%=endDateTime %>" dojoType="dijit.form.TimeTextBox" disabled="<%=(incremental)%>">
				</div>
				-->
			</td>
		</tr>
		
		
		<tr>
			<td align="right" valign="top" nowrap="true">
				<strong><%= LanguageUtil.get(pageContext, "Paths") %>: </strong>
				<a href="javascript: ;" id="pathsHintHook1">?</a> <span dojoType="dijit.Tooltip" connectId="pathsHintHook1" id="pathsHint1" class="fieldHint"><%=LanguageUtil.get(pageContext, "paths-hint") %></span>
				
				
				
			</td>
			<td>
				<div style="padding:0px;">
					<input onclick="changeIncludeExclude()"  type="radio" dojoType="dijit.form.RadioButton" id="includeAll" name="includeExclude" value="all" <%="all".equals(includeExclude) ? "checked='true'" : ""%>     ><label for="includeAll">&nbsp;<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "All")) %></label> &nbsp; &nbsp; &nbsp; 
					<input onclick="changeIncludeExclude()"  type="radio" dojoType="dijit.form.RadioButton" id="include" name="includeExclude" value="include" <%="include".equals(includeExclude) ? "checked='true'" : ""%>><label for="include">&nbsp;<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Include")) %></label> &nbsp; &nbsp; &nbsp; 
					<input onclick="changeIncludeExclude()"  type="radio" dojoType="dijit.form.RadioButton" id="exclude" name="includeExclude" value="exclude" <%="exclude".equals(includeExclude) ? "checked='true'" : ""%>><label for="exclude">&nbsp;<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Exclude")) %></label>
				</div>
				<br>
				<textarea  name="paths" id="paths" <%=("all".equals(includeExclude)) ? "disabled='true'" :"" %> type="text" dojoType='dijit.form.Textarea' style='width: 400px;min-height:70px;'" value="" /><%=(UtilMethods.isSet(paths)) ? paths : "/*" %></textarea>
			</td>
		</tr>
		
		
		
		<tr class="showScheduler">
			<td align="right" valign="top" nowrap="true">
				<span class="required"></span> <strong><%= LanguageUtil.get(pageContext, "cron-expression") %>: </strong> <br>
			</td>
			<td>

				<input name="CRON_EXPRESSION" id="cronExpression" type="text" dojoType='dijit.form.ValidationTextBox' required="true" style='width: 200px'" value="<%=CRON_EXPRESSION %>" size="10" />
				 <div style="width: 350px; margin:20px; text-align: left;" id="cronHelpDiv" class="callOutBox2">
					<h3><%= LanguageUtil.get(pageContext, "cron-examples") %></h3>
					<span style="font-size: 88%;">
					<p></p>
			        <p><b><%= LanguageUtil.get(pageContext, "cron-once-an-hour") %>:</b> 0 0/60 * * * ?</p> 	
			        <p><b><%= LanguageUtil.get(pageContext, "cron-twice-a-day") %>:</b> 0 0 10-11 ? * *</p> 	
			        <p><b><%= LanguageUtil.get(pageContext, "cron-once-a-day-1am")%>:</b> 0 0 1 * * ?</p> 
					</span>
				</div>

			</td>
		</tr>
		
		
		
		
		<tr>
			<td align="center" valign="top" nowrap="true" colspan="2">
	
				<div class="buttonRow">
					<div class="showScheduler" style='<%=(runNow) ? "display: none; " : "" %>;padding:10px;'>
						<button dojoType="dijit.form.Button"
							id="saveButton" onClick="scheduleJob()"
							iconClass="calListIcon"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Schedule")) %>
						</button>
					</div>

					<div class="showRunNow" style='<%=(!runNow) ? "display: none; " : "" %>;padding:10px;'>
						<button dojoType="dijit.form.Button"
							id="saveAndExecuteButton" onClick="scheduleJob();"
							iconClass="republishIcon"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Execute")) %>
						</button>
					</div>
				</div>
			</td>
		</tr>
		
	</table>
</form>