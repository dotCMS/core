<%--
    Wrapper JSP that renders ONLY the Roles Admin Tools (Tool Groups /
    Layouts) widget so it can be embedded inside the Angular Roles and
    Tools portlet (`dot-role-tools-iframe`).

    Interim per epic #36909 — same pattern as
    `view_role_permissions_wrapper.jsp`. Reuses the Dojo JS + dialog
    markup from the existing portlet so the eventual Angular replacement
    of the Tools tab (design still being scoped) can drop this file
    without touching the Angular shell.

    Query params:
      - roleId (required in practice): the role whose layouts to render.
        Omitting it leaves the widget in its idle state; the wrapper does
        NOT redirect or error so the JSP is safe to hit standalone.
--%>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%--
    Do NOT include `/html/portlet/ext/roleadmin/init.jsp` — it wraps
    `<portlet:defineObjects />`, which requires a portlet container context
    that is absent when this JSP is hit as a plain HTTP request from an
    iframe. Include `/html/common/init.jsp` directly instead (same pattern
    as `/html/portlet/ext/categories/permissions.jsp` and
    `/html/portlet/ext/folders/permissions.jsp`).
--%>
<%@ include file="/html/common/init.jsp" %>
<%@ include file="/html/common/top_inc.jsp" %>
<%@ include file="/html/common/messages_inc.jsp" %>

<%
    final String roleId = request.getParameter("roleId");
%>

<%-- Dojo widgets + DWR RoleAjax bootstrap used by the Tools JS. --%>
<%@ include file="/html/portlet/ext/roleadmin/view_roles_js_inc.jsp" %>

<style type="text/css">
    <%@ include file="/html/portlet/ext/roleadmin/view_roles.css" %>

    /* Iframe presentation tweaks: the parent Angular tab already scrolls,
       so drop the outer margins/backgrounds that make sense in the full
       Dojo portlet but leave dead space inside the iframe. */
    body { background: #fff; margin: 0; padding: 8px; }
    #roleToolsWrapper { padding: 0; }
</style>

<%-- Renders the New Layout dialog, Custom Portlet dialog, and the Tool
     Groups grid — all bound by name to the Dojo JS. --%>
<%@ include file="/html/portlet/ext/roleadmin/view_role_tools_inc.jsp" %>

<script type="text/javascript">
    // Kick off the tools load once Dojo + DWR are ready. When the parent
    // Angular tab swaps the selected role, it changes the iframe `src`
    // (roleId query param) which reloads the whole page — that is fine at
    // this level; no incremental state is preserved intentionally so the
    // JSP stays a thin, replaceable wrapper.
    dojo.addOnLoad(function () {
        <% if (UtilMethods.isSet(roleId)) { %>
        loadRoleLayouts('<%= UtilMethods.escapeSingleQuotes(roleId) %>');
        <% } %>
    });
</script>
