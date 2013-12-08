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
/*   background: #3f65b7; */
/*   background-clip: padding-box; */
/*   border: 1px solid #172b4e; */
/*   border-bottom-color: #142647; */
  border-radius: 1px;
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
/* 	background: #ececec; */
	width: 50%;
	text-align: left;
}

.statusTable {
	text-align: right;
}


.listingTable2  th, .listingTable2 td {
font-size: 11px;
padding:5px 8px;
border:0px solid #d0d0d0;
}


</style>
<script type="text/javascript">


// Cache Cluster Status
function renderCacheClusterStatus() {
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
			{ innerHTML: "<table class='statusTable listingTable2' style='background:white; width:100%;'>"
							+ "<tr><td colspan='2' align='center' style='font-weight:bold'><%= LanguageUtil.get(pageContext, "configuration_Cluster_Config_Cache") %></td></tr>"
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

	dojo.empty(dojo.byId("cacheClusterStatus"));
	dojo.place(clusterStatusDiv, dojo.byId("cacheClusterStatus"))
}


function renderESClusterStatus() {

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
			{ innerHTML: "<table class='statusTable listingTable2' style='background:white; width:100%; font-size:8px'>"
							+ "<tr><td colspan='2' align='center' style='font-weight:bold'><%= LanguageUtil.get(pageContext, "configuration_Cluster_Config_ES") %></td></tr>"
							+ "<tr><td class='left_td'>Cluster Name</td><td>"+esClusterStatus.clusterName+"</td></tr>"
							+ "<tr><td class='left_td'>Status</td><td>"+esClusterStatus.status+"</td></tr>"
							+ "<tr><td class='left_td'>Number of Nodes</td><td>"+esClusterStatus.numerOfNodes+"</td></tr>"
							+ "<tr><td class='left_td'>active_primary_shards</td><td>"+esClusterStatus.activePrimaryShards+"</td></tr>"
							+ "<tr><td class='left_td'>active_shards</td><td>"+esClusterStatus.activeShards+"</td></tr>"
							+ "<tr><td class='left_td'>unassigned_shards</td><td>"+esClusterStatus.unasignedPrimaryShards+"</td></tr>"
	// 							+ "<tr><td class='left_td'>Cache Status</td><td><div id='cache_"+item.id+"'style='cursor:pointer;background:GREEN; width:20px;height:20px;'></td></tr>"
							+ "</table>"
			});

	dojo.empty(dojo.byId("esClusterStatus"));
	dojo.place(esClusterStatusDiv, dojo.byId("esClusterStatus"));

}

function renderNodesStatus() {

	//Node List
	var nodeList;

	xhrArgs = {
		url : "/api/cluster/getNodesStatus/",
		handleAs : "json",
		sync: true,
		load : function(data) {
			nodeList = data;
		},
		error : function(error) {
			targetNode.innerHTML = "An unexpected error occurred: " + error;
		}
	}

	deferred = dojo.xhrGet(xhrArgs);


	var nodesTableHTML = "<table style='width:100%; font-size:11px'> "
	+ "<tr ><th style='background: #F7F7F7'>Server ID</th>"
	+ "<th style='background: #F7F7F7'>IP Address</th>"
	+ "<th style='background: #F7F7F7'>Contacted</th>"
	+ "<th style='background: #F7F7F7'>Cache Status</th>"
	+ "<th style='background: #F7F7F7'>Cache Port</th>"
	+ "<th style='background: #F7F7F7'>ES Status</th>"
	+ "<th style='background: #F7F7F7'>ES Port</th>"
	+ "</tr>";

	dojo.forEach(nodeList, function(item, index){
		nodesTableHTML +=	"<tr><td style='vertical-align:middle'><table class='listingTable2'><td width=3px'><span class='backupIcon' ></span></td>"
								+ "<td width='240px' style='text-align:left;'>" + item.serverId + "</td><td width='3px'>" + (item.myself?"<span class='femaleIcon'></span>":"")+"</td></table></td>"
								+ "<td align='left'>"+item.ipAddress+"</td>"
								+ "<td align='left'>"+item.contacted+" secs ago</td>"
								+ "<td align='center'><div style='background:"+(item.cacheStatus?"GREEN":"RED")+"; width:20px;height:20px;'></div></td>"
								+ "<td align='left'>"+item.cachePort+"</td>"
								+ "<td align='center'><div style='background:"+(item.esStatus?"GREEN":"RED")+"; width:20px;height:20px;'></div></td>"
								+ "<td align='left'>"+item.esPort+"</td>"
								+ "</tr>";
				});

	nodesTableHTML += "</table>"

	var nodesTable = dojo.create("div", { innerHTML: nodesTableHTML });

	dojo.empty(dojo.byId("nodes"));
	dojo.place(nodesTable, dojo.byId("nodes"));
	dojo.parser.parse("nodes");

}


function refreshStatus() {
	renderCacheClusterStatus();
	renderESClusterStatus();
	renderNodesStatus();
}

function showClusterPropertiesDialog() {

	dojo.destroy("nodes");
	dojo.create("div", { id: 'nodes' }, "nodesTD");

	//ES Cluster Nodes Status
	var properties;

	xhrArgs = {
		url : "/api/cluster/getESConfigProperties/",
		handleAs : "json",
		sync: true,
		load : function(data) {
			properties = data;
		},
		error : function(error) {
			targetNode.innerHTML = "An unexpected error occurred: " + error;
		}
	}

	deferred = dojo.xhrGet(xhrArgs);

	var html = "<table class='listingTable' style='width:90%'>";

	for(var key in properties){
		var value = properties[key];
		html += "<tr><td class='left_td' style='font-size:11px'>"+key+"</td><td><input style='width: 95%; font-size:11px' type='text' data-dojo-type='dijit/form/TextBox' name='"+key+"+' value="+value+"></input></td></tr>"

	}

	html += "</table>"

	var nodeDiv = dojo.create("div",
			{ innerHTML: html
			});

	dojo.empty(dojo.byId("propertiesDiv"));
	dojo.place(nodeDiv, dojo.byId("propertiesDiv"))

	var form = dojo.byId("propertiesForm");


	dojo.connect(form, "onSubmit", function(event){

	    // Stop the submit event since we want to control form submission.
	    dojo.stopEvent(event);

	    // The parameters to pass to xhrPost, the form, how to handle it, and the callbacks.
	    // Note that there isn't a url passed.  xhrPost will extract the url to call from the form's
	    //'action' attribute.  You could also leave off the action attribute and set the url of the xhrPost object
	    // either should work.

	    var xhrArgs = {
	      form: dojo.byId("propertiesForm"),
	      handleAs: "text",
	      load: function(data){
	        dojo.byId("response").innerHTML = "Form posted.";
	      },
	      error: function(error){
	        // We'll 404 in the demo, but that's okay.  We don't have a 'postIt' service on the
	        // docs server.
	        dojo.byId("response").innerHTML = "Form posted.";
	      }
	    }
	    // Call the asynchronous xhrPost
	    dojo.byId("response").innerHTML = "Form being sent..."
	    var deferred = dojo.xhrPost(xhrArgs);
	  });

	dijit.byId('clusterPropertiesDialog').show();


}

renderCacheClusterStatus();
renderESClusterStatus();
renderNodesStatus();



</script>

<div class="yui-g portlet-toolbar">
    <div class="yui-u first">
        <span class="rServerIcon"></span>
        <span  style="line-height:20px;font-weight: bold;"><%= LanguageUtil.get(pageContext, "configuration_Cluster_Status") %></span>
    </div>
    <div class="yui-u" style="text-align:right;">
    	<button  dojoType="dijit.form.Button" onClick="refreshStatus();" iconClass="resetIcon">
            <%= LanguageUtil.get(pageContext, "publisher_Refresh") %>
        </button>
        <button dojoType="dijit.form.Button" onClick="refreshStatus();" iconClass="plusIcon">
            Wire This Node
        </button>
    </div>
</div>

<table id="cacheCluster" class="listingTable shadowBox">
    <tr>
        <th style="font-size: 8pt;" width="20%"><%= LanguageUtil.get(pageContext, "configuration_Cluster_General_Status") %></th>
        <th style="font-size: 8pt;text-align: center"><%= LanguageUtil.get(pageContext, "configuration_Cluster_Servers") %></th>
    </tr>
    <tr style="text-align: center">
        <td width="25%" style="padding:0px">
        	<div id='generalStatus'>
        		<table>
					    <tr>
					        <td width="30%" style="padding:0px"><div id='cacheClusterStatus'></div></td>
					    </tr>
					    <tr>
					        <td width="30%" style="padding:0px"><div id='esClusterStatus'></div></td>
					    </tr>
					</table>
        	</div>
        </td>
        <td valign="top" id="nodesTD" style="padding: 0;">
        	<div id='nodes' style="width: 100%">
        	</div>
        </td>
    </tr>
</table><br>



<div id="clusterPropertiesDialog" dojoType="dijit.Dialog" disableCloseButton="true" title="<%=LanguageUtil.get(pageContext, "configuration_Cluster_Edit_Config")%>" style="display: none; height: 470px; width:500px">
    <div style="padding:10px 15px;">
      <form action="/api/cluster/updateESConfigProperties/" id="propertiesForm" method="post">
            <div style="height: 380px;">
                <div id='propertiesDiv'></div>
            </div>
            <div align="center">
               <button style="padding-bottom: 10px;" dojoType="dijit.form.Button"
					iconClass="saveIcon"
					type="submit"><%=LanguageUtil.get(pageContext, "Save")%></button>
				<button style="padding-bottom: 10px;" dojoType="dijit.form.Button"
					onClick='bundles.modifyExtraPackages()' iconClass="cancelIcon"
					type="button"><%=LanguageUtil.get(pageContext, "Cancel")%></button>
			</div>
        </form>
    </div>
</div>

