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

html {overflow-y: scroll;}

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
// 		sync: true,
		load : function(data) {
			cacheClusterStatus = data;
			var clusterStatusDiv = dojo.create("div",
					{ innerHTML: "<table class='statusTable listingTable2' style='background:white; width:100%;'>"
									+ "<tr><td colspan='2' align='center' style='font-weight:bold'><%= LanguageUtil.get(pageContext, "configuration_cluster_config_cache") %></td></tr>"
									+ "<tr><td class='left_td'><%= LanguageUtil.get(pageContext, "configuration_cluster_name") %></td><td>"+(cacheClusterStatus.clusterName?cacheClusterStatus.clusterName:"<%= LanguageUtil.get(pageContext, "configuration_cluster_NA") %>")+"</td></tr>"
									+ "<tr><td class='left_td'><%= LanguageUtil.get(pageContext, "configuration_cluster_channel_open") %></td><td>"+(cacheClusterStatus.open?cacheClusterStatus.open:"<%= LanguageUtil.get(pageContext, "configuration_cluster_NA") %>")+"</td></tr>"
									+ "<tr><td class='left_td'><%= LanguageUtil.get(pageContext, "configuration_cluster_number_of_nodes") %></td><td>"+(cacheClusterStatus.numberOfNodes?cacheClusterStatus.numberOfNodes:"<%= LanguageUtil.get(pageContext, "configuration_cluster_NA") %>")+"</td></tr>"
									+ "<tr><td class='left_td'><%= LanguageUtil.get(pageContext, "configuration_cluster_address") %></td><td>"+(cacheClusterStatus.address?cacheClusterStatus.address:"<%= LanguageUtil.get(pageContext, "configuration_cluster_NA") %>")+"</td></tr>"
									+ "<tr><td class='left_td'><%= LanguageUtil.get(pageContext, "configuration_cluster_received_bytes") %></td><td>"+(cacheClusterStatus.receivedBytes?cacheClusterStatus.receivedBytes:"<%= LanguageUtil.get(pageContext, "configuration_cluster_NA") %>")+"</td></tr>"
									+ "<tr><td class='left_td'><%= LanguageUtil.get(pageContext, "configuration_cluster_received_messages") %></td><td>"+(cacheClusterStatus.receivedMessages?cacheClusterStatus.receivedMessages:"<%= LanguageUtil.get(pageContext, "configuration_cluster_NA") %>")+"</td></tr>"
									+ "<tr><td class='left_td'><%= LanguageUtil.get(pageContext, "configuration_cluster_sent_bytes") %></td><td>"+(cacheClusterStatus.sentBytes?cacheClusterStatus.sentBytes:"<%= LanguageUtil.get(pageContext, "configuration_cluster_NA") %>")+"</td></tr>"
									+ "<tr><td class='left_td'><%= LanguageUtil.get(pageContext, "configuration_cluster_sent_messages") %></td><td>"+(cacheClusterStatus.sendMessages?cacheClusterStatus.sendMessages:"<%= LanguageUtil.get(pageContext, "configuration_cluster_NA") %>")+"</td></tr>"
			// 							+ "<tr><td class='left_td'>Cache Status</td><td><div id='cache_"+item.id+"'style='cursor:pointer;background:GREEN; width:20px;height:20px;'></td></tr>"
									+ "</table>"
					});

			dojo.empty(dojo.byId("cacheClusterStatus"));
			dojo.place(clusterStatusDiv, dojo.byId("cacheClusterStatus"))
		},
		error : function(error) {
			targetNode.innerHTML = "An unexpected error occurred: " + error;
		}
	}

	var deferred = dojo.xhrGet(xhrArgs);

}


function renderESClusterStatus() {

	// ES Cluster Status
	var esClusterStatus;


	xhrArgs = {
		url : "/api/cluster/getESClusterStatus/",
		handleAs : "json",
		load : function(data) {
			esClusterStatus = data;
			var esClusterStatusDiv = dojo.create("div",
					{ innerHTML: "<table class='statusTable listingTable2' style='background:white; width:100%; font-size:8px'>"
					+ "<tr><td colspan='2' align='center' style='font-weight:bold'><%= LanguageUtil.get(pageContext, "configuration_cluster_config_es") %></td></tr>"
					+ "<tr><td class='left_td'><%= LanguageUtil.get(pageContext, "configuration_cluster_name") %></td><td>"+(esClusterStatus.clusterName?esClusterStatus.clusterName:"<%= LanguageUtil.get(pageContext, "configuration_cluster_NA") %>")+"</td></tr>"
					+ "<tr><td class='left_td'><%= LanguageUtil.get(pageContext, "status") %></td><td>"+(esClusterStatus.status?esClusterStatus.status:"<%= LanguageUtil.get(pageContext, "configuration_cluster_NA") %>")+"</td></tr>"
					+ "<tr><td class='left_td'><%= LanguageUtil.get(pageContext, "configuration_cluster_number_of_nodes") %></td><td>"+(esClusterStatus.numberOfNodes?esClusterStatus.numberOfNodes:"<%= LanguageUtil.get(pageContext, "configuration_cluster_NA") %>")+"</td></tr>"
					+ "<tr><td class='left_td'><%= LanguageUtil.get(pageContext, "configuration_cluster_active_primary_shards") %></td><td>"+(esClusterStatus.activePrimaryShards?esClusterStatus.activePrimaryShards:"<%= LanguageUtil.get(pageContext, "configuration_cluster_NA") %>")+"</td></tr>"
					+ "<tr><td class='left_td'><%= LanguageUtil.get(pageContext, "configuration_cluster_active_shards") %></td><td>"+(esClusterStatus.activeShards?esClusterStatus.activeShards:"<%= LanguageUtil.get(pageContext, "configuration_cluster_NA") %>")+"</td></tr>"
					+ "<tr><td class='left_td'><%= LanguageUtil.get(pageContext, "configuration_cluster_unassigned_shards") %></td><td>"+(esClusterStatus.unasignedPrimaryShards?esClusterStatus.unasignedPrimaryShards:"<%= LanguageUtil.get(pageContext, "configuration_cluster_NA") %>")+"</td></tr>"
					+ "</table>"
					});

			dojo.empty(dojo.byId("esClusterStatus"));
			dojo.place(esClusterStatusDiv, dojo.byId("esClusterStatus"));
		},
		error : function(error) {
			targetNode.innerHTML = "An unexpected error occurred: " + error;
		}
	}

	deferred = dojo.xhrGet(xhrArgs);
}

function renderNodesStatus() {

	//Node List
	var nodeList;

	xhrArgs = {
		url : "/api/cluster/getNodesStatus/",
		handleAs : "json",
		load : function(data) {
			nodeList = data;
			var nodesTableHTML = "<table style='width:100%; font-size:11px; '> "
				+ "<tr ><th style='background: #F7F7F7; border-left:0px'><%= LanguageUtil.get(pageContext, "configuration_cluster_server_id") %></th>"
				+ "<th style='background: #F7F7F7'><%= LanguageUtil.get(pageContext, "configuration_cluster_ip_address") %></th>"
				+ "<th style='background: #F7F7F7'><%= LanguageUtil.get(pageContext, "configuration_cluster_contacted") %></th>"
				+ "<th style='background: #F7F7F7'><%= LanguageUtil.get(pageContext, "configuration_cluster_cache_status") %></th>"
				+ "<th style='background: #F7F7F7'><%= LanguageUtil.get(pageContext, "configuration_cluster_cache_port") %></th>"
				+ "<th style='background: #F7F7F7'><%= LanguageUtil.get(pageContext, "configuration_cluster_es_status") %></th>"
				+ "<th style='background: #F7F7F7; border-right:0px'><%= LanguageUtil.get(pageContext, "configuration_cluster_es_port") %></th>"
				+ "</tr>";

				dojo.forEach(nodeList, function(item, index){
					var cacheBg = (item.cacheStatus=='true'?"GREEN":item.cacheStatus=='false'?"RED":"");
					var esBg = (item.esStatus=='true'?"GREEN":item.esStatus=='false'?"RED":"");
					var cacheText = (item.cacheStatus=='N/A'?"<%= LanguageUtil.get(pageContext, "configuration_cluster_NA") %>":"");
					var esText = (item.cacheStatus=='N/A'?"<%= LanguageUtil.get(pageContext, "configuration_cluster_NA") %>":"");

					if(item.myself && cacheBg!='GREEN' || esBg!='GREEN') {
						dijit.byId("wireButton").setDisabled(false);
					} else {
						dijit.byId("wireButton").setLabel('<%= LanguageUtil.get(pageContext, "configuration_cluster_rewire_node") %>');
					}

					nodesTableHTML +=	"<tr><td style='vertical-align:middle; border-left:0px'><table class='listingTable2'><td width=3px'><span class='backupIcon' ></span></td>"
					+ "<td width='240px' style='text-align:left; '>" + item.serverId + "</td><td width='3px'>" + (item.myself?"<span class='femaleIcon'></span>":"")+"</td></table></td>"
					+ "<td align='left'>"+item.ipAddress+"</td>"
					+ "<td align='left'>"+item.contacted+" secs ago</td>"
					+ "<td align='center'><div style='background:"+cacheBg+"; width:20px;height:20px;'>"+cacheText+"</div></td>"
					+ "<td align='left'>"+(item.cachePort?item.cachePort:"<%= LanguageUtil.get(pageContext, "configuration_cluster_NA") %>") +"</td>"
					+ "<td align='center'><div style='background:"+esBg+"; width:20px;height:20px;'>"+esText+"</div></td>"
					+ "<td align='left' style='border-right:0px'>"+(item.esPort?item.esPort:"<%= LanguageUtil.get(pageContext, "configuration_cluster_NA") %>") +"</td>"
					+ "</tr>";
				});

				nodesTableHTML += "</table>"

				var nodesTable = dojo.create("div", { innerHTML: nodesTableHTML });

				dojo.empty(dojo.byId("nodes"));
				dojo.place(nodesTable, dojo.byId("nodes"));
				dojo.parser.parse("nodes");
		},
		error : function(error) {
			targetNode.innerHTML = "An unexpected error occurred: " + error;
		}
	}

	deferred = dojo.xhrGet(xhrArgs);

}


function refreshStatus() {
	renderCacheClusterStatus();
	renderESClusterStatus();
	renderNodesStatus();
}

function showClusterPropertiesDialog() {

	//ES Cluster Nodes Status
	var properties;

	xhrArgs = {
		url : "/api/cluster/getESConfigProperties/",
		handleAs : "json",
		load : function(data) {
			properties = data;
			var html = "<table style='width:100%'>";

			for(var key in properties){
				var value = properties[key];
				html += "<tr><td style='font-size:11px; width:50%'><span style='padding-left:15px; font-size:11px;'>"+key+"</span></td><td style='padding:5px'><input class='props' size='40' style='width: 100%; font-size:11px' type='text' data-dojo-type='dijit/form/TextBox' name='"+key+"' value='"+value+"'></input></td></tr>"
			};

			html += "</table>"

			dojo.empty(dojo.byId("propertiesDiv"));
			dojo.place(html, dojo.byId("propertiesDiv"))

			disableCustomProps();
			dijit.byId('clusterPropertiesDialog').show();
		},
		error : function(error) {
			targetNode.innerHTML = "An unexpected error occurred: " + error;
		}
	}

	deferred = dojo.xhrGet(xhrArgs);
	dijit.byId('dialogWireButton').setDisabled(false);
	dijit.byId("defaultPropsRadio").setChecked(true);
	dojo.byId("wiringResult").style.display = 'none';

}

function disableCustomProps() {
	var props = dojo.query(".props");

	dojo.forEach(props, function(entry, i){
		  entry.disabled = 'disabled';
		  entry.style = 'color:#d0d0d0'
		});
}

function enableCustomProps() {
	var props = dojo.query(".props");

	dojo.forEach(props, function(entry, i){
		  entry.disabled = '';
		  entry.style = 'color:#555555'
		});
}

function wireNode() {
	var json = "{}";
	var props = dojo.query(".props");

	var usingCustomProps = dojo.byId("customPropsRadio").checked;

	if(usingCustomProps) {
		json = "{";
		dojo.forEach(props, function(entry, i){
			  json += "'"+entry.name+"':'"+entry.value+"'";

			  if(i<props.length-1) {
				  json += ",";
			  }
		});

		json += "}";
	}

	xhrArgs = {
			url : "/api/cluster/wirenode/",
			handleAs : "json",
			postData: json,
			headers: { "Content-Type": "application/json"},
// 			sync: true,
			load : function(data) {
				dojo.byId("wiringResult").style.display = 'block';
				dojo.byId('wiringNode').style.display = 'none';
				dijit.byId('cancelWiringButton').setDisabled(false);

				if(data.result=="OK") {
					dojo.byId("wireResult").innerHTML = '<%= LanguageUtil.get(pageContext, "configuration_cluster_wiring_success") %>.';
					dojo.byId("wireResult").style = 'color:green';
				} else if(data.result.indexOf('ERROR')>-1) {
					dojo.byId("wireResult").innerHTML = data.result;
					dojo.byId("wireResult").style = 'color:red';
					dojo.byId("showErrorDetailButton").style.display = '';
					dijit.byId('dialogWireButton').setDisabled(false);
					dojo.byId("errorDetail").innerHTML = data.detail;
				}
			},
			error : function(error) {
// 				targetNode.innerHTML = "An unexpected error occurred: " + error;
			}
		}

	deferred = dojo.xhrPost(xhrArgs);
	dojo.byId('wiringNode').style.display = 'block';
	dijit.byId('dialogWireButton').setDisabled(true);
	dijit.byId('cancelWiringButton').setDisabled(true);

}

function closeDialog() {
	dijit.byId("clusterPropertiesDialog").hide();
	refreshStatus();
}



require(["dojo/ready"], function(ready){
	  ready(function(){
		  renderCacheClusterStatus();
		  renderESClusterStatus();
		  renderNodesStatus();
	  });
	});

function hideStatusBar() {
	dojo.byId("wiringResult").style.display = 'none';
}

function showErrorDetail(element) {
	dojo.byId('errorDetail').style.display = 'block';
	dojo.byId('hideErrorDetailButton').style.display='';
	element.style.display='none'
}

function hideErrorDetail(element) {
	dojo.byId('errorDetail').style.display = 'none'
	element.style.display = 'none'
	dojo.byId('showErrorDetailButton').style.display = ''
}

</script>

<table style="width: 100%" >
	<tr>
		<td width="30%" style="padding-left: 10px; padding-bottom: 10px">
			<span class="rServerIcon"></span>
        <span  style="line-height:20px;font-weight: bold;"><%= LanguageUtil.get(pageContext, "configuration_cluster_status") %></span>
		</td>
		<td width="30%" style="padding-bottom: 10px" align="center">


		</td>
		<td width="30%" align="right" style="padding-right: 10px; padding-bottom: 10px">
			<button  dojoType="dijit.form.Button" onClick="refreshStatus();" iconClass="resetIcon">
            <%= LanguageUtil.get(pageContext, "publisher_Refresh") %>
	        </button>
<!-- 	        <button dojoType="dijit.form.Button" onClick="showClusterPropertiesDialog();" iconClass="plusIcon" id="wireButton" disabled="disabled"> -->
	        <button dojoType="dijit.form.Button" onClick="showClusterPropertiesDialog();" iconClass="plusIcon" id="wireButton" >
	           <%= LanguageUtil.get(pageContext, "configuration_cluster_wire_node") %>
	        </button>
		</td>
	</tr>
   </table>

<table id="cacheCluster" class="listingTable shadowBox">
    <tr>
        <th style="font-size: 8pt;" width="20%"><%= LanguageUtil.get(pageContext, "configuration_cluster_general_status") %></th>
        <th style="font-size: 8pt;text-align: center"><%= LanguageUtil.get(pageContext, "configuration_cluster_servers") %></th>
    </tr>
    <tr style="text-align: center">
        <td width="25%" style="padding:0px">
        	<div id='generalStatus'>
	        	<div id='cacheClusterStatus'></div>
	        	<hr>
	        	<div id='esClusterStatus'></div>
        	</div>
        </td>
        <td valign="top" id="nodesTD" style="padding: 0;">
        	<div id='nodes' style="width: 100%">
        	</div>
        </td>
    </tr>
</table><br>



<div id="clusterPropertiesDialog" dojoType="dijit.Dialog" disableCloseButton="true" title="<%=LanguageUtil.get(pageContext, "configuration_Cluster_Edit_Config")%>"
	style="display: none; width:550px">
    <div style="padding:0px 15px;">
<!--       <form action="/api/cluster/wirenode/" id="propertiesForm" method="post"> -->
      		<div style="padding-top: 10px; padding-bottom: 10px; text-align: center; font-size: 12px">
				<input type="radio" dojoType="dijit.form.RadioButton" onChange="enableCustomProps()" value=""
					name="propsRadio" id="defaultPropsRadio" checked="checked"><label for="defaultPropsRadio"><%= LanguageUtil.get(pageContext, "configuration_cluster_use_default_properties") %></label>&nbsp;
				<input type="radio" dojoType="dijit.form.RadioButton" onChange="disableCustomProps()" value=""
					name="propsRadio" id="customPropsRadio" ><label for="customPropsRadio"><%= LanguageUtil.get(pageContext, "configuration_cluster_use_custom_properties") %></label>
			</div>
			<div id="wiringResult" style="display: none; text-align: center; font-size:11px; padding-bottom: 10px">
				<span id="wireResult"></span>
				<a href='#' onclick="showErrorDetail(this)" id="showErrorDetailButton" ><%=LanguageUtil.get(pageContext, "show")%> <%=LanguageUtil.get(pageContext, "details")%></a>
				<a href='#' onclick="hideErrorDetail(this)" id="hideErrorDetailButton" style="display: none"><%=LanguageUtil.get(pageContext, "hide")%> <%=LanguageUtil.get(pageContext, "details")%></a>
<!-- 				<a href='#' onclick="hideStatusBar()">Clear</a> -->
				<div id="errorDetail" style="display: none; text-align: center; width: 100%; font-size:11px"></div>
			</div>
			<div id="wiringNode" style="display: none; text-align: center; width: 100%; font-size:11px; padding-bottom: 10px">::: Wiring Node :::</div>

            <div id='propertiesDiv' style="height: 170px;overflow: scroll; border:1px solid #d0d0d0;"></div>
            <div align="center" style="padding-top: 10px">
               <button style="padding-bottom: 10px;" dojoType="dijit.form.Button"
					iconClass="saveIcon" onclick="wireNode()" id="dialogWireButton"
					type="button"><%=LanguageUtil.get(pageContext, "Wire")%>
				</button>
				<button style="padding-bottom: 10px;" dojoType="dijit.form.Button"
					onClick='closeDialog()' iconClass="cancelIcon" id="cancelWiringButton"
					type="button"><%=LanguageUtil.get(pageContext, "Cancel")%>
				</button>
			</div>
<!--         </form> -->
    </div>
</div>

