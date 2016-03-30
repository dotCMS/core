package com.dotmarketing.portlets.rules.business;

import java.util.Date;

/**
 * Created by freddyrodriguez on 28/3/16.
 */
public class FiredRule implements Comparable<FiredRule>{
    private Date fireTime;
    private String ruleID;


    FiredRule(Date fireTime, String ruleID) {
        this.fireTime = fireTime;
        this.ruleID = ruleID;
    }

    public Date getFireTime() {
        return fireTime;
    }

    public String getRuleID() {
        return ruleID;
    }

    public void setFireTime( Date newFireTime ) {
        this.fireTime = newFireTime;
    }

    @Override
    public int compareTo(FiredRule o) {
        return o.getFireTime().compareTo( fireTime );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FiredRule firedRule = (FiredRule) o;

        if (!fireTime.equals(firedRule.fireTime)) return false;
        return ruleID.equals(firedRule.ruleID);

    }

    @Override
    public int hashCode() {
        int result = fireTime.hashCode();
        result = 31 * result + ruleID.hashCode();
        return result;
    }
}
