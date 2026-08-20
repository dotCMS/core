import { Dispatcher } from '@ngrx/signals/events';
import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { signal } from '@angular/core';

import { Confirmation, ConfirmationService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID, DotExperimentStatus, Variant } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsResultsSummaryTableComponent } from './dot-experiments-results-summary-table.component';

import { DotExperimentResultVariantDetail } from '../../../shared/models';
import { dotExperimentsResultsPageEvents } from '../../../store/dot-experiments-results-page.events';
import { DotExperimentsResultsStore } from '../../../store/dot-experiments-results.store';

const NO_LIFT = '—';
const VARIANT_ID = 'variant-b';

const ABOVE_THRESHOLD_COPY = 'The result clears the 95% threshold.';
const BELOW_THRESHOLD_COPY = 'Below the 95% threshold.';
const ENDS_EXPERIMENT_COPY = 'Promoting now ends the Experiment automatically.';

const messageServiceMock = new MockDotMessageService({
    'experiments.promote.variant': 'Variant',
    'experiments.reports.sessions': 'Sessions',
    'experiments.reports.conversions': 'Conversions',
    'experiments.reports.conversions.rate': 'Conversion Rate',
    'experiments.reports.probability.best': 'Probability to be Best',
    'experiments.reports.conversion.rate.range': 'Conversion Rate Range (95%)',
    'experiments.reports.promote': 'Promote',
    'experiments.results.summary.column.lift': 'Lift vs Original',
    'experiments.results.summary.chip.leading': 'LEADING',
    'experiments.results.summary.chip.promoted': 'Promoted',
    'experiments.configure.variants.control-chip': 'CONTROL',
    'experiments.results.promote.confirm.header': 'Promote Variant',
    'experiments.results.promote.confirm.above-threshold': ABOVE_THRESHOLD_COPY,
    'experiments.results.promote.confirm.below-threshold': BELOW_THRESHOLD_COPY,
    'experiments.results.promote.confirm.ends-experiment': ENDS_EXPERIMENT_COPY,
    'dot.common.dialog.reject': 'Cancel'
});

/** The control, always expected first however the results happen to order it. */
const CONTROL_ROW: DotExperimentResultVariantDetail = {
    id: DEFAULT_VARIANT_ID,
    name: 'Original',
    conversions: 12,
    conversionRate: '12%',
    conversionRateRange: '9% to 15%',
    sessions: 100,
    probabilityToBeBest: '20%',
    isWinner: false,
    isPromoted: false,
    liftVsOriginal: NO_LIFT,
    liftTone: 'neutral'
};

/** A variant with far fewer sessions than the control: the gate is experiment-wide, not per row. */
const VARIANT_ROW: DotExperimentResultVariantDetail = {
    id: VARIANT_ID,
    name: 'Variant B',
    conversions: 2,
    conversionRate: '25%',
    conversionRateRange: '10% to 40%',
    sessions: 8,
    probabilityToBeBest: '96%',
    isWinner: true,
    isPromoted: false,
    liftVsOriginal: '+13.0 pts',
    liftTone: 'positive'
};

const PROMOTED_VARIANT: Variant = {
    id: VARIANT_ID,
    name: 'Variant B',
    weight: 50,
    promoted: true,
    url: ''
};

/**
 * Real signals, not `jest.fn()`: the component is OnPush, so a plain mock whose return value is
 * swapped after the first render never reaches the template. `set()` marks it dirty the way the
 * real store does.
 */
const createStoreMock = () => ({
    $detailData: signal<DotExperimentResultVariantDetail[]>([VARIANT_ROW, CONTROL_ROW]),
    $hasEnoughSessionsForTable: signal(true),
    $promotedVariant: signal<Variant | null>(null),
    $status: signal(DotExperimentStatus.RUNNING),
    $isLoading: signal(false),
    $isSaving: signal(false)
});

describe('DotExperimentsResultsSummaryTableComponent', () => {
    let spectator: Spectator<DotExperimentsResultsSummaryTableComponent>;
    let storeMock: ReturnType<typeof createStoreMock>;
    let dispatch: jest.SpyInstance;
    let confirm: jest.SpyInstance;

    const createComponent = createComponentFactory({
        component: DotExperimentsResultsSummaryTableComponent,
        providers: [
            { provide: DotExperimentsResultsStore, useFactory: () => storeMock },
            { provide: DotMessageService, useValue: messageServiceMock }
        ],
        detectChanges: false
    });

    /** `injectDispatch` appends a scope argument, so only the event itself is compared. */
    const dispatchedEvents = () => dispatch.mock.calls.map(([event]) => event);

    const textsOf = (testId: string): string[] =>
        spectator.queryAll(byTestId(testId)).map((element) => element.textContent?.trim() ?? '');

    const clickPromote = (index = 0) => {
        const host = spectator.queryAll(byTestId('summary-row-promote-btn'))[index];
        spectator.click(host?.querySelector('button') as HTMLElement);
        spectator.detectChanges();
    };

    /** Accepts the confirmation opened by the last Promote and returns it. */
    const acceptConfirmation = (): Confirmation => {
        const confirmation = confirm.mock.calls[0][0] as Confirmation;
        confirmation.accept?.();

        return confirmation;
    };

    beforeEach(() => {
        storeMock = createStoreMock();
        spectator = createComponent();
        dispatch = jest.spyOn(spectator.inject(Dispatcher), 'dispatch');
        const confirmationService = spectator.inject(ConfirmationService, true);
        confirm = jest
            .spyOn(confirmationService, 'confirm')
            .mockReturnValue(confirmationService) as jest.SpyInstance;
        spectator.detectChanges();
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    describe('session gate', () => {
        it('replaces the whole table with one empty state below the threshold', () => {
            storeMock.$hasEnoughSessionsForTable.set(false);
            spectator.detectChanges();

            expect(spectator.queryAll(byTestId('empty-template')).length).toBe(1);
            expect(spectator.queryAll(byTestId('detail-row')).length).toBe(0);
        });

        it('shows every row in full above the threshold, however few sessions a row saw', () => {
            expect(spectator.query(byTestId('empty-template'))).toBeNull();
            expect(spectator.queryAll(byTestId('detail-row')).length).toBe(2);
            expect(textsOf('summary-row-sessions')).toEqual(['100', '8']);
            expect(textsOf('summary-row-conversion-rate')).toEqual(['12%', '25%']);
        });
    });

    describe('rows', () => {
        it('draws the control first whatever order the results arrive in', () => {
            expect(textsOf('summary-row-conversions')).toEqual(['12', '2']);
            expect(spectator.queryAll(byTestId('summary-row-control-chip')).length).toBe(1);
        });

        it('renders the lift exactly as it was built, em dash included', () => {
            expect(textsOf('summary-row-lift')).toEqual([NO_LIFT, '+13.0 pts']);
        });

        it('colours the lift by its tone', () => {
            const [control, variant] = spectator.queryAll(byTestId('summary-row-lift'));

            expect(control).toHaveClass('text-surface-400');
            expect(variant).toHaveClass('text-green-800');
        });

        it('chips the backend-suggested winner as leading', () => {
            expect(spectator.queryAll(byTestId('summary-row-leading-chip')).length).toBe(1);
        });
    });

    describe('promote', () => {
        it('offers Promote on every variant but the control', () => {
            expect(spectator.queryAll(byTestId('summary-row-promote-btn')).length).toBe(1);
        });

        it('dispatches promoteRequested with the variant id once confirmed', () => {
            clickPromote();
            acceptConfirmation();

            expect(dispatchedEvents()).toEqual([
                dotExperimentsResultsPageEvents.promoteRequested(VARIANT_ID)
            ]);
        });

        it('says the experiment will be ended while it is still running', () => {
            clickPromote();

            expect(acceptConfirmation().message).toBe(
                `${ABOVE_THRESHOLD_COPY} ${ENDS_EXPERIMENT_COPY}`
            );
        });

        it('omits the ending copy once the experiment has ended', () => {
            storeMock.$status.set(DotExperimentStatus.ENDED);
            spectator.detectChanges();
            clickPromote();

            expect(acceptConfirmation().message).toBe(ABOVE_THRESHOLD_COPY);
        });

        it('warns when the result has not cleared the threshold', () => {
            storeMock.$detailData.set([CONTROL_ROW, { ...VARIANT_ROW, isWinner: false }]);
            storeMock.$status.set(DotExperimentStatus.ENDED);
            spectator.detectChanges();
            clickPromote();

            expect(acceptConfirmation().message).toBe(BELOW_THRESHOLD_COPY);
        });

        it('chips the promoted variant and offers Promote nowhere once one has been promoted', () => {
            storeMock.$promotedVariant.set(PROMOTED_VARIANT);
            storeMock.$detailData.set([CONTROL_ROW, { ...VARIANT_ROW, isPromoted: true }]);
            spectator.detectChanges();

            expect(spectator.queryAll(byTestId('summary-row-promoted-chip')).length).toBe(1);
            expect(spectator.queryAll(byTestId('summary-row-promote-btn')).length).toBe(0);
        });
    });
});
