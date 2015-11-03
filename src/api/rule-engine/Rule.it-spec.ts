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


describe('Integration.api.rule-engine.RuleService', function () {

  var ruleService:RuleService

  var rulesToRemove:Array<RuleModel>


  beforeEach(function () {
    ruleService = injector.get(RuleService)
    rulesToRemove = []
  });

  afterEach(function(){
    rulesToRemove.forEach((rule) => {
      ruleService.remove(rule);
    })
  })

  it("Can create a simple rule.", function(done){
    var clientRule:RuleModel
    clientRule = new RuleModel()
    clientRule .enabled = true
    clientRule .name = "TestRule-" + new Date().getTime()
    let ruleOnAddSub = ruleService.onAdd.subscribe((serverRule:RuleModel) => {
      rulesToRemove.push(serverRule)
      expect(serverRule.isPersisted()).toBe(true)
      expect(serverRule.enabled).toBe(true)
      expect(serverRule.name).toBe(clientRule.name)
      let randomKey = 'abc_' + Math.round(Math.random()*1000)
      serverRule[randomKey] = "The object provided by the observer is the same instance as the one added."
      expect(clientRule[randomKey]).toBe(serverRule[randomKey])
      done()
    }, (err) => {
      expect(err).toBeUndefined("error was thrown creating Rule.")
      done()
    })

    ruleService.add(clientRule) // will trigger callback to subscription.

  })

});
