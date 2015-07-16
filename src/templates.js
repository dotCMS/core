import ruleEngineTemplate from './rule-engine-view/app/rule-engine.tpl.html!text'
import ruleTemplate from './rule-engine-view/app/rule-component.tpl.html!text'
import ruleActionTemplate from './rule-engine-view/app/rule-action-component.tpl.html!text'
import conditionGroupTemplate from './rule-engine-view/app/rule-condition-group-component.tpl.html!text'
import conditionTemplate from './rule-engine-view/app/rule-condition-component.tpl.html!text'

var templates = {
  ruleEngineTemplate: ruleEngineTemplate,
  ruleTemplate: ruleTemplate,
  ruleActionTemplate: ruleActionTemplate,
  conditionGroupTemplate: conditionGroupTemplate,
  conditionTemplate: conditionTemplate
}
let RuleEngine =  { templates }
// an ugly, ugly hack. A stepping stone on the way to transpiling typescript using JSPM 0.16+
Object.assign(window, {
  RuleEngine: RuleEngine
})

export {RuleEngine}
