<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.dotmarketing.business.Layout"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.util.URLEncoder"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<html xmlns="http://www.w3.org/1999/xhtml">

<%
User user = WebAPILocator.getUserWebAPI().getLoggedInUser(request);
if(user == null){
	response.setStatus(403);
	return;
}

Layout layoutOb = (Layout) request.getAttribute(WebKeys.LAYOUT);
String layout = null;
if (layoutOb != null) {
	layout = layoutOb.getId();
}

%>
<script type="text/javascript">
	dojo.require("dijit.form.NumberTextBox");
    dojo.require("dojox.layout.ContentPane");
	
	function doQueueFilter () {
	
		var url="";
		if(dijit.byId("showPendings").checked){
			url="&showPendings=true";
		}
		if(dijit.byId("showErrors").checked){
			url+="&showErrors=true";
		}
		if(url==""){
			dijit.byId("showPendings").setValue(true);
			url="showPendings=true";
		}
		url="layout=<%=layout%>"+url;
		refreshQueueList(url);
	}
	var lastUrlParams ;
	
	function refreshQueueList(urlParams){
		lastUrlParams = urlParams;
		var url = "/html/portlet/ext/contentlet/publishing/view_publish_queue_list.jsp?"+ urlParams;		
		
		var myCp = dijit.byId("solrToolCp");	
		
		if (myCp) {
			myCp.destroyRecursive(true);
		}
		myCp = new dojox.layout.ContentPane({
			id : "solrToolCp"
		}).placeAt("queue_results");

		myCp.attr("href", url);

	}
	
	function doLuceneFilter () {
		
		var url="";
		url="&query="+encodeURIComponent(dijit.byId("query").value);
		url+="&sort="+dijit.byId("sort").value;
		url="layout=<%=layout%>"+url;
		refreshLuceneList(url);

	}
	
	var lastLuceneUrlParams ;
	
	function refreshLuceneList(urlParams){
		lastLuceneUrlParams = urlParams;
		var url = "/html/portlet/ext/contentlet/publishing/view_publish_content_list.jsp?"+ urlParams;		
		
		var myCp = dijit.byId("solrLuceneToolCp");
		
		
		if (myCp) {
			myCp.destroyRecursive(true);
		}
		myCp = new dojox.layout.ContentPane({
			id : "solrLuceneToolCp"
		}).placeAt("lucene_results");

		myCp.attr("href", url);

	}
	function loadSolrServers(){
		var url = "/html/portlet/ext/contentlet/publishing/view_publish_servers.jsp";		
		
		var myCp = dijit.byId("solrServersToolCp");	
		
		if (myCp) {
			myCp.destroyRecursive(true);
		}
		myCp = new dojox.layout.ContentPane({
			id : "solrServersToolCp"
		}).placeAt("solr_servers");

		myCp.attr("href", url);
	}
</script>
<div class="portlet-wrapper">
	<div>
		<img src="/html/portlet/ext/contentlet/publishing/images/solr.gif"> <%= LanguageUtil.get(pageContext, "publisher_Manager") %>
		<hr/>
	</div>
	<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">
  		<div id="search" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "publisher_Search") %>" >
  			<div>
				<dl>	
					<dt><strong><%= LanguageUtil.get(pageContext, "publisher_Lucene_Query") %> </strong></dt>
					<dd>
						<textarea dojoType="dijit.form.Textarea" name="query" style="width:500px;min-height: 150px;" id="query" type="text"></textarea>
					</dd>
					<dt><strong><%= LanguageUtil.get(pageContext, "publisher_Sort") %> </strong></dt><dd><input name="sort" id="sort" dojoType="dijit.form.TextBox" type="text" value="modDate" size="10" /></dd>
					<dt></dt><dd><button dojoType="dijit.form.Button" onclick="doLuceneFilter();" iconClass="searchIcon"><%= LanguageUtil.get(pageContext, "publisher_Search_Content") %></button></dd>
				</dl>
			</div>
			<hr>
			<div>&nbsp;</div>
			<div id="lucene_results">
			</div>
		</div>	
  		<div id="queue" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "publisher_Queue") %>" >
  		    <div>
				<button dojoType="dijit.form.Button" onClick="deleteQueue();" iconClass="deleteIcon">
					<%= LanguageUtil.get(pageContext, "publisher_Delete_from_queue") %> 
				</button>
				&nbsp;&nbsp;<%= LanguageUtil.get(pageContext, "publisher_Show") %> 
				<input dojoType="dijit.form.CheckBox" checked="checked" type="checkbox" name="showPendings" value="true" id="showPendings" onclick="doQueueFilter()" /> <label for="showPendings"><%=LanguageUtil.get(pageContext, "publisher_Queue_Pending")%></label> 
				<input dojoType="dijit.form.CheckBox" checked="checked" type="checkbox" name="showErrors" value="true" id="showErrors"  onclick="doQueueFilter()"  /> <label for="showErrors"><%=LanguageUtil.get(pageContext, "publisher_Queue_Error")%></label>
				<button class="solr_right" dojoType="dijit.form.Button" onClick="doQueueFilter();" iconClass="resetIcon">
					<%= LanguageUtil.get(pageContext, "publisher_Refresh") %> 
				</button> 
			</div>			
			<hr>
			<div>&nbsp;</div>
  			<div id="queue_results">
			</div>
			<script type="text/javascript">
			dojo.ready(function(){
				doQueueFilter();
			});
			</script>
  		</div>
  		<div id="instances" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "publisher_Servers") %>" >
  			<div>
				<%= LanguageUtil.get(pageContext, "publisher_Servers_Intro") %> 
			</div>			
			<hr>
			<div>&nbsp;</div>
  			<div id="solr_servers">
			</div>
			<script type="text/javascript">
			dojo.ready(function(){
				loadSolrServers();
			});
			</script>
  		</div>
	</div>
</div>
