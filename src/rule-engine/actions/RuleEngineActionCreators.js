import  {Core} from '../../coreweb/index.js'

import {AppDispatcher} from '../dispatcher/AppDispatcher.js';



export let actionTypes = Core.Collections.copyKeysToValues({
  UPDATE: null,
  ADD_RULE: null,
  REMOVE_RULE: null,
  UPDATE_RULE: null,
  ADD_CLAUSE: null,
  UPDATE_CLAUSE: null,
  REMOVE_CLAUSE: null
}, 'RuleActions.')


export let actions = {

  update(path){
    AppDispatcher.dispatch({
      key: actionTypes.UPDATE,
      path: path
    });
  },

  addRule(rule) {
    AppDispatcher.dispatch({
      key: actionTypes.ADD_RULE,
      path: rule.path,
      rule: rule
    });
  },

  updateRule(rule) {
    AppDispatcher.dispatch({
      key: actionTypes.UPDATE_RULE,
      path: rule.path,
      rule: rule
    });
  },

  removeRule(ruleKey) {
    AppDispatcher.dispatch({
      key: actionTypes.REMOVE_RULE,
      path: rule.path,
      ruleKey: ruleKey
    });
  }

}


