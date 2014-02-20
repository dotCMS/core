<%@ include file="/html/portlet/ext/cmsconfig/init.jsp" %>
<%@ include file="/html/common/uservalidation.jsp"%>

<%@page import="com.dotmarketing.business.APILocator"%>

<script type="text/javascript">

    dojo.require("dojox.widget.ColorPicker");
    dojo.require("dojo.parser");// scan page for widgets and instantiate them

    function styler (val) {
        dojo.byId("colorBlock").style.background = val;
        dojo.byId("bgColor").value = val;
    }

    function imgSwap (val) {
        dojo.byId("imageBlock").style.backgroundImage = "url('" + val + "')";
        dojo.byId("bgURL").value = val;
    }

    dojo.addOnLoad(function () {
        styler('<%= company.getSize() %>');
        imgSwap('<%= company.getHomeURL() %>');
    });

</script>

<style type="text/css">
    table{font-size:12px;}
    td{font-size:12px;}
    dt{font-size:12px;padding-top:12px;}
</style>

<table class="listingTable shadowBox" style="font-size:12px;">
    <tr>
        <th><%= LanguageUtil.get(pageContext, "basic-information") %></th>
        <th><%= LanguageUtil.get(pageContext, "logo") %></th>
    </tr>
    <tr>
        <td>
            <dl>

                <dt><%= LanguageUtil.get(pageContext, "portal-url") %></dt>
                <dd><input dojoType="dijit.form.TextBox" id="companyPortalUrl" name="companyPortalUrl" size="25" type="text" value="<%= company.getPortalURL() %>"></dd>

                <dt><%= LanguageUtil.get(pageContext, "mail-domain") %></dt>
                <dd><input dojoType="dijit.form.TextBox" id="companyMX" name="companyMX" size="25" type="text" value="<%= company.getMx() %>"></dd>

                <dt><%= LanguageUtil.get(pageContext, "email-address") %></dt>
                <dd><input dojoType="dijit.form.TextBox" id="companyEmailAddress" name="companyEmailAddress" size="20" type="text" value="<%= company.getEmailAddress() %>"></dd>

                <dt>Background Color</dt>
                <dd style="position:relative;">
                    <div id="colorBlock" style="position:absolute;left:143px;border-left:1px solid #b3b3b3;top:9px;width:50px;height:26px;display:inline-block;margin:0 0 -5px 10px;"></div>
                    <input id="bgColor" dojoType="dijit.form.TextBox" name="companySize" size="5" type="text" value="<%= company.getSize() %>">
                    <button id="buttonOne" dojoType="dijit.form.Button" type="button" iconClass="colorIcon">
                        Color Picker
                        <script type="dojo/method" data-dojo-event="onClick" data-dojo-args="evt">
                            dijit.byId("colorPicker").show();
                        </script>
                    </button>
                </dd>

                <dt>Background Image</dt>
                <dd>
                    <input id="bgURL" dojoType="dijit.form.TextBox" name="companyHomeUrl" size="25" type="text" value="<%= company.getHomeURL() %>">
                    <button id="buttonTwo" dojoType="dijit.form.Button" type="button" iconClass="bgIcon">
                        Backgrounds
                        <script type="dojo/method" data-dojo-event="onClick" data-dojo-args="evt">
                            dijit.byId("bgPicker").show();
                        </script>
                    </button>
                </dd>
                <dd>
                    <div id="imageBlock" style="width:250px; height:170px; border:1px solid #b3b3b3;background-repeat:no-repeat; background-size:100% 100%;"></div>
                </dd>

            </dl>

            <div id="colorPicker" data-dojo-type="dijit.Dialog" title="Color Picker">
                <div id="pickerLive" dojoType="dojox.widget.ColorPicker"
                     webSafe="false"
                     liveUpdate="true"
                     value="<%= company.getSize() %>"
                     onChange="styler(arguments[0])">
                </div>
            </div>

            <div id="bgPicker" data-dojo-type="dijit.Dialog" title="Backgrounds">
                <table class="bgThumbnail">
                    <tr>
                        <td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-1.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-1-sm.jpg" width="75" height="47"></a></div></td>
                        <td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-2.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-2-sm.jpg" width="75" height="47"></a></div></td>
                        <td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-3.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-3-sm.jpg" width="75" height="47"></a></div></td>
                        <td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-4.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-4-sm.jpg" width="75" height="47"></a></div></td>
                    </tr>
                    <tr>
                        <td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-5.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-5-sm.jpg" width="75" height="47"></a></div></td>
                        <td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-6.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-6-sm.jpg" width="75" height="47"></a></div></td>
                        <td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-7.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-7-sm.jpg" width="75" height="47"></a></div></td>
                        <td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-8.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-8-sm.jpg" width="75" height="47"></a></div></td>
                    </tr>
                    <tr>
                        <td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-9.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-9-sm.jpg" width="75" height="47"></a></div></td>
                        <td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-10.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-10-sm.jpg" width="75" height="47"></a></div></td>
                        <td><div><a href="#" onclick="imgSwap('/html/images/backgrounds/bg-11.jpg'); dijit.byId('bgPicker').hide();"><img src="/html/images/backgrounds/bg-11-sm.jpg" width="75" height="47"></a></div></td>
                        <td><div><a href="#" onclick="imgSwap('');"> <img src="/html/images/backgrounds/bg-no-sm.jpg" width="75" height="47"></a></div></td>
                    </tr>
                </table>
            </div>

        </td>
        <td valign="middle" align="center" style="border-bottom: none;">
            <img style="max-width: 300px;" border="1" hspace="0" src="<%= IMAGE_PATH %>/company_logo?img_id=<%= company.getCompanyId() %>&key=<%= ImageKey.get(company.getCompanyId()) %>" vspace="0"><br>
            <div class="buttonRow" style="margin-top:30px;">

                <form action="/api/config/saveCompanyLogo" enctype="multipart/form-data" id="companyLogoForm" name="companyLogoForm" method="post">
                    <div style="text-align: center"><%= LanguageUtil.get(pageContext, "File") %>  : <input type="file" id="logoFile" name="logoFile"></div>
                    <br>
                    <div>&nbsp;</div>
                    <div style="text-align: center">
                        <button  dojoType="dijit.form.Button" onClick="uploadCompanyLogo();" iconClass="saveIcon">
                            <%= LanguageUtil.get(pageContext, "upload-image") %>
                        </button>
                    </div>

                </form>

            </div>
        </td>
    </tr>
    <tr>
        <td valign="middle" align="center">
            <div class="buttonRow">
                <button dojoType="dijit.form.Button" onclick="saveCompanyBasicInfo();" type="button" id="basicSubmitButton" iconClass="saveIcon">
                    <%= LanguageUtil.get(pageContext, "save") %>
                </button>
            </div>
        </td>
    </tr>

    <tr>
        <th colspan="2"><%= LanguageUtil.get(pageContext, "locale") %></th>
    </tr>
    <tr>
        <td colspan="2">
            <dl>
                <dt><%= LanguageUtil.get(pageContext, "language") %></dt>
                <dd>
                    <%
                        User defuser=APILocator.getUserAPI().getDefaultUser();
                    %>
                    <select dojoType="dijit.form.FilteringSelect"  value="<%=defuser.getLocale().getLanguage()+ "_" + defuser.getLocale().getCountry()%>"
                            id="companyLanguageId" name="companyLanguageId" style="width: 250px;">
                        <%
                            Locale[] locales = LanguageUtil.getAvailableLocales();
                            for (int i = 0; i < locales.length; i++) {
                        %>
                        <option  value="<%= locales[i].getLanguage() + "_" + locales[i].getCountry() %>"><%= locales[i].getDisplayName(locale) %></option>
                        <%}%>
                    </select>
                </dd>

                <dt><%= LanguageUtil.get(pageContext, "time-zone") %></dt>
                <dd>
                    <span id="userTimezoneWrapper">
                        <select dojoType="dijit.form.FilteringSelect" value="<%=company.getTimeZone().getID() %>"
                                id="companyTimeZoneId" name="companyTimeZoneId" style="width: 250px;">
                            <% String[] ids = TimeZone.getAvailableIDs();
                                Arrays.sort(ids);
                                for(String id : ids) {
                                    TimeZone tmz=TimeZone.getTimeZone(id);%>
                                <option value="<%= id %>"><%= tmz.getDisplayName(locale) %> (<%= tmz.getID() %>) </option>
                            <% }%>
                        </select>
                    </span>
                </dd>
                <dd>
                    <div class="buttonRow" style="float: left;">
                        <button dojoType="dijit.form.Button" onclick="saveCompanyLocaleInfo();" type="button" id="locateSubmitButton" iconClass="saveIcon">
                            <%= LanguageUtil.get(pageContext, "save") %>
                        </button>
                    </div>
                </dd>
            </dl>
        </td>
    </tr>
    <tr>
        <th colspan="2"><%= LanguageUtil.get(pageContext, "security") %></th>
    </tr>
    <tr>
        <td colspan="2">
            <dl>
                <dt><%= LanguageUtil.get(pageContext, "authentication-type") %></dt>
                <dd>
                    <select dojoType="dijit.form.FilteringSelect"  value="<%= company.getAuthType()%>"  id="companyAuthType" name="companyAuthType" style="width: 200px;">
                        <option value="<%= Company.AUTH_TYPE_EA %>"><%= LanguageUtil.get(pageContext, "email-address") %></option>
                        <option value="<%= Company.AUTH_TYPE_ID %>"><%= LanguageUtil.get(pageContext, "user-id") %></option>
                    </select>
                </dd>
                <dd>
                    <div class="buttonRow" style="float: left;">
                        <button dojoType="dijit.form.Button" onclick="saveCompanyAuthTypeInfo();" type="button" id="securitySubmitButton" iconClass="saveIcon">
                            <%= LanguageUtil.get(pageContext, "save") %>
                        </div>
                    </button>
                </dd>
            </dl>
        </td>
    </tr>
</table>