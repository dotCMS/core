<%@ include file="/html/common/init.jsp" %>
<h3><%= System.getProperty("dotcms_level_name")%></h3>
<b><%= ReleaseInfo.getVersion() %> (<%= LanguageUtil.get(pageContext,"Build") %>: <%= ReleaseInfo.getBuildNumber() %> / <%= ReleaseInfo.getBuildDateString(LanguageUtil.getLocale(pageContext)) %>)</b><br><br>

<%= LanguageUtil.format(pageContext, "please-email-all-questions-to", "<b>" + company.getEmailAddress() + "</b>", false) %><br><br>

<%= LanguageUtil.get(pageContext,"More-information-about-dotCMS-is-available-at") %> <a href="http://dotcms.com" target="_blank">http://dotcms.com</a>.
