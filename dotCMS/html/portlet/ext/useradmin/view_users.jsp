<%@ include file="/html/portlet/ext/useradmin/init.jsp" %>
<%@ include file="/html/portlet/ext/remotepublish/init.jsp" %>

<%@page import="com.dotmarketing.util.Config"%>
<%@ page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint" %>
<%@ page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI" %>
<%@ page import="com.dotcms.enterprise.LicenseUtil" %>

<%
    int additionalVariablesCount = Config.getIntProperty("MAX_NUMBER_VARIABLES_TO_SHOW", 0);
    String[] additionalVariableLabels = new String[additionalVariablesCount + 1];
    for(int i = 1; i <= additionalVariablesCount; i++) {
        additionalVariableLabels[i] = LanguageUtil.get(pageContext, "user.profile.var"+i);
    }

%>

<script type="text/javascript">
    <% Boolean enterprise = (LicenseUtil.getLevel() > 199); %>
    var enterprise = <%=enterprise%>;
    <%
    PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();
    List<PublishingEndPoint> sendingEndpoints = pepAPI.getReceivingEndPoints();
    Boolean endPoints = UtilMethods.isSet(sendingEndpoints) && !sendingEndpoints.isEmpty();
    %>
    var sendingEndpoints = <%=endPoints%>;
</script>
<%@ include file="/html/portlet/ext/useradmin/view_users_js_inc.jsp" %>



<style type="text/css">
    @import url("/html/portlet/ext/useradmin/view_users.css");
    <%request.setAttribute("ViewingUserRole", true); %>
    <%@ include file="/html/portlet/ext/roleadmin/view_role_permissions.css" %>
</style>

<div class="buttonBoxLeft">
    <span style="vertical-align:middle;"><%= LanguageUtil.get(pageContext, "Filter") %>:</span>
    <input dojoType="dijit.form.TextBox" onkeyup="filterUsers()" trim="true" name="usersFilter" id="usersFilter" />
    <button dojoType="dijit.form.Button" onclick="clearUserFilter()" type="button" iconClass="resetIcon"><%= LanguageUtil.get(pageContext, "Clear") %></button>
</div>

<div class="buttonBoxRight">
    <button dojoType="dijit.form.Button" type="button" onclick="addUser()" iconClass="plusIcon"><%= LanguageUtil.get(pageContext, "Add-User") %></button>

    <% if ( enterprise ) {%>
        <% if ( endPoints ) {%>
            <button dojoType="dijit.form.Button" type="button" onclick="remotePublishUsers()" iconClass="sServerIcon"><%= LanguageUtil.get(pageContext, "Remote-Publish") %></button>
        <%}%>
    <button dojoType="dijit.form.Button" type="button" onclick="addToBundleUsers()" iconClass="bundleIcon"><%= LanguageUtil.get(pageContext, "Add-To-Bundle") %></button>
    <%}%>

</div>


<div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" id="borderContainer" class="shadowBox headerBox" style="white-space:nowrap;height:400px;">



<!-- START Left Column User listing -->
<div dojoType="dijit.layout.ContentPane" splitter="false" region="leading" style="width: 350px;margin-top:38px;overflow:auto;" class="lineRight">

    <div id="usersGrid"></div>
    <div id="loadingUsers"><img src="/html/js/lightbox/images/loading.gif"></div>
    <div class="clear"></div>
    <div class="inputCaption" style="padding:3px 0 10px 10px;"><%= LanguageUtil.get(pageContext, "Limit-Max-50-Results") %></div>

</div>
<!-- END Left Column User listing -->

<!-- START Right Column User Details -->
<div dojoType="dijit.layout.ContentPane" splitter="true" region="center">

<div id="loadingUserProfile" style="display: none;"><img src="/html/js/lightbox/images/loading.gif"></div>

<div id="userProfileTabs">

<!-- START User Tabs -->
<div dojoType="dijit.layout.TabContainer" id="userTabsContainer">

<!-- START User Detail Tab -->
<div style="overflow-y:auto;" dojoType="dijit.layout.ContentPane" id="userDetailsTab" title="<%= LanguageUtil.get(pageContext, "User-Details") %>">

    <div class="yui-g nameHeader">
        <div class="yui-u first">
            <span id="fullUserName" class="fullUserName"></span>
        </div>
    </div>

    <div style="padding:0 0 10px 0; border-bottom:1px solid #ccc;">
        <form id="userInfoForm" dojoType="dijit.form.Form">
            <input type="hidden" name="userPasswordChanged" value="false"/>
            <dl>
                <% if(authByEmail) { %>
                <dt id="userIdLabel"><%= LanguageUtil.get(pageContext, "User-ID") %>: <input type="hidden" id="userId" name="userId" value=""/></dt>
                <dd id="userIdValue"></dd>
                <% } else {%>
                <dt id="userIdLabel"><%= LanguageUtil.get(pageContext, "User-ID") %>:</dt>
                <dd id="userIdValue"><input id="userId" type="text" onkeyup="userInfoChanged()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" disabled="disabled" /></dd>
                <% } %>
                <dt><%= LanguageUtil.get(pageContext, "First-Name") %>:</dt>
                <dd><input id="firstName" type="text" onkeyup="userInfoChanged()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
                <dt><%= LanguageUtil.get(pageContext, "Last-Name") %>:</dt>
                <dd><input id="lastName" type="text" onkeyup="userInfoChanged()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
                <dt><%= LanguageUtil.get(pageContext, "Email-Address") %>:</dt>
                <dd><input id="emailAddress" type="text" onkeyup="userInfoChanged()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
                <dt><%= LanguageUtil.get(pageContext, "Password") %>:</dt>
                <dd><input id="password" type="password" onkeyup="userPasswordChanged()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
                <dt><%= LanguageUtil.get(pageContext, "Password-Again") %>:</dt>
                <dd><input id="passwordCheck" type="password" onkeyup="userPasswordChanged()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
            </dl>
        </form>
    </div>
    <div class="clear"></div>
    <div class="buttonRow">
        <button dojoType="dijit.form.Button" onclick="saveUserDetails()" type="button" iconClass="saveIcon"><%= LanguageUtil.get(pageContext, "Save") %></button>
    </div>

</div>
<!-- END User Detail Tab -->

<!-- START Roles Tab -->
<div style="overflow-y:auto;" dojoType="dijit.layout.ContentPane" id="userRolesTab" title="<%= LanguageUtil.get(pageContext, "Roles") %>">

    <div class="yui-g nameHeader">
        <div class="yui-u first">
            <span id="fullUserName" class="fullUserName"></span>
        </div>
    </div>

    <div id=userRolesContainer style="overflow: hidden; height: 80%; ">
        <div  style="width: 45%; height: 100%; float:left; padding-left:8px; ">
            <div style="text-align: center; "><%= LanguageUtil.get(pageContext, "Roles-To-Grant") %>:</div>
            <div id="userRolesTreeWrapper" style="float:left; width: 100%; height:85%;display: none; padding:5px; border:1px solid #ccc;">

                <div id="userRolesTree"></div>
            </div>
        </div>
        <div id="actionsDiv" style="width: 6%; text-align:center;  float:left; height:80%; margin-left: 2%; margin-right:1%; margin-top: 15%; ">
            <div style="">
                <button id="addUserRoleBtn" dojoType="dijit.form.Button" onclick="addUserRoles()" type="button" disabled="disabled">&#62;&#62;</button>
            </div>
            <div style="margin-top:5px">
                <button id="removeUserRoleBtn" dojoType="dijit.form.Button" onclick="removeUserRoles()" type="button" disabled="disabled">&#60;&#60;</button>
            </div>
        </div>
        <div style="height:100%; width:45%;">
            <div style="text-align: center; padding-left: 160%"><%= LanguageUtil.get(pageContext, "Roles-Granted") %>:</div>
            <div id="userRolesSelectWrapper" style="width: 100%;height:87%;"></div>
        </div>
    </div>

    <div id="loadingRolesWrapper">
        <img src="/html/js/lightbox/images/loading.gif">
    </div>

    <div id="noRolesFound" style="display: none;">
        <%= LanguageUtil.get(pageContext, "No-roles-found") %>
    </div>



    <div class="buttonRow">
        <button dojoType="dijit.form.Button" onclick="resetRoles()" type="button" iconClass="resetIcon"><%= LanguageUtil.get(pageContext, "Reset") %></button>
        <button dojoType="dijit.form.Button" onclick="saveRoles()" type="button" iconClass="saveIcon"><%= LanguageUtil.get(pageContext, "Save") %></button>
    </div>

</div>
<!-- END Roles Tab -->

<!-- START Permissions Tab -->
<div style="overflow-y:auto;" dojoType="dijit.layout.ContentPane" id="userPermissionsTab" title="<%= LanguageUtil.get(pageContext, "Permissions") %>">
    <br />
    <br />
    <%@ include file="/html/portlet/ext/roleadmin/view_role_permissions_inc.jsp" %>
</div>
<!-- END Permissions Tab -->

<!-- START Additional Info Tab -->
<div style="overflow-y:auto;" dojoType="dijit.layout.ContentPane" id="userAdditionalInfoTab" title="<%= LanguageUtil.get(pageContext, "Additional-Info") %>">

    <div class="yui-g nameHeader">
        <div class="yui-u first">
            <span id="fullUserName" class="fullUserName"></span>
        </div>
    </div>

    <div class="userInfoForm" id="additionalUserInfoFormWrapper"  style="border-bottom:1px solid #ccc;">
        <form id="userAdditionalInfoForm" dojoType="dijit.form.Form">
            <dl>
                <dt><%= LanguageUtil.get(pageContext, "Active") %>:</dt>
                <dd><input id="userActive" type="checkbox" onkeyup="userInfoChanged()" checked="checked" dojoType="dijit.form.CheckBox" /></dd>
                <dt><%= LanguageUtil.get(pageContext, "Prefix") %>:</dt>
                <dd><input id="prefix" type="text" onkeyup="userInfoChanged()" value="" dojoType="dijit.form.TextBox" /></dd>
                <dt><%= LanguageUtil.get(pageContext, "Suffix") %>:</dt>
                <dd><input id="suffix" type="text" onkeyup="userInfoChanged()" value="" dojoType="dijit.form.TextBox" /></dd>
                <dt><%= LanguageUtil.get(pageContext, "Title") %>:</dt>
                <dd><input id=title type="text" onkeyup="userInfoChanged()" value="" dojoType="dijit.form.TextBox" /></dd>
                <dt><%= LanguageUtil.get(pageContext, "Company") %>:</dt>
                <dd><input id="company" type="text" onkeyup="userInfoChanged()" value="" dojoType="dijit.form.TextBox" /></dd>
                <dt><%= LanguageUtil.get(pageContext, "Website") %>:</dt>
                <dd><input id="website" type="text" onkeyup="userInfoChanged()" value="" dojoType="dijit.form.TextBox" /></dd>
                <% for (int i = 1; i <= additionalVariablesCount; i++) { %>
                <dt id="var<%=i%>Label"><%=additionalVariableLabels[i]%>:</dt>
                <dd id="var<%=i%>Value"><input id="var<%=i%>" type="text" onkeyup="userInfoChanged()" value="" dojoType="dijit.form.TextBox" /></dd>
                <% } %>
            </dl>
        </form>

        <hr/>
        <div class="clear"></div>

        <div class="buttonRow" style="text-align:right;">
            <button dojoType="dijit.form.Button" id="addAddressIcon" onclick="addAddress()" type="button" iconClass="plusIcon">
                <%= LanguageUtil.get(pageContext, "Addresses-Phones") %>
            </button>
        </div>

        <div dojoType="dojox.grid.DataGrid" jsId="userAddressesGrid" id="userAddressesGrid" autoHeight="true" store="userAddressesStore" structure="userAddressesGridLayout" query="{ addressId: '*' }"> </div>

    </div>

    <div class="clear"></div>

    <div class="buttonRow">
        <button dojoType="dijit.form.Button" onclick="saveUserAdditionalInfo()" type="button" iconClass="saveIcon"><%= LanguageUtil.get(pageContext, "Save") %></button>
    </div>


    <!-- START Popup Address -->
    <div id="addressDialog" title="<%= LanguageUtil.get(pageContext, "edit-address") %>" dojoType="dijit.Dialog" style="display: none; width:400px;">
        <form id="addressForm" dojoType="dijit.form.Form" class="userInfoForm">
            <input id="addressId" type="hidden" />
            <dl>
                <dt><%= LanguageUtil.get(pageContext, "Description") %>:</dt>
                <dd><input id="addressDescription" type="text" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
                <dt><%= LanguageUtil.get(pageContext, "Street-1") %>:</dt>
                <dd><input id="addressStreet1" type="text" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
                <dt><%= LanguageUtil.get(pageContext, "Street-2") %>:</dt>
                <dd><input id="addressStreet2" type="text" dojoType="dijit.form.ValidationTextBox" /></dd>
                <dt><%= LanguageUtil.get(pageContext, "City") %>:</dt>
                <dd><input id="addressCity" type="text" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
                <dt><%= LanguageUtil.get(pageContext, "State") %>:</dt>
                <dd><input id="addressState" type="text" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
                <dt><%= LanguageUtil.get(pageContext, "Zip") %>:</dt>
                <dd><input id="addressZip" type="text" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
                <dt><%= LanguageUtil.get(pageContext, "Country") %>:</dt>
                <dd><input id="addressCountry" type="text" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
                <dt><%= LanguageUtil.get(pageContext, "Phone") %>:</dt>
                <dd><input id="addressPhone" type="text" dojoType="dijit.form.ValidationTextBox" /></dd>
                <dt><%= LanguageUtil.get(pageContext, "Fax") %>:</dt>
                <dd><input id="addressFax" type="text" dojoType="dijit.form.ValidationTextBox" /></dd>
                <dt><%= LanguageUtil.get(pageContext, "Cell") %>:</dt>
                <dd><input id="addressCell" type="text" dojoType="dijit.form.ValidationTextBox" /></dd>
            </dl>
            <div class="clear"></div>
            <div class="buttonRow">
                <button dojoType="dijit.form.Button" type="button" onClick="saveAddress()" iconClass="saveIcon"><%= LanguageUtil.get(pageContext, "Save") %></button>
                <button dojoType="dijit.form.Button" type="button" onClick="cancelSaveAddress()" iconClass="cancelIcon"><%= LanguageUtil.get(pageContext, "Cancel") %></button>
            </div>
        </form>
    </div>
    <!-- END Popup Address -->

</div>
<!-- END Additional Info Tab -->

<!-- START Marketing Tab -->
<div style="overflow-y:auto;" dojoType="dijit.layout.ContentPane" id="marketingInfoTab" title="<%= LanguageUtil.get(pageContext, "Marketing") %>">

    <div class="yui-g nameHeader">
        <div class="yui-u first">
            <span id="fullUserName" class="fullUserName"></span>
        </div>
    </div>

    <div id="marketingInfoWrapper">

        <table class="listingTable" style="margin:10px;width:98%;">
            <tr>
                <th>
                    <div style="float:left;">
                        <span class="statisticsIcon"></span> <%= LanguageUtil.get(pageContext, "Click-Tracking") %>
                    </div>
                    <div style="float:right;font-weight:normal;">
                        <input type="checkbox" dojoType="dijit.form.CheckBox" name="userClickTrackingCheck" id="userClickTrackingCheck" onClick="userClicktrackingChanged()" />
                        <label for="userClickTrackingCheck"><%= LanguageUtil.get(pageContext, "Disable-click-tracking") %></label>
                    </div>
                    <div class="clear"></div>
                </th>
            </tr>
            <tr>
                <td align="center">
                    <button dojoType="dijit.form.Button" onClick="viewFullClickHistory();" iconClass="statisticsIcon"><%= LanguageUtil.get(pageContext, "Full-Visit-History") %></button>
                </td>
            </tr>
        </table>

        <table class="listingTable" style="margin:10px;width:98%;">
            <tr><th nowrap><span class="tagIcon"></span> <%= LanguageUtil.get(pageContext, "User-Tags") %></th></tr>
            <tr><td nowrap><%@ include file="/html/portlet/ext/useradmin/view_users_tags_inc.jsp" %></td></tr>
        </table>

        <!-- DOTCMS-4943 <table class="listingTable" style="margin:10px;width:98%;">
								<tr><th nowrap><span class="mapPinIcon"></span> <%= LanguageUtil.get(pageContext, "User-Locale") %></th></tr>
								<tr><td style="font-size:100%;"><%@ include file="/html/portlet/ext/useradmin/view_users_locale_inc.jsp" %></td></tr>
							</table>
				-->
        <div id="userClickHistoryDialog" title="<%= LanguageUtil.get(pageContext, "user-clicks-history") %>" dojoType="dijit.Dialog" style="display: none">
            <div id="userClickHistoryPane"></div>
            <div id="userClickHistoryDetailPane" style="display:none; overflow:auto;"></div>
        </div>
    </div>
</div>
<!-- END Marketing Tab -->



</div>
<!-- END User Tabs -->

</div>

</div>
<!-- END Right Column User Details -->

</div>

<form id="remotePublishForm">
    <input name="assetIdentifier" id="assetIdentifier" type="hidden" value="">
    <input name="remotePublishDate" id="remotePublishDate" type="hidden" value="">
    <input name="remotePublishTime" id="remotePublishTime" type="hidden" value="">
    <input name="remotePublishExpireDate" id="remotePublishExpireDate" type="hidden" value="">
    <input name="remotePublishExpireTime" id="remotePublishExpireTime" type="hidden" value="">
    <input name="remoteFilterDate" id="remoteFilterDate" type="hidden" value="">
    <input name="iWantTo" id=iWantTo type="hidden" value="">
    <input name="whoToSend" id=whoToSend type="hidden" value="">
	<input name="bundleName" id=bundleName type="hidden" value="">
	<input name="bundleSelect" id=bundleSelect type="hidden" value="">
	<input name="forcePush" id=forcePush type="hidden" value="">
</form>

<div dojoType="dijit.Menu" id="usersGrid_rowMenu" jsId="usersGrid_rowMenu" style="display: none;">
    <% if ( endPoints ) {%>
        <div dojoType="dijit.MenuItem" iconClass="sServerIcon" onClick="remotePublishUser"><%=LanguageUtil.get(pageContext, "Remote-Publish") %></div>
    <%}%>
    <div dojoType="dijit.MenuItem" iconClass="bundleIcon" onClick="addToBundleUser"><%=LanguageUtil.get(pageContext, "Add-To-Bundle") %></div>
</div>

<script type="text/javascript">

    dojo.addOnLoad(function () {
        var userId='<%= request.getParameter("user_id")%>';
        if(userId!='null'){
            editUser(userId);
        }
    });

    resizeRoleBrowser();

</script>