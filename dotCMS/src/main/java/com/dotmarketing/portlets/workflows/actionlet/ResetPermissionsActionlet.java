package com.dotmarketing.portlets.workflows.actionlet;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionBitAPIImpl;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.ajax.PermissionAjax;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Map;


/**
 * This Actionlet allows the user to reset the permissions of a contentlet
 * The user must have edit permissions to fire this action.
 */
public class ResetPermissionsActionlet extends WorkFlowActionlet{
    @Override
    public List<WorkflowActionletParameter> getParameters() {
        return List.of();
    }

    @Override
    public String getName() {
        return "Reset Permissions";
    }

    @Override
    public String getHowTo() {
        return "This actionlet will reset permissions of the selected contentlets. It does not require any parameters.";
    }

    /*
    * This method will reset the permissions of the contentlet after checking that the user has edit permissions to modify it.
    * */
    @Override
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

        try {

            User user = processor.getUser();

            PermissionAPI permissionAPI = APILocator.getPermissionAPI();
            PermissionBitAPIImpl api = (PermissionBitAPIImpl) APILocator.getPermissionAPI();
            Permissionable asset = processor.getContentlet();
            if (!user.isAdmin() && !api.doesUserHavePermission(asset, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, user) &&
                    !api.checkIfContentletTypeHasEditPermissions(asset, user)) {

                    throw new DotSecurityException("User id: " + user.getUserId() + " does not have permission to alter permissions on asset " + asset.getPermissionId());
            }
            permissionAPI.removePermissions(asset);
        } catch ( Exception e) {
            Logger.debug(ResetPermissionsActionlet.class, e.getMessage());
            throw new WorkflowActionFailureException(e.getMessage(), e);
        }

    }
}
