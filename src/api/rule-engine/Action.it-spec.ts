/// <reference path="../../../jspm_packages/npm/angular2@2.0.0-alpha.44/typings/jasmine/jasmine.d.ts" />

import {RuleModel, RuleService} from 'api/rule-engine/Rule';
import {ActionModel} from 'api/rule-engine/Action';

import {Injector, Provider} from 'angular2/angular2';

import {DataStore} from 'api/persistence/DataStore'
import {LocalDataStore} from "api/persistence/LocalDataStore";
import {RestDataStore} from "api/persistence/RestDataStore";



import {ApiRoot} from 'api/persistence/ApiRoot';
import {UserModel} from "api/auth/UserModel";
import {EntityMeta, EntitySnapshot} from 'api/persistence/EntityBase';
import {ActionTypesProvider} from 'api/rule-engine/ActionType';
import {ConditionTypesProvider} from 'api/rule-engine/ConditionTypes';
import {I18NCountryProvider} from 'api/system/locale/I18NCountryProvider'


import {RuleService, RuleModel} from "api/rule-engine/Rule";
import {ActionService, ActionModel} from "api/rule-engine/Action";
import {ConditionGroupService, ConditionGroupModel} from "api/rule-engine/ConditionGroup";
import {ConditionService} from "api/rule-engine/Condition";

import {CwChangeEvent} from "api/util/CwEvent";
import {RestDataStore} from "api/persistence/RestDataStore";
import {DataStore} from "api/persistence/DataStore";
import {ActionTypeModel} from "./ActionType";


var injector = Injector.resolveAndCreate([ApiRoot,
  //ActionTypesProvider,
  //ConditionTypesProvider,
  UserModel,
  //I18NCountryProvider,
  RuleService,
  ActionService,
  ConditionService,
  ConditionGroupService,
  new Provider(DataStore, {useClass: RestDataStore})
])


describe('Integration.api.rule-engine.ActionService', function () {

  var ruleService:RuleService
  var ruleOnAddSub

  var actionService:ActionService
  var ruleUnderTest:RuleModel

  beforeEach(function (done) {
    ruleService = injector.get(RuleService)
    actionService = injector.get(ActionService)
    ruleOnAddSub = ruleService.onAdd.subscribe((rule:RuleModel) => {
      ruleUnderTest = rule
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
    var anAction = new ActionModel()
    anAction.actionType = new ActionTypeModel("SetSessionAttributeActionlet")
    anAction.owningRule = ruleUnderTest
    anAction.setParameter("sessionKey", "foo")
    anAction.setParameter("sessionValue", "bar")

    var subscriber = actionService.onAdd.subscribe((action:ActionModel) => {
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
    var anAction = new ActionModel()
    anAction.actionType = new ActionTypeModel("SetSessionAttributeActionlet")
    anAction.owningRule = ruleUnderTest
    anAction.setParameter("sessionKey", "foo")
    anAction.setParameter("sessionValue", "bar")

    actionService.onAdd.subscribe((action:ActionModel) => {
      expect(ruleUnderTest.actions[action.key]).toBe(true, "Check the ActionService.onAdd listener in the RuleService.")
      done()
    }, (err) => {
      expect(err).toBeUndefined("error was thrown.")
      done()
    })
    actionService.add(anAction)
  })


  it("Action being added to the owning rule is persisted to server.", function(done){
    var anAction = new ActionModel()
    anAction.actionType = new ActionTypeModel("SetSessionAttributeActionlet")
    anAction.owningRule = ruleUnderTest
    anAction.setParameter("sessionKey", "foo")
    anAction.setParameter("sessionValue", "bar")

    var firstPass = actionService.onAdd.subscribe((action:ActionModel) => {
      //noinspection TypeScriptUnresolvedFunction
      firstPass.unsubscribe() // don't want to run THIS watcher twice.
      expect(ruleUnderTest.actions[action.key]).toBe(true, "Check the ActionService.onAdd listener in the RuleService.")
      ruleService.save(ruleUnderTest, () => {
        ruleService.get(ruleUnderTest.key, (rule:RuleModel)=> {
          expect(rule.actions[action.key]).toBe(true)

          /* Now read the Actions off the rule we just got back. Add listener first, then trigger call. */
          actionService.onAdd.subscribe((action:ActionModel)=>{
            expect(action.getParameter("sessionKey")).toEqual("foo")
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


});


class Gen {

  static createRules(ruleService:RuleService){
    console.log('Attempting to create rule.')
    let rule = new RuleModel()
    rule.enabled = true
    rule.name = "TestRule-" + new Date().getTime()

    ruleService.add(rule)

  }

}


