<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@ include file="/html/portlet/ext/contentlet/publishing/init.jsp" %>
<%@page import="com.liferay.portal.util.WebKeys"%>
<%@page import="com.dotmarketing.business.Layout"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@page import="com.dotmarketing.util.URLEncoder"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%
	String portletId1 = "EXT_CONTENT_PUBLISHING_TOOL";
	Portlet portlet1 = PortletManagerUtil.getPortletById(company.getCompanyId(), portletId1);
	String strutsAction = ParamUtil.get(request, "struts_action", null);
	
	if (!com.dotmarketing.util.UtilMethods.isSet(strutsAction) || strutsAction.equals(portlet1.getInitParams().get("view-action"))) {
		List<CrumbTrailEntry> crumbTrailEntries = new ArrayList<CrumbTrailEntry>();
		crumbTrailEntries.add(new CrumbTrailEntry(LanguageUtil.get(pageContext, "javax.portlet.title." + portletId1), null));
		request.setAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS, crumbTrailEntries);
	}
	
	request.setAttribute(com.dotmarketing.util.WebKeys.DONT_DISPLAY_SUBNAV_ALL_HOSTS, false);

	
%>
<div class="portlet-wrapper">
	<%@ include file="/html/portlet/ext/common/sub_nav_inc.jsp" %>
</div>





<script type="text/javascript">
	dojo.require("dijit.form.NumberTextBox");
    dojo.require("dojox.layout.ContentPane");
	
	function doQueueFilter () {
	

		refreshQueueList("");
	}
	
	function doAuditFilter() {

		refreshAuditList("");
	}
	
	
	var lastUrlParams ;
	
	function refreshQueueList(urlParams){

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
		url="&query="+encodeURIComponent(dijit.byId("luceneQuery").getValue());
		url+="&sort="+dijit.byId("sort").value;
		
		url="layout=<%=layout.getId()%>"+url;
		refreshLuceneList(url);
		dijit.byId("clearButton").setDisabled(false);
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
			myCp.destroyRecursive(false);
		}
		myCp = new dojox.layout.ContentPane({
			id : "endpointsContent"
		}).placeAt("endpoint_servers");

		myCp.attr("href", url);
		myCp.refresh();
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
			id: 'addEndpoint',
	        title: "<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Edit")%>",
	        style: "width: 800px; ",
	        content: new dojox.layout.ContentPane({
	            href: "/html/portlet/ext/contentlet/publishing/add_publish_endpoint.jsp?op=edit&id="+identifier
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

	function backToEndpointsList(){

		dijit.byId("addEndpoint").hide();
		loadPublishQueueEndpoints();

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

	
	
	   
   function filterStructure(varName){
	   var q  = dijit.byId("query").getValue();
	   if(q.indexOf(varName) <0){
		   q = q + " +structureName:" + varName;
		   dijit.byId("query").setValue(q);
		   doLuceneFilter ();
	   }
   }
	   
	function clearLuceneSearch(){
		   dijit.byId("luceneQuery").setValue("*");
		   dojo.byId("lucene_results").innerHTML="";
		   dijit.byId("clearButton").setDisabled(true);
		   doLuceneFilter ();
	}
	
	
	
	
	
	
	
	
	dojo.ready(function(){
		//loadPublishQueueEndpoints();
		//doQueueFilter();
		//doAuditFilter();

		var tab =dijit.byId("mainTabContainer");
	   	dojo.connect(tab, 'selectChild',
			function (evt) {
			 	selectedTab = tab.selectedChildWidget;
				  	if(selectedTab.id =="queue"){
				  		doQueueFilter();
				  	}
				  	else if(selectedTab.id =="audit"){
				  		refreshAuditList("");
				  	}
				  	else if(selectedTab.id =="endpoints"){
				  		loadPublishQueueEndpoints();
				  	}
			});

	});
	
	
	function doEnterSearch(e){
	    if(e.keyCode == dojo.keys.ENTER){
	        dojo.stopEvent(e);
	        doLuceneFilter();
	    }
	}
	
	
	function showBundleUpload(){
		dijit.byId("uploadBundleDiv").show();

	}
	
	dojo.require("dojo.io.iframe");
	function doBundleUpload(){
		var suffix = ".tar.gz";
		var filename = dojo.byId("uploadBundleFile").value;
		
		
		if(filename.indexOf(suffix) == -1 || (filename.length - suffix.length != filename.indexOf(suffix))){
			alert("<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "publisher_please_upload_bundle_ending_with_targz")) %>");
			return false;
		}
		
		var td = dojo.io.iframe.send({
			url: "/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/uploadBundle",
			form: "uploadBundleForm",
			method: "post",
			content: {fnx:1},
			timeoutSeconds: 5,
			preventCache: true,
			handleAs: "text",
			load: dojo.hitch(this, function(response) {
                if (response.status=='error') {
                    alert("Error Uploading the Bundle");
                } else {
                	backToBundleList();
                }
            }) 
		});
		
	}

	
	function backToBundleList(){

		dijit.byId("uploadBundleDiv").hide();
		refreshAuditList("");
	}
	
	
	
	
	

	
</script>



<div class="portlet-wrapper">
	<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">

  		<div id="searchLucene" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "publisher_Search") %>" >
  			<div>
				<dl>	
					<dt><strong><%= LanguageUtil.get(pageContext, "Search") %>:</strong></dt>
					<dd>
						<textarea onkeydown="doEnterSearch" dojoType="dijit.form.Textarea" name="luceneQuery" style="width:500px;min-height:75px;"  id="luceneQuery" ></textarea>
					</dd>
					<dt><strong><%= LanguageUtil.get(pageContext, "publisher_Sort") %> </strong></dt><dd><input name="sort" id="sort" dojoType="dijit.form.TextBox" type="text" value="modDate desc" size="10" /></dd>	
					
					<dt></dt>
					<dd>
					
						<button dojoType="dijit.form.Button" onclick="doLuceneFilter();" iconClass="searchIcon"><%= LanguageUtil.get(pageContext, "publisher_Search_Content") %></button>
	                    &nbsp;
	                    <button dojoType="dijit.form.Button" id="clearButton" disabled="true" onClick="clearLuceneSearch();" iconClass="resetIcon">
	                            <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Clear-Search")) %>
	                    </button>
					</dd>
				</dl>
			</div>
			<hr>
			<div>&nbsp;</div>
			<div id="lucene_results"></div>
		</div>	
		
		
		
		
		
		
		
		
  		<div id="queue" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "publisher_Queue") %>" >
  		   <div class="buttonRow" >
  		    	
	  		    <div style="float:left">
					<button dojoType="dijit.form.Button" onClick="deleteQueue();" iconClass="deleteIcon">
						<%= LanguageUtil.get(pageContext, "publisher_Delete_from_queue") %> 
					</button>
				</div>
				<div style="float:right">
					<button  dojoType="dijit.form.Button" onClick="doQueueFilter();" iconClass="resetIcon">
						<%= LanguageUtil.get(pageContext, "publisher_Refresh") %> 
					</button> 
				</div>			
			
				<div>&nbsp;</div>
			</div>
			<div style="height:10px;"></div>
  			<div id="queue_results"></div>

  		</div>
		

		
  		<div id="audit" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "publisher_Audit") %>" >
			<div class="buttonRow" >
	  		    <div style="float:left">
					<button dojoType="dijit.form.Button" onClick="deleteAudits();" id="deleteAuditsBtn" iconClass="deleteIcon">
						<%= LanguageUtil.get(pageContext, "Delete") %> 
					</button>
				</div>
				
				
				<div style="float:right">
					<button  dojoType="dijit.form.Button" onClick="showBundleUpload();" iconClass="uploadIcon">
						<%= LanguageUtil.get(pageContext, "publisher_upload") %> 
					</button> 
					<button  dojoType="dijit.form.Button" onClick="doAuditFilter();" iconClass="resetIcon">
						<%= LanguageUtil.get(pageContext, "publisher_Refresh") %> 
					</button> 
				</div>			
				<div>&nbsp;</div>
			</div>
			<div style="height:10px;"></div>
  			<div id="audit_results"></div>
  		</div>
  		
  		<div id="endpoints" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "publisher_Endpoints") %>" >
  			<div id="endpoint_servers">
			</div>

  		</div>
	</div>
</div>




<div dojoType="dijit.Dialog" id="uploadBundleDiv" >
	<form action="/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/uploadBundle" enctype="multipart/form-data" id="uploadBundleForm" name="uploadBundleForm" method="post">
		<div>
			<%= LanguageUtil.get(pageContext, "File") %>  : <input type="file" style="width:400px;"  id="uploadBundleFile" name="uploadBundleFile"><br>
		</div>
		<div>&nbsp;</div>
		<div style="text-align: center">
			<button  dojoType="dijit.form.Button" onClick="doBundleUpload();" iconClass="uploadIcon">
				<%= LanguageUtil.get(pageContext, "publisher_upload") %> 
			</button> 
		</div>
		
	</form>
</div>

