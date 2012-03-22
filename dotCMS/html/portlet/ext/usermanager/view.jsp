<%@ include file="/html/portlet/ext/usermanager/init.jsp" %>
<%
	java.util.Hashtable params = new java.util.Hashtable ();
	params.put("struts_action", new String [] {"/ext/usermanager/view_usermanagerlist"} );
	params.put("cmd", new String [] {com.liferay.portal.util.Constants.SEARCH.toString()} );
	String referer = com.dotmarketing.util.PortletURLUtil.getRenderURL(request, javax.portlet.WindowState.MAXIMIZED.toString(), params);

	UserManagerListSearchForm form = (UserManagerListSearchForm)request.getAttribute(com.dotmarketing.util.WebKeys.USERMANAGERLISTFORM);

	String active = null;
	if (form != null) {
		active = form.getActive();
	}

	int numberGenericVariables = Config.getIntProperty("MAX_NUMBER_VARIABLES_TO_SHOW");

	java.util.Properties properties = (java.util.Properties)request.getAttribute(com.dotmarketing.util.WebKeys.USERMANAGER_PROPERTIES);
%>
<!-- SCRIPT FOR AJAX MANAGEMENT -->
<script type='text/javascript' src='/dwr/interface/TagAjax.js'></script>
<script type='text/javascript' src='/dwr/engine.js'></script>
<script type='text/javascript' src='/dwr/util.js'></script>
<!-- END SCRIPT FOR AJAX MANAGEMENT -->

<script type="text/javascript">
<!--
	<liferay:include page="/html/js/calendar/calendar_js_box_ext.jsp" flush="true">
		<liferay:param name="calendar_num" value="9" />
	</liferay:include>
	//lastLoginDateFrom
	function <portlet:namespace />setCalendarDate_0 (year, month, day) {
		var textbox = document.getElementById('lastLoginDateFrom');
		textbox.value = month + '/' + day + '/' + year;
	}
	//lastLoginDateTo
	function <portlet:namespace />setCalendarDate_1 (year, month, day) {
		var textbox = document.getElementById('lastLoginDateTo');
		textbox.value = month + '/' + day + '/' + year;
	}
	//createdDateFrom
	function <portlet:namespace />setCalendarDate_2 (year, month, day) {
		var textbox = document.getElementById('createdDateFrom');
		textbox.value = month + '/' + day + '/' + year;
	}
	//createdDateTo
	function <portlet:namespace />setCalendarDate_3 (year, month, day) {
		var textbox = document.getElementById('createdDateTo');
		textbox.value = month + '/' + day + '/' + year;
	}
	//lastVisitDateFrom
	function <portlet:namespace />setCalendarDate_4 (year, month, day) {
		var textbox = document.getElementById('lastVisitDateFrom');
		textbox.value = month + '/' + day + '/' + year;
	}
	//lastVisitDateTo
	function <portlet:namespace />setCalendarDate_5 (year, month, day) {
		var textbox = document.getElementById('lastVisitDateTo');
		textbox.value = month + '/' + day + '/' + year;
	}
	//dateOfBirthFrom
	function <portlet:namespace />setCalendarDate_6 (year, month, day) {
		var textbox = document.getElementById('dateOfBirthFrom');
		textbox.value = month + '/' + day + '/' + year;
	}
	//dateOfBirthTo
	function <portlet:namespace />setCalendarDate_7 (year, month, day) {
		var textbox = document.getElementById('dateOfBirthTo');
		textbox.value = month + '/' + day + '/' + year;
	}
	//dateOfBirthSince
	function <portlet:namespace />setCalendarDate_8 (year, month, day) {
		var textbox = document.getElementById('dateOfBirthSince');
		textbox.value = month + '/' + day + '/' + year;
	}
	function doSearchSubmit () {
		var form = document.getElementById('searchForm');
		form.action = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/view_usermanagerlist" /><portlet:param name="cmd" value="<%=com.liferay.portal.util.Constants.SEARCH%>" /></portlet:renderURL>';
		form.submit ();
	}
	
	
	function doCleanForm(){
		document.getElementById("firstName").value = "";
		document.getElementById("middleName").value = "";
		document.getElementById("lastName").value = "";
		document.getElementById("emailAddress").value = "";
		document.getElementById("country").value = "";
		document.getElementById("state").value = "";
		document.getElementById("city").value = "";
		document.getElementById("zipStr").value = "";
		document.getElementById("phone").value = "";
		document.getElementById("fax").value = "";
		document.getElementById("cellPhone").value = "";
		document.getElementById("userReferer").value = "";
<%		
	for (int j=1; j<=numberGenericVariables; j++) { 
%>
		var var<%=j%> = document.getElementById("var<%=j%>");
		if (var<%=j%> != null) {
			var<%=j%>.value = "";
		}
<%			
	}
%>
		for(var n = 0; n < 20; n++){
			var select = document.getElementById("categories"+n);
	        if(select != null){
	        	for (var i = 0; i < select.options.length; i ++) {
	          		var o=select.options[i];
	           		if (o.selected) {
			     		o.selected = false;
			   		}
	        	}
        	}
        }

		document.getElementById('tagName').value = "";

		document.getElementById("dateOfBirthSince").value = "";
		document.getElementById("dateOfBirthFrom").value = "";
		document.getElementById("dateOfBirthTo").value = "";

		document.getElementById("lastLoginSince").value = "";
		document.getElementById("lastLoginDateFrom").value = "";
		document.getElementById("lastLoginDateTo").value = "";

		document.getElementById("createdSince").value = "";
		document.getElementById("createdDateFrom").value = "";
		document.getElementById("createdDateTo").value = "";

		document.getElementById("lastVisitSince").value = "";
		document.getElementById("lastVisitDateFrom").value = "";
		document.getElementById("lastVisitDateTo").value = "";
	}
	
	function addUser() {
		window.location.href = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/edit_usermanager" /><portlet:param name="cmd" value="load_register_user" /></portlet:actionURL>';
	}

	//-->
</script>

<table border="0" cellpadding="2" cellspacing="9" align="center" width="95%">
<html:form styleId="searchForm" action="/ext/usermanager/view_usermanagerlist">

<tr>
<td colspan="3" width="100%">
		<%
		Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();
        if (comp.getAuthType().equalsIgnoreCase(Company.AUTH_TYPE_ID)) {%>
        <table width="100%">
			<tr>
				<td>
					<b><%= LanguageUtil.get(pageContext, "Set-As-Default") %>User ID</b><br>
					<html:text styleClass="form-text" property="userIdSearch" styleId="userIdSearch" />
				</td>
				<td>
					<b><%= LanguageUtil.get(pageContext, "Email-Address") %></b><br>
					<html:text styleClass="form-text" property="emailAddress" styleId="emailAddress" />
				</td>
				</tr>
				<tr>
				<td>
					<b><%= LanguageUtil.get(pageContext, "First-Name") %></b><br>
					<html:text styleClass="form-text" property="firstName" styleId="firstName" />
				</td>
				<td>
					<b><%= LanguageUtil.get(pageContext, "Last-Name") %></b><br>
					<html:text styleClass="form-text" property="lastName" styleId="lastName" />
				</td>
			</tr>
		</table>		
		<%}else{%>
		<table width="100%">
			<tr>
				<td>
					<b><%= LanguageUtil.get(pageContext, "Email-Address") %></b><br>
					<html:text styleClass="form-text" property="emailAddress" styleId="emailAddress" />
				</td>
				<td>
					<b><%= LanguageUtil.get(pageContext, "First-Name") %></b><br>
					<html:text styleClass="form-text" property="firstName" styleId="firstName" />
				</td>
				<td>
					<b><%= LanguageUtil.get(pageContext, "Last-Name") %></b><br>
					<html:text styleClass="form-text" property="lastName" styleId="lastName"/>
				</td>
			</tr>
		</table>		
		<%}%>
</td>
</tr>

<tr valign="middle">
	<td colspan="1">
		<b><%= LanguageUtil.get(pageContext, "Logged-in") %></b><br/>
		<span style="font-size:11px;">
		<html:radio property="lastLoginTypeSearch" styleId="lastLoginTypeSearch" value="DateRange" />
		<html:text styleClass="form-text" property="lastLoginDateFrom" styleId="lastLoginDateFrom" size="10" maxlength="10" style="font-size:11px;"/>&nbsp;
		<img align="absmiddle" border="0" hspace="0" id="<portlet:namespace />calendar_input_0_button" 
		src="<%= COMMON_IMG %>/calendar/calendar.gif" vspace="0" onClick="<portlet:namespace />calendarOnClick_0();">
		
		&nbsp;<%= LanguageUtil.get(pageContext, "To") %>&nbsp;
		<html:text styleClass="form-text" property="lastLoginDateTo" styleId="lastLoginDateTo" size="10" maxlength="10" style="font-size:11px;"/>&nbsp;
		<img align="absmiddle" border="0" hspace="0" id="<portlet:namespace />calendar_input_1_button" src="<%= COMMON_IMG %>/calendar/calendar.gif"
		 vspace="0" onClick="<portlet:namespace />calendarOnClick_1();"> <br/>
		
		<html:radio property="lastLoginTypeSearch" styleId="lastLoginTypeSearch" value="Since" />
		<%= LanguageUtil.get(pageContext, "Since") %>&nbsp;<html:text styleClass="form-text" property="lastLoginSince" styleId="lastLoginSince" size="5" maxlength="4"/>
		&nbsp;<%= LanguageUtil.get(pageContext, "days-ago") %>
		</span>
	</td>
	
	<td colspan="2">
	     <b><%= LanguageUtil.get(pageContext, "Created") %></b><br>
		<span style="font-size:11px;">
		<html:radio property="createdTypeSearch" styleId="createdTypeSearch" value="DateRange" />
		<html:text styleClass="form-text" property="createdDateFrom" styleId="createdDateFrom" size="10" maxlength="10" style="font-size:11px;"/>
		&nbsp;<img align="absmiddle" border="0" hspace="0" id="<portlet:namespace />calendar_input_2_button" 
		src="<%= COMMON_IMG %>/calendar/calendar.gif" vspace="0" onClick="<portlet:namespace />calendarOnClick_2();">
		
		&nbsp;<span style="font-size:11px;"><%= LanguageUtil.get(pageContext, "To") %></span>&nbsp;
		<html:text styleClass="form-text" property="createdDateTo" styleId="createdDateTo" size="10" maxlength="10"  style="font-size:11px;"/>&nbsp;
		<img align="absmiddle" border="0" hspace="0" id="<portlet:namespace />calendar_input_3_button" 
		src="<%= COMMON_IMG %>/calendar/calendar.gif" vspace="0" onClick="<portlet:namespace />calendarOnClick_3();"> <br/>
		
		<html:radio property="createdTypeSearch" styleId="createdTypeSearch" value="Since" />&nbsp;<%= LanguageUtil.get(pageContext, "Since") %>
		<html:text styleClass="form-text" property="createdSince" styleId="createdSince" size="5" maxlength="4"/>&nbsp;<%= LanguageUtil.get(pageContext, "days-ago") %>
		</span>
	</td>

</tr>
		
	
<tr>	
	
	<td  width="40%" >
		<b><%= LanguageUtil.get(pageContext, "Visited-Web-Site") %></b><br>
		<span  style="font-size:11px;">
		<html:radio property="lastVisitTypeSearch" styleId="lastVisitTypeSearch" value="DateRange" />
		<html:text styleClass="form-text" property="lastVisitDateFrom" styleId="lastVisitDateFrom" size="10" maxlength="10"  style="font-size:11px;"/>
		&nbsp;<img align="absmiddle" border="0" hspace="0" id="<portlet:namespace />calendar_input_4_button" 
		src="<%= COMMON_IMG %>/calendar/calendar.gif" vspace="0" onClick="<portlet:namespace />calendarOnClick_4();">
		
		&nbsp;<%= LanguageUtil.get(pageContext, "To") %>&nbsp; <html:text styleClass="form-text" property="lastVisitDateTo" styleId="lastVisitDateTo" size="10" maxlength="10"  style="font-size:11px;"/>
		&nbsp;<img align="absmiddle" border="0" hspace="0" id="<portlet:namespace />calendar_input_5_button" 
		src="<%= COMMON_IMG %>/calendar/calendar.gif" vspace="0" onClick="<portlet:namespace />calendarOnClick_5();"> <br/>
		
		<html:radio property="lastVisitTypeSearch" styleId="lastVisitTypeSearch" value="Since" />&nbsp;<%= LanguageUtil.get(pageContext, "Since") %>
		<html:text styleClass="form-text" property="lastVisitSince" styleId="lastVisitSince" size="5" maxlength="4"/>&nbsp;<%= LanguageUtil.get(pageContext, "days-ago") %>
		</span>
	</td>
	
	<td colspan="2" valign="top">
		<b><%= LanguageUtil.get(pageContext, "active") %></b><br>
		<select name="active" style="width:100px;">
		<option <%= (active==null) ? "selected" : "" %> value=""></option>
		<option <%= ((active!=null) && (active.equalsIgnoreCase("true"))) ? "selected" : "" %> value="true">
		<%= LanguageUtil.get(pageContext, "yes") %>
		</option>
		<option <%= ((active!=null) && (active.equalsIgnoreCase("false"))) ? "selected" : "" %> value="false">
		<%= LanguageUtil.get(pageContext, "no") %>
		</option>
		</select>
				
    </td>
	
</tr>


<tr>
	<td>
		<b><%= LanguageUtil.get(pageContext, "Country") %></b><br>
		<script language="javascript">writeCountriesSelect("country", "");</script>
	</td>
	<td>
		<b><%= LanguageUtil.get(pageContext, "State") %></b><br>
		<html:text styleClass="form-text" property="state" styleId="state" />
	</td>
	<td>
		<b><%= LanguageUtil.get(pageContext, "City") %></b><br>
		<html:text styleClass="form-text" property="city" styleId="city" />
	</td>
</tr>


<tr>
	<td colspan="3">
		<table border="0" cellpadding="1" cellspacing="0" width="100%">
			<tr>
				<td align="left" width="42%">
					<b><%= LanguageUtil.get(pageContext, "Zip") %></b><br>
					<html:text styleClass="form-text" property="zipStr" styleId="zipStr"/>
				</td>
			
				<td align="left" width="30%">
					<b><%= LanguageUtil.get(pageContext, "Phone") %></b><br>
					<html:text styleClass="form-text" property="phone" styleId="phone" />
				</td>
				<td  align="left">
					<b><%= LanguageUtil.get(pageContext, "Fax") %></b><br>
					<html:text styleClass="form-text" property="fax" styleId="fax" />
				</td>
				
			</tr>
		</table>
	</td>
</tr>
<tr>
<td colspan="2">
<table cellpadding="3" cellspacing="0" width="100%" border="0">
<tr>
	<td valign="top" width="42%">
		<b><%= LanguageUtil.get(pageContext, "Tag") %> <a href="#" onmouseover="showHint('hintTag')" onmouseout="hideHint('hintTag')">?</a></b><br>
		
		<html:textarea property="tagName" styleId="tagName" style="height:50px; width:150px;" onkeyup="suggestTagsForSearch(this, 'suggestedTagsDiv');" /><br>
	</td>
	<td valign="top">
		<b><%= LanguageUtil.get(pageContext, "Suggested-Tags") %></b><br>
		<div id="suggestedTagsDiv" style="height: 50px; width: 150px;  border:1px solid #CCCCCC; overflow: auto;"></div>
	</td>
</tr>
<tr>
  <td colspan="3"><span style="color: #848284; font-size:11px;">
 <%= LanguageUtil.get(pageContext, "Tip") %>: <%= LanguageUtil.get(pageContext, "Type-your-tag-You-can-enter-multiple-comma-separated-tags") %></span>
</td>
</tr>
</table>
</td>
<td align="left">
		<div>
			<font class="gamma" size="2"><b><%= LanguageUtil.get(pageContext, "Referrer") %></b></font><br>
			<html:text styleClass="form-text" property="userReferer" styleId="userReferer" />
		 </div>
	</td>
</tr>
<!-- optional fields -->
<%
	int columns = 3;

	String keyMiddleName = "Middle Name";
	keyMiddleName = keyMiddleName.replace(" ", "_");
	String displayMiddleName = properties.get(keyMiddleName) == null ? "" : (String)properties.get(keyMiddleName);

	String keyDOB = "Date of Birth";
	keyDOB = keyDOB.replace(" ", "_");
	String displayDOB = properties.get(keyDOB) == null ? "" : (String)properties.get(keyDOB);

	String keyCell = "Cell";
	keyCell = keyCell.replace(" ", "_");
	String displayCell = properties.get(keyCell) == null ? "" : (String)properties.get(keyCell);

	String keyCategories = "Categories";
	keyCategories = keyCategories.replace(" ", "_");
	String displayCategories = properties.get(keyCategories) == null ? "" : (String)properties.get(keyCategories);

	boolean firstTr = false;
	if (displayDOB.equalsIgnoreCase("true")) {
		columns--;
		firstTr = true;
%>
<tr>
	<td valign="top">
		<font class="gamma" size="2"><b><%= LanguageUtil.get(pageContext, "Date-of-Birth") %></b></font><br>
		<span style="font-size:11px;">
		<html:radio property="dateOfBirthTypeSearch" styleId="dateOfBirthTypeSearch" value="DateRange" />
		<html:text styleClass="form-text" property="dateOfBirthFrom" styleId="dateOfBirthFrom" size="10" maxlength="10" style="font-size:11px;"/>&nbsp;
		<img align="absmiddle" border="0" hspace="0" id="<portlet:namespace />calendar_input_6_button" 
		src="<%= COMMON_IMG %>/calendar/calendar.gif" vspace="0" onClick="<portlet:namespace />calendarOnClick_6();">
		&nbsp;to&nbsp;<html:text styleClass="form-text" property="dateOfBirthTo" styleId="dateOfBirthTo" size="10" maxlength="10" style="font-size:11px;"/>
		
		&nbsp;<img align="absmiddle" border="0" hspace="0" id="<portlet:namespace />calendar_input_7_button" 
		src="<%= COMMON_IMG %>/calendar/calendar.gif" vspace="0" onClick="<portlet:namespace />calendarOnClick_7();"><br>
		
		<html:radio property="dateOfBirthTypeSearch" styleId="dateOfBirthTypeSearch" value="Since" />&nbsp;On&nbsp;
		<html:text styleClass="form-text" property="dateOfBirthSince" styleId="dateOfBirthSince" size="10" maxlength="10" style="font-size:11px;"/>&nbsp;
		<img align="absmiddle" border="0" hspace="0" id="<portlet:namespace />calendar_input_8_button" 
		src="<%= COMMON_IMG %>/calendar/calendar.gif" vspace="0" onClick="<portlet:namespace />calendarOnClick_8();">
		</span>
	</td>
<%
	}
	else {
%>
	<input type="hidden" name="dateOfBirthTypeSearch" id="dateOfBirthTypeSearch" value="">
	<input type="hidden" name="dateOfBirthFrom" id="dateOfBirthFrom" value="">
	<input type="hidden" name="dateOfBirthTo" id="dateOfBirthTo" value="">
	<input type="hidden" name="dateOfBirthSince" id="dateOfBirthSince" value="">
<%
	}

	if (displayMiddleName.equalsIgnoreCase("true")) {
		columns--;
		if (!firstTr) {
			firstTr = true;
%>
<tr>
<%
		}
%>
	<td valign="top">
		<b><%= LanguageUtil.get(pageContext, "Middle-Name") %></b><br>
		<html:text styleClass="form-text" property="middleName" styleId="middleName" />
	</td>
	
	
<%
	}
	else {
%>
	<input type="hidden" name="middleName" id="middleName" value="">
<%
	}

	if (displayCell.equalsIgnoreCase("true")) {
		columns--;
		if (!firstTr) {
			firstTr = true;
%>

<%
		}
%>

<tr>

	<td valign="top">
		<b><%= LanguageUtil.get(pageContext, "Cell") %></b><br>
		<html:text styleClass="form-text" property="cellPhone" styleId="cellPhone" />
	</td>
<%
	}
	else {
%>
	<input type="hidden" name="cellPhone" id="cellPhone" value="">
<%
	}
	if (firstTr) {
		if (columns > 0) {
			while (columns > 0) {
%>
	<td>&nbsp;</td> 
<%
			
			columns--;
			}
		}
%>
</tr>
<%
	}

	int totalGenericVariableToShow = numberGenericVariables;
	for (int var=1; var<=numberGenericVariables; var++) {
		String key = LanguageUtil.get(pageContext, "user.profile.var"+var).replace(" ", "_");
		String display = properties.get(key) == null ? "" : (String)properties.get(key);
		if (display.equalsIgnoreCase("false")) {
			totalGenericVariableToShow--;
		}
	}
	
	int genericVariablePerRow = 3;
	if (totalGenericVariableToShow > 0) {
%>
</tr> 
 <tr>

<%
		int variablesShowed = 0;
		int variableIndex = 1;
		while ((variablesShowed < totalGenericVariableToShow) &&
				(variableIndex <= numberGenericVariables)) {

			String varName = "var"+variableIndex;

			String key = LanguageUtil.get(pageContext, "user.profile.var"+variableIndex).replace(" ", "_");
			String display = properties.get(key) == null ? "" : (String)properties.get(key);
			
			if (display.equalsIgnoreCase("true")) {
				variablesShowed++;
%>
	<td>
		<b><%= LanguageUtil.get(pageContext, "user.profile.var"+variableIndex) %></b><br>
		<html:text styleClass="form-text" property="<%= varName %>" styleId="<%= varName %>" />
	</td>
<%
			}
			else {
%>
	<input type="hidden" name="<%= varName %>" id="<%= varName %>" value="">
<%
			}
			variableIndex++;

			int rowBegin = variablesShowed % genericVariablePerRow;
			if ((variablesShowed > 0) && (rowBegin == 0)) {
%>
</tr>
<%
			}
		}
	}
%>
<tr>
    <td colspan="3" align="center">
            <button dojoType="dijit.form.Button" name="search" onClick="doSearchSubmit()">
                <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Search")) %>
            </button> &nbsp;&nbsp;

             <button dojoType="dijit.form.Button" name="clean" onClick="doCleanForm()">
                  <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Clean-Form")) %>  
             </button>
	</td>
</tr>
</html:form>
</table>

<div style="position: absolute; top:0px;left:0px; display: none;" id="tag_div">
</div>
<div id="hintTag" style="position: absolute; display: none; background-color: #FFFFE7; border-color: #000000; border-style: solid; border-width: 1px;"><font style="font-family: Verdana, Arial,Helvetica; color: #000000; font-style: italic;"><%= LanguageUtil.get(pageContext, "Tags-are-descriptors-that-you-can-assign-to-users-Tags-are-a-little-bit-like-keywords") %></font></div>

<table align=right>
	<tr>
		<td nowrap>

			<font class="gamma" size="2">
   				<%--a class="bg" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/register_user" /><portlet:param name="referer" value="<%=referer %>" /></portlet:actionURL>">add new</a--%>
   				<a class="bg" href="<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/edit_usermanager" /><portlet:param name="cmd" value="load_register_user" /><portlet:param name="referer" value="<%=referer %>" /></portlet:actionURL>"><%= LanguageUtil.get(pageContext, "Add-New") %></a>
			</font> 
			| 
			<font class="gamma" size="2">
				<a class="bg" href="<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/view_usermanagerlist" /><portlet:param name="cmd" value="load" /></portlet:renderURL>"><%= LanguageUtil.get(pageContext, "Load-Users") %></a>&nbsp;&nbsp;
			</font>
		</td>
	</tr>
</table>
