<%@ include file="/html/common/init.jsp" %>
<h3><%= System.getProperty("dotcms_level_name")%></h3>
<p style="font-weight:bold;font-size:12px;"><%= ReleaseInfo.getVersion() %> (<%= ReleaseInfo.getBuildDateString(LanguageUtil.getLocale(pageContext)) %>)</p>

<%= LanguageUtil.format(pageContext, "please-email-all-questions-to", "<b>" + company.getEmailAddress() + "</b>", false) %><br><br>

<%= LanguageUtil.get(pageContext,"More-information-about-dotCMS-is-available-at") %> <a href="http://dotcms.com" target="_blank">http://dotcms.com</a>.
