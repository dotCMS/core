import XDebug from 'debug';
let log = XDebug('RulesEngine.api');

import {mocks} from '../datamocks/rule.mocks.js';

import {AppDispatcher} from '../../rules-engine/dispatcher/AppDispatcher.js';
import {Core} from '../../coreweb/api/Core.js';
import {Check} from '../../coreweb/api/Check.js';

import * as RuleEngine from '../actions/RuleEngineActionCreators.js';
import * as RuleTypes from  './RuleEngineTypes.js';


let dispatchToken = AppDispatcher.register((action) => {
  switch (action.key) {
    case RuleEngine.actionTypes.REMOVE_RULE:
      break;
    default:
    // do nothing
  }
});
let Storage = {
  get length() {
    return localStorage.length
  },
  key(idx) {
    return localStorage.key(idx)
  },
  clear() {
    localStorage.clear()
  },
  setItem(key, value) {
    key = Check.exists(key, "Cannot save with an empty key")
    value = Check.exists(value, "Cannot save empty values. Did you mean to remove?")
    localStorage.setItem(key, JSON.stringify(value))
  },
  getItem(key) {
    let item = localStorage.getItem(key)
    item = item === null ? null : JSON.parse(item)
    return item
  },
  getItems(...keys){
    return keys.map((key) => {
      return Storage.getItem(key)
    })
  },
  hasItem(key){
    return localStorage.getItem(key) !== null
  },
  removeItem(key) {
    let itemJson = localStorage.getItem(key)
    let item = null
    if(itemJson !== null){
      item = JSON.parse(item)
    }
    localStorage.removeItem(key)
    return item;
  },
  childKeys(path){
    let pathLen = path.length
    let childKeys = Storage.childPaths(path).map((childPath) => {
      return childPath.substring(pathLen)
    })
    return childKeys;
  },
  childPaths(path) {
    let childPaths = []
    for (let i = 0; i < localStorage.length; i++) {
      let childPath = localStorage.key(i)
      if (childPath.startsWith(path)) {
        childPaths.push(childPath)
      }
    }
    return childPaths;
  },
  childItems(path){
    let pathLen = path.length
    return Storage.childPaths(path).map((childPath) => {
      return { path:childPath,  key: childPath.substring(pathLen), val: Storage.getItem(childPath) }
    })
  }

}


let clauseRepo = {
  basePath: '/api/rules-engine/clauses/',
  inward: {
    transform(_group, key) {

    },
  },
  outward: {
    transform(clause) {
      return {
        key: clause.$key, val: {
          type: clause.type,
          owningGroup: clause.owningGroup,
          operator: clause.operator,
          value: clause.value
        }
      }
    }
  },
  push(clause) {
    clause.$key = Core.Key.next()
    return clauseRepo.set(clause)
  },
  set(clause) {
    clause = Check.exists(clause, "Cannot save null clause")
    Check.exists(clause.$key, "Cannot save clause with an empty key")
    return new Promise((resolve, reject) => {
      let _xForm = clauseRepo.outward.transform(clause);
      Storage.setItem(clauseRepo.basePath + _xForm.key, _xForm.val)
      resolve(clause)
    })
  },
  getByGroup(groupKey) {
    return new Promise((resolve, reject) => {
      let clausesByGroup = new Map();
      let _clauses = Storage.childItems(clauseRepo.basePath).filter((_stub)=>{
        return _stub.val.owningGroup == groupKey
      }).map((_stub) => {
        return {
          $key: _stub.key,
          owningGroup: _stub.val.owningGroup,
          value: _stub.val.value,
          operator: _stub.val.operator
        }
      })
      resolve(clausesByGroup.get(groupKey) || [])
    })
  },
  get(key) {

  }
}


let ruleRepo = {
  basePath: '/api/rules-engine/rules/',
  inward: {
    transform(_rule, key) {
      let groups = Core.Collections.asArray(_rule.groups || {}, ruleRepo.inward.transformGroup)
      let rule = {
        $key: key,
        name: _rule.name,
        enabled: _rule.enabled,
        groups: []
      }
      return Promise.all(groups).then((groupsAry)=> {
        rule.groups = groupsAry
        return rule
      })
    },
    transformGroup(_group, key) {
      let clausesPromises = []
      let group = {
        $key: key,
        operator: _group.operator,
        clauses: {}
      }
      clausesPromises.push(clauseRepo.getByGroup(key).then((clauses) => {
        group.clauses = clauses
        return group;
      }))
      return Promise.all(clausesPromises).then(() => {
        return group;
      })
    }
  },
  outward: {
    transform(rule) {
      let _rule = {
        name: rule.name,
        enabled: rule.enabled,
        groups: {}
      }
      let clauses = []
      rule.groups.forEach((group) => {
        let _group = {
          operator: group.operator
        }
        _rule.groups[group.$key] = group;
        group.clauses.forEach((clause) => {
          clause["owningGroup"] = group.$key;
          clauses.push(clause)
        })
      })
      return {
        key: rule.$key,
        val: _rule,
        clauses: clauses
      }
    }
  },
  push(rule) {
    if (rule.$key) {
      throw new Error("Cannot push Rule: rule already has a key. Use #set()")
    }
    rule.$key = Core.Key.next()
    return ruleRepo.set(rule)
  },
  set(rule) {
    rule = Check.exists(rule, "Cannot save null rule.")
    Check.notEmpty(rule.name, "Name is required to save a Rule")

    return new Promise((resolve, reject) => {
      let _xForm = ruleRepo.outward.transform(rule)
      let path = ruleRepo.basePath + _xForm.key
      let previous = Storage.getItem(path)
      Storage.setItem(path, _xForm.val)
      let clausePromises = _xForm.clauses.map(clauseRepo.set)
      Promise.all(clausePromises).then((_clausesAry)=> {
        if (previous) {
          RuleEngine.actions.updateRule(rule)
        } else {
          RuleEngine.actions.addRule(rule)
        }
        resolve(rule)
      }, (e) => {throw e;})
    })
  },
  get(ruleKey) {
    let _rule = Storage.getItem(ruleRepo.basePath + ruleKey)
    return ruleRepo.inward.transform(_rule, ruleKey).then((rule) => {
      RuleEngine.actions.addRule(rule)
      return rule;
    });
  },
  remove(ruleKey) {
    return new Promise((resolve, reject) => {
      let path = ruleRepo.basePath + ruleKey
      resolve(Storage.removeItem(path))
    });

  },
  init() {
    let paths = Storage.childKeys(ruleRepo.basePath)
    let rules = paths.map(ruleRepo.get)

    return Promise.all(rules).then((resolvedRules) => {
      return resolvedRules
    })
  }

}

export {ruleRepo};






