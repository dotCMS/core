<%@page import="com.dotmarketing.util.StringUtils"%>
<%@page import="java.net.URLDecoder"%>
<%@page import="com.dotcms.publishing.sitesearch.SiteSearchResult"%>
<%@page import="com.dotcms.publishing.sitesearch.SiteSearchResults"%>
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



String testIndex = (request.getParameter("testIndex") == null) ? info.site_search : request.getParameter("testIndex");
String testQuery = request.getParameter("testQuery");


int testStart = 0;
int testLimit = 50;



SiteSearchResults results= new SiteSearchResults();
if(testQuery != null && testIndex != null){
	results = APILocator.getSiteSearchAPI().search(testIndex, testQuery, testStart, testLimit);
}


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



List<String> indices=ssapi.listIndices();
Map<String,String> alias=esapi.getIndexAlias(indices);
Map<String, IndexStatus> indexInfo = esapi.getIndicesAndStatus();

SimpleDateFormat dater = APILocator.getContentletIndexAPI().timestampFormatter;


Map<String,ClusterIndexHealth> map = esapi.getClusterHealth();


%>

<script>
dojo.connect(dijit.byId("testQuery"), 'onkeypress', function (evt) {
    key = evt.keyCode;

    if(key == dojo.keys.ENTER) {
    	doTestSearch();
    }
});

</script>


<style>
	.trIdxBuilding{
	background:#F8ECE0;
	}
	.trIdxActive{
	background:#D8F6CE;
	}
	.trIdxNothing td{
	color:#aaaaaa;
	
	}
	.trIdxNothing:hover,.trIdxActive:hover,.trIdxBuilding:hover {background:#e0e9f6 !important;}
	 #restoreIndexUploader {
	   width:200px !important;
	 }
	 #uploadProgress {
	   float: right;
	   display: none;
	 }
</style>

	
	<div name="testSiteForm" dojoType="dijit.form.Form" id="testSiteForm" action="/html/portlet/ext/sitesearch/test_site_search_results.jsp" method="POST">
		<div class="buttonRow" style="padding:20px;">
			<select id="testIndex" name="testIndex" dojoType="dijit.form.FilteringSelect" style="width:250px;">
					<%for(String x : indices){ %>
						<option value="<%=x%>" <%=(x.equals(testIndex)) ? "selected='true'": ""%>><%=alias.get(x)%> <%=(x.equals(APILocator.getIndiciesAPI().loadIndicies().site_search)) ? "(" +LanguageUtil.get(pageContext, "Default") +") " : ""  %></option>
					<%} %>
			</select>
		
			
			<input type="text"  dojoType="dijit.form.TextBox" style="width:300px;" name="testQuery" value="<%=UtilMethods.webifyString(testQuery) %>" id="testQuery">
			<button dojoType="dijit.form.Button"
				id="testIndexButton" onClick="doTestSearch();"
				iconClass="saveIcon"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Search")) %>
			</button>
		</div>
	</div>
	<div dojoType="dojox.layout.ContentPane" id="siteSearchResults" >
	
	
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
			<tr>
				<td colspan="100" align="center">
					<div style="padding:20px;">
						<%= LanguageUtil.get(pageContext,"No-Results-Found") %>
					</div>
				</td>
			</tr>
			</table>
	
	
	</div>