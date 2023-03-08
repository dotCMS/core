import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';

import { NgForOf, NgIf } from '@angular/common';

import { ConfirmPopupModule } from 'primeng/confirmpopup';

import { DotMessageService } from '@dotcms/data-access';
import { Goal, GOAL_OPERATORS, GOAL_PARAMETERS, GOAL_TYPES } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsExperimentGoalConfigurationDetailComponent } from './dot-experiments-experiment-goal-configuration-detail.component';

const messageServiceMock = new MockDotMessageService({
    'experiments.goal.reach_page.name': 'Reach page',
    'experiments.goal.conditions.default': 'Default',
    'experiments.goal.bounce_rate.name': 'Bounce rate'
});

describe('DotExperimentsExperimentGoalConfigurationDetailComponent', () => {
    let spectator: Spectator<DotExperimentsExperimentGoalConfigurationDetailComponent>;

    const createComponent = createComponentFactory({
        imports: [ConfirmPopupModule, NgIf, NgForOf],
        component: DotExperimentsExperimentGoalConfigurationDetailComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(async () => {
        spectator = createComponent({ detectChanges: false });
    });

    it('should render the goal and conditions of type REACH_PAGE', () => {
        const TYPE_OF_GOAL_TRANSLATED = 'Reach page';
        const goalMock: Goal = {
            name: 'default',
            type: GOAL_TYPES.REACH_PAGE,
            conditions: [
                {
                    parameter: GOAL_PARAMETERS.URL,
                    operator: GOAL_OPERATORS.EQUALS,
                    value: 'index'
                },
                {
                    parameter: GOAL_PARAMETERS.REFERER,
                    operator: GOAL_OPERATORS.CONTAINS,
                    value: 'index',
                    isDefault: true
                }
            ]
        };
        spectator.setInput({
            goal: goalMock
        });

        spectator.detectChanges();

        expect(spectator.query(byTestId('goal-label-type-goal'))).toContainText(
            TYPE_OF_GOAL_TRANSLATED
        );
        expect(spectator.query(byTestId('goal-label-name-goal'))).toContainText(goalMock.name);
        expect(spectator.queryAll(byTestId('goal-condition')).length).toBe(
            goalMock.conditions.length
        );
        expect(spectator.query(byTestId('goal-default-parameter'))).toExist();
        expect(spectator.queryLast(byTestId('goal-parameter'))).toContainText(
            'Default -  ' + goalMock.conditions[1].parameter
        );
        expect(spectator.queryLast(byTestId('goal-operator'))).toContainText(
            goalMock.conditions[1].operator.toLowerCase()
        );
        expect(spectator.queryLast(byTestId('goal-value'))).toContainText(
            goalMock.conditions[1].value
        );
    });

    it('should render the goal and conditions of type BOUNCE_RATE', () => {
        const TYPE_OF_GOAL_TRANSLATED = 'Bounce rate';
        const goalMock: Goal = {
            name: 'default',
            type: GOAL_TYPES.BOUNCE_RATE,
            conditions: [
                {
                    parameter: GOAL_PARAMETERS.URL,
                    operator: GOAL_OPERATORS.CONTAINS,
                    value: 'index',
                    isDefault: true
                }
            ]
        };
        spectator.setInput({
            goal: goalMock
        });

        spectator.detectChanges();

        expect(spectator.query(byTestId('goal-label-type-goal'))).toContainText(
            TYPE_OF_GOAL_TRANSLATED
        );
        expect(spectator.query(byTestId('goal-label-name-goal'))).toContainText(goalMock.name);
        expect(spectator.queryAll(byTestId('goal-condition')).length).toBe(
            goalMock.conditions.length
        );
        expect(spectator.query(byTestId('goal-default-parameter'))).toExist();
        expect(spectator.queryLast(byTestId('goal-parameter'))).toContainText(
            'Default -  ' + goalMock.conditions[0].parameter
        );
        expect(spectator.queryLast(byTestId('goal-operator'))).toContainText(
            goalMock.conditions[0].operator.toLowerCase()
        );
        expect(spectator.queryLast(byTestId('goal-value'))).toContainText(
            goalMock.conditions[0].value
        );
    });
});
