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

<%
    PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();
    EnvironmentAPI eAPI = APILocator.getEnvironmentAPI();

    List<Environment> environments = eAPI.findAllEnvironments();
    List<PublishingEndPoint> endpoints = pepAPI.getAllEndPoints();
%>

<script type="text/javascript">

    function goToAddEnvironment() {
        var dialog = new dijit.Dialog({
            id: 'addEnvironment',
            title: "<%= LanguageUtil.get(pageContext, "publisher_Environment_Add")%>",
            style: "width: 700px; ",
            content: new dojox.layout.ContentPane({
                href: "/html/portlet/ext/contentlet/publishing/add_publish_environment.jsp"
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

    function deleteEndpoint(identifier, fromEnvironment) {

        if (confirm("Are you sure you want to delete this endpoint?")) {

            var xhrArgs = {
                url: "/api/config/deleteEndpoint",
                content: {
                    'endPoint': identifier
                },
                handleAs: "json",
                load: function (data) {

                    var isError = false;
                    if (data.success == false || data.success == "false") {
                        isError = true;
                    }

                    loadRemotePublishingTab();
                    showDotCMSSystemMessage(data.message, isError);
                },
                error: function (error) {
                    showDotCMSSystemMessage(error.responseText, true);
                }
            };
            dojo.xhrPost(xhrArgs);
        }

    }

    function deleteEnvironment(identifier) {

        if (confirm("<%= LanguageUtil.get(pageContext, "publisher_Delete_Environment_Confirm")%>")) {

            var xhrArgs = {
                url: "/api/config/deleteEnvironment",
                content: {
                    'environment': identifier
                },
                handleAs: "json",
                load: function (data) {

                    var isError = false;
                    if (data.success == false || data.success == "false") {
                        isError = true;
                    }

                    loadRemotePublishingTab();
                    showDotCMSSystemMessage(data.message, isError);
                },
                error: function (error) {
                    showDotCMSSystemMessage(error.responseText, true);
                }
            };
            dojo.xhrPost(xhrArgs);
        }

    }

    function goToAddEndpoint(environmentId, isSender) {
        var dialog = new dijit.Dialog({
            id: 'addEndpoint',
            title: "<%= LanguageUtil.get(pageContext, "publisher_Endpoint_Add")%>",
            style: "width: 800px; ",
            content: new dojox.layout.ContentPane({
                href: "/html/portlet/ext/contentlet/publishing/add_publish_endpoint.jsp?environmentId=" + environmentId + "&isSender=" + isSender
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


    function refreshWhoCanUse() {
        dojo.empty("whoCanUseTbl");
        var table = dojo.byId("whoCanUseTbl");
        var x = "";

        this.whoCanUse = this.whoCanUse.sort(function (a, b) {
            var x = a.name.toLowerCase();
            var y = b.name.toLowerCase();
            return ((x < y) ? -1 : ((x > y) ? 1 : 0));
        });
        for (i = 0; i < this.whoCanUse.length; i++) {
            var what = (this.whoCanUse[i].id.indexOf("user") > -1) ? " (<%=LanguageUtil.get(pageContext, "User")%>)" : "";
            x = x + this.whoCanUse[i].id + ",";
            var tr = dojo.create("tr", null, table);
            dojo.create("td", { width: 10, innerHTML: "<span class='deleteIcon'></span>", className: "wfXBox", onClick: "removeFromWhoCanUse('" + this.whoCanUse[i].id + "');refreshWhoCanUse()" }, tr);
            dojo.create("td", { innerHTML: this.whoCanUse[i].name + what}, tr);

        }
        dojo.byId('whoCanUse').value = x;
    }

    function addSelectedToWhoCanUse() {

        var select = dijit.byId("whoCanUseSelect");

        var user = select.getValue();
        var userName = select.attr('displayedValue');

        if(user=='0') return;

        addToWhoCanUse(user, userName);
        refreshWhoCanUse();

        select.set('value', '0');

    }

    function addToWhoCanUse(myId, myName) {

        for (i = 0; i < this.whoCanUse.length; i++) {
            if (myId == this.whoCanUse[i].id || myId == "user-" + this.whoCanUse[i].id || myId == "role-" + this.whoCanUse[i].id) {
                return;
            }
        }

        var entry = {name: myName, id: myId };
        this.whoCanUse[this.whoCanUse.length] = entry;
    }

    function backToEnvironmentList(addedEndPoint) {

        if (!addedEndPoint) {
            dijit.byId("addEnvironment").hide();
        } else {
            dijit.byId("addEndpoint").hide();
        }
        loadRemotePublishingTab();
    }

    function backToEndpointsList(){

        dijit.byId("addEndpoint").hide();
        loadRemotePublishingTab();
    }

    var whoCanUse = new Array()

    function removeFromWhoCanUse(myId) {

        var x = 0;
        var newCanUse = new Array();
        for (i = 0; i < this.whoCanUse.length; i++) {
            if (myId != this.whoCanUse[i].id) {
                newCanUse[x] = this.whoCanUse[i];
                x++;
            }
        }
        this.whoCanUse = newCanUse;

        var select = dijit.byId("whoCanUseSelect");
        select.set('value', '0');
    }

    function deleteEnvPushHistory(envId) {

    	var xhrArgs = {
    		url : '/api/bundle/deleteenvironmentpushhistory/environmentid/'+envId,
    		handleAs : "json",
    		sync: false,
    		load : function(data) {
				alert('<%= LanguageUtil.get(pageContext, "publisher_Environments_deleted_assets-history") %>');
    		},
    		error : function(error) {
    			targetNode.innerHTML = "An unexpected error occurred: " + error;
    		}
    	};

    	var deferred = dojo.xhrGet(xhrArgs);
    }

</script>

<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>
<%--START OF ENVIROMENTS--%>
<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>
<div class="yui-g portlet-toolbar">

    <div class="yui-u first">
        <span class="sServerIcon"></span>
        <span  style="line-height:20px;font-weight: bold;"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Sending_Server_Short") %></span>
    </div>

    <div class="yui-u" style="text-align:right;">
        <button dojoType="dijit.form.Button" onClick="goToAddEnvironment();" iconClass="plusIcon">
            <%= LanguageUtil.get(pageContext, "publisher_Add_Environment") %>
        </button>
    </div>
</div>

<div style="padding-top: 5px">
    <table  class="listingTable">
        <tr>
            <th>
            </th>
            <th nowrap="nowrap">
                <%= LanguageUtil.get(pageContext, "publisher_Environment_Name") %>
            </th>
            <th nowrap="nowrap" width="100%" >
                <%= LanguageUtil.get(pageContext, "Servers") %>
            </th>
            <th nowrap="nowrap">
                <%= LanguageUtil.get(pageContext, "Push-To-All") %>
            </th>
            <th nowrap="nowrap">
           	 <%= LanguageUtil.get(pageContext, "Actions") %>
            </th>
        </tr>

        <%
        	boolean altRow = false;
            boolean hasEnvironments = false;
            for(Environment environment : environments){
                hasEnvironments=true;%>

        <tr style="<%=(altRow) ? "background:#f3f3f3" :""%>">
        	<%altRow=!altRow; %>
            <td nowrap="nowrap" valign="top">
                <a style="cursor: pointer" onclick="deleteEnvironment('<%=environment.getId()%>')" title="<%= LanguageUtil.get(pageContext, "publisher_Delete_Environment") %>">
                    <span class="deleteIcon"></span></a>&nbsp;
                <a style="cursor: pointer" onclick="goToEditEnvironment('<%=environment.getId()%>')" title="<%= LanguageUtil.get(pageContext, "publisher_Edit_Environment_Title") %>">
                    <span class="editIcon"></span></a>
            </td>
            <td valign="top" nowrap="nowrap">
                <b><%=environment.getName()%></b>
            </td>
            <td style="padding:0px;" valign="top">

                    <%
                        List<PublishingEndPoint> environmentEndPoints = pepAPI.findSendingEndPointsByEnvironment(environment.getId());
                        boolean hasRow = false;
                        for(PublishingEndPoint endpoint : environmentEndPoints){
                            if(endpoint.isSending()){
                                continue;
                            }
                            hasRow=true;%>
						<div style="padding:10px;border-bottom:1px solid silver;margin-bottom:-1px">
	                        <div style="float:right">
		                            <a style="cursor: pointer" onclick="deleteEndpoint('<%=endpoint.getId()%>', true)" title="<%= LanguageUtil.get(pageContext, "publisher_Delete_Endpoint_Title") %>">
		                                <span class="deleteIcon"></span></a>
		                    </div>
		                    <div <%=(!endpoint.isEnabled()?" style='color:silver;'":"")%> style="cursor:pointer" onclick="goToEditEndpoint('<%=endpoint.getId()%>', '<%=environment.getId()%>', 'false')">

	                            <div >
	                            	<%=(endpoint.isEnabled()?"<span class='liveIcon'></span>":"<span class='greyDotIcon' style='opacity:.4'></span>")%><%=endpoint.getServerName()%>
	                            </div>
	                            <div>
									<%=("https".equals(endpoint.getProtocol())) ? "<span class='encryptIcon'></span>": "<span class='shimIcon'></span>" %>
	                            	<i style="color:#888;"><%=endpoint.getProtocol()%>://<%=endpoint.getAddress()%>:<%=endpoint.getPort()%></i>
								</div>
		                    </div>
	                    </div>
                    <%}%>

                    <%if(!hasRow){ %>
                        <div  style="padding:5px;">
                        	<%= LanguageUtil.get(pageContext, "publisher_No_Servers") %> <a style="text-decoration: underline;" href="javascript:goToAddEndpoint('<%=environment.getId()%>', 'false');"><%= LanguageUtil.get(pageContext, "publisher_add_one_now") %></a>
                     	</div>
                    <%}%>

            </td>
            <td align="center" valign="top" nowrap="nowrap">
                <%=environment.getPushToAll()%>
            </td>
            <td valign="top" nowrap="nowrap">
                <button dojoType="dijit.form.Button" onClick="goToAddEndpoint('<%=environment.getId()%>', 'false');" iconClass="plusIcon">
                    <%= LanguageUtil.get(pageContext, "publisher_Add_Endpoint") %>
                </button>
                 <button dojoType="dijit.form.Button" onClick="deleteEnvPushHistory('<%=environment.getId()%>');" iconClass="deleteIcon" >
					<%= LanguageUtil.get(pageContext, "publisher_delete_asset_history") %>
				</button>
            </td>

        </tr>

    <%}%>

    <%if(!hasEnvironments){ %>
        <tr>
            <td colspan="100" align="center">
                <%= LanguageUtil.get(pageContext, "publisher_no_environments") %><a href="javascript:goToAddEnvironment();"> <%= LanguageUtil.get(pageContext, "publisher_add_one_now") %></a>
            </td>
        </tr>
    <%}%>

    </table><br>

</div>
<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>
<%--END OF ENVIROMENTS--%>
<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>

<hr style="margin-top: 30px;margin-bottom: 30px;">

<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>
<%--START OF END POINTS--%>
<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>
<div class="yui-g portlet-toolbar">
    <div class="yui-u first">
        <span class="rServerIcon"></span>
        <span  style="line-height:20px;font-weight: bold;"><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Receiving_Server_Short") %></span>
    </div>
    <div class="yui-u" style="text-align:right;">
        <button dojoType="dijit.form.Button" onClick="goToAddEndpoint(null, 'true');" iconClass="plusIcon">
            <%= LanguageUtil.get(pageContext, "publisher_Add_Endpoint") %>
        </button>
    </div>
</div>

<div style="padding-top: 5px">
    <table class="listingTable">
        <tr>
            <th style="width:40px"></th>
            <th><%= LanguageUtil.get(pageContext, "publisher_Endpoints_Server_Name") %></th>

        </tr>
        <%
            boolean hasRow = false;
            for(PublishingEndPoint endpoint : endpoints){
                if(!endpoint.isSending()){
                    continue;
                }
                hasRow=true;%>
        <tr <%=(!endpoint.isEnabled()?" style='color:silver;'":"")%>>
            <td nowrap="nowrap" valign="top">
                <a style="cursor: pointer" onclick="deleteEndpoint('<%=endpoint.getId()%>')" title="<%= LanguageUtil.get(pageContext, "publisher_Delete_Endpoint_Title") %>">
                    <span class="deleteIcon"></span></a>&nbsp;
                <a style="cursor: pointer" onclick="goToEditEndpoint('<%=endpoint.getId()%>', null, 'true')" title="<%= LanguageUtil.get(pageContext, "publisher_Edit_Endpoint_Title") %>">
                    <span class="editIcon"></span></a>
            </td>

            <td style="cursor: pointer" width="100%" onclick="goToEditEndpoint('<%=endpoint.getId()%>', null, 'true')">
                <b><%=(endpoint.isEnabled()?"<span class='liveIcon'></span>":"<span class='greyDotIcon' style='opacity:.4'></span>")%><%=endpoint.getServerName()%></b>
                <br>
                <i><span class='shimIcon'></span><%=endpoint.getAddress()%></i>
            </td>



        </tr>
        <%}%>

        <%if(!hasRow){ %>

        <tr>
            <td colspan="100" align="center">
                <%= LanguageUtil.get(pageContext, "publisher_no_servers_set_up") %><a href="javascript:goToAddEndpoint(null, 'true');"> <%= LanguageUtil.get(pageContext, "publisher_add_one_now") %></a>
            </td>

        </tr>
        <%}%>
    </table>
</div>
<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>
<%--END OF END POINTS--%>
<%--++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--%>