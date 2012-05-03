<%@page import="com.dotmarketing.util.StringUtils"%>
<%@page import="java.net.URLDecoder"%>
<%@page import="com.dotcms.publishing.sitesearch.SiteSearchResult"%>
<%@page import="com.dotcms.publishing.sitesearch.DotSearchResults"%>
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

List<Structure> structs = StructureFactory.getStructures();
SiteSearchAPI ssapi = APILocator.getSiteSearchAPI();
ESIndexAPI esapi = APILocator.getESIndexAPI();
IndiciesInfo info=APILocator.getIndiciesAPI().loadIndicies();



String testIndex = request.getParameter("testIndex");
String testQuery = (request.getParameter("testQuery") != null) 
		? request.getParameter("testQuery")
				: "*";


int testStart = 0;
int testLimit = 50;
String testSort = "score";






DotSearchResults results= APILocator.getSiteSearchAPI().search(testIndex, testQuery, testSort, testStart, testLimit);

String myError = (results.getError()!= null && results.getError().indexOf("nested:") > -1) 
				? results.getError().substring(0, results.getError().indexOf("nested:"))  
						: results.getError();

try {
	user = com.liferay.portal.util.PortalUtil.getUser(request);
	if(user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_SITESEARCH", user)){
		throw new DotSecurityException("Invalid user accessing index_stats.jsp - is user '" + user + "' logged in?");
	}
} catch (Exception e) {
	Logger.error(this.getClass(), e.getMessage());
	%>
	
		<div class="callOutBox2" style="text-align:center;margin:40px;padding:20px;">
		<%= LanguageUtil.get(pageContext,"you-have-been-logged-off-because-you-signed-on-with-this-account-using-a-different-session") %><br>&nbsp;<br>
		<a href="/admin"><%= LanguageUtil.get(pageContext,"Click-here-to-login-to-your-account") %></a>
		</div>
		
	<% 
	//response.sendError(403);
	return;
}




%>
	

<table style="width:98%">
	<tr>
		<td>
		<%if(results == null || results.getResults() ==null || results.getResults().size() >0){ %>
			<div style="padding:10px;">
				<b><%= LanguageUtil.get(pageContext,"Results") %></b>: <%= results.getResults().size()%> <%= LanguageUtil.get(pageContext,"of") %> <%=results.getTotalResults() %> <%= LanguageUtil.get(pageContext,"total") %>
			</div>
		<%} %>
		
		</td>
		<td align="right">
			<%if(UtilMethods.isSet(results.getQuery())){ %>
				<div style="padding:10px;">
					<b><%= LanguageUtil.get(pageContext,"Query") %></b>: <%=results.getQuery() %>
				</div>
			<%} %>
		</td>
	</tr>
</table>


<table class="listingTable" style="width:98%">
	<thead>
		<tr>
			<th>Score</th>
			<th>Title</th>
			<th>Author</th>
			<th>ContentLength</th>
			<th>Url</th>
			<th>Uri</th>
			
			<th>MimeType</th>
			<th>FileName</th>
			
		</tr>
	</thead>
	
	
	<%if(results == null || results.getResults() ==null || results.getResults().size() ==0){ %>
		
		<%if(UtilMethods.isSet(results.getError()) && UtilMethods.isSet(results.getQuery())){ %>	
			<tr>
				<td colspan="100" align="center">
					<div style="padding:20px;">
						<b><%= LanguageUtil.get(pageContext,"Error") %>:</b><br>
					<%=myError %>
				</div>
				</td>
			</tr>
		<%}else{ %>
	
			<tr>
				<td colspan="100" align="center">
					<div style="padding:20px;">
						<%= LanguageUtil.get(pageContext,"No-Results-Found") %>
					</div>
				</td>
			</tr>
		<%} %>
	<%}else{ %>
		<%for(SiteSearchResult ssr : results.getResults()){ %>
			<tr>
				<td><%=ssr.getScore() %></td>
				<td><%=UtilMethods.webifyString(ssr.getTitle()) %></td>
				<td><%=UtilMethods.webifyString(ssr.getAuthor()) %></td>
				<td><%=ssr.getContentLength() %></td>
				<td><%=UtilMethods.webifyString(ssr.getUrl()) %></td>
				<td><%=UtilMethods.webifyString(ssr.getUri()) %></td>
				
				<td><%=UtilMethods.webifyString(ssr.getMimeType()) %></td>
				<td><%=UtilMethods.webifyString(ssr.getFileName()) %></td>
			</tr>
		<%} %>
	<%} %>
</table>
<div style="text-align: center;padding:20px;"><%=UtilMethods.webifyString(results.getTook()) %></div>

		