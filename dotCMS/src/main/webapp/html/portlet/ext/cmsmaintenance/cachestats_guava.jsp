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

<%

    long maxMemory = Runtime.getRuntime().maxMemory();
    long totalMemoryInUse = Runtime.getRuntime().totalMemory();
    long freeMemory = Runtime.getRuntime().freeMemory();
    long usedMemory = totalMemoryInUse - freeMemory;
    long availableMemory = maxMemory - usedMemory;

%>


<link rel="stylesheet" href="/html/js/sortable-0.8.0/css/sortable-theme-minimal.css" />
<script src="/html/js/sortable-0.8.0/js/sortable.min.js"></script>
<div style="padding-bottom:30px;">
    <table class="listingTable shadowBox" style="width:400px">
        <tr>
            <th><%= LanguageUtil.get( pageContext, "Total-Memory-Available" ) %> / Xmx
            </th>
            <td align="right"><%=UtilMethods.prettyByteify( maxMemory )%>
            </td>
        </tr>
        <tr>
            <th><%= LanguageUtil.get( pageContext, "Memory-Allocated" ) %>
            </th>
            <td align="right"><%= UtilMethods.prettyByteify( totalMemoryInUse )%>
            </td>
        </tr>
        <tr>
            <th><%= LanguageUtil.get( pageContext, "Filled-Memory" ) %>
            </th>
            <td align="right"><%= UtilMethods.prettyByteify( usedMemory )%>
            </td>
        </tr>
        <tr>
            <th><%= LanguageUtil.get( pageContext, "Free-Memory" ) %>
            </th>
            <td align="right"><%= UtilMethods.prettyByteify( availableMemory )%>
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


<h2 style="text-align:left"><%= providerStat.getProviderName() %></h2>


<table class="listingTable" data-sortable id="sortme<%=providerStat.hashCode()%>" style="margin-bottom:50px;">
    <thead style="cursor: pointer;">
    	<% for (String col : statColumns) { %>
    		<th style="text-align:left"><%= LanguageUtil.get(pageContext, col) %></th>
		<% } %>
    </thead>
    <% for (CacheStats stat : stats) { %>
    <tr>
        <% for (String col : statColumns) {  %>
        <td style="text-align:left"><%= stat.getStatValue(col) %></td>
        <% } %>
    </tr>
    <% } %>
</table>
<script>
dojo.ready(function() {
	var newTableObject = document.getElementById("sortme<%=providerStat.hashCode()%>");
	Sortable.initTable(newTableObject)
});

</script>
<% } %>







</div>
