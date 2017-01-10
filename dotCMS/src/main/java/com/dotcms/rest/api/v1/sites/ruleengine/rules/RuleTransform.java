package com.dotcms.rest.api.v1.sites.ruleengine.rules;

import com.dotcms.repackage.org.apache.commons.lang.SerializationUtils;
import com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions.ConditionGroupTransform;
import com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions.RestConditionGroup;
import com.dotcms.rest.exception.BadRequestException;
import com.dotcms.rest.exception.InternalServerException;
import com.dotcms.rest.exception.NotFoundException;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import static com.dotcms.util.DotPreconditions.checkNotNull;

import java.util.*;

/**
 * @author Geoff M. Granum
 */
public class RuleTransform {
    private final RulesAPI rulesAPI;
    private ConditionGroupTransform groupTransform;

    public RuleTransform() { this(new ApiProvider()); }

    public RuleTransform(ApiProvider apiProvider) {
        this.rulesAPI = apiProvider.rulesAPI();
        groupTransform = new ConditionGroupTransform(apiProvider);
    }

    public Rule restToApp(RestRule rest, User user) {
        Rule app = new Rule();
        return applyRestToApp(rest, app, user);
    }

    public Rule applyRestToApp(RestRule rest, Rule rule, User user) {
    	Rule app = (Rule) SerializationUtils.clone(rule);
    	
        app.setName(rest.name);
        app.setFireOn(Rule.FireOn.valueOf(rest.fireOn));
        app.setPriority(rest.priority);
        app.setShortCircuit(rest.shortCircuit);
        app.setEnabled(rest.enabled);
        List<ConditionGroup> groups = checkGroupsExist(rest, app, user);
        Collections.sort(groups);

        app.setGroups(groups);
        return app;
    }

    private List<ConditionGroup> checkGroupsExist(RestRule rest, Rule app, User user) {
        List<ConditionGroup> groups = new ArrayList<>();

        for (Map.Entry<String, RestConditionGroup> group : rest.conditionGroups.entrySet()) {
            try {
                ConditionGroup existingGroup = checkNotNull(rulesAPI.getConditionGroupById(group.getKey(), user, false),
                        BadRequestException.class, "Group with key '%s' not found", group);

                if(!existingGroup.getRuleId().equals(app.getId()))
                    throw new BadRequestException("Group with key '%s' does not belong to Rule", existingGroup.getId());

                groups.add(existingGroup);
            } catch (DotDataException | DotSecurityException e) {
                Logger.error(this, "Error applying RestRule to Rule", e);
                throw new BadRequestException(e, e.getMessage());
            }
        }
        return groups;
    }

    public RestRule appToRest(Rule app, User user) {
        try{
            Map<String, RestConditionGroup> groups = new HashMap<>();
            Map<String, Boolean> ruleActions = new HashMap<>();

            for (ConditionGroup conditionGroup : app.getGroups()) {
                groups.put(conditionGroup.getId(), groupTransform.appToRest(conditionGroup));
            }

            for (RuleAction ruleAction : app.getRuleActions()) {
                if (rulesAPI.findActionlet(ruleAction.getActionlet()) != null) {
                    ruleActions.put(ruleAction.getId(), true);
                } else {
                    Logger.error(this, "Actionlet not found: " + ruleAction.getActionlet());
                    throw new NotFoundException("Actionlet not found: '%s'", ruleAction.getActionlet());
                }
            }

            return new RestRule.Builder()
                    .key(app.getId())
                    .name(app.getName())
                    .fireOn(app.getFireOn().toString())
                    .shortCircuit(app.isShortCircuit())
                    .priority(app.getPriority())
                    .enabled(app.isEnabled())
                    .conditionGroups(groups)
                    .ruleActions(ruleActions)
                    .build();

        } catch (Exception e) {
            try{
                APILocator.getRulesAPI().disableRule(app, user);
            } catch (DotDataException|DotSecurityException dse){
                Logger.error(this, "Error trying to disable Rule: " + app.getId(), dse);
            }

            throw new InternalServerException(e, "Could not create ReST Rule from Server, Rule id '%s'", app.getId());
        }
    }

}

