import {RuleModel, RuleService} from '../../api/rule-engine/Rule';
import {ConditionService} from '../../api/rule-engine/Condition';
import {Injector, Provider} from 'angular2/core';
import {ApiRoot} from '../../api/persistence/ApiRoot';
import {UserModel} from '../../api/auth/UserModel';
import {ConditionTypeService} from '../../api/rule-engine/ConditionType';
import {ActionService} from '../../api/rule-engine/Action';
import {ActionTypeService} from "./ActionType";
import {ConditionGroupService} from '../../api/rule-engine/ConditionGroup';
import {I18nService} from "../system/locale/I18n";
import {HTTP_PROVIDERS} from "angular2/http";


var injector = Injector.resolveAndCreate([
  ApiRoot,
  I18nService,
  UserModel,
  RuleService,
  ActionService,
  ActionTypeService,
  ConditionTypeService,
  ConditionService,
  ConditionGroupService,
  HTTP_PROVIDERS
])

describe('Integration.api.rule-engine.RuleService', function () {

  var ruleService:RuleService

  var rulesToRemove:Array<RuleModel>


  beforeEach(function () {
    ruleService = injector.get(RuleService)
    rulesToRemove = []
  });

  afterEach(function () {
    rulesToRemove.forEach((rule) => {
      ruleService.deleteRule(rule.key);
    })
  })

  it("Can create a simple rule.", function (done) {
    var clientRule:RuleModel
    clientRule = new RuleModel(null)
    clientRule.enabled = true
    clientRule.name = "TestRule-" + new Date().getTime()

    ruleService.createRule(clientRule).subscribe( (serverRule:RuleModel) => {
      rulesToRemove.push(serverRule)
      expect(serverRule.isPersisted()).toBe(true)
      expect(serverRule.enabled).toBe(true)
      expect(serverRule.name).toBe(clientRule.name)
      let randomKey = 'abc_' + Math.round(Math.random() * 1000)
      serverRule[randomKey] = "The object provided by the observer is the same instance as the one added."
      expect(clientRule[randomKey]).toBe(serverRule[randomKey])
      done()
    })
  })

});
