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
		var ran=new Date().getTime();
		var url = "/html/portlet/ext/contentlet/publishing/view_publish_audit_list.jsp?v="+ ran + "&" + urlParams;

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


	function goToAddEndpoint(environmentId, isSender){
		var dialog = new dijit.Dialog({
			id: 'addEndpoint',
	        title: "<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Add")%>",
	        style: "width: 800px; ",
	        content: new dojox.layout.ContentPane({
	            href: "/html/portlet/ext/contentlet/publishing/add_publish_endpoint.jsp?environmentId="+environmentId+"&isSender="+isSender
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

	function goToEditEndpoint(identifier, envId, isSender){
		var dialog = new dijit.Dialog({
			id: 'addEndpoint',
	        title: "<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Edit")%>",
	        style: "width: 800px; ",
	        content: new dojox.layout.ContentPane({
	            href: "/html/portlet/ext/contentlet/publishing/add_publish_endpoint.jsp?op=edit&id="+identifier+"&environmentId="+envId+"&isSender="+isSender
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

	function deleteEndpoint(identifier, fromEnvironment){
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

			if(fromEnvironment) {
				loadEnvironments();
			}
		}
	}

	function loadEnvironments(){
		var url = "/html/portlet/ext/contentlet/publishing/view_publish_environments.jsp";

		var myCp = dijit.byId("environmentsContent");

		if (myCp) {
			myCp.destroyRecursive(false);
		}
		myCp = new dojox.layout.ContentPane({
			id : "environmentsContent"
		}).placeAt("environmentsDiv");

		myCp.attr("href", url);
		myCp.refresh();
	}

	function goToAddEnvironment(){
		var dialog = new dijit.Dialog({
			id: 'addEnvironment',
	        title: "<%= LanguageUtil.get(pageContext, "publisher_Environment_Add")%>",
	        style: "width: 700px; ",
	        content: new dojox.layout.ContentPane({
	            href: "/html/portlet/ext/contentlet/publishing/add_publish_environment.jsp"
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

	function goToEditEnvironment(identifier){
		var dialog = new dijit.Dialog({
			id: 'addEnvironment',
	        title: "<%= LanguageUtil.get(pageContext, "publisher_Edit_Environment_Title")%>",
	        style: "width: 600px; ",
	        content: new dojox.layout.ContentPane({
	        	href: "/html/portlet/ext/contentlet/publishing/add_publish_environment.jsp?op=edit&id="+identifier
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

	function backToEnvironmentList(addedEndPoint){

		if(!addedEndPoint) {
			dijit.byId("addEnvironment").hide();
		} else {
			dijit.byId("addEndpoint").hide();
		}
		loadEnvironments();

	}

	function deleteEnvironment(identifier){
		if(confirm("<%= LanguageUtil.get(pageContext, "publisher_Delete_Environment_Confirm")%>")){
			var url = "/html/portlet/ext/contentlet/publishing/view_publish_environments.jsp?delEnv="+identifier;

			var myCp = dijit.byId("environmentsContent");

			if (myCp) {
				myCp.destroyRecursive(false);
			}
			myCp = new dojox.layout.ContentPane({
				id : "environmentsContent"
			}).placeAt("environmentsDiv");

			myCp.attr("href", url);
			myCp.refresh();
		}
	}

	function loadUnpushedBundles(){
		var url = "/html/portlet/ext/contentlet/publishing/view_unpushed_bundles.jsp";

		var myCp = dijit.byId("unpushedBundlesContent");

		if (myCp) {
			myCp.destroyRecursive(false);
		}
		myCp = new dojox.layout.ContentPane({
			id : "unpushedBundlesContent"
		}).placeAt("unpushedBundlesDiv");

		myCp.attr("href", url);
		myCp.refresh();
	}

	function deleteBundle(identifier){
		if(confirm("<%= LanguageUtil.get(pageContext, "publisher_Unpushed_Bundles_Delete_Confirm")%>")){
			var url = "/html/portlet/ext/contentlet/publishing/view_unpushed_bundles.jsp?delBundle="+identifier;

			var myCp = dijit.byId("unpushedBundlesContent");

			if (myCp) {
				myCp.destroyRecursive(false);
			}
			myCp = new dojox.layout.ContentPane({
				id : "unpushedBundlesContent"
			}).placeAt("unpushedBundles");

			myCp.attr("href", url);
			myCp.refresh();
		}
	}

	function deleteAsset(assetId, bundleId){
		if(confirm("<%= LanguageUtil.get(pageContext, "publisher_Unpushed_Bundles_Delete_Asset_Confirm")%>")){
			var url = "/html/portlet/ext/contentlet/publishing/view_unpushed_bundles.jsp?delAsset="+assetId+"&bundleId="+bundleId;

			var myCp = dijit.byId("unpushedBundlesContent");

			if (myCp) {
				myCp.destroyRecursive(false);
			}
			myCp = new dojox.layout.ContentPane({
				id : "unpushedBundlesContent"
			}).placeAt("unpushedBundles");

			myCp.attr("href", url);
			myCp.refresh();
		}
	}

	function goToEditBundle(identifier){
		var dialog = new dijit.Dialog({
			id: 'editBundle',
	        title: "<%= LanguageUtil.get(pageContext, "publisher_Unpushed_Bundles_Edit")%>",
	        style: "width: 400px; ",
	        content: new dojox.layout.ContentPane({
	        	href: "/html/portlet/ext/contentlet/publishing/edit_publish_bundle.jsp?id="+identifier
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

    /**
     * Downloads a selected bundle id. This selected bundle is an Unpushed Bundle
     * @param bundleId
     * @param operation publish/unpublish
     */
    var downloadUnpushedBundle = function (bundleId, operation) {
        window.location = '/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/downloadUnpushedBundle/bundleId/' + bundleId + '/operation/' + operation;
    };

	dojo.require("dotcms.dojo.push.PushHandler");
	var pushHandler = new dotcms.dojo.push.PushHandler('<%=LanguageUtil.get(pageContext, "Remote-Publish-Bundle")%>', true);

	function remotePublish(objId) {
		pushHandler.showDialog(objId);
	}


   function filterStructure(varName){
	   var q  = dijit.byId("query").getValue();
	   if(q.indexOf(varName) <0){
		   q = q + " +structureName:" + varName;
		   dijit.byId("query").setValue(q);
		   doLuceneFilter ();
	   }
   }


	dojo.ready(function(){
		//loadPublishQueueEndpoints();
		//doQueueFilter();
		//doAuditFilter();
		loadUnpushedBundles();

		var tab =dijit.byId("mainTabContainer");
	   	dojo.connect(tab, 'selectChild',
			function (evt) {
			 	selectedTab = tab.selectedChildWidget;
				  	if(selectedTab.id =="queue"){
				  		doQueueFilter();
				  	}
				  	else if(selectedTab.id =="unpushedBundles"){
				  		loadUnpushedBundles();
				  	}
				  	else if(selectedTab.id =="audit"){
				  		refreshAuditList("");
				  	}
				  	else if(selectedTab.id =="endpoints"){
				  		loadPublishQueueEndpoints();
				  	}
				  	else if(selectedTab.id =="environments"){
				  		loadEnvironments();
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

	var whoCanUse = new Array()

	function addSelectedToWhoCanUse(){

		var select = dijit.byId("whoCanUseSelect");

		var user = select.getValue();
		var userName = select.attr('displayedValue');

		addToWhoCanUse(user, userName);
		refreshWhoCanUse();
	}

	function addToWhoCanUse ( myId, myName){
		for(i=0;i < this.whoCanUse.length;i++){
			if(myId == this.whoCanUse[i].id  ||  myId == "user-" + this.whoCanUse[i].id || myId == "role-" + this.whoCanUse[i].id){
				return;
			}
		}

		var entry = {name:myName,id:myId };
		this.whoCanUse[this.whoCanUse.length] =entry;

	}

	function refreshWhoCanUse(){
		dojo.empty("whoCanUseTbl");
		var table = dojo.byId("whoCanUseTbl");
		var x = "";

		this.whoCanUse = this.whoCanUse.sort(function(a,b){
			var x = a.name.toLowerCase();
		    var y = b.name.toLowerCase();
		    return ((x < y) ? -1 : ((x > y) ? 1 : 0));
		});
		for(i=0; i< this.whoCanUse.length ; i++){
			var what = (this.whoCanUse[i].id.indexOf("user") > -1) ? " (<%=LanguageUtil.get(pageContext, "User")%>)" : "";
			x = x + this.whoCanUse[i].id + ",";
			var tr = dojo.create("tr", null, table);
			dojo.create("td", { innerHTML: "<span class='deleteIcon'></span>",className:"wfXBox", onClick:"removeFromWhoCanUse('" + this.whoCanUse[i].id +"');refreshWhoCanUse()" }, tr);
			dojo.create("td", { innerHTML: this.whoCanUse[i].name + what}, tr);

		}
		dojo.byId('whoCanUse').value = x;

	}

	function removeFromWhoCanUse(myId){

		var x=0;
		var newCanUse = new Array();
		for(i=0;i < this.whoCanUse.length;i++){
			if(myId != this.whoCanUse[i].id){
				newCanUse[x] = this.whoCanUse[i];
				x++;
			}
		}
		this.whoCanUse= newCanUse;
	}


</script>



<div class="portlet-wrapper">
	<div id="mainTabContainer" dojoType="dijit.layout.TabContainer" dolayout="false">

		<div id="unpushedBundles" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "publisher_Unpushed_Bundles") %>" >
  			<div id="unpushedBundlesDiv">
			</div>

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
                    <button  dojoType="dijit.form.Button" onClick="retryBundles();" iconClass="repeatIcon">
                        <%= LanguageUtil.get(pageContext, "publisher_retry_bundles") %>
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

  		<div id="environments" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Sending_Server_Short") %>" >
  			<div id="environmentsDiv">
			</div>

  		</div>

  		<div id="endpoints" dojoType="dijit.layout.ContentPane" title="<%= LanguageUtil.get(pageContext, "publisher_Endpoints_Receiving_Server_Short") %>" >
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

<form id="remotePublishForm">
	<input name="assetIdentifier" id="assetIdentifier" type="hidden" value="">
	<input name="remotePublishDate" id="remotePublishDate" type="hidden" value="">
	<input name="remotePublishTime" id="remotePublishTime" type="hidden" value="">
	<input name="remotePublishExpireDate" id="remotePublishExpireDate" type="hidden" value="">
	<input name="remotePublishExpireTime" id="remotePublishExpireTime" type="hidden" value="">
	<input name="iWantTo" id=iWantTo type="hidden" value="">
	<input name="whoToSend" id=whoToSend type="hidden" value="">
	<input name="newBundle" id=newBundle type="hidden" value="">
	<input name="bundleName" id=bundleName type="hidden" value="">
	<input name="bundleSelect" id=bundleSelect type="hidden" value="">
	<input name="forcePush" id=forcePush type="hidden" value="">
</form>

