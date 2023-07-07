import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { GOAL_TYPES, Goals, RangeOfDateAndTime, SummaryLegend } from '@dotcms/dotcms-models';
import { GoalsMock, MockDotMessageService, suggestedWinnerMock } from '@dotcms/utils-testing';

import { DotExperimentsExperimentSummaryComponent } from './dot-experiments-experiment-summary.component';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.scheduling.name': 'Scheduling',
    'experiments.configure.scheduling.start': 'When the experiment start'
});

describe('DotExperimentsExperimentSummaryComponent', () => {
    let spectator: Spectator<DotExperimentsExperimentSummaryComponent>;
    const createComponent = createComponentFactory({
        imports: [],
        component: DotExperimentsExperimentSummaryComponent,

        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should rendered the goal Input', () => {
        const goals: Goals = GoalsMock;
        spectator.setInput({
            goals
        });

        expect(spectator.query(byTestId('goal-label'))).toHaveText(
            spectator.component.goalTypeMap[GOAL_TYPES.REACH_PAGE].label
        );
    });

    it('should rendered the scheduling Input', () => {
        const longEnUSFormatter = new Intl.DateTimeFormat('en-US', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });

        const date = new Date();

        const scheduling: RangeOfDateAndTime = {
            startDate: date.getTime(),
            endDate: date.getTime()
        };

        spectator.setInput({
            scheduling
        });

        expect(spectator.query(byTestId('schedule-start-date'))).toHaveText(
            longEnUSFormatter.format(date)
        );
        expect(spectator.query(byTestId('schedule-end-date'))).toHaveText(
            longEnUSFormatter.format(date)
        );
    });

    it('should rendered suggestedWinner input', () => {
        const suggestedWinner: SummaryLegend = suggestedWinnerMock;
        spectator.setInput({
            suggestedWinner
        });

        expect(spectator.query(byTestId('suggested-winner-icon'))).toHaveClass(
            suggestedWinner.icon
        );
        expect(spectator.query(byTestId('suggested-winner-legend'))).toHaveText(
            suggestedWinner.legend
        );
    });
    it('should rendered session reached number', () => {
        const sessionsReached = 50;
        spectator.setInput({
            sessionsReached
        });
        expect(spectator.query(byTestId('summary-sessions-reached'))).toHaveText(
            sessionsReached.toString()
        );
    });
});
