package com.dotmarketing.portlets.rules.model;

import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.RelatedPermissionableGroup;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONIgnore;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Rule implements Permissionable, Serializable {

    private static final long serialVersionUID = 1L;

    public enum FireOn {
        EVERY_PAGE("EveryPage"),
        ONCE_PER_VISIT("OncePerVisit"),
        ONCE_PER_VISITOR("OncePerVisitor"),
        EVERY_REQUEST("EveryRequest");

        private String camelCaseName;

        FireOn(String camelCaseName) {
            this.camelCaseName = camelCaseName;
        }

        @Override
        public String toString() {
            return super.name();
        }

        public String getCamelCaseName() {
            return camelCaseName;
        }


    }

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
    private List<RuleAction> ruleActions;

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

    public List<RuleAction> getRuleActions() {
        if(ruleActions == null) {
            try {
                ruleActions = FactoryLocator.getRulesFactory().getRuleActionsByRule(id);
            } catch (DotDataException e) {
                Logger.error(this, "Unable to get rule actions for rule: " + id, e);
            }
        }
        return ruleActions;
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

        /* @todo ggranum: this logic fails for a three groups where:  (Group1 AND Group2 OR Group3). Also, as written it can be greatly simplified. */
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

    @Override
    public boolean equals(Object o) {
        if(o == this) return true;
        if(!(o instanceof Rule)) return false;
        String id = ((Rule) o).getId();
        return id!=null && id.equals(this.id);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + id.hashCode();
        return result;
    }

    @JSONIgnore
    @Override
    public String toString() {
        return "Rule [id=" + id + ", name=" + name + ", fireOn=" + fireOn
                + ", shortCircuit=" + shortCircuit + ", host=" + host
                + ", folder=" + folder + ", priority=" + priority
                + ", enabled=" + enabled + ", modDate=" + modDate + "]";
    }
 
}
