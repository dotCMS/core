package com.dotmarketing.beans;

import java.util.ArrayList;
import java.util.List;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.RelatedPermissionableGroup;
import com.dotmarketing.exception.DotDataException;

/**
 *
 * @author maria
 */
public class UserProxy implements Permissionable {


    private static final long serialVersionUID = 1L;
    private final String userId;

    public UserProxy(String userId) {
        this.userId = userId;

    }

    public List<PermissionSummary> acceptedPermissions() {
        List<PermissionSummary> accepted = new ArrayList<PermissionSummary>();
        accepted.add(new PermissionSummary("view", "view-permission-description", PermissionAPI.PERMISSION_READ));
        accepted.add(new PermissionSummary("edit", "edit-permission-description", PermissionAPI.PERMISSION_WRITE));
        accepted.add(new PermissionSummary("edit-permissions", "edit-permissions-permission-description",
                        PermissionAPI.PERMISSION_EDIT_PERMISSIONS));
        return accepted;
    }

    @Override
    public String getPermissionId() {
        return "user:" + this.userId;
    }

    @Override
    public String getOwner() {
        return null;
    }

    @Override
    public void setOwner(String owner) {
        
    }

    @Override
    public List<RelatedPermissionableGroup> permissionDependencies(int requiredPermission) {
        return null;
    }

    @Override
    public Permissionable getParentPermissionable() throws DotDataException {
        return APILocator.systemHost();
    }

    @Override
    public String getPermissionType() {
        return UserProxy.class.getCanonicalName();
    }

    @Override
    public boolean isParentPermissionable() {
        return false;
    }
}
