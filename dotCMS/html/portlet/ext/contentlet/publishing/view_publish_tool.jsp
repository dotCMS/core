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
		url="layout=<%=layout%>";
		refreshQueueList(url);
	}
	
	function doAuditFilter() {
		var url="";
		url="layout=<%=layout%>";
		refreshAuditList(url);
	}
	
	
	var lastUrlParams ;
	
	function refreshQueueList(urlParams){
		lastUrlParams = urlParams;
		var url = "/html/portlet/ext/contentlet/publishing/view_publish_queue_list.jsp?"+ urlParams;		
		
		var myCp = dijit.byId("queueContent");	
		
		if (myCp) {
			myCp.destroyRecursive(false);
		}
		myCp = new dojox.layout.ContentPane({
			id : "queueContent"
		}).placeAt("queue_results");

		myCp.attr("href", url);
		
		myCp.refresh();

	}
	
	function refreshAuditList(urlParams){
		lastUrlParams = urlParams;
		var url = "/html/portlet/ext/contentlet/publishing/view_publish_audit_list.jsp?"+ urlParams;		
		
		var myCp = dijit.byId("auditContent");	
		
		if (myCp) {
			myCp.destroyRecursive(false);
		}
		myCp = new dojox.layout.ContentPane({
			id : "auditContent"
		}).placeAt("audit_results");

		myCp.attr("href", url);
		
		myCp.refresh();

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
		
		var myCp = dijit.byId("searchLuceneContent");
		
		
		if (myCp) {
			myCp.destroyRecursive(false);
		}
		myCp = new dojox.layout.ContentPane({
			id : "searchLuceneContent"
		}).placeAt("lucene_results");

		myCp.attr("href", url);
		
		myCp.refresh();

	}
	function loadPublishQueueEndpoints(){
		var url = "/html/portlet/ext/contentlet/publishing/view_publish_endpoint_list.jsp";		
		
		var myCp = dijit.byId("endpointsContent");	
		
		if (myCp) {
			myCp.destroyRecursive(true);
		}
		myCp = new dojox.layout.ContentPane({
			id : "endpointsContent"
		}).placeAt("endpoint_servers");

		myCp.attr("href", url);
	}
	
	function goToAddEndpoint(){
		var dialog = new dijit.Dialog({
			id: 'addEndpoint',
	        title: "<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Add")%>",
	        style: "width: 800px; ",
	        content: new dojox.layout.ContentPane({
	            href: "/html/portlet/ext/contentlet/publishing/add_publish_endpoint.jsp"
	        }),
	        onHide: function() {
	        	var dialog=this;
	        	setTimeout(function() {
	        		dialog.destroyRecursive();
	        	},200);
	        },
	        onLoad: function() {
	        	
	        }
	    });
	    dialog.show();	    
	    dojo.style(dialog.domNode,'top','80px');
	}

	function goToEditEndpoint(identifier){
		var dialog = new dijit.Dialog({
			id: 'editEndpoint',
	        title: "<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Edit")%>",
	        style: "width: 800px; ",
	        content: new dojox.layout.ContentPane({
	            href: "/html/portlet/ext/contentlet/publishing/edit_publish_endpoint.jsp?op=edit&id="+identifier
	        }),
	        onHide: function() {
	        	var dialog=this;
	        	setTimeout(function() {
	        		dialog.destroyRecursive();
	        	},200);
	        },
	        onLoad: function() {
	        }
	    });
	    dialog.show();	    
	    dojo.style(dialog.domNode,'top','80px');
	}

	function backToEndpointsList(add){
		if(add)
			dijit.byId("addEndpoint").hide();			
		else
			dijit.byId("editEndpoint").hide();
		
		var url = "/html/portlet/ext/contentlet/publishing/view_publish_endpoint_list.jsp";		
		
		var myCp = dijit.byId("endpointsContent");	
		if (myCp) {
			myCp.destroyRecursive(false);
		}
		myCp = new dojox.layout.ContentPane({
			id : "endpointsContent"
		}).placeAt("endpoint_servers");

		myCp.attr("href", url);
		myCp.refresh();	
	}

	function deleteEndpoint(identifier){
		if(confirm("Are you sure you want to delete this endpoint?")){
			var url = "/html/portlet/ext/contentlet/publishing/view_publish_endpoint_list.jsp?delEp="+identifier;		
			
			var myCp = dijit.byId("endpointsContent");	
			
			if (myCp) {
				myCp.destroyRecursive(false);
			}
			myCp = new dojox.layout.ContentPane({
				id : "endpointsContent"
			}).placeAt("endpoint_servers");
		
			myCp.attr("href", url);			
			myCp.refresh();	
		}	
	}
</script>
<div class="portlet-wrapper">
	<div>
		 <%= LanguageUtil.get(pageContext, "publisher_Manager") %>
		<hr/>
	</div>
	<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">
  		<div id="searchLucene" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "publisher_Search") %>" >
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
				<%-- &nbsp;&nbsp;<%= LanguageUtil.get(pageContext, "publisher_Show") %> 
				<input dojoType="dijit.form.CheckBox" checked="checked" type="checkbox" name="showPendings" value="true" id="showPendings" onclick="doQueueFilter()" /> <label for="showPendings"><%=LanguageUtil.get(pageContext, "publisher_Queue_Pending")%></label> 
				<input dojoType="dijit.form.CheckBox" checked="checked" type="checkbox" name="showErrors" value="true" id="showErrors"  onclick="doQueueFilter()"  /> <label for="showErrors"><%=LanguageUtil.get(pageContext, "publisher_Queue_Error")%></label> --%>
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
  		
  		<div id="audit" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "publisher_Audit") %>" >
			<div>
				<button class="solr_right" dojoType="dijit.form.Button" onClick="doAuditFilter();" iconClass="resetIcon">
					<%= LanguageUtil.get(pageContext, "publisher_Refresh") %> 
				</button> 
			</div>			
			<hr>
			<div>&nbsp;</div>
  			<div id="audit_results">
			</div>
			<script type="text/javascript">
			dojo.ready(function(){
				doAuditFilter();
			});
			</script>
  		</div>
  		
  		<div id="endpoints" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "publisher_Endpoints") %>" >
  			<div id="endpoint_servers">
			</div>
			<script type="text/javascript">
			dojo.ready(function(){
				loadPublishQueueEndpoints();
			});
			</script>
  		</div>
	</div>
</div>


