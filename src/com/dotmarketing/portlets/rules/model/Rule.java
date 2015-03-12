package com.dotmarketing.portlets.rules.model;

import com.dotcms.repackage.org.codehaus.jackson.annotate.JsonIgnore;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.RelatedPermissionableGroup;
import com.dotmarketing.exception.DotDataException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Rule implements Permissionable {

    // Beginning Permissionable methods

    public String getPermissionId() {
        return this.getId();
    }

    public String getOwner() {
        return null;
    }

    public void setOwner(String owner) {}

    @JsonIgnore
    public List<PermissionSummary> acceptedPermissions() {
        List<PermissionSummary> accepted = new ArrayList<PermissionSummary>();
        accepted.add(new PermissionSummary("use",
                "use-permission-description", PermissionAPI.PERMISSION_USE));
        return accepted;
    }

    @JsonIgnore
    public List<RelatedPermissionableGroup> permissionDependencies(int requiredPermission) {
        return null;
    }

    @JsonIgnore
    public Permissionable getParentPermissionable() throws DotDataException {
        return null;
    }

    @JsonIgnore
    public String getPermissionType() {
        return this.getClass().getCanonicalName();
    }

    @JsonIgnore
    public boolean isParentPermissionable() {
        return false;
    }

    // End Permissionable methods

    public enum FirePolicy {
        EVERY_PAGE,
        ONCE_PER_VISIT,
        ONCE_PER_VISITOR,
        EVERY_REQUEST

    }

    private String id;
    private String name;
    private FirePolicy firePolicy;
    private boolean shortCircuit;
    private String host="SYSTEM_HOST";
    private String folder="SYSTEM_FOLDER";
    private int fireOrder;
    private boolean enabled;
    private String expression;
    private Date modDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FirePolicy getFirePolicy() {
        return firePolicy;
    }

    public void setFirePolicy(FirePolicy firePolicy) {
        this.firePolicy = firePolicy;
    }

    public boolean isShortCircuit() {
        return shortCircuit;
    }

    public void setShortCircuit(boolean shortCircuit) {
        this.shortCircuit = shortCircuit;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public int getFireOrder() {
        return fireOrder;
    }

    public void setFireOrder(int fireOrder) {
        this.fireOrder = fireOrder;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public Date getModDate() {
        return modDate;
    }

    public void setModDate(Date modDate) {
        this.modDate = modDate;
    }
}
