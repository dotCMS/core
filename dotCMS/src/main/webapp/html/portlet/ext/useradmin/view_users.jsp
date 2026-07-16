<%@page import="java.time.LocalDate"%>
<%@page import="java.time.temporal.ChronoUnit"%>
<%@page import="java.time.Instant"%>
<%@page import="java.time.format.DateTimeFormatter"%>
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
	
	#userRolesSelect option{
		font-family:Roboto;
		font-size:13px;
		padding:4px ;
		color: rgb(85,85,85);
	}

	#userRolesSelect option:checked{
		background: light-dark(rgb(206, 206, 206), rgb(84, 84, 84));
	}
	
	.tokenDivClass{
		background:#eeeeee;
		margin:auto;
		width:100%;
		height:190px;
		white-space:normal;
		padding:10px 20px;
		font-family:monospace; 
		font-size:13px;
		word-break: break-word;
	}
	#tokenFormDialog{
	   width:500px;

	}
	#revealJwtDialog{
	   width:400px;
	   height:320px;
	}

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
	<div dojoType="dijit.layout.ContentPane" splitter="false" region="leading" style="width:440px;" class="portlet-sidebar-wrapper">
		<div class="portlet-sidebar">
			<div class="inline-form view-users__filter-form">
				<input dojoType="dijit.form.TextBox" onkeyup="filterUsers()" trim="true" name="usersFilter" id="usersFilter" placeholder="<%= LanguageUtil.get(pageContext, "Filter") %>:" />
				<button dojoType="dijit.form.Button" onclick="clearUserFilter()" type="button" class="dijitButtonFlat"><%= LanguageUtil.get(pageContext, "Clear") %></button>
			</div>
         
            <div style="padding:10px;margin:auto;width:80%;">
                <label for="showAllUsers"><input type="radio" onclick="filterUsers(this)" dojoType="dijit.form.RadioButton" value="all" checked="true" name="showUsers" id="showAllUsers"> <%= LanguageUtil.get(pageContext, "All") %> </label>&nbsp; &nbsp;
                <label for="showFrontEndUsers"><input type="radio" onclick="filterUsers(this)" dojoType="dijit.form.RadioButton" value="frontEnd" name="showUsers" id="showFrontEndUsers"> <%= LanguageUtil.get(pageContext, "user.list.filter.frontend") %> </label>&nbsp; &nbsp;
                <label for="showBackEndUsers"><input type="radio" onclick="filterUsers(this)" dojoType="dijit.form.RadioButton" value="backEnd" name="showUsers" id="showBackEndUsers"> <%= LanguageUtil.get(pageContext, "user.list.filter.backend") %> </label>
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
			<div data-dojo-type="dijit/form/DropDownButton" data-dojo-props='iconClass:"fa-plus", class:"dijitDropDownActionButton"' style="position:absolute;top:8px;right:16px;">
				<span></span>
				<div data-dojo-type="dijit/Menu" class="contentlet-menu-actions">
					<div data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: addUser">
						<%= LanguageUtil.get(pageContext, "Add-User") %>
					</div>
					<% if ( enterprise ) {%>
						<% if ( endPoints ) {%>
							<div id="remotePublishUsersDiv" data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: remotePublishUsers">
								<%= LanguageUtil.get(pageContext, "Remote-Publish-all-users") %>
							</div>
						<%}%>
						<div id="addToBundleUsersDiv" data-dojo-type="dijit/MenuItem" data-dojo-props="onClick: addToBundleUsers">
							<%= LanguageUtil.get(pageContext, "Add-To-Bundle-all-users")%>
						</div>
					<%}%>
				</div>
			</div>

			<div id="loadingUserProfile" style="display: none;" class="view-users__loading">
				<div class="view-users__loading-image">
					<img src="/html/images/icons/processing.gif" />
				</div>
			</div>
<style>

.userInfoBox {
    float: left;
    min-width: 450px;
    margin: 10px;
    width:40%;
}   

.userInfoBox table tr{
 height:60px;
}
.gravitarThingy{
    display: flex; 
   position:absolute;
   top:0px;
   right:0px;
   border-radius: 50%;
   background-size: cover;
   width:50px;
   height:50px;
   font-size: 24px;
   text-align:center;
   color:white;
   font-weight: bold;
}
</style>

			<div id="userProfileTabs" class="view-users__profile-tabs" >
				<!-- START User Tabs -->
				<div dojoType="dijit.layout.TabContainer" id="userTabsContainer" class="view-users__profile-tabs-container">
					<!-- START User Detail Tab -->
					<div dojoType="dijit.layout.ContentPane" id="userDetailsTab" title="<%= LanguageUtil.get(pageContext, "User-Details") %>">
                        <div style="position:relative;border:0px solid green;height:60px;width:80%">
                                <div style="position:absolute;width:400px;top:0px;left:0px;"><h3 id="fullUserName" class="fullUserName"></h3></div>
                                <div class="gravitarThingy" id="gravatarTextHolder" style="background-color:<%=APILocator.getCompanyAPI().getDefaultCompany().getStreet() %>;">
                                    <p id="gravatarText" style="margin:auto;"></p> 
                                </div>
                                <div id="gravatarImage" class="gravitarThingy" style="z-index:100"></div>
                         </div>
                            

                        <div>
                            
                                <div class="userInfoBox">
                                  <form id="userInfoForm" dojoType="dijit.form.Form">
                                  <input type="hidden" name="userPasswordChanged" value="false"/>
                                   <div style="font-size:12pt;margin:10px;"><%= LanguageUtil.get(pageContext, "User-Details") %></div>
                                   <table class="listingTable" style="border:1px solid #eeeeee">
                                      <tr>
                                          <% if(authByEmail) { %>
                                              <th  style="width:30%" id="userIdLabel"><%= LanguageUtil.get(pageContext, "User-ID") %>: <input type="hidden" id="userId" name="userId" value=""/></th>
                                              <td  id="userIdValue"></td>
                                          <% } else {%>
                                              <th  style="width:30%" id="userIdLabel"><%= LanguageUtil.get(pageContext, "User-ID") %>:</th>
                                              <td  id="userIdValue"><input id="userId" type="text" onkeyup="userInfoChanged()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" disabled="disabled" /></td>
                                          <% } %>
                                      </tr>
                                      <tr>
                                          <th>
											<label for="firstName" class="required">
												<%= LanguageUtil.get(pageContext, "First-Name") %>
											</label>
										  </th>
                                          <td><input id="firstName" type="text" onkeyup="userInfoChanged()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></td>
                                      </tr>
                                      <tr>
                                          <th>
											<label for="lastName" class="required">
												<%= LanguageUtil.get(pageContext, "Last-Name") %>
											</label>
										  </th>
                                          <td><input id="lastName" type="text" onkeyup="userInfoChanged()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></td>
                                      </tr>
                                      <tr>
                                           <th>
											   <label for="emailAddress" class="required">
													<%= LanguageUtil.get(pageContext, "Email-Address") %>
												</label>
											</th>
                                          <td><input id="emailAddress" type="text" onkeyup="userEmailChanged()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></td>
                                      </tr>
                                      <tr>
                                        <th></th>
                                        <td><button dojoType="dijit.form.Button" onclick="generateSecurePasswordModal()" type="button"><%= LanguageUtil.get(pageContext, "generate.secure.password") %></button></td>
                                      </tr>
                                      <tr>
                                          	<th>
												<label for="password" class="required">
													<%= LanguageUtil.get(pageContext, "Password") %>
												</label>
											</th>
                                          <td><input id="password" type="password" onkeyup="userPasswordChanged()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" autocomplete="new-password" /></td>
                                      </tr>
                                      <tr>
                                          	<th>
												<label for="passwordCheck" class="required">
													<%= LanguageUtil.get(pageContext, "Password-Again") %>
												</label>
											</th>
                                          <td><input id="passwordCheck" type="password" onkeyup="userPasswordChanged()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" autocomplete="new-password" /></td>
                                      </tr>
                                    <tr>
                                        <th><%= LanguageUtil.get(pageContext, "Last-Login") %></th>
                                        <td id="lastLogin"  ></td>
                                    </tr>
                                    <tr>
                                        <th><%= LanguageUtil.get(pageContext, "user.detail.failed.logins") %></th>
                                        <td id="loginAttempts"></td>
                                    </tr>
                                    
                                    <tr style="text-align: center;border-bottom:0px;">
                                       <td colspan="2" style="text-align: center;border-bottom:0px;">
                                        <%if(hasAdminRole){ %>
                                            <button dojoType="dijit.form.Button" onclick="showDeleteUserBox()" type="button" class="dijitButtonDanger" id="deleteButton"><%= LanguageUtil.get(pageContext, "Delete") %></button> &nbsp; &nbsp;
                                        <%} %>
                                        <button dojoType="dijit.form.Button" onclick="saveUserDetails()" type="button"><%= LanguageUtil.get(pageContext, "Save") %></button>
                                    </tr>
                                 </table>
                                </form>
                             </div>
                             
                            <div class="userInfoBox" id="userAccessBox">
                               <div style="font-size:12pt;margin:10px;"><%= LanguageUtil.get(pageContext, "user.detail.access") %></div>
                               <table class="listingTable" style="border:1px solid #eeeeee" >
                                   <tr>
                                       <th style="width:30%;min-height:60px;"><label for="userActive"> <%= LanguageUtil.get(pageContext, "Active") %></label></th>
                                       <td><input id="userActive" type="checkbox" onclick="changeUserAccess(this)" value="true" dojoType="dijit.form.CheckBox" /></td>
                                   </tr>
                                   <%if(hasAdminRole){ %>
                                   <tr>
                                       <th style="width:30%;min-height:60px;"><label for="adminRoleCheck">CMS Admin</label></th>
                                       <td><input type="checkbox" id="adminRoleCheck" onclick="changeUserAccess(this)" value="true" dojoType="dijit.form.CheckBox"></td>
                                   </tr>
                                   <%} %>
                                   <tr>
                                       <th><label for="frontEndRoleCheck"><%= LanguageUtil.get(pageContext, "user.detail.frontend.user") %></label></th>
                                       <td><input type="checkbox" id="frontEndRoleCheck" onclick="changeUserAccess(this)" value="true" dojoType="dijit.form.CheckBox"></td>
                                   </tr>
                                   <tr>
                                       <th><label for="backEndRoleCheck"><%= LanguageUtil.get(pageContext, "user.detail.backend.user") %></label></th>
                                       <td><input type="checkbox" id="backEndRoleCheck" onclick="changeUserAccess(this)" value="true" dojoType="dijit.form.CheckBox"></td>
                                   </tr>
                                   <tr>
                                       <th><%= LanguageUtil.get(pageContext, "user.detail.can.login") %></span></th>
                                       <td>
                                       <span id="explainCanLogin" style="border-bottom:1px dotted blue">
                                        <span id="canLoginToConsole" style="font-weight:bold"></span>
                                       </span>
                                       </td>
                                   </tr>
                                   <tr>
                                       <th><label for="showStarter"> <%= LanguageUtil.get(pageContext, "starter.show.getting.started") %></label></th>
                                       <td><input type="checkbox" id="showStarter" onclick="setStarterPage(this)" value="true" dojoType="dijit.form.CheckBox"></td>
                                   </tr>
                               </table>
                            </div>
                            
                            
                            
                     
                     
                     
						</div>
		
						<%if(hasAdminRole){ %>
      						<div id="deleteUserDialog" title="<%= LanguageUtil.get(pageContext, "delete-User") %>" dojoType="dijit.Dialog" style="display: none; width:400px;">
      							<span style="vertical-align:middle;"><label for="deleteUsersFilter"><%= LanguageUtil.get(pageContext, "select-a-user-to-replace-current-user-entries-on-db") %></label></span>
      							<div dojoType="dotcms.dojo.data.UsersReadStore" jsId="usersStore" includeRoles="false"></div>
      							<div style="text-align:center;padding:20px;">
                                    <select id="deleteUsersFilter" name="deleteUsersFilter" dojoType="dijit.form.FilteringSelect" store="usersStore" searchDelay="300" pageSize="30" labelAttr="name" invalidMessage="<%= LanguageUtil.get(pageContext, "Invalid-option-selected") %>"></select>
      							</div>
                           <div class="clear"></div>
      							<div class="buttonRow">
      								<button dojoType="dijit.form.Button" onclick="deleteUser()" type="button" class="dijitButtonDanger"><%= LanguageUtil.get(pageContext, "Delete") %></button>
      								<button dojoType="dijit.form.Button" onclick="cancelDeleteUser()" type="button" iconClass="saveIcon"><%= LanguageUtil.get(pageContext, "Cancel") %></button>
      							</div>
      						</div>
						<%} %>
					</div>
					<!-- END User Detail Tab -->

                    <!-- START Additional Info Tab -->
                    <div dojoType="dijit.layout.ContentPane" id="userAdditionalInfoTab" title="<%= LanguageUtil.get(pageContext, "Additional-Info") %>" class="view-users__additional-info">
                        <div style="position:relative;border:0px solid green;height:60px;width:85%">
                            <h3 id="fullUserName" class="fullUserName"></h3>
                        </div>
                        <div class="userInfoBox">
                            <form id="userAdditionalInfoForm" dojoType="dijit.form.Form">
                            <table class="listingTable" style="border:1px solid #eeeeee">

                                <tr>
                                    <th><label for="prefix"><%= LanguageUtil.get(pageContext, "Prefix") %></label></th>
                                    <td><input id="prefix" type="text" onkeyup="userInfoChanged()" value="" dojoType="dijit.form.TextBox" /></td>
                                </tr>
                                <tr>
                                    <th><label for="suffix"><%= LanguageUtil.get(pageContext, "Suffix") %></label></th>
                                    <td><input id="suffix" type="text" onkeyup="userInfoChanged()" value="" dojoType="dijit.form.TextBox" /></td>
                                </tr>
                                <tr>
                                    <th><label for="title"><%= LanguageUtil.get(pageContext, "Title") %></label></th>
                                    <td><input id=title type="text" onkeyup="userInfoChanged()" value="" dojoType="dijit.form.TextBox" /></td>
                                </tr>
                                <tr>
                                    <th><label for="company"><%= LanguageUtil.get(pageContext, "Company") %></label></th>
                                    <td><input id="company" type="text" onkeyup="userInfoChanged()" value="" dojoType="dijit.form.TextBox" /></td>
                                </tr>
                                <tr>
                                    <th><label for="website"><%= LanguageUtil.get(pageContext, "Website") %></label></th>
                                    <td><input id="website" type="text" onkeyup="userInfoChanged()" value="" dojoType="dijit.form.TextBox" /></td>
                                </tr>
                                <% for (int i = 1; i <= additionalVariablesCount; i++) { %>
                                    <tr>
                                        <th><label for="var<%=i%>Label" id="var<%=i%>Label"><%=additionalVariableLabels[i]%></label></th>
                                        <td id="var<%=i%>Value"><input id="var<%=i%>" type="text" onkeyup="userInfoChanged()" value="" dojoType="dijit.form.TextBox" /></td>
                                    </tr>
                                <% } %>
                               </table>
                            </form>
                           <div style="text-align: center;">
                               <button dojoType="dijit.form.Button" onclick="saveUserAdditionalInfo()" type="button"><%= LanguageUtil.get(pageContext, "Save") %></button>
                           </div>
                        </div>

   

                    </div>
                    <!-- END Additional Info Tab -->




					<!-- START Roles Tab -->
					<div dojoType="dijit.layout.ContentPane" id="userRolesTab" title="<%= LanguageUtil.get(pageContext, "Roles") %>">

						<h3 id="fullUserName" class="fullUserName"></h3>

						<div id="userRolesContainer" class="view-users__roles-container">
							<div class="view-users__roles-to-grant">
								<h4><%= LanguageUtil.get(pageContext, "Roles-To-Grant") %></h4>
								<div id="userRolesTreeWrapper" style="display: none;" class="view-users__roles-container-item">
									<div id="userRolesTree"></div>
								</div>
							</div>
							<div id="actionsDiv" class="view-users__roles-actions">
								<button id="addUserRoleBtn" dojoType="dijit.form.Button" onclick="addUserRoles()" type="button" disabled="disabled">&#62;&#62;</button>
								<button id="removeUserRoleBtn" dojoType="dijit.form.Button" onclick="removeUserRoles()" type="button" disabled="disabled">&#60;&#60;</button>
							</div>
							<div class="view-users__roles-granted">
								<h4><%= LanguageUtil.get(pageContext, "Roles-Granted") %></h4>
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
							<button dojoType="dijit.form.Button" onclick="resetUserRoles()" type="button" class="dijitButtonFlat"><%= LanguageUtil.get(pageContext, "Reset") %></button>
							<button dojoType="dijit.form.Button" onclick="saveRoles()" type="button"><%= LanguageUtil.get(pageContext, "Save") %></button>
						</div>
					</div>
					<!-- END Roles Tab -->

					<!-- START Permissions Tab -->
					<div dojoType="dijit.layout.ContentPane" id="userPermissionsTab" title="<%= LanguageUtil.get(pageContext, "Permissions") %>">
						<h3 id="fullUserName" class="fullUserName"></h3>
						<%@ include file="/html/portlet/ext/roleadmin/view_role_permissions_inc.jsp" %>
					</div>
					<!-- END Permissions Tab -->


					
					
					
					
					
					<!-- START API Keys Tab -->
					<div dojoType="dijit.layout.ContentPane" id="apiKeysTab" title="<%= LanguageUtil.get(pageContext, "api.token.all.keys") %>">
                        <h3 id="fullUserName" class="fullUserName"></h3>
						<div class="buttonRow" style="text-align:right"><input type="checkbox" id="showRevokedApiTokens" onclick="loadApiKeys()" dojoType="dijit.form.CheckBox" /> <%= LanguageUtil.get(pageContext, "api.token.show.inactive") %></div>  
                        <div id="apiKeysDiv"></div>
                        <div class="buttonRow">
                            <button dojoType="dijit.form.Button" onclick="showRequestTokenDialog()" type="button" iconClass="saveIcon"><%= LanguageUtil.get(pageContext, "api.token.request.new.token") %></button>
                        </div>
                    </div>
					<!-- START API Keys Tab -->
					
					
					
					
					
				</div>
				<!-- END User Tabs -->
			</div>
		</div>
	</div>
	<!-- END Right Column User Details -->

</div>
<!-- End Portlet -->

<div dojoType="dijit.Dialog" id="tokenFormDialog" title="API Token"
	execute="requestNewAPIToken(arguments[0]);">
	<h3>Request New Token</h3>
	<table class="listingTable">
        <tr>
            <td><label for="nameLabel"><%=LanguageUtil.get(pageContext, "Label")%>
            </label></td>

            <td><input dojoType="dijit.form.TextBox" type="text"
                name="nameLabel" id="nameLabel" value=""></td>
        </tr>
    
    
		<tr>
			<td><label for="expiresDate"><%=LanguageUtil.get(pageContext, "api.token.request.expires.date")%>
			</label></td>
			<td><input dojoType="dijit.form.DateTextBox" type="text"
				name="expiresDate" id="expiresDate"
				value="<%=DateTimeFormatter.ofPattern("uuuu-MM-dd").format(LocalDate.now().plus(3, ChronoUnit.YEARS))%>"></td>
		</tr>

		<tr>
			<td><label for="network"><%=LanguageUtil.get(pageContext, "api.token.allowed.network")%>
			</label></td>

			<td><input dojoType="dijit.form.TextBox" type="text"
				name="network" id="network" value="0.0.0.0/0"></td>
		</tr>
	</table>

	<div class="buttonRow">
		<button dojoType="dijit.form.Button" type="button"
			class="dijitButtonFlat"
			onClick="dijit.byId('tokenFormDialog').hide();"><%=LanguageUtil.get(pageContext, "cancel")%></button>
		&nbsp;
		<button dojoType="dijit.form.Button" type="submit"
			onClick="return dijit.byId('tokenFormDialog').isValid();"><%=LanguageUtil.get(pageContext, "ok")%></button>
	</div>
</div>

<div dojoType="dijit.Dialog" id="revealJwtDialog" title="API Token">
	<textarea class="tokenDivClass" id="revealTokenDiv"></textarea>
	<div class="buttonRow">
		<button dojoType="dijit.form.Button" type="button" class=""
			onClick="dijit.byId('revealJwtDialog').hide();"><%=LanguageUtil.get(pageContext, "close")%></button>
	</div>
</div>



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
