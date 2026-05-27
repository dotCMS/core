<%@page import="com.dotcms.content.elasticsearch.business.ContentletIndexAPI"%>
<%@page import="com.dotmarketing.util.Logger"%>
<%@page import="com.dotmarketing.exception.DotSecurityException"%>
<%@page import="com.dotcms.content.index.domain.ClusterIndexHealth"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.portlets.contentlet.business.ContentletAPI"%>
<%@page import="com.dotcms.content.index.IndexAPI"%>
<%@page import="com.dotmarketing.portlets.structure.factories.StructureFactory"%>
<%@page import="com.dotmarketing.portlets.structure.model.Structure"%>
<%@page import="java.util.Map"%>
<%@ include file="/html/common/init.jsp"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.LinkedHashMap"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="com.dotcms.cluster.ClusterUtils"%>
<%@ page import="com.dotcms.content.index.domain.IndexStats" %>
<%@ page import="com.dotcms.content.index.VersionedIndices" %>
<%@ page import="com.dotcms.content.index.VersionedIndicesAPI" %>
<%@ page import="com.dotcms.content.elasticsearch.business.IndiciesAPI" %>
<%@ page import="com.dotcms.content.elasticsearch.business.IndiciesInfo" %>
<%@ page import="com.dotcms.content.index.IndexConfigHelper.MigrationPhase" %>
<%@ page import="java.util.Optional" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.HashSet" %>
<%
// ── DEBUG #35820: ghost index investigation ──────────────────────────────────
// Branch: fix/issue-35820-debug-ghost-index
// Remove this block after root cause is confirmed.
// ─────────────────────────────────────────────────────────────────────────────

List<Structure> structs = StructureFactory.getStructures();
ContentletIndexAPI idxApi = APILocator.getContentletIndexAPI();
ContentletAPI capi = APILocator.getContentletAPI();
IndexAPI esapi = APILocator.getESIndexAPI();
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

List<String> currentIdx = idxApi.getCurrentIndex();
List<String> newIdx     = idxApi.getNewIndex();

List<String> indices       = idxApi.listDotCMSIndices();
List<String> closedIndices = idxApi.listDotCMSClosedIndices();
Map<String, IndexStats>        indexInfo = esapi.getIndicesStats();
Map<String, ClusterIndexHealth> map      = esapi.getClusterHealth();

SimpleDateFormat dater = new SimpleDateFormat("yyyyMMddHHmmss");

// ── DEBUG #35820: collect diagnostic data ────────────────────────────────────
MigrationPhase currentPhase = MigrationPhase.current();

// ES store (indicies table)
IndiciesAPI indiciesAPI = APILocator.getIndiciesAPI();
IndiciesInfo esInfo = null;
String dbEsWorking = "ERROR", dbEsLive = "ERROR", dbEsReindexWorking = "n/a", dbEsReindexLive = "n/a";
try {
    esInfo = indiciesAPI.loadIndicies();
    if (esInfo != null) {
        dbEsWorking       = esInfo.getWorking()       != null ? esInfo.getWorking()       : "null";
        dbEsLive          = esInfo.getLive()           != null ? esInfo.getLive()           : "null";
        dbEsReindexWorking= esInfo.getReindexWorking() != null ? esInfo.getReindexWorking() : "null";
        dbEsReindexLive   = esInfo.getReindexLive()    != null ? esInfo.getReindexLive()    : "null";
    }
} catch (Exception dbEx) {
    dbEsWorking = "EXCEPTION: " + dbEx.getMessage();
}

// OS store (versioned_indices table)
VersionedIndicesAPI versionedAPI = APILocator.getVersionedIndicesAPI();
String dbOsWorking = "n/a", dbOsLive = "n/a", dbOsReindexWorking = "n/a", dbOsReindexLive = "n/a";
String dbOsVersion = "n/a";
try {
    Optional<VersionedIndices> osVI = versionedAPI.loadDefaultVersionedIndices();
    if (osVI.isPresent()) {
        VersionedIndices vi = osVI.get();
        dbOsWorking       = vi.working()       .orElse("null");
        dbOsLive          = vi.live()           .orElse("null");
        dbOsReindexWorking= vi.reindexWorking() .orElse("null");
        dbOsReindexLive   = vi.reindexLive()    .orElse("null");
        dbOsVersion       = vi.version();
    } else {
        dbOsWorking = "(no OS versioned indices record)";
    }
} catch (Exception osEx) {
    dbOsWorking = "EXCEPTION: " + osEx.getMessage();
}

// Build per-index origin label: is each index name traceable to ES store, OS store, or neither?
// We strip cluster prefix for comparison (same logic as removeClusterIdFromName)
java.util.function.Function<String,String> stripCluster = n ->
    (n != null && n.contains(".")) ? n.substring(n.lastIndexOf('.') + 1) : (n == null ? "" : n);

Set<String> esStoreNames = new HashSet<>();
if (!"null".equals(dbEsWorking)        && !"ERROR".equals(dbEsWorking))        esStoreNames.add(stripCluster.apply(dbEsWorking));
if (!"null".equals(dbEsLive)           && !"ERROR".equals(dbEsLive))           esStoreNames.add(stripCluster.apply(dbEsLive));
if (!"null".equals(dbEsReindexWorking))                                        esStoreNames.add(stripCluster.apply(dbEsReindexWorking));
if (!"null".equals(dbEsReindexLive))                                           esStoreNames.add(stripCluster.apply(dbEsReindexLive));

Set<String> osStoreNames = new HashSet<>();
if (!"n/a".equals(dbOsWorking)        && !"null".equals(dbOsWorking))         osStoreNames.add(stripCluster.apply(dbOsWorking));
if (!"n/a".equals(dbOsLive)           && !"null".equals(dbOsLive))            osStoreNames.add(stripCluster.apply(dbOsLive));
if (!"null".equals(dbOsReindexWorking) && !"n/a".equals(dbOsReindexWorking))  osStoreNames.add(stripCluster.apply(dbOsReindexWorking));
if (!"null".equals(dbOsReindexLive)   && !"n/a".equals(dbOsReindexLive))     osStoreNames.add(stripCluster.apply(dbOsReindexLive));

// Per-index: determine provider origin
Map<String, String> indexOrigin = new LinkedHashMap<>();
for (String idx : indices) {
    String  bare      = stripCluster.apply(idx);
    boolean inEs      = esStoreNames.contains(bare);
    boolean inOs      = osStoreNames.contains(bare);
    boolean hasStats  = indexInfo.get(idx) != null;
    boolean hasHealth = map.get(idx) != null;
    boolean isClosed  = closedIndices.contains(idx);
    String origin;
    if (inEs && inOs)           origin = "ES-store + OS-store (same name, deduped)";
    else if (inEs)              origin = "ES-store (indicies table)";
    else if (inOs)              origin = "OS-store (versioned_indices table)";
    else if (hasStats)          origin = "Stale ES index (in cluster, not in active DB — orphan)";
    else if (hasHealth && isClosed) origin = "Stale CLOSED ES index (closed in cluster, not in DB)";
    else if (hasHealth)         origin = "Stale ES index (in cluster health, no stats — possibly closed)";
    else                        origin = "UNKNOWN — not found in any data source";
    indexOrigin.put(idx, origin);
}

// Console output
Logger.info(this.getClass(),
    "[DEBUG #35820] ===== INDEX PORTLET DIAGNOSTIC =====");
Logger.info(this.getClass(),
    "[DEBUG #35820] Migration phase: " + currentPhase);
Logger.info(this.getClass(),
    "[DEBUG #35820] listDotCMSIndices() returned " + indices.size() + " indices: " + indices);
Logger.info(this.getClass(),
    "[DEBUG #35820] closedIndices: " + closedIndices);
Logger.info(this.getClass(),
    "[DEBUG #35820] currentIdx (active): " + currentIdx);
Logger.info(this.getClass(),
    "[DEBUG #35820] newIdx (building):   " + newIdx);
Logger.info(this.getClass(),
    "[DEBUG #35820] indexInfo (ES stats) keys: " + indexInfo.keySet());
Logger.info(this.getClass(),
    "[DEBUG #35820] clusterHealth keys: " + map.keySet());
Logger.info(this.getClass(),
    "[DEBUG #35820] ES store — working=" + dbEsWorking
    + " live=" + dbEsLive
    + " reindexWorking=" + dbEsReindexWorking
    + " reindexLive=" + dbEsReindexLive);
Logger.info(this.getClass(),
    "[DEBUG #35820] OS store — version=" + dbOsVersion
    + " working=" + dbOsWorking
    + " live=" + dbOsLive
    + " reindexWorking=" + dbOsReindexWorking
    + " reindexLive=" + dbOsReindexLive);
for (Map.Entry<String, String> e : indexOrigin.entrySet()) {
    Logger.info(this.getClass(),
        "[DEBUG #35820] index='" + e.getKey() + "' origin=" + e.getValue()
        + " hasStats=" + (indexInfo.get(e.getKey()) != null)
        + " hasHealth=" + (map.get(e.getKey()) != null)
        + " active=" + currentIdx.contains(e.getKey())
        + " building=" + newIdx.contains(e.getKey()));
}
Logger.info(this.getClass(),
    "[DEBUG #35820] ===== END DIAGNOSTIC =====");
// ── END DEBUG #35820 ─────────────────────────────────────────────────────────
%>

<style>
	/* ── DEBUG #35820 panel ── */
	#debug35820 {
		font-family: monospace;
		font-size: 12px;
		background: #1e1e1e;
		color: #d4d4d4;
		border: 2px solid #f0ad4e;
		border-radius: 4px;
		padding: 16px;
		margin: 10px 0 20px 0;
		white-space: pre-wrap;
		word-break: break-all;
	}
	#debug35820 .dbg-title   { color: #f0ad4e; font-weight: bold; font-size: 14px; }
	#debug35820 .dbg-section { color: #9cdcfe; font-weight: bold; margin-top: 8px; }
	#debug35820 .dbg-ok      { color: #4ec9b0; }
	#debug35820 .dbg-warn    { color: #f0ad4e; }
	#debug35820 .dbg-error   { color: #f44747; font-weight: bold; }
	#debug35820 .dbg-es      { color: #569cd6; }
	#debug35820 .dbg-os      { color: #ce9178; }
	#debug35820 .dbg-unknown { color: #f44747; font-weight: bold; }
	#debug35820 .dbg-toggle  { cursor: pointer; color: #f0ad4e; text-decoration: underline; }
</style>

<div id="debug35820">
<span class="dbg-title">&#9888; DEBUG PANEL — Issue #35820: Ghost Index Investigation</span>
<span class="dbg-toggle" onclick="document.getElementById('debug35820body').style.display=document.getElementById('debug35820body').style.display==='none'?'block':'none'">[toggle]</span>
<div id="debug35820body">
<span class="dbg-section">&#9654; Migration Phase</span>
  phase = <span class="<%= (currentPhase.ordinal() >= 1) ? "dbg-warn" : "dbg-ok" %>"><%= currentPhase %> (ordinal=<%= currentPhase.ordinal() %>)</span>

<span class="dbg-section">&#9654; ES Store (indicies table)</span>
  working        = <span class="dbg-es"><%= dbEsWorking %></span>
  live           = <span class="dbg-es"><%= dbEsLive %></span>
  reindexWorking = <span class="dbg-es"><%= dbEsReindexWorking %></span>
  reindexLive    = <span class="dbg-es"><%= dbEsReindexLive %></span>

<span class="dbg-section">&#9654; OS Store (versioned_indices table, version=<%= dbOsVersion %>)</span>
  working        = <span class="dbg-os"><%= dbOsWorking %></span>
  live           = <span class="dbg-os"><%= dbOsLive %></span>
  reindexWorking = <span class="dbg-os"><%= dbOsReindexWorking %></span>
  reindexLive    = <span class="dbg-os"><%= dbOsReindexLive %></span>

<span class="dbg-section">&#9654; listDotCMSIndices() — <%= indices.size() %> open indices (ES + OS via PhaseRouter)</span>
<% for (String idx : indices) { %>
  <% String origin = indexOrigin.getOrDefault(idx, "UNKNOWN"); %>
  <% boolean isOsOrigin = origin.startsWith("OS-store"); %>
  <% boolean isUnknown  = origin.startsWith("UNKNOWN") || origin.contains("ORPHAN"); %>
  <span class="<%= isUnknown ? "dbg-unknown" : (isOsOrigin ? "dbg-os" : "dbg-es") %>">&#187; <%= idx %></span>
    origin  = <%= origin %>
    active  = <%= currentIdx.contains(idx) %> | building = <%= newIdx.contains(idx) %>
    hasStats (ES getIndicesStats) = <%= indexInfo.get(idx) != null %><% if (indexInfo.get(idx) != null) { %> [count=<%= indexInfo.get(idx).documentCount() %> size=<%= indexInfo.get(idx).size() %>]<% } %>
    hasHealth (clusterHealth)     = <%= map.get(idx) != null %><% if (map.get(idx) != null) { %> [replicas=<%= map.get(idx).numberOfReplicas() %> status=<%= map.get(idx).status() %>]<% } %>
<% } %>
<% if (indices.isEmpty()) { %><span class="dbg-error">  (empty — no indices returned!)</span><% } %>

<span class="dbg-section">&#9654; Closed indices — <%= closedIndices.size() %></span>
<% for (String idx : closedIndices) { %>  <%= idx %>
<% } %>
<% if (closedIndices.isEmpty()) { %>  (none)<% } %>

<span class="dbg-section">&#9654; getIndicesStats() keys (ES read provider, Phase 1 = ES only)</span>
<% for (String k : indexInfo.keySet()) { %>  <span class="dbg-es"><%= k %></span>
<% } %>
<% if (indexInfo.isEmpty()) { %>  (empty map)<% } %>

<span class="dbg-section">&#9654; getClusterHealth() keys (Phase 1 = ES+OS merge, OS stub may be empty)</span>
<% for (String k : map.keySet()) { %>  <span class="dbg-es"><%= k %></span>
<% } %>
<% if (map.isEmpty()) { %>  (empty map)<% } %>

<span class="dbg-section">&#9654; currentIdx (active slots — Phase 1 reads ES store)</span>
<% for (String idx : currentIdx) { %>  <span class="dbg-es"><%= idx %></span>
<% } %>
<% if (currentIdx.isEmpty()) { %>  (empty)<% } %>

<span class="dbg-section">&#9654; newIdx (reindex slots)</span>
<% for (String idx : newIdx) { %>  <span class="dbg-es"><%= idx %></span>
<% } %>
<% if (newIdx.isEmpty()) { %>  (none)<% } %>
</div>
</div>

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
					<th style="text-align: center"><%= LanguageUtil.get(pageContext,"Replicas") %></th>
					<th style="text-align: center"><%= LanguageUtil.get(pageContext,"Size") %></th>
					<th style="text-align: center"><%= LanguageUtil.get(pageContext,"Health") %></th>
					<%-- DEBUG #35820 column --%>
					<th style="text-align:center;background:#333;color:#f0ad4e;font-size:11px;">[DBG] Provider / Origin</th>
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
				<%-- DEBUG #35820: per-row origin --%>
				<% String rowOrigin = indexOrigin.getOrDefault(x, "UNKNOWN"); %>
				<% boolean rowIsOs = rowOrigin.startsWith("OS-store"); %>
				<% boolean rowIsUnknown = rowOrigin.startsWith("UNKNOWN") || rowOrigin.contains("ORPHAN"); %>

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
						<%=status !=null ? status.documentCount() : "n/a"%>
					</td>
					<td align="center"><%=(health !=null) ? health.numberOfReplicas(): "n/a"%></td>
					<td align="center"><%=status !=null ? status.size(): "n/a"%></td>
					<td align="center">
					          <div onclick="showIndexClusterStatus('<%=x%>')"  style='cursor:pointer;background:<%=(health !=null) ? health.status().toString(): "n/a"%>; width:20px;height:20px;'>
					          </div>
					</td>
					<%-- DEBUG #35820: origin cell --%>
					<td align="center" style="background:#1e1e1e;font-family:monospace;font-size:11px;max-width:260px;word-break:break-word;">
						<% if (rowIsUnknown) { %>
							<span style="color:#f44747;font-weight:bold;">&#9888; <%= rowOrigin %></span>
						<% } else if (rowIsOs) { %>
							<span style="color:#ce9178;">&#9654; OS-store</span><br/>
							<span style="color:#888;font-size:10px;">in DB: <%= osStoreNames.contains(x) ? "yes" : "NO" %></span>
						<% } else { %>
							<span style="color:#569cd6;">&#9654; ES-store</span><br/>
							<span style="color:#888;font-size:10px;">in DB: <%= esStoreNames.contains(x) ? "yes" : "NO" %></span>
						<% } %>
						<br/><span style="color:#888;font-size:10px;">stats=<%= status != null ? "yes" : "null" %> health=<%= health != null ? "yes" : "null" %></span>
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
