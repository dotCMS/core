package com.dotmarketing.portlets.rules.actionlet;

public interface RuleActionletOSGIService {

    /**
     * Adds a given RuleActionlet class to the list of Rules Engine RuleActionlets
     *
     * @param actionletClass
     */
    String addRuleActionlet ( Class actionletClass );

    /**
     * Removes a given RuleActionlet class from the list of Rules Engine RuleActionlets.
     *
     * @param actionletName
     */
    void removeRuleActionlet ( String actionletName );
}
