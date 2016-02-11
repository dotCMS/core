package com.dotmarketing.portlets.rules.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonIgnore;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.RelatedPermissionableGroup;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.rules.exception.RuleEngineException;
import com.dotmarketing.portlets.rules.util.RulePermissionableUtil;
import com.dotmarketing.util.Logger;

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
    private String parent;
    private String folder = "SYSTEM_FOLDER";
    private int priority;
    private boolean enabled;
    private Date modDate;
    private List<ConditionGroup> groups;
    private List<RuleAction> ruleActions;
    private Permissionable parentPermissionable;

    public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

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
        if(groups == null) {
            try {
                groups = FactoryLocator.getRulesFactory().getConditionGroupsByRule(id);
            } catch (DotDataException e) {
                throw new RuleEngineException(e, "Could not read groups for Rule %s ", this.toString());
            }
        }
        Collections.sort(groups);
        return groups;
    }

    public void setGroups(List<ConditionGroup> groups) {
        this.groups = groups;
    }

    public void addGroup(ConditionGroup group) {
        if(groups != null) {
            groups.add(group);
        }
    }

    public void removeGroup(ConditionGroup group) {
        if(groups != null) {
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
    public String getPermissionId() {
        return this.getId();
    }

    public String getOwner() {
        return null;
    }

    public void setOwner(String owner) {
        //TODO
    }

    public List<PermissionSummary> acceptedPermissions() {
        List<PermissionSummary> accepted = new ArrayList<PermissionSummary>();
        accepted.add(new PermissionSummary("use",
                                           "use-permission-description", PermissionAPI.PERMISSION_USE));
        return accepted;
    }

    public List<RelatedPermissionableGroup> permissionDependencies(int requiredPermission) {
        return null;
    }

    @JsonIgnore
    public Permissionable getParentPermissionable() throws DotDataException {
        Permissionable pp;

        if(parentPermissionable != null){
            pp = parentPermissionable;
        } else {
            pp = RulePermissionableUtil.findParentPermissionable(getParent());
        }

        return pp;
    }

    @JsonIgnore
    public void setParentPermissionable(Permissionable parentPermissionable){
        this.parentPermissionable = parentPermissionable;
    }

    public String getPermissionType() {
        return this.getClass().getCanonicalName();
    }

    public boolean isParentPermissionable() {
        return false;
    }
    // End Permissionable methods

    public void checkValid() {
        for (ConditionGroup group : getGroups()) {
            group.checkValid();
        }
        for (RuleAction ruleAction : getRuleActions()) {
            ruleAction.checkValid();
        }
    }

    public boolean evaluate(HttpServletRequest req, HttpServletResponse res) {
        if(this.evaluateConditions(req, res, getGroups())) {
            this.evaluateActions(req, res, getRuleActions());
            return true;
        }
        return false;
    }

    private void evaluateActions(HttpServletRequest req, HttpServletResponse res, List<RuleAction> actions) {
        for (RuleAction action : actions) {
            try {
                action.evaluate(req, res);
            } catch (Exception e) {
                Logger.warn(this.getClass(),
                            String.format("Rule evaluation failed on action '%s' for rule %s. Skipping any remaining actions.",
                                          action.getId(),
                                          this.name), e);
            }
        }
    }

    /**
     * Evaluate the Rule conditions.
     * <p>
     * A list of condition groups will be evaluated as a single logical group; precisely as if the results of each group's evaluation were placed into a
     * corresponding 'if()' statement in any C based programming language.
     * <p>
     * A || B && C          ==>  A || ( B && C )
     * A && B || C && D     ==> ( A && B ) || ( C && D )
     */
    public boolean evaluateConditions(HttpServletRequest req, HttpServletResponse res, List<ConditionGroup> groups) {
        /**
         *  @todo ggranum: this logic fails for three groups where:  (Group1 AND Group2 OR Group3). Also, as written it can be greatly simplified.
         *  The correct logic cannot be implemented without a stack.
         **/
        boolean result = true;
        for (ConditionGroup group : groups) {
            boolean groupResult = group.evaluate(req, res, group.getConditions());
            if(group.getOperator() == Condition.Operator.AND) {
                result = result && groupResult;
            } else {
                result = result || groupResult;
            }

            if(!result) { return false; }
        }

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if(o == this) { return true; }
        if(!(o instanceof Rule)) { return false; }
        String id = ((Rule)o).getId();
        return id != null && id.equals(this.id);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + id.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Rule [name=" + name + ", id=" + id + ", fireOn=" + fireOn
               + ", shortCircuit=" + shortCircuit + ", parent=" + parent
               + ", folder=" + folder + ", priority=" + priority
               + ", enabled=" + enabled + ", modDate=" + modDate + "]";
    }
}
