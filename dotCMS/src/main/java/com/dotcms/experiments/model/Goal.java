package com.dotcms.experiments.model;

import com.dotcms.analytics.metrics.Metric;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.ClassNameIdResolver;
import com.liferay.util.StringPool;
import org.immutables.value.Value;


/**
 * Represent a Goal inside a {@link Experiment}, a Goal is the target the we are trying to reach
 * inside a Experiment, for example maybe we want "Maximize the amount of Reach Page for a specific page"
 * or "Minimize the amount of Bounce Rate for a specific page".
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @Type(value = ReachPageGoal.class, name = "REACH_PAGE"),
        @Type(value = BounceRateGoal.class, name = "BOUNCE_RATE"),
        @Type(value = ClickOnElementGoal.class, name = "CLICK_ON_ELEMENT"),
        @Type(value = UrlParameterGoal.class, name = "URL_PARAMETER")
})
public abstract class Goal {

    public  enum GoalType {
        MINIMIZE,
        MAXIMIZE;
    }

    private Metric metric;

    public Goal(final @JsonProperty("metric") Metric metric){
        this.metric = metric;
    }

    @JsonProperty()
    public Metric getMetric(){
        return this.metric;
    }

    @JsonIgnore
    public abstract GoalType type();

    @JsonIgnore
    public String name (){
        final String goalTypeName = type().name();
        return goalTypeName.charAt(0) + goalTypeName.substring(1) + StringPool.SPACE +
                getMetric().type().name();
    }
}
