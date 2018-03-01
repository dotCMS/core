<%@ include file="/html/portlet/ext/useradmin/init.jsp" %>
<%@ include file="/html/portlet/ext/remotepublish/init.jsp" %>
<%@ include file="/html/portlet/ext/roleadmin/view_role_permissions_js_inc.jsp" %>

<%@page import="com.dotmarketing.util.Config"%>
<%@ page import="com.dotcms.publisher.endpoint.bean.PublishingEndPoint" %>
<%@ page import="com.dotcms.publisher.endpoint.business.PublishingEndPointAPI" %>
<%@ page import="com.dotcms.enterprise.LicenseUtil" %>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>

<%
	int additionalVariablesCount = Config.getIntProperty("MAX_NUMBER_VARIABLES_TO_SHOW", 0);
	String[] additionalVariableLabels = new String[additionalVariablesCount + 1];
	for(int i = 1; i <= additionalVariablesCount; i++) {
		additionalVariableLabels[i] = LanguageUtil.get(pageContext, "user.profile.var"+i);
	}

	boolean hasAdminRole = com.dotmarketing.business.APILocator.getRoleAPI().doesUserHaveRole(user,com.dotmarketing.business.APILocator.getRoleAPI().loadCMSAdminRole());

%>

<script type="text/javascript">
    <% Boolean enterprise = (LicenseUtil.getLevel() >= LicenseLevel.STANDARD.level); %>
    var enterprise = <%=enterprise%>;
    <%
    PublishingEndPointAPI pepAPI = APILocator.getPublisherEndPointAPI();
    List<PublishingEndPoint> sendingEndpoints = pepAPI.getReceivingEndPoints();
    Boolean endPoints = UtilMethods.isSet(sendingEndpoints) && !sendingEndpoints.isEmpty();
    %>
    var sendingEndpoints = <%=endPoints%>;
</script>
<%@ include file="/html/portlet/ext/useradmin/view_users_js_inc.jsp" %>

<%
	String dojoPath = Config.getStringProperty("path.to.dojo");
%>


<style type="text/css">
	@import url("<%=dojoPath%>/dojox/grid/resources/tundraGrid.css?b=3.7.0");
	<%request.setAttribute("ViewingUserRole", true); %>
</style>

<script language="Javascript">
	/**
		focus on search box
	**/
	require([ "dijit/focus", "dojo/dom", "dojo/domReady!" ], function(focusUtil, dom){
		dojo.require('dojox.timing');
		t = new dojox.timing.Timer(500);
		t.onTick = function(){
		  focusUtil.focus(dom.byId("usersFilter"));
		  t.stop();
		}
		t.start();
	});
</script>

<div dojoType="dijit.layout.BorderContainer" design="sidebar" gutters="false" liveSplitters="true" id="borderContainer" style="white-space:nowrap;" class="view-users">
	<!-- START Left Column User listing -->
	<div dojoType="dijit.layout.ContentPane" splitter="false" region="leading" style="width:350px;" class="portlet-sidebar-wrapper">
		<div class="portlet-sidebar">
			<div class="inline-form view-users__filter-form">
				<input dojoType="dijit.form.TextBox" onkeyup="filterUsers()" trim="true" name="usersFilter" id="usersFilter" placeholder="<%= LanguageUtil.get(pageContext, "Filter") %>:" />
				<button dojoType="dijit.form.Button" onclick="clearUserFilter()" type="button" class="dijitButtonFlat"><%= LanguageUtil.get(pageContext, "Clear") %></button>
			</div>
			<div id="usersGrid"></div>
			<div id="loadingUsers"><img src="/html/js/dojo/custom-build/dojox/widget/Standby/images/loading.gif"></div>
			<div class="clear"></div>
			<div class="inputCaption" style="padding:3px 0 10px 10px;"><%= LanguageUtil.get(pageContext, "Limit-Max-50-Results") %></div>
		</div>
	</div>
	<!-- END Left Column User listing -->

	<!-- START Right Column User Details -->
	<div dojoType="dijit.layout.ContentPane" splitter="true" region="center">
		<div class="portlet-main">
			<div data-dojo-type="dijit/form/DropDownButton" data-dojo-props='iconClass:"actionIcon", class:"dijitDropDownActionButton"' style="position:absolute;top:16px;right:16px;">
				<span></span>
				<div data-dojo-type="dijit/Menu" class="contentlet-menu-actions">
					<div data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: addUser">
						<%= LanguageUtil.get(pageContext, "Add-User") %>
					</div>
					<% if ( enterprise ) {%>
						<% if ( endPoints ) {%>
							<div data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: remotePublishUsers">
								<%= LanguageUtil.get(pageContext, "Remote-Publish") %>
							</div>
						<%}%>
						<div data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: addToBundleUsers">
							<%= LanguageUtil.get(pageContext, "Add-To-Bundle") %>
						</div>
					<%}%>
				</div>
			</div>

			<div id="loadingUserProfile" style="display: none;" class="view-users__loading">
				<div class="view-users__loading-image">
					<img src="/html/images/icons/processing.gif" />
				</div>
			</div>

			<div id="userProfileTabs" class="view-users__profile-tabs">
				<!-- START User Tabs -->
				<div dojoType="dijit.layout.TabContainer" id="userTabsContainer" class="view-users__profile-tabs-container">
					<!-- START User Detail Tab -->
					<div dojoType="dijit.layout.ContentPane" id="userDetailsTab" title="<%= LanguageUtil.get(pageContext, "User-Details") %>">

							<h3 id="fullUserName" class="fullUserName"></h3>

							<div class="form-horizontal view-user__form">
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
									</dl>
									<dl>
										<dt><%= LanguageUtil.get(pageContext, "First-Name") %>:</dt>
										<dd><input id="firstName" type="text" onkeyup="userInfoChanged()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
									</dl>
									<dl>
										<dt><%= LanguageUtil.get(pageContext, "Last-Name") %>:</dt>
										<dd><input id="lastName" type="text" onkeyup="userInfoChanged()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
									</dl>
									<dl>
										<dt><%= LanguageUtil.get(pageContext, "Email-Address") %>:</dt>
										<dd><input id="emailAddress" type="text" onkeyup="userEmailChanged()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
									</dl>
									<dl>
										<dt><%= LanguageUtil.get(pageContext, "Password") %>:</dt>
										<dd><input id="password" type="password" onkeyup="userPasswordChanged()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" autocomplete="off" /></dd>
									</dl>
									<dl>
										<dt><%= LanguageUtil.get(pageContext, "Password-Again") %>:</dt>
										<dd><input id="passwordCheck" type="password" onkeyup="userPasswordChanged()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" autocomplete="off"/></dd>
									</dl>
								</form>
							</div>
							<div class="buttonRow view-user__buttonRow">
								<button dojoType="dijit.form.Button" onclick="saveUserDetails()" type="button"><%= LanguageUtil.get(pageContext, "Save") %></button>
								<%if(hasAdminRole){ %>
									<button dojoType="dijit.form.Button" onclick="showDeleteUserBox()" type="button" class="dijitButtonDanger" id="deleteButton"><%= LanguageUtil.get(pageContext, "Delete") %></button>
								<%} %>
							</div>
							<%if(hasAdminRole){ %>
							<div id="deleteUserDialog" title="<%= LanguageUtil.get(pageContext, "delete-User") %>" dojoType="dijit.Dialog" style="display: none; width:300px;">
								<span style="vertical-align:middle;"><%= LanguageUtil.get(pageContext, "select-a-user-to-replace-current-user-entries-on-db") %>:</span>
								<div dojoType="dotcms.dojo.data.UsersReadStore" jsId="usersStore" includeRoles="false"></div>
								<select id="deleteUsersFilter" name="deleteUsersFilter" dojoType="dijit.form.FilteringSelect" store="usersStore" searchDelay="300" pageSize="30" labelAttr="name" invalidMessage="<%= LanguageUtil.get(pageContext, "Invalid-option-selected") %>"></select>
								<div class="clear"></div>
								<div class="buttonRow">
									<button dojoType="dijit.form.Button" onclick="deleteUser()" type="button" class="dijitButtonDanger"><%= LanguageUtil.get(pageContext, "Delete") %></button>
									<button dojoType="dijit.form.Button" onclick="cancelDeleteUser()" type="button" iconClass="saveIcon"><%= LanguageUtil.get(pageContext, "Cancel") %></button>
								</div>
							</div>
							<%} %>
					</div>
					<!-- END User Detail Tab -->

					<!-- START Roles Tab -->
					<div dojoType="dijit.layout.ContentPane" id="userRolesTab" title="<%= LanguageUtil.get(pageContext, "Roles") %>">

						<h3 id="fullUserName" class="fullUserName"></h3>

						<div id="userRolesContainer" class="view-users__roles-container">
							<div class="view-users__roles-to-grant">
								<h4><%= LanguageUtil.get(pageContext, "Roles-To-Grant") %>:</h4>
								<div id="userRolesTreeWrapper" style="display: none;" class="view-users__roles-container-item">
									<div id="userRolesTree"></div>
								</div>
							</div>
							<div id="actionsDiv" class="view-users__roles-actions">
								<button id="addUserRoleBtn" dojoType="dijit.form.Button" onclick="addUserRoles()" type="button" disabled="disabled">&#62;&#62;</button>
								<button id="removeUserRoleBtn" dojoType="dijit.form.Button" onclick="removeUserRoles()" type="button" disabled="disabled">&#60;&#60;</button>
							</div>
							<div class="view-users__roles-granted">
								<h4><%= LanguageUtil.get(pageContext, "Roles-Granted") %>:</h4>
								<div id="userRolesSelectWrapper" class="view-users__roles-container-item"></div>
							</div>
						</div>

						<div id="loadingRolesWrapper">
							<img src="/html/images/icons/processing.gif">
						</div>

						<div id="noRolesFound" style="display: none;">
							<%= LanguageUtil.get(pageContext, "No-roles-found") %>
						</div>

						<div class="buttonRow">
							<button dojoType="dijit.form.Button" onclick="saveRoles()" type="button"><%= LanguageUtil.get(pageContext, "Save") %></button>
							<button dojoType="dijit.form.Button" onclick="resetUserRoles()" type="button" class="dijitButtonFlat"><%= LanguageUtil.get(pageContext, "Reset") %></button>
						</div>
					</div>
					<!-- END Roles Tab -->

					<!-- START Permissions Tab -->
					<div dojoType="dijit.layout.ContentPane" id="userPermissionsTab" title="<%= LanguageUtil.get(pageContext, "Permissions") %>">
						<%@ include file="/html/portlet/ext/roleadmin/view_role_permissions_inc.jsp" %>
					</div>
					<!-- END Permissions Tab -->

					<!-- START Additional Info Tab -->
					<div dojoType="dijit.layout.ContentPane" id="userAdditionalInfoTab" title="<%= LanguageUtil.get(pageContext, "Additional-Info") %>" class="view-users__additional-info">

						<h3 id="fullUserName" class="fullUserName"></h3>

						<div class="form-horizontal view-user__form" id="additionalUserInfoFormWrapper">
							<form id="userAdditionalInfoForm" dojoType="dijit.form.Form">
								<dl>
									<dt><%= LanguageUtil.get(pageContext, "Active") %>:</dt>
									<dd><input id="userActive" type="checkbox" onkeyup="userInfoChanged()" checked="checked" dojoType="dijit.form.CheckBox" /></dd>
								</dl>
								<dl>
									<dt><%= LanguageUtil.get(pageContext, "Prefix") %>:</dt>
									<dd><input id="prefix" type="text" onkeyup="userInfoChanged()" value="" dojoType="dijit.form.TextBox" /></dd>
								</dl>
								<dl>
									<dt><%= LanguageUtil.get(pageContext, "Suffix") %>:</dt>
									<dd><input id="suffix" type="text" onkeyup="userInfoChanged()" value="" dojoType="dijit.form.TextBox" /></dd>
								</dl>
								<dl>
									<dt><%= LanguageUtil.get(pageContext, "Title") %>:</dt>
									<dd><input id=title type="text" onkeyup="userInfoChanged()" value="" dojoType="dijit.form.TextBox" /></dd>
								</dl>
								<dl>
									<dt><%= LanguageUtil.get(pageContext, "Company") %>:</dt>
									<dd><input id="company" type="text" onkeyup="userInfoChanged()" value="" dojoType="dijit.form.TextBox" /></dd>
								</dl>
								<dl>
									<dt><%= LanguageUtil.get(pageContext, "Website") %>:</dt>
									<dd><input id="website" type="text" onkeyup="userInfoChanged()" value="" dojoType="dijit.form.TextBox" /></dd>
								</dl>
								<% for (int i = 1; i <= additionalVariablesCount; i++) { %>
									<dl>
										<dt id="var<%=i%>Label"><%=additionalVariableLabels[i]%>:</dt>
										<dd id="var<%=i%>Value"><input id="var<%=i%>" type="text" onkeyup="userInfoChanged()" value="" dojoType="dijit.form.TextBox" /></dd>
									</dl>
								<% } %>
							</form>
						</div>

						<div class="view-user__add-contact">
							<button dojoType="dijit.form.Button" id="addAddressIcon" onclick="addAddress()" type="button">
								<%= LanguageUtil.get(pageContext, "Addresses-Phones") %>
							</button>
						</div>

						<div dojoType="dojox.grid.DataGrid" jsId="userAddressesGrid" id="userAddressesGrid" autoHeight="true" store="userAddressesStore" structure="userAddressesGridLayout" query="{ addressId: '*' }"> </div>

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
									<div class="buttonRow">
										<button dojoType="dijit.form.Button" type="button" onClick="saveAddress()"><%= LanguageUtil.get(pageContext, "Save") %></button>
										<button dojoType="dijit.form.Button" type="button" onClick="cancelSaveAddress()" class="dijitButtonFlat"><%= LanguageUtil.get(pageContext, "Cancel") %></button>
									</div>
								</form>
							</div>
						<!-- END Popup Address -->

					</div>
					<!-- END Additional Info Tab -->
				</div>
				<!-- END User Tabs -->
			</div>
		</div>
	</div>
	<!-- END Right Column User Details -->

</div>
<!-- End Portlet -->

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

	function resetUserRoles() {
		var uId = document.getElementById("userId");
		editUser(uId.value);

	}

</script>