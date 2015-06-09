import XDebug from 'debug';
let log = XDebug('RulesEngine.mocks');

import RuleEngine from 'src/rules-engine/RuleEngine.js';


var clauses = {
  'rule01-group01-clause01': {
    type:        'IsAuthenticated',
    name:        'User is authenticated',
    owningGroup: 'rule01-group01',
    value:       true,
    operator:    'AND'
  },
  'rule01-group01-clause02': {
    type:        'VisitorLocation',
    name:        'User is visiting from france',
    owningGroup: 'rule01-group01',
    value:       'FR',
    operator:    'AND'
  },
  'rule01-group02-clause01': {
    type:        'IsAuthenticated',
    name:        'User is authenticated',
    owningGroup: 'rule01-group02',
    value:       true,
    operator:    'AND'
  },
  'rule01-group02-clause02': {
    type:        'VisitorLocation',
    name:        'User is visiting from france',
    owningGroup: 'rule01-group02',
    value:       'CA',
    operator:    'AND'
  }
}

var rules = {
  'rule01': {
    'name':    'Rule 01 - Is from somewhere far away.',
    'enabled': true,
    'groups':  {
      'rule01-group01': {
        'operator': 'AND'
      },
      'rule01-group02': {
        'operator': 'OR'
      }
    }
  },
  'rule02': {
    'name':    'Rule 02 - Is Mobile Device...',
    'enabled': true,
    'groups':  {
      'rule02-group01': {
        'operator': 'AND'
      }
    }
  },
  'rule03': {
    'name':    'Rule 03 - Url is ...',
    'enabled': true,
    'groups':  {
      'rule03-group01': {
        'operator': 'AND'
      },
      'rule03-group02': {
        'operator': 'AND'
      }
    },
  }
};

export let mocks = {
  rules:   rules,
  clauses: clauses,
  init(rootPath='/api'){
    return new Promise((resolve, reject) => {
      let rulePath = rootPath + '/rules-engine/rules/'
      let clausePath = rootPath + '/rules-engine/clauses/'
      Object.keys(rules).forEach((key)=> {
        localStorage.setItem(rulePath + key, JSON.stringify(rules[key]))
      });

      Object.keys(clauses).forEach((key)=> {
        localStorage.setItem(clausePath +  key, JSON.stringify(clauses[key]))
      });

      resolve()
    });


  }
}

