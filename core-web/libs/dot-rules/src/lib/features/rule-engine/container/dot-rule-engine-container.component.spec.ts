import { createComponentFactory, Spectator } from '@openng/spectator/jest';
import { BehaviorSubject, NEVER, Subject, of } from 'rxjs';

import { HttpHeaders } from '@angular/common/http';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { LoggerService } from '@dotcms/dotcms-js';

import { DotRuleEngineContainerComponent } from './dot-rule-engine-container.component';

import { ActionService } from '../../../services/api/action/Action';
import { BundleService } from '../../../services/api/bundle/bundle-service';
import { ConditionService } from '../../../services/api/condition/Condition';
import { ConditionGroupService } from '../../../services/api/condition-group/ConditionGroup';
import { RuleService } from '../../../services/api/rule/Rule';
import { RuleViewService } from '../../../services/ui/dot-view-rule-service';
import { DotRuleEngineComponent } from '../dot-rule-engine.component';

const makeRuleServiceMock = (errorsSubject$: Subject<unknown>) => ({
    _errors$: errorsSubject$,
    conditionTypes$: new BehaviorSubject([]),
    ruleActionTypes$: new BehaviorSubject([]),
    _ruleActionTypes: {},
    _conditionTypes: {},
    requestRules: jest.fn(),
    loadRules: jest.fn().mockReturnValue(NEVER)
});

describe('DotRuleEngineContainerComponent', () => {
    let spectator: Spectator<DotRuleEngineContainerComponent>;
    let errorsSubject$: Subject<unknown>;
    let showErrorMessage: jest.Mock;

    const createComponent = createComponentFactory({
        component: DotRuleEngineContainerComponent,
        schemas: [NO_ERRORS_SCHEMA],
        overrideComponents: [
            [
                DotRuleEngineContainerComponent,
                { remove: { imports: [DotRuleEngineComponent] }, add: {} }
            ]
        ],
        providers: [
            { provide: ActionService, useValue: { error: NEVER } },
            {
                provide: BundleService,
                useValue: { loadPublishEnvironments: jest.fn().mockReturnValue(of([])) }
            },
            { provide: ConditionGroupService, useValue: { error: NEVER } },
            { provide: ConditionService, useValue: { error: NEVER } },
            { provide: LoggerService, useValue: { info: jest.fn(), error: jest.fn() } },
            { provide: ActivatedRoute, useValue: { params: of({}), queryParams: of({}) } }
        ]
    });

    beforeEach(() => {
        errorsSubject$ = new Subject();
        showErrorMessage = jest.fn();

        spectator = createComponent({
            providers: [
                { provide: RuleService, useValue: makeRuleServiceMock(errorsSubject$) },
                { provide: RuleViewService, useValue: { showErrorMessage, message: NEVER } }
            ]
        });
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('VIEW permission error (_errors$)', () => {
        const emitError = (message: string) =>
            errorsSubject$.next({ error: { message }, headers: new HttpHeaders() });

        it('should set loading to false', () => {
            expect(spectator.component.loading()).toBe(true);
            emitError('some error');
            expect(spectator.component.loading()).toBe(false);
        });

        it('should set showRules to false', () => {
            emitError('some error');
            expect(spectator.component.showRules()).toBe(false);
        });

        it('should show the error message', () => {
            emitError('User does not have permissions to VIEW Rules');
            expect(showErrorMessage).toHaveBeenCalledWith(
                'User does not have permissions to VIEW Rules',
                false,
                ''
            );
        });

        it('should strip user ID from the message', () => {
            emitError(
                'User user-58b73546-bf7c-4f4c-a7d1-f51da0825ab0 does not have permissions to VIEW Rules'
            );
            expect(showErrorMessage).toHaveBeenCalledWith(
                'User does not have permissions to VIEW Rules',
                false,
                ''
            );
        });
    });

    describe('PUBLISH permission error (_handle403Error)', () => {
        const make403 = (message: string) => ({
            status: 403,
            error: { error: `dotcms.api.error.forbidden: ${message}` }
        });

        it('should return true and show the error message', () => {
            const handled = spectator.component['_handle403Error'](
                make403('User user-abc does not have permissions to PUBLISH Rules')
            );
            expect(handled).toBe(true);
            expect(showErrorMessage).toHaveBeenCalledWith(
                'User does not have permissions to PUBLISH Rules'
            );
        });

        it('should strip the forbidden prefix from the message', () => {
            spectator.component['_handle403Error'](
                make403('User does not have permissions to PUBLISH Rules')
            );
            expect(showErrorMessage).toHaveBeenCalledWith(
                'User does not have permissions to PUBLISH Rules'
            );
        });

        it('should strip user ID from the message', () => {
            spectator.component['_handle403Error'](
                make403(
                    'User user-58b73546-bf7c-4f4c-a7d1-f51da0825ab0 does not have permissions to PUBLISH Rules'
                )
            );
            expect(showErrorMessage).toHaveBeenCalledWith(
                'User does not have permissions to PUBLISH Rules'
            );
        });

        it('should return false for non-403 errors', () => {
            const handled = spectator.component['_handle403Error']({ status: 500, error: {} });
            expect(handled).toBe(false);
            expect(showErrorMessage).not.toHaveBeenCalled();
        });

        it('should return false when body has no error string', () => {
            const handled = spectator.component['_handle403Error']({ status: 403, error: {} });
            expect(handled).toBe(false);
        });
    });
});
