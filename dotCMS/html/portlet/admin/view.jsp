<%@ include file="/html/portlet/admin/init.jsp" %>

<%if(request.getAttribute(com.dotmarketing.util.WebKeys.CMS_CRUMBTRAIL_OPTIONS)==null){%>
	<jsp:include page="/html/portlet/admin/sub_nav.jsp"></jsp:include>
<%}%>




<%
boolean autoLogin = company.isAutoLogin();
boolean strangers = company.isStrangers();
//Boolean useChallengeQuestion = (Boolean) request.getAttribute("USE_CHALLENGE_QUESTION");

if (UtilMethods.isSet(request.getParameter("use_challenge_question"))) {
	try {
		Boolean bool = new Boolean(request.getParameter("use_challenge_question"));
		//com.dotmarketing.util.CompanyUtils.updateCompanyUseChallengeQuestion(bool);
	} catch (Exception e) {
		com.dotmarketing.util.Logger.error(this,e.toString());
	}
}


boolean challengeQuestion = false;

try {
	challengeQuestion = com.dotmarketing.util.Config.getBooleanProperty("USE_CHALLENGE_QUESTION");
} catch (Exception e) {
	com.dotmarketing.util.Logger.error(this, "edit_company.jsp - Need to set USE_CHALLENGE_QUESTION property.");
}



%>


<%@page import="com.dotmarketing.business.APILocator"%><script type="text/javascript">
	function addParams() {
		var redirect = document.<portlet:namespace />fm.<portlet:namespace />redirect.value;
		//redirect = redirect + "&use_challenge_question=" + document.<portlet:namespace />fm.use_challenge_question.options[document.<portlet:namespace />fm.use_challenge_question.selectedIndex].value;//DOTCMS-5046
		document.<portlet:namespace />fm.<portlet:namespace />redirect.value = redirect;
	}

	function initUserLocale() {
		
		var timeZoneSelect = dojo.query('#userTimezoneWrapper select')[0];
		if(timeZoneSelect)
			timeZoneSelect = new dijit.form.Select({ name:'<%= renderResponse.getNamespace() + "company_tz_id" %>' , value:'<%= company.getTimeZone().getID() %>'}, timeZoneSelect);
	}

	dojo.addOnLoad(function () {
    	initUserLocale();
    	styler('<%= company.getSize() %>');
    	imgSwap('<%= company.getHomeURL() %>');
	 });
	 

	dojo.require("dojox.widget.ColorPicker");
	dojo.require("dojo.parser");	// scan page for widgets and instantiate them

	var styler = function(val){
		dojo.byId("colorBlock").style.background = val;
		dojo.byId("bgColor").value = val;
	}
	
	var imgSwap = function(val){
		dojo.byId("imageBlock").style.backgroundImage = "url('" + val + "')";
		dojo.byId("bgURL").value = val;
	}
	

</script>

<%@page import="com.dotmarketing.util.UtilMethods"%>
<form action="<portlet:actionURL><portlet:param name="struts_action" value="/admin/update_company" /></portlet:actionURL>" method="post" name="<portlet:namespace />fm" onSubmit="addParams(); submitForm(this); return false;">
<!--form action="<portlet:actionURL><portlet:param name="struts_action" value="/admin/edit_company" /></portlet:actionURL>" method="post" name="<portlet:namespace />fm" onSubmit="submitForm(this); return false;"-->
<input name="<portlet:namespace />redirect" type="hidden" value="<portlet:renderURL><portlet:param name="struts_action" value="/admin/edit_company" /></portlet:renderURL>">




<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"company-profile\") %>" />


<input name="<portlet:namespace />company_name" type="hidden" value="<%= company.getName() %>">
<input type="hidden" value="<%= company.getType()%>" name="<portlet:namespace />company_type">
<input name="<portlet:namespace />company_short_name" size="25" type="hidden" value="<%= company.getShortName() %>">
<input name="<portlet:namespace />company_street" size="30" type="hidden" value="<%= company.getStreet() %>">
<input name="<portlet:namespace />company_city" size="20" type="hidden" value="<%= company.getCity() %>">
<input name="<portlet:namespace />company_state" size="5" type="hidden" value="<%= company.getState() %>">
<input name="<portlet:namespace />company_zip" size="10" type="hidden" value="<%= company.getZip() %>">
<input name="<portlet:namespace />company_phone" size="20" type="hidden" value="<%= company.getPhone() %>">
<input  name="<portlet:namespace />company_fax" size="20" type="hidden" value="<%= company.getFax() %>">


<style>
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
			<dd>
			<input dojoType="dijit.form.TextBox" name="<portlet:namespace />company_portal_url" size="25" type="text" value="<%= company.getPortalURL() %>">
			
			</dd>
			

			
			<dt><%= LanguageUtil.get(pageContext, "mail-domain") %></dt>
			<dd><input dojoType="dijit.form.TextBox" name="<portlet:namespace />company_mx" size="25" type="text" value="<%= company.getMx() %>"></dd>
			
			<dt><%= LanguageUtil.get(pageContext, "email-address") %></dt>
			<dd><input dojoType="dijit.form.TextBox" name="<portlet:namespace />company_email_address" size="20" type="text" value="<%= company.getEmailAddress() %>"></dd>

			
			<dt>Background Color</dt>
			<dd style="position:relative;">
				<div id="colorBlock" style="position:absolute;left:154px;border-left:1px solid #b3b3b3;top:9px;width:50px;height:26px;display:inline-block;margin:0 0 -5px; 10px;"></div>
				<input id="bgColor" dojoType="dijit.form.TextBox" name="<portlet:namespace />company_size" size="5" type="text" value="<%= company.getSize() %>">
				<button id="buttonOne" dojoType="dijit.form.Button" type="button" iconClass="colorIcon">
					Color Picker
				    <script type="dojo/method" data-dojo-event="onClick" data-dojo-args="evt">
				        dijit.byId("colorPicker").show();
				    </script>
				</button>
			</dd>
		
			<dt>Background Image</dt>
			<dd>
				<input id="bgURL" dojoType="dijit.form.TextBox" name="<portlet:namespace />company_home_url" size="25" type="text" value="<%= company.getHomeURL() %>">
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
		

    <div id="colorPicker" data-dojo-type="dijit.Dialog" title=Color Picker">
		<div id="pickerLive" dojoType="dojox.widget.ColorPicker"
			webSafe="false"
			liveUpdate="true"
			value="<%= company.getSize() %>"
			onChange="styler(arguments[0])">
		</div>
	</div>
	
	<div id="bgPicker" data-dojo-type="dijit.Dialog" title=Backgrounds">
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
	<td valign="middle" align="center">
		<img border="1" hspace="0" src="<%= IMAGE_PATH %>/company_logo?img_id=<%= company.getCompanyId() %>&key=<%= ImageKey.get(company.getCompanyId()) %>" vspace="0"><br>
		<div class="buttonRow" style="margin-top:30px;">
			<button dojoType="dijit.form.Button" onClick="self.location='<portlet:renderURL><portlet:param name="struts_action" value="/admin/change_company_logo" /></portlet:renderURL>';return false;" iconClass="browseIcon"><%= LanguageUtil.get(pageContext, "change") %></button>
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
				<select dojoType="dijit.form.Select"  value="<%=defuser.getLocale().getLanguage()+ "_" + defuser.getLocale().getCountry()%>" name="<portlet:namespace />company_language_id">                         
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
			    <select name="<%= renderResponse.getNamespace() + "company_tz_id" %>">
			       <% String[] ids = TimeZone.getAvailableIDs();
			          Arrays.sort(ids);
			          for(String id : ids) { 
			            TimeZone tmz=TimeZone.getTimeZone(id);%>
			            <option value="<%= id %>" >			                
			                (<%= tmz.getID() %>)
			                <%= tmz.getDisplayName(locale) %>
			            </option>
			       <% }%>
			    </select>
				</span>
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
				<select  dojoType="dijit.form.Select"  value="<%= company.getAuthType()%>"  name="<portlet:namespace />company_auth_type">
					<option value="<%= Company.AUTH_TYPE_EA %>"><%= LanguageUtil.get(pageContext, "email-address") %></option>
					<option value="<%= Company.AUTH_TYPE_ID %>"><%= LanguageUtil.get(pageContext, "user-id") %></option>
				</select>
			</dd>
			<%--
			<dt><%= LanguageUtil.get(pageContext, "allow-users-to-automatically-login") %></dt>
			<dd>
				<select dojoType="dijit.form.Select" <%= (autoLogin) ? "value=\"1\"" : "value=\"0\"" %>  name="<portlet:namespace />company_auto_login">
					<option  value="1"><%= LanguageUtil.get(pageContext, "yes") %></option>
					<option  value="0"><%= LanguageUtil.get(pageContext, "no") %></option>
				</select>
			</dd>
			
			<dt><%= LanguageUtil.get(pageContext, "allow-strangers-to-create-accounts") %></dt>
			<dd>
				<select dojoType="dijit.form.Select"  <%= (strangers) ? "value=\"1\"" : "value=\"0\"" %> name="<portlet:namespace />company_strangers">
					<option value="1"><%= LanguageUtil.get(pageContext, "yes") %></option>
					<option value="0"><%= LanguageUtil.get(pageContext, "no") %></option>
				</select>
			</dd>
			
			<dt><%= LanguageUtil.get(pageContext, "use-challenge-question") %></dt>
			<dd>
				<select dojoType="dijit.form.Select"  <%= (challengeQuestion) ? "value=\"true\"" : "value=\"false\"" %> name="use_challenge_question">
					<option  value="true"><%= LanguageUtil.get(pageContext, "yes") %></option>
					<option  value="false"><%= LanguageUtil.get(pageContext, "no") %></option>
				</select>
			</dd>
			 --%>
		</dl>
	</td>
</tr>
</table>

<div class="buttonRow">
	<button dojoType="dijit.form.Button" type="submit" id="submitButton" iconClass="saveIcon">
		<%= LanguageUtil.get(pageContext, "update") %>
	</button>      
	
	<button dojoType="dijit.form.Button"  iconClass="cancelIcon" onClick="self.location = '<portlet:renderURL windowState="<%= WindowState.NORMAL.toString() %>"><portlet:param name="struts_action" value="/admin/view" /></portlet:renderURL>';">
	   <%= LanguageUtil.get(pageContext, "cancel") %>
	</button>	
</div>

</liferay:box>

</form>

<script language="JavaScript">
	//document.<portlet:namespace />fm.<portlet:namespace />company_name.focus();//DOTCMS-5046
</script>