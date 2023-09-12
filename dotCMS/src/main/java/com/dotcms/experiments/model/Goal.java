package com.dotcms.experiments.model;

import com.dotcms.analytics.metrics.Metric;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.liferay.util.StringPool;
import java.io.Serializable;


/**
 * Represent a Goal inside a {@link Experiment}, a Goal is the target the we are trying to reach
 * inside a Experiment, for example maybe we want "Maximize the amount of Reach Page for a specific page"
 * or "Minimize the amount of Bounce Rate for a specific page".
 */

public class Goal implements Serializable {

    public  enum GoalType {
        MINIMIZE,
        MAXIMIZE;
    }

    private Metric metric;
    private GoalType type;

    public Goal(final @JsonProperty("metric") Metric metric, final @JsonProperty("type") GoalType type){
        this.metric = metric;
        this.type = type;
    }

    @JsonProperty()
    public Metric getMetric(){
        return this.metric;
    }

    @JsonIgnore
    public  GoalType type() {
        return type;
    }

    @JsonIgnore
    public String name (){
        final String goalTypeName = type().name();
        return goalTypeName.charAt(0) + goalTypeName.substring(1) + StringPool.SPACE +
                getMetric().type().name();
    }
}
