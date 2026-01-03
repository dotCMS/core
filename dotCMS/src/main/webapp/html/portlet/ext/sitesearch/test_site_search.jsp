<%@page import="com.dotcms.content.elasticsearch.business.ESIndexAPI"%>
<%@page import="com.dotcms.enterprise.publishing.sitesearch.SiteSearchResults"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.exception.DotSecurityException"%>
<%@page import="com.dotmarketing.portlets.structure.factories.StructureFactory"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.sitesearch.business.SiteSearchAPI"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="org.elasticsearch.cluster.health.ClusterIndexHealth"%>
<%@ page import="com.dotcms.content.elasticsearch.business.IndexStats" %>
<%@ page import="com.dotcms.content.elasticsearch.business.IndicesInfo" %>
<%@ include file="/html/common/init.jsp"%>
<%

List<Structure> structs = StructureFactory.getStructures();
SiteSearchAPI ssapi = APILocator.getSiteSearchAPI();
ESIndexAPI esapi = APILocator.getESIndexAPI();
IndicesInfo info=APILocator.getIndiciesAPI().loadLegacyIndices();



String testIndex = (request.getParameter("testIndex") == null) ? info.getSiteSearch() : request.getParameter("testIndex");
String testQuery = request.getParameter("testQuery");


int testStart = 0;
int testLimit = 50;



SiteSearchResults results= new SiteSearchResults();
if(testQuery != null && testIndex != null){
	results = APILocator.getSiteSearchAPI().search(testIndex, testQuery, testStart, testLimit);
}


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



List<String> indices=ssapi.listIndices();
Map<String,String> alias=esapi.getIndexAlias(indices);
Map<String, IndexStats> indexInfo = esapi.getIndicesStats();

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
	<!-- START Toolbar -->
	<div class="portlet-toolbar">
		<div class="portlet-toolbar__actions-primary">
			<div class="inline-form">
				
				<select id="testIndex" name="testIndex" dojoType="dijit.form.FilteringSelect" style="width:250px;">
					<%for(String x : indices){ %>
						<option value="<%=x%>" <%=(x.equals(testIndex)) ? "selected='true'": ""%>><%=alias.get(x) == null ? x:alias.get(x)%> <%=(x.equals(APILocator.getIndiciesAPI().loadLegacyIndices().getSiteSearch())) ? "(" +LanguageUtil.get(pageContext, "Default") +") " : ""  %></option>
					<%} %>
				</select>
				
				<input type="text"  dojoType="dijit.form.TextBox" style="width:300px;" name="testQuery" value="<%=UtilMethods.webifyString(testQuery) %>" id="testQuery">
				
				<button dojoType="dijit.form.Button"
					id="testIndexButton" onClick="doTestSearch();"
					iconClass="saveIcon"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Search")) %>
				</button>
				
			</div>
		</div>
    	<div class="portlet-toolbar__actions-secondary"></div>
   </div>
   <!-- END Toolbar -->
</div>

<div dojoType="dojox.layout.ContentPane" id="siteSearchResults" ></div>