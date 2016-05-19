import {
    RuleModel, RuleService, DEFAULT_RULE, IRule, IRuleAction, ActionModel,
    IConditionGroup, ConditionGroupModel
} from '../../api/rule-engine/Rule';
import {ConditionService} from '../../api/rule-engine/Condition';
import {Injector} from '@angular/core';
import {ApiRoot} from '../../api/persistence/ApiRoot';
import {UserModel} from '../../api/auth/UserModel';
import {ActionService} from '../../api/rule-engine/Action';
import {ConditionGroupService} from '../../api/rule-engine/ConditionGroup';
import {I18nService} from "../system/locale/I18n";
import {HTTP_PROVIDERS} from "@angular/http";
import {Observable} from "rxjs/Observable";
import {CwError} from "../system/http-response-util";
import {ServerSideTypeModel} from "./ServerSideFieldModel";


var injector = Injector.resolveAndCreate([
  ApiRoot,
  I18nService,
  UserModel,
  RuleService,
  ActionService,
  ConditionService,
  ConditionGroupService,
  HTTP_PROVIDERS
])

describe('Integration.api.rule-engine.RuleService', function () {

  var ruleService:RuleService
  var ruleActionService:ActionService
  var conditionService:ConditionService
  var conditionGroupService:ConditionGroupService

  var rulesToRemove:string[]


  beforeEach(function () {
    ruleService = injector.get(RuleService)
    ruleActionService = injector.get(ActionService)
    conditionService = injector.get(ConditionService)
    conditionGroupService = injector.get(ConditionGroupService)
    rulesToRemove = []
  });

  afterAll(function (done) {
    Observable.from(rulesToRemove).flatMap((ruleId:string)=>{
      console.log("Removing rule: ", ruleId)
      return ruleService.deleteRule(ruleId);
    }).subscribe(()=>{}, ( e ) => {
      console.error("Error deleting rules after test", e)
    }, (  ) => {
      done()
    })

  })
  it("can create a rule when only name is specified.", function (done) {
    var clientRule:IRule = {
      name: `Test-Rule-${new Date().getTime()}`
    }
    ruleService.createRule(new RuleModel(clientRule)).subscribe((serverRule:RuleModel) => {
          rulesToRemove.push(serverRule.key)
          expect(serverRule.key).toBeDefined("Should have a key")
          expect(serverRule.enabled).toBe(false)
          expect(serverRule.name).toBe(clientRule.name)
          expect(serverRule.fireOn).toBe(DEFAULT_RULE.fireOn)
          expect(serverRule.priority).toBe(DEFAULT_RULE.priority)
          let randomKey = 'abc_' + Math.round(Math.random() * 1000)
          serverRule[randomKey] = "The object provided by the observer is not the same instance as the one added."
          expect(clientRule[randomKey]).not.toBe(serverRule[randomKey])
        },
        (e) => console.error("error", e),
        done
    )
  })

  it("can update a rule's name.", function (done) {
    var clientRule:IRule = {
      name: `Test-Rule-${new Date().getTime()}`
    }
    var name2 = clientRule.name + '-Updated'
    ruleService.createRule(new RuleModel(clientRule)).flatMap((serverRule1:RuleModel)=> {
      rulesToRemove.push(serverRule1.key)
      expect(serverRule1.key).toBeDefined()
      expect(serverRule1.name).toBe(clientRule.name)
      serverRule1.name = name2
      return ruleService.updateRule(serverRule1.key, serverRule1)
    }).subscribe((serverRule2:IRule) => {
          expect(serverRule2.id).toBeDefined()
          expect(serverRule2.name).toBe(name2)
          console.log("yay", serverRule2)
        },
        (e) => console.error("error", e),
        done
    )
  })

  it("can remove a rule.", function (done) {
    var clientRule:IRule = {
      name: `Test-Rule-${new Date().getTime()}`
    }
    ruleService.createRule(new RuleModel(clientRule))
        .flatMap((serverRule1:RuleModel)=> ruleService.deleteRule(serverRule1.key))
        .subscribe((result:any) => {
              expect(result.success).toBeTruthy("Result should be 'success:true'")
            },
            (e) => console.error("error", e),
            done
        )
  })

  it("can load a rule.", function (done) {
    var clientRule:IRule = {
      name: `Test-Rule-${new Date().getTime()}`
    }
    var id
    ruleService.createRule(new RuleModel(clientRule))
        .flatMap((serverRule:RuleModel)=> {
          id = serverRule.key;
          rulesToRemove.push(serverRule.key)
          return ruleService.loadRule(serverRule.key)
        })
        .subscribe((result:RuleModel) => {

              expect(result.key).toBe(id)
              expect(result.name).toBe(clientRule.name)
            },
            (e) => console.error("error", e),
            done
        )
  })


  it("can load all rules.", function (done) {
    var clientRule1:IRule = {
      name: `Test-Rule-${new Date().getTime()}`
    }
    var clientRule2:IRule = {
      name: `Test-Rule-${new Date().getTime()}-2`
    }
    var id
    var ourSavedRules
    Observable.merge(ruleService.createRule(new RuleModel(clientRule1)), ruleService.createRule(new RuleModel(clientRule2)))
        .bufferCount(2, 0)
        .flatMap((ourRules:RuleModel[]) => {
          ourSavedRules = ourRules
          ourRules.forEach((r)=>{
            rulesToRemove.push(r.key)
          })
          return ruleService.loadRules()
        })
        .subscribe((rules:RuleModel[]) => {
              let onlyTestRules = rules.filter((rule:RuleModel) => (rule.key == ourSavedRules[0].key || rule.key == ourSavedRules[1].key))
              console.log("alksdjf", onlyTestRules)
              expect(onlyTestRules.length).toBe(2, "Both created rules should be in the loaded rules.")
              done()
            },
            (e) => console.error("error", e),
            done
        )
  })

  it("Can create a rule for each fireOn type.", function (done) {
    let fireOns = ["EVERY_PAGE",
      "ONCE_PER_VISIT",
      "ONCE_PER_VISITOR",
      "EVERY_REQUEST"]

    Observable.from(fireOns).subscribe((fireOn:string)=> {
      let name = "Test-create_fireOn" + new Date().getTime()
      ruleService.createRule(new RuleModel({
        name: name,
        enabled: true,
        fireOn: fireOn,
        priority: Math.floor(Math.random() * 100)
      })).subscribe((serverRule:RuleModel) => {
        rulesToRemove.push(serverRule.key)
        expect(serverRule.key).toBeDefined()
        expect(serverRule.enabled).toBe(true)
        expect(serverRule.name).toBe(name)
        expect(serverRule.fireOn).toBe(fireOn)
      }, (e)=> {
        expect(true).toBe(false, "Should not throw an error.")
        done()
      }, () => {
        done()
      })
    })
  })

  it("Provides an error if no name specified.", function (done) {
    let name = ""
    ruleService.createRule(new RuleModel({
      name: name,
      enabled: true,
      fireOn: "EVERY_REQUEST",
      priority: Math.floor(Math.random() * 100)
    })).subscribe((serverRule:RuleModel) => {
      expect(true).toBe(false, "Rule should require a name.")
      done()
    }, (e:CwError) => {
      expect(e.message).toContain("'name' may not be empty")
      done()
    })
  })

  it("Provides an error if no fireOn specified.", function (done) {
    let name = `ErrorIfNoFireOn-${new Date().getTime()}`
    ruleService.createRule(new RuleModel({
      name: name,
      enabled: true,
      fireOn: "",
      priority: Math.floor(Math.random() * 100)
    })).subscribe((serverRule:RuleModel) => {
      expect(true).toBe(false, "Rule should require a fireOn value.")
      done()
    }, (e:CwError) => {
      expect(e.message).toContain("'fireOn' {javax.validation.constraints.FireOn.message}")
      done()
    })
  })

  it("Can delete a persisted rule.", function (done) {
    let name = "CanDelete-" + new Date().getTime()
    ruleService.createRule(new RuleModel({
      name: name,
      enabled: true,
      fireOn: "EVERY_REQUEST",
      priority: Math.floor(Math.random() * 100)
    })).subscribe((serverRule:RuleModel) => {
      expect(serverRule.key).toBeDefined()
      ruleService.deleteRule(serverRule.key).subscribe((result:any)=> {
            expect(result.success).toBe(true)
            done()
          },
          (e) => {
            fail(e)
            done()
          },
          done)
    })
  })

  xit("can create an action.", function (done) {
    var clientRule:IRule = {
      name: `Test-Rule-${new Date().getTime()}`
    }
    var clientRuleAction:IRuleAction = {
      type: 'SetResponseHeaderActionlet',
      priority: 10,
      parameters: {
        'headerKey': {key: 'headerKey', value: 'Hi', priority: 0},
        'headerValue': {key: 'headerValue', value: 'Bob', priority: 1},
      }
    }
    var serverRule:RuleModel
    ruleService.createRule(new RuleModel(clientRule)).flatMap((rule:RuleModel)=> {
      serverRule = rule
      rulesToRemove.push(serverRule.key)
      return ruleActionService.createRuleAction(serverRule.key, new ActionModel(null, null, clientRuleAction.priority))
    }).subscribe((action:ActionModel) => {
          expect(action).toBeDefined()
          expect(action.key).toBeDefined()
          expect(serverRule.ruleActions[action.key]).toBeTruthy()
          done()
        },
        (e) => {
          fail(e)
          done()
        },
        done
    )
  })

  xit("can update an action.", function (done) {
    var clientRule:IRule = {
      name: `Test-Rule-${new Date().getTime()}`
    }
    var clientRuleAction:IRuleAction = {
      type: 'SetResponseHeaderActionlet',
      priority: 10,
      parameters: {
        'headerKey': {key: 'headerKey', value: 'Hi', priority: 0},
        'headerValue': {key: 'headerValue', value: 'Bob', priority: 1},
      }
    }
    var serverRule:RuleModel
    ruleService.createRule(new RuleModel(clientRule)).flatMap((rule:RuleModel)=> {
      serverRule = rule
      rulesToRemove.push(serverRule.key)
      return ruleActionService.createRuleAction(serverRule.key, new ActionModel(null, new ServerSideTypeModel(clientRuleAction.type, '', clientRuleAction.parameters), clientRuleAction.priority))
    }).flatMap((action:ActionModel)=> {
      return ruleActionService.updateRuleAction(serverRule.key, action)
    }).subscribe((action:ActionModel) => {
          expect(action.key).toBeDefined("Action should be provided, with key applied.")
          expect(action.priority).toBe(clientRuleAction.priority, "Priority should have been saved.")
          done()
        },
        (e) => {
          fail(e)
          done()
        },
        done
    )
  })


  it("can create a condition group.", function (done) {
    var clientRule:IRule = {
      name: `Test-Rule-${new Date().getTime()}`
    }
    var conditionGroup:IConditionGroup = {
      priority: 10,
      operator: 'AND',
    }
    var serverRule:RuleModel
    ruleService.createRule(new RuleModel(clientRule)).flatMap((rule:RuleModel)=> {
      serverRule = rule
      rulesToRemove.push(serverRule.key)
      return conditionGroupService.createConditionGroup(serverRule.key, new ConditionGroupModel(conditionGroup))
    }).subscribe((serverGroup:ConditionGroupModel) => {
          expect(serverGroup).toBeDefined("Should create and provide a condition group.")
          expect(serverGroup.key).toBeDefined("Group should have been assigned an ID by the PUT response.")
        },
        (e) => {
          fail(e)
          done()
        },
        done
    )
  })

  it("adds the conditionGroup to the owning rule.", function (done) {
    var aConditionGroup = new ConditionGroupModel({operator: 'OR', priority: 99})
    var clientRule:IRule = {
      name: `Test-Rule-${new Date().getTime()}`
    }
    var conditionGroup:IConditionGroup = {
      priority: 10,
      operator: 'AND',
    }
    var serverRule:RuleModel
    ruleService.createRule(new RuleModel(clientRule)).flatMap((rule:RuleModel)=> {
          serverRule = rule
          rulesToRemove.push(serverRule.key)
          return conditionGroupService.createConditionGroup(serverRule.key, aConditionGroup)
        })
        .subscribe((conditionGroup:ConditionGroupModel) => {
          ruleService.loadRule(serverRule.key).subscribe((rule:RuleModel)=> {
            expect(rule.conditionGroups[conditionGroup.key]).toBeDefined("Well that's odd")
            expect(rule.conditionGroups[conditionGroup.key].operator).toEqual("OR")
            /* Now read the ConditionGroups off the rule we just got back. Add listener first, then trigger call. */
            conditionGroupService.all(rule.key, Object.keys(rule.conditionGroups)).subscribe((conditionGroups:ConditionGroupModel[])=> {
              let condGroup = conditionGroups[0]
              expect(conditionGroup.operator).toEqual("OR")
              expect(conditionGroup.priority).toEqual(99)
              done()
            })
          })
        })
  })


  it("Can list condition types, and they are all persisted and valid.", function (done) {
    let count = 0;
    let subscription = ruleService.getConditionTypes().subscribe((types:ServerSideTypeModel[]) => {
      types.forEach((type:ServerSideTypeModel)=> {
        expect(type.key).toBeDefined("Condition types are readonly and should always be persisted.")
        expect(type.isValid()).toBe(true, "Condition types are readonly and should always be valid.")
      })
      done()
    }, (err) => {
      expect(err).toBeUndefined("error was thrown creating Rule.")
      done()
    }, ()=> {
    })
  })

  it("There are (n) active condition types.", function (done) {
    let implementedConditionTypeCount = 19;
    ruleService.getConditionTypes().subscribe((types:ServerSideTypeModel[])=> {
      expect(types.length).toEqual(implementedConditionTypeCount, `We have ${implementedConditionTypeCount} implemented condition types.`)
      done()
    })
  })

  it("There are (n) active rule action types.", function (done) {
    let implementedActionTypeCount = 6;
    ruleService.getRuleActionTypes().subscribe((types:ServerSideTypeModel[])=> {
      expect(types.length).toEqual(implementedActionTypeCount, `We have ${implementedActionTypeCount} implemented rule action types.`)
      done()
    })
  })
});
