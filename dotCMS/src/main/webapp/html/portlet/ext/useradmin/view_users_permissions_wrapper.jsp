<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotmarketing.business.Role" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.liferay.portal.model.User" %>

<%@ include file="/html/common/init.jsp" %>
<%@ include file="/html/common/top_inc.jsp" %>
<%@ include file="/html/common/messages_inc.jsp" %>

<%
    // Bridges the legacy Dojo role-permissions UI to the Angular Users portlet as an
    // iframe tab, until the native implementation against
    // GET/PUT /api/v1/permissions/user/{userId} lands. A user's permissions are
    // rendered through their implicit user role because
    // com.liferay.portal.model.User does not implement Permissionable — the
    // asset-flavoured UI does not apply.
    final String userId = request.getParameter("userId");
    Role userRole = null;

    if (UtilMethods.isSet(userId)
            && APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("users", user)) {
        final User userToEdit = APILocator.getUserAPI().loadUserById(userId, user, false);
        userRole = APILocator.getRoleAPI().getUserRole(userToEdit);
    }

    if (userRole != null) {
%>
<%@ include file="/html/portlet/ext/roleadmin/view_role_permissions_js_inc.jsp" %>

<style>
    /*
     * view_role_permissions_js_inc.jsp hard-codes the accordion to 400px.
     * Bump it so users see more permission rows at once. If it ever ends
     * up taller than the iframe viewport, the browser's default iframe
     * scrollbar reveals the rest — no other layout tricks needed.
     */
    #permissionsAccordionContainer {
        height: 720px !important;
    }
</style>

<script type="text/javascript">
    // The role-permissions include does NOT declare its own dojo.require calls
    // (they normally live in view_roles_js_inc.jsp / view_users_js_inc.jsp),
    // so declare them here or the page loads blank.
    dojo.require("dijit.Dialog");
    dojo.require("dijit.ProgressBar");
    dojo.require("dijit.Tooltip");
    dojo.require("dijit.form.Button");
    dojo.require("dijit.form.CheckBox");
    dojo.require("dijit.layout.AccordionContainer");
    dojo.require("dijit.layout.ContentPane");
    dojo.require("dotcms.dijit.form.HostFolderFilteringSelect");

    // Callers of view_role_permissions_js_inc.jsp are expected to expose a
    // `norm()` helper in scope; view_users_js_inc.jsp / view_roles_js_inc.jsp
    // each define one. When embedded standalone we must provide our own or
    // loadRoleCallback throws `ReferenceError: norm is not defined`.
    function norm(value) {
        return dojo.isArray(value) ? value[0] : value;
    }

    dojo.addOnLoad(function () {
        loadPermissionsForRole('<%= userRole.getId() %>');
    });
</script>

<%@ include file="/html/portlet/ext/roleadmin/view_role_permissions_inc.jsp" %>
<%
    }
%>
