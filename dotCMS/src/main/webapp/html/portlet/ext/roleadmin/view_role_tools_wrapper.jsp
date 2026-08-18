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

<%-- Hidden DOM stubs to satisfy the shared JS's unconditional
     `dojo.addOnLoad` initializers (roles tree, users grid) — see the
     include file's header for the full rationale. --%>
<%@ include file="/html/portlet/ext/roleadmin/view_role_iframe_stubs_inc.jsp" %>

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
    //
    // `loadRoleLayouts` reads `currentRole.editLayouts` DIRECTLY (see
    // `view_roles_js_inc.jsp` @ line ~992), so we MUST populate
    // `currentRole` before calling it — in the full portlet that
    // assignment happens inside `roleClicked()` when the user picks a
    // tree node. Here we fetch the role JSON via the same endpoint the
    // legacy `findRole()` uses and seed `currentRole` / `currentRoleId`
    // manually before kicking off the layouts load.
    dojo.addOnLoad(function () {
        <% if (UtilMethods.isSet(roleId)) { %>
        var _roleId = '<%= UtilMethods.escapeSingleQuotes(roleId) %>';
        dojo.xhrGet({
            url: '/api/role/loadbyid/id/' + encodeURIComponent(_roleId),
            handleAs: 'json',
            load: function (role) {
                currentRoleId = _roleId;
                currentRole = role;
                loadRoleLayouts(_roleId);
            },
            error: function (err) {
                console.error('Failed to load role for iframe wrapper', err);
            }
        });
        <% } %>
    });
</script>

<%--
    `bottom_inc.jsp` registers `dotMakeBodVisible` on `dojo.addOnLoad` —
    without it the `<body style="visibility:hidden">` set by `top_inc.jsp`
    is never toggled and nothing renders. It also anti-frame-busts
    correctly (only redirects to `/dotAdmin/` when the page is NOT inside
    an iframe, which is exactly our embed scenario).
--%>
<%@ include file="/html/common/bottom_inc.jsp" %>
