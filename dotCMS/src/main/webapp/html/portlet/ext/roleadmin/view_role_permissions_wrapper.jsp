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
<%@ page import="java.util.regex.Pattern" %>
<%!
    /*
     * Strict UUID guard for the reflected `roleId` param. The JSP is
     * reachable directly (not just via the Angular wrapper) and the
     * legacy `UtilMethods.escapeSingleQuotes` misleadingly UNESCAPES
     * single quotes rather than escaping them, so reflecting the raw
     * param into an inline `<script>` string would be a real XSS
     * vector. If the value is not a well-formed UUID we skip the JS
     * emission entirely — the JSP renders its idle state (same as
     * when the param is omitted).
     */
    private static final Pattern ROLE_ID_UUID = Pattern.compile(
            "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");
%>
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
    final String rawRoleId = request.getParameter("roleId");
    final String roleId = (rawRoleId != null && ROLE_ID_UUID.matcher(rawRoleId).matches())
            ? rawRoleId
            : null;
%>

<%-- Dojo widgets + DWR RoleAjax bootstrap used by the permissions JS. --%>
<%@ include file="/html/portlet/ext/roleadmin/view_roles_js_inc.jsp" %>
<%@ include file="/html/portlet/ext/roleadmin/view_role_permissions_js_inc.jsp" %>

<%-- Hidden DOM stubs to satisfy the shared JS's unconditional
     `dojo.addOnLoad` initializers (roles tree, users grid, portlet
     select) — see the include file's header for the full rationale. --%>
<%@ include file="/html/portlet/ext/roleadmin/view_role_iframe_stubs_inc.jsp" %>

<%-- The permissions wrapper does NOT ship a `<select id="portletList">`
     (no wrapper does since the Tools tab moved to Angular), but the shared
     `initializePortletInfoList` boot-time addOnLoad still tries to
     upgrade one via `dijit.form.FilteringSelect`. Provide a hidden
     stub here so the parser has an element to bind to.

     WRAPPED in a `display:none` container: `display:none` on the
     `<select>` itself doesn't help because dijit creates a NEW widget
     DOM element (`#widget_portletList`) as a sibling of the original
     — it takes its own dimensions from CSS, not from the source select
     — and would render as a visible 241px combobox at the top of the
     iframe. Putting it inside a hidden div hides the whole subtree.

     Do NOT move this into the shared stubs file — the tools wrapper
     needs its REAL select to be the first (and only) match for
     `#portletList`. --%>
<div style="display:none" aria-hidden="true">
    <select id="portletList"></select>
</div>

<%--
    Match the CSS load-set of the canonical `view_roles.jsp` portlet:
    ONLY `view_roles.css`, NOT `view_role_permissions.css`. The latter
    file carries a legacy `.dotcms .dijitAccordionTitle{height:23px}`
    rule that clips the accordion title and hides the
    `hostFolderAccordionPermissionsTitleWrapper` template. The modern
    `.permission__list .dijitAccordionTitle{height:auto}` override lives
    in `dotcms.css` (already pulled in by `top_inc.jsp`) — leaving
    `view_role_permissions.css` OUT lets that override win.
--%>
<style type="text/css">
    <%@ include file="/html/portlet/ext/roleadmin/view_roles.css" %>

    /* Iframe presentation tweaks — apply ONLY here (they never reach
       `view_roles.jsp`) so the current Dojo portlet is unaffected.
       Goal: the accordion fills the full iframe viewport instead of the
       hard-coded 400px that `view_role_permissions_js_inc.jsp` sets
       inline on `#permissionsAccordionContainer`. Chain height:100%
       from html → body → wrapper → accordion, and beat the inline
       height:400px with `!important`. */
    html, body { height: 100%; margin: 0; padding: 0; background: #fff; }
    body { padding: 8px; box-sizing: border-box; }
    #rolePermissionsWrapper {
        padding: 0;
        height: 100%;
        display: flex;
        flex-direction: column;
    }
    #permissionsAccordionContainer {
        flex: 1 1 auto;
        height: auto !important;
        min-height: 0;
    }
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
        <% if (roleId != null) { %>
        // `roleId` is validated against a UUID pattern above, so it's safe
        // to interpolate directly into this inline script.
        loadPermissionsForRole('<%= roleId %>');
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
