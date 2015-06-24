import XDebug from 'debug';
let log = XDebug('RulesEngine.api');

import {mocks} from '../datamocks/rule.mocks.js';

import {AppDispatcher} from '../../rules-engine/dispatcher/AppDispatcher.js';
import {Core} from '../../coreweb/api/Core.js';
import {Check} from '../../coreweb/api/Check.js';

import * as RuleEngine from '../actions/RuleEngineActionCreators.js';
import * as RuleTypes from  './RuleEngineTypes.js';

//import {LocalDataStore as Storage} from '../../coreweb/util/LocalDataStore.js'
import {RestDataStore as Storage} from '../../coreweb/util/RestDataStore.js'

let dispatchToken = AppDispatcher.register((action) => {
  switch (action.key) {
    case RuleEngine.actionTypes.REMOVE_RULE:
      break;
    default:
    // do nothing
  }
});


let clauseRepo = {
  basePath: '/api/v1/rules/clauses/',
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

let defaultSiteKey = "48190c8c-42c4-46af-8d1a-0cd5db894797"
let SERVER_CREATES_KEYS = true
let ruleRepo = {};
ruleRepo = {
  basePath: '/sites/{{siteKey}}/rules/{{ruleKey}}',
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
        priority: rule.priority,
        fireOn: rule.fireOn,
        shortCircuit: rule.shortCircuit,
        groups: {},
        actions: {}
      }
      let clauses = []
      Object.keys(rule.groups).forEach((groupId) => {
        let group = rule.groups[groupId]
        _rule.groups[group.$key] = group
        group.clauses.forEach((clause) => {
          clause["owningGroup"] = group.$key
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
  getPath(siteKey, ruleKey) {
    let path = ruleRepo.basePath.replace('{{siteKey}}', siteKey).replace('{{ruleKey}}', ruleKey)
    if(path.endsWith('/')){
      path = path.substring(0, path.length-1)
    }
    return path;
  },
  push(rule) {
    if (rule.$key) {
      throw new Error("Cannot push Rule: rule already has a key. Use #set()")
    }
    rule.$key = Core.Key.next()
    return ruleRepo.set(rule, true)
  },
  set(rule, isNew) {
    rule = Check.exists(rule, "Cannot save null rule.")
    Check.notEmpty(rule.name, "Name is required to save a Rule")
    /* @todo ggranum: SiteKey should be defined programatically, however... it's not yet. */
    return new Promise((resolve, reject) => {
      let _xForm = ruleRepo.outward.transform(rule)
      let path
      if(isNew === true && SERVER_CREATES_KEYS){
        path = ruleRepo.getPath( defaultSiteKey, '')
      } else {
        path = ruleRepo.getPath( defaultSiteKey, _xForm.key)
      }
      Storage.setItem(path, _xForm.val, isNew === true && SERVER_CREATES_KEYS)
      let clausePromises = _xForm.clauses.map(clauseRepo.set)
      Promise.all(clausePromises).then((_clausesAry)=> {
        if (isNew) {
          RuleEngine.actions.addRule(rule)
        } else {
          RuleEngine.actions.updateRule(rule)
        }
        resolve(rule)
      }, (e) => {
        throw e;
      })

    })
  },
  get(ruleKey) {
     return Storage.getItem(ruleRepo.getPath(defaultSiteKey, ruleKey)).then((_rule)=> {
       return ruleRepo.inward.transform(_rule, ruleKey).then((rule) => {
         RuleEngine.actions.addRule(rule)
         return rule;
       });
     })
  },
  remove(ruleKey) {
    return new Promise((resolve, reject) => {
      let path = ruleRepo.getPath(defaultSiteKey, ruleKey)
      resolve(Storage.removeItem(path))
    });

  },
  init() {
    return Storage.childKeys(ruleRepo.getPath(defaultSiteKey, '') + '/').then((paths) => {
      let rules = paths.map(ruleRepo.get)
      return Promise.all(rules).then((resolvedRules) => {
        return resolvedRules
      })
    })
  }

}

export {ruleRepo};






