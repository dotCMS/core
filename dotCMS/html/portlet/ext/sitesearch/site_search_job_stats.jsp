<%@page import="com.dotmarketing.portlets.languagesmanager.model.Language"%>
<%@page import="com.dotcms.enterprise.publishing.sitesearch.SiteSearchPublishStatus"%>
<%@page import="com.dotcms.publishing.PublishStatus"%>
<%@page import="com.dotmarketing.beans.Host"%>
<%@page import="com.dotmarketing.quartz.QuartzUtils"%>
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




%>

<table class="listingTable" style="width:99%">
	<tr>
	    <th nowrap><span><%= LanguageUtil.get(pageContext, "") %></span></th>
		<th nowrap><%= LanguageUtil.get(pageContext, "Job") %></th>
		<th nowrap><%= LanguageUtil.get(pageContext, "IndexAlias") %></th>
		<th nowrap><%= LanguageUtil.get(pageContext, "Hosts") %></th>
		<th nowrap><%= LanguageUtil.get(pageContext, "Language") %></th>
		<th nowrap><%= LanguageUtil.get(pageContext, "Cron") %></th>
		<th nowrap><%= LanguageUtil.get(pageContext, "Include/Exclude") %></th>
		<th nowrap><%= LanguageUtil.get(pageContext, "Paths") %></th>
	</tr>
	<%for(ScheduledTask task : ssapi.getTasks()){ 
		List<Host> selectedHosts = new ArrayList<Host>();
		String[] indexHosts = null;
		Object obj = (task.getProperties().get("indexhost") != null) ?task.getProperties().get("indexhost") : new String[0];
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
		String[] languageArr=(String[])task.getProperties().get("langToIndex");
		String languageStr="";
		if(UtilMethods.isSet(languageArr)) {
		    StringBuilder sb=new StringBuilder();
		    for(int i=0;i<languageArr.length;i++) {
		        Language lang=APILocator.getLanguageAPI().getLanguage(Long.parseLong(languageArr[i]));
		        sb.append(lang.getLanguage()).append(" - ").append(lang.getCountry()).append("<br/>");
		    }
		    languageStr=sb.toString();   
		}
		%>
		<tr style="cursor:pointer;" class="trIdxNothing">
		
			 <td nowrap valign="top" align="center">
				<%if(ssapi.isTaskRunning(task.getJobName())){ %>
					<%SiteSearchPublishStatus ps = ssapi.getTaskProgress(task.getJobName()); %>

					<div dojoType="dijit.ProgressBar" progress="<%=(ps.getCurrentProgress() + ps.getBundleErrors())%>" style="width:100px" id="<%=task.getJobName().hashCode()%> %>" maximum="<%=ps.getTotalBundleWork()%>"></div>

				
				<%} else{%>
					<span class="deleteIcon" onclick="deleteJob('<%=URLEncoder.encode(task.getJobName(),"UTF-8")%>')"></span>
				<%} %>
		   	</td>
			<td nowrap valign="top" onclick="showJobSchedulePane('<%=URLEncoder.encode(task.getJobName(),"UTF-8") %>')"><%=task.getJobName() %></td>
			<td valign="top" onclick="showJobSchedulePane('<%=URLEncoder.encode(task.getJobName(),"UTF-8") %>')"><%=task.getProperties().get("indexAlias")%></td>
			<td valign="top" onclick="showJobSchedulePane('<%=URLEncoder.encode(task.getJobName(),"UTF-8") %>')">
			
				<%for(Host h : selectedHosts){ %>
					<%=h.getHostname() %><br>
				
				<%} %>
				<%if(selectedHosts.size() ==0){ %>
					<%= LanguageUtil.get(pageContext, "index-all-hosts") %>
				<%} %>
			</td>
			<td valign="top"><%= languageStr %></td>
			<td valign="top" onclick="showJobSchedulePane('<%=URLEncoder.encode(task.getJobName(),"UTF-8") %>')"><%=task.getProperties().get("CRON_EXPRESSION")%></td>
			<td valign="top" onclick="showJobSchedulePane('<%=URLEncoder.encode(task.getJobName(),"UTF-8") %>')">
				<%=task.getProperties().get("includeExclude")%>
				<%if(!"all".equals(task.getProperties().get("includeExclude"))){ %>:
					<br>
					<%=UtilMethods.htmlLineBreak(UtilMethods.webifyString((String) task.getProperties().get("paths")))%>
				<%} %>
			</td>
			<td valign="top" onclick="showJobSchedulePane('<%=URLEncoder.encode(task.getJobName(),"UTF-8") %>')"></td>
			
			
			</td>


		</tr>
	<%} %>
	<%if(ssapi==null ||ssapi.getTasks() == null || ssapi.getTasks().size() ==0) {%>
		<tr>
			<td colspan="100" align="center">
				<div style="padding:30px;">
					<a href="#" onclick="showJobSchedulePane('');"><%= LanguageUtil.get(pageContext,"No-Results-Found") %></a>
				</div>
			</td>
			
		</tr>
	<%}else{ %>
	
			<tr>
			<td colspan="100" align="center">
				<div style="padding:30px;">
						<button dojoType="dijit.form.Button"
							id="refreshButton" onClick="refreshJobStats()"
							iconClass="resetIcon"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Refresh")) %>
						</button>
					
				</div>
			</td>
			
		</tr>
	<%} %>
</table>