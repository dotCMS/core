<%@ include file="/html/portlet/ext/usermanager/init.jsp" %>

<%@ page import="com.dotmarketing.portlets.usermanager.struts.UserManagerForm" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.util.UtilHTML" %>
<% 
UserManagerForm myAccountForm =(UserManagerForm) request.getAttribute(com.dotmarketing.util.WebKeys.USERMANAGER_EDIT_FORM);
String referrer = request.getParameter("referer");

boolean challengeQuestionProperty = false;

try {
	challengeQuestionProperty = com.dotmarketing.util.Config.getBooleanProperty("USE_CHALLENGE_QUESTION");
} catch (Exception e) {
	com.dotmarketing.util.Logger.error(this, "profile_password.jsp - Need to set USE_CHALLENGE_QUESTION property.");
}	
%>

<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<script type='text/javascript' src='/dwr/interface/OrganizationAjax.js'></script>
<script type='text/javascript' src='/dwr/engine.js'></script>
<script type='text/javascript' src='/dwr/util.js'></script>
<script>
function prefixChanged () {
	if (dijit.byId('prefix').attr('value') == "other") {
		dijit.byId('otherPrefix').attr('disabled', false);
		dijit.byId('otherPrefix').focus();
		
	} else {
		dijit.byId('otherPrefix').attr('disabled', true);
	}
}
	
/*Open the Facility PopUp*/
function openPopUp(url)
{
	var width = 850;
	var height = 575;
	var left = Math.floor( (screen.width - width) / 2);
	var top = Math.floor( (screen.height - height) / 2);
	var parameter = 'scrollbars=yes,resizable=yes,status=yes,toolbar=no,width=' + width + ',height=' + height+ ',top=' + top + ',left='+ left;
		
	window.open(url,'facility',parameter,false);
	toggleBox('organizationsLayer',0);
	
}
	
/*Callback from the facility PopUp*/
function callback(organizationInode,organizationTitle,street1,street2,state,city,zip,phone,fax)
{
	document.getElementById('organizationInodeAux').value = organizationInode;
	document.getElementById('organizationTitle').value = organizationTitle;
}

function getRadio(organization) { 	     
	return "<input type='radio' value='" + organization["inode"] + "' onclick='OrganizationAjax.getOrganizationMap(organizationSelected, " + organization["inode"] + ")' name='organizationInode' id='organizationInode'>";
}
	  
function getName(organization) 
{ 
  return organization["title"]; 
}
	  
function getAddress(organization) { 
  	var address = "(" + organization["street1"];
  	if (organization["street2"] != "")
  		address +=  " " + organization["street2"];
  		address +=  ", " + organization["state"];
  		address +=  ", " + organization["city"];
  		address +=  ", " + organization["zip"] + ")";	  	
	  	return address; 
}

function direct(data) { 
  	return data; 
}

function empty(data) { 
  	return ""; 
}

function fillTable(organizations)
{  		  				  
      DWRUtil.removeAllRows("organizationsTable");
      if (organizations.length > 0)
	  {		
	      DWRUtil.addRows("organizationsTable", organizations, [ getRadio, getName, getAddress ]);
	  }
	  else
	  {		
	      DWRUtil.addRows("organizationsTable", {"No School or System found":""}, [ empty, direct ]);
	  }
}
	  
function zipChanged() {
  	document.getElementById("partnerKey").value = ""
  	var zip = document.getElementById("zipCode").value;
  	if (zip == "" || zip.length < 3) {
      	DWRUtil.removeAllRows("organizationsTable");
 	  	toggleBox ("organizationsLayer", 0);
    } else {
 	  	OrganizationAjax.getOrganizationsByZipCodeLike (fillTable, zip);
 	  	toggleBox ("organizationsLayer", 1);
 	}
}
	  
function getByPartnerKey() 
{
  	document.getElementById("zipCode").value = "";
  	var key = document.getElementById("partnerKey").value;
  	OrganizationAjax.getOrganizationsByPartnerKey (fillAddress, key);
}
	  
function toggleBox(szDivID, iState) // 1 visible, 0 hidden
{
     var obj = document.layers ? document.layers[szDivID] :
     document.getElementById ?  document.getElementById(szDivID).style :
     document.all[szDivID].style;
     obj.visibility = document.layers ? (iState ? "show" : "hide") :
     (iState ? "visible" : "hidden");
     
}


function fillAddress(organizations)
{
      DWRUtil.removeAllRows("organizationsTable");
      if (organizations.length > 0)
	  {		
		 var organization = organizations[0];
	     organizationSelected(organization);   
	  }
	  else
	  {		
	      DWRUtil.addRows("organizationsTable", {"No School or System found":""}, [ empty, direct ]);
   	  	  toggleBox ("organizationsLayer", 1);
	  }
}
	  
function organizationSelected(organization) 
  {		
  	toggleBox ("organizationsLayer", 0);
	var organizationInode = organization["inode"]; 
	var organizationTitle = organization["title"];
	var street1 = organization["street1"];
  	var street2 = organization["street2"];
	var state = organization["state"];
    var city = organization["city"];
	var zip = organization["zip"];
	var phone = organization["phone"];
	var fax = organization["fax"];

	document.getElementById("organizationInodeAux").value = organizationInode;
	document.getElementById("organizationTitle").value = organizationTitle;
	
} 

function iDontBelongChecked() {
  	if (document.getElementById("noOrganization").checked) {
		document.getElementById("organizationInodeAux").value = '';
		document.getElementById("organizationTitle").value = "No School or System";
		document.getElementById("zipCode").value = "";
		document.getElementById("partnerKey").value = "";
		document.getElementById("zipCode").disabled = true;
		document.getElementById("partnerKey").disabled = true;
		document.getElementById("searchPartnerKey").disabled = true;
	} else {
		document.getElementById("zipCode").disabled = false;
		document.getElementById("partnerKey").disabled = false;
		document.getElementById("searchPartnerKey").disabled = false;
		document.getElementById("organizationTitle").value = document.getElementById("organizationTitle").value;
		document.getElementById("zipCode").focus();
	}
}

function doSave() {
				
    	var form = document.getElementById ("fm1");
    	form.action = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/edit_usermanager" /></portlet:actionURL>'
    	form.cmd.value = "save";
    	form.submit ();
    }
    
function doCancel() {
				
    	var form = document.getElementById ("fm1");
    	form.action = '<%=referrer%>'
    	form.cmd.value = "";
    	form.submit ();
    }
    
    
</script>
<script type="text/javascript">
<!--
	function validChallengeQuestion() {
<% if (challengeQuestionProperty) { %>	
		if (document.getElementById('challengeQuestionId').selectedIndex == 0) {
			alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.usermanager.select.challenge.question")) %>');
			return false;
		}
		
		var challengeQuestionAnswer = trimString(document.getElementById('challengeQuestionAnswer').value);
		if (challengeQuestionAnswer == '') {
			alert('<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "message.usermanager.challenge.question.invalid")) %>');
			return false;
		}
		var changingChallengeQuestion = document.getElementById('changingChallengeQuestion');
		changingChallengeQuestion.value = "true";
<% } %>
		return true;
	}
//-->
</script>
<html:form action="/ext/usermanager/edit_usermanager" styleId="fm1">
<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Edit-usermanager-userinfo-box-title")) %>' />

<input type="hidden" name="cmd">
<input type="hidden" name="referer" value="<%=referrer%>">
<table border="0" cellspacing="0" cellpadding="0" bgcolor="#F9F9F9" width="690" border="0" id="formTable">
 <tr>
	<td bgcolor="#ffffff" colspan="2" align="center"><br></td>
 </tr>
 <tr>
	<td bgcolor="#ffffff" colspan="2" align="center">
		<div id="btn">
            <button dojoType="dijit.form.Button"  onClick="doSave()">
               <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %>
            </button>&nbsp;&nbsp; 
            <button dojoType="dijit.form.Button" onclick="doCancel()">
               <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
            </button>
		</div>
	</td>
 </tr>
</table>
<table cellpadding="0" cellspacing="0" border="0">
	<!--  <tr>
		<td valign="top" align="left">
		<fieldset><legend><b>Personalize Your Experience</b></legend>
		<table border="0" cellspacing="0" cellpadding="0" bgcolor="#F9F9F9" width="690" border="0" id="formTable">
			<tr>
				<td bgcolor="#F9F9F9" colspan="2" align="center"><br></td>
			</tr>
			<tr>
				<td colspan="2">
					
				</td>
			</tr>
			<tr>
				<td bgcolor="#F9F9F9" colspan="2" align="center"><br></td>
			</tr>
			<tr>
				<td bgcolor="#F9F9F9" colspan="2" align="center"><br></td>
			</tr>
		</table>
		</fieldset>
		</td>
	</tr>-->
<tr>
	<td valign="top" align="left">
	<fieldset><legend><b><%=LanguageUtil.get(pageContext, "Edit-Personal-Information") %></b></legend>
		<table cellpadding="3" cellspacing="0" width="690" border="0" id="formTable">
			<tr>
				<td></td>
				<td width="280"><%= LanguageUtil.get(pageContext, "Prefix") %>:</td>
				<td>
					<% 	String strPrefix = myAccountForm.getPrefix();
						if(!UtilMethods.isSet(strPrefix)){
							strPrefix = "";
						}
						boolean isOther = false;
						if(!strPrefix.equals("mr") && !strPrefix.equals("mrs") && !strPrefix.equals("miss") && !strPrefix.equals("dr")){
							isOther = true;
						}
						
					%>
					<select dojoType="dijit.form.FilteringSelect" name="prefix" id="prefix" onchange="prefixChanged()" value="<%= UtilMethods.isSet(myAccountForm.getPrefix()) ? myAccountForm.getPrefix() : "" %>">
						<option value="mr" ><%= LanguageUtil.get(pageContext, "Mr") %>.</option>
						<option value="mrs" ><%= LanguageUtil.get(pageContext, "Mrs") %>.</option>
						<option value="miss" ><%= LanguageUtil.get(pageContext, "Ms") %>.</option>
						<option value="dr" ><%= LanguageUtil.get(pageContext, "Dr") %>.</option>
						<option value="other"><%= LanguageUtil.get(pageContext, "Other") %></option>
					</select>
					<input type="text" dojoType="dijit.form.TextBox" style="width:87px" name="otherPrefix" id="otherPrefix" value="<%= UtilMethods.isSet(myAccountForm.getOtherPrefix()) ? myAccountForm.getOtherPrefix() : "" %>" />
				</td>
			</tr>
			<tr>
				<td align="right"><span class="required"></span>&nbsp;</td>
				<td><%= LanguageUtil.get(pageContext, "First-Name") %>:</td>
				<td><input type="text" dojoType="dijit.form.TextBox" name="firstName" id="firstName" style="width:150px" value="<%= UtilMethods.isSet(myAccountForm.getFirstName()) ? myAccountForm.getFirstName() : "" %>" /></td>
			</tr>
			<tr>
				<td align="right"><span class="required"></span>&nbsp;</td>
				<td><%= LanguageUtil.get(pageContext, "Last-Name") %>:</td>
				<td><input type="text" dojoType="dijit.form.TextBox" name="lastName" id="lastName" style="width:150px" value="<%= UtilMethods.isSet(myAccountForm.getLastName()) ? myAccountForm.getLastName() : "" %>" /></td>
			</tr>
			<tr>
				<td></td>
				<td><%= LanguageUtil.get(pageContext, "Suffix") %>:</td>
				<td><input type="text" dojoType="dijit.form.TextBox" name="suffix" id="suffix" style="width:150px" value="<%= UtilMethods.isSet(myAccountForm.getSuffix()) ? myAccountForm.getSuffix() : "" %>" /></td>
			</tr>
			<tr>
				<td></td>
				<td><%= LanguageUtil.get(pageContext, "Title") %>:</td>
				<td><input type="text" dojoType="dijit.form.TextBox" name="title" id="title" style="width:150px" value="<%= UtilMethods.isSet(myAccountForm.getTitle()) ? myAccountForm.getTitle() : "" %>" /></td>
			</tr>
			<tr>
				<td align="right"><span class="required"></span>&nbsp;</td>
				<td><%= LanguageUtil.get(pageContext, "Username-E-mail-address") %>:</td>
				<td><input type="text" dojoType="dijit.form.TextBox" name="emailAddress" id="emailAddress" style="width:150px" value="<%= UtilMethods.isSet(myAccountForm.getEmailAddress()) ? myAccountForm.getEmailAddress() : "" %>" /></td>
			</tr>
			<tr>
				<td></td>
				<td><%= LanguageUtil.get(pageContext, "New-Password") %>:</td>
				<td><input type="password" dojoType="dijit.form.TextBox" name="password" id="password" value="" style="width:150px"/></td>
			</tr>
			<tr>
				<td></td>
				<td><%= LanguageUtil.get(pageContext, "New-Password-again") %>:</td>
				<td><input type="password" dojoType="dijit.form.TextBox" name="verifyPassword" id="verifyPassword" value="" style="width:150px"/></td>
			</tr>	
		
		<% if (challengeQuestionProperty) { %>
	<tr>
		<td></td>
		<td><%= LanguageUtil.get(pageContext, "Challenge Question") %></td>
		<td>
<%
	com.dotmarketing.beans.UserProxy userProxy = com.dotmarketing.business.APILocator.getUserProxyAPI().getUserProxy(myAccountForm.getUserID(),com.dotmarketing.business.APILocator.getUserAPI().getSystemUser(), false);
	List challengeQuestions = com.dotmarketing.util.UserUtils.getChallengeQuestionList();
%>
			<select dojoType="dijit.form.FilteringSelect" name="challengeQuestionId" id="challengeQuestionId">
				<option value="0">Select a question</option>
<%
	com.dotmarketing.beans.ChallengeQuestion challengeQuestion;
	for (int i = 0; i < challengeQuestions.size(); ++i) {
		challengeQuestion = (com.dotmarketing.beans.ChallengeQuestion) challengeQuestions.get(i);
%>
				<option value="<%= challengeQuestion.getChallengeQuestionId() %>" <%= (userProxy.getChallengeQuestionId() != null) && (userProxy.getChallengeQuestionId().equals("" + challengeQuestion.getChallengeQuestionId())) ? "selected" : "" %> ><%= challengeQuestion.getChallengeQuestionText() %></option>
<%
	}
%>
			</select>
		</td>
	</tr>
	<tr>
		<td width="10">&nbsp;</td>
		<td><%= LanguageUtil.get(pageContext, "Challenge-Question-Answer") %></td>
		<td>
			<input type="text" dojoType="dijit.form.TextBox" name="challengeQuestionAnswer" id="challengeQuestionAnswer" value="<%= userProxy.getChallengeQuestionAnswer() != null ? userProxy.getChallengeQuestionAnswer() : "" %>" size="22" />
		</td>
	</tr>
<% 
	}
	else {
%>
<input name="challengeQuestionId" type="hidden" value="">
<input name="challengeQuestionAnswer" type="hidden" value="">
<% 
	}
%>
	<input name="changingChallengeQuestion" id="changingChallengeQuestion" type="hidden" value="false">
	
	  </table>
</fieldset>
</td>
</tr>
<tr>
	<td><html:hidden property="userID" /></td>
</tr>
	<tr>
			<td valign="top" align="left">
				<fieldset><legend><b><%= LanguageUtil.get(pageContext, "Mailing-Address") %></b></legend>
					<table cellpadding="3" cellspacing="0" width="690" border="0" id="formTable">
						<tr>
							<td bgcolor="#ffffff" colspan="3" valign="top">&nbsp;</td>
						</tr>
						<tr>
							<td>&nbsp;</td>
							<td colspan="2">
								<input dojoType="dijit.form.RadioButton" type="radio" name="description" id="home" value="home" <%= "home".equals(myAccountForm.getDescription()) ? "checked" : "" %> /> <%= LanguageUtil.get(pageContext, "Home-Address") %>
								<input dojoType="dijit.form.RadioButton" type="radio" name="description" id="work" value="work" <%= "work".equals(myAccountForm.getDescription()) ? "checked" : "" %> /> <%= LanguageUtil.get(pageContext, "Work-Address") %> 
								<input dojoType="dijit.form.RadioButton" type="radio" name="description" styleId="other" value="other" <%= "other".equals(myAccountForm.getDescription()) ? "checked" : "" %> /> <%= LanguageUtil.get(pageContext, "Other") %>
							 </td>
						</tr>
						<tr>
							<td align="right"><span class="required"></span>&nbsp;</td>
							<td><%= LanguageUtil.get(pageContext, "Address") %> <%= LanguageUtil.get(pageContext, "Street") %> 1: </td>
							<td><input type="text" dojoType="dijit.form.TextBox" name="street1" id="street1" style="width:150px" value="<%= UtilMethods.isSet(myAccountForm.getStreet1()) ? myAccountForm.getStreet1() : "" %>" /></td>
						</tr>
						<tr>
							<td></td>
							<td> <%= LanguageUtil.get(pageContext, "Address") %> <%= LanguageUtil.get(pageContext, "Street") %> 2: </td>
							<td><input type="text" dojoType="dijit.form.TextBox" name="street2" id="street2" style="width:150px" value="<%= UtilMethods.isSet(myAccountForm.getStreet2()) ? myAccountForm.getStreet2() : "" %>" /></td>
						</tr>
						<tr>
							<td align="right"><span class="required"></span>&nbsp;</td>
							<td><%= LanguageUtil.get(pageContext, "City") %>: </td>
							<td><input type="text" dojoType="dijit.form.TextBox" name="city" id="city" style="width:150px" value="<%= UtilMethods.isSet(myAccountForm.getCity()) ? myAccountForm.getCity() : "" %>" /></td>
						</tr>
						<tr>
							<td align="right"><span class="required"></span>&nbsp;</td>
							<td><%= LanguageUtil.get(pageContext, "State") %>: </td>
							<td><input type="text" dojoType="dijit.form.TextBox" name="state" id="state" style="width:150px" maxlength="2" value="<%= UtilMethods.isSet(myAccountForm.getState()) ? myAccountForm.getState() : "" %>" /></td>
						</tr>
						<tr>
							<td align="right"><span class="required"></span>&nbsp;</td>
							<td><%= LanguageUtil.get(pageContext, "Zip") %>: </td>
							<td><input type="text" dojoType="dijit.form.TextBox" name="zip" id="zip" style="width:150px" value="<%= UtilMethods.isSet(myAccountForm.getZip()) ? myAccountForm.getZip() : "" %>" /></td>
						</tr>
						<tr>
							<td align="right"><span class="required"></span>&nbsp;</td>
							<td><%= LanguageUtil.get(pageContext, "Phone") %>: </td>
							<td><input type="text" dojoType="dijit.form.TextBox" name="phone" id="phone" style="width:150px" value="<%= UtilMethods.isSet(myAccountForm.getPhone()) ? myAccountForm.getPhone() : "" %>" /></td>
						</tr>
						<tr>
							<td></td>
							<td> <%= LanguageUtil.get(pageContext, "Fax") %>: </td>
							<td><input type="text" dojoType="dijit.form.TextBox" name="fax" id="fax" style="width:150px" value="<%= UtilMethods.isSet(myAccountForm.getFax()) ? myAccountForm.getFax() : "" %>" /></td>
						</tr>

					</table>
				</fieldset>
			</td>
		</tr>
</table>

<table border="0" cellspacing="0" cellpadding="0" bgcolor="#F9F9F9" width="690" border="0" id="formTable">
 <tr>
	<td bgcolor="#ffffff" colspan="2" align="center"><br></td>
 </tr>
 <tr>
	<td bgcolor="#ffffff" colspan="2" align="center">
		<div id="btn">
            <button dojoType="dijit.form.Button" onClick="doSave()">
               <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %>
            </button>
			&nbsp;&nbsp; 
            <button dojoType="dijit.form.Button" onclick="doCancel()">
               <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
            </button>
		</div>
	</td>
 </tr>
</table>
</liferay:box>
</html:form>
<script type="text/javascript">
	dojo.addOnLoad(function () {
//		iDontBelongChecked();
		prefixChanged();
	});
</script>


