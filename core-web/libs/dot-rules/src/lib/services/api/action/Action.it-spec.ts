import {} from 'jasmine';

import { Observable, Subscription } from 'rxjs';

import { ReflectiveInjector } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { CwError } from '@dotcms/dotcms-js';
import { ApiRoot } from '@dotcms/dotcms-js';
import { UserModel } from '@dotcms/dotcms-js';

import { ActionService } from './Action';
import { ConditionService } from '../condition/Condition';
import { ConditionGroupService } from '../condition-group/ConditionGroup';
import { RuleModel, RuleService, ActionModel } from '../rule/Rule';

import { I18nService } from '../../i18n/i18n.service';

const injector = ReflectiveInjector.resolveAndCreate([
    ApiRoot,
    I18nService,
    UserModel,
    RuleService,
    ActionService,
    ConditionService,
    ConditionGroupService,
    BrowserModule
]);

describe('Integration.api.rule-engine.ActionService', () => {
    let ruleService: RuleService;
    let ruleOnAddSub: Subscription;

    let actionService: ActionService;
    let ruleUnderTest: RuleModel;
    const actionTypes = {};
    let setSessionActionlet;

    beforeAll((done) => {
        ruleService = injector.get(RuleService);
        actionService = injector.get(ActionService);
        ruleService.getRuleActionTypes().subscribe((typesAry) => {
            typesAry.forEach((item) => (actionTypes[item.key] = item));
            setSessionActionlet = actionTypes['SetSessionAttributeActionlet'];
            done();
        });
    });

    beforeEach(function (done): void {
        Gen.createRules(ruleService);
        ruleOnAddSub = ruleService.loadRules().subscribe(
            (rule: RuleModel[]) => {
                ruleUnderTest = rule[0];
                done();
            },
            (err) => {
                expect(err).toBeUndefined('error was thrown.');
                done();
            }
        );
    });

    afterEach((done) => {
        ruleService.deleteRule(ruleUnderTest.key).subscribe(() => {
            ruleUnderTest = null;
            ruleOnAddSub.unsubscribe();
            done();
        });
    });

    it('Has rules that we can add actions to', () => {
        expect(ruleUnderTest.isPersisted()).toBe(true);
    });

    it('Can add a new Action', (done) => {
        console.log('can add new', setSessionActionlet);
        const anAction = new ActionModel(null, setSessionActionlet);
        anAction.setParameter('sessionKey', 'foo');
        anAction.setParameter('sessionValue', 'bar');

        actionService
            .createRuleAction(ruleUnderTest.key, anAction)
            .subscribe((action: ActionModel) => {
                expect(action.isPersisted()).toBe(true, 'Action is not persisted!');
                done();
            });
    });

    it('Action being added to the owning rule is persisted to server.', (done) => {
        const anAction = new ActionModel(null, setSessionActionlet);
        anAction.setParameter('sessionKey', 'foo');
        anAction.setParameter('sessionValue', 'bar');

        actionService
            .createRuleAction(ruleUnderTest.key, anAction)
            .subscribe((action: ActionModel) => {
                ruleService.updateRule(ruleUnderTest.key, ruleUnderTest).subscribe(() => {
                    ruleService.loadRule(ruleUnderTest.key).subscribe((rule: RuleModel) => {
                        expect(rule.ruleActions[action.key]).toBe(true);
                        const sub = actionService
                            .allAsArray(rule.key, Object.keys(rule.ruleActions))
                            .subscribe(
                                (actions: ActionModel[]) => {
                                    console.log('Rule: ', rule);
                                    console.log('Rehydrated Rule: ', rule);
                                    console.log('Rehydrated Actions: ', actions);
                                    const rehydratedAction = actions[0];
                                    expect(
                                        rehydratedAction.getParameterValue('sessionKey')
                                    ).toEqual('foo');
                                    sub.unsubscribe();
                                    done();
                                },
                                (e) => {
                                    console.log(e);
                                    expect(e).toBeUndefined('Test Failed');
                                }
                            );
                    });
                });
            });
    });

    it('Will add a new action parameters to an existing action.', (done) => {
        const clientAction = new ActionModel(null, setSessionActionlet);
        clientAction.setParameter('sessionKey', 'foo');
        clientAction.setParameter('sessionValue', 'bar');

        const key = 'sessionKey';
        const value = 'aParamValue';

        actionService.createRuleAction(ruleUnderTest.key, clientAction).subscribe(() => {
            expect(clientAction.isPersisted()).toBe(true, 'Action is not persisted!');

            clientAction.setParameter(key, value);
            actionService.updateRuleAction(ruleUnderTest.key, clientAction).subscribe(() => {
                // savedAction is also the same instance as resultAction
                actionService
                    .get(ruleUnderTest.key, clientAction.key)
                    .subscribe((updatedAction) => {
                        // updatedAction and clientAction SHOULD NOT be the same instance object.
                        updatedAction['abc123'] = 100;
                        expect(clientAction['abc123']).toBeUndefined();
                        expect(clientAction.getParameterValue(key)).toBe(value);
                        expect(updatedAction.getParameterValue(key)).toBe(value);
                        done();
                    });
            });
        });
    });

    it('Can update action parameter values on existing action.', (done) => {
        const param1 = { key: 'sessionKey', v1: 'value1', v2: 'value2' };
        const param2 = { key: 'sessionValue', v1: 'abc123', v2: 'def456' };

        const clientAction = new ActionModel(null, setSessionActionlet);
        clientAction.setParameter(param1.key, param1.v1);
        clientAction.setParameter(param2.key, param2.v1);

        actionService.createRuleAction(ruleUnderTest.key, clientAction).subscribe(() => {
            clientAction.setParameter(param1.key, param1.v2);
            actionService.updateRuleAction(ruleUnderTest.key, clientAction).subscribe(() => {
                actionService
                    .get(ruleUnderTest.key, clientAction.key)
                    .subscribe((updatedAction) => {
                        expect(updatedAction.getParameterValue(param1.key)).toBe(param1.v2);
                        expect(updatedAction.getParameterValue(param2.key)).toBe(param2.v1);
                        done();
                    });
            });
        });
    });
});

class Gen {
    static createRules(ruleService: RuleService): Observable<RuleModel | CwError> {
        console.log('Attempting to create rule.');
        const rule = new RuleModel(null);
        rule.enabled = true;
        rule.name = 'TestRule-' + new Date().getTime();

        return ruleService.createRule(rule);
    }
}
