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
		<th nowrap><%= LanguageUtil.get(pageContext, "Cron") %></th>
	</tr>
	<%for(ScheduledTask task : ssapi.getTasks()){ %>
		<tr>
		    <td nowrap><span class="deleteIcon" onclick="deleteJob('<%=URLEncoder.encode(task.getJobName(),"UTF-8")%>')"></span></td>
			<td nowrap><%=task.getJobName() %></td>
		<td nowrap><%=task.getProperties().get("CRON_EXPRESSION") %></td>
		</tr>
	<%} %>
	<%if(ssapi==null ||ssapi.getTasks() == null || ssapi.getTasks().size() ==0) {%>
		<tr>
			<td colspan="100" align="center">
				<div style="padding:20px;">
					<%= LanguageUtil.get(pageContext,"No-Results-Found") %>
				</div>
			</td>
		</tr>
	<%} %>
</table>