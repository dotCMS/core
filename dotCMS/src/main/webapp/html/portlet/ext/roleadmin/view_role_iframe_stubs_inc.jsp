<%--
    Neutralizes the portlet-scoped boot chain that `view_roles_js_inc.jsp`
    kicks off when the shared JS is loaded from an iframe wrapper that
    renders only ONE widget (permissions or tools).

    The shared file registers several `dojo.addOnLoad` callbacks
    unconditionally at include time:
      - `buildRolesTree()` — builds the roles tree AND the dijit
        `roleTreeMenu` context menu, dereferencing multiple DOM elements
        (`noRolesFound`, `rolesTreeWrapper`, ...) and the dijit widget
        `roleTreeMenu` (line 222) whose absence produces
        `Cannot read properties of undefined (reading 'bindDomNode')`.
      - The anonymous users-grid init at line 765 — instantiates a
        `dojox.grid.DataGrid` on `usersGrid`.
      - `initializePortletInfoList()` — creates a `FilteringSelect` on
        `portletList`.

    Two-part strategy:

      1. Provide hidden DOM stubs for the shared file's `dojo.byId(...)`
         targets so the anonymous callbacks (which we cannot override)
         don't NPE.

      2. Override the two NAMED initializers (`buildRolesTree`,
         `initializePortletInfoList`) with no-ops in a `<script>` that
         runs AFTER `view_roles_js_inc.jsp` executes but BEFORE dojo
         fires its onLoad queue. This is safe because dojo's onLoad
         queue only fires once all dojo modules finish loading, which
         happens strictly after synchronous script evaluation.

    Include AFTER `view_roles_js_inc.jsp` and BEFORE the wrapper's own
    inline `dojo.addOnLoad`.
--%>
<div id="rolesIframeStubsWrapper" style="display:none" aria-hidden="true">
    <%-- buildRolesTree() @ view_roles_js_inc.jsp:99-131 --%>
    <div id="noRolesFound"></div>
    <div id="loadingRolesWrapper"></div>
    <div id="rolesTreeWrapper"></div>
    <div id="rolesTree"></div>
    <input type="text" id="rolesFilter" />
    <div id="roleTabs"></div>

    <%-- editRoleMenu et al @ view_roles_js_inc.jsp:254-284 (only fires
         on tree-node context menu, but stub them anyway to be safe). --%>
    <div id="editRoleMenu"></div>
    <div id="lockRoleMenu"></div>
    <div id="unlockRoleMenu"></div>
    <div id="deleteRoleMenu"></div>

    <%-- Users-grid init @ view_roles_js_inc.jsp:765-818. --%>
    <div id="usersGrid"></div>

    <%-- NOTE: `portletList` is NOT stubbed here. Injecting one would run
         BEFORE `view_role_tools_inc.jsp`'s real `<select id="portletList">`
         is parsed, so `dijit.form.FilteringSelect` would upgrade THE STUB
         (first-in-DOM wins for `getElementById`) — leaving the real select
         inside the New Layout dialog with no widget and an empty dropdown.
         Wrappers that DO need the stub (currently `view_role_permissions_wrapper.jsp`)
         should inject their own. --%>
</div>

<script type="text/javascript">
    // Suppress the tree bootstrap only — everything else the shared
    // `view_roles_js_inc.jsp` needs at load time is now satisfied by the
    // hidden DOM stubs above (including the `<select id="portletList">`
    // that `initializePortletInfoList` upgrades to a FilteringSelect).
    //
    // DO NOT override `initializePortletInfoList` here: `createNewLayout`
    // calls it with itself as the callback and expects the callback to
    // fire only AFTER `allPortletInfoList` is populated. A synchronous
    // no-op that just invokes the callback creates an infinite recursion
    // (`createNewLayout` -> `initializePortletInfoList(createNewLayout)`
    // -> `createNewLayout` -> ...).
    //
    // `window.buildRolesTree` covers both the initial tree build AND the dijit
    // `roleTreeMenu.bindDomNode(...)` call inside `initializeRolesTreeWidget`.
    // Explicit `window.` (not a bare assignment) so this stays a shared-global
    // contract with `view_roles_js_inc.jsp` even if that file is ever refactored
    // to strict-mode / ES modules and loses its implicit globals.
    window.buildRolesTree = function () {};
</script>
