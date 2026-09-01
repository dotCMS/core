<%@ page import="com.dotmarketing.business.APILocator" %>
<%@ page import="com.dotmarketing.business.Role" %>
<%@ page import="com.dotmarketing.exception.DotDataException" %>
<%@ page import="com.dotmarketing.exception.DotSecurityException" %>
<%@ page import="com.dotmarketing.util.Logger" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.liferay.portal.model.User" %>
<%--
    Do NOT `page import` `com.dotmarketing.business.NoSuchUserException` here:
    `/html/common/init.jsp` transitively pulls in `com.liferay.portal.NoSuchUserException`,
    and the JSP compiler rejects two imports with the same simple name. We reach
    the dotCMS variant by its FQN in the catch clause below.
--%>

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
        try {
            final User userToEdit = APILocator.getUserAPI().loadUserById(userId, user, false);
            userRole = APILocator.getRoleAPI().getUserRole(userToEdit);
        } catch (com.dotmarketing.business.NoSuchUserException | DotSecurityException e) {
            // The id did not resolve, or the viewer lacks READ on the target user.
            // Both are expected: leave userRole null so the page renders a blank
            // body and the Angular tab shows its `unavailable` message. Letting
            // these escape would put a container error page inside the iframe —
            // top_inc.jsp has already written to the response by this point.
            //
            // FQN on the catch is deliberate: `init.jsp` imports Liferay's
            // `NoSuchUserException` as the simple name, so we cannot page-import
            // the dotmarketing variant without a compile collision.
            Logger.warn(this.getClass(),
                    "Cannot render permissions for User ID '" + userId + "': " + e.getMessage());
        } catch (DotDataException e) {
            Logger.error(this.getClass(),
                    "Error loading permissions for User ID '" + userId + "': " + e.getMessage(), e);
        }
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

<%--
    Success marker read by dot-users-permissions-tab. The embedding component
    cannot tell a rendered permissions UI from a failed one by inspecting the
    body: top_inc.jsp and messages_inc.jsp run on every path and leave <script>
    elements behind, so the body is never empty. This element renders last, so
    its presence means the whole granted path completed — a mid-render failure
    aborts the page before it and correctly reads as unavailable.
--%>
<div id="dot-permissions-ready" hidden></div>
<%
    }
%>
