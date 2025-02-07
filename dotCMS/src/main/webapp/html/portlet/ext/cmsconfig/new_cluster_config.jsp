<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotcms.enterprise.LicenseUtil" %>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
<%@ page import="com.dotmarketing.util.Config" %>
<%@ include file="/html/common/uservalidation.jsp"%>

<style media="all" type="text/css">
    .error-detail {
        width: 100%;
        font-size: 11px;
        margin: 10px 0;
        text-align: left;
        overflow: scroll;
        max-height: 300px;
    }
    
	.loader {
	    width: 30px;
	    height: 30px;
	    margin:auto;
	    animation: spin 2s linear infinite;
	}
	
	@keyframes spin {
	    0% { transform: rotate(0deg); }
	    100% { transform: rotate(360deg); }
	}
    
</style>

<script type="text/javascript">

        var actionPanelTable;
        dojo.require("dojox.widget.Standby");

        /**
            ActionPanel JS Object
        **/
        dojo.declare("com.dotcms.ui.ActionPanel", null, {

            jspToShow:"/html/portlet/ext/clusterconfig/cluster_config_right_panel.jsp",
            nodeData:undefined,

            actionPanelHtml:undefined,
            myServerId : "<%=APILocator.getServerAPI().readServerId()%>",
            ff : 0,
            targetNode:undefined,
            constructor: function(args){
                this.targetNode = dojo.byId("actionPanel");

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
                this.drawNodeTable();
            },


            
            
            
            /**
                hide/show action Panel
            **/
            toggle:function(row){
            	
            	
            	
            	
				var nodeStatus;
				if(row==undefined){
					row = this.myServerId;
				}
			
                
   				if(this.nodeData != undefined){
	                for(i=0;i<this.nodeData.serverInfo.length;i++){
	                	if(row==this.nodeData.serverInfo[i].serverId){
	                		nodeStatus= this.nodeData.serverInfo[i];
	                	}
	                }
   				}
                //console.log(nodeStatus)
                dojo.addClass("actionPanel", "");
                dojo.destroy("display-arrow");

                // deactivate last clicked row,
                if(this.lastRow != undefined){
                    dojo.removeClass('row-' + this.lastRow, "active");
                }
				if(dojo.byId('row-' + row)!=undefined){
	                dojo.addClass('row-' + row, "active");
	                this.lastRow=row;
				}
				// Set Container Height
				var x = window.innerHeight - 140;
				dojo.style("container", "height", x + "px");


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
                    id : "actionPanelContainer",
                    height: dojo.style(dojo.byId(hanger,"height"),"height")
                    },"actionPanelContent");





				var result = actionPanelTable.actionPanelHtml;
                var output = '';
				if(this.nodeData != undefined  && this.actionPanelHtml!=undefined){
				const clusterHealth = this.nodeData.clusterHealth;

                require(["dojo/_base/lang"], function(lang){
                      output = lang.replace(
                        result,
                        {
                          cache: {
                        	  
                            status:  clusterHealth,
                            clusterName:  nodeStatus.clusterName,
                            numberOfNodes: nodeStatus.numberOfNodes,
                            open:   nodeStatus.open,
                            address:   nodeStatus.ipAddress,
                            receivedBytes:   nodeStatus.receivedBytes,
                            port:   nodeStatus.port,
                            cacheTransportClass: nodeStatus.cacheTransportClass
                          },

                          es: {
                            status: nodeStatus.esStatus,
                            clusterName: nodeStatus.esClusterName,
                            timedout: nodeStatus.esTimedout,
                            numberOfNodes: nodeStatus.esNumberOfNodes,
                            numberOfDataNodes: nodeStatus.esNumberOfDataNodes,
                            activePrimaryShards: nodeStatus.esActivePrimaryShards,
                            activeShards: nodeStatus.esActiveShards,
                              relocatingShards: nodeStatus.esRelocatingShards,
                              initializingShards: nodeStatus.esInitializingShards,
                              unassignedShards: nodeStatus.esUnassignedShards,
                              delayedUnasignedShards: nodeStatus.esDelayedUnasignedShards,
                              numberOfPendingTasks: nodeStatus.esNumberOfPendingTasks,
                              numberOfInFlightFetch: nodeStatus.esNumberOfInFlightFetch,
                              taskMaxWaitingInQueueMillis: nodeStatus.esTaskMaxWaitingInQueueMillis,
                              activeShardsPercentAsNumber: nodeStatus.esActiveShardsPercentAsNumber,
                          },

                          assets: {
                              status: nodeStatus.assetsStatus,
                              canRead: nodeStatus.assetsCanRead,
                              canWrite: nodeStatus.assetsCanWrite,
                              path: nodeStatus.assetsPath
                          },


                          server: {
                              serverID: nodeStatus.serverId.substring(0,8),
                              displayServerId : nodeStatus.displayServerId,
                              host: nodeStatus.friendlyName
                          }

                        }
                      );
                    });
				}
				
                dojo.byId("actionPanelContainer").innerHTML = output;
                actionPanelTable.initPanel();
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

                if(bottomOfTheHeader < 0 ){
                    dojo.style("actionPanel", "position","fixed");
                    dojo.style("actionPanel", "top", "0px");
                    dojo.style("actionPanel", "left", tableHeader.x + "px");
                    var arrowY =    selectedRow.y - scroll +13  ;
                    dojo.style("arrow", "top", arrowY + "px");

                    return;
                }
                else{
                    dojo.style("actionPanel", "top", bottomOfTheHeader + "px");
                    dojo.style("actionPanel", "left", tableHeader.x + "px");
                    var arrowY =    selectedRow.y -  (Math.abs( bottomOfTheHeader) )  - scroll + 13;
                    dojo.style("arrow", "top", arrowY + "px");
                }
            },

            initPanel : function(){
                var tableHeader = dojo.position("actionPanelTableHeader",true);
                var panelPage = dojo.position(document.getElementById("actionPanel").parentNode,true);
                var panelMinHeight = document.getElementById("actionPanel").parentNode.style.minHeight;
                dojo.style("actionPanel", "width", tableHeader.w -1 + "px");
                require(["dojo/window"], function(win){
                    dojo.style("actionPanelContent", "height", panelPage.h + "px");
                });
                actionPanelTable.placeActionPanel();
            },
            
            refreshData : function(){
            	this.loadNodeData();
            	this.loadActionPanelHtml();
            },

            
            loadNodeData : function() {
                xhrArgs = {
                    url : "/api/cluster/getNodesStatus/",
                    handleAs : "json",
                    sync: false,
                    load : function(data) {
                        if (data != undefined && data.serverInfo  != undefined && data.serverInfo.length > 0) {
                            actionPanelTable.nodeData = data;
                            actionPanelTable.drawNodeTable();
                        } else {
                            dojo.removeClass('loader', 'loader');
                            dojo.byId('loader').innerHTML = '<%= LanguageUtil.get(pageContext, "configuration_cluster_no_nodes_found") %>';
                        }
						actionPanelTable.toggle();
                    },
                    error : function(error) {
                    	actionPanel.innerHTML = "An unexpected error occurred: " + error;
                    }
                };
                dojo.xhrGet(xhrArgs);
            },
            


            loadActionPanelHtml : function(){
                // Execute a HTTP GET request
                dojo.xhr.get({
                    preventCache:true,
                    url: this.jspToShow+'?rewireable='+false,
                    load: function(result) {
                    	actionPanelTable.actionPanelHtml=result;
                    	actionPanelTable.toggle();
                    }
                });
            },
            
            drawNodeTable : function() {

                var nodesTableHTML = "<table class='listingTable actionTable network__listing'> "
                    + "<tr>"
                        + "<th width='7%'>&nbsp;</th>"
                        + "<th width='7%'>&nbsp;</th>"
                        + "<th width='15%'><%= LanguageUtil.get(pageContext, "configuration_cluster_server_id") %></th>"
                        + "<th width='15%'><%= LanguageUtil.get(pageContext, "version") %></th>"
                        + "<th width='20%'><%= LanguageUtil.get(pageContext, "configuration_cluster_host") %></th>"
                        + "<th width='10%'><%= LanguageUtil.get(pageContext, "configuration_cluster_ip_address") %></th>"
                        + "<th width='10%'><%= LanguageUtil.get(pageContext, "configuration_cluster_contacted") %></th>"
                        + "<th width='6%' style='text-align:center;'><%= LanguageUtil.get(pageContext, "status") %></th>"

                        <!-- /Add these up to 100% -->

                        <%-- the width of the inner div is the only thing you have to set to change the width --%>
                        + "<th id='actionPanelTableHeader' network-action__header><div style='width:400px;'></div></th>"
                    + "</tr>";

                    
                    
                    nodeList = this.nodeData == undefined ? [] : this.nodeData.serverInfo;
                	



                    if(nodeList.length ==0){
                        nodesTableHTML += "<tr><td colspan='8' id='row-data' style=''><div class='loader' id='loader'></div></td></tr>";
                    }
                    
                    
                    dojo.forEach(nodeList, function(item, index){

                        if(index==0) {
                            initialRow = item.serverId;
                        }
						




                        nodesTableHTML += ""
                        + "<tr id='row-"+item.serverId+"' onclick='javascript:actionPanelTable.toggle(\""+item.serverId+"\");'>"
                            + "<td align='center'><img src='/html/images/skin/icon-server.png' class='icon network__listing-icon' /></td>"
                            + "<td align='center' style='color:#8c9ca9;'>" + (item.serverId==actionPanelTable.myServerId?"<i class='userIcon'></i>":"")+"</td>"
                            + "<td>" + item.licenseId + "</td>"
                            + "<td>" + item.key + "</td>"
                            + "<td>" + item.friendlyName + "</td>"
                            + "<td align='left'>"+item.ipAddress+"</td>"
                            + "<td align='left'>"+item.contacted+"</td>"
                            + "<td align='center'><i class='statusIcon " + item.status + "'></i></td>"
                            + "<td id='td-"+index+"'></td>"
                        + "</tr>";

                        nodesTableHTML += "<input type='hidden' id='serverId-"+index+"' value='"+item.serverId+"'>"
                    });

                    nodesTableHTML += "</table>";

                    var nodesTable = dojo.create("div", { innerHTML: nodesTableHTML });

                    dojo.empty(dojo.byId("container"));
                    dojo.place(nodesTable, dojo.byId("container"));

                    var actionPanelDiv = dojo.create("div", { id: "actionPanel", class: "network-action " });
                    var arrowDiv = dojo.create("div", { id: "arrow", innerHtml: "<img src='/html/images/skin/arrow.png'/>" });
                    var actionPanelContentDiv = dojo.create("div", { id: "actionPanelContent", style: "overflow:auto" });

                    dojo.place(arrowDiv, actionPanelDiv);
                    dojo.place(actionPanelContentDiv, actionPanelDiv);
                    dojo.place(actionPanelDiv, dojo.byId("container"));

            }
        });


        
        
        require(["dojo/ready"], function(ready){
              ready(function(){

                 /**
                    Set the jsp to load up when the action panel is activated
                 **/
                  actionPanelTable = new com.dotcms.ui.ActionPanel();
          
                  actionPanelTable.refreshData();
                 	
                 
              });
              
              
        });




        /*
            Displays the error message sent from the server
         */
        var showErrorDetail = function () {
            dojo.byId("showErrorDetailButton").hide();
            dojo.byId("hideErrorDetailButton").show();
            dojo.byId("errorDetail").show();
        };

        /*
            Hiddes the error message sent from the server
         */
        var hideErrorDetail = function () {
            dojo.byId("hideErrorDetailButton").hide();
            dojo.byId("showErrorDetailButton").show();
            dojo.byId("errorDetail").hide()
        };


        
</script>

<div id="myCacheDialog" dojoType="dijit.Dialog" draggable="false" style="display:none" title="Servers Not in Cache">
    <div id="myCacheDialogName"></div>
</div>

<div id="container">
    <div id="actionPanel" class="network-action">
        <div id="arrow"><img src='/html/images/skin/arrow.png'/></div>
        <div id="actionPanelContent" style="overflow:auto;"></div>
    </div>
</div>
