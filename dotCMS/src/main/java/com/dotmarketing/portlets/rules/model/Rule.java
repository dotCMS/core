package com.dotmarketing.portlets.rules.model;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.manifest.ManifestItem;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.RelatedPermissionableGroup;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.rules.exception.RuleEngineException;
import com.dotmarketing.portlets.rules.util.LogicalCondition;
import com.dotmarketing.portlets.rules.util.LogicalStatement;
import com.dotmarketing.portlets.rules.util.RulePermissionableUtil;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A Rule is composed of a list of conditions that get checked against when a 
 * visitor requests a page. If the conditions in a rule are met, then one or 
 * more actions are fired. Rules can be configured from their specific portlet 
 * in the back-end and via RESTful services as well.
 * 
 * @author Daniel Silva
 * @version 1.0
 * @since Mar 10, 2015
 *
 */
public class Rule implements Permissionable, Serializable, ManifestItem {

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

    public Rule(){

    }

    public Rule(Rule ruleToCopy) {

        id = ruleToCopy.id;
        name = ruleToCopy.name;
        fireOn = ruleToCopy.fireOn;
        shortCircuit = ruleToCopy.shortCircuit;
        parent = ruleToCopy.parent;
        folder = ruleToCopy.folder;
        priority = ruleToCopy.priority;
        enabled = ruleToCopy.enabled;
        modDate = ruleToCopy.modDate;
        if(ruleToCopy.getGroups() != null) {
            groups = Lists.newArrayList();
            for (ConditionGroup group : ruleToCopy.getGroups()) {
                groups.add(new ConditionGroup(group));
            }
        }
        if(ruleToCopy.getRuleActions() != null){
            ruleActions = Lists.newArrayList();
            for (RuleAction ruleAction : ruleToCopy.getRuleActions()) {
                ruleActions.add(new RuleAction(ruleAction));
            }
        }
    }

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

    public List<ConditionGroup> getGroupsRaw() {
        return groups;
    }

    @CloseDBIfOpened
    public List<ConditionGroup> getGroups() {
        if(groups == null) {
            try {
                //This will return the Groups sorted by priority asc directly from DB.
                groups = FactoryLocator.getRulesFactory().getConditionGroupsByRule(id);
            } catch (DotDataException e) {
                throw new RuleEngineException(e, "Could not read groups for Rule %s ", this.toString());
            }
        }

        //Return a shallow copy of the list.
        return Lists.newArrayList(groups);
    }

    public void setGroups(List<ConditionGroup> groups) {
        this.groups = groups;
    }

    public List<RuleAction> getRuleActionsRaw() {

        return ruleActions;
    }

    @CloseDBIfOpened
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
        List<PermissionSummary> accepted = new ArrayList<>();
        accepted.add(new PermissionSummary("use",
                                           "use-permission-description", PermissionAPI.PERMISSION_USE));
        return accepted;
    }

    public List<RelatedPermissionableGroup> permissionDependencies(int requiredPermission) {
        return null;
    }

    @CloseDBIfOpened
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

	/**
	 * Evaluates the set of conditions that make up this rule based on the
	 * issued HTTP request. If the final result of such an evaluation is true,
	 * then a set of one or more actions is executed.
	 * 
	 * @param req
	 *            - The {@link HttpServletRequest} object that is triggering the
	 *            rules evaluation/execution.
	 * @param res
	 *            - The {@link HttpServletResponse} object.
	 * @return If the given conditions satisfy the rule, returns
	 *         <code>true</code>. Otherwise, returns <code>false</code>.
	 */
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
                Logger.debug(this, ()->"Evaluating action: " + action.getActionlet());
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
        LogicalStatement statement = new LogicalStatement();
        for (ConditionGroup group : groups) {
            GroupLogicalCondition logicalCondition = new GroupLogicalCondition(group, req, res);
            if(group.getOperator() == LogicalOperator.AND) {
                statement.and(logicalCondition);
            } else {
                statement.or(logicalCondition);
            }
        }

        return statement.evaluate();
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
        return "Rule [id=" + id + ", name=" + name + ", fireOn=" + fireOn
               + ", shortCircuit=" + shortCircuit + ", parent=" + parent
               + ", folder=" + folder + ", priority=" + priority
               + ", enabled=" + enabled + ", modDate=" + modDate + "]";
    }

    @JsonIgnore
    @Override
    public ManifestInfo getManifestInfo(){
        return new ManifestInfoBuilder()
                .objectType(PusheableAsset.RULE.getType())
                .id(this.id)
                .title(this.name)
                .folderId(this.folder)
                .build();
    }

    private final class GroupLogicalCondition implements LogicalCondition {

        private final ConditionGroup group;
        private final HttpServletRequest req;
        private final HttpServletResponse res;

        public GroupLogicalCondition(ConditionGroup group, HttpServletRequest req, HttpServletResponse res) {
            this.group = group;
            this.req = req;
            this.res = res;
        }

        @Override
        public boolean evaluate() {
            return group.evaluate(req, res, group.getConditions());
        }
    }
}
