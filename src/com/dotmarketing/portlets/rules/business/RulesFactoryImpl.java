package com.dotmarketing.portlets.rules.business;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;

import java.util.List;

public class RulesFactoryImpl implements RulesFactory {
    @Override
    public List<Rule> getRules(String hostOrFolder) {
        return null;
    }

    @Override
    public List<Rule> getRulesByNameFilter(String nameFilter) {
        return null;
    }

    @Override
    public Rule getRuleById(String id) {
        return null;
    }

    @Override
    public void deleteRule(Rule rule) {

    }

    @Override
    public List<Condition> getConditionsByRule(String ruleId) {
        return null;
    }

    @Override
    public Condition getConditionById(String id) {
        return null;
    }

    @Override
    public void saveRule(Rule rule) {
        boolean isNew = true;
        if (UtilMethods.isSet(rule.getId())) {
            try {
                if (getRuleById(rule.getId()) != null) {
                    isNew = false;
                }
            } catch (final Exception e) {
                Logger.debug(this.getClass(), e.getMessage(), e);
            }
        } else {
            rule.setId(UUIDGenerator.generateUuid());
        }

        final DotConnect db = new DotConnect();
        if (isNew) {

            db.setSQL(sql.INSERT_STEP);
            db.addParam(rule.getId());
            db.addParam(rule.getName());
            db.addParam(rule.getFirePolicy().toString());
            db.addParam(rule.isShortCircuit());
            db.addParam(rule.getHost());
            db.addParam(rule.getFolder());
            db.addParam(rule.getFireOrder());
            db.addParam(rule.isEnabled());

            db.loadResult();
        } else {
            db.setSQL(sql.UPDATE_STEP);
            db.addParam(step.getName());
            db.addParam(step.getSchemeId());
            db.addParam(step.getMyOrder());
            db.addParam(step.isResolved());
            db.addParam(step.isEnableEscalation());
            if(step.isEnableEscalation()) {
                db.addParam(step.getEscalationAction());
                db.addParam(step.getEscalationTime());
            }
            else {
                db.addParam((Object)null);
                db.addParam(0);
            }
            db.addParam(step.getId());
            db.loadResult();
        }
        cache.remove(step);
    }

    @Override
    public void saveCondition(Condition condition) {

    }

    @Override
    public void deleteCondition(Condition condition) {

    }
}
