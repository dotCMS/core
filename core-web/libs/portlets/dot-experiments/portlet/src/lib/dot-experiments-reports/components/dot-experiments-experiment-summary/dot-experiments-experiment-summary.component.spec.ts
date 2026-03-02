import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import {
    GOAL_OPERATORS,
    GOAL_PARAMETERS,
    GOAL_TYPES,
    Goals,
    RangeOfDateAndTime,
    SummaryLegend
} from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsExperimentSummaryComponent } from './dot-experiments-experiment-summary.component';

const messageServiceMock = new MockDotMessageService({
    'experiments.summary.winner.testing': 'Testing',
    'experiments.summary.winner.session-to-date': 'Sessions to date:',
    Refresh: 'Refresh'
});

const GOALS_MOCK: Goals = {
    primary: {
        name: 'Bounce Rate',
        type: GOAL_TYPES.BOUNCE_RATE,
        conditions: [
            {
                parameter: GOAL_PARAMETERS.URL,
                operator: GOAL_OPERATORS.EQUALS,
                value: ''
            }
        ]
    }
};

const SCHEDULING_MOCK: RangeOfDateAndTime = {
    startDate: new Date('2024-01-01T00:00:00Z').getTime(),
    endDate: new Date('2024-12-31T23:59:59Z').getTime()
};

const SUGGESTED_WINNER_MOCK: SummaryLegend = {
    icon: 'dot-trophy',
    legend: 'Variant A is winning'
};

describe('DotExperimentsExperimentSummaryComponent', () => {
    let spectator: Spectator<DotExperimentsExperimentSummaryComponent>;

    const createComponent = createComponentFactory({
        component: DotExperimentsExperimentSummaryComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                goals: GOALS_MOCK,
                scheduling: SCHEDULING_MOCK,
                sessionsReached: 1234
                // Note: Using alias names (goals, scheduling, sessionsReached) instead of signal names
                // ($goals, $scheduling, $sessionsReached) because Spectator handles signal inputs
                // with aliases correctly when using the alias name in props.
            } as unknown
        });
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should rendered the goal Input', () => {
        const goalLabel = spectator.query(byTestId('goal-label'));

        expect(goalLabel).toExist();
        expect(goalLabel).toHaveText('Bounce Rate');
    });

    it('should rendered the scheduling Input', () => {
        const startDateElement = spectator.query(byTestId('schedule-start-date'));
        const endDateElement = spectator.query(byTestId('schedule-end-date'));

        expect(startDateElement).toExist();
        expect(endDateElement).toExist();

        // Check that dates are rendered (format may vary by locale)
        expect(startDateElement?.textContent?.trim()).toBeTruthy();
        expect(endDateElement?.textContent?.trim()).toBeTruthy();
    });

    it('should rendered suggestedWinner input', () => {
        spectator.setInput('suggestedWinner', SUGGESTED_WINNER_MOCK);

        const winnerIcon = spectator.query(byTestId('suggested-winner-icon'));
        const winnerLegend = spectator.query(byTestId('suggested-winner-legend'));

        expect(winnerIcon).toExist();
        expect(winnerLegend).toExist();
        expect(winnerLegend).toContainText('Variant A is winning');
    });

    it('should rendered session reached number', () => {
        const sessionsElement = spectator.query(byTestId('summary-sessions-reached'));

        expect(sessionsElement).toExist();
        expect(sessionsElement).toContainText('1,234');
    });

    it('should reload results', () => {
        let emittedValue;
        spectator.output('updateResults').subscribe((result) => (emittedValue = result));

        const reloadButton = spectator.query(byTestId('reload-button')) as HTMLButtonElement;
        spectator.click(reloadButton);

        expect(emittedValue).toBeUndefined();
        expect(reloadButton).toExist();
    });
});
