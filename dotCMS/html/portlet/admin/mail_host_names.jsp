<%
/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
%>

<%@ include file="/html/portlet/admin/init.jsp" %>

<%
UserConfig userConfig = AdminConfigManagerUtil.getUserConfig(company.getCompanyId());
%>

<form action="<portlet:actionURL><portlet:param name="struts_action" value="/admin/update_mail_host_names" /></portlet:actionURL>" method="post" name="<portlet:namespace />fm" onSubmit="submitForm(this); return false;">
<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="mhn">

<c:if test="<%= SessionMessages.contains(renderRequest, UpdateUserConfigAction.class.getName()) %>">
	<table border="0" cellpadding="0" cellspacing="0" width="95%">
	<tr>
		<td>
			<font class="bg" size="1"><span class="bg-pos-alert"><%= LanguageUtil.get(pageContext, "you-have-successfully-updated-the-mail-host-names") %></span></font>
		</td>
	</tr>
	</table>

	<br>
</c:if>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"mail-host-names\") %>" />

	<table border="0" cellpadding="0" cellspacing="0" width="95%">
	<tr>
		<td align="center">
			<table border="0" cellpadding="0" cellspacing="2">
			<tr>
				<td>
					<font class="bg" size="2"><%= LanguageUtil.format(pageContext, "enter-one-mail-host-name-per-line-for-all-additional-mail-host-names-besides-x", company.getMx(), false) %>.</font>
				</td>
			</tr>
			<tr>
				<td>
					<textarea class="form-text" cols="70" name="<portlet:namespace />config_mhn" rows="10" wrap="soft"><%= StringUtil.merge(userConfig.getMailHostNames(), "\n") %></textarea>
				</td>
			</tr>
			<tr>
				<td align="center">
					<br>
                    <button dojoType="dijit.form.Button" type="submit" id="submitButton"><%= LanguageUtil.get(pageContext, "save") %></button>
                      
                    <button dojoType="dijit.form.Button"  
                    onClick="self.location = '<portlet:renderURL><portlet:param name="struts_action" value="/admin/list_users" /></portlet:renderURL>';">
                        <%= LanguageUtil.get(pageContext, "cancel") %>
                    </button>
				</td>
			</tr>
			</table>
		</td>
	</tr>
	</table>
</liferay:box>

</form>

<script language="JavaScript">
	document.<portlet:namespace />fm.<portlet:namespace />config_mhn.focus();
</script>