import {RuleModel, RuleService} from '../../api/rule-engine/Rule';
import {ConditionService} from '../../api/rule-engine/Condition';
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

describe('Integration.api.rule-engine.ConditionGroupService', function () {

  var ruleService:RuleService
  var onAddSub
  var conditionGroupService:ConditionGroupService
  var ruleUnderTest:RuleModel

  beforeEach(function (done) {
    ruleService = injector.get(RuleService)
    conditionGroupService = injector.get(ConditionGroupService)

    Gen.createRules(ruleService)

    onAddSub = ruleService.list().subscribe((rule:RuleModel[]) => {
      ruleUnderTest = rule[0]
      done()
    }, (err) => {
      expect(err).toBeUndefined("error was thrown.")
      done()
    })
  });

  afterEach(function(done){
    ruleService.remove(ruleUnderTest, ()=>{
      ruleUnderTest = null
      onAddSub.unsubscribe()
      done()
    });

  })

  it("Has rules that we can add conditionGroups to", function(){
    expect(ruleUnderTest.isPersisted()).toBe(true)
  })

  it("Can add a new ConditionGroup", function(done){
    var aConditionGroup = new ConditionGroupModel(null, ruleUnderTest, "OR", 1)

    conditionGroupService.add(aConditionGroup, (conditionGroup:ConditionGroupModel) => {
      //noinspection TypeScriptUnresolvedFunction
      expect(conditionGroup.isPersisted()).toBe(true, "ConditionGroup is not persisted!")
      done()
    })
  })

  it("ConditionGroup being added to the owning rule is persisted to server.", function(done){
    var aConditionGroup = new ConditionGroupModel(null, ruleUnderTest, "OR", 99)
    aConditionGroup.owningRule = ruleUnderTest
    conditionGroupService.add(aConditionGroup,(conditionGroup:ConditionGroupModel) => {
        ruleService.get(ruleUnderTest.key, (rule:RuleModel)=> {
          expect(rule.groups[conditionGroup.key]).toBeDefined("Well that's odd")
          expect(rule.groups[conditionGroup.key].operator).toEqual("OR")

          // @todo ggranum: Defect=Cannot set priority at creation time.
          //expect(rule.groups[conditionGroup.key].priority).toEqual(99)

          /* Now read the ConditionGroups off the rule we just got back. Add listener first, then trigger call. */
          conditionGroupService.list(rule).subscribe((conditionGroups:ConditionGroupModel[])=>{
            let condGroup = conditionGroups[0]
            expect(conditionGroup.operator).toEqual("OR")
            // @todo ggranum: Defect=Cannot set priority at creation time.
            //expect(conditionGroup.priority).toEqual(99)
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
