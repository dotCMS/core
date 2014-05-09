<%@page import="com.dotcms.enterprise.license.LicenseManager"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.List"%>
<%@page import="com.dotcms.enterprise.license.LicenseManager.Contract"%>
<%
String error=null;
String message=null;
if (request.getMethod().equalsIgnoreCase("POST") ) {
    error=LicenseUtil.processForm(request);
    

}


boolean isCommunity = ("100".equals(System.getProperty("dotcms_level")));

String expireString = "unknown";
Date expires = null;
try{
    expires = new Date(Long.parseLong(System.getProperty("dotcms_valid_until")));
    SimpleDateFormat format =
        new SimpleDateFormat("MMMM d, yyyy");
    expireString=  format.format(expires);
}
catch(Exception e){
    
}
boolean expired = (expires !=null && expires.before(new Date()));
boolean isPerpetual = System.getProperty("dotcms_license_perpetual").equals("true");

String requestCode=(String)request.getAttribute("requestCode");

List<Contract> contracts=(List<Contract>)request.getAttribute("contracts");

SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
SimpleDateFormat dfOut = new SimpleDateFormat("MMM dd, yyyy");

%>

<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="java.util.Date"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotcms.enterprise.LicenseUtil"%>
<%@page import="java.text.SimpleDateFormat"%>
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
    dojo.style("requestMe", "display", "none");
    dojo.style("licensedata", "display", "none");
    dojo.style("userauth", "display", "none");
    
    
    if(dijit.byId("pasteRadio").checked){
        dojo.style("pasteMe", "display", "");
    }
    else if(dijit.byId("requestRadio").checked) {
        dojo.style("requestMe", "display", "");
    }
    else if(dijit.byId("reqcodeRadio").checked) {
        dojo.style("licensedata", "display", "");
    }
    else if(dijit.byId("reqonlineRadio").checked) {
        dojo.style("userauth", "display", "");
        <% if(contracts!=null) { %>
            dojo.style("licensedata", "display", "");

        <% } %>
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
                Server ID: <%= LicenseManager.getInstance().getDisplayServerId() %>
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
                        <dd><%= System.getProperty("dotcms_level_name")  %>
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
                        <dd><%=  UtilMethods.isSet(System.getProperty("dotcms_license_client_name")) ? System.getProperty("dotcms_license_client_name") + "": "No License Found" %></dd>
                        <dt><%= LanguageUtil.get(pageContext, "license-type") %></dt>
                        <dd><%=System.getProperty("dotcms_license_type_name")%></dd>
                        <dt><%= LanguageUtil.get(pageContext, "license-serial") %></dt>
                        <dd><%=System.getProperty("dotcms_license_serial")%></dd>
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
                            
                            <input onChange="doShowHideRequest()" type="radio" checked="true" name="iwantTo" id="requestRadio"  dojoType="dijit.form.RadioButton" value="request_trial" style="display:none">
                            <label for="requestRadio" style="display:none"><%= LanguageUtil.get(pageContext, "request-trial-license") %></label>

                            <input onChange="doShowHideRequest()" type="radio" checked="false" name="iwantTo" id="reqonlineRadio"  dojoType="dijit.form.RadioButton" value="request_online" style="display:none">
                            <label for="reqonlineRadio" style="display:none"><%= LanguageUtil.get(pageContext, "request-online-to-support-portal") %></label>

                            <input onChange="doShowHideRequest()" type="radio" checked="false" name="iwantTo" id="reqcodeRadio"  dojoType="dijit.form.RadioButton" value="request_code">
                            <label for="reqcodeRadio"><%= LanguageUtil.get(pageContext, "request-code-for-support-portal") %></label><br/>


                            <input onChange="doShowHideRequest()"  type="radio" name="iwantTo" id="pasteRadio"  dojoType="dijit.form.RadioButton" value="paste_license">
                            <label for="pasteRadio"><%= LanguageUtil.get(pageContext, "I-already-have-a-license") %></label><br/>

                        </dd>
                    <%} else { %>
                            <input type="hidden" name="iwantTo" value="paste_license"/> 
                    <%}%>
                    <dt>
                    <dd id="requestMe" style="display:none">
                        <button  id="requestTrialButton" iconClass="keyIcon" onclick="requestTrial()"  dojoType="dijit.form.Button" value="request_trial"><%= LanguageUtil.get(pageContext, "request-trial-license") %></button>
                        <br />&nbsp;<br><%= LanguageUtil.get(pageContext, "license-you-request-will-automatically-be-downloaded-and-installed") %>
                    
                    </dd>
                    
                    <dd id="pasteMe" style="<%if(isCommunity){ %>display:none<%} %>">
                        <b><%= LanguageUtil.get(pageContext, "paste-your-license") %></b>:<br><textarea rows="10" cols="60"  name="license_text" ></textarea>
                        <div style="padding:10px;">
                            <button type="button" onclick="doPaste()" id="uploadButton" dojoType="dijit.form.Button" name="upload_button" iconClass="keyIcon" value="upload"><%= LanguageUtil.get(pageContext, "save-license") %></button>      
                        </div>
                    </dd>

                    <dd id="userauth" style="display:none">
                        <p><%= LanguageUtil.get(pageContext, "request-license-online-description")%></p>

                        <%if(contracts!=null) {%>
                            <table border=1 id="contracts-table">
                                <thead>
                                    <tr>
                                        <th></th>
                                        <th>Level</th>
                                        <th>Start</th>
                                        <th>End</th>
                                        <th>Perpetual</th>
                                        <th>Site</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <%for(Contract con : contracts) {%>
                                        <tr>
                                            <td><input type="radio" name="contractId" value="<%=con.getId()%>" dojoType="dijit.form.RadioButton"/></td>
                                            <td><%=con.getLevel()%></td>
                                            <td><%=dfOut.format(df.parseObject(con.getStart()))%></td>
                                            <td><%=dfOut.format(df.parseObject(con.getEnd()))%></td>
                                            <td><%=con.isPerpetual()%></td>
                                            <td><%=con.isSite()%></td>
                                        </tr>
                                    <%}%>
                                </tbody>
                            </table> 
                            <input type="hidden" name="support_user" value="<%=request.getParameter("support_user")%>"/>
                            <input type="hidden" name="support_pass" value="<%=request.getParameter("support_pass")%>"/>
                        <%} else {%>
                            <label for="support_user"><%= LanguageUtil.get(pageContext, "request-license-user") %></label>
                            <input type="text" dojoType="dijit.form.TextBox" name="support_user" id="support_user"/>
                            <br/>
                            <label for="support_pass"><%= LanguageUtil.get(pageContext, "request-license-pass") %></label>
                            <input type="password" dojoType="dijit.form.TextBox" name="support_pass" id="support_pass"/>

                            <div style="padding:10px;">
                                <button type="button" onclick="doCodeRequest()" id="onlinereqButton" dojoType="dijit.form.Button" name="onlinereqButton" iconClass="keyIcon" value="upload"><%= LanguageUtil.get(pageContext, "request-online-contracts") %></button>      
                            </div>
                        <%}%>
                        
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
                        <% if(contracts==null) {%>
                            
                            <button type="button" onclick="doCodeRequest()" id="codereqButton" 
                             dojoType="dijit.form.Button" name="codereqButton" iconClass="keyIcon" value="upload"><%= LanguageUtil.get(pageContext, "request-license-code") %> </button>      
                            
                        <%} else {%>
                            <button type="button" onclick="doOnlineLicenseRequest()" id="codereqButton" 
                             dojoType="dijit.form.Button" name="codereqButton" iconClass="keyIcon" value="upload"><%= LanguageUtil.get(pageContext, "request-license-online") %> </button>      
                        <%}%>
                        </div>
                    </dd>

                    

                    </dt>
                    
                </dl>
            </div>
        </form>

    </div>
</div>  


