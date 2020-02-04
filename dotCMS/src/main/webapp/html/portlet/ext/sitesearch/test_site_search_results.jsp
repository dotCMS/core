<%@page import="com.dotcms.enterprise.publishing.sitesearch.SiteSearchResult"%>
<%@page import="com.dotcms.enterprise.publishing.sitesearch.SiteSearchResults"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.exception.DotSecurityException"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@ include file="/html/common/init.jsp"%>
<%

String testIndex = request.getParameter("testIndex");
String testQuery = (request.getParameter("testQuery") != null) 
		? request.getParameter("testQuery")
		: "*";


int testStart = 0;
int testLimit = 50;





SiteSearchResults results= APILocator.getSiteSearchAPI().search(testIndex, testQuery,  testStart, testLimit);

String myError = (results.getError()!= null && results.getError().indexOf("nested:") > -1) 
		? results.getError().substring(0, results.getError().indexOf("nested:"))  
				: results.getError();

try {
	user = com.liferay.portal.util.PortalUtil.getUser(request);
	if(user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("site-search", user)){
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
	

<table class="listingTable" style="margin-bottom: 30px;">
	<%if(results == null || results.getResults() ==null || results.getResults().size() >0){ %>
		<tr>
			<td><b><%= LanguageUtil.get(pageContext,"Results") %></b>:</td>
			<td><%= results.getResults().size()%> <%= LanguageUtil.get(pageContext,"of") %> <%=results.getTotalResults() %> <%= LanguageUtil.get(pageContext,"total") %></td>
		</tr>
	<%} %>
		
	<%if(UtilMethods.isSet(results.getQuery())){ %>
		<tr>
			<td><b><%= LanguageUtil.get(pageContext,"Query") %></b>:</td>
			<td><%=results.getQuery() %></td>
		</tr>
	<%} %>
</table>


<table class="listingTable">
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

		