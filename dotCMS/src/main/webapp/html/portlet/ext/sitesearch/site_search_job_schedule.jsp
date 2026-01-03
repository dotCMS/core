<%@page import="com.dotcms.content.elasticsearch.business.ESIndexAPI"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotmarketing.sitesearch.business.SiteSearchAPI"%>
<%@ include file="/html/common/init.jsp"%>
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

ESIndexAPI iapi=new ESIndexAPI();
List<String> indexes = ssapi.listIndices();
Map<String,String> alias = iapi.getIndexAlias(indexes);

SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
SimpleDateFormat tdf = new SimpleDateFormat("HH:mm:ss");

Date startDate = new Date(0);
Date endDate = new Date();
String startDateDate = (props.get("startDateDate") != null) ? (String) props.get("startDateDate"): "" ;//sdf.format(startDate);
String endDateDate = (props.get("endDateDate") != null) ? (String) props.get("endDateDate"): "" ;//ssdf.format(new Date());
String startDateTime = (props.get("startDateTime") != null) ? (String) props.get("startDateTime"): "" ;//s"T" + tdf.format(startDate);
String endDateTime = (props.get("endDateTime") != null) ? (String) props.get("endDateTime"): "" ;//s"T" + tdf.format(endDate);
String[] langToIndexArr = (props.get("langToIndex") != null) ? (String[]) props.get("langToIndex"): null ;
String QUARTZ_JOB_NAME =  UtilMethods.isSet((String) props.get("QUARTZ_JOB_NAME")) ? (String) props.get("QUARTZ_JOB_NAME"): "" ;
String CRON_EXPRESSION = UtilMethods.webifyString((String) props.get("CRON_EXPRESSION"));

Set<String> langToIndexSet = new HashSet<String>();
if(UtilMethods.isSet(langToIndexArr))
    langToIndexSet.addAll(Arrays.asList(langToIndexArr));

boolean runNow = false;

try{
	runNow =new Boolean((String)props.get("RUN_NOW")) ;
}
catch(Exception e){
}

String indexName = UtilMethods.webifyString((String) props.get("indexAlias"));
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

boolean hasDefaultIndex = APILocator.getIndiciesAPI().loadLegacyIndices().getSiteSearch() != null;


List<Language> langs=APILocator.getLanguageAPI().getLanguages();

String paths = UtilMethods.webifyString((String) props.get("paths"));
String includeExclude = (String) props.get("includeExclude") ==null ? "all": (String) props.get("includeExclude");


boolean hasPath = false;

final String siteSearch = APILocator.getIndiciesAPI().loadLegacyIndices().getSiteSearch();

%>


<style>
	.showScheduler {
	   <%=(runNow) ? "display: none; " : "" %>
	}
</style>

<form dojoType="dijit.form.Form" style="height:auto" name="sitesearch" id="sitesearch" action="/DotAjaxDirector/com.dotmarketing.sitesearch.ajax.SiteSearchAjaxAction/cmd/scheduleJob" method="post">
	<div class="form-horizontal">
		<dl>
			<dt>
				<b><%= LanguageUtil.get(pageContext, "Run") %></b>
			</dt>
			<dd>
				<div class="radio">
					<input type="radio" onclick="runNow('now')" <%=(runNow) ? "checked='true'" : "" %> dojoType="dijit.form.RadioButton" name="RUN_NOW" id="whenToRunNow" value="true"><label for="whenToRunNow"><%= LanguageUtil.get(pageContext, "Now") %></label>&nbsp; &nbsp;
				</div>
				<div class="radio">
					<input type="radio" onclick="runNow('schedule')" <%=(!runNow) ? "checked='true'" : "" %> dojoType="dijit.form.RadioButton" name="RUN_NOW" id="whenToRunCron" value="false"><label for="whenToRunCron"><%= LanguageUtil.get(pageContext, "Scheduled") %></label>
				</div>
			</dd>
		</dl>

		<dl class="showScheduler">
			<dt>
				<label for="QUARTZ_JOB_NAME" class="required">
					<%= LanguageUtil.get(pageContext, "name") %>
				</label> 
			</dt>
			<dd>
				<input name="QUARTZ_JOB_NAME" id="QUARTZ_JOB_NAME" type="text" dojoType='dijit.form.ValidationTextBox' regExp="[\w -]+" required="true" style='width: 400px' value="<%=QUARTZ_JOB_NAME %>" size="200" />
				<input id="OLD_QUARTZ_JOB_NAME" name="OLD_QUARTZ_JOB_NAME" type="hidden" value="<%=QUARTZ_JOB_NAME%>" />
			</dd>
		</dl>

		<dl>
			<dt>
				<label for="hostSelector" class="required">
					<%= LanguageUtil.get(pageContext, "select-hosts-to-index") %><a href="javascript: ;" id="hostsHintHook">?</a> <span dojoType="dijit.Tooltip" connectId="hostsHintHook" id="hostsHint" class="fieldHint"><%=LanguageUtil.get(pageContext, "hosts-hint") %></span>
				</label>
			</dt>
			<dd>
				<div class="inline-form">
					<select id="hostSelector" name="hostSelector" dojoType="dijit.form.FilteringSelect" store="HostStore" pageSize="30" labelAttr="hostname" searchAttr="hostname" invalidMessage="<%= LanguageUtil.get(pageContext, "Invalid-option-selected")%>" <%=indexAll?"disabled=true":"" %> required="false" style="width:308px"></select>
					<button id="addHostButton" dojoType="dijit.form.Button" type="button" iconClass="plusIcon" onclick="addNewHost()" <%=indexAll?"disabled":"" %>><%= LanguageUtil.get(pageContext, "Add-Host") %></button>
				</div>

				<table class="listingTable job-scheduler__host-list" id="hostTable">
					<thead>
						<tr>
							<th><span><%= LanguageUtil.get(pageContext, "Delete") %></span></th>
							<th nowrap><%= LanguageUtil.get(pageContext, "Host") %></th>
						</tr>
					</thead>

					<%if(!indexAll){ %>
				  		<% for (int k=0;k<selectedHosts.size();k++) { %>
							<%Host host = selectedHosts.get(k); %>
							<%boolean checked = false; %>
							<%if(!host.isSystemHost()){ %>
								<tr id="<%=host.getIdentifier()%>">
								    <td class="job-scheduler__host-list-actions">
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
				
				<div class="checkbox">
					<input name="indexAll" id="indexAll" dojoType="dijit.form.CheckBox" type="checkbox" value="true" <%=!indexAll?"":"checked='true'"%> onclick="indexAll(this.checked)" />
					<%= LanguageUtil.get(pageContext, "index-all-hosts") %>
				</div>
			</dd>
		</dl>

		<dl>
			<dt>
				<label for="indexAlias" class="required">
					<%= LanguageUtil.get(pageContext, "Index-Name") %><a href="javascript: ;" id="aliasHintHook">?</a> <span dojoType="dijit.Tooltip" connectId="aliasHintHook" id="aliasHint" class="fieldHint"><%=LanguageUtil.get(pageContext, "search-alias-hint") %></span>
				</label>
			</dt>
			<dd>
				<select id="indexAlias" name="indexAlias" dojoType="dijit.form.ComboBox" required="true" style="width:400px">
					<%for(final String x : indexes){ %>
						<option value="<%=alias.get(x) == null ? x:alias.get(x)%>" <%=(x.equals(indexName)||(alias.get(x)!=null && alias.get(x).equals(indexName))) ? "selected='true'": ""%>>
						  <%=alias.get(x) == null ? x:alias.get(x) %> <%=(x.equals(siteSearch)) ? "(" +LanguageUtil.get(pageContext, "Default") +") " : ""  %>
						</option>
					<%} %>
				</select>
			</dd>
		</dl>

		<dl>
			<dt></dt>
			<dd>
				<div class="checkbox">
					<input  type="checkbox" dojoType="dijit.form.CheckBox" id="incremental" name="incremental" value="true" <%=(incremental) ? "checked='true'": "" %>>
					<label for="incremental">&nbsp;<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Incremental")) %>&nbsp;</label>
					<a href="javascript: ;" id="incrementalHintHook1">?</a>
					<span dojoType="dijit.Tooltip" connectId="incrementalHintHook1" class="fieldHint">
					  <%=LanguageUtil.get(pageContext, "incremental-hint") %>
					</span>
				</div>

			</dd>
		</dl>

		<dl>
            <dt>
              <label>
					<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Language")) %>
			  </label>
            </dt>
            <dd>
                 <% for(Language lang : langs) { %>
					<div class="checkbox">
						<input type="checkbox" dojoType="dijit.form.CheckBox" id="op_<%=lang.getId()%>"
						name="langToIndex" value="<%=lang.getId()%>"
						<%=(langToIndexSet.contains(Long.toString(lang.getId()))) ? "checked='true'" : "" %>/>
						<label for="op_<%=lang.getId()%>">
							<%= lang.getLanguage() + " - " + lang.getCountry() %>
						</label>
					</div>
                 <% } %>
            </dd>
        </dl>

		<dl>
			<dt>
				<strong><%= LanguageUtil.get(pageContext, "Paths") %> </strong>
				<a href="javascript: ;" id="pathsHintHook1">?</a> <span dojoType="dijit.Tooltip" connectId="pathsHintHook1" id="pathsHint1" class="fieldHint"><%=LanguageUtil.get(pageContext, "paths-hint") %></span>
			</dt>
			<dd>
				<div class="radio">
					<input onChange="changeIncludeExclude()" type="radio" dojoType="dijit.form.RadioButton" id="includeAll" name="includeExclude" value="all" <%="all".equals(includeExclude) ? "checked='true'" : ""%>>
					<label for="includeAll">&nbsp;<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "All")) %></label>
				</div>
				<div class="radio">
					<input onChange="changeIncludeExclude()" type="radio" dojoType="dijit.form.RadioButton" id="include" name="includeExclude" value="include" <%="include".equals(includeExclude) ? "checked='true'" : ""%>>
					<label for="include">&nbsp;<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Include")) %></label>
				</div>
				<div class="radio">
					<input onChange="changeIncludeExclude()" type="radio" dojoType="dijit.form.RadioButton" id="exclude" name="includeExclude" value="exclude" <%="exclude".equals(includeExclude) ? "checked='true'" : ""%>>
					<label for="exclude">&nbsp;<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Exclude")) %></label>
				</div>
				<textarea  name="paths" id="paths" <%=("all".equals(includeExclude)) ? "disabled='true'" :"" %> type="text" dojoType='dijit.form.Textarea' style='width: 400px;min-height:70px;margin-top: 15px;'"><%=(UtilMethods.isSet(paths)) ? paths : "/*" %></textarea>
			</dd>
		</dl>

		<dl class="showScheduler">
			<dt>
				<label for="cronExpression" class="required">
					<%= LanguageUtil.get(pageContext, "cron-expression") %>
				</label>
			</dt>
			<dd>
				<input name="CRON_EXPRESSION" id="cronExpression" type="text" dojoType='dijit.form.ValidationTextBox' required="true" style='width: 400px'" value="<%=CRON_EXPRESSION %>" size="10" />
				 <div style="width: 400px; margin:16px 0; text-align: left;" id="cronHelpDiv" class="callOutBox2">
					<h3><%= LanguageUtil.get(pageContext, "cron-examples") %></h3>
					<p><i><%= LanguageUtil.get(pageContext, "cron-once-an-hour") %>:</i> 0 0/60 * * * ?</p>
					<p><i><%= LanguageUtil.get(pageContext, "cron-twice-a-day") %>:</i> 0 0 10-11 ? * *</p>
					<p><i><%= LanguageUtil.get(pageContext, "cron-once-a-day-1am")%>:</i> 0 0 1 * * ?</p>
				</div>
			</dd>
		</dl>

	</div>

	<div class="buttonRow" style="white-space: nowrap;">
		<button dojoType="dijit.form.Button"
			id="cacnelButton" onClick="showJobsListingPane();"
			class="dijitButtonFlat"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Cancel")) %>
		</button>
		<span class="showRunNow" style='<%=(!runNow) ? "display: none; " : "" %>;'>
			<button dojoType="dijit.form.Button"
				id="saveAndExecuteButton" onClick="scheduleJob();"
				iconClass="republishIcon"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Execute")) %>
			</button>
		</span>
		<span class="showScheduler" style='<%=(runNow) ? "display: none; " : "" %>;'>
			<button dojoType="dijit.form.Button"
				id="saveButton" onClick="scheduleJob()"
				iconClass="calListIcon"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Schedule")) %>
			</button>
		</span>
	</div>
</form>
