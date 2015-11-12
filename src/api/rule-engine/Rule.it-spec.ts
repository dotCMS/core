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


var injector = Injector.resolveAndCreate([ApiRoot,
  UserModel,
  RuleService,
  ActionService,
  ConditionTypeService,
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

  afterEach(function () {
    rulesToRemove.forEach((rule) => {
      ruleService.remove(rule);
    })
  })

  it("Can create a simple rule.", function (done) {
    var clientRule:RuleModel
    clientRule = new RuleModel()
    clientRule.enabled = true
    clientRule.name = "TestRule-" + new Date().getTime()
    let ruleOnAddSub = ruleService.onAdd.subscribe((serverRule:RuleModel) => {
      rulesToRemove.push(serverRule)
      expect(serverRule.isPersisted()).toBe(true)
      expect(serverRule.enabled).toBe(true)
      expect(serverRule.name).toBe(clientRule.name)
      let randomKey = 'abc_' + Math.round(Math.random() * 1000)
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
