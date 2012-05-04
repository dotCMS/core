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
Map<String, IndexStatus> indexInfo = esapi.getIndicesAndStatus();

SimpleDateFormat dater = APILocator.getContentletIndexAPI().timestampFormatter;


Map<String,ClusterIndexHealth> map = esapi.getClusterHealth();


%>

<script language="Javascript">
	dojo.require("dijit.DropDownMenu");
	dojo.addOnLoad (function(){
		checkReindexation();
	});
</script>












		<div class="buttonRow" style="text-align: right;padding:20px;">

		    <button dojoType="dijit.form.Button"  onClick="doCreateSiteSearch()" iconClass="addIcon">
               <%= LanguageUtil.get(pageContext,"Create-SiteSearch-Index") %>
            </button>

		    <button dojoType="dijit.form.Button"  onClick="refreshIndexStats()" iconClass="reloadIcon">
               <%= LanguageUtil.get(pageContext,"Refresh") %>
            </button>
		
		
		</div>


		<table class="listingTable" style="width:98%">
			<thead>
				<tr>
					<th style="text-align: center">Status</th>
					<th>Index Name</th>
					<th>Created</th>
					<th style="text-align: center">Count</th>
					<th style="text-align: center">Shards</th>
					<th style="text-align: center">Replicas</th>
					<th style="text-align: center">Size</th>
					<th style="text-align: center">Health</th>
				</tr>
			</thead>
			<%for(String x : indices){%>
				<%ClusterIndexHealth health = map.get(x); %>
				<%IndexStatus status = indexInfo.get(x); %>

				<%boolean active =x.equals(info.site_search);%>
				<%	Date d = null;
					String myDate = null;
					try{
						 myDate = x.split("_")[1];
						d = dater.parse(myDate);

						myDate = UtilMethods.dateToPrettyHTMLDate(d)  + " "+ UtilMethods.dateToHTMLTime(d);
						}
						catch(Exception e){

					}%>


				<tr class="<%=(active) ? "trIdxActive"  : "trIdxNothing" %> pointer" id="<%=x%>Row" onclick="showSiteSearchPane('<%=x%>')">
					<td  align="center" class="showPointer" >
						<%if(active){ %>
							<%= LanguageUtil.get(pageContext,"default") %>
						<%}%>
					</td>
					<td  class="showPointer" ><%=x %></td>
					<td><%=UtilMethods.webifyString(myDate) %></td>

					<td align="center">
						<%=(status !=null && status.getDocs() != null) ? status.getDocs().numDocs(): "n/a"%>
					</td>
					<td align="center"><%=(status !=null) ? status.getShards().size() : "n/a"%></td>
					<td align="center"><%=(health !=null) ? health.getNumberOfReplicas(): "n/a"%></td>
					<td align="center"><%=(status !=null) ? status.getStoreSize(): "n/a"%></td>
					<td align="center"><div  style='background:<%=(health !=null) ? health.getStatus().toString(): "n/a"%>; width:20px;height:20px;'></div></td>
				</tr>
			<%} %>
			<tr>
				<td colspan="15" align="center" style="padding:20px;"><a href="#" onclick="refreshIndexStats()"><%= LanguageUtil.get(pageContext,"refresh") %></a></td>
			</tr>

		</table>





		<%---   RIGHT CLICK MENUS --%>

		<%for(String x : indices){%>
			<%boolean active =x.equals(info.site_search);%>

			<%ClusterIndexHealth health = map.get(x); %>
			<div dojoType="dijit.Menu" contextMenuForWindow="false" style="display:none;" 
			     targetNodeIds="<%=x%>Row" onOpen="dohighlight('<%=x%>Row')" onClose="undohighlight('<%=x%>Row')">
                 
                <div dojoType="dijit.MenuItem" onClick="updateReplicas('<%=x %>',<%=health.getNumberOfReplicas()%>);" class="showPointer">
                    <span class="fixIcon"></span>
                    <%= LanguageUtil.get(pageContext,"Update-Replicas-Index") %>
                </div>
                
                
			 	<div dojoType="dijit.MenuItem" onClick="showRestoreIndexDialog('<%=x %>');" class="showPointer">
			 		<span class="uploadIcon"></span>
			 		<%= LanguageUtil.get(pageContext,"Restore-Index") %>
			 	</div>
			 	<div dojoType="dijit.MenuItem" onClick="doDownloadIndex('<%=x %>');" class="showPointer">
			 		<span class="downloadIcon"></span>
			 		<%= LanguageUtil.get(pageContext,"Download-Index") %>
			 	</div>
			 	<%if(!active){%>
				 	<div dojoType="dijit.MenuItem" onClick="doActivateIndex('<%=x %>');" class="showPointer">
				 		<span class="publishIcon"></span>
				 		<%= LanguageUtil.get(pageContext,"Make-Default") %>
				 	</div>
				 	<div dojoType="dijit.MenuItem" onclick="deleteIndex('<%=x%>', false)" class="showPointer">
				 		<span class="deleteIcon"></span>
				 		<%= LanguageUtil.get(pageContext,"Delete-Index") %>
				 	</div>
			 	<%}else{ %>
				 	<div dojoType="dijit.MenuItem" onClick="doDeactivateIndex('<%=x %>');" class="showPointer">
				 		<span class="unpublishIcon"></span>
				 		<%= LanguageUtil.get(pageContext,"Remove-Default") %>
				 	</div>
			 	<%} %>

			</div>
		<%} %>




		<div data-dojo-type="dijit.Dialog" style="width:400px;" id="restoreIndexDialog">
		    <img id="uploadProgress" src="/html/images/icons/round-progress-bar.gif"/>
		    <span id="uploadFileName"></span>
			<form method="post"
			      action="/DotAjaxDirector/com.dotmarketing.portlets.cmsmaintenance.ajax.IndexAjaxAction/cmd/restoreIndex"
			      id="restoreIndexForm"
			      enctype="multipart/form-data">

			   <input type="hidden" id="indexToRestore" name="indexToRestore" value=""/>

			   <input name="uploadedfile" multiple="false"
			          type="file" data-dojo-type="dojox.form.Uploader"
			          label="Select File" id="restoreIndexUploader"
			          showProgress="true"
			          onComplete="restoreUploadCompleted()"/>

			   <span id="uploadWarningWorking">
			      <span class="exclamation"></span>
			      <%= LanguageUtil.get(pageContext,"File-Doesnt-Look-As-A-Working-Index-Data") %>
			   </span>

			   <span id="uploadWarningLive">
			      <span class="exclamation"></span>
			      <%= LanguageUtil.get(pageContext,"File-Doesnt-Look-As-A-Live-Index-Data") %>
			   </span>

			   <br/>

			   <input type="checkbox" name="clearBeforeRestore"/><%= LanguageUtil.get(pageContext,"Clear-Existing-Data") %>
		   </form>
		   <br/>



		   <button data-dojo-type="dijit.form.Button" type="button">
		      <span class="deleteIcon"></span>
		      <%= LanguageUtil.get(pageContext,"Close") %>
              <script type="dojo/method" data-dojo-event="onClick" data-dojo-args="evt">hideRestoreIndex();</script>
           </button>
		</div>
