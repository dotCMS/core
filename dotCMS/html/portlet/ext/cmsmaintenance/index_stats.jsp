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
ESIndexAPI idxApi = new ESIndexAPI();
ContentletAPI capi = APILocator.getContentletAPI();

try {
	user = com.liferay.portal.util.PortalUtil.getUser(request);

} catch (Exception e) {

}

boolean ADMIN_MODE = (session.getAttribute(com.dotmarketing.util.WebKeys.ADMIN_MODE_SESSION) != null);
if(!ADMIN_MODE || user == null){
	response.sendError(403);
	return;
}
List<String> currentIdx =idxApi.getCurrentIndex();
List<String> newIdx =idxApi.getNewIndex();

List<String> indices=idxApi.listDotCMSIndices();
Map<String, IndexStatus> indexInfo = idxApi.getIndicesAndStatus();

SimpleDateFormat dater = new SimpleDateFormat("yyyyMMddHHmmss");


Map<String,ClusterIndexHealth> map = new ESIndexAPI().getClusterHealth();


%>

<script language="Javascript">
	dojo.require("dijit.DropDownMenu");
	dojo.addOnLoad (function(){
		checkReindexation();
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











		<div class="buttonRow" style="text-align: right;padding:20px;">

			<div dojoType="dijit.form.DropDownButton">
				<span><%= LanguageUtil.get(pageContext,"Add-Index") %></span>
					<div dojoType="dijit.Menu">
					
					 	<div dojoType="dijit.MenuItem" onClick="doCreateWorking();">
		                    <span class="addIcon"></span><%= LanguageUtil.get(pageContext,"Create-Working-Index") %>
		                </div>
		                <div dojoType="dijit.MenuItem" onClick="doCreateLive();" >
		                    <span class="addIcon"></span>
		                    <%= LanguageUtil.get(pageContext,"Create-Live-Index") %>
		                </div>
		           </div>
			</div>
		    <button dojoType="dijit.form.Button"  onClick="refreshIndexStats()" iconClass="reloadIcon">
               <%= LanguageUtil.get(pageContext,"Refresh") %>
            </button>
		
		
		</div>


		<table class="listingTable">
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

				<%boolean active =currentIdx.contains(x);%>
				<%boolean building =newIdx.contains(x);%>
				<%	Date d = null;
					String myDate = null;
					try{
						 myDate = x.split("_")[1];
						d = dater.parse(myDate);

						myDate = UtilMethods.dateToPrettyHTMLDate(d)  + " "+ UtilMethods.dateToHTMLTime(d);
						}
						catch(Exception e){

					}%>


				<tr class="<%=(active) ? "trIdxActive" : (building) ? "trIdxBuilding" : "trIdxNothing" %>" id="<%=x%>Row">
					<td  align="center" class="showPointer" >
						<%if(active){ %>
							<%= LanguageUtil.get(pageContext,"active") %>
						<%}else if(building){ %>
							<%= LanguageUtil.get(pageContext,"Building") %>
						<%} %>
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
			<%boolean active =currentIdx.contains(x);%>
			<%boolean building =newIdx.contains(x);%>
			<%if(building)continue; %>
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
			 		<%= LanguageUtil.get(pageContext,"Activate-Index") %>
			 	</div>
			 	<%}else{ %>
			 	<div dojoType="dijit.MenuItem" onClick="doDeactivateIndex('<%=x %>');" class="showPointer">
			 		<span class="unpublishIcon"></span>
			 		<%= LanguageUtil.get(pageContext,"Deactivate-Index") %>
			 	</div>
			 	<%} %>
			 	<div dojoType="dijit.MenuItem" onClick="doClearIndex('<%=x %>');" class="showPointer">
			 		<span class="shrinkIcon"></span>
			 		<%= LanguageUtil.get(pageContext,"Clear-Index") %>
			 	</div>
			 	<%if(!active){%>
				 	<div dojoType="dijit.MenuItem" onclick="deleteIndex('<%=x%>', <%=(currentIdx.contains(x)) %>)" class="showPointer">
				 		<span class="deleteIcon"></span>
				 		<%= LanguageUtil.get(pageContext,"Delete-Index") %>
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


		<%--
		<%if(indices != null && indices.size() >0 ){ %>
				<div class="buttonRow" style="text-align: left">


				<%=LanguageUtil.get(pageContext, "Execute") %> :
				<select name="performAction" id="performAction" dojoType="dijit.form.FilteringSelect">
					<option value=""><%=LanguageUtil.get(pageContext, "Delete-Index") %></option>



				</select>

				<button dojoType="dijit.form.Button" onClick="excuteWorkflowAction()">
					<%=LanguageUtil.get(pageContext, "Perform-Workflow") %>
				</button>

				</div>
			<%} %>
		--%>