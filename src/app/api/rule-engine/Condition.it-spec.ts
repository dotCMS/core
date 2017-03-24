import * as Rx from 'rxjs/Rx';
import {RuleModel, RuleService, ConditionGroupModel, ConditionModel} from '../../api/rule-engine/Rule';
import {ConditionService} from '../../api/rule-engine/Condition';
import {Injector} from '@angular/core';
import {ApiRoot} from '../../api/persistence/ApiRoot';
import {UserModel} from '../../api/auth/UserModel';
import {ActionService} from '../../api/rule-engine/Action';
import {ConditionGroupService} from '../../api/rule-engine/ConditionGroup';
import {I18nService} from '../system/locale/I18n';
import {HTTP_PROVIDERS} from '@angular/http';

import {CwError} from '../system/http-response-util';

let injector = Injector.resolveAndCreate([ApiRoot,
  I18nService,
  UserModel,
  RuleService,
  ActionService,
  ConditionService,
  ConditionGroupService,
  HTTP_PROVIDERS
]);

describe('Integration.api.rule-engine.ConditionService', () => {

  let ruleService: RuleService;
  let ruleOnAddSub: Rx.Subscription<RuleModel>;

  let conditionGroupService: ConditionGroupService;
  let conditionService: ConditionService;
  let ruleUnderTest: RuleModel;
  let groupUnderTest: ConditionGroupModel;

  let conditionTypes = {};
  let usersCountryConditionType;
  let requestHeaderConditionType;

  beforeAll((done) => {
    ruleService = injector.get(RuleService);
    conditionGroupService = injector.get(ConditionGroupService);
    conditionService = injector.get(ConditionService);
    ruleService.getConditionTypes().subscribe((typesAry: ConditionModel[]) => {
      typesAry.forEach((item) => conditionTypes[item.key] = item );
      usersCountryConditionType = conditionTypes['UsersCountryConditionlet'];
      requestHeaderConditionType = conditionTypes['RequestHeaderConditionlet'];
      done();
    });
  });

  beforeEach( done => {
    ruleUnderTest = null;
    groupUnderTest = null;

    Gen.createRules(ruleService);
    ruleOnAddSub = ruleService.loadRules().subscribe((rule: RuleModel[]) => {
      ruleUnderTest = rule[0];
      groupUnderTest = new ConditionGroupModel({operator: 'OR', priority: 1});
      conditionGroupService.createConditionGroup(ruleUnderTest.key, groupUnderTest).subscribe( () => done);
    });
  });

  afterEach(done => {
    ruleService.deleteRule(ruleUnderTest.key).subscribe( () => {
      ruleUnderTest = null;
      ruleOnAddSub.unsubscribe();
      done();
    });

  });

  it('Has a rule and group that we can add conditions to', () => {
    expect(ruleUnderTest.isPersisted()).toBe(true);
    expect(groupUnderTest.isPersisted()).toBe(true);
  });

  it('Can add a new Condition', done => {
    let aCondition = new ConditionModel({_type: usersCountryConditionType});
    aCondition.setParameter('sessionKey', 'foo');
    aCondition.setParameter('sessionValue', 'bar');

    conditionService.add(groupUnderTest.key, aCondition).subscribe((condition: ConditionModel) => {
      //noinspection TypeScriptUnresolvedFunction
      expect(condition.isPersisted()).toBe(true, 'Condition is not persisted!');
      done();
    });
  });

  it('Condition being added to the owning group is persisted to server.', done => {
    let aCondition = new ConditionModel({_type: usersCountryConditionType});
    aCondition.setParameter('comparatorValue', 'is');
    aCondition.setParameter('isoCode', 'US');
    // save the condition to the group:
    conditionService.add(ruleUnderTest.key, aCondition).subscribe( (condition: ConditionModel) => {
        ruleService.loadRule(ruleUnderTest.key).subscribe( (rule: RuleModel) => {
          let rehydratedGroup = rule.conditionGroups[groupUnderTest.key];
          expect(rehydratedGroup).toBeDefined('The condition group should still exist');
          expect(rehydratedGroup.conditions[condition.key]).toBeDefined('The condition should still exist as a child of the group.');
          done();
        });
    });
  });

  it('Will add new condition parameters to an existing condition.', done => {
    console.log('will add new ', '', requestHeaderConditionType);
    let clientCondition = new ConditionModel({type: requestHeaderConditionType});
    clientCondition.setParameter('browser-header', 'foo');
    clientCondition.setParameter('header-value', 'bar');

    let key = 'browser-header';
    let value = 'aParamValue';

    conditionService.add(groupUnderTest.key, clientCondition).subscribe( (resultCondition) => {
      // serverCondition is the same instance as resultCondition
      expect(clientCondition.isPersisted()).toBe(true, 'Condition is not persisted!');
      clientCondition.setParameter(key, value);
      conditionService.save(groupUnderTest.key, clientCondition).subscribe( (savedCondition) => {
        // savedCondition is also the same instance as resultCondition
        conditionService.get(clientCondition.key).subscribe( (updatedCondition) => {
          updatedCondition['abc123'] = 100;
          expect(clientCondition['abc123']).toBeUndefined('updatedCondition and clientCondition SHOULD NOT be the same instance object.');
          expect(clientCondition.getParameterValue(key)).toBe(value);
          expect(updatedCondition.getParameterValue(key)).toBe(value);
          done();
        });
      });
    });
  });

  it('Can update condition parameter values on existing condition.', done => {
    let param1 = { key: 'browser-header', v1: 'value1', v2: 'value2'};
    let param2 = { key: 'header-value', v1: 'abc123', v2: 'def456'};

    let clientCondition = new ConditionModel({type: requestHeaderConditionType});
    clientCondition.setParameter(param1.key, param1.v1);
    clientCondition.setParameter(param2.key, param2.v1);

    conditionService.add(groupUnderTest.key, clientCondition).subscribe( (resultCondition) => {
      clientCondition.setParameter(param1.key, param1.v2);
      conditionService.save(groupUnderTest.key, clientCondition).subscribe( (savedCondition) => {
        conditionService.get(clientCondition.key).subscribe( (updatedCondition) => {
          expect(updatedCondition.getParameterValue(param1.key)).toBe(param1.v2);
          expect(updatedCondition.getParameterValue(param2.key)).toBe(param2.v1);
          done();
        });
      });
    });
  });
});

class Gen {

  static createRules(ruleService: RuleService): Rx.Observable<RuleModel|CwError>  {
    console.log('Attempting to create rule.');
    let rule = new RuleModel(null);
    rule.enabled = true;
    rule.name = 'TestRule-' + new Date().getTime();

    return ruleService.createRule(rule);

  }

}
