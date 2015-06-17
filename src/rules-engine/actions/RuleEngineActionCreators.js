import {AppDispatcher} from 'src/rules-engine/dispatcher/AppDispatcher.ts';

import  {Core} from 'src/dc/index.js'


export let actionTypes = Core.Collections.asSymmetricObjectMap({
  ADD_RULE: null,
  REMOVE_RULE: null,
  UPDATE_RULE: null,
  ADD_CLAUSE: null,
  UPDATE_CLAUSE: null,
  REMOVE_CLAUSE: null
}, 'RuleActions.')


export let actions = {

  addRule(rule) {
    AppDispatcher.dispatch({
      key: actionTypes.ADD_RULE,
      rule: rule
    });
  },

  updateRule(rule) {
    AppDispatcher.dispatch({
      key: actionTypes.UPDATE_RULE,
      rule: rule
    });
  },

  removeRule(ruleKey) {
    AppDispatcher.dispatch({
      key: actionTypes.REMOVE_RULE,
      ruleKey: ruleKey
    });
  }

}



