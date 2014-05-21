<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.List"%>
<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="java.util.Date"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="java.text.SimpleDateFormat"%>

<%
String error=null;
String message=null;
if (request.getMethod().equalsIgnoreCase("POST") ) {
    error=LicenseUtil.processForm(request);
    

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

<script type="text/javascript">

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
    dojo.style("loadpack", "display", "none");
    
    
    if(dijit.byId("pasteRadio").checked){
        dojo.style("pasteMe", "display", "");
    }
    else if(dijit.byId("reqcodeRadio").checked) {
        dojo.style("licensedata", "display", "");
    }
    else if(dijit.byId("loadLicensePack").checked) {
        dojo.style("loadpack", "display", "");
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

<% if(isCommunity) { %>
    dojo.addOnLoad(function() {
        dijit.byId('reqcodeRadio').set('checked','true');
        dijit.byId("license_type").set("value","trial");
        toggleLevel();

    });    
<% } %>

</script>

<style type="text/css">
#contracts-table tbody tr:hover {
    background-color:#D8F6CE;
    cursor: pointer;
}
</style>

<div class="portlet-wrapper">


    <div style="min-height:400px;" id="borderContainer" class="shadowBox headerBox">                            
        <div style="padding:7px;">
            <div>
                <h3><%= LanguageUtil.get(pageContext, "com.dotcms.repackage.portlet.javax.portlet.title.EXT_LICENSE_MANAGER") %></h3>
            </div>
                <br clear="all">
        </div>
            
            <div style="margin-left:auto;margin-right:auto;width:600px;background:#eee;" class="callOutBox">
                Server ID: <%= LicenseUtil.getDisplayServerId() %>
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
            
        <form name="query" id="uploadLicenseForm" action="<%= com.dotmarketing.util.PortletURLUtil.getRenderURL(request,null,null,"EXT_LICENSE_MANAGER") %>" method="post" onsubmit="return false;">
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
                        <dd><%=  UtilMethods.isSet(LicenseUtil.getClientName()) ? UtilMethods.isSet(LicenseUtil.getClientName()) : "No License Found" %></dd>
                        <dt><%= LanguageUtil.get(pageContext, "license-type") %></dt>
                        <dd><%= LicenseUtil.getLevelName() %></dd>
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
            <div style="margin:auto;width:600px;padding:20px;padding-top:0px;">
                <dl style="padding:20px;">
                    <%if(isCommunity){ %>
                        <dt><%= LanguageUtil.get(pageContext, "I-want-to") %>:</dt>
                        <dd>
                            
                            <input onChange="doShowHideRequest()" type="radio" checked="false" name="iwantTo" id="reqcodeRadio"  dojoType="dijit.form.RadioButton" value="request_code">
                            <label for="reqcodeRadio"><%= LanguageUtil.get(pageContext, "request-code-for-support-portal") %></label><br/>


                            <input onChange="doShowHideRequest()"  type="radio" name="iwantTo" id="pasteRadio"  dojoType="dijit.form.RadioButton" value="paste_license">
                            <label for="pasteRadio"><%= LanguageUtil.get(pageContext, "I-already-have-a-license") %></label><br/>

                            <input onChange="doShowHideRequest()" type="radio" checked="false" name="iwantTo" id="loadLicensePack"  dojoType="dijit.form.RadioButton" value="load_license_pack">
                            <label for="loadLicensePack"><%= LanguageUtil.get(pageContext, "load-license-pack") %></label>

                        </dd>
                    <%} else { %>
                            <input type="hidden" name="iwantTo" value="paste_license"/> 
                    <%}%>
                    <dt>
                    
                    <dd id="pasteMe" style="<%if(isCommunity){ %>display:none<%} %>">
                        <b><%= LanguageUtil.get(pageContext, "paste-your-license") %></b>:<br><textarea rows="10" cols="60"  name="license_text" ></textarea>
                        <div style="padding:10px;">
                            <button type="button" onclick="doPaste()" id="uploadButton" dojoType="dijit.form.Button" name="upload_button" iconClass="keyIcon" value="upload"><%= LanguageUtil.get(pageContext, "save-license") %></button>      
                        </div>
                    </dd>


                    <dd id="licensedata" style="display:none">
                        <label for="license_type"><%= LanguageUtil.get(pageContext, "request-license-type") %></label>
                        <select id="license_type" name="license_type" dojoType="dijit.form.Select" onChange="toggleLevel()">
                            <option value="trial"><%= LanguageUtil.get(pageContext, "request-license-trial") %></option>
                            <option value="prod"><%= LanguageUtil.get(pageContext, "request-license-prod") %></option>
                            <option value="dev"><%= LanguageUtil.get(pageContext, "request-license-dev") %></option>
                        </select>
                        <br/>
                        <label for="license_level"><%= LanguageUtil.get(pageContext, "request-license-level") %></label>
                        <select id="license_level" name="license_level" dojoType="dijit.form.Select">
                            <option value="200"><%= LanguageUtil.get(pageContext, "request-license-standard") %></option>
                            <option value="300"><%= LanguageUtil.get(pageContext, "request-license-professional") %></option>
                            <option value="400"><%= LanguageUtil.get(pageContext, "request-license-prime") %></option>
                        </select>
                        <div style="padding:10px;">
                        <button type="button" onclick="doCodeRequest()" id="codereqButton" 
                         dojoType="dijit.form.Button" name="codereqButton" iconClass="keyIcon" value="upload"><%= LanguageUtil.get(pageContext, "request-license-code") %> </button>
                        </div>
                    </dd>


                    </dt>
                    
                </dl>
            </div>
        </form>

        <form id="licensePackForm" action="<%= com.dotmarketing.util.PortletURLUtil.getRenderURL(request,null,null,"EXT_LICENSE_MANAGER") %>" method="post" onsubmit="return false;">
            <div id="loadpack" style="margin:auto;width:600px;padding:20px;padding-top:0px;display:none;" >
               <dl>
                <dt>
                <dd>
                    <input type="file" name="file" accept=".zip"/> 
                    <button type="submit" dojoType="dijit.form.Button" iconClass="keyIcon" value="upload" onClick="doUploadLicensePack()">
                        <%=LanguageUtil.get(pageContext,"upload-license-pack")%>
                    </button>
                <dd>
                </dt>
               <dl>
            </div>
        </form>

    </div>
</div>  


