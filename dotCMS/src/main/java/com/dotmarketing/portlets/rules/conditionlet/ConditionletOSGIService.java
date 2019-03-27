package com.dotmarketing.portlets.rules.conditionlet;

/**
 * Created by Oscar Arrieta on 10/16/15.
 */
public interface ConditionletOSGIService {

    /**
     * Adds a given Conditionlet class to the list of Rules Engine Conditionlet, this method will instantiate and
     * initialize (init method) the given Conditionlet.
     *
     * @param conditionletClass
     */
    String addConditionlet ( Class conditionletClass );

    /**
     * Removes a given Conditionlet class from the list of Rules Engine Conditionlet.
     *
     * @param conditionletName
     */
    void removeConditionlet ( String conditionletName );
}
