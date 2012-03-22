<%@ page import="com.dotmarketing.portlets.categories.business.CategoryAPI" %>
<%@ page import="com.dotmarketing.portlets.categories.model.Category" %>
<%@ page import="com.dotmarketing.util.*"%>
<%@ page import="com.dotmarketing.business.APILocator"%>
<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Iterator"%>
<%

	CategoryAPI catAPI = APILocator.getCategoryAPI();

	//by setting the bean on context we can use the form tag instead of the struts form tag, and use the autocomplete=off on the form
	if(form == null){
		form = new UserManagerListSearchForm();
	}
	pageContext.setAttribute("org.apache.struts.taglib.html.BEAN",form);

	String active = null;
	if (form != null) {
		active = form.getActive();
	}
	
	String tagName = "";
	if (form != null) {
		if(UtilMethods.isSet(form.getTagName())){
			tagName = form.getTagName();
		}
	}

	int numberGenericVariables = Config.getIntProperty("MAX_NUMBER_VARIABLES_TO_SHOW");

	java.util.Properties properties = (java.util.Properties)request.getAttribute(com.dotmarketing.util.WebKeys.USERMANAGER_PROPERTIES);
%>

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
	function doSearchSubmit() 
	{
		var form = document.getElementById('<portlet:namespace />fm');
		<% if (viewUserManager) { %>
		form.action = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/view_usermanagerlist" /><portlet:param name="cmd" value="<%=com.liferay.portal.util.Constants.SEARCH%>" /><portlet:param name="page" value="1" /></portlet:renderURL>';
		<% } else { %>
		form.action = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>">' +
						'<portlet:param name="struts_action" value="/ext/userfilter/edit_userfilter" />' +
						'<portlet:param name="cmd" value="<%=com.liferay.portal.util.Constants.SEARCH%>" />' +
						'<portlet:param name="page" value="1" />' +
						'<portlet:param name="userFilterListInode" value="<%= form.getUserFilterListInode() %>" />' +
						'<portlet:param name="userFilterTitle" value="<%= form.getUserFilterTitle() %>" />' +
					  '</portlet:actionURL>';
		<% }%>
		form.submit ();
	}
	
	function doCleanForm(){
		if(dijit.byId("firstName")!=null){
		    dijit.byId("firstName").setValue("");
		}
		if(dijit.byId("middleName")!=null){
		    dijit.byId("middleName").setValue("");
		}
		if(dijit.byId("lastName")!=null){
		    dijit.byId("lastName").setValue("");
		}
		if(dijit.byId("emailAddress")!=null){
		    dijit.byId("emailAddress").setValue("");
		}
		if(dijit.byId("country")!=null){
		    dijit.byId("country").setValue("");
		}
		if(dijit.byId("state")!=null){
		    dijit.byId("state").setValue("");
		}
		if(dijit.byId("city")!=null){
		   dijit.byId("city").setValue("");
		}
		if(dijit.byId("zipStr")!=null){
		   dijit.byId("zipStr").setValue("");
		}
		if(dijit.byId("phone")!=null){
		   dijit.byId("phone").setValue("");
		}
		if(dijit.byId("fax")!=null){
		   dijit.byId("fax").setValue("");
		}
		if(dijit.byId("cellPhone")!=null){
		   dijit.byId("cellPhone").setValue("");
		}
		if(dijit.byId("userReferer")!=null){
		   dijit.byId("userReferer").setValue("");
		}
		
<%		
	for (int j=1; j<=numberGenericVariables; j++) { 
%>
		var var<%=j%> = dijit.byId("var<%=j%>");
		if (var<%=j%>!=null && var<%=j%>.getValue() != null) {
			var<%=j%>.setValue("");
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

		if(dijit.byId("cellPhone")!=null){
		    dijit.byId('cellPhone').setValue("");
		}
		if(dijit.byId("cellPhone")!=null){		
	    	dijit.byId("cellPhone").setValue("");
		}
		if(dijit.byId("dateOfBirthFromDojo")!=null){
	    	dijit.byId("dateOfBirthFromDojo").setValue("");
		}
		if(dijit.byId("dateOfBirthToDojo")!=null){
		   dijit.byId("dateOfBirthToDojo").setValue("");
		}

		if(dijit.byId("lastLoginSince")!=null){
		   dijit.byId("lastLoginSince").setValue("");
		}
		if(dijit.byId("lastLoginDateFromDojo")!=null){
		   dijit.byId("lastLoginDateFromDojo").setValue("");
		}
		if(dijit.byId("lastLoginDateToDojo")!=null){
		  dijit.byId("lastLoginDateToDojo").setValue("");
		}

		if(dijit.byId("createdSince")!=null){
		  dijit.byId("createdSince").setValue("");
		}
		if(dijit.byId("createdDateFromDojo")!=null){
		  dijit.byId("createdDateFromDojo").setValue("");
		}
		if(dijit.byId("createdDateToDojo")!=null){
		  dijit.byId("createdDateToDojo").setValue("");
		}

		if(dijit.byId("lastVisitSince")!=null){
		  dijit.byId("lastVisitSince").setValue("");
		}
		if(dijit.byId("lastVisitDateFromDojo")!=null){
		  dijit.byId("lastVisitDateFromDojo").setValue("");
		}
		if(dijit.byId("lastVisitDateToDojo")!=null){
		   dijit.byId("lastVisitDateToDojo").setValue("");
		}

		if(dijit.byId("active")!=null){
		  dijit.byId("active").setValue("");
		}
	}
	
	function addUser() {
		window.location.href = '<portlet:actionURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/edit_usermanager" /><portlet:param name="cmd" value="load_register_user" /></portlet:actionURL>';
	}
	function doLoadSubmit () {
		var form = document.getElementById('<portlet:namespace />fm');
		form.action = '<portlet:actionURL><portlet:param name="struts_action" value="/ext/usermanager/view_usermanagerlist" /><portlet:param name="cmd" value="<%=com.liferay.portal.util.Constants.ADD%>" /></portlet:actionURL>';
		form.submit ();
	}

	function toggleBox(szDivID, labelID, height)
	{
		if(document.layers)	   //NN4+
	    {
	    	var obj = document.layers[szDivID];
	        if (obj.visibility == '' || obj.visibility == "hidden") {
	        	obj.visibility = "show";
				document.getElementById(labelID).innerHTML = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "hide")) %>';
				obj.height = height + 'px';
	        }
	        else {
	        	obj.visibility = "hide";
	        	document.getElementById(labelID).innerHTML = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "show")) %>';
				obj.height = '0px';
	        }
	    }
	    else if(document.getElementById)	  //gecko(NN6) + IE 5+
	    {
	        var obj = document.getElementById(szDivID);
	        if (obj.style.display == '' || obj.style.display == "none") {
	        	obj.style.display = "inline";
				document.getElementById(labelID).innerHTML = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "hide")) %>';
				obj.style.height = height + 'px';
	        }
	        else {
	        	obj.style.display = "none";
	        	document.getElementById(labelID).innerHTML = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "show")) %>';
				obj.style.height = '0px';
	        }
	    }
	    else if(document.all)	// IE 4
	    {
	    	var obj = document.all[szDivID]
	        if (obj.style.display == '' || obj.style.display == "none") {
	        	obj.style.display = "inline";
				document.getElementById(labelID).innerHTML = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "hide")) %>';
				obj.style.height = height + 'px';
	        }
	        else {
	        	obj.style.display = "none";
	        	document.getElementById(labelID).innerHTML = '<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "show")) %>';
				obj.style.height = '0px';
	        }
	    }
	}
	//-->
</script>
<input type="hidden" name="userID" id="userID" value="">
<input type="hidden" name="returnPath" id="returnPath" value="">

<div style="text-align:right;padding:3px 5px;margin-bottom:10px;">
	<button dojoType="dijit.form.ToggleButton" id="advancedSearchButton" iconClass="previewIcon" onclick="showHideAdvancedSearchFilters(this.checked); return false;">
		<%= LanguageUtil.get(pageContext, "advanced") %>
	</button>
</div>

<div id="searchWrapper" style="overflow-y:auto;overflow-x:hidden;border-bottom:1px solid #ccc;margin-bottom:5px;">
	
	<dl>
	<%
	Company comp = com.dotmarketing.cms.factories.PublicCompanyFactory.getDefaultCompany();
	if (comp.getAuthType().equalsIgnoreCase(Company.AUTH_TYPE_ID)) {%>
	
		<dt><%= LanguageUtil.get(pageContext, "User-ID") %></dt>
		<dd><input type="text" class="form-text" name="userIdSearch" id="userIdSearch" size="37" dojoType="dijit.form.TextBox" value="<%= UtilMethods.isSet(form.getUserIdSearch()) ? form.getUserIdSearch() : "" %>" /></dd>
		
		<dt><%= LanguageUtil.get(pageContext, "Email-Address") %></dt>
		<dd><input type="text" class="form-text" name="emailAddress" id="emailAddress" size="37" dojoType="dijit.form.TextBox" value="<%= UtilMethods.isSet(form.getEmailAddress()) ? form.getEmailAddress() : "" %>" /></dd>
		
		<dt><%= LanguageUtil.get(pageContext, "First-Name") %></dt>
		<dd><input type="text" class="form-text" name="firstName" id="firstName" size="25" dojoType="dijit.form.TextBox" value="<%= UtilMethods.isSet(form.getFirstName()) ? form.getFirstName() : "" %>" /></dd>
		
		<dt><%= LanguageUtil.get(pageContext, "Last-Name") %></dt>
		<dd><input type="text" class="form-text" name="lastName" id="lastName" size="25" dojoType="dijit.form.TextBox" value="<%= UtilMethods.isSet(form.getLastName()) ? form.getLastName() : "" %>" /></dd>
	
	<%}else{%>
	
		<dt><%= LanguageUtil.get(pageContext, "Email-Address") %></dt>
		<dd><input type="text" class="form-text" name="emailAddress" id="emailAddress" size="37" dojoType="dijit.form.TextBox" value="<%= UtilMethods.isSet(form.getEmailAddress()) ? form.getEmailAddress() : "" %>" /></dd>
		
		<dt><%= LanguageUtil.get(pageContext, "First-Name") %></dt>
		<dd><input type="text" class="form-text" name="firstName" id="firstName" size="25" dojoType="dijit.form.TextBox" value="<%= UtilMethods.isSet(form.getFirstName()) ? form.getFirstName() : "" %>" /></dd>
		
		<dt><%= LanguageUtil.get(pageContext, "Last-Name") %></dt>
		<dd><input type="text" class="form-text" name="lastName" id="lastName" size="25" dojoType="dijit.form.TextBox" value="<%= UtilMethods.isSet(form.getLastName()) ? form.getLastName() : "" %>" /></dd>
	<%}%>
	
		<dt><%= LanguageUtil.get(pageContext, "Country") %></dt>
		<dd><script language="javascript">writeCountriesSelect("country", "<bean:write property='country' name='UserManagerListSearchForm'/>", true);</script></dd>
		
		<dt><%= LanguageUtil.get(pageContext, "State") %></dt>
		<dd><input type="text" class="form-text" name="state" id="state" size="30" dojoType="dijit.form.TextBox" value="<%= UtilMethods.isSet(form.getState()) ? form.getState() : "" %>" /></dd>
	</dl>

<script>
	function showHideAdvancedSearchFilters(toggle) {
		if (toggle) {
			var animation = dojo.fadeIn({ // returns a dojo._Animation
				// this is an Object containing properties used to define the animation
				node:"advancedSearchFilters",
				delay:500,
				beforeBegin: function(){
					document.getElementById("advancedSearchFilters").style.display = "block";
				}
			});
			dijit.byId('advancedSearchButton').attr('iconClass','hideIcon');
			animation.play();
		} else {
			var animation = dojo.fadeOut({ // returns a dojo._Animation
				// this is an Object containing properties used to define the animation
				node:"advancedSearchFilters",
				delay:500,
				onEnd: function(){
					document.getElementById("advancedSearchFilters").style.display = "none";
				}
			});
			dijit.byId('advancedSearchButton').attr('iconClass','previewIcon');
			animation.play();
		}
	}
	
	dojo.addOnLoad(this, function() { showHideAdvancedSearchFilters(false) });
</script>

<div id="advancedSearchFilters" style="display: none;">

	<input type="hidden" name="lastLoginTypeSearch" id="lastLoginTypeSearch" value="" />
	<script type="text/javascript">
		function formatDateToDotcmsDate(date) {
			return (date.getMonth() + 1) + '/' + date.getDate() + '/' + date.getFullYear();
		}
	</script>
	
	<script type="text/javascript">
	function loginDateRangeFromChanged() {
		var temp = dijit.byId("lastLoginDateFromDojo").getValue();
		if ((temp != null) && (temp != "")) {
			document.getElementById("lastLoginTypeSearch").value = "DateRange";
			dijit.byId("lastLoginSince").setValue("");
			document.getElementById("lastLoginDateFrom").value = formatDateToDotcmsDate(temp);
		} else {
			document.getElementById("lastLoginDateFrom").value = "";
		}
	}
	
	function loginDateRangeToChanged() {
		var temp = dijit.byId("lastLoginDateToDojo").getValue();
		if ((temp != null) && (temp != "")) {
			document.getElementById("lastLoginTypeSearch").value = "DateRange";
			dijit.byId("lastLoginSince").setValue("");
			document.getElementById("lastLoginDateTo").value = formatDateToDotcmsDate(temp);
		} else {
			document.getElementById("lastLoginDateTo").value = "";
		}
	}
	
	function loginSinceChanged() {
		var temp = dijit.byId("lastLoginSince").getValue();
		if ((temp != null) && (temp != "")) {
			document.getElementById("lastLoginTypeSearch").value = "Since";
			dijit.byId("lastLoginDateFromDojo").setValue("");
			dijit.byId("lastLoginDateToDojo").setValue("");
			document.getElementById("lastLoginDateFrom").value = "";
			document.getElementById("lastLoginDateTo").value = "";
		}
	}
	</script>
	
<hr/>
<dl>
	<dt><%= LanguageUtil.get(pageContext, "Logged-in") %></dt>
	<dd>
		<input type="text" dojoType="dijit.form.DateTextBox" name="lastLoginDateFromDojo" id="lastLoginDateFromDojo" size="10" maxlength="10" style="width:80px;font-size:11px;"
		 onchange="dijit.byId('lastLoginDateToDojo').constraints.min = arguments[0]; loginDateRangeFromChanged()"
		 value="<%= UtilMethods.isSet(form.getLastLoginDateFromDate()) ? UtilMethods.dateToShortJDBC(form.getLastLoginDateFromDate()) : "" %>" />
		<input type="hidden" name="lastLoginDateFrom" id="lastLoginDateFrom" value="<%= UtilMethods.isSet(form.getLastLoginDateFromDate()) ? UtilMethods.dateToHTMLDate(form.getLastLoginDateFromDate()) : "" %>">

		<%= LanguageUtil.get(pageContext, "To") %>

		<input type="text" dojoType="dijit.form.DateTextBox" name="lastLoginDateToDojo" id="lastLoginDateToDojo" size="10" maxlength="10" style="width:80px;font-size:11px;"
		 onchange="dijit.byId('lastLoginDateFromDojo').constraints.max = arguments[0]; loginDateRangeToChanged()"
		 value="<%= UtilMethods.isSet(form.getLastLoginDateToDate()) ? UtilMethods.dateToShortJDBC(form.getLastLoginDateToDate()) : "" %>" />
		<input type="hidden" name="lastLoginDateTo" id="lastLoginDateTo" value="<%= UtilMethods.isSet(form.getLastLoginDateToDate()) ? UtilMethods.dateToHTMLDate(form.getLastLoginDateToDate()) : "" %>">
	</dd>
	
	<dt><%= LanguageUtil.get(pageContext, "Since") %></dt>
	<dd>
		<input type="text" dojoType="dijit.form.TextBox" name="lastLoginSince" style="width:50px;font-size:11px;" id="lastLoginSince" size="5" maxlength="5" onchange="loginSinceChanged()" value="<%= UtilMethods.isSet(form.getLastLoginSince()) ? form.getLastLoginSince() : "" %>" />
		<%= LanguageUtil.get(pageContext, "days-ago") %>
	</dd>
</dl>

<hr/>

<dl>
	<input type="hidden" name="createdTypeSearch" id="createdTypeSearch" value="" />
	<script type="text/javascript">
	function createdDateRangeFromChanged() {
		var temp = dijit.byId("createdDateFromDojo").getValue();
		if ((temp != null) && (temp != "")) {
			document.getElementById("createdTypeSearch").value = "DateRange";
			dijit.byId("createdSince").setValue("");
			document.getElementById("createdDateFrom").value = formatDateToDotcmsDate(temp);
		} else {
			document.getElementById("createdDateFrom").value = "";
		}
	}
	
	function createdDateRangeToChanged() {
		var temp = dijit.byId("createdDateToDojo").getValue();
		if ((temp != null) && (temp != "")) {
			document.getElementById("createdTypeSearch").value = "DateRange";
			dijit.byId("createdSince").setValue("");
			document.getElementById("createdDateTo").value = formatDateToDotcmsDate(temp);
		} else {
			document.getElementById("createdDateTo").value = "";
		}
	}
	
	function createdSinceChanged() {
		var temp = dijit.byId("createdSince").getValue();
		if ((temp != null) && (temp != "")) {
			document.getElementById("createdTypeSearch").value = "Since";
			dijit.byId("createdDateFromDojo").setValue("");
			dijit.byId("createdDateToDojo").setValue("");
			document.getElementById("createdDateFrom").value = "";
			document.getElementById("createdDateTo").value = "";
		}
	}
	</script>
	
	<dt><%= LanguageUtil.get(pageContext, "Created") %></dt>
	<dd>
		<input type="text" dojoType="dijit.form.DateTextBox" name="createdDateFromDojo" id="createdDateFromDojo" size="10" maxlength="10" style="width:80px;font-size:11px;"
		 onchange="dijit.byId('createdDateToDojo').constraints.min = arguments[0]; createdDateRangeFromChanged()"
		 value="<%= UtilMethods.isSet(form.getCreatedDateFromDate()) ? UtilMethods.dateToShortJDBC(form.getCreatedDateFromDate()) : "" %>" />
		<input type="hidden" name="createdDateFrom" id="createdDateFrom" value="<%= UtilMethods.isSet(form.getCreatedDateFromDate()) ? UtilMethods.dateToHTMLDate(form.getCreatedDateFromDate()) : "" %>">
		
		<%= LanguageUtil.get(pageContext, "To") %>

		<input type="text" dojoType="dijit.form.DateTextBox" name="createdDateToDojo" id="createdDateToDojo" size="10" maxlength="10" style="width:80px;font-size:11px;"
		 onchange="dijit.byId('createdDateFromDojo').constraints.max = arguments[0]; createdDateRangeToChanged()"
		 value="<%= UtilMethods.isSet(form.getCreatedDateToDate()) ? UtilMethods.dateToShortJDBC(form.getCreatedDateToDate()) : "" %>" />
		<input type="hidden" name="createdDateTo" id="createdDateTo" value="<%= UtilMethods.isSet(form.getCreatedDateToDate()) ? UtilMethods.dateToHTMLDate(form.getCreatedDateToDate()) : "" %>">
	</dd>
	
	<dt><%= LanguageUtil.get(pageContext, "Since") %></dt>
	<dd><input type="text" dojoType="dijit.form.TextBox" style="width:50px;font-size:11px;" name="createdSince" id="createdSince" size="5" maxlength="5" onchange="createdSinceChanged()" value="<%= UtilMethods.isSet(form.getCreatedSince()) ? form.getCreatedSince() : "" %>" /> <%= LanguageUtil.get(pageContext, "days-ago") %></dd>
</dl>

<hr/>

<dl>
	<input type="hidden" name="lastVisitTypeSearch" id="lastVisitTypeSearch" value="" />
	<script type="text/javascript">
	function lastVisitDateRangeFromChanged() {
		var temp = dijit.byId("lastVisitDateFromDojo").getValue();
		if ((temp != null) && (temp != "")) {
			document.getElementById("lastVisitTypeSearch").value = "DateRange";
			dijit.byId("lastVisitSince").setValue("");
			document.getElementById("lastVisitDateFrom").value = formatDateToDotcmsDate(temp);
		} else {
			document.getElementById("lastVisitDateFrom").value = "";
		}
	}
	
	function lastVisitDateRangeToChanged() {
		var temp = dijit.byId("lastVisitDateToDojo").getValue();
		if ((temp != null) && (temp != "")) {
			document.getElementById("lastVisitTypeSearch").value = "DateRange";
			dijit.byId("lastVisitSince").setValue("");
			document.getElementById("lastVisitDateTo").value = formatDateToDotcmsDate(temp);
		} else {
			document.getElementById("lastVisitDateTo").value = "";
		}
	}
	
	function lastVisitSinceChanged() {
		var temp = dijit.byId("lastVisitSince").getValue();
		if ((temp != null) && (temp != "")) {
			document.getElementById("lastVisitTypeSearch").value = "Since";
			dijit.byId("lastVisitDateFromDojo").setValue("");
			dijit.byId("lastVisitDateToDojo").setValue("");
			document.getElementById("lastVisitDateFrom").value = "";
			document.getElementById("lastVisitDateTo").value = "";
		}
	}
	</script>
	<dt><%= LanguageUtil.get(pageContext, "Visited-Web-Site") %></dt>
	<dd>
		<input type="text" dojoType="dijit.form.DateTextBox" name="lastVisitDateFromDojo" id="lastVisitDateFromDojo" size="10" maxlength="10" style="width:80px;font-size:11px;"
		 onchange="dijit.byId('lastVisitDateToDojo').constraints.min = arguments[0]; lastVisitDateRangeFromChanged()"
		 value="<%= UtilMethods.isSet(form.getLastVisitDateFromDate()) ? UtilMethods.dateToShortJDBC(form.getLastVisitDateFromDate()) : "" %>" />
		<input type="hidden" name="lastVisitDateFrom" id="lastVisitDateFrom" value="<%= UtilMethods.isSet(form.getLastVisitDateFromDate()) ? UtilMethods.dateToHTMLDate(form.getLastVisitDateFromDate()) : "" %>">
		
		<%= LanguageUtil.get(pageContext, "To") %>

		<input type="text" dojoType="dijit.form.DateTextBox" name="lastVisitDateToDojo" id="lastVisitDateToDojo" size="10" maxlength="10" style="width:80px;font-size:11px;"
		 onchange="dijit.byId('lastVisitDateFromDojo').constraints.max = arguments[0]; lastVisitDateRangeToChanged()"
		 value="<%= UtilMethods.isSet(form.getLastVisitDateToDate()) ? UtilMethods.dateToShortJDBC(form.getLastVisitDateToDate()) : "" %>" />
		<input type="hidden" name="lastVisitDateTo" id="lastVisitDateTo" value="<%= UtilMethods.isSet(form.getLastVisitDateToDate()) ? UtilMethods.dateToHTMLDate(form.getLastVisitDateToDate()) : "" %>">
	</dd>
	
	<dt><%= LanguageUtil.get(pageContext, "Since") %></dt>
	<dd><input type="text" dojoType="dijit.form.TextBox" style="width:50px;font-size:11px;" name="lastVisitSince" id="lastVisitSince" size="5" maxlength="5" onchange="lastVisitSinceChanged()" value="<%= UtilMethods.isSet(form.getLastVisitSince()) ? form.getLastVisitSince() : "" %>" />&nbsp;<%= LanguageUtil.get(pageContext, "days-ago") %></dd>
</dl>

<hr/>

<dl>
	<dt><font class="gamma" size="2"><%= LanguageUtil.get(pageContext, "Referrer") %></font></dt>
	<dd><input type="text" dojoType="dijit.form.TextBox" class="form-text" name="userReferer" id="userReferer" size="30" value="<%= UtilMethods.isSet(form.getUserReferer()) ? form.getUserReferer() : "" %>" /></dd>
	
	<dt><%= LanguageUtil.get(pageContext, "active") %></dt>
	<dd>
		<select name="active" id="active" dojoType="dijit.form.FilteringSelect" autocomplete="false" value="<%= UtilMethods.isSet(active) ? active.toLowerCase() : "" %>">
			<option <%= (active==null) ? LanguageUtil.get(pageContext, "selected") : "" %> value=""></option>
			<option <%= ((active!=null) && (active.equalsIgnoreCase("true"))) ? "selected" : "" %> value="true"><%= LanguageUtil.get(pageContext, "yes") %></option>
			<option <%= ((active!=null) && (active.equalsIgnoreCase("false"))) ? "selected" : "" %> value="false"><%= LanguageUtil.get(pageContext, "no") %></option>
		</select>
	</dd>
	
	<dt><%= LanguageUtil.get(pageContext, "Tag") %> <a href="#" onmouseover="showHint('hintTag')" onmouseout="hideHint('hintTag')">?</a></dt>
	
	<script>
    dojo.require("dojo.data.ItemFileReadStore");
    var richData;
	var suggestedTag;
	
	function fillSuggestedTagsForSearch() {
		suggestedTag = document.getElementById("tagName").value;
		suggestedTag = RTrim(suggestedTag);
		suggestedTag = LTrim(suggestedTag);
		if ((suggestedTag != "")) {
			TagAjax.getSuggestedTag(suggestedTag, "", fillTagsForSearch);
		}
		else {
			TagAjax.getUsersTags(fillTagsForSearch);
		}
	}

	function fillTagsForSearch(result) {
		DWRUtil.setValues(result);

		if (0 < result.length) {
			var temp = 'richData = {';
			temp += 'identifier: "name",';
			temp += 'label: "label",';
			temp += 'items: [';
			
			var count = 0;
			for (var i = 0; i < result.length; i++) {
				var tagName = result[i]["tagName"];
				tagName = RTrim(tagName);
				tagName = LTrim(tagName);
				if(count>0){
					temp += ', {name: "' + tagName + '", label:"' + tagName + '"}';
				}else{
					temp += '{name: "' + tagName + '", label:"' + tagName + '"}';
				}
				count++;
			}

			temp += ']';
			temp += '};';
			eval(temp);

			updateSuggestTagsForSearch();
		}
		else {
			clearFilledSuggestTagsForSearch();
		}
	}

	function clearFilledSuggestTagsForSearch() {
		if ((suggestedTag != null) && (suggestedTag != "")) {
			suggestedTag = "";
			var temp = 'richData = {';
			temp += 'identifier: "name",';
			temp += 'label: "label",';
			temp += 'items: [';
			temp += ']';
			temp += '};';
			eval(temp);
		}
		updateSuggestTagsForSearch();
	}

	function updateSuggestTagsForSearch() {
		var dojoStore = new dojo.data.ItemFileReadStore({
            data: richData
        });
        
		dijit.byId("tagName").attr('store', dojoStore);
	}
	</script>
	<dd>
		<select name="tagName" id="tagName" dojoType="dijit.form.FilteringSelect" autocomplete="false" onkeyup="fillSuggestedTagsForSearch();">
			<option <%= UtilMethods.isSet(tagName)? "selected" : "" %> value="<%= tagName %>"><%= tagName %></option>
		</select>
	</dd>
	
	<dt><%= LanguageUtil.get(pageContext, "City") %></dt>
	<dd><input type="text" dojoType="dijit.form.TextBox" name="city" id="city" size="30" value="<%= form.getCity() != null ? form.getCity() : "" %>" /></dd>
	
	<dt><%= LanguageUtil.get(pageContext, "Zip") %></dt>
	<dd><input type="text" dojoType="dijit.form.TextBox" style="width:90px;font-size:11px;" name="zipStr" id="zipStr" size="30" value="<%= form.getZipStr() != null ? form.getZipStr() : "" %>" /></dd>
	
	<dt><%= LanguageUtil.get(pageContext, "Phone") %></dt>
	<dd><input type="text" dojoType="dijit.form.TextBox" name="phone" id="phone" size="20" value="<%= form.getPhone() != null ? form.getPhone() : "" %>" /></dd>
	
	<dt><%= LanguageUtil.get(pageContext, "Fax") %></dt>
	<dd><input type="text" dojoType="dijit.form.TextBox" name="fax" id="fax" size="20" value="<%= form.getFax() != null ? form.getFax() : "" %>" /></dd>
</dl>

<hr/>

<dl>

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
	<input type="hidden" name="dateOfBirthTypeSearch" id="dateOfBirthTypeSearch" value="" />
	<script type="text/javascript">
	function dateOfBirthFromChanged() {
		var temp = dijit.byId("dateOfBirthFromDojo").getValue();
		if ((temp != null) && (temp != "")) {
			document.getElementById("dateOfBirthTypeSearch").value = "DateRange";
			dijit.byId("dateOfBirthSinceDojo").setValue("");
			document.getElementById("dateOfBirthSince").value = "";
			document.getElementById("dateOfBirthFrom").value = formatDateToDotcmsDate(temp);
		} else {
			document.getElementById("dateOfBirthFrom").value = "";
		}
	}
	
	function dateOfBirthToChanged() {
		var temp = dijit.byId("dateOfBirthToDojo").getValue();
		if ((temp != null) && (temp != "")) {
			document.getElementById("dateOfBirthTypeSearch").value = "DateRange";
			dijit.byId("dateOfBirthSinceDojo").setValue("");
			document.getElementById("dateOfBirthSince").value = "";
			document.getElementById("dateOfBirthTo").value = formatDateToDotcmsDate(temp);
		} else {
			document.getElementById("dateOfBirthTo").value = "";
		}
	}
	
	function dateOfBirthSinceChanged() {
		var temp = dijit.byId("dateOfBirthSinceDojo").getValue();
		if ((temp != null) && (temp != "")) {
			document.getElementById("dateOfBirthSince").value = formatDateToDotcmsDate(temp);
			document.getElementById("dateOfBirthTypeSearch").value = "Since";
			dijit.byId("dateOfBirthFromDojo").setValue("");
			dijit.byId("dateOfBirthToDojo").setValue("");
			document.getElementById("dateOfBirthFrom").value = "";
			document.getElementById("dateOfBirthTo").value = "";
		} else {
			document.getElementById("dateOfBirthSince").value = "";
		}
	}
	</script>
	<dt><%= LanguageUtil.get(pageContext, "Date-of-Birth") %></dt>
	<dd>
		<input type="text" dojoType="dijit.form.DateTextBox" class="form-text" name="dateOfBirthFromDojo" id="dateOfBirthFromDojo" size="10" maxlength="10" style="font-size:11px;"
		 onchange="dijit.byId('dateOfBirthToDojo').constraints.min = arguments[0]; dateOfBirthFromChanged()"
		 value="<%= UtilMethods.isSet(form.getDateOfBirthFromDate()) ? UtilMethods.dateToShortJDBC(form.getDateOfBirthFromDate()) : "" %>" />
		 
		<input type="hidden" name="dateOfBirthFrom" id="dateOfBirthFrom" value="<%= UtilMethods.isSet(form.getDateOfBirthFromDate()) ? UtilMethods.dateToHTMLDate(form.getDateOfBirthFromDate()) : "" %>">

		<input type="text" dojoType="dijit.form.DateTextBox" class="form-text" name="dateOfBirthToDojo" id="dateOfBirthToDojo" size="10" maxlength="10" style="font-size:11px;"
		 onchange="dijit.byId('dateOfBirthFromDojo').constraints.max = arguments[0]; dateOfBirthToChanged()"
		 value="<%= UtilMethods.isSet(form.getDateOfBirthToDate()) ? UtilMethods.dateToShortJDBC(form.getDateOfBirthToDate()) : "" %>" />
		
		<input type="hidden" name="dateOfBirthTo" id="dateOfBirthTo" value="<%= UtilMethods.isSet(form.getDateOfBirthToDate()) ? UtilMethods.dateToHTMLDate(form.getDateOfBirthToDate()) : "" %>">
	</dd>
	
	<dt>On</dt>
	<dd>
		<input type="text" dojoType="dijit.form.DateTextBox" class="form-text" name="dateOfBirthSinceDojo" id="dateOfBirthSinceDojo" size="10" maxlength="10" style="font-size:11px;"
		 onchange="dateOfBirthSinceChanged()"
		 value="<%= UtilMethods.isSet(form.getDateOfBirthSinceDate()) ? UtilMethods.dateToShortJDBC(form.getDateOfBirthSinceDate()) : "" %>" />
		<input type="hidden" name="dateOfBirthSince" id="dateOfBirthSince" value="<%= UtilMethods.isSet(form.getDateOfBirthSinceDate()) ? UtilMethods.dateToHTMLDate(form.getDateOfBirthSinceDate()) : "" %>">
	</dd>
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
		}
%>
	<dt><%= LanguageUtil.get(pageContext, "Middle-Name") %></dt>
	<dd><input type="text" dojoType="dijit.form.TextBox" class="form-text" name="middleName" id="middleName" size="25" value="<%= form.getMiddleName() != null ? form.getMiddleName() : "" %>" /></dd>
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
		}
%>
	<dt><%= LanguageUtil.get(pageContext, "Cell") %></dt>
	<dd><input type="text" dojoType="dijit.form.TextBox" class="form-text" name="cellPhone" id="cellPhone" size="20" value="<%= form.getCellPhone() != null ? form.getCellPhone() : "" %>" /></dd>
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
			columns--;
			}
		}
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
	<dt><%= LanguageUtil.get(pageContext, "user.profile.var"+variableIndex) %></dt>
	<dd><input type="text" dojoType="dijit.form.TextBox" class="form-text" name="<%= varName %>" id="<%= varName %>" size="20"  value="<%= form.getVar(variableIndex) != null ? form.getVar(variableIndex) : "" %>" /></dd>
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
			}
		}
	}
%>
</dl>
</div>
</div>

<div style="text-align: center;">
	<button dojoType="dijit.form.Button" name="search" onClick="doSearchSubmit()">
		<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Search" )) %>   
	</button>
	<button dojoType="dijit.form.Button" name="clean" onClick="doCleanForm()">
		<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Clean-Form")) %>    
	</button>
</div>

<div style="position: absolute; top:0px;left:0px; display: none;" id="tag_div"></div>

<script language="javascript">
dojo.addOnLoad(	function () {TagAjax.getUsersTags(fillTagsForSearch);});
</script>