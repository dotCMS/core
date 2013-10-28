<%@ include file="/html/portlet/ext/cmsconfig/init.jsp" %>

<%@ page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint"%>
<%@ page import="java.util.List"%>
<%@ page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI"%>
<%@ page import="com.dotmarketing.business.APILocator"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotcms.publisher.environment.business.EnvironmentAPI"%>
<%@ page import="com.dotcms.publisher.environment.bean.Environment"%>
<%@ page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint"%>
<%@ page import="com.dotcms.enterprise.LicenseUtil" %>

<%	if( LicenseUtil.getLevel()<300){ %>
    <%@ include file="/html/portlet/ext/contentlet/publishing/not_licensed.jsp" %>
<%return;} %>

<style>

.node {
  margin: 20px;
  padding: 2px;
  width: auto;
  background: #3f65b7;
  background-clip: padding-box;
  border: 1px solid #172b4e;
  border-bottom-color: #142647;
  border-radius: 5px;
  background-image: -webkit-radial-gradient(cover, #437dd6, #3960a6);
  background-image: -moz-radial-gradient(cover, #437dd6, #3960a6);
  background-image: -o-radial-gradient(cover, #437dd6, #3960a6);
  background-image: radial-gradient(cover, #437dd6, #3960a6);
  -webkit-box-shadow: inset 0 1px rgba(255, 255, 255, 0.3), inset 0 0 1px 1px rgba(255, 255, 255, 0.1), 0 2px 10px rgba(0, 0, 0, 0.5);
  box-shadow: inset 0 1px rgba(255, 255, 255, 0.3), inset 0 0 1px 1px rgba(255, 255, 255, 0.1), 0 2px 10px rgba(0, 0, 0, 0.5);
  display:inline-block;
  text-align: left;
}

.node > h1 {
  margin-bottom: 20px;
  font-size: 12px;
  font-weight: bold;
  text-align: left;
  text-shadow: 0 -1px rgba(0, 0, 0, 0.4);
  background-color: #fff;
}

div.centre
{
  width: 200px;
  display: block;
  margin-left: auto;
  margin-right: auto;
}

.cluster {
  margin: 20px;
  padding: 10px;
  width: auto;
  background-clip: padding-box;
  border: 1px solid #d0d0d0;
  border-radius: 5px;

  text-align: center;
}

.left_td {
	background: #ececec;
}
</style>
<script type="text/javascript">

dojo.require("dojox.data.QueryReadStore");
dojo.require("dojox.grid.DataGrid");

// Cache Cluster Status
var cacheClusterStatus;

var xhrArgs = {
	url : "/api/cluster/getCacheClusterStatus/",
	handleAs : "json",
	sync: true,
	load : function(data) {
		cacheClusterStatus = data;
	},
	error : function(error) {
		targetNode.innerHTML = "An unexpected error occurred: " + error;
	}
}

var deferred = dojo.xhrGet(xhrArgs);

var clusterStatusDiv = dojo.create("div",
		{ innerHTML: "<table class='listingTable' style='background:white; width:auto'>"
						+ "<tr><td class='left_td'>Cluster Name</td><td>"+cacheClusterStatus.clusterName+"</td></tr>"
						+ "<tr><td class='left_td'>Channel Open</td><td>"+cacheClusterStatus.open+"</td></tr>"
						+ "<tr><td class='left_td'>Number of Nodes</td><td>"+cacheClusterStatus.numerOfNodes+"</td></tr>"
						+ "<tr><td class='left_td'>Address</td><td>"+cacheClusterStatus.address+"</td></tr>"
						+ "<tr><td class='left_td'>Received Bytes</td><td>"+cacheClusterStatus.receivedBytes+"</td></tr>"
						+ "<tr><td class='left_td'>Received Messages</td><td>"+cacheClusterStatus.receivedMessages+"</td></tr>"
						+ "<tr><td class='left_td'>Sent Bytes</td><td>"+cacheClusterStatus.sentBytes+"</td></tr>"
						+ "<tr><td class='left_td'>Sent Messages</td><td>"+cacheClusterStatus.sentMessages+"</td></tr>"
// 							+ "<tr><td class='left_td'>Cache Status</td><td><div id='cache_"+item.id+"'style='cursor:pointer;background:GREEN; width:20px;height:20px;'></td></tr>"
						+ "</table>"
		});
dojo.place(clusterStatusDiv, dojo.byId("cacheClusterStatus"))


// Cache Cluster Nodes Status
var cacheClusterNodes;

xhrArgs = {
	url : "/api/cluster/getCacheNodesStatus/",
	handleAs : "json",
	sync: true,
	load : function(data) {
		cacheClusterNodes = data;
	},
	error : function(error) {
		targetNode.innerHTML = "An unexpected error occurred: " + error;
	}
}

deferred = dojo.xhrGet(xhrArgs);

dojo.forEach(cacheClusterNodes, function(item, index){
	var nodeDiv = dojo.create("div",
			{ innerHTML: "<table class='listingTable' style='background:white; width:auto'>"
							+ "<tr><td class='left_td'>ID</td><td>"+item.id+"</td></tr>"
							+ "<tr><td class='left_td'>Physical Address</td><td>"+item.ip+"</td></tr>"
							+ "</table>"
			});
	dojo.attr(nodeDiv, "class", "node");
	dojo.place(nodeDiv, dojo.byId("cacheClusterNodeStatus"))

});


// ES Cluster Status
var esClusterStatus;

xhrArgs = {
	url : "/api/cluster/getESClusterStatus/",
	handleAs : "json",
	sync: true,
	load : function(data) {
		esClusterStatus = data;
	},
	error : function(error) {
		targetNode.innerHTML = "An unexpected error occurred: " + error;
	}
}

deferred = dojo.xhrGet(xhrArgs);

var esClusterStatusDiv = dojo.create("div",
		{ innerHTML: "<table class='listingTable' style='background:white; width:auto'>"
						+ "<tr><td class='left_td'>Cluster Name</td><td>"+esClusterStatus.cluster_name+"</td></tr>"
						+ "<tr><td class='left_td'>Status</td><td>"+esClusterStatus.status+"</td></tr>"
						+ "<tr><td class='left_td'>Number of Nodes</td><td>"+esClusterStatus.number_of_nodes+"</td></tr>"
						+ "<tr><td class='left_td'>active_primary_shards</td><td>"+esClusterStatus.active_primary_shards+"</td></tr>"
						+ "<tr><td class='left_td'>active_shards</td><td>"+esClusterStatus.active_shards+"</td></tr>"
						+ "<tr><td class='left_td'>relocating_shards</td><td>"+esClusterStatus.relocating_shards+"</td></tr>"
						+ "<tr><td class='left_td'>initializing_shards</td><td>"+esClusterStatus.initializing_shards+"</td></tr>"
						+ "<tr><td class='left_td'>unassigned_shards</td><td>"+esClusterStatus.unassigned_shards+"</td></tr>"
// 							+ "<tr><td class='left_td'>Cache Status</td><td><div id='cache_"+item.id+"'style='cursor:pointer;background:GREEN; width:20px;height:20px;'></td></tr>"
						+ "</table>"
		});
dojo.place(esClusterStatusDiv, dojo.byId("esClusterStatus"))

// ES Cluster Nodes Status
var esClusterNodes;

xhrArgs = {
	url : "/api/cluster/getESNodesStatus/",
	handleAs : "json",
	sync: true,
	load : function(data) {
		esClusterNodes = data.nodes;
	},
	error : function(error) {
		targetNode.innerHTML = "An unexpected error occurred: " + error;
	}
}

deferred = dojo.xhrGet(xhrArgs);


for(var prop in esClusterNodes){
	var node = esClusterNodes[prop];
	var nodeDiv = dojo.create("div",
			{ innerHTML: "<table class='listingTable' style='background:white; width:auto'>"
							+ "<tr><td class='left_td'>Name</td><td>"+node.name+"</td></tr>"
							+ "<tr><td class='left_td'>Transport Address</td><td>"+node.transport_address+"</td></tr>"
							+ "<tr><td class='left_td'>Hostname</td><td>"+node.hostname+"</td></tr>"
							+ "<tr><td class='left_td'>Http Address</td><td>"+node.http_address+"</td></tr>"
							+ "</table>"
			});
	dojo.attr(nodeDiv, "class", "node");
	dojo.place(nodeDiv, dojo.byId("esClusterNodeStatus"))

}

function showClusterPropertiesDialog() {
    var dialog = new dijit.Dialog({
        id: 'clusterConfigDialog',
        title: "<%= LanguageUtil.get(pageContext, "configuration_Cluster_Edit_Config")%>",
        style: "width: 700px; ",
        content: new dojox.layout.ContentPane({
            href: "/html/portlet/ext/cmsconfig/edit_cluster_properties.jsp"
        }),
        onHide: function () {
            var dialog = this;
            setTimeout(function () {
                dialog.destroyRecursive();
            }, 200);
        },
        onLoad: function () {

        }
    });
    dialog.show();
    dojo.style(dialog.domNode, 'top', '80px');
}


</script>

<div class="yui-g portlet-toolbar" align="center" style="width: 98%">
     <div class="yui-u first" style="width: 100%">
        <span  style="line-height:20px;font-weight: bold;"><%= LanguageUtil.get(pageContext, "configuration_Cluster_Config_Cache") %></span>
    </div>
</div>

<div class="">
<table id="cacheCluster" class="listingTable shadowBox">
    <tr>
        <th style="font-size: 8pt;" width="30%"><%= LanguageUtil.get(pageContext, "configuration_Cluster_Config_Status") %></th>
        <th style="font-size: 8pt;"><%= LanguageUtil.get(pageContext, "configuration_Cluster_Config_Node_Status") %></th>
    </tr>
    <tr style="text-align: center">
        <td width="30%"><div id='cacheClusterStatus'></div></td>
        <td><div id='cacheClusterNodeStatus'></div></td>
    </tr>
</table><br>

<div class="yui-g portlet-toolbar" align="center" style="width: 98%">
    <div class="yui-u first" style="width: 100%">
        <span  style="font-weight: bold;"><%= LanguageUtil.get(pageContext, "configuration_Cluster_Config_ES") %></span>
    </div>
    <div class="yui-u" style="text-align:right;">
        <button dojoType="dijit.form.Button" onClick="showClusterPropertiesDialog();" iconClass="plusIcon">
            <%= LanguageUtil.get(pageContext, "configuration_Cluster_Edit_Config") %>
        </button>
    </div>
</div>
<table class="listingTable shadowBox" >
    <tr>
        <th style="font-size: 8pt;" "width="30%"><%= LanguageUtil.get(pageContext, "configuration_Cluster_Config_Status") %></th>
        <th style="font-size: 8pt;"><%= LanguageUtil.get(pageContext, "configuration_Cluster_Config_Node_Status") %></th>
    </tr>
    <tr style="text-align: center">
        <td width="30%"><div id='esClusterStatus'></div></td>
        <td><div id='esClusterNodeStatus'></div></td>
    </tr>
</table>
</div>
<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>
<%--END OF ENVIROMENTS--%>
<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>

