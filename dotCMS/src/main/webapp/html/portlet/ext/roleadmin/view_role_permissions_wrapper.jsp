<%--
    Wrapper JSP that renders ONLY the Roles Admin permissions widget so it
    can be embedded inside the Angular Roles and Tools portlet
    (`dot-role-permissions-iframe`).

    Scope note (interim, tracked in the Roles migration epic #36909):
    the Angular Beta portlet embeds this page via `<iframe>` while the
    Angular re-implementation of the permissions matrix is scoped. The
    file intentionally reuses the existing Dojo JS (`view_role_permissions_js_inc.jsp`)
    and markup (`rolePermissionsWrapper` div) so the eventual swap-to-Angular
    is a drop-in — no new UI logic ships here.

    Query params:
      - roleId (required in practice): the role whose permissions to render.
        Omitting it leaves the widget in its idle state; the wrapper does
        NOT redirect or error so the JSP is safe to hit standalone (useful
        when debugging JSP-level errors independent of Angular).
--%>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ include file="/html/portlet/ext/roleadmin/init.jsp" %>
<%@ include file="/html/common/top_inc.jsp" %>
<%@ include file="/html/common/messages_inc.jsp" %>

<%
    final String roleId = request.getParameter("roleId");
%>

<%-- Dojo widgets + DWR RoleAjax bootstrap used by the permissions JS. --%>
<%@ include file="/html/portlet/ext/roleadmin/view_roles_js_inc.jsp" %>
<%@ include file="/html/portlet/ext/roleadmin/view_role_permissions_js_inc.jsp" %>

<style type="text/css">
    <%@ include file="/html/portlet/ext/roleadmin/view_role_permissions.css" %>
    <%@ include file="/html/portlet/ext/roleadmin/view_roles.css" %>

    /* Iframe presentation tweaks: the parent Angular tab already scrolls,
       so drop the outer margins/backgrounds that make sense in the full
       Dojo portlet but leave dead space inside the iframe. */
    body { background: #fff; margin: 0; padding: 8px; }
    #rolePermissionsWrapper { padding: 0; }
</style>

<%-- Renders the `rolePermissionsWrapper` div + `permissionsAccordionContainer`
     placeholder that the permissions JS binds to. --%>
<%@ include file="/html/portlet/ext/roleadmin/view_role_permissions_inc.jsp" %>

<script type="text/javascript">
    // Kick off the permissions load once Dojo + DWR are ready. When the
    // parent Angular tab swaps the selected role, it changes the iframe
    // `src` (roleId query param) which reloads the whole page — that is
    // fine at this level; no incremental state is preserved intentionally
    // so the JSP stays a thin, replaceable wrapper.
    dojo.addOnLoad(function () {
        <% if (UtilMethods.isSet(roleId)) { %>
        loadPermissionsForRole('<%= UtilMethods.escapeSingleQuotes(roleId) %>');
        <% } %>
    });
</script>

</body>
</html>
