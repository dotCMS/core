package com.dotmarketing.portlets.rules.business;

import java.util.*;

/**
 * Created by freddyrodriguez on 29/3/16.
 */
public class FiredRulesList {
    private List<FiredRule> list = new LinkedList<>();
    private Map<String, Integer> map = new HashMap();

    public List<FiredRule>  values(){
        synchronized ( list ) {
            return new LinkedList<>(list);
        }
    }

    public void add(FiredRule firedRule){
        String ruleID = firedRule.getRuleID();
        Integer index  = map.get(ruleID);

        if (index == null){
            list.add( firedRule );
            map.put(ruleID, list.size() - 1);
        }else{
            FiredRule firedRuleInMap = list.get( index );
            firedRuleInMap.setFireTime( firedRule.getFireTimeAsLong() );
        }
    }

    public String getLastFiredTime(String ruleId){
        Integer index  = map.get(ruleId);

        if (index != null){
            return list.get( index ).getFireTime();
        }else{
            return null;
        }
    }

    public int size() {
        return list.size();
    }

}
