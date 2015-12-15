import * as Rx from 'rxjs/Rx.KitchenSink'

import {RuleModel, RuleService} from '../../api/rule-engine/Rule';

import {Injector, Provider} from 'angular2/angular2';

import {LocalDataStore} from "../../api/persistence/LocalDataStore";
import {RestDataStore} from "../../api/persistence/RestDataStore";
import {DataStore} from "../../api/persistence/DataStore";


import {ApiRoot} from '../../api/persistence/ApiRoot';
import {UserModel} from "../../api/auth/UserModel";
import {EntityMeta, EntitySnapshot} from '../../api/persistence/EntityBase';
import {I18NCountryProvider} from '../../api/system/locale/I18NCountryProvider'
import {ConditionTypeService} from '../../api/rule-engine/ConditionType';


import {ActionService, ActionModel} from "../../api/rule-engine/Action";
import {ConditionGroupService, ConditionGroupModel} from "../../api/rule-engine/ConditionGroup";
import {ConditionService} from "../../api/rule-engine/Condition";

import {CwChangeEvent} from "../../api/util/CwEvent";
import {ActionTypeService} from "./ActionType";
import {I18nService} from "../system/locale/I18n";
import {ServerSideTypeModel} from "./ServerSideFieldModel";

var injector = Injector.resolveAndCreate([ApiRoot,
  I18nService,
  UserModel,
  RuleService,
  ActionService,
  ActionTypeService,
  ConditionTypeService,
  ConditionService,
  ConditionGroupService,
  new Provider(DataStore, {useClass: RestDataStore})
])


describe('Integration.api.rule-engine.ActionService', function () {

  var ruleService:RuleService
  var ruleOnAddSub:Rx.Subscription<RuleModel>

  var actionService:ActionService
  var ruleUnderTest:RuleModel

  beforeEach(function (done) {
    ruleService = injector.get(RuleService)
    actionService = injector.get(ActionService)
    ruleOnAddSub = ruleService.list().subscribe((rule:RuleModel[]) => {
      ruleUnderTest = rule[0]
      done()
    }, (err) => {
      expect(err).toBeUndefined("error was thrown.")
      done()
    })
    Gen.createRules(ruleService)
  });

  afterEach(function(){
    ruleService.remove(ruleUnderTest);
    ruleUnderTest = null
    ruleOnAddSub.unsubscribe()
  })

  it("Has rules that we can add actions to", function(){
    expect(ruleUnderTest.isPersisted()).toBe(true)
  })

  it("Can add a new Action", function(done){
    var anAction = new ActionModel(null, new ServerSideTypeModel("SetSessionAttributeActionlet"), ruleUnderTest)
    anAction.setParameter("sessionKey", "foo")
    anAction.setParameter("sessionValue", "bar")

    actionService.list(ruleUnderTest).subscribe((action:ActionModel) => {
      //noinspection TypeScriptUnresolvedFunction
      expect(action.isPersisted()).toBe(true, "Action is not persisted!")
      done()
    }, (err) => {
      expect(err).toBeUndefined("error was thrown.")
      done()
    })

    actionService.add(anAction)

  })

  it("Is added to the owning rule's list of actions.", function(done){
    var anAction = new ActionModel(null, new ServerSideTypeModel("SetSessionAttributeActionlet"), ruleUnderTest)
    anAction.setParameter("sessionKey", "foo")
    anAction.setParameter("sessionValue", "bar")

    actionService.list(ruleUnderTest).subscribe((action:ActionModel) => {
      expect(ruleUnderTest.actions[action.key]).toBe(true, "Check the ActionService.list(ruleUnderTest) listener in the RuleService.")
      done()
    }, (err) => {
      expect(err).toBeUndefined("error was thrown.")
      done()
    })
    actionService.add(anAction)
  })


  it("Action being added to the owning rule is persisted to server.", function(done){
    var anAction = new ActionModel(null, new ServerSideTypeModel("SetSessionAttributeActionlet"), ruleUnderTest)
    anAction.setParameter("sessionKey", "foo")
    anAction.setParameter("sessionValue", "bar")

    var firstPass = actionService.list(ruleUnderTest).subscribe((action:ActionModel) => {
      //noinspection TypeScriptUnresolvedFunction
      firstPass.unsubscribe() // don't want to run THIS watcher twice.
      expect(ruleUnderTest.actions[action.key]).toBe(true, "Check the ActionService.list(ruleUnderTest) listener in the RuleService.")
      ruleService.save(ruleUnderTest, () => {
        ruleService.get(ruleUnderTest.key, (rule:RuleModel)=> {
          expect(rule.actions[action.key]).toBe(true)

          /* Now read the Actions off the rule we just got back. Add listener first, then trigger call. */
          let sub = actionService.list(ruleUnderTest).subscribe((action:ActionModel)=>{
            expect(action.getParameter("sessionKey")).toEqual("foo")
            sub.unsubscribe()
            done()
          })
          actionService.list(rule)
        })
      })
    }, (err) => {
      expect(err).toBeUndefined("error was thrown!")
      done()
    })
    actionService.add(anAction)
  })


  it("Will add a new action parameters to an existing action.", function(done){
    var clientAction = new ActionModel(null, new ServerSideTypeModel("SetSessionAttributeActionlet"), ruleUnderTest)
    clientAction.setParameter("sessionKey", "foo")
    clientAction.setParameter("sessionValue", "bar")

    let key = "aParamKey"
    let value = "aParamValue"

    actionService.add(clientAction, ()=>{
      // serverAction is the same instance as resultAction
      expect(clientAction.isPersisted()).toBe(true, "Action is not persisted!")

      clientAction.setParameter(key, value)
      actionService.save(clientAction, ()=>{
        // savedAction is also the same instance as resultAction
        actionService.get(clientAction.owningRule, clientAction.key, (updatedAction)=>{
          // updatedAction and clientAction SHOULD NOT be the same instance object.
          updatedAction['abc123'] = 100
          expect(clientAction['abc123']).toBeUndefined()
          expect(clientAction.getParameter(key)).toBe(value, "ClientAction param value should still be set.")
          expect(updatedAction.getParameterValue(key)).toBe(value, "Action refreshed from server should have the correct param value.")
          expect(Object.keys(updatedAction.parameters).length).toEqual(1, "The old keys should have been removed.")
          done()

        })
      })
    })

  })

  it("Can update action parameter values on existing action.", function(done){
    let param1 = { key: 'sessionKey', v1: 'value1', v2: 'value2'}
    let param2 = { key: 'sessionValue', v1: 'abc123', v2: 'def456'}

    var clientAction = new ActionModel(null, new ServerSideTypeModel("SetSessionAttributeActionlet"), ruleUnderTest)
    clientAction.setParameter(param1.key, param1.v1)
    clientAction.setParameter(param2.key, param2.v1)



    actionService.add(clientAction, ()=>{
      clientAction.setParameter(param1.key, param1.v2)
      actionService.save(clientAction, ()=>{
        actionService.get(clientAction.owningRule, clientAction.key, (updatedAction)=>{
          expect(updatedAction.getParameterValue(param1.key)).toBe(param1.v2, "Action refreshed from server should have the correct param value.")
          expect(updatedAction.getParameterValue(param2.key)).toBe(param2.v1, "Action refreshed from server should have the correct param value.")
          expect(Object.keys(updatedAction.parameters).length).toEqual(2, "The old keys should have been removed.")
          done()

        })
      })
    })

  })


});


class Gen {

  static createRules(ruleService:RuleService){
    console.log('Attempting to create rule.')
    let rule = new RuleModel(null)
    rule.enabled = true
    rule.name = "TestRule-" + new Date().getTime()

    ruleService.add(rule)

  }

}


