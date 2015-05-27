import events from 'events';
import XDebug from 'debug';
let log = XDebug('RulesEngine.store');

import {Core} from 'src/dc/index.es6'
import {AppDispatcher} from 'src/rules-engine/dispatcher/AppDispatcher.ts';
import * as RuleEngine from 'src/rules-engine/RuleEngine.es6';


let CHANGE_EVENT = 'change';


class _RuleStore extends events.EventEmitter {

  dispatchToken:any;

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
    RuleEngine.api.ruleRepo.init()
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
        RuleEngine.api.ruleRepo.get(key).then((rule)=> {
          resolve(rule ? true : false)
        }, (e) => reject(e) )
      });
    }
    return p
  }
}
let _store:Map<any,any> = new Map()

let registrationFn = function (action) {
  switch (action.key) {
    case RuleEngine.actionTypes.ADD_RULE:
      _store.set(action.rule.$key, action.rule)
      log(action.key, action.rule.$key, action.rule.name, 'count:' + _store.size)
      RuleStore.emitChange(action)
      break;
    case RuleEngine.actionTypes.REMOVE_RULE:
      _store.delete(action.ruleKey)
      log(action.key, action.rule.$key, action.ruleKey, 'count:  ' + _store.size)
      RuleStore.emitChange(action)
      break;
    default:
      log(action.key, "Unhandled")
  }
}

export let RuleStore = new _RuleStore();
