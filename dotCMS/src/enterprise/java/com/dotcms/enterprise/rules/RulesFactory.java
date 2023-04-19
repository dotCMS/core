/* 
* Licensed to dotCMS LLC under the dotCMS Enterprise License (the
* “Enterprise License”) found below 
* 
* Copyright (c) 2023 dotCMS Inc.
* 
* With regard to the dotCMS Software and this code:
* 
* This software, source code and associated documentation files (the
* "Software")  may only be modified and used if you (and any entity that
* you represent) have:
* 
* 1. Agreed to and are in compliance with, the dotCMS Subscription Terms
* of Service, available at https://www.dotcms.com/terms (the “Enterprise
* Terms”) or have another agreement governing the licensing and use of the
* Software between you and dotCMS. 2. Each dotCMS instance that uses
* enterprise features enabled by the code in this directory is licensed
* under these agreements and has a separate and valid dotCMS Enterprise
* server key issued by dotCMS.
* 
* Subject to these terms, you are free to modify this Software and publish
* patches to the Software if you agree that dotCMS and/or its licensors
* (as applicable) retain all right, title and interest in and to all such
* modifications and/or patches, and all such modifications and/or patches
* may only be used, copied, modified, displayed, distributed, or otherwise
* exploited with a valid dotCMS Enterprise license for the correct number
* of dotCMS instances.  You agree that dotCMS and/or its licensors (as
* applicable) retain all right, title and interest in and to all such
* modifications.  You are not granted any other rights beyond what is
* expressly stated herein.  Subject to the foregoing, it is forbidden to
* copy, merge, publish, distribute, sublicense, and/or sell the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
* 
* For all third party components incorporated into the dotCMS Software,
* those components are licensed under the original license provided by the
* owner of the applicable component.
*/

package com.dotcms.enterprise.rules;


import com.dotmarketing.business.Ruleable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.rules.model.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RulesFactory {

    List<Rule> getEnabledRulesByParent(Ruleable parent) throws DotDataException;

    List<Rule> getAllRulesByParent(Ruleable parent) throws DotDataException;

    List<Rule> getAllRules() throws DotDataException;

    Set<Rule> getRulesByParent(String parent, Rule.FireOn fireOn) throws DotDataException;

    List<Rule> getRulesByNameFilter(String nameFilter);

    Rule getRuleById(String id) throws DotDataException;

    /**
     * This method is intended to be used only on logic that involves delete operations on rules
     * @param id
     * @return
     * @throws DotDataException
     */
    Rule getRuleByIdFromDB(String id) throws DotDataException;

    List<RuleAction> getRuleActionsByRule(String ruleId) throws DotDataException;

    /**
     * This method is intended to be used only on logic that involves delete operations on rules
     * @param ruleId
     * @return
     * @throws DotDataException
     */
    List<RuleAction> getRuleActionsByRuleFromDB(String ruleId) throws DotDataException;

    RuleAction getRuleActionById(String ruleActionId) throws DotDataException;

    ParameterModel getRuleActionParameterById(String id) throws DotDataException;

    List<ConditionGroup> getConditionGroupsByRule(String ruleId) throws DotDataException;

    /**
     * This method is intended to be used only on logic that involves delete operations on rules
     * @param ruleId
     * @return
     * @throws DotDataException
     */
    List<ConditionGroup> getConditionGroupsByRuleFromDB(String ruleId)
            throws DotDataException;

    ConditionGroup getConditionGroupById(String conditionGroupId) throws DotDataException;

    List<Condition> getConditionsByGroup(String groupId) throws DotDataException;

    Condition getConditionById(String id) throws DotDataException ;

    ParameterModel getConditionValueById(String id) throws DotDataException;

    void saveRule(Rule rule) throws DotDataException;

    void saveConditionGroup(ConditionGroup condition) throws DotDataException;

    void saveCondition(Condition condition) throws DotDataException;

    void saveConditionValue(ParameterModel parameterModel) throws DotDataException;

    void saveRuleAction(RuleAction ruleAction) throws DotDataException;

    void deleteRule(Rule rule) throws DotDataException;

    void deleteConditionGroup(ConditionGroup conditionGroup) throws DotDataException;

    void deleteConditionsByGroup(ConditionGroup conditionGroup) throws DotDataException;

    void deleteCondition(Condition condition) throws DotDataException;

    void deleteConditionValue(ParameterModel parameterModel) throws DotDataException;

    void deleteRuleAction(RuleAction ruleAction) throws DotDataException;

    void deleteRuleActionsByRule(Rule rule) throws DotDataException;

    void deleteRuleActionsParameters(RuleAction action) throws DotDataException;

    void deleteConditionValues(Condition condition) throws DotDataException;

    Map<String, ParameterModel> getRuleActionParameters(RuleAction action) throws DotDataException;

}
