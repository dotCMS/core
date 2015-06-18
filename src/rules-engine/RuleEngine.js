import {ruleRepo} from '../rules-engine/api/RuleEngineAPI.js';
import {actions, actionTypes} from '../rules-engine/actions/RuleEngineActionCreators.js';
import {RuleStore} from '../rules-engine/stores/RuleEngineStore.js';

let RuleEngine = {
  ruleRepo: ruleRepo,
  actions: actions,
  actionTypes: actionTypes,
  store: RuleStore
}

export {RuleEngine};


