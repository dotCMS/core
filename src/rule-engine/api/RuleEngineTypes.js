import XDebug from 'debug';
let log = XDebug('RuleEngine.api');

import  {Core} from '../../coreweb/index.js'
import  {EntityForge as EF, ValidationError} from '../../entity-forge/index.js'


let defaultSiteId = '48190c8c-42c4-46af-8d1a-0cd5db894797'
let objFn = (function () {return {}})

let RuleDefinition = {
  $key: EF.string(),
  name: EF.string().minLength(5).maxLength(100),
  enabled: EF.bool(true),
  site: EF.string().minLength(36).maxLength(36),
  priority: EF.int(0).min(0).max(100),
  fireOn: EF.enum().values([
    'EVERY_PAGE',
    'ONCE_PER_VISIT',
    'ONCE_PER_VISITOR',
    'EVERY_REQUEST'
  ]).initTo(0),
  folder: EF.string().minLength(5).maxLength(250),
  shortCircuit: EF.bool(true),
  groups: EF.obj('groups', {
    operator: EF.enum().values(['AND', 'OR']),
    priority: EF.int(0).min(0).max(100)
  }),
  actions: EF.any()
}


let RuleGroupDefinition = {
  $key: EF.string(),
  priority: EF.int(0).min(0).max(100),
  operator: EF.string(),
  ruleKey: EF.string()
}



let RuleGroup = EF.obj('RuleGroup', RuleGroupDefinition).asNewable()
let Rule = EF.obj('Rule', RuleDefinition).asNewable()

export {Rule, RuleGroup}