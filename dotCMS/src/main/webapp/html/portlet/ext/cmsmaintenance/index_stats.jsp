<%@page import="com.dotcms.content.elasticsearch.business.ContentletIndexAPI"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotmarketing.exception.DotSecurityException"%>
<%@page import="org.elasticsearch.cluster.health.ClusterIndexHealth"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="com.dotcms.content.elasticsearch.business.ESIndexAPI"%>
<%@page import="com.dotmarketing.portlets.structure.factories.StructureFactory"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="java.util.Map"%>
<%@ include file="/html/common/init.jsp"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="com.dotcms.cluster.ClusterUtils"%>
<%@ page import="com.dotcms.content.elasticsearch.business.IndexStats" %>
<%

List<Structure> structs = StructureFactory.getStructures();
ContentletIndexAPI idxApi = APILocator.getContentletIndexAPI();
ContentletAPI capi = APILocator.getContentletAPI();
ESIndexAPI esapi = APILocator.getESIndexAPI();
try {
	user = com.liferay.portal.util.PortalUtil.getUser(request);
	if(user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("maintenance", user)){
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
List<String> closedIndices=idxApi.listDotCMSClosedIndices();
Map<String, IndexStats> indexInfo = esapi.getIndicesStats();

SimpleDateFormat dater = new SimpleDateFormat("yyyyMMddHHmmss");


Map<String,ClusterIndexHealth> map = esapi.getClusterHealth();


%>



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

	#restoreIndexUploader {
		width:200px !important;
	}

	#uploadProgress {
		float: right;
		display: none;
	}
</style>

		<div class="buttonRow" style="text-align: right;padding:20px;">
		    <button dojoType="dijit.form.Button"  onClick="refreshIndexStats()" iconClass="resetIcon">
               <%= LanguageUtil.get(pageContext,"Refresh") %>
            </button>
		</div>

		<table class="listingTable">
			<thead>
				<tr>
					<th style="text-align: center"><%= LanguageUtil.get(pageContext,"Status") %></th>
					<th><%= LanguageUtil.get(pageContext,"Index-Name") %></th>
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


				<tr class="showPointer <%=(active) ? "trIdxActive" : (building) ? "trIdxBuilding" : "trIdxNothing" %>" id="<%=x%>Row">
					<td  align="center" class="showPointer">
						<%if(active){ %>
							<%= LanguageUtil.get(pageContext,"active") %>
						<%}else if(building){ %>
							<%= LanguageUtil.get(pageContext,"Building") %>
						<%} %>
					</td>
					<td  class="showPointer" ><%=x %></td>
					<td><%=UtilMethods.webifyString(myDate) %></td>

					<td align="center">
						<%=status !=null ? status.getDocumentCount() : "n/a"%>
					</td>
					<td align="center"><%=(health !=null) ? health.getNumberOfShards() : "n/a"%></td>
					<td align="center"><%=(health !=null) ? health.getNumberOfReplicas(): "n/a"%></td>
					<td align="center"><%=status !=null ? status.getSize(): "n/a"%></td>
					<td align="center">
					          <div onclick="showIndexClusterStatus('<%=x%>')"  style='cursor:pointer;background:<%=(health !=null) ? health.getStatus().toString(): "n/a"%>; width:20px;height:20px;'>
					          </div>
					</td>
				</tr>
			<%} %>
			
			<% for(String idx : closedIndices) {%>
			    
			    <%   Date d = null;
                    String myDate = null;
                    try{
                         myDate = idx.split("_")[1];
                        d = dater.parse(myDate);

                        myDate = UtilMethods.dateToPrettyHTMLDate(d)  + " "+ UtilMethods.dateToHTMLTime(d);
                        }
                        catch(Exception e){}%>
			    
			    
			    <tr class="trIdxNothing" id="<%=idx%>Row">
			         <td  align="center" class="showPointer"> <%= LanguageUtil.get(pageContext,"Closed") %> </td>
			         <td  class="showPointer" ><%=idx%></td>
			         <td><%=UtilMethods.webifyString(myDate) %></td>
			         <td align="center">n/a</td>
					<td align="center">n/a</td>
					<td align="center">n/a</td>
					<td align="center">n/a</td>
					<td align="center">n/a</td>
			    </tr>
			<% } %>
			<tr>
				<td colspan="15" align="center" style="padding:20px;"><a href="#" onclick="refreshIndexStats()"><%= LanguageUtil.get(pageContext,"refresh") %></a></td>
			</tr>

		</table>





		<%--   RIGHT CLICK MENUS --%>

		<%for(String x : indices){%>
			<%boolean active =currentIdx.contains(x);%>
			<%boolean building =newIdx.contains(x);%>

			<%ClusterIndexHealth health = map.get(x); %>
			<div dojoType="dijit.Menu" contextMenuForWindow="false" style="display:none;"
			     targetNodeIds="<%=x%>Row" onOpen="dohighlight('<%=x%>Row')" onClose="undohighlight('<%=x%>Row')">

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
				 	<div dojoType="dijit.MenuItem" onClick="doCloseIndex('<%=x %>');" class="showPointer">
	                    <span class="deleteIcon"></span>
	                    <%= LanguageUtil.get(pageContext,"Close-Index") %>
	                </div>
			 	
				 	<div dojoType="dijit.MenuItem" onclick="deleteIndex('<%=x%>', <%=(currentIdx.contains(x)) %>)" class="showPointer">
				 		<span class="deleteIcon"></span>
				 		<%= LanguageUtil.get(pageContext,"Delete-Index") %>
				 	</div>
			 	<%} %>
			</div>
		<%} %>

        <% for(String idx : closedIndices) { %>
             <div dojoType="dijit.Menu" contextMenuForWindow="false" style="display:none;" 
                 targetNodeIds="<%=idx%>Row" onOpen="dohighlight('<%=idx%>Row')" onClose="undohighlight('<%=idx%>Row')">
                 
                 <div dojoType="dijit.MenuItem" onClick="doOpenIndex('<%=idx %>');" class="showPointer">
                     <span class="publishIcon"></span>
                     <%= LanguageUtil.get(pageContext,"Open-Index") %>
                 </div>
                 
                 <div dojoType="dijit.MenuItem" onclick="deleteIndex('<%=idx%>', false)" class="showPointer">
                     <span class="deleteIcon"></span>
                     <%= LanguageUtil.get(pageContext,"Delete-Index") %>
                 </div>
                 
             </div>
        <% } %>


		<div data-dojo-type="dijit.Dialog" style="width:345px;text-align: center;" id="restoreIndexDialog">
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
			   <br/>

			   <input type="checkbox" name="clearBeforeRestore"/>&nbsp;<%= LanguageUtil.get(pageContext,"Clear-Existing-Data") %>
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