<%@page import="com.dotmarketing.util.Config"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%String dojoPath = Config.getStringProperty("path.to.dojo");%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
<link rel="shortcut icon" href="//www.dotcms.com/global/favicon.ico" type="image/x-icon">

	<title>dotCMS Style Guide</title>
	<style media="all" type="text/css">
		@import "/html/common/css.jsp";
        @import "<%=dojoPath%>/dijit/themes/dmundra/dmundra.css";
	</style>

	<script type="text/javascript" src="<%=dojoPath%>/dojo/dojo.js" djConfig="parseOnLoad:true, isDebug:false"></script>
	<script type="text/javascript" src="<%=dojoPath%>/dojo/dot-dojo.js"></script>
	<script type="text/javascript">
		dojo.require("dijit.form.Button");
		dojo.require('dijit.layout.TabContainer');
		dojo.require('dijit.layout.ContentPane');
		dojo.require('dijit.form.FilteringSelect');
		dojo.require('dojo.data.ItemFileReadStore');
		dojo.require("dojo.fx");
		dojo.require("dijit.layout.SplitContainer");
		dojo.require("dijit.ColorPalette");
		dojo.require("dijit.form.Slider");
		dojo.require("dijit.Dialog");
		dojo.require("dijit.ProgressBar");
		dojo.require("dijit.form.ComboBox");
		dojo.require("dijit.Dialog");
		dojo.require("dijit.form.Button");
		dojo.require("dijit.form.CheckBox");
		dojo.require("dijit.form.DateTextBox");
		dojo.require("dijit.form.FilteringSelect");
		dojo.require("dijit.form.TextBox");
		dojo.require("dijit.form.ValidationTextBox");
		dojo.require("dijit.form.Textarea");
		dojo.require("dijit.Menu");
		dojo.require("dijit.MenuItem");
		dojo.require("dijit.MenuSeparator");
		dojo.require("dijit.ProgressBar");
		dojo.require("dijit.PopupMenuItem");
		dojo.require('dijit.layout.TabContainer');
		dojo.require('dijit.layout.ContentPane');
		dojo.require("dijit.layout.BorderContainer");
		dojo.require("dijit.TitlePane");
		dojo.require("dijit.Tooltip");
		dojo.require("dojo.parser");
		dojo.require("dojo.fx");
		dojo.require("dojox.layout.ContentPane");
		dojo.require("dojo.window")
		dojo.require("dojo/request")
		dojo.require("dojo/request/xhr")
	</script>

	<script>

		var actionPanelTable;
		var initialRow;
		var myServerId;

		/**
			ActionPanel JS Object
		**/
		dojo.declare("com.dotcms.ui.ActionPanel", null, {
			lastRow:undefined,
			jspToShow:undefined,
			ff : 0,
			constructor: function(/* Object */args){

				this.alwaysShow = args.alwaysShow;
				this.jspToShow = args.jspToShow;

				if(this.alwaysShow){
					dojo.ready(function(){
						if( dojo.byId("row-"+initialRow) != undefined){
							actionPanelTable.toggle(initialRow);
						}
					})
				}

				if(dojo.isMozilla) {
					this.ff=-1;
				}

				/**
					Handle scolling and resize
				**/
				dojo.connect(window, 'onscroll', this, function(event) {
					this.placeActionPanel();

				});
				dojo.connect(window, 'onresize', this, function(event) {
					this.initPanel();
				});



			},


			/** Setting this true will show the first row automatically **/
			alwaysShow:false,

			/**
				hide/show action Panel
			**/
			toggle:function(row){
				if(this.jspToShow == undefined){
					return;
				}

				dojo.addClass("actionPanel", "hideMe");
				dojo.destroy("display-arrow");

				// deactivate last clicked row,
				if(this.lastRow != undefined){
					dojo.removeClass('row-' + this.lastRow, "active");
					if( ! this.alwaysShow){
						if(this.lastRow == row){
							this.lastRow = null;
							return;
						}
					}
				}

				dojo.addClass('row-' + row, "active");
				this.lastRow=row;

				// Build the action Panel
				var panelDiv = dojo.byId("actionPanelContainer");
				var hanger = dojo.byId("actionPanelContent");
				if(!hanger){
					return;
				}
				if (panelDiv) {
					dojo.destroy("panelDiv")
				}


				panelDiv = dojo.create("div", {
					id : "actionPanelContainer"
					},"actionPanelContent");


				var nodeStatus;

				xhrArgs = {
					url : "/api/cluster/getNodeStatus/id/"+row,
					handleAs : "json",
					sync: true,
					load : function(data) {
						nodeStatus = data;
					},
					error : function(error) {
						targetNode.innerHTML = "An unexpected error occurred: " + error;
					}
				}

				deferred = dojo.xhrGet(xhrArgs);

				var canRewire = row==myServerId;

		        // Execute a HTTP GET request
		        dojo.xhr.get({
		        	preventCache:true,
		            url: this.jspToShow+'?rewireable='+canRewire,
		            load: function(result) {
		            	var output = '';

		            	require(["dojo/_base/lang"], function(lang){
		            		  output = lang.replace(
		            		    result,
		            		    {
		            		      cache: {
		            		        status:  nodeStatus.cacheStatus,
		            		        clusterName:  nodeStatus.cacheClusterName,
		            		        numberOfNodes: nodeStatus.cacheNumberOfNodes,
		            		        open:   nodeStatus.cacheOpen,
		            		        address:   nodeStatus.cacheAddress,
		            		        receivedBytes:   nodeStatus.cacheReceivedBytes,
		            		        port:   nodeStatus.cachePort,
		            		      },

		            		      es: {
		            		    	status: nodeStatus.esStatus,
		            		    	clusterName: nodeStatus.esClusterName,
		            		    	numberOfNodes: nodeStatus.esNumberOfNodes,
		            		    	activeShards: nodeStatus.esActiveShards,
		            		    	port: nodeStatus.esPort,
		            		      },

		            		      assets: {
		            		    	  status: nodeStatus.assetsStatus,
		            		    	  canRead: nodeStatus.assetsCanRead,
		            		    	  canWrite: nodeStatus.assetsCanWrite,
		            		    	  path: nodeStatus.assetsPath,
		            		      }

		            		    }
		            		  );
		            		});

		            	dojo.byId("actionPanelContainer").innerHTML = output;
		            }
		        });




				actionPanelTable.initPanel();




				dojo.removeClass("actionPanel", "hideMe");

				//dojo.parser.parse("actionPanel");
				dojo.style("actionPanel", "width", dojo.position("actionPanelTableHeader",true).w -1 +this.ff + "px");

			},

			/**
				Move actionpanel/arrow around - does not hide/show
			**/
			placeActionPanel:function(){

				if(this.lastRow == undefined){
					return;
				}

				var selectedRow = dojo.position('row-' + this.lastRow, true);
				var tableHeader = dojo.position("actionPanelTableHeader",true);
				var actionPanel = dojo.position("actionPanel", true);
				var scroll = window.pageYOffset ? window.pageYOffset : document.documentElement.scrollTop ? document.documentElement.scrollTop : document.body.scrollTop;
				var bottomOfTheHeader = tableHeader.h + this.ff + tableHeader.y -scroll;

				/*
				console.log("--------------------------");
				console.log("tableHeader x:" + tableHeader.x);
				console.log("tableHeader y:" + tableHeader.y);
				console.log("tableHeader h:" + tableHeader.h);
				console.log("tableHeader w:" + tableHeader.w);
				console.log("selectedRow:" + this.lastRow);
				console.log("bottomOfTheHeader x:" + bottomOfTheHeader);
				console.log("selectedRow y:" + selectedRow.y);
				console.log("selectedRow h:" + selectedRow.h);
				console.log("actionPanel y:" + actionPanel.y);
				console.log("actionPanel x:" + actionPanel.x);
				console.log("Scroll:" +scroll);
				*/


				if(bottomOfTheHeader < 0 ){
					dojo.style("actionPanel", "position","fixed");
					dojo.style("actionPanel", "top", "0px");
					dojo.style("actionPanel", "left", tableHeader.x + "px");
					var arrowY = 	selectedRow.y - scroll +13  ;
					dojo.style("arrow", "top", arrowY + "px");

					return;
				}
				else{
					dojo.style("actionPanel", "top", bottomOfTheHeader + "px");
					dojo.style("actionPanel", "left", tableHeader.x + "px");
					var arrowY = 	selectedRow.y -  (Math.abs( bottomOfTheHeader) )  - scroll + 13;
					dojo.style("arrow", "top", arrowY + "px");
				}
			},



			initPanel : function(){
				var tableHeader = dojo.position("actionPanelTableHeader",true);
				var scroll = window.pageYOffset ? window.pageYOffset : document.documentElement.scrollTop ? document.documentElement.scrollTop : document.body.scrollTop;
				var bottomOfTheHeader = tableHeader.h + tableHeader.y -scroll;

				dojo.style("actionPanel", "width", tableHeader.w -1 + "px");

				require(["dojo/window"], function(win){
					   var vs = win.getBox();
						dojo.style("actionPanelContent", "height", vs.h -bottomOfTheHeader  + "px");
					});

				actionPanelTable.placeActionPanel();


			}
		});

		function renderNodesStatus() {

			//Node List
			var nodeList;

			xhrArgs = {
				url : "/api/cluster/getNodesStatus/",
				handleAs : "json",
				sync: true,
				load : function(data) {
					nodeList = data;
					var nodesTableHTML = "<table class='listingTable actionTable'> "
						+ "<tr>"
							+ "<th width='7%'>&nbsp;</th>"
							+ "<th width='7%'>&nbsp;</th>"
							+ "<th width='35%'><%= LanguageUtil.get(pageContext, "configuration_cluster_host") %></th>"
							+ "<th width='25%'><%= LanguageUtil.get(pageContext, "configuration_cluster_ip_address") %></th>"
							+ "<th width='20%'><%= LanguageUtil.get(pageContext, "configuration_cluster_contacted") %></th>"
							+ "<th width='6%' style='text-align:center;'><%= LanguageUtil.get(pageContext, "status") %></th>"
							<!-- /Add these up to 100% -->

							<%-- the width of the inner div is the only thing you have to set to change the width --%>
							+ "<th id='actionPanelTableHeader'><div style='width:400px;'></div></th>"
						+ "</tr>";

						dojo.forEach(nodeList, function(item, index){

							if(index==0) {
								initialRow = item.serverId;
							}

							if(item.myself) {
								myServerId = item.serverId;
							}

							nodesTableHTML += ""
							+ "<tr id='row-"+item.serverId+"' onclick='javascript:actionPanelTable.toggle(\""+item.serverId+"\");'>"
								+ "<td align='center'><img src='/html/images/skin/icon-server.png'></td>"
								+ "<td align='center' style='color:#8c9ca9;'>" + (item.myself?"<i class='fa fa-user fa-3x'></i>":"")+"</td>"
								+ "<td>" + item.friendlyName + "</td>"
								+ "<td align='left'>"+item.ipAddress+"</td>"
								+ "<td align='left'>"+item.contacted+"</td>"
								+ "<td align='left'><i class='fa fa-circle fa-2x "+item.status+"'></i></td>"
								+ "<td id='td-"+index+"'></td>"
							+ "</tr>";

							nodesTableHTML += "<input type='hidden' id='serverId-"+index+"' value='"+item.serverId+"'>"
						});

						nodesTableHTML += "</table>"

						var nodesTable = dojo.create("div", { innerHTML: nodesTableHTML });

						dojo.empty(dojo.byId("container"));
						dojo.place(nodesTable, dojo.byId("container"));

// 						<div id="actionPanel" class="hideMe" >
// 						<div id="arrow"></div>
// 						<div id="actionPanelContent" style="overflow:auto;">

// 						</div>
// 					</div>

						var actionPanelDiv = dojo.create("div", { id: "actionPanel", class: "hideMe" });
						var arrowDiv = dojo.create("div", { id: "arrow", innerHtml: "<img src='/html/images/skin/arrow.png'/>" });
						var actionPanelContentDiv = dojo.create("div", { id: "actionPanelContent", style: "overflow:auto" });

						dojo.place(arrowDiv, actionPanelDiv);
						dojo.place(actionPanelContentDiv, actionPanelDiv);
						dojo.place(actionPanelDiv, dojo.byId("container"));

				},
				error : function(error) {
					targetNode.innerHTML = "An unexpected error occurred: " + error;
				}
			}

			deferred = dojo.xhrGet(xhrArgs);

		}

		require(["dojo/ready"], function(ready){
			  ready(function(){

				  renderNodesStatus();

// 				 /**
// 					Set the jsp to load up when the action panel is activated
// 				 **/

				  actionPanelTable = new com.dotcms.ui.ActionPanel({jspToShow:"/html/portlet/ext/cmsconfig/cluster_config_right_panel.jsp", alwaysShow:true});

			  });
		});


		function showClusterPropertiesDialog() {

			//ES Cluster Nodes Status
			var properties;


			xhrArgs = {
				url : "/api/cluster/getESConfigProperties/",
				handleAs : "json",
				load : function(data) {
					properties = data;

					dojo.destroy("propsTable");

					var html = "<table style='width:100%' id='propsTable'>";

					for(var key in properties){
						var value = properties[key];

						  var checkbox = dijit.byId("use_"+key);
						  var input = dijit.byId(key);

						  if(checkbox) {
							  checkbox.destroyRecursive(false);
						  }

						  if(input) {
							  input.destroyRecursive(false);
						  }

						html += "<tr><td><input type='checkbox' data-dojo-type='dijit/form/CheckBox' id='use_"+key+"' onChange='enableDisableProp(\""+key+"\")'></td><td style='font-size:11px; width:50%' ><span style='padding-left:15px; color:#d0d0d0; font-size:11px;' id='label_"+key+"'>"+key+"</span></td><td style='padding:5px; text-align:center'><input class='props' style='font-size:11px; width:100px' type='text' data-dojo-type='dijit/form/TextBox' disabled='disabled'  id='"+key+"' value='"+value+"'></input></td></tr>"
					};

					html += "</table>"

					dojo.empty(dojo.byId("propertiesDiv"));
					dojo.place(html, dojo.byId("propertiesDiv"))
					dojo.parser.parse("propertiesDiv");

					dijit.byId('clusterPropertiesDialog').show();
				},
				error : function(error) {
					targetNode.innerHTML = "An unexpected error occurred: " + error;
				}
			}

			deferred = dojo.xhrGet(xhrArgs);
			dijit.byId('dialogWireButton').setDisabled(false);
//		 	dijit.byId("defaultPropsRadio").setChecked(true);
			dojo.byId("wiringResult").style.display = 'none';

		}

		function enableDisableProp(key) {

			if(dijit.byId(key).get("disabled")) {
				dijit.byId(key).set("disabled", false);
				dojo.byId("label_"+key).style.color = '#555';
			} else {
				dijit.byId(key).set("disabled", true);
				dojo.byId("label_"+key).style.color = '#d0d0d0';
			}
		}

		function wireNode() {
			var json = "{}";
			var props = dojo.query(".props");

			json = "{";
			dojo.forEach(props, function(entry, i){

				  var realId = entry.id.substring(7, entry.id.length);
				  var widget = dijit.byId(realId);

				  if(!widget.get("disabled")) {
				  	json += "'"+widget.get("id")+"':'"+widget.get("value")+"'";
				  }

				  if(i<props.length-1 && json!="{") {
					  json += ",";
				  }
			});

			json += "}";


			xhrArgs = {
					url : "/api/cluster/wirenode/",
					handleAs : "json",
					postData: json,
					headers: { "Content-Type": "application/json"},
//		 			sync: true,
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
						targetNode.innerHTML = "An unexpected error occurred: " + error;
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

		function refreshStatus(serverId) {
			 renderNodesStatus();
			 actionPanelTable = new com.dotcms.ui.ActionPanel({jspToShow:"/html/portlet/ext/cmsconfig/cluster_config_right_panel.jsp", alwaysShow:true});
		}

	</script>






</head>

<body class="dmundra">


<div id="doc3">


	<div class="actionPannelPage" id="container" style="min-height: 400px">

		<div id="actionPanel" class="hideMe" >
			<div id="arrow"><img src='/html/images/skin/arrow.png'/></div>
			<div id="actionPanelContent" style="overflow:auto;">

			</div>
		</div>

	</div>

</div>

<div id="clusterPropertiesDialog" dojoType="dijit.Dialog" disableCloseButton="true" title="<%=LanguageUtil.get(pageContext, "configuration_cluster_wire_node")%>"
	style="display: none; width:350px">
    <div style="padding:0px 15px;">
			<div id="wiringResult" style="display: none; text-align: center; font-size:11px; padding-bottom: 10px">
				<span id="wireResult"></span>
				<a href='#' onclick="showErrorDetail(this)" id="showErrorDetailButton" style="display: none"><%=LanguageUtil.get(pageContext, "show")%> <%=LanguageUtil.get(pageContext, "details")%></a>
				<a href='#' onclick="hideErrorDetail(this)" id="hideErrorDetailButton" style="display: none"><%=LanguageUtil.get(pageContext, "hide")%> <%=LanguageUtil.get(pageContext, "details")%></a>
<!-- 				<a href='#' onclick="hideStatusBar()">Clear</a> -->
				<div id="errorDetail" style="display: none; text-align: center; width: 100%; font-size:11px"></div>
			</div>
			<div id="wiringNode" style="display: none; text-align: center; width: 100%; font-size:11px; padding-bottom: 10px">::: Wiring Node :::</div>

            <div id='propertiesDiv' style="height: auto;"></div>
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
    </div>
</div>

</body>
</html>