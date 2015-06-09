import * as RuleEngine from 'src/rules-engine/RuleEngine.js';
import {Core, Check} from 'src/dc/index.js';
import {mocks} from 'src/rules-engine/datamocks/rule.mocks.js'

import rest from 'rest';


describe('RuleEngine.api', function () {
  let idx = 0;

  // rules that have random key values - to avoid growing exponentially.
  let rulesToRemove = new Map()

  let mockRule = function () {

    return {
      $key: 'rule0' + (++idx),
      name: 'Rule 0' + idx + ' - mock rule.',
      enabled: true,
      groups: [
        {
          $key: 'rule0' + idx + '-group01',
          operator: 'AND',
          clauses: [
            {
              $key: 'rule0' + idx + '-group01-clause01',
              type: 'IsAuthenticated',
              name: 'User is authenticated',
              owningGroup: 'rule01-group01',
              value: true,
              operator: 'AND'
            },
            {
              $key: 'rule0' + idx + '-group01-clause02',
              type: 'VisitorLocation',
              name: 'User is visiting from france',
              owningGroup: 'rule01-group01',
              value: 'FR',
              operator: 'AND'
            }
          ]
        },
        {
          $key: 'rule0' + idx + '-group02',
          operator: 'OR',
          clauses: [
            {
              $key: 'rule0' + idx + '-group02-clause01',
              type: 'IsAuthenticated',
              name: 'User is authenticated',
              owningGroup: 'rule0' + idx + '-group02',
              value: true,
              operator: 'AND'
            },
            {
              $key: 'rule0' + idx + '-group02-clause02',
              type: 'VisitorLocation',
              name: 'User is visiting from france',
              owningGroup: 'rule0' + idx + '-group02',
              value: 'CA',
              operator: 'AND'
            }
          ]
        }
      ]
    }
  }

  beforeEach(function () {
  })

  beforeAll(function() {
    mocks.init()
  })

  afterAll(function () {
    rulesToRemove.forEach((rule, key) => {
      RuleEngine.api.ruleRepo.remove(key)
    })
  })

  it('load rules already persisted when initialized', function (done) {
    let rulePromise = RuleEngine.api.ruleRepo.init();
    rulePromise.then((rules) => {
      expect(rules).toBeDefined()
      expect(rules.length).toBeGreaterThan(0)
      done()
    })
  })

  it('Can push a rule', function (done) {
    let aRule = mockRule();
    delete aRule['$key'];
    RuleEngine.api.ruleRepo.push(aRule).then((addedRule) => {
      expect(addedRule.$key).toBeDefined()
      rulesToRemove.set(addedRule.$key, addedRule)
      RuleEngine.api.ruleRepo.get(aRule['$key']).then((retrievedRule)=> {
        expect(retrievedRule).toBeDefined()
        expect(retrievedRule.$key).toBe(addedRule.$key)
        expect(addedRule.$key).toBeDefined()
        done()
      })
    })
  })

  it('Throws an error if pushing a rule with a key.', function (done) {
    let aRule = mockRule()
    expect(() => {
      RuleEngine.api.ruleRepo.push(aRule).then((addedRule) => {})
    }).toThrowError("Cannot push Rule: rule already has a key. Use #set()")
    done()
  })

  it("Won't save a rule with no name", function (done) {
    let aRule = mockRule()
    delete aRule['$key']
    delete aRule['name']

    expect(() => {
      RuleEngine.api.ruleRepo.push(aRule).then((addedRule) => { })
    }).toThrow()
    done()
  })

  it("Won't save a clause with no key", function (done) {
    let aRule = mockRule()
    delete aRule.groups[0].clauses[0]['$key']

    expect(() => {
      RuleEngine.api.ruleRepo.push(aRule).then((addedRule) => { })
    }).toThrow()
    done()
  })

});