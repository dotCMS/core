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

    <%-- Portlet FilteringSelect @ view_roles_js_inc.jsp:1147-1316. The
         tools wrapper already provides `portletList` in its inc file, so
         only inject a stub if it isn't already present. --%>
    <script>
        if (!document.getElementById('portletList')) {
            var _portletListStub = document.createElement('select');
            _portletListStub.id = 'portletList';
            document.getElementById('rolesIframeStubsWrapper').appendChild(_portletListStub);
        }
    </script>
</div>

<script type="text/javascript">
    // Override the NAMED portlet-scoped initializers to no-ops before
    // dojo fires its onLoad queue. See file header for the rationale.
    // `buildRolesTree` covers both the initial tree build AND the dijit
    // `roleTreeMenu.bindDomNode(...)` call inside `initializeRolesTreeWidget`.
    buildRolesTree = function () {};
    initializePortletInfoList = function (callback) { if (callback) callback(); };
</script>
