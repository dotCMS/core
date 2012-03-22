<%@ include file="/html/portlet/ext/usermanager/init.jsp" %>
<%@page import="com.dotmarketing.business.APILocator"%>
<%@page import="com.dotmarketing.business.PermissionAPI"%>
<%@page import="com.dotmarketing.util.InodeUtils"%>
<% 
UserManagerForm myAccountForm =(UserManagerForm) request.getAttribute(com.dotmarketing.util.WebKeys.USERMANAGER_EDIT_FORM);
String referer = (request.getParameter("referer")!=null) ? request.getParameter("referer") : "";

String sex = null;
if (myAccountForm != null) {
	sex = myAccountForm.getSex();
}

	String userFilterInode = (String)request.getAttribute(com.dotmarketing.util.WebKeys.USER_FILTER_LIST_INODE);

	UserFilter uf = new UserFilter();
	Set<Role> readRoles = new HashSet<Role>();
	Set<Role> writeRoles = new HashSet<Role>();

	PermissionAPI perAPI = APILocator.getPermissionAPI();
	
	if (InodeUtils.isSet(userFilterInode)) {
		uf = UserFilterFactory.getUserFilter(userFilterInode);
		readRoles = perAPI.getReadRoles(uf);
		writeRoles = perAPI.getWriteRoles(uf);
	}

	boolean challengeQuestionProperty = false;

	try {
		challengeQuestionProperty = com.dotmarketing.util.Config.getBooleanProperty("USE_CHALLENGE_QUESTION");
	} catch (Exception e) {
		com.dotmarketing.util.Logger.error(this, "register_user.jsp - Need to set USE_CHALLENGE_QUESTION property.");
	}

%>

<%@page import="com.dotmarketing.factories.InodeFactory"%>


<%@page import="com.dotmarketing.db.HibernateUtil"%><script language="javascript">
<!--
	<liferay:include page="/html/js/calendar/calendar_js_box_ext.jsp" flush="true">
		<liferay:param name="calendar_num" value="1" />
	</liferay:include>
	//dateOfBirth
	function <portlet:namespace />setCalendarDate_0 (year, month, day) {
		var textbox = document.getElementById('dateOfBirth');
		textbox.value = month + '/' + day + '/' + year;
	}

//New functions for the events registration

//Submit the form
function submitForm()
{
    var UserManagerForm = document.getElementById("UserManagerForm");
//    UserManagerForm.cmd.value = "save";
	UserManagerForm.cmd.value = "save_register_user";
//	UserManagerForm.action = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/register_user" /><portlet:param name="cmd" value="save" /></portlet:actionURL>';
	UserManagerForm.action = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/edit_usermanager" /><portlet:param name="cmd" value="save_register_user" /></portlet:actionURL>';
	UserManagerForm.submit();
}

function cancel()
{
	window.location.href = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/view_usermanagerlist" /><portlet:param name="cmd" value="search" /></portlet:renderURL>';
}


function prefixChanged () {
	var obj = document.getElementById("prefix");
	if (obj.options[obj.selectedIndex].value == "other") {
		document.getElementById("otherPrefix").disabled = false;
		document.getElementById("otherPrefix").focus();
		
	} else {
		document.getElementById("otherPrefix").disabled = true;
	}
}

function addressSwitch(){
	var ele = document.getElementById("noAddress");
	if(ele.checked){
		document.getElementById("street1").disabled=true;
		document.getElementById("street2").disabled=true;
		document.getElementById("city").disabled=true;
		document.getElementById("state").disabled=true;
		document.getElementById("country").disabled=true;
		document.getElementById("zip").disabled=true;
		document.getElementById("phone").disabled=true;
		document.getElementById("fax").disabled=true;
		document.getElementById("cell").disabled=true;
	}
	else{
		document.getElementById("street1").disabled=false;
		document.getElementById("street2").disabled=false;
		document.getElementById("city").disabled=false;
		document.getElementById("state").disabled=false;
		document.getElementById("country").disabled=false;
		document.getElementById("zip").disabled=false;
		document.getElementById("phone").disabled=false;
		document.getElementById("fax").disabled=false;
		document.getElementById("cell").disabled=false;
	}


}

function randomPassword(){

  chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!@#$%^&*()_+][;?><,.-=";
  pass = "";
  for(x=0;x<10;x++){
    i = Math.floor(Math.random() * chars.length-1);
    pass += chars.charAt(i);
  }
 
  document.getElementById("password").value=pass;
  document.getElementById("verifyPassword").value=pass;
  
 
}


-->
</script>

<table border="0" width="100%" cellpadding="0" cellspacing="0" class="portletMenu">
	<tr style="height:25px;">
		<td width="100%" align="left">
			<a href="#" id="main_tab" class="alpha">
		     <%= LanguageUtil.get(pageContext, "Edit-usermanager-userinfo-box-title") %>
		</td>
	</tr>

	<tr class="blue_Border" >
		<td><img border="0" height="5" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
	</tr>
	<tr>
		<td><img border="0" height="5" hspace="0" src="<%= COMMON_IMG %>/spacer.gif" vspace="0" width="1"></td>
	</tr>
				
</table>


<table cellpadding="0" cellspacing="0" border="0" class="portletBox" width="100%">
<tr>
<td align="center">


<table align="center">
	<tr>
		<td></td>
	</tr>
</table>

<%--form action="/ext/usermanager/register_user" method="post" id="UserManagerForm" name="UserManagerForm" autocomplete="off"--%>
<form action="/ext/usermanager/edit_usermanager" method="post" id="UserManagerForm" name="UserManagerForm" autocomplete="off">


	<%--input type="hidden" name="cmd"--%>
	<input type="hidden" name="cmd" value="load_register_user">
	
	<input type="hidden" id="referer" name="referer" value="<%=referer%>">

<table border="0" cellpadding="0" cellspacing="0" width="100%">
<tr>
	<td valign="top" width="46%" align="right">

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"main-profile\") %>" />
	<liferay:param name="box_width" value="<%= Integer.toString((int)(RES_TOTAL * .46)) %>" />
<table align="center">



	<c:if test="<%= company.getAuthType().equals(Company.AUTH_TYPE_EA) %>">
		<input type="hidden" name="userID" value="<%= myAccountForm.getUserID() %>">
	</c:if>
	<input type="hidden" name="userProxyInode" value="<%= myAccountForm.getUserProxyInode() %>">
	<c:if test="<%= company.getAuthType().equals(Company.AUTH_TYPE_ID) %>">
		<tr>
			<td><span class="required"></span> </td>
			<td>
				<font class="gamma" size="2">
				<b><%= LanguageUtil.get(pageContext, "user-id") %></b>
				</font>
			</td>
			<td>
				<input type="text" name="userID" tabindex="1" id="userID" value="<%= myAccountForm.getUserID()==null ? "": myAccountForm.getUserID()%>"  class="form-text" />
			</td>
		</tr>
	</c:if>
	<tr>
		<td><span class="required"></span> </td>
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "first-name") %></b>
			</font>
		</td>
		<td>
			<input type="text" name="firstName" tabindex="1" id="firstName" value="<%= myAccountForm.getFirstName() %>"  class="form-text" />
		</td>
	</tr>
	<tr>
		<td></td>
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "middle-name") %></b>
			</font>
		</td>
		<td>
			<input type="text" name="middleName" tabindex="2" id="middleName" value="<%= myAccountForm.getMiddleName() %>"  class="form-text" />
		</td>
	</tr>
	<tr>
		<td><span class="required"></span> </td>
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "last-name") %></b>
			</font>
		</td>
		<td>
			<input type="text" name="lastName" tabindex="3" id="lastName" value="<%= myAccountForm.getLastName() %>"  class="form-text" />
		</td>
	</tr>
	<tr>
		<td><span class="required"></span> </td>

		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "email-address") %></b>
			</font>
		</td>
		<td>
			<input style="width:200px;" type="text" tabindex="6" value="<%= myAccountForm.getEmailAddress() %>" name="emailAddress" id="emailAddress" class="form-text"/>
		</td>
	</tr>
	<tr>
		<td></td>

		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "Nickname") %></b>
			</font>
		</td>
		<td>
			<input type="text" name="nickName" tabindex="7" id="nickName" value="<%= myAccountForm.getNickName() %>"  class="form-text" />
		</td>
	</tr>
	<tr>
		<td><span class="required"></span> </td>
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "password") %></b>
			</font>
		</td>
		<td>
			<input type="password" autocomplete="off" tabindex="10" value="<%= myAccountForm.getPassword() %>"  name="password" id="password" class="form-text" />
			&nbsp;
            <button dojoType="dijit.form.Button" onclick="randomPassword()"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "random")) %></button>
		</td>
	</tr>
	<tr>
		<td><span class="required"></span> </td>
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "Verify-Password") %></b>
			</font>
		</td>
		<td>
			<input type="password" autocomplete="off" tabindex="11" name="verifyPassword" value="<%= myAccountForm.getVerifyPassword() %>" id="verifyPassword"  class="form-text" />
		</td>
	</tr>

<% if (challengeQuestionProperty) { %>	
	<tr>
		<td><span class="required"></span> </td>
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "Challenge-Question") %></b>
			</font>
		</td>
		<td>
<%
List challengeQuestions = com.dotmarketing.util.UserUtils.getChallengeQuestionList();

%>
			<select tabindex="12" style="width:200px;" name="challengeQuestionId" id="challengeQuestionId" class="form-text">
				<option value="0">Select a question</option>
<%
	com.dotmarketing.beans.ChallengeQuestion challengeQuestion;
	for (int i = 0; i < challengeQuestions.size(); ++i) {
		challengeQuestion = (com.dotmarketing.beans.ChallengeQuestion) challengeQuestions.get(i);
%>
				<option value="<%= challengeQuestion.getChallengeQuestionId() %>" <%= (myAccountForm.getChallengeQuestionId() != null) && (myAccountForm.getChallengeQuestionId().equals("" + challengeQuestion.getChallengeQuestionId())) ? "selected" : "" %> ><%= challengeQuestion.getChallengeQuestionText() %></option>
<%
	}
%>
			</select>
		</td>
	</tr>
	<tr>
		<td><% if (challengeQuestionProperty) { %><span class="required"></span> <% } %></td>
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "Challenge-Question-Answer") %></b>
			</font>
		</td>
		<td>
			<input type="text" tabindex="13" name="challengeQuestionAnswer" id="challengeQuestionAnswer" value="<%= myAccountForm.getChallengeQuestionAnswer() %>" class="form-text" />
		</td>
	</tr>
<% } %>
	<tr>
		<td></td>
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "Birthday") %></b>
			</font>
		</td>
		<td>
			<input type="text" name="dateOfBirth" tabindex="14" id="dateOfBirth" value="<%= (myAccountForm.getDateOfBirth()!=null)?myAccountForm.getDateOfBirth():"" %>"  class="form-text" />
			<img align="absmiddle" border="0" hspace="0" id="<portlet:namespace />calendar_input_0_button" src="<%= COMMON_IMG %>/calendar/calendar.gif" vspace="0" onClick="<portlet:namespace />calendarOnClick_0();">
		</td>
	</tr>
	<tr>
		<td></td>
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "Sex") %></b>
			</font>
		</td>
		<td>
			<select tabindex="15" id="sex" name="sex" class="form-text">
				<option <%= ((sex!=null) && (sex.equalsIgnoreCase("M"))) ? "selected" : "" %> value="M"><%= LanguageUtil.get(pageContext, "Male") %></option>
				<option <%= ((sex!=null) && (sex.equalsIgnoreCase("F"))) ? "selected" : "" %> value="F"><%= LanguageUtil.get(pageContext, "Female") %></option>
			</select>
		</td>
	</tr>
	<tr>
		<td colspan="3">
			&nbsp; &nbsp; <span class="required"></span>  = <%= LanguageUtil.get(pageContext, "Required-Fields") %>
		</td>
	</tr>
</table>
	
</liferay:box>

<br>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"Other-User-Information\") %>" />
	<liferay:param name="box_width" value="<%= Integer.toString((int)(RES_TOTAL * .46)) %>" />

<table align="center">
	<tr>
		<td>
	       	<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "Prefix") %></b>
			</font>
			<% 	String strPrefix = myAccountForm.getPrefix();
				if(!UtilMethods.isSet(strPrefix)){
					strPrefix = "";
				}
				boolean isOther = false;
				if(!strPrefix.equals("mr") && !strPrefix.equals("mrs") && !strPrefix.equals("miss") && !strPrefix.equals("dr")){
					isOther = true;
				}
				
			%>
			</td>
			<td>
			<select tabindex="16" id="prefix" name="prefix" onchange="prefixChanged(this)"  class="form-text">
				<option value="mr" <%= (strPrefix.equals("mr")) ? "selected" : "" %>><%= LanguageUtil.get(pageContext, "Mr") %>.</option>
				<option value="mrs" <%= (strPrefix.equals("mrs")) ? "selected" : "" %>><%= LanguageUtil.get(pageContext, "Mrs") %>.</option>
				<option value="miss" <%= (strPrefix.equals("miss")) ? "selected" : "" %>><%= LanguageUtil.get(pageContext, "Mst") %>.</option>
				<option value="dr" <%= (strPrefix.equals("dr")) ? "selected" : "" %>><%= LanguageUtil.get(pageContext, "Dr") %>.</option>
				<option value="other" <%= (strPrefix.equals("other")) ? "selected" : "" %>><%= LanguageUtil.get(pageContext, "Other") %> : </option>
			</select>
			<input tabindex="8" style="width:85px;" type="text"  class="form-text" value="<%= myAccountForm.getOtherPrefix() %>" name="otherPrefix" id="otherPrefix" <%= (!isOther) ? "disabled" : "" %>/>
		</td>
	</tr>

	<tr>
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "Suffix") %></b>
			</font>
		</td>
		<td>
			<input type="text" name="suffix" tabindex="17" id="suffix" value="<%= myAccountForm.getSuffix() %>"  class="form-text" />
		</td>
	</tr>
	<tr>
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "Title") %></b>
			</font>
		</td>
		<td>
			<input type="text" name="title" id="title" tabindex="18" value="<%= myAccountForm.getTitle() %>"  class="form-text" />
		</td>
	</tr>
	<tr>	
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "School") %></b>
			</font>
		</td>
		<td>
			<input type="text" name="school" id="school" tabindex="19" value="<%= myAccountForm.getSchool() %>"  class="form-text" />
		</td>
	</tr>
	<tr>	
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "Graduation-Year") %></b>
			</font>
		</td>
		<td>
			<input type="text" name="graduation_year" id="graduation_year" tabindex="20" value="<%= myAccountForm.getGraduation_year() %>"  class="form-text" />
		</td>
	</tr>
</table>
</liferay:box>

<br>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"Role-Permission\") %>" />
	<liferay:param name="box_width" value="<%= Integer.toString((int)(RES_TOTAL * .46)) %>" />
	<%= LanguageUtil.get(pageContext, "Please-select-the-Roles-that-will-have-permissions-to-View-or-Modify-this-user") %>
<table align="center" border="0" cellpadding="2" cellspacing="2" id="permissions"  width="100%">
	<tr>
		<td>
			<% int numberColumnsRoles = 1; %>
			<% String width = "400px"; %>
			<%@ include file="/html/portlet/ext/usermanager/select_permissions_inc.jsp" %>
		</td>
	</tr>
</table>
</liferay:box>


	</td>
	<td width="3%">&nbsp;
		
	</td>
	<td valign="top" width="46%">


<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"Address-Phone\") %>" />
	<liferay:param name="box_width" value="<%= Integer.toString((int)(RES_TOTAL * .46)) %>" />
	<input type="hidden" name="addressID" value="<%= myAccountForm.getAddressID() %>">
<table align="center" >
	<tr>
		<td colspan="2">
		<input onclick="addressSwitch()" id="noAddress" tabindex="21" type="radio" name="description" value="none" <%= (myAccountForm.getDescription().equals("none") || myAccountForm.getDescription().equals("")) ? "checked" : "" %>><label for="noAddress"><%= LanguageUtil.get(pageContext, "N-A") %></label>&nbsp;

		<input onclick="addressSwitch()" id="work" tabindex="21" type="radio" name="description" value="work" <%= (myAccountForm.getDescription().equals("work")) ? "checked" : "" %>><label for="work"><%= LanguageUtil.get(pageContext, "Work") %></label>&nbsp;


		<input onclick="addressSwitch()" id="home" tabindex="21" type="radio" name="description" value="home" <%= (myAccountForm.getDescription().equals("home")) ? "checked" : "" %>><label for="home"><%= LanguageUtil.get(pageContext, "Home")%> </label>&nbsp;

		<input onclick="addressSwitch()" id="other" tabindex="21" type="radio" name="description" value="other" <%= (myAccountForm.getDescription().equals("other")) ? "checked" : "" %>><label for="other"><%= LanguageUtil.get(pageContext, "Other") %></label>
		</td>
	</tr>
	<tr>
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "Street") %> 1</b>
			</font>
		</td>
		<td>
			<input tabindex="22" type="text" id="street1" name="street1" value="<%= myAccountForm.getStreet1() %>" id="street1"  class="form-text" />
		</td>
	</tr>
	<tr>
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "Street") %> 2</b>
			</font>
		</td>
		<td>
			<input tabindex="23" type="text" id="street2" name="street2" value="<%= myAccountForm.getStreet2() %>" id="street2"  class="form-text" />
		</td>
	</tr>
	<tr>
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "City") %></b>
			</font>
		</td>
		<td>
			<input tabindex="24" type="text" id="city" name="city" value="<%= myAccountForm.getCity() %>" id="city"  class="form-text" />
		</td>
	</tr>
	<tr>
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "State") %></b>
			</font>
		</td>
		<td>
			<input tabindex="25" type="text" id="state" name="state" value="<%= myAccountForm.getState() %>" id="state"  class="form-text"  MAXLENGTH ="2"/>
		</td>
	</tr>
	<tr>
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "Country") %></b>
			</font>
		</td>
		<td>
		<% 
			String country = myAccountForm.getCountry();
			if(com.dotmarketing.util.UtilMethods.isSet(country)){
				country = "\"<bean:write property='country' name='myAccountForm'/>\"";
			}
			else {
				country = "\"United States of America\"";
			}
		%>
			<script language="javascript">writeCountriesSelect("country", <%=country%>);</script>
		</td>
	</tr>
	<tr>
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "Zip") %></b>
			</font>
		</td>
		<td>
			<input tabindex="26" type="text"  name="zip" value="<%= myAccountForm.getZip() %>" id="zip"  class="form-text" />
		</td>
	</tr>
	<tr>
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "Phone") %></b>
			</font>
		</td>
		<td>
			<input tabindex="27" type="text"  name="phone" value="<%= myAccountForm.getPhone() %>" id="phone"  class="form-text" />
		</td>
	</tr>
	<tr>
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "Cell") %></b>
			</font>
		</td>
		<td>
			<input tabindex="28" type="text" name="cell" id="cell" value="<%= myAccountForm.getCell() %>"  class="form-text" />
		</td>
	</tr>
	<tr>
		<td>
			<font class="gamma" size="2">
			<b><%= LanguageUtil.get(pageContext, "Fax") %></b>
			</font>
		</td>
		<td>
			<input tabindex="29" type="text" name="fax" id="fax" value="<%= myAccountForm.getFax() %>"  class="form-text" />
		</td>
	</tr>
</table>
</liferay:box>
	<!-- END Mailing Address -->
<br>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"Categories-title\") %>" />
	<liferay:param name="box_width" value="<%= Integer.toString((int)(RES_TOTAL * .46)) %>" />

</liferay:box>

	</td>


	</tr>
	<tr>
		<td align="center" colspan="3">
            <button dojoType="dijit.form.Button" tabindex="50" onClick="submitForm();"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %></button>
            &nbsp;&nbsp;
            <button dojoType="dijit.form.Button" onClick="cancel();" tabindex="51"><%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %></button>
		</td>
	</tr>
</table>



</form>

</td></tr>
<table>

</table>

<script language="javascript">
addressSwitch();
prefixChanged();
</script>
