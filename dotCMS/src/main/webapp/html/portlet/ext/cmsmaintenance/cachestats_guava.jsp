<%@page import="com.dotmarketing.util.Logger" %>
<%@page import="com.dotmarketing.exception.DotSecurityException" %>
<%@page import="com.dotmarketing.business.APILocator" %>
<%@page import="com.dotmarketing.business.CacheLocator" %>
<%@ include file="/html/common/init.jsp" %>
<%@ page import="com.dotmarketing.business.cache.provider.CacheProviderStats" %>
<%@ page import="java.util.*" %>
<%@ page import="com.dotmarketing.business.cache.provider.CacheStats" %>
<%
    try {
        user = com.liferay.portal.util.PortalUtil.getUser(request);
        if (user == null || !APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("maintenance", user)) {
            throw new DotSecurityException("Invalid user accessing cachestats_guava.jsp - is user '" + user + "' logged in?");
        }
    } catch (Exception e) {
        Logger.error(this.getClass(), e.getMessage());
%>

<div class="callOutBox2" style="text-align:center;margin:40px;padding:20px;">
    <%= LanguageUtil.get(pageContext, "you-have-been-logged-off-because-you-signed-on-with-this-account-using-a-different-session") %>
    <br>&nbsp;<br>
    <a href="/admin"><%= LanguageUtil.get(pageContext, "Click-here-to-login-to-your-account") %>
    </a>
</div>
<%}%>

<div style="padding-bottom:30px;">
    <table class="listingTable shadowBox" style="width:400px">
        <tr>
            <th><%= LanguageUtil.get(pageContext, "Total-Memory-Available") %>
            </th>
            <td align="right"><%=UtilMethods.prettyByteify(Runtime.getRuntime().maxMemory())%>
            </td>
        </tr>
        <tr>
            <th><%= LanguageUtil.get(pageContext, "Memory-Allocated") %>
            </th>
            <td align="right"><%= UtilMethods.prettyByteify(Runtime.getRuntime().totalMemory())%>
            </td>
        </tr>
        <tr>
            <th><%= LanguageUtil.get(pageContext, "Filled-Memory") %>
            </th>
            <td align="right"><%= UtilMethods.prettyByteify(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())%>
            </td>
        </tr>
        <tr>
            <th><%= LanguageUtil.get(pageContext, "Free-Memory") %>
            </th>
            <td align="right"><%= UtilMethods.prettyByteify(Runtime.getRuntime().freeMemory())%>
            </td>
        </tr>
    </table>
    <div class="clear"></div>
</div>

<%
    List<CacheProviderStats> providerStats = CacheLocator.getCacheAdministrator().getCacheStatsList();
    for (CacheProviderStats providerStat : providerStats) {
        java.util.LinkedHashSet<String> statColumns = providerStat.getStatColumns();
        List<CacheStats> stats = providerStat.getStats();

%>
<div><%= providerStat.getProviderName() %>
</div>
<table class="listingTable ">
    <thead>
    <%
        for (String col : statColumns) {
    %>


    <th><%= LanguageUtil.get(pageContext, col) %>
    </th>

    <%
        }
    %>
    </thead>
    <%
        for (CacheStats stat : stats) {
    %>
    <tr>
        <%
            for (String col : statColumns) {
        %>
        <td><%= stat.getStatValue(col) %></td>
        <%
            }
        %>
    </tr>
    <%
        }
    %>
</table>
<%
    }
%>
