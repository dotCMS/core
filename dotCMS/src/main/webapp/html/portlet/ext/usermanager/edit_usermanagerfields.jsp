<%@ include file="/html/portlet/ext/usermanager/init.jsp" %>

<%	
	//java.util.TreeMap properties =  request.getAttribute(com.dotmarketing.util.WebKeys.USERMANAGER_PROPERTIES) == null ? new java.util.TreeMap() : (java.util.TreeMap) request.getAttribute(com.dotmarketing.util.WebKeys.USERMANAGER_PROPERTIES);
	java.util.Properties properties =  (java.util.Properties)request.getAttribute(com.dotmarketing.util.WebKeys.USERMANAGER_PROPERTIES);
		
	Set set = properties.keySet();
%>
<script language="Javascript">
	function cancelEdit() {
		self.location = '<portlet:renderURL><portlet:param name="struts_action" value="/ext/usermanager/view_usermanagerlist" /></portlet:renderURL>';
	}
	function saveForm() {
		var form = document.getElementById('searchFields');

	    var currentFields = listSelect(form.<portlet:namespace />currentFields);
	    var availableFields = listSelect(form.<portlet:namespace />availableFields);
	    var condition = "&currentFields=" + currentFields + "&availableFields=" + availableFields;

		form.action = '<portlet:renderURL windowState="<%= WindowState.MAXIMIZED.toString() %>"><portlet:param name="struts_action" value="/ext/usermanager/view_usermanagerlist" /><portlet:param name="cmd" value="saveFile" /></portlet:renderURL>'+ condition;
		form.submit ();

	}
</script>
<script type="text/javascript" src="/html/js/util.js"></script>
<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
<liferay:param name="box_title" value='<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "Search-Fields")) %>'/>
<form id="searchFields" name="searchFields" method="post">
<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="">

	<table border="0" cellpadding="0" cellspacing="0" width="95%">
		<tr>
			<td class="alpha">
				<table border="0" cellpadding="2" cellspacing="1" width="100%">
					<tr>
						<td align="center" class="gamma" valign="top" width="48%">
							<table border="0" cellpadding="2" cellspacing="0" width="100%">
								<tr>
									<td align="center" class="alpha">
										<font class="alpha" size="2"><b><%= LanguageUtil.get(pageContext, "Current-Fields") %></b></font>
									</td>
								</tr>
								<tr>
									<td align="center">
										<table border="0" cellpadding="0" cellspacing="2">
											<tr>
												<td>
													<select multiple id="<portlet:namespace />currentFields" name="<portlet:namespace />currentFields" size="10">
										<%
										Iterator iterCurrent = set.iterator();
										while(iterCurrent.hasNext()) {
											String key = (String)iterCurrent.next();
											String value = properties.get(key) == null ? "" : (String)properties.get(key);
											if (value.equalsIgnoreCase("true")) {
										%>
														<option value="<%= key %>"><%= key.replace("_", " ") %></option>
										<%
											}
										}
										%>
													</select>
												</td>
											</tr>
										</table>
									</td>
								</tr>
							</table>
						</td>
						<td align="center" class="gamma" valign="middle" width="4%">
							<br>
							<a class="bg" href="javascript: moveItem(document.searchFields.<portlet:namespace />currentFields, document.searchFields.<portlet:namespace />availableFields, true);"><img border="0" height="16" hspace="0" src="<%= SKIN_COMMON_IMG %>/03_right.gif" vspace="2" width="16" onClick="self.focus();"></a><br>
							<br>
							<a class="bg" href="javascript: moveItem(document.searchFields.<portlet:namespace />availableFields, document.searchFields.<portlet:namespace />currentFields, true);"><img border="0" height="16" hspace="0" src="<%= SKIN_COMMON_IMG %>/03_left.gif" vspace="2" width="16" onClick="self.focus();"></a><br>
							<br>
						</td>
						<td align="center" class="bg" valign="top" width="48%">
							<table border="0" cellpadding="2" cellspacing="0" width="100%">
								<tr>
									<td align="center" class="alpha">
										<font class="alpha" size="2"><b><%= LanguageUtil.get(pageContext, "Available-Fields") %></b></font>
									</td>
								</tr>
								<tr>
									<td align="center">
										<table border="0" cellpadding="0" cellspacing="2">
											<tr>
												<td>
													<select multiple id="<portlet:namespace />availableFields" name="<portlet:namespace />availableFields" size="10">
										<%
										Iterator iterAvailable = set.iterator();
										while(iterAvailable.hasNext()) {
											String key = (String)iterAvailable.next();
											String value = properties.get(key) == null ? "false" : (String)properties.get(key);
											if (value.equalsIgnoreCase("false")) {
										%>
														<option value="<%= key %>"><%= key.replace("_", " ") %></option>
										<%
											}
										}
										%>
	
													</select>
												</td>
											</tr>
										</table>
									</td>
								</tr>
							</table>
						</td>
					</tr>
				</table>
			</td>
		</tr>
		<tr>
			<td>
				<table border="0" width="60%" cellspacing="0" cellpadding="3" align="center">
					<tr bgcolor="#ffffff">
						<td align="center">
                            <button dojoType="dijit.form.Button"  onClick="saveForm();">
								<%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "save")) %>
                            </button>
                            &nbsp;&nbsp;&nbsp;
                            <button dojoType="dijit.form.Button"  onClick="cancelEdit();">
                               <%= UtilMethods.escapeSingleQuotes(LanguageUtil.get(pageContext, "cancel")) %>
                            </button>
						</td>
				 	</tr>
				</table>
			</td>
		</tr>
	</table>

</form>
</liferay:box>
				