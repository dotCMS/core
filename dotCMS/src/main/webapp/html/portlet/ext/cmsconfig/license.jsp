<%@page import="com.dotcms.enterprise.license.LicenseManager"%>
<%@page import="com.dotcms.repackage.org.apache.struts.Globals"%>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.liferay.portal.struts.MultiMessageResources"%>
<%@ include file="/html/portlet/ext/cmsconfig/init.jsp" %>
<% request.setAttribute("requiredPortletAccess", PortletID.CONFIGURATION.toString()); %>
<%@ include file="/html/common/uservalidation.jsp"%>

<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="java.util.Date"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>
<%@page import="java.text.SimpleDateFormat"%>

<%
    String error=null;
    String message=null;
    String newLicenseMessage="";
    String newLicenseURL="";

    if ( session.getAttribute( "applyForm" ) != null && session.getAttribute( "applyForm" ).equals( Boolean.TRUE ) ) {
        error = LicenseUtil.processForm( request );
    }

    String serverId= APILocator.getServerAPI().readServerId();





    boolean isCommunity =LicenseUtil.getLevel()==LicenseLevel.COMMUNITY.level;

    String expireString = "unknown";
    Date expires = null;
    try{
        expires = LicenseUtil.getValidUntil();
        SimpleDateFormat format =
                new SimpleDateFormat("MMMM d, yyyy");
        expireString=  format.format(expires);
    }
    catch(Exception e){

    }
    boolean expired = (expires !=null && expires.before(new Date()));
    boolean isPerpetual = LicenseUtil.isPerpetual();

    String requestCode=(String)request.getAttribute("requestCode");

    SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
    SimpleDateFormat dfOut = new SimpleDateFormat("MMM dd, yyyy");

%>



<script type="text/javascript">

    function emitLicenseChangedEvent(){
        var customEvent = new CustomEvent("ng-event", {
            detail: {
                name: "license-changed",
            }
        });

        document.dispatchEvent(customEvent);
    }

    dojo.declare("dotcms.dijit.cmsconfig.LicenseAdmin", null, {

        isCommunity     :"<%=isCommunity%>",

        requestTrial : function(){

        	dojo.byId("trialLicenseForm").submit();

        },

        resetLicense :function () {

            var data = {"licenseText":"reset"};
            dojo.xhrPost({
                url: "/api/license/resetLicense/",
                handleAs: "text",
                postData: data,
                load: function(message) {
                    emitLicenseChangedEvent()
                    licenseAdmin.refreshLayout('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-reset")) %>')
                },
                error: function(error){
                    showDotCMSSystemMessage("ERROR:" + error,true);
                    //licenseAdmin.refreshLayout();

                }
            });
        },


        refreshLayout : function(text){
            if(dijit.byId('uploadDiaWindow')){
                dijit.byId('uploadDiaWindow').hide();
            }
            if(dijit.byId('uploadgetLicenseCodeDiaDiaWindow')){
                dijit.byId('getLicenseCodeDia').hide();
            }

            loadLicenseTabMessage(text);
        },



        doLicensePaste :function () {


            var data;

            if(dojo.byId("licenseCodePasteField").value==undefined || dojo.byId("licenseCodePasteField").value.length>0){
                 data = dojo.byId("licenseCodePasteField").value;
            }
            else{

                 data = dojo.byId("licenseCodePasteFieldTwo").value;
            }


            var dataEncoded = encodeURIComponent(data);


            dojo.xhrPost({
                url: "/api/license/applyLicense?licenseText="+dataEncoded,
                handleAs: "text",
                load: function(message) {

                    if(! message ){
                        emitLicenseChangedEvent()
                        licenseAdmin.refreshLayout('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-applied")) %>');
                    }
                    else{
                        showDotCMSSystemMessage("ERROR: " + message,true);
                        console.log("message:" + message);
                    }
                },
                error: function(error){


                }
            });
        },

         levelName : function(level) {

            var x = parseInt(level);
            switch(x) {
                case 100: return "Community";break;
                case 200: return "Professional"; break;
                case 300: return "Enterprise"; break;
                case 400: return "Prime"; break;
                case 500: return "Platform"; break;
                default: return "-";
            }
        },

        typeName : function (type) {
            switch(type) {
                case "dev": return "Development"; break;
                case "prod": return "Production"; break;
                case "trial": return "Trial"; break;
                default: return "-";
            }
        },

         currentServerId :'<%= serverId %>',



         displayId : function (id){
             if(id== undefined || id.length==0){
                 return "";
             }
             else{
                 return id.split("-")[0]
             }

         },



         load : function () {
            if(dojo.byId("repotableBody") ==undefined){
                return;
            }
            dojo.empty("repotableBody");
            dojo.xhrGet({
                url: "/api/license/all/",
                handleAs: "json",
                load: function(data) {
                    dojo.forEach(data, function(lic) {
                        var row;
                        var expiredSpan=(lic.expired) ?  "<span style='text-decoration: line-through;'>" : "<span>";
                        var closeSpan= "</span>";
                        if(lic.serverId===licenseAdmin.currentServerId) {
                            row=dojo.create("tr",{"class":"current_server_row selected"},dojo.byId("repotableBody"),"first");
                        }
                        else {
                            row=dojo.create("tr",null,dojo.byId("repotableBody"));
                        }

                        var serial=lic.id;
                        var optd=dojo.create("td",{"nowrap":"true", class: 'license-manager__listing-action'},row);

                        if(lic.serverId==licenseAdmin.currentServerId  ) {

                            dojo.create("span",{"class":"unlockIcon", title:"<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-tip-free") )%>"},
                                    dojo.create("a",{href:"javascript:licenseAdmin.free()"},optd));
                            dojo.create("img", {src:"/html/images/shim.gif", style:"width:16px;height:16px"},optd);
                        } else if(lic.available == "false") {

                            dojo.create("span",{"class":"unlockIcon", title:"<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-tip-free") )%>"},
                                    dojo.create("a",{href:"javascript:licenseAdmin.free('"+lic.id+"','"+lic.fullserverid+"')"},optd));
                        } else {
                            if(lic.expired){
                            	dojo.create("img", {src:"/html/images/shim.gif", style:"width:16px;height:16px"},optd);
                            }
                            else{
                                dojo.create("span",{"class":"downloadIcon",title:"<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-tip-pick")) %>"},
                                    dojo.create("a",{href:"javascript:licenseAdmin.pick('"+serial+"')"},optd));
                            }
                            dojo.create("span",{"class":"deleteIcon", title:"<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-tip-del")) %>"},
                                    dojo.create("a",{href:"javascript:licenseAdmin.del('"+serial+"')"},optd));
                        }

                        if(lic.serverId==licenseAdmin.currentServerId  ) {
                            dojo.create("td", { innerHTML:expiredSpan + licenseAdmin.displayId(lic.serverId) + " (this server)" }, row);
                        }
                        else{
                            dojo.create("td", { innerHTML: expiredSpan + licenseAdmin.displayId(lic.serverId) + closeSpan }, row);
                        }

                        dojo.create("td", { innerHTML: expiredSpan + licenseAdmin.displayId(lic.serial) + closeSpan}, row);
                        dojo.create("td", { innerHTML: lic.serverId ? lic.startupTime  : ""}, row);
                        dojo.create("td", { innerHTML: lic.serverId ? lic.lastPingStr : ""}, row);



                        dojo.create("td", { innerHTML: (lic.perpetual )
                            ? "<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "request-license-perpetual")) %>"
                            : (lic.expired)
                               ? "<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "request-license-expired")) %>"
                               : lic.validUntilStr}, row);
                        dojo.create("td", { innerHTML:expiredSpan + lic.level + closeSpan}, row);
                        dojo.create("td", { innerHTML:expiredSpan + licenseAdmin.typeName(lic.licenseType)+ closeSpan}, row);
                    });
                    if(data.length==0){
                        var row=dojo.create("tr",null,dojo.byId("repotableBody"));
                        var optd=dojo.create("td",{"colspan":100,"align":"center"},row);
                        optd.innerHTML="<a href='#' onclick=\"dijit.byId('uploadDiaWindow').show()\"><%= LanguageUtil.get(pageContext, "No-Results-Found") %></a>";
                    }


                }



            });
            <%if(UtilMethods.isSet(request.getParameter("message"))){ %>
                showDotCMSSystemMessage('<%= UtilMethods.javaScriptify(request.getParameter("message")) %>');
            <%} %>
        },

        del:  function (serial) {
            if(!confirm('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-repo-confirm-delete")) %>')) return;
            dojo.xhrDelete({
                url: "/api/license/delete/id/"+serial+"/",
                load: function() {
                    licenseAdmin.refreshLayout('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-deleted")) %>');
                }
            });
        },

        pick:  function (serial) {
            if(!confirm('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-repo-confirm-pick")) %>')) return;
            dojo.xhrPost({
                url: "/api/license/pick/serial/"+serial+"/",
                load: function() {
                    licenseAdmin.refreshLayout('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-applied")) %>');
                }
            });
        },

        free: function () {
            if(!confirm('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-repo-confirm-free")) %>')) return;
            dojo.xhrPost({
                url: "/api/license/free/",
                load: function() {
                    licenseAdmin.refreshLayout('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-freed") )%>');
                }
            });

        },

        free: function (serial, serverid) {
            if(!confirm('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-repo-confirm-free-remote")) %>')) return;
            dojo.xhrPost({
                url: "/api/license/free/serial/"+serial+"/serverid/"+serverid+"/",
                load: function() {
                    licenseAdmin.refreshLayout('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "license-freed") )%>');
                }
            });

        },

        doPackUpload : function () {

            if(!dojo.byId("uploadPackFile").value || dojo.byId("uploadPackFile").value.length<1){
                return;
            }

            var xhr = new XMLHttpRequest();

            xhr.open("POST", "/api/license/upload/");

            xhr.onload = function(event){
                emitLicenseChangedEvent()
                licenseAdmin.refreshLayout('<%= UtilMethods.javaScriptify(LanguageUtil.get(pageContext, "licenses-uploaded") )%>');
            };
            var formData = new FormData(document.getElementById("uploadPackForm"));
            xhr.send(formData);

            return false;
        }
    });


        var licenseAdmin = new dotcms.dijit.cmsconfig.LicenseAdmin({});

    dojo.require("dojo.io.iframe");
    dojo.ready(licenseAdmin.load);

</script>




<style type="text/css">
    .blankTable,  .blankTable th, .blankTable td {
        border: 0px solid #ffffff;
    }
    .blankTable {width: 100%;}
    .blankTable td {padding: 8px 10px;}
    .blankTable th {font-weight: bold;background: #fff;}
    .alert {
        border-radius: 4px;
        padding: 25px 20px 20px 20px;
        border: 1px solid transparent;
    }
    .alert-info {
        color: #31708f;
        background-color: #d9edf7;
        border-color: #bce8f1;
    }
    .alert-warning {
        color: #8a6d3b;
        background-color: #fcf8e3;
        border-color: #faebcc;
    }
    .hidden{display: none;}
    hr {
        margin: 40px 0;
    }
    .stepHeader {
        color: #31708f;
        background-color: #d9edf7;
        border: 1px solid #bce8f1;
        padding:5px 8px;
        font-size: 16px;
        font-weight: bold;
    }
    .stepHeader small {
        font-weight: 300;
        font-size: 12px;
    }
    .stepBody {
        border: 1px solid #bce8f1;
        padding: 15px 10px;
        border-top: 0;
        height: 90%;
    }
    .btn{
        display: inline-blcok;
        padding: 8px 12px;
        font-size: 1.2em;
        text-decoration: none;
        margin-right: 20px;
        -webkit-transition: background 200ms ease-in 200ms; /* property duration timing-function delay */
        -moz-transition: background 200ms ease-in 200ms;
        -o-transition: background 200ms ease-in 200ms;
        transition: background 200ms ease-in 200ms;
        background: #f2f2f2;
        color: #555555;
    }
    .btn:hover{
        background: #DDD;
    }

    .btn-info {
        background: #088AC8;
        color: #ffffff;
    }
    .btn-info:hover{
        background: #22A1D7;
    }
    .btn-warning {
        background: #f0ad4e;
        color: #ffffff;
    }
    .btn-warning:hover{
        background: #ec971f;
    }

    .license-manager{
        padding: 0 30px;
    }
    .license-manager-information__title{
        margin-bottom: 1.5rem;
     }
    .license-manager-information__actions{
        margin-top: 1.5rem;
        display: flex;
        gap: 1rem;
        justify-content: right;
     }
</style>

<form name="trialLicenseForm" id="trialLicenseForm" method="POST" target="trialRequestWindow" action="https://dotcms.com/licensing/request-a-license-3/">
    <input type="hidden" name="trialLicenseRequestCode" id="trialLicenseRequestCode" value="<%=LicenseManager.getInstance().createTrialLicenseRequestCode()%>">
</form>

    <div class="license-manager">
        <!-- CURRENT LICENSE INFO -->
        <div class="license-manager-information">
           <h2 class="license-manager-information__title" ><%= LanguageUtil.get(pageContext, "license-current-info") %></h2>


            <table class="listingTable" >

                <tr>
                    <th width="25%" nowrap="true">
                        <%= LanguageUtil.get(pageContext, "license-level") %>
                    </th>

                    <td>
                        <% if(isCommunity){  %>
                            <a href="#"
                                    onclick="licenseAdmin.requestTrial()"
                                    >
                                <b><%= LicenseUtil.getLevelName()  %></b>
                            </a>

                        <%}else{ %>
                            <b><%= LicenseUtil.getLevelName()  %></b>
                        <%} %>
                    </td>
                </tr>

                <tr>
                    <th>
                        Server ID:
                    </th>
                    <td>
                        <%= serverId.split("-")[0] %>
                    </td>
                </tr>
                 <% if(!isCommunity){  %>
                        <tr>
                            <th nowrap="true"><%= LanguageUtil.get(pageContext, "license-valid-until") %>:</th>
                            <td>
                                    <% if(isPerpetual) { %>
                                        <%= LanguageUtil.get(pageContext, "request-license-perpetual") %>
                                    <%} else {%>
                                        <%if(expired && !isPerpetual){ %>
                                            <font color="red">
                                        <%} %>
                                        <%= expireString %>
                                        <%if(expired && !isPerpetual){ %>
                                            <%= LanguageUtil.get(pageContext, "request-license-expired") %></font>
                                        <%} %>
                                 <%}%>
                            </td>
                        </tr>

                        <tr>
                            <th><%= LanguageUtil.get(pageContext, "licensed-to") %>:</th>
                            <td><%=  UtilMethods.isSet(LicenseUtil.getClientName()) ? LicenseUtil.getClientName() : "No License Found" %></td>
                        </tr>
                        <tr>
                            <th><%= LanguageUtil.get(pageContext, "license-type") %>:</th>
                            <td><%= LicenseUtil.getLicenseType() %></td>
                        </tr>
                        <tr>
                            <th><%= LanguageUtil.get(pageContext, "license-serial") %>:</th>
                            <td><%= LicenseUtil.getDisplaySerial() %></td>
                        </tr>
                    <% } %>
            </table>


        <div class="license-manager-information__actions">
            <button data-dojo-type="dijit.form.Button" onClick="dijit.byId('uploadDiaWindow').show()">
                    <%= LanguageUtil.get(pageContext, "I-already-have-a-license-to-upload") %>
             </button>

        <% if(!isCommunity){  %>

            <button data-dojo-type="dijit.form.Button" onClick="licenseAdmin.resetLicense()" iconClass="resetIcon">
                <%= LanguageUtil.get(pageContext, "license-bad-id-button") %>
            </button>

         <%} else {%>
                <button type="button" onclick="licenseAdmin.requestTrial()" id="trailBtn" data-dojo-type="dijit.form.Button">
                    <%= LanguageUtil.get(pageContext, "request-trial-license") %>
                </button>
         <%} %>

        </div>
    </div>



        <!-- LICENSE PACK -->
        <div class="license-manager-licensePack" id="licensePack">


            <h2><%= LanguageUtil.get(pageContext, "Licenses") %></h2>
            <p><%= LanguageUtil.get(pageContext, "request-license-cluster-license-explaination") %></p>

            <table border="0" width="100%" class="listingTable">
                <thead>
                <tr>
                    <th>&nbsp;</th>
                    <th><%= LanguageUtil.get(pageContext, "license-repo-serverid") %></th>
                    <th><%= LanguageUtil.get(pageContext, "license-serial") %></th>
                    <th><%= LanguageUtil.getOrDefaultValue("license-repo-running-since", "Started") %></th>
                    <th>
                        <%= LanguageUtil.get(pageContext, "license-repo-last-ping") %>
                    </th>
                    <th><%= LanguageUtil.get(pageContext, "license-repo-validuntil") %></th>
                    <th><%= LanguageUtil.get(pageContext, "license-repo-level") %></th>
                    <th><%= LanguageUtil.get(pageContext, "license-repo-type") %></th>
                </tr>
                </thead>
                <tbody id="repotableBody">
                    <tr>
                        <td colspan="7"><%= LanguageUtil.get(pageContext, "license-repo-type") %></td>
                    </tr>
                </tbody>
            </table>
        </div>




</div><!-- /CONTENT WRAPPER -->

<div dojoType="dijit.Dialog" id="uploadDiaWindow" title="<%= LanguageUtil.get(pageContext, "Upload-license-pack") %>">
    <form method="POST" data-dojo-type="dijit.form.Form" action="/api/license/upload/" onSubmit="return false" encType="multipart/form-data" id="uploadPackForm">
        <div class="inline-form">
            <input type="file" name="file" id="uploadPackFile" accept="application/zip"/>
            <button data-dojo-type="dijit.form.Button" name="btnSubmit" iconClass="uploadIcon" onClick="licenseAdmin.doPackUpload()"><%= LanguageUtil.get(pageContext, "Upload-license-pack-button") %></button>
        </div>
    </form>
</div>
