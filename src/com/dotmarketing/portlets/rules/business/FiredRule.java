package com.dotmarketing.portlets.rules.business;

import com.dotmarketing.portlets.rules.model.Rule;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by freddyrodriguez on 28/3/16.
 */
public class FiredRule implements Comparable<FiredRule>{

    private static final DateFormat SIMPLE_DATE_FORMAT = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT);

    private final String ruleName;
    private final byte fireOn;
    private long fireTime;
    private String ruleID;


    FiredRule(Date fireTime, Rule rule) {
        this.fireTime = fireTime.getTime();
        this.ruleID = rule.getId();
        this.ruleName = rule.getName();
        this.fireOn = (byte) rule.getFireOn().ordinal();
    }

    public void setFireTime(long fireTime){
        this.fireTime = fireTime;
    }

    public String getFireTime() {
        return SIMPLE_DATE_FORMAT.format(fireTime);
    }

    public String getRuleID() {
        return ruleID;
    }

    @Override
    public int compareTo(FiredRule o) {
        return (int) (fireTime - o.fireTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FiredRule firedRule = (FiredRule) o;

        return ruleID != null ? ruleID.equals(firedRule.ruleID) : firedRule.ruleID == null;

    }

    @Override
    public int hashCode() {
        return ruleID != null ? ruleID.hashCode() : 0;
    }

    public String getRuleName() {
        return ruleName;
    }

    public Rule.FireOn getFireOn() {
        return Rule.FireOn.values()[fireOn];
    }

    public long getFireTimeAsLong() {
        return fireTime;
    }
}
