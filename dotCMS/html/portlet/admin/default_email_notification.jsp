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

EmailConfig registrationEmail = userConfig.getRegistrationEmail();
%>

<form action="<portlet:actionURL><portlet:param name="struts_action" value="/admin/update_default_email_notification" /></portlet:actionURL>" method="post" name="<portlet:namespace />fm" onSubmit="submitForm(this); return false;">
<input name="<portlet:namespace /><%= Constants.CMD %>" type="hidden" value="den">

<c:if test="<%= SessionMessages.contains(renderRequest, UpdateUserConfigAction.class.getName()) %>">
	<table border="0" cellpadding="0" cellspacing="0" width="100%">
	<tr>
		<td>
			<font class="bg" size="1"><span class="bg-pos-alert"><%= LanguageUtil.get(pageContext, "you-have-successfully-updated-the-default-email-notification") %></span></font>
		</td>
	</tr>
	</table>

	<br>
</c:if>

<liferay:box top="/html/common/box_top.jsp" bottom="/html/common/box_bottom.jsp">
	<liferay:param name="box_title" value="<%= LanguageUtil.get(pageContext, \"default-email-notification\") %>" />

	<table border="0" cellpadding="0" cellspacing="2" width="95%">
	<tr>
		<td>
			<table border="0" cellpadding="0" cellspacing="0">
			<tr>
				<td>
					<font class="bg" size="2"><%= LanguageUtil.get(pageContext, "send-email-notification-to-new-users") %></font>
				</td>
				<td width="10">
					&nbsp;
				</td>
				<td>
					<select name="<portlet:namespace />config_re_send">
						<option <%= registrationEmail.isSend() ? "selected" : "" %> value="1"><%= LanguageUtil.get(pageContext, "true") %></option>
						<option <%= !registrationEmail.isSend() ? "selected" : "" %> value="0"><%= LanguageUtil.get(pageContext, "false") %></option>
					</select>
				</td>
			</tr>
			</table>
		</td>
	</tr>
	<tr>
		<td>
			<br>

			<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "email-subject") %></b></font>
		</td>
	</tr>
	<tr>
		<td>
			<input class="form-text" name="<portlet:namespace />config_re_subject" size="70" type="text" value="<%= GetterUtil.getString(registrationEmail.getSubject()) %>">
		</td>
	</tr>
	<tr>
		<td>
			<br>

			<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "email-body") %></b></font>
		</td>
	</tr>
	<tr>
		<td>
			<textarea class="form-text" cols="70" name="<portlet:namespace />config_re_body" rows="10" wrap="soft"><%= GetterUtil.getString(registrationEmail.getBody()) %></textarea>
		</td>
	</tr>
	<tr>
		<td>
			<br>

			<font class="bg" size="2"><b><%= LanguageUtil.get(pageContext, "definition-of-terms") %></b></font>
		</td>
	</tr>
	<tr>
		<td>
			<table border="0" cellpadding="4" cellspacing="0">
			<tr>
				<td>
					<font class="bg" size="1"><b>
					[$ADMIN_EMAIL_ADDRESS$]
					</b></font>
				</td>
				<td>
					<font class="bg" size="1">
					<%= company.getEmailAddress() %>
					</font>
				</td>
			</tr>
			<tr>
				<td>
					<font class="bg" size="1"><b>
					[$ADMIN_NAME$]
					</b></font>
				</td>
				<td>
					<font class="bg" size="1">
					<%= company.getShortName() %> Administrator
					</font>
				</td>
			</tr>
			<tr>
				<td>
					<font class="bg" size="1"><b>
					[$COMPANY_MX$]
					</b></font>
				</td>
				<td>
					<font class="bg" size="1">
					<%= company.getMx() %>
					</font>
				</td>
			</tr>
			<tr>
				<td>
					<font class="bg" size="1"><b>
					[$COMPANY_NAME$]
					</b></font>
				</td>
				<td>
					<font class="bg" size="1">
					<%= company.getName() %>
					</font>
				</td>
			</tr>
			<tr>
				<td>
					<font class="bg" size="1"><b>
					[$PORTAL_URL$]
					</b></font>
				</td>
				<td>
					<font class="bg" size="1">
					http://<%= company.getPortalURL() %>
					</font>
				</td>
			</tr>
			<tr>
				<td>
					<font class="bg" size="1"><b>
					[$USER_EMAIL_ADDRESS$]
					</b></font>
				</td>
				<td>
					<font class="bg" size="1">
					The email address of the new user
					</font>
				</td>
			</tr>
			<tr>
				<td>
					<font class="bg" size="1"><b>
					[$USER_ID$]
					</b></font>
				</td>
				<td>
					<font class="bg" size="1">
					The id of the new user
					</font>
				</td>
			</tr>
			<tr>
				<td>
					<font class="bg" size="1"><b>
					[$USER_NAME$]
					</b></font>
				</td>
				<td>
					<font class="bg" size="1">
					The name of the new user
					</font>
				</td>
			</tr>
			<tr>
				<td>
					<font class="bg" size="1"><b>
					[$USER_PASSWORD$]
					</b></font>
				</td>
				<td>
					<font class="bg" size="1">
				    	The password of the new user
					</font>
				</td>
			</tr>
			</table>
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
</liferay:box>

</form>

<script language="JavaScript">
	document.<portlet:namespace />fm.<portlet:namespace />config_re_subject.focus();
</script>