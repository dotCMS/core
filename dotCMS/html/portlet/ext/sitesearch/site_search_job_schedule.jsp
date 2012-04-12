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
String submitURL = "test";
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

String QUARTZ_JOB_NAME =  "SiteSearchJob" ;


CRON_EXPRESSION = UtilMethods.webifyString(request.getParameter("CRON_EXPRESSION"));
pathsToIgnore  = UtilMethods.webifyString(request.getParameter("pathsToIgnore"));
extToIgnore    = UtilMethods.webifyString(request.getParameter("extToIgnore"));
indexAll = !UtilMethods.isSet(request.getParameter("indexAll"))?false:Boolean.valueOf((String)request.getParameter("indexAll"));
indexHosts = request.getParameterValues("indexhost");


List<String> indexes = ssapi.listIndices();

SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
SimpleDateFormat tdf = new SimpleDateFormat("HH:mm");

Date startDate = new Date(0);
Date endDate = new Date();
String startDateStr = sdf.format(endDate);
String endDateStr = sdf.format(new Date());
String startTimeStr = tdf.format(startDate);
String endTimeStr = tdf.format(endDate);
boolean hasPath = false;
%>


<form dojoType="dijit.form.Form"  name="sitesearch" id="sitesearch" action="/DotAjaxDirector/com.dotmarketing.sitesearch.ajax.SiteSearchAjaxAction/cmd/scheduleJob" method="post">
<table style="align:center;width:800px;" class="listingTable">

	<tr>
		<td align="right" valign="top" nowrap="true">
			<span class="required"></span> <strong><%= LanguageUtil.get(pageContext, "name") %></strong>: 
		</td>
		<td>
			<input name="QUARTZ_JOB_NAME" id="QUARTZ_JOB_NAME" type="text" dojoType='dijit.form.ValidationTextBox' style='width: 400px' value="<%=QUARTZ_JOB_NAME %>" size="200" />
		</td>
	</tr>
	
	<tr>
		<td align="right" valign="top" nowrap="true">
			<span class="required"></span> <strong><%= LanguageUtil.get(pageContext, "select-hosts-to-index") %>:</strong> <a href="javascript: ;" id="hostsHintHook">?</a> <span dojoType="dijit.Tooltip" connectId="hostsHintHook" id="hostsHint" class="fieldHint"><%=LanguageUtil.get(pageContext, "hosts-hint") %></span>
		</td>
		<td>
	
			<div class="selectHostIcon"></div>
				<select id="hostSelector" name=hostSelector" dojoType="dijit.form.FilteringSelect"  store="HostStore"  pageSize="30" labelAttr="hostname"  searchAttr="hostname"  invalidMessage="<%= LanguageUtil.get(pageContext, "Invalid-option-selected")%>" <%=indexAll?"disabled":"" %>>></select>
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
			<strong><%= LanguageUtil.get(pageContext, "index-all-hosts") %>: </strong><input name="indexAll" id="indexAll" dojoType="dijit.form.CheckBox" type="checkbox" value="true" <%=!indexAll?"":"checked"%> onclick="indexAll(this.checked)" />
		</td>
	</tr>
	
	<tr>
		<td align="right" valign="top" nowrap="true">
			<span class="required"></span> <strong><%= LanguageUtil.get(pageContext, "Index-Name") %>: </strong>
		</td>
		<td>
			<select id="indexName" name="indexName" dojoType="dijit.form.FilteringSelect">
			<option value=""><%= LanguageUtil.get(pageContext, "New-Index") %></option>
				<%for(String x : indexes){ %>
					<option value="<%=x%>"><%=x%> <%=(x.equals(APILocator.getIndiciesAPI().loadIndicies().site_search)) ? "(" +LanguageUtil.get(pageContext, "active") +") " : ""  %></option>
				<%} %>
			</select>
		</td>
	</tr>
	
	<tr>
		<td align="right" valign="top" nowrap="true">
			<span class="required"></span> <strong><%= LanguageUtil.get(pageContext, "cron-expression") %>: </strong> 
		</td>
		<td>
			<input name="CRON_EXPRESSION" id="cronExpression" type="text" dojoType='dijit.form.TextBox' style='width: 200px'" value="<%=showBlankCronExp?"":CRON_EXPRESSION %>" size="10" />
			
			
			
			
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
		<td align="right" valign="top" nowrap="true">
			<strong><%= LanguageUtil.get(pageContext, "Date-Range") %>: </strong> <a href="javascript: ;" id="dateRangeHintHook1">?</a> <span dojoType="dijit.Tooltip" connectId="dateRangeHintHook1" class="fieldHint"><%=LanguageUtil.get(pageContext, "date-range-hint") %></span>
		</td>
		<td>
			<div style="padding:0px;">
				<input checked="false" type="checkbox" dojoType="dijit.form.CheckBox" id="incremental" name="incremental" value="true"><label for="incremental">&nbsp;<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Incremental")) %></label> &nbsp; &nbsp; &nbsp; 
			</div>
			<div style="padding:4px;">
				<div style="width:50px;float:left;display: block-inline">
					<%= LanguageUtil.get(pageContext, "Start:") %>
				</div>
				<input type="text" name="startDateDate" value="<%=startDateStr %>" dojoType="dijit.form.DateTextBox">  
				<input type="text" name="startDateTime" value="<%=startTimeStr %>" dojoType="dijit.form.TimeTextBox">
			</div>
			<div style="padding:4px;">
				<div style="width:50px;float:left;display: block-inline">
					<%= LanguageUtil.get(pageContext, "End:") %>
				</div>
				<input type="text" name="endDateDate" value="<%=endDateStr %>" dojoType="dijit.form.DateTextBox">  
				<input type="text" name="endDateTime" value="<%=endTimeStr %>" dojoType="dijit.form.TimeTextBox">
			</div>
		</td>
	</tr>
		<tr>
		<td align="right" valign="top" nowrap="true">
			<strong><%= LanguageUtil.get(pageContext, "Paths") %>: </strong>
			<a href="javascript: ;" id="pathsHintHook1">?</a> <span dojoType="dijit.Tooltip" connectId="pathsHintHook1" id="pathsHint1" class="fieldHint"><%=LanguageUtil.get(pageContext, "paths-hint") %></span>
			
			
			
		</td>
		<td>
			<div style="padding:0px;">
				<input onclick="changeIncludeExclude()" checked="true" type="radio" dojoType="dijit.form.RadioButton" id="includeAll" name="includeExclude" value="all"><label for="includeAll">&nbsp;<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "All")) %></label> &nbsp; &nbsp; &nbsp; 
				<input onclick="changeIncludeExclude()" type="radio" dojoType="dijit.form.RadioButton" id="include" name="includeExclude" value="include"><label for="include">&nbsp;<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Include")) %></label> &nbsp; &nbsp; &nbsp; 
				<input onclick="changeIncludeExclude()" type="radio" dojoType="dijit.form.RadioButton" id="exclude" name="includeExclude" value="exclude"><label for="exclude">&nbsp;<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Exclude")) %></label>
			</div>
			<br>
			<textarea  name="paths" id="paths" <%=(hasPath) ? "" : "disabled='true' " %> type="text" dojoType='dijit.form.Textarea' style='width: 400px;min-height:70px;'" value="" /><%=(hasPath) ? "" : "/*" %></textarea>
		</td>
	</tr>
	<tr>
		<td align="center" valign="top" nowrap="true" colspan="2">

			<div class="buttonRow">
		
			<button dojoType="dijit.form.Button"
				id="saveButton" onClick="scheduleJob()"
				iconClass="saveIcon"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Schedule")) %>
			</button>
			&nbsp; &nbsp; &nbsp; 
			<button dojoType="dijit.form.Button"
				id="saveAndExecuteButton" onClick="runNow();"
				iconClass="saveIcon"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Execute")) %>
				</button>
				</div>
			</td>
		</tr>
	
	</table>
</form>