import * as Rx from 'rxjs/Rx'
import {RuleModel, RuleService} from '../../api/rule-engine/Rule';
import {ConditionService, ConditionModel} from '../../api/rule-engine/Condition';
import {Injector, Provider} from 'angular2/core';
import {DataStore} from '../../api/persistence/DataStore'
import {RestDataStore} from '../../api/persistence/RestDataStore';
import {ApiRoot} from '../../api/persistence/ApiRoot';
import {UserModel} from '../../api/auth/UserModel';
import {ConditionTypeService} from '../../api/rule-engine/ConditionType';
import {ActionService} from '../../api/rule-engine/Action';
import {ConditionGroupService, ConditionGroupModel} from '../../api/rule-engine/ConditionGroup';
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

describe('Integration.api.rule-engine.ConditionService', function () {

  var ruleService:RuleService
  var ruleOnAddSub:Rx.Subscription<RuleModel>


  var conditionGroupService:ConditionGroupService
  var conditionService:ConditionService
  var ruleUnderTest:RuleModel
  var groupUnderTest:ConditionGroupModel

  var conditionTypeService:ConditionTypeService
  var conditionTypes = {}
  var usersCountryConditionType
  var requestHeaderConditionType


  beforeAll(function(done){
    ruleService = injector.get(RuleService)
    conditionGroupService = injector.get(ConditionGroupService)
    conditionTypeService = injector.get(ConditionTypeService)
    conditionService = injector.get(ConditionService)
    conditionTypeService.list().subscribe((typesAry)=>{
      typesAry.forEach((item) => conditionTypes[item.key] = item )
      usersCountryConditionType = conditionTypes["UsersCountryConditionlet"]
      requestHeaderConditionType = conditionTypes["RequestHeaderConditionlet"]
      done()
    })


  })

  beforeEach(function (done) {
    ruleUnderTest = null;
    groupUnderTest = null;

    Gen.createRules(ruleService)
    ruleOnAddSub = ruleService.list().subscribe((rule:RuleModel[]) => {
      ruleUnderTest = rule[0]
      groupUnderTest = new ConditionGroupModel(null, ruleUnderTest, "OR", 1)
      conditionGroupService.add(groupUnderTest, done)
    })
  });

  afterEach(function(done){
    ruleService.remove(ruleUnderTest, ()=>{
      ruleUnderTest = null
      ruleOnAddSub.unsubscribe()
      done()
    });

  })

  it("Has a rule and group that we can add conditions to", function(){
    expect(ruleUnderTest.isPersisted()).toBe(true)
    expect(groupUnderTest.isPersisted()).toBe(true)
  })

  it("Can add a new Condition", function(done){
    var aCondition = new ConditionModel(null, usersCountryConditionType)
    aCondition.owningGroup = groupUnderTest
    aCondition.setParameter("sessionKey", "foo")
    aCondition.setParameter("sessionValue", "bar")

    conditionService.add(aCondition,(condition:ConditionModel) => {
      //noinspection TypeScriptUnresolvedFunction
      expect(condition.isPersisted()).toBe(true, "Condition is not persisted!")
      done()
    })
  })


  it("Condition being added to the owning group is persisted to server.", function(done){
    var aCondition = new ConditionModel(null, usersCountryConditionType)
    aCondition.owningGroup = groupUnderTest
    aCondition.setParameter("comparatorValue", "is")
    aCondition.setParameter("isoCode", "US")



    // save the condition to the group:
    conditionService.add(aCondition, (condition:ConditionModel) => {
        ruleService.get(ruleUnderTest.key, (rule:RuleModel)=> {
          let rehydratedGroup = rule.groups[groupUnderTest.key]
          expect(rehydratedGroup).toBeDefined("The condition group should still exist")
          expect(rehydratedGroup.conditions[condition.key]).toBeDefined("The condition should still exist as a child of the group.")
          done()
        })
    })
  })


  it("Will add new condition parameters to an existing condition.", function(done){
    console.log("will add new ", "", requestHeaderConditionType)
    var clientCondition = new ConditionModel(null, requestHeaderConditionType)
    clientCondition.owningGroup = groupUnderTest
    clientCondition.setParameter("browser-header", "foo")
    clientCondition.setParameter("header-value", "bar")

    let key = "browser-header"
    let value = "aParamValue"

    conditionService.add(clientCondition, (resultCondition)=>{
      // serverCondition is the same instance as resultCondition
      expect(clientCondition.isPersisted()).toBe(true, "Condition is not persisted!")
      clientCondition.setParameter(key, value)
      conditionService.save(clientCondition, (savedCondition)=>{
        // savedCondition is also the same instance as resultCondition
        conditionService.get(clientCondition.owningGroup, clientCondition.key, (updatedCondition)=>{
          updatedCondition['abc123'] = 100
          expect(clientCondition['abc123']).toBeUndefined("updatedCondition and clientCondition SHOULD NOT be the same instance object.")
          expect(clientCondition.getParameterValue(key)).toBe(value)
          expect(updatedCondition.getParameterValue(key)).toBe(value)
          done()
        })
      })
    })
  })

  it("Can update condition parameter values on existing condition.", function(done){
    let param1 = { key: 'browser-header', v1: 'value1', v2: 'value2'}
    let param2 = { key: 'header-value', v1: 'abc123', v2: 'def456'}

    var clientCondition = new ConditionModel(null, requestHeaderConditionType)
    clientCondition.owningGroup = groupUnderTest
    clientCondition.setParameter(param1.key, param1.v1)
    clientCondition.setParameter(param2.key, param2.v1)

    conditionService.add(clientCondition, (resultCondition)=>{
      clientCondition.setParameter(param1.key, param1.v2)
      conditionService.save(clientCondition, (savedCondition)=>{
        conditionService.get(clientCondition.owningGroup, clientCondition.key, (updatedCondition)=>{
          expect(updatedCondition.getParameterValue(param1.key)).toBe(param1.v2)
          expect(updatedCondition.getParameterValue(param2.key)).toBe(param2.v1)
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

    return ruleService.add(rule)

  }

}
