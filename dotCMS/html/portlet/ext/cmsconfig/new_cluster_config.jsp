<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotcms.enterprise.LicenseUtil" %>
<%@ page import="com.dotmarketing.util.Config" %>

<%  if(!Config.getBooleanProperty("ENABLE_SERVER_HEARTBEAT", true)) { //In case user tries to acess jsp directly%>
<%@ include file="/html/portlet/ext/cmsconfig/network/not_licensed.jsp" %>
<%
        return;
    }
%>

<%  if( LicenseUtil.getLevel()<300){ %>
    <%@ include file="/html/portlet/ext/cmsconfig/network/not_licensed.jsp" %>
<%
        return;
    }
%>

<style media="all" type="text/css">
    .error-detail {
        width: 100%;
        font-size: 11px;
        margin: 10px 0;
        text-align: left;
        overflow: scroll;
        max-height: 300px;
    }
</style>

<script type="text/javascript">

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
                    id : "actionPanelContainer",
                    height: dojo.style(dojo.byId(hanger,"height"),"height")
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
                };

                deferred = dojo.xhrGet(xhrArgs);

                var licenseStatus;

                licxhr = {
                    url : "/api/cluster/licenseRepoStatus",
                    handleAs : "json",
                    sync: true,
                    load : function(data) {
                        licenseStatus = data;
                    },
                    error : function(error) {
                        targetNode.innerHTML = "An unexpected error occurred: " + error;
                    }
                };

                dojo.xhrGet(licxhr);

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
                                    port:   nodeStatus.cachePort
                                  },

                                  es: {
                                    status: nodeStatus.esStatus,
                                    clusterName: nodeStatus.esClusterName,
                                    numberOfNodes: nodeStatus.esNumberOfNodes,
                                    activeShards: nodeStatus.esActiveShards,
                                    activePrimaryShards: nodeStatus.esActivePrimaryShards,
                                    port: nodeStatus.esPort
                                  },

                                  assets: {
                                      status: nodeStatus.assetsStatus,
                                      canRead: nodeStatus.assetsCanRead,
                                      canWrite: nodeStatus.assetsCanWrite,
                                      path: nodeStatus.assetsPath
                                  },

                                  licenseRepo: {
                                      total: licenseStatus.total,
                                      available: licenseStatus.available
                                  },

                                  server: {
                                      serverID: nodeStatus.serverId.substring(0,8),
                                      displayServerId : nodeStatus.displayServerId,
                                      host: nodeStatus.friendlyName
                                  }

                                }
                              );
                            });

                        dojo.byId("actionPanelContainer").innerHTML = output;
                        if(nodeStatus.cacheServersNotResponded){
                            dojo.byId("myCacheDialogName").innerHTML = nodeStatus.cacheServersNotResponded;

                            var myShowCacheButton = dojo.byId("myShowCacheButton");
                            dojo.connect(myShowCacheButton, "onclick", function(evt){
                                var myCacheDialog = dijit.byId("myCacheDialog");
                                myCacheDialog.show();
                            });
                        }
                        
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
                if(panelPage.h < panelMinHeight){
                    dojo.style("actionPanel", "height", panelMinHeight - tableHeader.h + "px");
                }
                else{
                    dojo.style("actionPanel", "height", panelPage.h - tableHeader.h + "px");
                }
                require(["dojo/window"], function(win){
                       var vs = win.getBox();
                        dojo.style("actionPanelContent", "height", panelPage.h - tableHeader.h + "px");
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
                            + "<th width='25%'><%= LanguageUtil.get(pageContext, "configuration_cluster_server_id") %></th>"
                            + "<th width='25%'><%= LanguageUtil.get(pageContext, "configuration_cluster_host") %></th>"
                            + "<th width='10%'><%= LanguageUtil.get(pageContext, "configuration_cluster_ip_address") %></th>"
                            + "<th width='10%'><%= LanguageUtil.get(pageContext, "configuration_cluster_contacted") %></th>"
                            + "<th width='6%' style='text-align:center;'><%= LanguageUtil.get(pageContext, "status") %></th>"
                            + "<th width='10%' style='text-align:center;'><%= LanguageUtil.get(pageContext, "configuration_cluster_delete") %></th>"
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

                            var deleteServer = "<td align='center'></td>";
                            
                            if(item.heartbeat && item.heartbeat == "false"){
                                deleteServer = "<td align='center'><img onclick='removeFromCluster(\""+item.serverId+"\");' src='/html/images/icons/cross.png'></td>";
                            }

                            nodesTableHTML += ""
                            + "<tr id='row-"+item.serverId+"' onclick='javascript:actionPanelTable.toggle(\""+item.serverId+"\");'>"
                                + "<td align='center'><img src='/html/images/skin/icon-server.png'></td>"
                                + "<td align='center' style='color:#8c9ca9;'>" + (item.myself?"<i class='fa fa-user fa-3x'></i>":"")+"</td>"
                                + "<td>" + item.displayServerId + "</td>"
                                + "<td>" + item.friendlyName + "</td>"
                                + "<td align='left'>"+item.ipAddress+"</td>"
                                + "<td align='left'>"+item.contacted+"</td>"
                                + "<td align='center'><i class='fa fa-circle fa-2x "+item.status+"'></i></td>"
                                + deleteServer
                                + "<td id='td-"+index+"'></td>"
                            + "</tr>";

                            nodesTableHTML += "<input type='hidden' id='serverId-"+index+"' value='"+item.serverId+"'>"
                        });

                        nodesTableHTML += "</table>";

                        var nodesTable = dojo.create("div", { innerHTML: nodesTableHTML });

                        dojo.empty(dojo.byId("container"));
                        dojo.place(nodesTable, dojo.byId("container"));

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
            };

            deferred = dojo.xhrGet(xhrArgs);

        }

        require(["dojo/ready"], function(ready){
              ready(function(){

                  renderNodesStatus();

                 /**
                    Set the jsp to load up when the action panel is activated
                 **/
                  actionPanelTable = new com.dotcms.ui.ActionPanel({jspToShow:"/html/portlet/ext/clusterconfig/cluster_config_right_panel.jsp", alwaysShow:true});

              });
        });


        function refreshStatus(serverId) {
             renderNodesStatus();
             actionPanelTable = new com.dotcms.ui.ActionPanel({jspToShow:"/html/portlet/ext/clusterconfig/cluster_config_right_panel.jsp", alwaysShow:true});
        }

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

        function removeFromCluster(serverId){
            if(!confirm('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "configuration_cluster_remove_server_confirm")) %>')) return;
            dojo.xhrPost({
                url: "/api/cluster/remove/serverid/"+serverId,
                load: function() {
                    renderNodesStatus();
                    actionPanelTable = new com.dotcms.ui.ActionPanel({jspToShow:"/html/portlet/ext/clusterconfig/cluster_config_right_panel.jsp", alwaysShow:true});
                }
            });
        }

        function showCacheDialog(){
           
        }
        
</script>

<div id="myCacheDialog" dojoType="dijit.Dialog" draggable="false" style="display:none" title="Servers Not in Cache">
    <div id="myCacheDialogName"></div>
</div>

<div id="doc3" style="min-height: 600px">

    <div class="actionPannelPage" id="container" style="min-height: 600px">
        <div id="actionPanel" class="hideMe" style="height: 600px">
            <div id="arrow"><img src='/html/images/skin/arrow.png'/></div>
            <div id="actionPanelContent" style="overflow:auto;"></div>
        </div>
    </div>

</div>