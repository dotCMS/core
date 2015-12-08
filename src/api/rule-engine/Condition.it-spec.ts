
import {RuleModel, RuleService} from '../../api/rule-engine/Rule';
import {ConditionService, ConditionModel} from '../../api/rule-engine/Condition';

import {Injector, Provider} from 'angular2/angular2';

import {DataStore} from '../../api/persistence/DataStore'
import {LocalDataStore} from '../../api/persistence/LocalDataStore';
import {RestDataStore} from '../../api/persistence/RestDataStore';

import {ApiRoot} from '../../api/persistence/ApiRoot';
import {UserModel} from '../../api/auth/UserModel';
import {EntityMeta, EntitySnapshot} from '../../api/persistence/EntityBase';
import {I18NCountryProvider} from '../../api/system/locale/I18NCountryProvider'
import {ConditionTypeService, ConditionTypeModel} from '../../api/rule-engine/ConditionType';


import {ActionService} from '../../api/rule-engine/Action';
import {ConditionGroupService, ConditionGroupModel} from '../../api/rule-engine/ConditionGroup';
import {CwChangeEvent} from '../../api/util/CwEvent';
import {ActionTypeService} from "./ActionType";
import {I18nService} from "../system/locale/I18n";

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


describe('Integration.api.rule-engine.ConditionService', function () {

  var ruleService:RuleService
  var ruleOnAddSub

  var conditionGroupService:ConditionGroupService
  var conditionService:ConditionService
  var ruleUnderTest:RuleModel
  var groupUnderTest:ConditionGroupModel

  beforeEach(function (done) {
    ruleUnderTest = null;
    groupUnderTest = null;
    ruleService = injector.get(RuleService)
    conditionGroupService = injector.get(ConditionGroupService)
    conditionService = injector.get(ConditionService)
    ruleOnAddSub = ruleService.onAdd.subscribe((rule:RuleModel) => {
      if(!ruleUnderTest) {
        ruleUnderTest = rule
        groupUnderTest = new ConditionGroupModel()
        groupUnderTest.operator = "OR"
        groupUnderTest.owningRule = ruleUnderTest
        conditionGroupService.add(groupUnderTest, done)
      }
    }, (err) => {
      expect(err).toBeUndefined("error was thrown.")
      done()
    })
    Tools.createRule(ruleService)

  });

  afterEach(function(){
    ruleService.remove(ruleUnderTest);
    ruleUnderTest = null
    ruleOnAddSub.unsubscribe()
  })

  it("Has a rule and group that we can add conditions to", function(){
    expect(ruleUnderTest.isPersisted()).toBe(true)
    expect(groupUnderTest.isPersisted()).toBe(true)
  })

  it("Can add a new Condition", function(done){
    var aCondition = new ConditionModel()
    aCondition.name = "pointless_name-" + new Date().getTime()
    aCondition.conditionType = new ConditionTypeModel("UsersCountryConditionlet")
    aCondition.owningGroup = groupUnderTest
    aCondition.setParameter("sessionKey", "foo")
    aCondition.setParameter("sessionValue", "bar")

    var sub = conditionService.onAdd.subscribe((condition:ConditionModel) => {
      sub.unsubscribe()
      //noinspection TypeScriptUnresolvedFunction
      expect(condition.isPersisted()).toBe(true, "Condition is not persisted!")
      done()
    }, (err) => {
      expect(err).toBeUndefined("error was thrown.")
      done()
    })

    conditionService.add(aCondition)

  })

  it("Is added to the owning rule's list of conditions.", function(done){
    var aCondition = new ConditionModel()
    aCondition.name = "pointless_name-" + new Date().getTime()
    aCondition.conditionType = new ConditionTypeModel("UsersCountryConditionlet")
    aCondition.owningGroup = groupUnderTest
    aCondition.setParameter("comparatorValue", "is")
    aCondition.setParameter("isoCode", "US")

    let sub = conditionService.onAdd.subscribe((condition:ConditionModel) => {
      sub.unsubscribe()
      expect(groupUnderTest.conditions[condition.key]).toBe(true, "Check the ConditionService.onAdd listener in the RuleService.")
      done()
    }, (err) => {
      expect(err).toBeUndefined("error was thrown.")
      done()
    })
    conditionService.add(aCondition)
  })


  it("Condition being added to the owning group is persisted to server.", function(done){
    var aCondition = new ConditionModel()
    aCondition.name = "pointless_name-" + new Date().getTime()
    aCondition.conditionType = new ConditionTypeModel("UsersCountryConditionlet")
    aCondition.owningGroup = groupUnderTest
    aCondition.setParameter("comparatorValue", "is")
    aCondition.setParameter("isoCode", "US")



    var firstPass = conditionService.onAdd.subscribe((condition:ConditionModel) => {
      //noinspection TypeScriptUnresolvedFunction
      firstPass.unsubscribe() // don't want to run THIS watcher twice.
      expect(groupUnderTest.conditions[condition.key]).toBe(true, "Check the ConditionService.onAdd listener in the RuleService.")
      // condition was persisted to server and is present on the Rule.
      ruleService.save(ruleUnderTest, () => {
        ruleService.get(ruleUnderTest.key, (rule:RuleModel)=> {
          let rehydratedGroup = rule.groups[groupUnderTest.key]
          expect(rehydratedGroup).toBeDefined("The condition group should still exist")
          expect(rehydratedGroup.conditions[condition.key]).toBeDefined("The condition should still exist as a child of the group.")
          done()
        })
      })
    }, (err) => {
      expect(err).toBeUndefined("error was thrown!")
      done()
    })
    // save the condition to the group:
    conditionService.add(aCondition)
  })


  it("Will add a new condition parameters to an existing condition.", function(done){
    var clientCondition = new ConditionModel()
    clientCondition.conditionType = new ConditionTypeModel("UserBrowserHeaderConditionlet")
    clientCondition.owningGroup = groupUnderTest
    clientCondition.setParameter("headerKey", "foo")
    clientCondition.setParameter("headerValue", "bar")

    let key = "aParamKey"
    let value = "aParamValue"

    conditionService.add(clientCondition, (resultCondition)=>{
      // serverCondition is the same instance as resultCondition
      expect(clientCondition.isPersisted()).toBe(true, "Condition is not persisted!")
      clientCondition.clearParameters()
      clientCondition.setParameter(key, value)
      conditionService.save(clientCondition, (savedCondition)=>{
        // savedCondition is also the same instance as resultCondition
        conditionService.get(clientCondition.owningGroup, clientCondition.key, (updatedCondition)=>{
          // updatedCondition and clientCondition SHOULD NOT be the same instance object.
          updatedCondition['abc123'] = 100
          expect(clientCondition['abc123']).toBeUndefined()
          expect(clientCondition.getParameterValue(key)).toBe(value, "ClientCondition param value should still be set.")
          expect(updatedCondition.getParameterValue(key)).toBe(value, "Condition refreshed from server should have the correct param value.")
          expect(Object.keys(updatedCondition.parameters).length).toEqual(1, "The old keys should have been removed.")
          done()
        })
      })
    })
  })

  it("Can update condition parameter values on existing condition.", function(done){
    let param1 = { key: 'sessionKey', v1: 'value1', v2: 'value2'}
    let param2 = { key: 'sessionValue', v1: 'abc123', v2: 'def456'}

    var clientCondition = new ConditionModel()
    clientCondition.conditionType = new ConditionTypeModel("SetSessionAttributeConditionlet")
    clientCondition.owningGroup = groupUnderTest
    clientCondition.setParameter(param1.key, param1.v1)
    clientCondition.setParameter(param2.key, param2.v1)

    conditionService.add(clientCondition, (resultCondition)=>{
      clientCondition.setParameter(param1.key, param1.v2)
      conditionService.save(clientCondition, (savedCondition)=>{
        conditionService.get(clientCondition.owningGroup, clientCondition.key, (updatedCondition)=>{
          expect(updatedCondition.getParameterValue(param1.key)).toBe(param1.v2, "Condition refreshed from server should have the correct param value.")
          expect(updatedCondition.getParameterValue(param2.key)).toBe(param2.v1, "Condition refreshed from server should have the correct param value.")
          expect(Object.keys(updatedCondition.parameters).length).toEqual(2)
          done()

        })
      })
    })

  })
});


class Tools {
  static createRule(ruleService:RuleService, cb:Function=null){
    console.log('Attempting to create rule.')
    let rule = new RuleModel()
    rule.enabled = true
    rule.name = "TestRule-" + new Date().getTime()
    ruleService.add(rule, cb)

  }

}


