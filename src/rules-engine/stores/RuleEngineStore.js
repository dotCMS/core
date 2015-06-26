import events from 'events';
import XDebug from 'debug';
let log = XDebug('RulesEngine.store');

import {Core} from '../../coreweb/api/Core.js';
import {Check} from '../../coreweb/api/Check.js';
import {AppDispatcher} from '../../rules-engine/dispatcher/AppDispatcher.js';
import {ruleRepo} from '../api/RuleEngineAPI.js'
import {actionTypes} from '../actions/RuleEngineActionCreators.js'

let CHANGE_EVENT = 'change';


class _RuleStore extends events.EventEmitter {

  dispatchToken;

  constructor() {
    super()
    this.dispatchToken = AppDispatcher.register(registrationFn);
  }

  emitChange(action) {
    this.emit(CHANGE_EVENT, action);
  }

  /**
   * @param {function} callback
   */
  addChangeListener(callback) {
    this.on(CHANGE_EVENT, callback);
  }

  removeChangeListener(callback) {
    this.removeListener(CHANGE_EVENT, callback);
  }

  init() {
    ruleRepo.init()
  }

  get(key) {
    let p
    if (!key) {
      return new Promise((resolve, reject) => {
        resolve(Array.from(_store.values()))
      });
    }
    else if (key && _store.has(key)) {
      return new Promise((resolve, reject) => {
        resolve(_store.get(key))
      });
    }
    else {
      p = new Promise((resolve, reject) => {
        ruleRepo.get(key).then((rule)=> {
          resolve(rule ? true : false)
        }, (e) => reject(e) )
      });
    }
    return p
  }
}
let _store = new Map()

let registrationFn = function (action) {
  switch (action.key) {
    case actionTypes.UPDATE_RULE:
      _store.set(action.rule.$key, action.rule)
      log(action.key, action.rule.$key, action.rule.name, 'count:' + _store.size)
      RuleStore.emitChange(action)
      break;
    case actionTypes.ADD_RULE:
      _store.set(action.rule.$key, action.rule)
      log(action.key, action.rule.$key, action.rule.name, 'count:' + _store.size)
      RuleStore.emitChange(action)
      break;
    case actionTypes.REMOVE_RULE:
      _store.delete(action.ruleKey)
      log(action.key, action.rule.$key, action.ruleKey, 'count:  ' + _store.size)
      RuleStore.emitChange(action)
      break;
    default:
      log(action.key, "Unhandled")
  }
}

export let RuleStore = new _RuleStore();
