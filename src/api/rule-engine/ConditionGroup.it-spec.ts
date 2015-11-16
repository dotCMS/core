
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
import {ConditionTypeService} from '../../api/rule-engine/ConditionType';


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


describe('Integration.api.rule-engine.ConditionGroupService', function () {

  var ruleService:RuleService
  var onAddSub
  var conditionGroupService:ConditionGroupService
  var ruleUnderTest:RuleModel

  beforeEach(function (done) {
    ruleService = injector.get(RuleService)
    conditionGroupService = injector.get(ConditionGroupService)
    onAddSub = ruleService.onAdd.subscribe((rule:RuleModel) => {
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
    onAddSub.unsubscribe()
  })

  it("Has rules that we can add conditionGroups to", function(){
    expect(ruleUnderTest.isPersisted()).toBe(true)
  })

  it("Can add a new ConditionGroup", function(done){
    var aConditionGroup = new ConditionGroupModel()

    aConditionGroup.owningRule = ruleUnderTest
    aConditionGroup.operator = "OR"

    var subscriber = conditionGroupService.onAdd.subscribe((conditionGroup:ConditionGroupModel) => {
      //noinspection TypeScriptUnresolvedFunction
      expect(conditionGroup.isPersisted()).toBe(true, "ConditionGroup is not persisted!")
      done()
    }, (err) => {
      expect(err).toBeUndefined("error was thrown.")
      done()
    })

    conditionGroupService.add(aConditionGroup)

  })

  it("Is added to the owning rule's list of conditionGroups.", function(done){
    var aConditionGroup = new ConditionGroupModel()
    aConditionGroup.owningRule = ruleUnderTest
    aConditionGroup.operator = "OR"

    conditionGroupService.onAdd.subscribe((conditionGroup:ConditionGroupModel) => {
      expect(ruleUnderTest.groups[conditionGroup.key]).toBeDefined()
      done()
    }, (err) => {
      expect(err).toBeUndefined("error was thrown.")
      done()
    })
    conditionGroupService.add(aConditionGroup)
  })


  it("ConditionGroup being added to the owning rule is persisted to server.", function(done){
    var aConditionGroup = new ConditionGroupModel()
    aConditionGroup.owningRule = ruleUnderTest
    aConditionGroup.operator = "OR"
    aConditionGroup.priority = 99


    var firstPass = conditionGroupService.onAdd.subscribe((conditionGroup:ConditionGroupModel) => {
      firstPass.unsubscribe() // don't want to run THIS watcher twice.
      expect(ruleUnderTest.groups[conditionGroup.key]).toBeDefined("Expected group to be on the rule." )
      ruleService.save(ruleUnderTest, () => {

        ruleService.get(ruleUnderTest.key, (rule:RuleModel)=> {
          expect(rule.groups[conditionGroup.key]).toBeDefined("Well that's odd")
          expect(rule.groups[conditionGroup.key].operator).toEqual("OR")
          // @todo ggranum: Defect=Cannot set priority at creation time.
          //expect(rule.groups[conditionGroup.key].priority).toEqual(99)

          /* Now read the ConditionGroups off the rule we just got back. Add listener first, then trigger call. */
          conditionGroupService.onAdd.subscribe((conditionGroup:ConditionGroupModel)=>{
            expect(conditionGroup.operator).toEqual("OR")
            // @todo ggranum: Defect=Cannot set priority at creation time.
            //expect(conditionGroup.priority).toEqual(99)
            done()
          })
          conditionGroupService.list(rule)
        })
      })
    }, (err) => {
      expect(err).toBeUndefined("error was thrown!")
      done()
    })
    conditionGroupService.add(aConditionGroup)
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


