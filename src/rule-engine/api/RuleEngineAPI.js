import XDebug from 'debug';
let log = XDebug('RuleEngine.api');


import {Core, Check} from '../../coreweb/index.js';

import {actions, actionTypes} from '../actions/RuleEngineActionCreators.js';
import {Rule, RuleGroup} from  './RuleEngineTypes.js';

import {RestDataStore as Storage, EntityMeta} from '../../entity-forge/index.js'

let defaultSiteKey = "48190c8c-42c4-46af-8d1a-0cd5db894797"
let SERVER_CREATES_KEYS = true
export let ruleRepo = {};
ruleRepo = {
  basePath: 'api/v1/sites/{{siteKey}}/rules/{{ruleKey}}',
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
        priority: _group.priority
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
        priority: rule.priority || 1,
        fireOn: rule.fireOn,
        shortCircuit: rule.shortCircuit,
        groups: {},
        actions: {}
      }
      return {
        key: rule.$key,
        val: _rule
      }
    }
  },
  getPath(siteKey, ruleKey) {
    let path = ruleRepo.basePath.replace('{{siteKey}}', siteKey).replace('{{ruleKey}}', ruleKey)
    if (path.endsWith('/')) {
      path = path.substring(0, path.length - 1)
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
  set(rule, isNew = false) {
    rule = Check.exists(rule, "Cannot save null rule.")
    Check.notEmpty(rule.name, "Name is required to save a Rule")
    /* @todo ggranum: SiteKey should be defined programatically, however... it's not yet. */
    return new Promise((resolve, reject) => {
      let _xForm = ruleRepo.outward.transform(rule)
      let path
      if (isNew === true && SERVER_CREATES_KEYS) {
        path = ruleRepo.getPath(defaultSiteKey, '')
      } else {
        path = ruleRepo.getPath(defaultSiteKey, _xForm.key)
      }
      Storage.setItem(path, _xForm.val, isNew === true && SERVER_CREATES_KEYS)
      if (isNew) {
        actions.addRule(rule)
      } else {
        actions.updateRule(rule)
      }
      resolve(rule)
    })
  },
  get(ruleKey) {
    return Storage.getItem(ruleRepo.getPath(defaultSiteKey, ruleKey)).then((_rule)=> {
      return ruleRepo.inward.transform(_rule, ruleKey).then((rule) => {
        actions.addRule(rule)
        return rule;
      });
    })
  },
  remove(ruleKey) {
    let path = ruleRepo.getPath(defaultSiteKey, ruleKey)
    var item = Storage.removeItem(path);
    actions.update()
    return item
  },
  list() {
    return Storage.childKeys(ruleRepo.getPath(defaultSiteKey, '') + '/').then((paths) => {
      let rules = paths.map(ruleRepo.get)
      return Promise.all(rules).then((resolvedRules) => {
        return resolvedRules
      })
    })
  }
}

export let ruleGroupRepo = {};
ruleGroupRepo = {
  basePath: 'api/v1/sites/{{siteKey}}/rules/{{ruleKey}}/conditiongroups/{{groupKey}}',
  outward: {
    transform(group) {
      let _group = {
        priority: group.priority,
        operator: group.operator || "OR"
      }
      return {
        key: group.$key,
        val: _group
      }
    }
  },
  getPath(siteKey, ruleKey, groupKey) {
    let path = ruleGroupRepo.basePath
        .replace('{{siteKey}}', siteKey)
        .replace('{{ruleKey}}', ruleKey)
        .replace('{{groupKey}}', groupKey)
    if (path.endsWith('/')) {
      path = path.substring(0, path.length - 1)
    }
    return path;
  },
  push(group){
    if (group.$key) {
      throw new Error("Cannot push Group: group already has a key. Use #set()")
    }
    group.$key = Core.Key.next()
    return ruleGroupRepo.set(group, true)
  },
  set(group, isNew) {
    return new Promise((resolve, reject) => {
      let _xForm = ruleGroupRepo.outward.transform(group)
      let path
      if (isNew === true && SERVER_CREATES_KEYS) {
        path = ruleGroupRepo.getPath(defaultSiteKey, group.ruleKey, '')
      } else {
        path = ruleGroupRepo.getPath(defaultSiteKey, group.ruleKey, _xForm.key)
      }
      Storage.setItem(path, _xForm.val, isNew === true && SERVER_CREATES_KEYS).then((result) => {
        resolve(result)
      }).catch((e) => {
        reject(e)
      })

    })
  }
}


export let clauseRepo = {
  basePath: 'api/v1/rules/clauses/',
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
      actions.update()
    })
  },
  getByGroup(groupKey) {
    return new Promise((resolve, reject) => {
      let clausesByGroup = new Map();
      let _clauses = Storage.childItems(clauseRepo.basePath).filter((_stub)=> {
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


export let RuleEngineAPI = {

  rules: {
    list: function () {
      //return ruleRepo.list()
      let foo = new EntityMeta('/api/v1/sites/48190c8c-42c4-46af-8d1a-0cd5db894797/rules')
      return foo.once('value', (result) => {
        result.forEach((ruleSnap) => {
          ruleRepo.inward.transform(ruleSnap.val(), ruleSnap.key()).then((rule) => {
            actions.addRule(rule)
          })
        })
      })
    },
    self: function (key) {
    },
    remove: function (key) {
    },
    update: function (rule) {
    },
    add: function (rule) {
    },
    conditionGroups: {
      list: function (rule) {
      },
      self: function (rule, groupKey) {
      },
      remove: function (rule, groupKey) {
      },
      update: function (rule, group) {
      },
      add: function (ruleGroup, group) {
      }
    },
    conditions: {
      list: function (ruleGroup) {
      },
      self: function (rule, conditionKey) {
      },
      remove: function (rule, conditionKey) {
      },
      update: function (rule, condition) {
      },
      add: function (ruleGroup, condition) {
      }
    },
    ruleActions: {
      list: function (rule) {
      },
      self: function (rule, actionKey) {
      },
      remove: function (rule, actionKey) {
      },
      update: function (rule, action) {
      },
      add: function (rule, action) {
      }
    }
  }
}









