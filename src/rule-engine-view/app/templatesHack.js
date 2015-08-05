import ruleEngineTemplate from './templates/rule-engine.tpl.html!text'
import conditionGroupTemplate from './templates/rule-condition-group-component.tpl.html!text'
import ruleTemplate from './templates/rule-component.tpl.html!text'
import conditionTemplate from './templates/rule-condition-component.tpl.html!text'
import ruleActionTemplate from './templates/rule-action-component.tpl.html!text'


let a = ruleEngineTemplate;
let b = conditionGroupTemplate;
let c = ruleTemplate;
let d = conditionTemplate;
let e = ruleActionTemplate;

export {
    a as ruleEngineTemplate,
    b as conditionGroupTemplate,
    c as ruleTemplate,
    d as conditionTemplate,
    e as ruleActionTemplate
}