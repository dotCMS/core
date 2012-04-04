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
ESIndexAPI idxApi = new ESIndexAPI();
ContentletAPI capi = APILocator.getContentletAPI();

try {
	user = com.liferay.portal.util.PortalUtil.getUser(request);

	if(user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("EXT_CMS_MAINTENANCE", user)){
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



List<String> currentIdx =idxApi.getCurrentIndex();
List<String> newIdx =idxApi.getNewIndex();

List<String> indices=idxApi.listDotCMSIndices();
Map<String, IndexStatus> indexInfo = idxApi.getIndicesAndStatus();

SimpleDateFormat dater = new SimpleDateFormat("yyyyMMddHHmmss");






%>

<script language="Javascript">
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

</style>




	<div style="height:20px">&nbsp;</div>
		
		<div class="indexActionsDiv" <%=(idxApi.isInFullReindex()) ? "style='display:none'" : "" %>>
			<table class="listingTable">
				<tr>
					<th colspan="2"><%= LanguageUtil.get(pageContext,"Content-Index-Tasks") %></th>
				</tr>
				<tr>
					<td colspan="2" align="center">
						<div id="currentIndexDirDiv"></div>
					</td>
				</tr>
				<tr>
					<td>
						<div id="lastIndexationDiv"></div>
			
							<%= LanguageUtil.get(pageContext,"Reindex") %>:
							<select id="structure" dojoType="dijit.form.ComboBox" style="width:250px;" autocomplete="true" name="structure" onchange="indexStructureChanged();">
								<option><%= LanguageUtil.get(pageContext,"Rebuild-Whole-Index") %></option>
								<%

									for(Structure structure : structs){%>
									<option><%=structure.getVelocityVarName()%></option>
								<%}%>
							</select>
			
					</td>
					<td style="text-align:center;white-space:nowrap;" width="350">
			            <button dojoType="dijit.form.Button" id="idxReindexButton" iconClass="reindexIcon" onClick="doFullReindex()">
			                <%= LanguageUtil.get(pageContext,"Reindex-Structure(s)") %>
			            </button>
			            <button dojoType="dijit.form.Button"  iconClass="reindexIcon" onClick="cleanReindexStructure();return false;" id="cleanReindexButton">
			                <%= LanguageUtil.get(pageContext,"Delete-Reindex-Structure") %>
			            </button>
					</td>
				</tr>
				<tr>
					<td>
						<%= LanguageUtil.get(pageContext,"Optimize-Index") %> (<%= LanguageUtil.get(pageContext,"Optimize-Index-Info") %> )
					</td>
					<td align="center">
			        	<button dojoType="dijit.form.Button"  iconClass="shrinkIcon" onClick="CMSMaintenanceAjax.optimizeIndices(optimizeCallback)">
			            	<%= LanguageUtil.get(pageContext,"Optimize-Index") %>
						</button>
			    	 </td>
				</tr>
			</table>
		</div>
		
		<!-- START Re-Index Progress Display -->		
		<div id="reindexationInProgressDiv"  <%=(idxApi.isInFullReindex()) ? "" : "style='display:none'" %>>
			<table class="listingTable">
				<tr>
					<th colspan="2"><%= LanguageUtil.get(pageContext,"Content-Index-Tasks") %></th>
				</tr>
				<tr>
					<td colspan="2" align="center">
						<div>
							<%= LanguageUtil.get(pageContext,"A-reindexation-process-is-in-progress") %>
						</div>
						<div id="indexationStartTimeDiv"></div>
						<div id="newIndexDirPathDiv"></div>
						<div style="width:200px" maximum="200" id="reindexProgressBar" progress="0" dojoType="dijit.ProgressBar"></div>
						<div id="indexationProgressDiv"></div>
					</div>
					</td>
				</tr>
				<tr>
					<td colspan="2" align="center">
						<button dojoType="dijit.form.Button"  iconClass="reindexIcon" onClick="stopReIndexing();">
			                <%= LanguageUtil.get(pageContext,"Stop-Reindexation") %>
			            </button>					
					</td>
				</tr>				
			</table>
		</div>
		
		
		
		
		
		<div style="height:20px">&nbsp;</div>
		

		<table class="listingTable">
			<thead>
				<tr>
					<th>Status</th>
					<th>Index Name</th>
					<th>Created</th>
					<th>Count</th>
					<th>Shards</th>
					<th>Size</th>

				</tr>
			</thead>
			<%for(String x : indices){%>
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
					<td  class="showPointer" >
						<%if(active){ %>
							<%= LanguageUtil.get(pageContext,"active") %>
						<%}else if(building){ %>
							<%= LanguageUtil.get(pageContext,"Building") %>
						<%} %>
					</td>
					<td  class="showPointer" ><%=x %></td>
					<td><%=UtilMethods.webifyString(myDate) %></td>
				
					<td>
					
						<%=status.getDocs().getNumDocs()%>
					</td>
					<td><%=status.getShards().size() %></td>
					<td><%=status.getStoreSize()%></td>

				</tr>
			<%} %>
			<tr>
				<td colspan="15" align="center" style="padding:20px;"><a href="#" onclick="refreshIndexStats()"><%= LanguageUtil.get(pageContext,"refresh") %></a></td>
			</tr>
			
		</table>
		
		
		
		
		
		<%---   RIGHT CLICK MENUS --%>
		
		<%for(String x : indices){%>	
			<div dojoType="dijit.Menu" contextMenuForWindow="false" style="display:none;" targetNodeIds="<%=x%>Row">
			 	<div dojoType="dijit.MenuItem" onClick="doCreateWorking();" class="showPointer">
                    <span class="addIcon"></span>
                    <%= LanguageUtil.get(pageContext,"Create-Working-Index") %>
                </div>
                <div dojoType="dijit.MenuItem" onClick="doCreateLive();" class="showPointer">
                    <span class="addIcon"></span>
                    <%= LanguageUtil.get(pageContext,"Create-Live-Index") %>
                </div>
			 	<div dojoType="dijit.MenuItem" onClick="showRestoreIndexDialog('<%=x %>');" class="showPointer">
			 		<span class="uploadIcon"></span>
			 		<%= LanguageUtil.get(pageContext,"Restore-Index") %>
			 	</div>
			 	<div dojoType="dijit.MenuItem" onClick="doDownloadIndex('<%=x %>');" class="showPointer">
			 		<span class="downloadIcon"></span>
			 		<%= LanguageUtil.get(pageContext,"Download-Index") %>
			 	</div>
			 	<div dojoType="dijit.MenuItem" onClick="doActivateIndex('<%=x %>');" class="showPointer">
			 		<span class="publishIcon"></span>
			 		<%= LanguageUtil.get(pageContext,"Activate-Index") %>
			 	</div>
			 	<div dojoType="dijit.MenuItem" onClick="doDeactivateIndex('<%=x %>');" class="showPointer">
			 		<span class="unpublishIcon"></span>
			 		<%= LanguageUtil.get(pageContext,"Deactivate-Index") %>
			 	</div>
			 	<div dojoType="dijit.MenuItem" onClick="doClearIndex('<%=x %>');" class="showPointer">
			 		<span class="shrinkIcon"></span>
			 		<%= LanguageUtil.get(pageContext,"Clear-Index") %>
			 	</div>
			 	<div dojoType="dijit.MenuItem" onclick="deleteIndex('<%=x%>', <%=(currentIdx.contains(x)) %>)" class="showPointer">
			 		<span class="deleteIcon"></span>
			 		<%= LanguageUtil.get(pageContext,"Delete-Index") %>
			 	</div>
			</div>
		<%} %>
		
		
		<style type="text/css">
		  #restoreIndexUploader {
		    width:200px !important;
		  }
		  #uploadProgress {
		    float: right;
		    display: none;
		  }
        </style>
		
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