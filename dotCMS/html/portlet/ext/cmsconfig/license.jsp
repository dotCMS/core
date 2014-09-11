<%@ include file="/html/portlet/ext/cmsconfig/init.jsp" %>
<% request.setAttribute("requiredPortletAccess", "9"); %>
<%@ include file="/html/common/uservalidation.jsp"%>

<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="java.util.Map"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="java.util.Date"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="java.text.SimpleDateFormat"%>

<script type="text/javascript">
    dojo.require("dojo.parser");// scan page for widgets and instantiate them
</script>

<%

    String licenseTab = "/c/portal/layout?p_l_id=" + layoutId + "&p_p_id=9&tab=licenseTab";

    String error=null;
    String message=null;
    String newLicenseMessage="";
    String newLicenseURL="";

    if ( session.getAttribute( "applyForm" ) != null && session.getAttribute( "applyForm" ).equals( Boolean.TRUE ) ) {
        error = LicenseUtil.processForm( request );
    }

    String serverId = "";
    boolean badId=false;
    try {
        LicenseUtil.getLevel();
        serverId = LicenseUtil.getDisplayServerId();
    }
    catch(Exception ex) {
        badId=true;
    }


    boolean isCommunity = LicenseUtil.getLevel()==100;

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

<% if(badId) { %>
<div class="portlet-wrapper">
    <div style="min-height:400px;" id="borderContainer" >
        <div style="margin-left:auto;margin-right:auto;width:600px;background:#eee;" class="callOutBox">
            <p><%= LanguageUtil.get(pageContext, "license-bad-id-explanation") %></p>
            <form method="POST">
                <input type="hidden" name="iwantTo" value="reset-license"/>
                <input type="submit" name="submit" value="<%= LanguageUtil.get(pageContext, "license-bad-id-button") %>"/>
            </form>
        </div>
    </div>
</div>
<% } else { %>

<script type="text/javascript">

    var isCommunity = "<%=isCommunity%>";

    <%if(UtilMethods.isSet(error)){ %>
    showDotCMSSystemMessage("<%=error %>");
    <%} %>

    function requestTrial(){
        dojo.byId("uploadLicenseForm").submit();
    }

    function doCodeRequest() {
        dojo.byId("uploadLicenseForm").submit();
    }

    function doOnlineLicenseRequest() {
        dojo.byId("uploadLicenseForm").submit();
    }

    function toggleLevel() {
        //dijit.byId('license_level').set('disabled','');
        if(dijit.byId("license_type").get("value")=="trial") {
            dijit.byId('license_level').set('value','400');
            //dijit.byId('license_level').set('disabled','disabled');
        }
    }

    function doShowHideRequest(){

        dojo.style("pasteMe", "display", "none");
        dojo.style("licensedata", "display", "none");

        if(dijit.byId("pasteRadio").checked){
            dojo.style("pasteMe", "display", "");
        }
        else if(dijit.byId("reqcodeRadio").checked) {
            dojo.style("licensedata", "display", "");
        }
    }

    function doPaste(){
        if(!<%=isCommunity%>){

            if(!confirm("<%= LanguageUtil.get(pageContext, "confirm-license-override") %>")){
                return false;
            }

        }

        dojo.byId("uploadLicenseForm").submit();
    }

    var currentPane = dijit.byId("licenseTabContent");
    currentPane.connect(currentPane, 'onLoad', function() {
        <% if(isCommunity) { %>
            dijit.byId("reqcodeRadio").set("checked", "true");
            dijit.byId("license_type").set("value", "trial");
            toggleLevel();
        <% } %>
    });


</script>

<style type="text/css">
    tr.current_server_row td {
        background-color:#D8F6CE
    }
</style>
<div class="portlet-wrapper">

<div style="min-height:400px;" id="borderContainer" class="shadowBox headerBox">
<div style="padding:7px;">
    <div>
        <h3><%= LanguageUtil.get(pageContext, "com.dotcms.repackage.javax.portlet.title.EXT_LICENSE_MANAGER") %></h3>
    </div>
</div>

<div style="margin-left:auto;margin-right:auto;width:600px;background:#eee;" class="callOutBox">
    Server ID: <%= serverId %>
</div>

<%if(request.getAttribute("LICENSE_APPLIED_SUCCESSFULLY") != null){ %>
<div style="margin-left:auto;margin-right:auto;width:600px;" class="callOutBox">
    <%= LanguageUtil.get(pageContext, "license-trial-applied-successfully") %>
</div>
<%} %>

<% if(requestCode!=null) {%>
<div style="margin-left:auto;margin-right:auto;width:600px;" class="callOutBox">
    <p><%= LanguageUtil.get(pageContext, "license-code-description") %></p>
    <p style="word-wrap: break-word;"><strong><%=requestCode%></strong></p>
</div>
<% } %>

<form name="query" id="uploadLicenseForm" action="<%= licenseTab %>" method="post" onsubmit="return false;">
    <div style="width:600px;margin:auto;border:1px solid silver;padding:20px;background:#eee;">
        <dl>
            <dt>
                <span class='<%if(isCommunity){  %>lockIcon<%}else{ %>unlockIcon<%} %>'></span>
                <%= LanguageUtil.get(pageContext, "license-level") %>
            </dt>
            <dd><%= LicenseUtil.getLevelName()  %>
            </dd>
            <% if (!isCommunity) { %>
            <dt><%= LanguageUtil.get(pageContext, "license-valid-until") %>:</dt>
            <% if(isPerpetual) { %>
            <dd>Perpetual</dd>
            <%} else {%>
            <dd><%if(expired && !isPerpetual){ %><font color="red"><%} %>
                <%= expireString %>
                <%if(expired && !isPerpetual){ %> (expired)</font><%} %>
            </dd>
            <%}%>
            <dt><%= LanguageUtil.get(pageContext, "licensed-to") %></dt>
            <dd><%=  UtilMethods.isSet(LicenseUtil.getClientName()) ? LicenseUtil.getClientName() : "No License Found" %></dd>
            <dt><%= LanguageUtil.get(pageContext, "license-type") %></dt>
            <dd><%= LicenseUtil.getLicenseType() %></dd>
            <dt><%= LanguageUtil.get(pageContext, "license-serial") %></dt>
            <dd><%= LicenseUtil.getSerial() %></dd>
            <% } %>
        </dl>
    </div>


    <%if(isCommunity){ %>
    <div style="margin:auto;width:500px;padding-top:30px;">
        <%= LanguageUtil.get(pageContext, "license-trial-promo") %>
    </div>
    <%} %>
    <div style="margin:auto;width:600px;padding:20px;padding-top:0px;padding-bottom:0px;">
        <dl style="padding:20px;padding-bottom:5px;">
            <dt><%= LanguageUtil.get(pageContext, "I-want-to") %>:</dt>
            <dd>

                <input onChange="doShowHideRequest()" type="radio" checked="false" name="iwantTo" data-dojo-id="reqcodeRadio" id="reqcodeRadio" data-dojo-type="dijit.form.RadioButton" value="request_code">
                <label for="reqcodeRadio"><%= LanguageUtil.get(pageContext, "request-code-for-support-portal") %></label><br/>

                <input onChange="doShowHideRequest()"  type="radio" name="iwantTo" data-dojo-id="pasteRadio" id="pasteRadio" data-dojo-type="dijit.form.RadioButton" value="paste_license">
                <label for="pasteRadio"><%= LanguageUtil.get(pageContext, "I-already-have-a-license") %></label><br/>


            </dd>

            <dt>

            <dd id="pasteMe" style="display:none;">
                <b><%= LanguageUtil.get(pageContext, "paste-your-license") %></b>:<br><textarea rows="10" cols="60"  name="license_text" ></textarea>
                <div style="padding:10px;">
                    <button type="button" onclick="doPaste()" data-dojo-id="uploadButton" id="uploadButton" data-dojo-type="dijit.form.Button" name="upload_button" iconClass="keyIcon" value="upload"><%= LanguageUtil.get(pageContext, "save-license") %></button>
                </div>
            </dd>


            <dd id="licensedata" style="display:none">
                <label for="license_type"><%= LanguageUtil.get(pageContext, "request-license-type") %></label>
                <select data-dojo-id="license_type" id="license_type" name="license_type" data-dojo-type="dijit.form.Select" onChange="toggleLevel()">
                    <option value="trial"><%= LanguageUtil.get(pageContext, "request-license-trial") %></option>
                    <option value="prod"><%= LanguageUtil.get(pageContext, "request-license-prod") %></option>
                    <option value="dev"><%= LanguageUtil.get(pageContext, "request-license-dev") %></option>
                </select>
                <br/>
                <label for="license_level"><%= LanguageUtil.get(pageContext, "request-license-level") %></label>
                <select data-dojo-id="license_level" id="license_level" name="license_level" data-dojo-type="dijit.form.Select">
                    <option value="200"><%= LanguageUtil.get(pageContext, "request-license-standard") %></option>
                    <option value="300"><%= LanguageUtil.get(pageContext, "request-license-professional") %></option>
                    <option value="400"><%= LanguageUtil.get(pageContext, "request-license-prime") %></option>
                </select>
                <div style="padding:10px;">
                    <button type="button" onclick="doCodeRequest()" data-dojo-id="codereqButton" id="codereqButton"
                            data-dojo-type="dijit.form.Button" name="codereqButton" iconClass="keyIcon" value="upload"><%= LanguageUtil.get(pageContext, "request-license-code") %> </button>
                </div>
            </dd>


            </dt>

        </dl>
    </div>
</form>
<br/>
<div id="licensepackdiv" style="margin-left:auto;margin-right:auto;width:700px;">

    <script type="text/javascript">
        function doPackUpload() {
            dojo.byId('uploadPackForm').submit();
            return true;
        }
    </script>
    <div style="width:700px;margin:0px;">
        <form method="POST" action="/api/license/upload/" encType="multipart/form-data" id="uploadPackForm">
            <label for="file"><%= LanguageUtil.get(pageContext, "Upload-license-pack") %></label>
            <input type="file" name="file" accept="application/zip"/>
            <input type="hidden" name="return" value="<%= licenseTab %>"/>
            <button data-dojo-type="dijit.form.Button" name="btnSubmit" onClick="doPackUpload()"><%= LanguageUtil.get(pageContext, "Upload-license-pack-button") %></button>
        </form>
    </div>

    <% if(LicenseUtil.getLicenseRepoTotal()>0) { %>
    <script type="text/javascript" >
        dojo.ready(load);

        function levelName(level) {
            switch(level) {
                case 100: return "Community";break;
                case 200: return "Professional"; break;
                case 300: return "Enterprise"; break;
                case 400: return "Prime"; break;
                default: return "-";
            }
        }
        function typeName(type) {
            switch(type) {
                case "dev": return "Development"; break;
                case "prod": return "Production"; break;
                case "trial": return "Trial"; break;
                default: return "-";
            }
        }

        var currentServerId='<%= serverId %>';

        function load() {
            dojo.empty("repotableBody");
            dojo.xhrGet({
                url: "/api/license/all/",
                handleAs: "json",
                load: function(data) {
                    dojo.forEach(data, function(lic) {
                        var row;

                        if(lic.serverid===currentServerId) {
                            row=dojo.create("tr",{"class":"current_server_row"},dojo.byId("repotableBody"),"first");
                        }
                        else {
                            row=dojo.create("tr",null,dojo.byId("repotableBody"));
                        }

                        var serial=lic.id;
                        var optd=dojo.create("td",null,row);

                        if(lic.serverid===currentServerId) {
                            dojo.create("span",{"class":"unlockIcon", title:"<%= LanguageUtil.get(pageContext, "license-tip-free") %>"},
                                    dojo.create("a",{href:"javascript:free()"},optd));
                        }
                        else if(lic.available) {

                            dojo.create("span",{"class":"downloadIcon",title:"<%= LanguageUtil.get(pageContext, "license-tip-pick") %>"},
                                    dojo.create("a",{href:"javascript:pick('"+serial+"')"},optd));

                            dojo.create("span",{"class":"deleteIcon", title:"<%= LanguageUtil.get(pageContext, "license-tip-del") %>"},
                                    dojo.create("a",{href:"javascript:del('"+serial+"')"},optd));
                        }

                        dojo.create("td",{ innerHTML: lic.id}, row);
                        dojo.create("td",{ innerHTML: (!lic.serverid || lic.serverid==="") ? "Available" :
                                lic.serverid+(lic.available ? " (Available)":"")}, row);
                        dojo.create("td",{ innerHTML: !lic.available || lic.serverid ? lic.lastping : ""}, row);
                        dojo.create("td",{ innerHTML: lic.perpetual ? "Perpetual" : lic.validUntil}, row);
                        dojo.create("td",{ innerHTML: levelName(lic.level)}, row);
                        dojo.create("td",{ innerHTML: typeName(lic.licenseType)}, row);
                    });
                }
            });
        }

        function del(serial) {
            if(!confirm('<%= LanguageUtil.get(pageContext, "license-repo-confirm-delete") %>')) return;

            dojo.xhrDelete({
                url: "/api/license/delete/id/"+serial+"/",
                load: load
            });
        }

        function pick(serial) {
            if(!confirm('<%= LanguageUtil.get(pageContext, "license-repo-confirm-pick") %>')) return;

            dojo.xhrPost({
                url: "/api/license/pick/serial/"+serial+"/",
                load: function() {
                    document.location.reload(true);
                }
            });
        }

        function free() {
            if(!confirm('<%= LanguageUtil.get(pageContext, "license-repo-confirm-free") %>')) return;

            dojo.xhrPost({
                url: "/api/license/free/",
                load: function() {
                    document.location.reload(true);
                }
            });
        }
    </script>

    <table id="repotable" class="listingTable">

        <thead>
        <tr>
            <th>&nbsp;</th>
            <th><%= LanguageUtil.get(pageContext, "license-repo-serial") %></th>
            <th><%= LanguageUtil.get(pageContext, "license-repo-serverid") %></th>
            <th><%= LanguageUtil.get(pageContext, "license-repo-last-ping") %></th>
            <th><%= LanguageUtil.get(pageContext, "license-repo-validuntil") %></th>
            <th><%= LanguageUtil.get(pageContext, "license-repo-level") %></th>
            <th><%= LanguageUtil.get(pageContext, "license-repo-type") %></th>
        </tr>
        </thead>
        <tbody id="repotableBody">

        </tbody>
    </table>
    <button data-dojo-type="dijit.form.Button" onClick="load()" iconClass="resetIcon">
        <%= LanguageUtil.get(pageContext, "license-repo-refresh-button") %>
    </button>

    <% } %>


    </div>
</div>
</div>
</div>
<% } /* badId */ %>
