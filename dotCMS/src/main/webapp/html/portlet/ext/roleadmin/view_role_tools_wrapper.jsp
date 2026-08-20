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

<%--
    Match `view_roles.jsp`'s CSS load-set: only `view_roles.css`. See the
    `view_role_permissions_wrapper.jsp` header for the full rationale on
    why we DO NOT include the legacy `view_role_permissions.css`.
--%>
<style type="text/css">
    <%@ include file="/html/portlet/ext/roleadmin/view_roles.css" %>

    /* Iframe presentation tweaks — scoped to this wrapper only, so the
       canonical `view_roles.jsp` portlet is not affected. */
    body { background: #fff; margin: 0; padding: 8px; }
    #roleToolsWrapper { padding: 0; }

    /* `.toolTable` styling lives INLINE in `view_roles.jsp` (lines 60-70),
       not in `view_roles.css`, so the New Layout dialog form fields
       collapse without any cell spacing in this iframe. Mirror the same
       rules here so the dialog matches the canonical portlet. */
    .toolTable { margin: 0 auto; }
    .toolTable td {
        padding: 10px 8px;
        vertical-align: middle;
        position: relative;
    }

    /*
     * dojox.grid.DataGrid's default layout uses `position: absolute` on
     * `.dojoxGridHeader` (line 49 of Grid.css) with an inline height set
     * from `measureHeader()` (`_ViewManager.js:134`). In the canonical
     * portlet a `dijit.layout.TabContainer` fires `resize()` on tab
     * activation which triggers `_Grid.js#_getHeaderHeight()` and paints
     * the header at the correct height. In this iframe there is no
     * TabContainer, so `measureHeader()` returns 0 (the offsetHeight of
     * an absolute-positioned collapsed element) and dojox writes inline
     * `masterHeader.style.height = "0px"` — with `.dojoxGridHeader`'s
     * `overflow: hidden` (line 51 of Grid.css) the header cells are
     * clipped and the first data row paints over the empty header slot.
     *
     * Rather than fight dojox's runtime height computation we neutralize
     * its absolute-positioned layout entirely for this iframe: force the
     * grid's internal nodes to `position: static / relative` with
     * `overflow: visible` so header + rows stack via normal document
     * flow. Row virtualization is not needed here (Tool Groups grids
     * have ~10-20 rows in practice, well under any perf threshold).
     */
    #roleLayoutsGridWrapper { position: relative; }

    /* Undo dojox's absolute-positioned scaffolding for the OUTER structure. */
    #roleLayoutsGridWrapper .dojoxGrid,
    #roleLayoutsGridWrapper #roleLayoutsGrid,
    #roleLayoutsGridWrapper .dojoxGridMasterHeader,
    #roleLayoutsGridWrapper .dojoxGridMasterView {
        position: static !important;
        height: auto !important;
        overflow: visible !important;
    }
    /*
     * `.dojoxGridView` and `.dojoxGridContent` MUST be `position: relative`
     * (not static) so they establish a containing block for dojox's
     * `position: absolute` row canvas (an unclassed inner div with
     * inline `top: 0; left: 0`). Left as static, that canvas escapes up
     * the ancestor chain until it finds `#roleLayoutsGridWrapper`
     * (which we made `position: relative` further up) and paints its
     * rows at Y=0 of the wrapper — right on top of the header. Keeping
     * these two ancestors relative anchors the canvas to its natural
     * document-flow position.
     */
    #roleLayoutsGridWrapper .dojoxGridView,
    #roleLayoutsGridWrapper .dojoxGridScrollbox,
    #roleLayoutsGridWrapper .dojoxGridContent {
        position: relative !important;
        height: auto !important;
        overflow: visible !important;
    }
    /*
     * The unclassified `<div role="presentation" style="position:absolute;
     * top:0; left:0; ...">` that dojox drops inside `.dojoxGridContent`
     * is the row canvas — its `position: absolute` means its rows do
     * NOT contribute to the parent's height. `.dojoxGridContent` collapses
     * to 0, which lets the `.buttonRow` that follows the grid paint
     * right on top of the rows. Force this canvas into normal flow so
     * its intrinsic row-list height bubbles up.
     */
    #roleLayoutsGridWrapper .dojoxGridContent > div {
        position: static !important;
        top: auto !important;
        left: auto !important;
        height: auto !important;
    }
    /*
     * `<input class="dojoxGridHiddenFocus" aria-hidden="true">` are two
     * keyboard-focus helpers dojox drops inside every `.dojoxGridView` —
     * meant to be off-screen and clipped by the grid's default
     * `overflow: hidden`. Our `overflow: visible !important` above
     * uncloaks them; they render as two floating checkboxes between the
     * header and the first data row. They are already hidden from AT via
     * `aria-hidden`, so hiding them visually is safe.
     */
    #roleLayoutsGridWrapper .dojoxGridHiddenFocus {
        display: none !important;
    }
    #roleLayoutsGridWrapper .dojoxGridHeader {
        position: relative !important;
        top: auto !important;
        left: auto !important;
        right: auto !important;
        width: auto !important;
        height: auto !important;
        overflow: visible !important;
    }
    #roleLayoutsGridWrapper .dojoxGridHeader .dojoxGridRowTable,
    #roleLayoutsGridWrapper .dojoxGridScrollbox {
        overflow: visible !important;
    }
    /* The header container ships with `width:9000em` inline to allow
       horizontal scroll of the header alongside the body; with static
       layout that would blow the iframe out sideways. Cap it to fit. */
    #roleLayoutsGridWrapper .dojoxGridHeader [dojoattachpoint="headerNodeContainer"] {
        width: 100% !important;
    }
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
                // Populate the `.view-roles__heading` h3 manually — the
                // shared `setRoleName` also touches `displayRoleName1/2`
                // which don't exist in this wrapper (they're in the full
                // portlet's other tabs); calling it here would NPE.
                var nameEl = dojo.byId('displayRoleName3');
                if (nameEl) {
                    nameEl.innerHTML = (role && role.name) ? role.name : '';
                }
                loadRoleLayouts(_roleId);

                // dojox.grid.DataGrid measures its header height once at
                // `startup()`; in the canonical portlet a dijit TabContainer
                // fires `.resize()` on tab activation, which triggers the
                // grid to re-layout with a proper header height. This
                // wrapper has no TabContainer, so the header stays at
                // height 0 (invisible, behind the first data row). Poll
                // for the grid dijit and call `.resize()` explicitly as
                // soon as it exists — bail out after ~3s in case a BE
                // failure means the grid never mounts.
                var resizeAttempts = 0;
                var resizeInterval = setInterval(function () {
                    resizeAttempts++;
                    var grid = dijit.byId('roleLayoutsGrid');
                    if (grid) {
                        grid.resize();
                        clearInterval(resizeInterval);
                    } else if (resizeAttempts > 30) {
                        clearInterval(resizeInterval);
                    }
                }, 100);
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
