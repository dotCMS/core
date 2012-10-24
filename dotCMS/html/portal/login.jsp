<%@page import="com.dotmarketing.business.UserAPI"%>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@ include file="/html/portal/init.jsp" %><%@ include file="/html/common/top_inc.jsp" %><%
String cmd = ParamUtil.getString(request, "my_account_cmd");
session.removeAttribute(WebKeys.USER_ID);
String emailAddress = request.getParameter("my_account_email_address");
Xss.strip(emailAddress);
if ((emailAddress == null) || (emailAddress.equals("null"))) {
	emailAddress = "";
}

String login = request.getParameter("my_account_login");
if(!UtilMethods.isSet(login)){
	login = (String) session.getAttribute("_failedLoginName");
	
}
if(!UtilMethods.isSet(login)){
	login = GetterUtil.getString(CookieUtil.get(request.getCookies(), CookieKeys.LOGIN));
	if (Validator.isNull(login) && company.getAuthType().equals(Company.AUTH_TYPE_EA)) {
		login = "@" + company.getMx();
	}
}
login = Xss.strip(login);



String uId = null;
Cookie[] cookies = request.getCookies();
if(cookies != null){
	for(Cookie c : cookies){
	
		if(CookieKeys.ID.equals(c.getName())){
			try{
				uId = PublicEncryptionFactory.decryptString(c.getValue());
			}
			catch(Exception e){
				Logger.info(this, "An ivalid attempt to login as " + uId + " has been made from IP: " + request.getRemoteAddr());
				uId = null;
			}
		}
		
	}
}
if(UtilMethods.isSet(uId)){	
	session.setAttribute(WebKeys.USER_ID, uId);
	String referer = (String)session.getAttribute(WebKeys.REFERER);

	//DOTCMS-4943
	UserAPI userAPI = APILocator.getUserAPI();			
	boolean respectFrontend = WebAPILocator.getUserWebAPI().isLoggedToBackend(request);
	User loggedInUser = userAPI.loadUserById(uId, userAPI.getSystemUser(), respectFrontend);
	session.setAttribute(org.apache.struts.Globals.LOCALE_KEY, loggedInUser.getLocale());
	
	if(UtilMethods.isSet(referer)){
		session.removeAttribute(WebKeys.REFERER);
		response.sendRedirect(referer);
		return;
	}
	referer = (String)request.getAttribute(WebKeys.REFERER);
	if(UtilMethods.isSet(referer)){
		session.removeAttribute(WebKeys.REFERER);
		response.sendRedirect(referer);
		return;
	}
	out.println("<script>");
	out.println("window.location='/c/';");
	out.println("</script>");
	return;
	
}










boolean editPassword = Boolean.valueOf(PropsUtil.get("password.forgot.show"));
boolean rememberMe = ParamUtil.get(request, "my_account_r_m", false);

//PortletURL createAccountURL = new PortletURLImpl(request, PortletKeys.MY_ACCOUNT, layout.getId(), true);

//createAccountURL.setWindowState(WindowState.MAXIMIZED);
//createAccountURL.setPortletMode(PortletMode.VIEW);

//createAccountURL.setParameter("struts_action", "/my_account/create_account");

//String createAccountURLToString = createAccountURL.toString();



//Build errors

String errorMessage = null;
if(cmd.equals("send") && SessionErrors.contains(request, NoSuchUserException.class.getName())){
	errorMessage = LanguageUtil.get(pageContext, "the-email-address-you-requested-is-not-registered-in-our-database") ;
} 
else if(cmd.equals("send") && SessionErrors.contains(request, SendPasswordException.class.getName())){
	errorMessage = LanguageUtil.get(pageContext, "a-new-password-can-only-be-sent-to-an-external-email-address");
}
else if(cmd.equals("send") && SessionErrors.contains(request, UserEmailAddressException.class.getName())){
	errorMessage = LanguageUtil.get(pageContext, "please-enter-a-valid-email-address");
}
else if(cmd.equals("send") && SessionMessages.contains(request, "new_password_sent")){
	String recipient = (String)SessionMessages.get(request, "new_password_sent");
	errorMessage = LanguageUtil.format(pageContext, "a-new-password-has-been-sent-to-x", recipient, false);
} 

else  if(cmd.equals("auth") && SessionErrors.contains(request, NoSuchUserException.class.getName()) || SessionErrors.contains(request, UserEmailAddressException.class.getName())){
	errorMessage = LanguageUtil.get(pageContext, "please-enter-a-valid-login");
	SessionErrors.clear(request);
}
else  if(cmd.equals("auth") && SessionErrors.contains(request, AuthException.class.getName())|| SessionErrors.contains(request, UserEmailAddressException.class.getName())){
	 errorMessage = LanguageUtil.get(pageContext, "authentication-failed");
}
else  if(cmd.equals("auth") && SessionErrors.contains(request, AuthException.class.getName())){
	 errorMessage = LanguageUtil.get(pageContext, "authentication-failed");
}
else  if(cmd.equals("auth") && SessionErrors.contains(request, UserPasswordException.class.getName())){
	 errorMessage = LanguageUtil.get(pageContext, "please-enter-a-valid-password");
}
else  if(cmd.equals("auth") && SessionErrors.contains(request, RequiredLayoutException.class.getName())){
	 errorMessage = LanguageUtil.get(pageContext, "user-without-portlet");
}
else  if(cmd.equals("auth") && SessionErrors.contains(request, UserActiveException.class.getName())){
	 errorMessage = LanguageUtil.format(pageContext, "your-account-is-not-active", new LanguageWrapper[] {new LanguageWrapper("<b><i>", login, "</i></b>")}, false); 
}
if(errorMessage != null){
	session.setAttribute("_dotLoginMessages", errorMessage);
	session.setAttribute("_failedLoginName", login);
	SessionErrors.clear(request);
	out.println("<html><head><script>top.location = '/c/portal_public/login';</script></head><body></body></html>");
	return;
}
%>
	



<!-- 
<jsp:include page="/html/portal/about.jsp"></jsp:include>


 -->

<script type="text/javascript" src="https://ajax.googleapis.com/ajax/libs/chrome-frame/1/CFInstall.min.js"></script>

<style>
	body{background-color:<%= company.getSize() %>;background-image:url(<%= dotBackImage %>);background-repeat:no-repeat;background-position:bottom top;background-size:100% 100%;}
	#loginBox, #forgotPassword{-moz-box-shadow:2px 2px 8px #274665;-webkit-box-shadow:2px 2px 8px #274665;width:450px;}
	.dijitTooltipFocusNode:focus{outline: none;}
	.dijitDialogUnderlay{opacity: 0.2;}
	.bannerBG, .imageBG{display:none;}
	.chromeFrameOverlayContent{}
	.chromeFrameOverlayContent iframe{}
	.chromeFrameOverlayCloseBar{display:none;}
	.chromeFrameOverlayUnderlay{background-color:#222;}
</style>


<%@page import="com.dotmarketing.cms.factories.PublicEncryptionFactory"%>
<%@page import="com.dotmarketing.util.Logger"%>
<script type="text/javascript">
	
	dojo.addOnLoad(function(){
		if (dojo.isIE <= 7) {
			CFInstall.check({
				mode: "overlay"
			});
		}else{
			showLogin();
		}
	});
	
	
   //dojo.addOnLoad(showLogin);
   
	function showLogin(){
	       var myDialog = dijit.byId("loginBox");
	       dojo.style(myDialog.closeButtonNode, "visibility", "hidden"); 
	       myDialog.tabStart = dojo.byId("loginPasswordTextBox");
	       myDialog.show();
	       setTimeout("dijit.byId('loginPasswordTextBox').focus()",200);
	}
   
	function showForgot(){
		var myDialog = dijit.byId("loginBox");
		myDialog.hide();
		
		myDialog = dijit.byId("forgotPassword");
		myDialog.connect(myDialog,"hide",showLogin); 
		
		myDialog.show();
	}

	dojo.require("dojox.validate.web");
	function signIn() {

		// set values
		document.fm.my_account_cmd.value = "auth";
		document.fm.referer.value = "<%= CTX_PATH %>";
		
		var loginTextValue = dojo.byId("loginTextBox").value;
		
		<%if(company.getAuthType().equals(Company.AUTH_TYPE_EA)){%>
			if(!dojox.validate.isEmailAddress(loginTextValue)){
				dojo.byId("dotLoginMessagesDiv").innerHTML = '<%= LanguageUtil.get(pageContext, "please-enter-a-valid-email-address") %>';
				return false;
			}
		<%}else{%>
			if((loginTextValue.length == 0)					
					|| (loginTextValue.indexOf('>') != -1)
					|| (loginTextValue.indexOf('<') != -1)){
				dojo.byId("dotLoginMessagesDiv").innerHTML = '<%= LanguageUtil.get(pageContext, "please-enter-a-valid-user-id") %>';
				return false;
			}
		<%}%>
		dojo.byId("my_account_login").value = dojo.byId("loginTextBox").value;
		dojo.byId("password").value = dojo.byId("loginPasswordTextBox").value;


		var myDialog = dijit.byId("loginBox");
		myDialog.hide();

		var myDialog = dijit.byId("progressBarBox");
	    dojo.style(myDialog.closeButtonNode, "visibility", "hidden"); 
		myDialog.show();
		var progressBarBox = dijit.byId("progressBarBox");


        numParts = Math.floor(100/7);
        jsProgress.update({ maximum: numParts, progress:0 });
        for (var i=0; i<=numParts; i++){
            // This plays update({progress:0}) at 1nn milliseconds,
            // update({progress:1}) at 2nn milliseconds, etc.
            setTimeout(
               "jsProgress.update({ progress: " + i + " })",
               (i+1)*100 + Math.floor(Math.random()*100)
            );
        }

		document.fm.submit();
	}

	function forgotPassword() {
		if (confirm('<%= UnicodeLanguageUtil.get(pageContext, "a-new-password-will-be-generated-and-sent-to-your-external-email-address") %>')) { 
			document.fm.my_account_cmd.value = 'send'; 

			dojo.byId("my_account_email_address").value = dojo.byId("forgotPasswordEmailBox").value;

			document.fm.submit();
		}
	}



	dojo.connect(dojo.byId("loginBox"), "onkeypress", function(e){
	        var key = e.keyCode || e.charCode;
	        var k = dojo.keys;
	        if (key == 13) {
	                signIn();
	        }
	}); 


function showLanguageSelector(){

	var test =dojo.byId("");

}


</script>


	<% if(editPassword){ %>
		<div id="forgotPassword" style="display:none" draggable="false" dojoType="dijit.Dialog" title="<%= LanguageUtil.get(pageContext, "forgot-password") %>">
			<dl>
				<dt><label for="forgotPasswordEmailBox" class="formLabel"><%if(company.getAuthType().equals(Company.AUTH_TYPE_EA)){%>
						<%= LanguageUtil.get(pageContext, "email-address") %> : 
					<%}else{ %>
						<%= LanguageUtil.get(pageContext, "user-id") %> : 
					<%} %></label></dt>
				<dd><input id="forgotPasswordEmailBox" name="forgotPasswordEmailBox" type="text"  value="<%= login %>"  dojoType="dijit.form.TextBox"></dd>
				<dd><button dojoType="dijit.form.Button"  onClick="forgotPassword()"><%= LanguageUtil.get(pageContext, "get-new-password") %></button></dd>
			</dl>
		</div>
	<% } %>
	

	<div id="loginBox" dojoType="dijit.Dialog" draggable="false" style="display:none" title="<%= LanguageUtil.get(pageContext, "Login") %>">

		<%if(session.getAttribute("_dotLoginMessages") != null ){ %>
		<%String myMessages = (String) session.getAttribute("_dotLoginMessages") ; %>
		
			<div class="error-message" id="dotLoginMessagesDiv"><%=myMessages%></div>
			
		<%session.removeAttribute("_dotLoginMessages");  %>
		<%}else{ %>
			<div class="error-message" id="dotLoginMessagesDiv"></div>
		<%} %>
		<dl>
			<dt style="width:180px">
				<label for="loginTextBox">
					<%if(company.getAuthType().equals(Company.AUTH_TYPE_EA)){%>
						<%= LanguageUtil.get(pageContext, "email-address") %> : 
					<%}else{ %>
						<%= LanguageUtil.get(pageContext, "user-id") %> : 
					<%} %>
				</label>
			</dt>
			<dd><input name="loginTextBox" id="loginTextBox" dojoType="dijit.form.TextBox" size="25" required="true" type="text" tabindex="1"  value="<%= login %>"></dd>
			
			<dt style="width:180px"><label for="loginPasswordTextBox"><%= LanguageUtil.get(pageContext, "password") %> : </label></dt>
			<dd><input name="loginPasswordTextBox" id="loginPasswordTextBox" dojoType="dijit.form.TextBox" size="25" required="true" type="password" value="" tabindex="2" ></dd>
			
			<dt style="width:180px"><label for="rememberMe"><%= LanguageUtil.get(pageContext, "remember-me") %> &nbsp;</label></dt>
			<dd>
				<c:if test="<%= company.isAutoLogin()%>">
					<input id="rememberMe" tabindex="3" <%= rememberMe ? "checked" : "" %> type="checkbox"  dojoType="dijit.form.CheckBox"
					onclick="
					<c:if test="<%= company.isAutoLogin() && !request.isSecure() %>">
						if (this.checked) {document.fm.my_account_r_m.value = 'on';}else {document.fm.my_account_r_m.value = 'off';}
					</c:if>">
				</c:if>
			</dd>
		</dl>		

		<!-- Button Row --->
			<div class="buttonRow">
				<button dojoType="dijit.form.Button" iconClass="loginIcon" tabindex="4"  onClick="signIn();return false;">
					<%= LanguageUtil.get(pageContext, "sign-in") %>
				</button>
			</div>
			
			<div
				style="float: left;  cursor: pointer;padding-bottom:6px;" class="inputCaption" 
				onmousedown="dijit.popup.open({popup: myDialog, around: dojo.byId('myLanguageImage')})">
					<img title="" id="myLanguageImage" alt="" src="/html/images/languages/<%= locale.getLanguage() + "_" + locale.getCountry() %>.gif" align="left" style="padding:1px;border:1px solid #ffffff">
					<%-- <%=locale.getDisplayLanguage(locale)%> --%>
			</div>



			<%------  Language Selector -----%>
			    <div id="languageSelectorBar" style="visibility: hidden; display: none;" title="Select Language">
			    	<div style="text-align: right;margin-right:-5px;margin-top:-3px;">
			    		<img onclick="dijit.popup.close(myDialog);" alt="<%= LanguageUtil.get(pageContext, "close") %>" title="<%= LanguageUtil.get(pageContext, "close") %>" src="/html/js/dojo/release/dojo/dijit/themes/dmundra/images/tabCloseHover.png" width="10" height="10" style="cursor: pointer;">
			    		
			    	</div>
			    	<div style="text-align: center;padding-left:10px;padding-right:10px;border: none;">
				    	<% Locale[] locales = LanguageUtil.getAvailableLocales();%>
					    <% for (int i = 0; i < locales.length; i++) { %>
						    <%if(locale.equals(locales[i])){ %>
								<img title="<%= locales[i].getDisplayLanguage(locales[i])%>" alt="<%= locales[i].getDisplayLanguage(locales[i])%>" src="/html/images/languages/<%= locales[i].getLanguage() + "_" + locales[i].getCountry() %>.gif" style="padding:2px;border:1px solid blue;margin-right:3px;">
						   	<%}else{ %>
						   		<img onclick="window.location='/html/portal/login.jsp?switchLocale=<%= locales[i].getLanguage() + "_" + locales[i].getCountry() %>';" title="<%= locales[i].getDisplayLanguage(locales[i])%>" alt="<%= locales[i].getDisplayLanguage(locales[i])%>" src="/html/images/languages/<%= locales[i].getLanguage() + "_" + locales[i].getCountry() %>.gif" style="padding:2px;border:1px solid #dddddd;cursor: pointer;">
						   	<%} %>
					    <% } %>
				    </div>
			   </div>
			<%------/  Language Selector -----%>
			
			
			<% if(editPassword){ %>
				<div class="inputCaption" style="float:right;">
					<a href="javascript:showForgot()"><%= LanguageUtil.get(pageContext, "forgot-password") %></a>
				</div>
			<%} %>
		<!-- /Button Row --->
	</div>

	<div id="progressBarBox" dojoType="dijit.Dialog" style="display:none" title="<%= LanguageUtil.get(pageContext, "login") %>">
		<center>
			<div dojoType="dijit.ProgressBar" style="width:200px;text-align:center;" jsId="jsProgress" id="downloadProgress"></div>
		</center>
	</div>

</div>


<!-- Get Chrome for IE7 -->

<div id="getChrome" style="display:none" draggable="false" dojoType="dijit.Dialog" title="Get Chrome Plugin for IE7">
	<h2>You are using a no supported version of Internet Explore!</h2>
	<p>You have two options:1. Update your browser to the newest version or 2. install the Chrome plugin.</p>
	<div class="buttonRow">
			<button dojoType="dijit.form.Button" iconClass="loginIcon" tabindex="4"  onClick="">
				Get Google Chrome Plugin
			</button>
		</div>
</div>


<form action="<%= CTX_PATH %>/portal<%= PortalUtil.getAuthorizedPath(request) %>/login" method="post" name="fm" target="actionJackson">
	<input name="my_account_cmd" type="hidden" value="">
	<input name="referer" type="hidden" value="<%= CTX_PATH %>">
	<input name="my_account_r_m" id="my_account_r_m" type="hidden" value="<%= rememberMe %>">
	<input name="password" id="password" type="hidden" value="">
	<input name="my_account_login" id="my_account_login" type="hidden" value="">
	<input name="my_account_email_address" id="my_account_email_address" type="hidden" value="">
	
</form>

<div class="inputCaption" style="color:#dddddd;text-align:right;position:absolute;bottom:10px; right:10px;">
	<%String serverId = Config.getStringProperty("DIST_INDEXATION_SERVER_ID");%>
	<% if (UtilMethods.isSet(serverId)){ %>
		<%= LanguageUtil.get(pageContext, "Server") %>: <%=serverId%> <br />
	<%} %>
	<%= System.getProperty("dotcms_level_name")%>
	<%= ReleaseInfo.getVersion() %><br/>
	(<%= ReleaseInfo.getBuildDateString() %>)
</div>
<iframe name="actionJackson" id="actionJackson" style="width:0px;height:0px;" src="/html/portal/touch_protected.jsp" ></iframe>
<script type="text/javascript">
	var myDialog = new dijit.TooltipDialog({style:'display:none;'}, "languageSelectorBar");
	myDialog.startup();
	
</script>
<%@ include file="/html/common/bottom_inc.jsp" %>
