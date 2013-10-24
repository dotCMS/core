<%@page import="com.dotmarketing.util.UtilMethods"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portlet.PortletURLImpl"%>
<%@page import="javax.portlet.WindowState"%>
<%@page import="javax.portlet.PortletMode"%>
<%@page import="com.liferay.portal.util.PortletKeys"%>
<%@page import="javax.portlet.PortletURL"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.dotmarketing.business.Role"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@ page import="com.dotmarketing.util.CompanyUtils" %>
<%@page import="com.dotmarketing.util.Config"%>
<%@page import="com.liferay.portal.util.ReleaseInfo" %>
<%@page import="java.net.URLEncoder"%>
<%@page import="com.dotmarketing.viewtools.JSONTool" %>
<%@page import="com.dotmarketing.util.json.JSONObject" %>
<%@page import="com.dotmarketing.util.json.JSONArray" %>
<%
    boolean isCommunity = ("100".equals(System
            .getProperty("dotcms_level")));
    String licenseMessage = null;
    String licenseURL = "http://www.dotcms.com/buy-now";
    List<Layout> layoutListForLicenseManager=null;
    try {
        if (isCommunity) {
        	licenseMessage = LanguageUtil.get(pageContext, "Try-Enterprise-Now") + "!" ;

            layoutListForLicenseManager=APILocator.getLayoutAPI().findAllLayouts();
            for (Layout layoutForLicenseManager:layoutListForLicenseManager) {
                List<String> portletIdsForLicenseManager=layoutForLicenseManager.getPortletIds();
                if (portletIdsForLicenseManager.contains("EXT_LICENSE_MANAGER")) {
                    licenseURL = "/c/portal/layout?p_l_id=" + layoutForLicenseManager.getId() +"&p_p_id=EXT_LICENSE_MANAGER&p_p_action=0";
                    break;
                }

            }



        } else {
            boolean isPerpetual = new Boolean(System
                    .getProperty("dotcms_license_perpetual"));
            boolean isTrial = true;
            try{
                isTrial = (System.getProperty("dotcms_license_client_name").toLowerCase().indexOf("trial") >-1);
            }
            catch(Exception e){}
            Date td = new Date();
            Date ed = new Date(Long.parseLong(System
                    .getProperty("dotcms_valid_until")));

            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            start.setTime(td);
            start.set(Calendar.HOUR, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            start.set(Calendar.MILLISECOND, 0);
            end.setTime(ed);
            long milliseconds1 = start.getTimeInMillis();
            long milliseconds2 = end.getTimeInMillis();
            long diff = milliseconds2 - milliseconds1;
            long diffDays = diff / (24 * 60 * 60 * 1000);
            if (!isPerpetual && isTrial) {
                if (diffDays > 1) {
                	licenseMessage = LanguageUtil.format(pageContext, "days-remaining-Purchase-now",diffDays,false);
                    licenseURL = "http://dotcms.com/buy-now";
                }
                if (diffDays == 1) {
                	licenseMessage = LanguageUtil.format(pageContext, "day-remaining-Purchase-now",diffDays,false);
                    licenseURL = "http://dotcms.com/buy-now";
                }
                if (diffDays < 1) {
                	licenseMessage = LanguageUtil.get(pageContext,"Trial-Expired-Purchase-Now");
                    licenseURL = "http://dotcms.com/buy-now";
                }

            }else if (!isPerpetual) {
                if (diffDays > 1 && diffDays < 30) {
                	licenseMessage = LanguageUtil.format(pageContext, "Subscription-expires-in-days",diffDays,false);
                    licenseURL = "http://dotcms.com/renew-now";
                }
                if (diffDays == 1) {
                	licenseMessage = LanguageUtil.format(pageContext, "Subscription-expires-in-day",diffDays,false);
                    licenseURL = "http://dotcms.com/renew-now";
                }
                if (diffDays < 1) {
                	licenseMessage = LanguageUtil.get(pageContext,"Subscription-Expired-Renew-Now");
                    licenseURL = "http://dotcms.com/buy-now";
                }
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    boolean hasRolesPortlet = false;
    boolean hasLicenseManagerPortlet = false;
    String portletLinkHREF = "";
    java.util.List<com.dotmarketing.business.Layout> userLayouts = APILocator.getLayoutAPI().loadLayoutsForUser(user);
    if ((userLayouts != null) && (userLayouts.size() != 0)) {
        int count = 0;
        for (int i = 0; i < userLayouts.size(); i++) {
            java.util.List<String> portletids = userLayouts.get(i).getPortletIds();
            for (int j = 0; j < portletids.size(); j++) {
                if (portletids.get(j).equals("EXT_ROLE_ADMIN") && !hasRolesPortlet) {
                    hasRolesPortlet = true;

                }

                if (portletids.get(j).equals("EXT_LICENSE_MANAGER") && !hasLicenseManagerPortlet) {
                    hasLicenseManagerPortlet = true;
                }
            }
            if(!userLayouts.get(i).getName().equals("CMS Admin")){
                count+=1;
            }
        }

    }
    if (!hasRolesPortlet || hasLicenseManagerPortlet) {

         java.util.List<com.dotmarketing.business.Layout> allLayouts = APILocator.getLayoutAPI().findAllLayouts();
         com.liferay.portal.model.Portlet portlet = null;
         String ticket = "";
        if(!hasRolesPortlet){
          String roleAdminPortletId = "";
          portlet = APILocator.getPortletAPI().findPortlet("EXT_ROLE_ADMIN");
            if(portlet!=null){
                roleAdminPortletId = portlet.getPortletId();
            }

        if ((allLayouts != null) && (allLayouts.size() != 0)) {
            for (int i = 0; i < allLayouts.size(); i++) {
                if(allLayouts.get(i).getName().equals("CMS Admin")){
                    PortletURLImpl portletURLImpl = new PortletURLImpl(
                            request, roleAdminPortletId, allLayouts.get(i).getId(), false);
                     ticket  = String.valueOf(portletURLImpl.hashCode());
                    if(request.getSession().getAttribute("roleAdminOverrideTicket")==null){
                        request.getSession().setAttribute("roleAdminOverrideTicket",ticket);
                    }else{
                        ticket = (String)request.getSession().getAttribute("roleAdminOverrideTicket");
                    }

                    portletLinkHREF = portletURLImpl.toString()+ "&dm_rlout=1&roleAdminOverrideTicket="+ticket;
                    break;
                }
            }
         }
        }

        if(!hasLicenseManagerPortlet){
            String licenseManagerPortletId = "";
            portlet = APILocator.getPortletAPI().findPortlet("EXT_LICENSE_MANAGER");
                if(portlet!=null){
                    licenseManagerPortletId = portlet.getPortletId();
                }


            if ((allLayouts != null) && (allLayouts.size() != 0)) {

                if (APILocator.getUserAPI().isCMSAdmin(user)){
                        PortletURLImpl portletURLImpl = new PortletURLImpl(
                                request, licenseManagerPortletId, allLayouts.get(1).getId(), false);
                        ticket  = String.valueOf(portletURLImpl.hashCode());
                        if(request.getSession().getAttribute("licenseManagerOverrideTicket")==null){
                            request.getSession().setAttribute("licenseManagerOverrideTicket",ticket);
                        }else{
                            ticket = (String)request.getSession().getAttribute("licenseManagerOverrideTicket");
                        }

                        licenseURL = portletURLImpl.toString()+ "&dm_rlout=1&licenseManagerOverrideTicket="+ticket;

                    }
                }

            }
    }else {
        if(request.getSession().getAttribute("roleAdminOverrideTicket")!=null){
           request.getSession().removeAttribute("roleAdminOverrideTicket");
        }

        if(request.getSession().getAttribute("licenseManagerOverrideTicket")!=null){
               request.getSession().removeAttribute("licenseManagerOverrideTicket");
        }
    }





        boolean emailAuth = false;
        if(company.getAuthType().equals(com.liferay.portal.model.Company.AUTH_TYPE_EA)) {
            emailAuth = true;
        }

%>






<%@page import="java.util.Date"%>
<%@page import="java.util.Calendar"%>

<%@page import="com.dotmarketing.business.Layout"%>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.util.WebKeys"%>

<%@page import="com.dotmarketing.util.UtilMethods"%>


<script type="text/javascript" src="/dwr/interface/UserAjax.js"></script>

<script type="text/javascript">

dojo.require("dojo.cookie");

    dojo.addOnLoad (function () {
        dojo.connect(dijit.byId('portal_login_as_user'), 'onChange',
            function (val) {
                if(val != '') {
                    <%try {%>
                        UserAjax.hasUserRoles(dijit.byId('portal_login_as_user').value.split("-")[1], [ '<%=APILocator.getRoleAPI().loadRoleByKey(
                        Role.ADMINISTRATOR).getId()%>', '<%=APILocator.getRoleAPI().loadCMSAdminRole().getId()%>' ], portal_loginAs_checkAdminRole);
                    <%} catch (Exception ex) {

                      }%>
                } else {
                    //dojo.byId('portal_loginasbutton').disabled = true;
                }
        });

        dojo.connect(dijit.byId('portal_loginasbutton'), 'onClick', function () {
            dojo.byId('portal_login_as_users_form').submit();
        });

        <%if (request.getSession().getAttribute("portal_login_as_error") != null) {%>
                dojo.byId('portal_loginas_errors').innerHTML = '<bean:message key='<%=(String) request.getSession().getAttribute(
                            "portal_login_as_error")%>'/>';
                portal_showLoginAs();

        <%request.getSession().removeAttribute(
                            "portal_login_as_error");
                }%>


    });

    function clearErrorMsg()
    {
    	 <%request.getSession().removeAttribute("portal_login_as_error");%>
         dojo.byId('portal_loginas_errors').innerHTML = '';
    }
    
    function portal_loginAs_checkAdminRole(isAdmin) {
        var wrapper = dojo.byId('portal_login_as_password_wrapper');
        if(isAdmin) {
            dojo.style(wrapper, { display: '' });
        } else {
            dojo.style(wrapper, { display: 'none' });
        }
    }

    function portal_showLoginAs() {
        dijit.byId('portal_login_as_users_wrapper').show();
        dijit.byId('portal_login_as_user').value = '';
    }

    function portal_cancelLoginAs() {
        dijit.byId('portal_login_as_users_wrapper').hide();
    }

    function portal_showRolesPortlet() {
        dijit.byId('portal_roles_wrapper').show();
    }

    function portal_showMyAccount() {
        dijit.byId('portal_myaccount_wrapper').show();
        var userId='<%= user.getUserId()%>';
        if(userId!='null'){
            editUserMyAccount(userId);
        }
    }

    <% if(emailAuth) { %>
    	var emailAuth = true;
    <% } else { %>
    	var emailAuth = false;
    <% }  %>

    var passwordsDontMatchError = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "passwords-dont-match-error")) %>';
    var userSavedMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "User-Info-Saved")) %>';
    var sameEmailAlreadyRegisteredErrorMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "user-email-already-registered")) %>';
    var sameUserIdAlreadyRegisteredErrorMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "user-id-already-registered")) %>';
    var doNotHavePermissionsMsg = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "dont-have-permissions-msg")) %>';


    var currentUserMyAccount;
    function editUserMyAccount(userIdMyAccount) {
        var user = {
        		id : "<%= user.getUserId() %>",
        		type : "user",
                firstName : "<%= user.getFirstName() %>",
                lastName : "<%= user.getLastName() %>",
                emailaddress : "<%=user.getEmailAddress()%>",
                name : "<%=user.getFullName()%>"
        };

        //Global user variable
        currentUserMyAccount = user;

        //SEtting user info form
        if(!emailAuth) {
            dijit.byId('userIdMyAccount').attr('value', user.id);
            dijit.byId('userIdMyAccount').setDisabled(true);
        } else {
            dojo.byId('userIdValueMyAccount').innerHTML = user.id;
            dojo.byId('userIdMyAccount').value = user.id;
        }
        dojo.byId('userIdLabelMyAccount').style.display = '';
        dojo.byId('userIdValueMyAccount').style.display = '';
        dijit.byId('firstNameMyAccount').attr('value', user.firstName);
        dijit.byId('lastNameMyAccount').attr('value', user.lastName);
        dijit.byId('emailAddressMyAccount').attr('value', user.emailaddress);
        dijit.byId('passwordMyAccount').attr('value', '********');
        dijit.byId('passwordCheckMyAccount').attr('value', '********');

        dojo.query(".fullUserName").forEach(function (elem) { elem.innerHTML = '<b>' + user.name + '</b>'; });

        userChangedMyAccount = false;
        //dojo.byId('loadingUserProfile').style.display = 'none';

    }


    //Handler from when the user info has changed
    var userChangedMyAccount = false;
    function userInfoChangedMyAccount() {
        userChangedMyAccount = true;
    }

    //Handler from when the user password has changed
    var passwordChangedMyAccount = false;
    function userPasswordChangedMyAccount() {
        userChangedMyAccount = true;
        passwordChangedMyAccount = true;
    }

    //Handler to save the user details
    function saveUserDetailsMyAccount() {

        //If user has not changed do nothing
        if(!userChangedMyAccount) {
            alert(userSavedMsg);
            return;
        }

        //If the form is not valid focus on the first not valid field and
        //hightlight the other not valid ones
        if(!dijit.byId('userInfoFormMyAccount').validate()) {
            return;
        }

        var myAccountpassswordValue;
        var myAccountreenterPasswordValue;
        if(passwordChangedMyAccount) {
            myAccountpassswordValue = dijit.byId('passwordMyAccount').attr('value');
            myAccountreenterPasswordValue = dijit.byId('passwordCheckMyAccount').attr('value');
            if(myAccountpassswordValue != myAccountreenterPasswordValue) {
                alert(passwordsDontMatchError);
                return;
            }
        }

        //Executing the update user logic
        var callbackOptions = {
            callback: saveUserCallbackMyAccount,
            exceptionHandler: saveUserExceptionMyAccount
        };
        UserAjax.updateUser(currentUserMyAccount.id, currentUserMyAccount.id, dijit.byId('firstNameMyAccount').attr('value'),
                dijit.byId('lastNameMyAccount').attr('value'),
                dijit.byId('emailAddressMyAccount').attr('value'), myAccountpassswordValue, callbackOptions);

    }

    //Callback from the server to confirm the user saved
    function saveUserCallbackMyAccount (userId) {
        if(userId) {
            userChangedMyAccount = false;
            passwordChangedMyAccount = false;
            alert(userSavedMsg);
            editUserMyAccount(userId);
        } else {
            alert(userSaveFailedMsg);
        }
    }

    function saveUserExceptionMyAccount(message, exception){
        if(exception.javaClassName == 'com.dotmarketing.business.DuplicateUserException') {
            if(emailAuth) {
                alert(sameEmailAlreadyRegisteredErrorMsg);
            }
            else {
                alert(sameUserIdAlreadyRegisteredErrorMsg);
            }
        } else if(exception.javaClassName == 'com.dotmarketing.exception.DotSecurityException'){
                alert(doNotHavePermissionsMsg);
            }else {
            alert("Server error: " + exception);
        }
    }

    function showAutoUpdaterPopUp(){
        dijit.byId('portal_autoupdater_wrapper').show();
    }

    function remindMeLater(){
        dojo.cookie("autoupdater-reminder", "true",{expires: 7, path: '/'});
        dijit.byId('portal_autoupdater_wrapper').hide();
        document.getElementById('autoUpdaterLink').style.display = "none";
    }

    var major;
    var minor;

    function whatsNew(version){
        var href = "http://dotcms.com/dotCMSVersions#";
        if (version=='current'){
            href+="<%= ReleaseInfo.getVersion() %>";
        }else if(version=='major'){
            href+=major;
        }else if(version=='minor'){
            href+=minor;
        }
        window.open(href);
    }

    function howTo(){

        var href = "<%= LanguageUtil.get(pageContext, "Autoupdater-link")%>";
        window.open(href);
    }

    function setTextContent(element, text) {
        while (element.firstChild!==null)
            element.removeChild(element.firstChild); // remove all existing content
        element.appendChild(document.createTextNode(text));
    }

    function enableAutoUpdaterLink(versionInfo){
        major = versionInfo.major;
        minor = versionInfo.minor;
        build = versionInfo.buildNumber;
        if(major!=''){
            document.getElementById('majorUpdate').style.display="";
            setTextContent(document.getElementById('wnMajor'), major + "(<%= LanguageUtil.get(pageContext, "Whats-new") %>)");
        }
        if(minor!=''){
            document.getElementById('minorUpdate').style.display="";
            if(build!='' && build!='0'){
                setTextContent(document.getElementById('wnMinor'),  minor + " Build: " + build +"(<%= LanguageUtil.get(pageContext, "Whats-new") %>)");
            }else{
                setTextContent(document.getElementById('wnMinor'),minor + "(<%= LanguageUtil.get(pageContext, "Whats-new") %>)");
            }
        }
        var autoUpdaterCookie = dojo.cookie("autoupdater-reminder");
        if(!autoUpdaterCookie || autoUpdaterCookie=='false' || autoUpdaterCookie=='undefined'){
            if(versionInfo.showUpdate){
                 //var animArgs = {node: 'autoUpdaterLink',duration: 1000, delay: 50};
                 //dojo.fadeIn(animArgs).play();
                 dojo.style("autoUpdaterLink", "display", "inline-block");
            }
        }

    }

	function showAboutDotCMSMessage(){
       var myDialog = dijit.byId("dotBackEndDialog");
       myDialog.titleNode.innerHTML="<%= LanguageUtil.get(pageContext, "about") %> dotCMS";
       dijit.byId("dotBackEndDialogCP").setHref("/html/portal/about.jsp");
       myDialog.show();
	}


	function showDisclaimerMessage(){
       var myDialog = dijit.byId("dotBackEndDialog");
       myDialog.titleNode.innerHTML="<%= UnicodeLanguageUtil.get(pageContext, "disclaimer") %>";
       dijit.byId("dotBackEndDialogCP").setHref("/html/portal/disclaimer.jsp");
       myDialog.show();
	}
	
	 function toggleAccount() {
		if(document.getElementById("account-menu").style.display=="none") {
			document.getElementById("account-menu").style.display="";
			document.getElementById("closeTab").style.display="";
			document.getElementById("account-trigger").setAttribute("class", "trigger-on");
		} else {
			document.getElementById("account-menu").style.display="none";
			document.getElementById("closeTab").style.display="none";
			document.getElementById("account-trigger").setAttribute("class", "trigger-off");
		}
	}	

</script>

<div id="admin-banner-logo-div">
    <h1>dotCMS</h1>
</div>

<!-- Start Site Tools -->
<% if (signedIn) { %>
    <div id="admin-site-tools-div">
		<!-- Updates -->
        <% if (licenseMessage != null) { %>
            <a class="goEnterpriseLink" href="<%=licenseURL%>"><span class="keyIcon"></span><%=licenseMessage%></a>
        <% } %>

   		<a id="autoUpdaterLink" style="display:none;" class="goEnterpriseLink"  href="javascript: showAutoUpdaterPopUp();"><span class="exclamation-red"></span><%= LanguageUtil.get(pageContext, "Update-available") %></a>
		
		<!-- User Actions -->
		<% if (request.getSession().getAttribute(WebKeys.PRINCIPAL_USER_ID) == null) { %>
			<a href="#" id="account-trigger" onclick="toggleAccount();" class="trigger-off"><%=user.getFullName()%></a>
	    <% } else { %>
	        <a href="<%=CTX_PATH%>/portal<%=PortalUtil.getAuthorizedPath(request)%>/logout_as?referer=<%=CTX_PATH%>"><span class="plusIcon"></span><bean:message key="logout-as" /> <%=user.getFullName()%></a>
	    <% } %>
	</div>
<% } %>
    
<!-- End Site Tools -->


<!-- User Info Drop Down -->

<div id="account-menu" class="account-flyout" style="display:none;">
	<div class="my-account">
		<h3><%=user.getFullName()%></h3>
		<a href="javascript: portal_showMyAccount();toggleAccount();">
			<%=LanguageUtil.get(pageContext, "my-account")%>
		</a>
	</div>
	<div class="service-links">
		<a  href="javascript:showAboutDotCMSMessage();toggleAccount();" ><%= LanguageUtil.get(pageContext, "about")  %></a>
		<a  href="javascript:showDisclaimerMessage();toggleAccount();"><%= LanguageUtil.get(pageContext, "disclaimer")  %></a>
		<a  href="#" onClick="dijit.byId('showSupport').show();toggleAccount();"><%=LanguageUtil.get(pageContext, "Support") %></a>
	</div>
	<div class="login-out">
		<table>
			<tbody>
				<tr>
					<td>
						<a  href="<%=CTX_PATH%>/portal<%=PortalUtil.getAuthorizedPath(request)%>/logout?referer=<%=CTX_PATH%>"><%=LanguageUtil.get(pageContext, "Logout")%></a>
					</td>
					<c:if test="<%= APILocator.getRoleAPI().doesUserHaveRole(user, APILocator.getRoleAPI().loadRoleByKey(Role.LOGIN_AS)) && request.getSession().getAttribute(WebKeys.PRINCIPAL_USER_ID) == null %>">
						<td style="border-left:1px solid #d0d0d0;width:50%;"><a href="javascript: portal_showLoginAs();toggleAccount();"><bean:message key="login-as" /></a></td>
				    </c:if>
				</tr>
			</tbody>
		</table>
	</div>
	
    <% if (!hasRolesPortlet && APILocator.getUserAPI().isCMSAdmin(user) ) { %>
        <a class="rolePortletLink" href="<%=portletLinkHREF%>"><bean:message key="warning-roles-portlet" /></a>
    <% } %>
</div>

<div id="closeTab" onClick="toggleAccount();" style="display:none;"></div>

<!-- End User Info Drop Down -->

<!-- Start Login As pop up -->
    <%
        if (APILocator.getRoleAPI().doesUserHaveRole(user,
            APILocator.getRoleAPI().loadRoleByKey(Role.LOGIN_AS))
            && request.getSession().getAttribute(
                WebKeys.PRINCIPAL_USER_ID) == null) {
    %>
        <div id="portal_login_as_users_wrapper" dojoType="dijit.Dialog"  style="display:none;height:180px;vertical-align: middle;padding-top:15px\9;" draggable="false" >
            <div id="portal_loginas_errors"></div>
            <form id="portal_login_as_users_form" action="<%=CTX_PATH%>/portal<%=PortalUtil.getAuthorizedPath(request)%>/login_as?referer=<%=CTX_PATH%>" method="post">
                <div id="portal_login_as_users_select" class="formRow" style="text-align:center;">
                    <div dojoType="dotcms.dojo.data.UsersReadStore" jsId="usersStore" includeRoles="false"></div>
                    <bean:message key="Select-User" /> : &nbsp;
                        <select id="portal_login_as_user" name="portal_login_as_user" dojoType="dijit.form.FilteringSelect" onchange="clearErrorMsg()"
                        store="usersStore" searchDelay="300" pageSize="30" labelAttr="name"
                        invalidMessage="<%=LanguageUtil.get(pageContext,
                            "Invalid-option-selected")%>"
                        ></select>
                </div><br/>
                <div class="formRow" id="portal_login_as_password_wrapper" style="text-align:center; display: none;">
                    <bean:message key="enter-your-password" /> <input type="password" name="portal_login_as_password" id="portal_login_as_password"/><br/>
                </div>
                <div class="formRow"  style="text-align:center">
                    <button dojoType="dijit.form.Button" id="portal_loginasbutton" iconClass="loginAsIcon"><bean:message key="login-as" /></button>
                    <button dojoType="dijit.form.Button" iconClass="cancelIcon" onclick="portal_cancelLoginAs()"><bean:message key="cancel" /></button>
                </div>
            </form>
        </div>
    <% } %>
<!-- End Login As pop up -->

<!-- START User Detail pop up -->
    <div id="portal_myaccount_wrapper" dojoType="dijit.Dialog" style="display:none;height:400px;width:450px;vertical-align: middle;" draggable="false" >
        <div style="overflow-y:auto;" dojoType="dijit.layout.ContentPane">
            <div class="yui-g nameHeader">
                <div class="yui-u first">
                    <span id="fullUserNameMyAccount" class="fullUserName"></span>
                </div>
            </div>

            <div style="padding:0 0 10px 0; border-bottom:1px solid #ccc;">
                <form id="userInfoFormMyAccount" dojoType="dijit.form.Form">
                    <input type="hidden" name="userPasswordChanged" value="false"/>
                    <dl>
                        <% if(emailAuth) { %>
                            <dt id="userIdLabelMyAccount"><%= LanguageUtil.get(pageContext, "User-ID") %>: <input type="hidden" id="userIdMyAccount" name="userId" value=""/></dt>
                            <dd id="userIdValueMyAccount"></dd>
                        <% } else {%>
                            <dt id="userIdLabelMyAccount"><%= LanguageUtil.get(pageContext, "User-ID") %>:</dt>
                            <dd id="userIdValueMyAccount"><input id="userIdMyAccount" type="text" onkeyup="userInfoChangedMyAccount()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" disabled="disabled" /></dd>
                        <% } %>
                        <dt><%= LanguageUtil.get(pageContext, "First-Name") %>:</dt>
                        <dd><input id="firstNameMyAccount" type="text" onkeyup="userInfoChangedMyAccount()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
                        <dt><%= LanguageUtil.get(pageContext, "Last-Name") %>:</dt>
                        <dd><input id="lastNameMyAccount" type="text" onkeyup="userInfoChangedMyAccount()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
                        <dt><%= LanguageUtil.get(pageContext, "Email-Address") %>:</dt>
                        <dd><input id="emailAddressMyAccount" type="text" onkeyup="userInfoChangedMyAccount()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
                        <dt><%= LanguageUtil.get(pageContext, "Password") %>:</dt>
                        <dd><input id="passwordMyAccount" type="password" onkeyup="userPasswordChangedMyAccount()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
                        <dt><%= LanguageUtil.get(pageContext, "Password-Again") %>:</dt>
                        <dd><input id="passwordCheckMyAccount" type="password" onkeyup="userPasswordChangedMyAccount()" required="true" invalidMessage="Required." dojoType="dijit.form.ValidationTextBox" /></dd>
                    </dl>
                </form>
            </div>
            <div class="clear"></div>
            <div class="buttonRow">
                <button dojoType="dijit.form.Button" onclick="saveUserDetailsMyAccount()" type="button" iconClass="saveIcon"><%= LanguageUtil.get(pageContext, "Save") %></button>
            </div>
        </div>
    </div>
<!-- END User Detail pop up -->

<% if (APILocator.getUserAPI().isCMSAdmin(user)) { %>

    <!-- START Auto Updater pop up -->
        <div id="portal_autoupdater_wrapper" dojoType="dijit.Dialog" style="display:none;height:280px;width:450px;vertical-align: middle;" draggable="false" >
            <div style="overflow-y:auto;" dojoType="dijit.layout.ContentPane">
                <div class="yui-g nameHeader">
                    <div class="yui-u first">
                        <span class="fullUserName"><b><%= LanguageUtil.get(pageContext, "Update-available") %></b></span>
                    </div>
                </div>
                <div style="padding:0 0 10px 0; border-bottom:1px solid #ccc;" align="center">
                <b><%= LanguageUtil.get(pageContext, "You-are-running") %>:</b> <a href="javascript:whatsNew('current');"><%= ReleaseInfo.getVersion() %>(<%= LanguageUtil.get(pageContext, "Whats-new") %>)</a>
                <br />
                <br />
                <div id="minorUpdate" style="display:none;">
                <b><%= LanguageUtil.get(pageContext, "Latest-update") %>:</b> <a id="wnMinor" href="javascript:whatsNew('minor');"></a>
                </div>
                <br />
                <div id="majorUpdate" style="display:none;">
                <b><%= LanguageUtil.get(pageContext, "New-version") %>:</b> <a id="wnMajor" href="javascript:whatsNew('major');"></a>
                </div>
                <br />
                <br />
                <a href="javascript:howTo();"><%= LanguageUtil.get(pageContext, "Learn-how-to-use-the-autoupdater") %></a>
                <br />
                <br />
                </div>
                <div class="clear"></div>
                <div class="buttonRow">
                    <button dojoType="dijit.form.Button" onclick="remindMeLater()" type="button"><%= LanguageUtil.get(pageContext, "Remind-me-later") %></button>
                </div>
            </div>
        </div>
        <%}%>
    <!-- END Auto Updater pop up -->
	
	<!-- About pop up -->
	<div id="dotBackEndDialog" dojoType="dijit.Dialog" style="display:none" title="<%= LanguageUtil.get(pageContext, "about") %> dotCMS">
		<!-- Server Info -->
			<%String serverId = Config.getStringProperty("DIST_INDEXATION_SERVER_ID");%>
			<% if (UtilMethods.isSet(serverId)){ %>
				<div class="serverID"><strong>Server:</strong> <%=serverId%></div>
			<% } %>
		<!-- End Server Info -->
		<div dojoType="dijit.layout.ContentPane" style="width:400px;height:150px;" class="box" hasShadow="true" id="dotBackEndDialogCP"></div>
		<div class="copyright">&copy;<%=new GregorianCalendar().get(Calendar.YEAR)%> dotCMS Software, LLC <%= LanguageUtil.get(pageContext, "All-rights-reserved") %>.</div>
	</div>

	<!-- Support pop up -->
	<div id="showSupport" dojoType="dijit.Dialog" style="display: none">
		<table width="600"><tr>
			<td valign="top" width="50%" style="padding:10px;border-right:1px solid #dcdcdc;">
				<h2><%=LanguageUtil.get(pageContext, "Report-a-Bug") %></h2>
				<p><%=LanguageUtil.get(pageContext, "dotCMS-is-dedicated-to-quality-assurance") %></p>
				<div class="buttonRow">
					<button dojoType="dijit.form.Button" iconClass="bugIcon" onclick="window.open('https://github.com/dotCMS');">
						<%=LanguageUtil.get(pageContext, "Report-a-Bug") %>
					</button>
                </div>
			</td>
			<td valign="top" width="50%" style="padding:10px 10px 10px 20px;">
				<h2><%=LanguageUtil.get(pageContext, "Professional-Support") %></h2>
				<p><%=LanguageUtil.get(pageContext, "Let-our-support-engineers-get-you-back-on-track") %></p>
				<div style="text-align:center;font-size:146.5%;color:#990000;">+1 877-9-DOTCMS</div>
				<div style="text-align:center;font-size:77%;color:#999;"><%=LanguageUtil.get(pageContext, "Toll-Free") %>+1 877-936-8267</div>
				<div style="text-align:center;font-size:146.5%;color:#999;"><%=LanguageUtil.get(pageContext, "or") %></div>
				<div style="text-align:center;">
					<a href="http://www.dotcms.org/enterprise/" target="_blank"><%=LanguageUtil.get(pageContext, "Click-here-to-login-to-your-account") %></a>
				</div>
			</td>
		</tr></table>
	</div>		



    <%if(APILocator.getUserAPI().isCMSAdmin(user)) {
	    if(session.getAttribute("_autoupdater_showUpdate") == null) {%>
	        <script type="text/javascript" src="/dwr/interface/AutoUpdaterAjax.js"></script>
	        <script>
	            dojo.addOnLoad(function(){
	                AutoUpdaterAjax.getLatestVersionInfo(dojo.hitch(enableAutoUpdaterLink));
	            })
	        </script>
	    <%}else if((Boolean) session.getAttribute("_autoupdater_showUpdate") == true) {%>
	        <script>
	            dojo.addOnLoad(function(){
	                var enableupdatevar = {
	                        showUpdate : <%=session.getAttribute("_autoupdater_showUpdate")%>,
	                        major : "<%=session.getAttribute("_autoupdater_major")%>",
	                        minor : "<%=session.getAttribute("_autoupdater_minor")%>",
	                        buildNumber : '0'
	                }
	                if(enableupdatevar.showUpdate){
	                    enableAutoUpdaterLink(enableupdatevar);
	                }
	            })
	        </script>
    <%	}
    }%>


