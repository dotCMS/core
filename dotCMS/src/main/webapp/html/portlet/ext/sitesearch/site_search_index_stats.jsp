<%@page import="com.dotcms.cluster.ClusterUtils"%>
<%@page import="com.dotcms.content.elasticsearch.business.ESIndexAPI"%>
<%@page import="com.dotcms.content.elasticsearch.business.IndicesInfo"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.exception.DotSecurityException"%>
<%@page import="com.dotmarketing.portlets.structure.factories.StructureFactory"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="com.dotmarketing.sitesearch.business.SiteSearchAPI"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="org.elasticsearch.cluster.health.ClusterIndexHealth"%>
<%@ page import="com.dotcms.content.elasticsearch.business.IndexStats" %>
<%@ include file="/html/common/init.jsp"%>

<%

SiteSearchAPI ssapi = APILocator.getSiteSearchAPI();
ESIndexAPI esapi = APILocator.getESIndexAPI();
IndicesInfo info=APILocator.getIndiciesAPI().loadLegacyIndices();

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
List<String> closedIndices=ssapi.listClosedIndices();

Map<String, IndexStats> indexInfo = esapi.getIndicesStats();
Map<String, String> alias = esapi.getIndexAlias(indexInfo.keySet().toArray(new String[indexInfo.size()]));
SimpleDateFormat dater = APILocator.getContentletIndexAPI().timestampFormatter;


Map<String,ClusterIndexHealth> map = esapi.getClusterHealth();


%>



<div data-dojo-type="dijit.Dialog" style="width:400px;" id="createIndexDialog">
	<div class="dotForm">
		<label form="createIndexAlias" class="required" for="createIndexAlias">Alias</label>
		<input id="createIndexAlias" dojoType="dijit.form.TextBox" class="dotFormInput"/><br/><br/>
		<label id="createIndexNumShards" class="required" for="createIndexNumShards">Shards:</label>
		<input id="createIndexNumShards" dojoType="dijit.form.TextBox" class="dotFormInput"/><br/><br/>
		<div class="buttonRow-right">
			<button dojoType="dijit.form.Button" class="dijitButtonFlat" onClick="dijit.byId('createIndexDialog').hide()">
				<%= LanguageUtil.get(pageContext,"Cancel") %>
			</button>
			<button dojoType="dijit.form.Button"  iconClass="addIcon"
				onClick="doCreateSiteSearch(dijit.byId('createIndexAlias').attr('value'),dijit.byId('createIndexNumShards').attr('value'))">
				<%= LanguageUtil.get(pageContext,"Create-SiteSearch-Index") %>
			</button>
		</div>
	</div>
</div>


<!-- START Toolbar -->
	<div class="portlet-toolbar">
		<div class="portlet-toolbar__actions-primary">
			<button dojoType="dijit.form.Button"  onClick="showNewIndexDialog()" iconClass="addIcon">
               <%= LanguageUtil.get(pageContext,"Create-SiteSearch-Index") %>
            </button>
		</div>
		<div class="portlet-toolbar__info">
			
		</div>
    	<div class="portlet-toolbar__actions-secondary">
    		<button dojoType="dijit.form.Button"  onClick="refreshIndexStats()">
               <%= LanguageUtil.get(pageContext,"Refresh") %>
            </button>
    	</div>
   </div>
<!-- END Toolbar -->

<table class="listingTable">
	<thead>
		<tr>
			<th style="text-align: center"><%= LanguageUtil.get(pageContext,"Status") %></th>
			<th><%= LanguageUtil.get(pageContext,"Index-Name") %></th>
			<th><%= LanguageUtil.get(pageContext,"Alias") %></th>					
			<th><%= LanguageUtil.get(pageContext,"Created") %></th>
			<th style="text-align: center"><%= LanguageUtil.get(pageContext,"Count") %></th>
			<th style="text-align: center"><%= LanguageUtil.get(pageContext,"Shards") %></th>
			<th style="text-align: center"><%= LanguageUtil.get(pageContext,"Replicas") %></th>
			<th style="text-align: center"><%= LanguageUtil.get(pageContext,"Size") %></th>
			<th style="text-align: center"><%= LanguageUtil.get(pageContext,"Health") %></th>					
		</tr>
	</thead>
	<%for(String x : indices){%>
		<%ClusterIndexHealth health = map.get(x); %>
		<%IndexStats status = indexInfo.get(x); %>

		<%boolean active =x.equals(info.getSiteSearch());%>
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
			<td><%= alias.get(x) == null ? "": alias.get(x)%></td>
			<td><%=UtilMethods.webifyString(myDate) %></td>

			<td align="center">
				<%=status !=null ? status.getDocumentCount() : "n/a"%>
			</td>
			<td align="center"><%=(health !=null) ? health.getNumberOfShards() : "n/a"%></td>
			<td align="center"><%=(health !=null) ? health.getNumberOfReplicas(): "n/a"%></td>
			<td align="center"><%=status !=null ? status.getSize(): "n/a"%></td>
			<td align="center"><div  style='background:<%=(health !=null) ? health.getStatus().toString(): "n/a"%>; width:20px;height:20px;'></div></td>
		</tr>
	<%} %>
	
	<% for(String x : closedIndices) { %>
	    <%   Date d = null;
            String myDate = null;
            try {
                 myDate = x.split("_")[1];
                 d = dater.parse(myDate);
                 myDate = UtilMethods.dateToPrettyHTMLDate(d)  + " "+ UtilMethods.dateToHTMLTime(d);
            }
            catch(Exception e){}%>
	    <tr class="trIdxNothing pointer" id="<%=x%>Row">
            <td  align="center" class="showPointer" >
               <%= LanguageUtil.get(pageContext,"Closed") %>
            </td>
            <td  class="showPointer" ><%=x %></td>
            <td><%= alias.get(x) == null ? "": alias.get(x)%></td>
            <td><%=UtilMethods.webifyString(myDate) %></td>

            <td colspan="5" align="center">
                n/a
            </td>
        </tr>
	<% } %>
	<tr>
		<td colspan="15" align="center" style="padding:20px;"><a href="#" onclick="refreshIndexStats()"><%= LanguageUtil.get(pageContext,"refresh") %></a></td>
	</tr>

</table>





<%--   RIGHT CLICK MENUS --%>

		<%for(String x : indices){%>
			<%boolean active =x.equals(info.getSiteSearch());%>

			<%ClusterIndexHealth health = map.get(x); %>
			<div dojoType="dijit.Menu" contextMenuForWindow="false" style="display:none;" 
			     targetNodeIds="<%=x%>Row" onOpen="dohighlight('<%=x%>Row')" onClose="undohighlight('<%=x%>Row')">

			 	<%if(!active){%>
				 	<div dojoType="dijit.MenuItem" onClick="doActivateIndex('<%=x %>');" class="showPointer">
				 		<span class="publishIcon"></span>
				 		<%= LanguageUtil.get(pageContext,"Make-Default") %>
				 	</div>
				 	<div dojoType="dijit.MenuItem" onclick="doCloseIndex('<%=x%>', false)" class="showPointer">
	                     <span class="deleteIcon"></span>
	                     <%= LanguageUtil.get(pageContext,"Close-Index") %>
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

        <% for(String x : closedIndices) { %>
            <div dojoType="dijit.Menu" contextMenuForWindow="false" style="display:none;" 
                 targetNodeIds="<%=x%>Row" onOpen="dohighlight('<%=x%>Row')" onClose="undohighlight('<%=x%>Row')">
                 <div dojoType="dijit.MenuItem" onclick="doOpenIndex('<%=x%>', false)" class="showPointer">
                     <span class="publishIcon"></span>
                     <%= LanguageUtil.get(pageContext,"Open-Index") %>
                 </div>
                 
                 <div dojoType="dijit.MenuItem" onclick="deleteIndex('<%=x%>', false)" class="showPointer">
                       <span class="deleteIcon"></span>
                       <%= LanguageUtil.get(pageContext,"Delete-Index") %>
                 </div>
            </div>
        <% } %>

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
			   <br/>

			   <input type="checkbox" name="clearBeforeRestore"/><%= LanguageUtil.get(pageContext,"Clear-Existing-Data") %>
		   </form>
		   <br/>

           <button id="uploadSubmit" data-dojo-type="dijit.form.Button" type="button">
              <span class="uploadIcon"></span>
              <%= LanguageUtil.get(pageContext,"Upload-File") %>
              <script type="dojo/method" data-dojo-event="onClick" data-dojo-args="evt">doRestoreIndex();</script>
           </button>

		   <button data-dojo-type="dijit.form.Button" type="button">
		      <span class="deleteIcon"></span>
		      <%= LanguageUtil.get(pageContext,"Close") %>
              <script type="dojo/method" data-dojo-event="onClick" data-dojo-args="evt">hideRestoreIndex();</script>
           </button>
		</div>