<div class="buttonRow" style="white-space:nowrap;">
	<span style="vertical-align:middle;"><%= LanguageUtil.get(pageContext, "language") %>:</span>
	<select dojoType="dijit.form.FilteringSelect" id="userLanguage" name="userLanguageId">
	    <% Locale[] locales = LanguageUtil.getAvailableLocales();%>
	    <% for (int i = 0; i < locales.length; i++) { %>
			<option value="<%= locales[i].getLanguage() + "_" + locales[i].getCountry() %>"><%= locales[i].getDisplayName(locale) %></option>
	    <% } %>
	</select>
	
	<span style="vertical-align:middle;">&nbsp;&nbsp;</span>
	
	<span style="vertical-align:middle;"><%= LanguageUtil.get(pageContext, "time-zone") %>:</span>
	<span id="userTimezoneWrapper">
        <liferay:input-time-zone name="user_tz_id" value="" locale="<%= locale.toString()%>" />
    </span>
		
	<button dojoType="dijit.form.Button" onClick="updateUserLocale()" iconClass="plusIcon">
        <%= LanguageUtil.get(pageContext, "update") %>
    </button>
</div>
