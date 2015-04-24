package com.dotmarketing.portlets.rules.model;

import com.dotcms.repackage.org.codehaus.jackson.annotate.JsonIgnore;
import com.dotmarketing.business.*;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.rules.business.RulesAPI;
import com.dotmarketing.portlets.rules.business.RulesFactory;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class Rule implements Permissionable {

    public enum FireOn {
        EVERY_PAGE,
        ONCE_PER_VISIT,
        ONCE_PER_VISITOR,
        EVERY_REQUEST;

        @Override
        public String toString() {
            return super.name();
        }
    }

    private static final String BEGIN_CONDITION = "com.dotmarketing.portlets.rules.BeginCondition";
    private static final String END_CONDITION = "com.dotmarketing.portlets.rules.EndCondition";
    private static final String CONDITION_ = "com.dotmarketing.portlets.rules.EndCondition";



    private String id;
    private String name;
    private FireOn fireOn = FireOn.EVERY_PAGE;
    private boolean shortCircuit;
    private String host;
    private String folder="SYSTEM_FOLDER";
    private int priority;
    private boolean enabled;
    private Date modDate;
    private List<ConditionGroup> groups;

    @JSONIgnore
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

    public FireOn getFireOn() {
        return fireOn;
    }

    public void setFireOn(FireOn fireOn) {
        this.fireOn = fireOn;
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

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Date getModDate() {
        return modDate;
    }

    public void setModDate(Date modDate) {
        this.modDate = modDate;
    }

    public List<ConditionGroup> getGroups() {
        if(groups==null) {
            try {
                groups = FactoryLocator.getRulesFactory().getConditionGroupsByRule(id);
            } catch (DotDataException e) {
                Logger.error(this, "Unable to get condition groups for rule: " + id, e);
            }
        }
        return groups;
    }

    public void setGroups(List<ConditionGroup> groups) {
        this.groups = groups;
    }

    public void addGroup(ConditionGroup group) {
        if(groups!=null) {
            groups.add(group);
        }
    }

    public void removeGroup(ConditionGroup group) {
        if(groups!=null) {
            groups.remove(group);
        }
    }
    // Beginning Permissionable methods

    @JSONIgnore
    public String getPermissionId() {
        return this.getId();
    }

    @JSONIgnore
    public String getOwner() {
        return null;
    }

    public void setOwner(String owner) {}

    @JSONIgnore
    public List<PermissionSummary> acceptedPermissions() {
        List<PermissionSummary> accepted = new ArrayList<PermissionSummary>();
        accepted.add(new PermissionSummary("use",
                "use-permission-description", PermissionAPI.PERMISSION_USE));
        return accepted;
    }

    @JSONIgnore
    public List<RelatedPermissionableGroup> permissionDependencies(int requiredPermission) {
        return null;
    }

    @JSONIgnore
    public Permissionable getParentPermissionable() throws DotDataException {
        return null;
    }

    @JSONIgnore
    public String getPermissionType() {
        return this.getClass().getCanonicalName();
    }

    @JSONIgnore
    public boolean isParentPermissionable() {
        return false;
    }

    // End Permissionable methods

    public boolean evaluate(HttpServletRequest req, HttpServletResponse res) throws DotDataException {
        boolean result = true;

        for (ConditionGroup group : getGroups()) {
            if(group.getOperator()== Condition.Operator.AND) {
                result = result && group.evaluate(req, res);
            } else {
                result = result || group.evaluate(req, res);
            }

            if(!result) return false;
        }

        return result;

    }

    public boolean equals(Object o) {
        if(!UtilMethods.isSet(o)) return false;

        if(!(o instanceof Rule)) return false;

        return id.equals(((Rule)o).getId());
    }
}
