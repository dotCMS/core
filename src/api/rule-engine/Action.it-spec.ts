import * as Rx from 'rxjs/Rx'

import {RuleModel, RuleService} from '../../api/rule-engine/Rule';
import {Injector, Provider} from 'angular2/core';
import {RestDataStore} from "../../api/persistence/RestDataStore";
import {DataStore} from "../../api/persistence/DataStore";
import {ApiRoot} from '../../api/persistence/ApiRoot';
import {UserModel} from "../../api/auth/UserModel";
import {ConditionTypeService} from '../../api/rule-engine/ConditionType';
import {ActionService, ActionModel} from "../../api/rule-engine/Action";
import {ConditionGroupService} from "../../api/rule-engine/ConditionGroup";
import {ConditionService} from "../../api/rule-engine/Condition";
import {ActionTypeService} from "./ActionType";
import {I18nService} from "../system/locale/I18n";
import {HTTP_PROVIDERS} from "angular2/http";

var injector = Injector.resolveAndCreate([ApiRoot,
  I18nService,
  UserModel,
  RuleService,
  ActionService,
  ActionTypeService,
  ConditionTypeService,
  ConditionService,
  ConditionGroupService,
  HTTP_PROVIDERS,
  new Provider(DataStore, {useClass: RestDataStore})
])


describe('Integration.api.rule-engine.ActionService', function () {

  var ruleService:RuleService
  var ruleOnAddSub:Rx.Subscription<RuleModel>

  var actionService:ActionService
  var ruleUnderTest:RuleModel
  var actionTypeService:ActionTypeService
  var actionTypes = {}
  var setSessionActionlet

  beforeAll(function (done) {
    ruleService = injector.get(RuleService)
    actionTypeService = injector.get(ActionTypeService)
    actionService = injector.get(ActionService)
    actionTypeService.list().subscribe((typesAry)=> {
      typesAry.forEach((item) => actionTypes[item.key] = item)
      setSessionActionlet = actionTypes["SetSessionAttributeActionlet"]
      done()
    })


  })

  beforeEach(function (done) {
    Gen.createRules(ruleService)
    ruleOnAddSub = ruleService.list().subscribe((rule:RuleModel[]) => {
      ruleUnderTest = rule[0]
      done()
    }, (err) => {
      expect(err).toBeUndefined("error was thrown.")
      done()
    })
  });

  afterEach(function (done) {
    ruleService.remove(ruleUnderTest, ()=> {
      ruleUnderTest = null
      ruleOnAddSub.unsubscribe()
      done()
    });
  })

  it("Has rules that we can add actions to", function () {
    expect(ruleUnderTest.isPersisted()).toBe(true)
  })

  it("Can add a new Action", function (done) {
    console.log("can add new", setSessionActionlet)
    var anAction = new ActionModel(null, setSessionActionlet, ruleUnderTest)
    anAction.setParameter("sessionKey", "foo")
    anAction.setParameter("sessionValue", "bar")

    actionService.add(anAction, (action:ActionModel) => {
      expect(action.isPersisted()).toBe(true, "Action is not persisted!")
      done()
    })


  })

  it("Action being added to the owning rule is persisted to server.", function (done) {
    var anAction = new ActionModel(null, setSessionActionlet, ruleUnderTest)
    anAction.setParameter("sessionKey", "foo")
    anAction.setParameter("sessionValue", "bar")

    actionService.add(anAction, (action:ActionModel) => {
      ruleService.save(ruleUnderTest, () => {
        ruleService.get(ruleUnderTest.key, (rule:RuleModel)=> {
          expect(rule.actions[action.key]).toBe(true)
          let sub = actionService.list(rule).subscribe((actions:ActionModel[])=> {
            console.log("Rule: ", rule)
            console.log("Rehydrated Rule: ", rule)
            console.log("Rehydrated Actions: ", actions)
            let rehydratedAction = actions[0]
            expect(rehydratedAction.getParameterValue("sessionKey")).toEqual("foo")
            sub.unsubscribe()
            done()
          }, (e)=> {
            console.log(e)
            expect(e).toBeUndefined("Test Failed")
          })
        })
      })
    })
  })

  it("Will add a new action parameters to an existing action.", function (done) {
    var clientAction = new ActionModel(null, setSessionActionlet, ruleUnderTest)
    clientAction.setParameter("sessionKey", "foo")
    clientAction.setParameter("sessionValue", "bar")

    let key = "sessionKey"
    let value = "aParamValue"

    actionService.add(clientAction, ()=> {
      expect(clientAction.isPersisted()).toBe(true, "Action is not persisted!")

      clientAction.setParameter(key, value)
      actionService.save(clientAction, ()=> {
        // savedAction is also the same instance as resultAction
        actionService.get(clientAction.owningRule, clientAction.key, (updatedAction)=> {
          // updatedAction and clientAction SHOULD NOT be the same instance object.
          updatedAction['abc123'] = 100
          expect(clientAction['abc123']).toBeUndefined()
          expect(clientAction.getParameterValue(key)).toBe(value)
          expect(updatedAction.getParameterValue(key)).toBe(value)
          done()
        })
      })
    })
  })

  it("Can update action parameter values on existing action.", function (done) {
    let param1 = {key: 'sessionKey', v1: 'value1', v2: 'value2'}
    let param2 = {key: 'sessionValue', v1: 'abc123', v2: 'def456'}

    var clientAction = new ActionModel(null, setSessionActionlet, ruleUnderTest)
    clientAction.setParameter(param1.key, param1.v1)
    clientAction.setParameter(param2.key, param2.v1)

    actionService.add(clientAction, ()=> {
      clientAction.setParameter(param1.key, param1.v2)
      actionService.save(clientAction, ()=> {
        actionService.get(clientAction.owningRule, clientAction.key, (updatedAction)=> {
          expect(updatedAction.getParameterValue(param1.key)).toBe(param1.v2)
          expect(updatedAction.getParameterValue(param2.key)).toBe(param2.v1)
          done()
        })
      })
    })

  })
});

class Gen {
  static createRules(ruleService:RuleService) {
    console.log('Attempting to create rule.')
    let rule = new RuleModel(null)
    rule.enabled = true
    rule.name = "TestRule-" + new Date().getTime()

    return ruleService.add(rule)
  }
}