import { Dispatcher } from '@ngrx/signals/events';
import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';
import { Subject } from 'rxjs';

import { Confirmation, ConfirmationService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { Tooltip } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import {
    CONFIGURATION_CONFIRM_DIALOG_KEY,
    DotExperiment,
    EXP_CONFIG_ERROR_LABEL_CANT_EDIT,
    MAX_VARIANTS_ALLOWED,
    TrafficProportionTypes,
    Variant
} from '@dotcms/dotcms-models';
import { DotCopyButtonComponent } from '@dotcms/ui';
import { getExperimentMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsConfigureVariantsComponent } from './dot-experiments-configure-variants.component';

import { ADD_VARIANT_DIALOG_WIDTH } from '../../../shared/constants';
import { DotExperimentConfigurePage } from '../../../shared/models';
import { dotExperimentsConfigureApiEvents } from '../../../store/dot-experiments-configure-api.events';
import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';
import {
    DotExperimentsAddVariantDialogComponent,
    DotExperimentsAddVariantDialogResult
} from '../dot-experiments-add-variant-dialog/dot-experiments-add-variant-dialog.component';

/** The two-variant experiment of the shared mocks: `Original` (control) plus one variant. */
const EXPERIMENT: DotExperiment = getExperimentMock(1);
const [CONTROL_VARIANT, SECOND_VARIANT] = EXPERIMENT.trafficProportion.variants;
const THIRD_VARIANT: Variant = { ...SECOND_VARIANT, id: '222', name: 'variant b' };

const SELECTED_PAGE: DotExperimentConfigurePage = {
    pageId: EXPERIMENT.pageId,
    title: 'Blog',
    path: '/blog/index'
};

const ADD_DIALOG_HEADER = 'Add Variant';
const CAP_REACHED_COPY = 'Maximum number of variants reached';
const EDIT_CONTENT_UNAVAILABLE_COPY = 'Available soon';
const CANT_EDIT_COPY = 'Only a draft experiment can be edited';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.variants.add-dialog.header': ADD_DIALOG_HEADER,
    'experiments.configure.variants.cap-reached': CAP_REACHED_COPY,
    'experiments.configure.variants.edit-content.unavailable': EDIT_CONTENT_UNAVAILABLE_COPY,
    'experiments.configure.variants.control-chip': 'CONTROL',
    'experiments.configure.variants.meta.control': 'Original page content',
    'experiments.configure.variants.meta.variant': 'Copy of the original page',
    'experiments.configure.variants.action.preview': 'Preview',
    'experiments.configure.variants.action.edit-content': 'Edit Content',
    'experiments.configure.variants.error.min': 'Add at least one variant',
    'experiments.configure.variants.weights.warning': 'The weights add up to {0}%',
    [EXP_CONFIG_ERROR_LABEL_CANT_EDIT]: CANT_EDIT_COPY
});

/**
 * The card reads the store and never writes to it, so every signal is a plain `jest.fn()` whose
 * value each test decides before the component is created.
 */
const createStoreMock = () => ({
    $variants: jest.fn().mockReturnValue([CONTROL_VARIANT, SECOND_VARIANT]),
    $disabledTooltipKey: jest.fn().mockReturnValue(null),
    $totalWeight: jest.fn().mockReturnValue(100),
    $hasInvalidWeights: jest.fn().mockReturnValue(false),
    validationErrors: jest.fn().mockReturnValue([]),
    selectedPage: jest.fn().mockReturnValue(SELECTED_PAGE)
});

describe('DotExperimentsConfigureVariantsComponent', () => {
    let spectator: Spectator<DotExperimentsConfigureVariantsComponent>;
    let storeMock: ReturnType<typeof createStoreMock>;
    let dispatch: jest.SpyInstance;
    let confirm: jest.SpyInstance;
    let dialogClosed: Subject<DotExperimentsAddVariantDialogResult | undefined>;
    let dialogServiceMock: { open: jest.Mock };

    const createComponent = createComponentFactory({
        component: DotExperimentsConfigureVariantsComponent,
        // `componentProviders` replaces the card's own `providers`, which is exactly the
        // `DialogService` the Add Variant dialog is opened through.
        componentProviders: [{ provide: DialogService, useFactory: () => dialogServiceMock }],
        providers: [
            { provide: DotExperimentsConfigureStore, useFactory: () => storeMock },
            { provide: DotMessageService, useValue: messageServiceMock },
            ConfirmationService
        ],
        detectChanges: false
    });

    /** `injectDispatch` appends a scope argument, so only the event itself is compared. */
    const dispatchedEvents = () => dispatch.mock.calls.map(([event]) => event);

    const rows = () => spectator.queryAll(byTestId('variant-row'));

    const queryIn = (rowIndex: number, testId: string): HTMLElement | null =>
        rows()[rowIndex]?.querySelector(`[data-testid="${testId}"]`) ?? null;

    const clickButton = (testId: string, root: Element | null = spectator.element) => {
        const host = root?.querySelector(`[data-testid="${testId}"]`);
        spectator.click(host?.querySelector('button') as HTMLElement);
        spectator.detectChanges();
    };

    const isButtonDisabled = (testId: string): boolean =>
        (
            spectator.query(byTestId(testId))?.querySelector('button') as
                | HTMLButtonElement
                | undefined
        )?.disabled ?? false;

    const tooltipOf = (testId: string): Tooltip =>
        spectator.query(`[data-testid="${testId}"]`, { read: Tooltip }) as Tooltip;

    /** Same tooltip on every row, so they are read as a list and indexed by row. */
    const tooltipsOf = (testId: string): Tooltip[] =>
        spectator.queryAll(`[data-testid="${testId}"]`, { read: Tooltip }) as Tooltip[];

    const isRowButtonDisabled = (rowIndex: number, testId: string): boolean =>
        (queryIn(rowIndex, testId)?.querySelector('button') as HTMLButtonElement | undefined)
            ?.disabled ?? false;

    /** Accepts the confirmation opened by the last action and returns it. */
    const acceptConfirmation = (): Confirmation => {
        const confirmation = confirm.mock.calls[0][0] as Confirmation;
        confirmation.accept?.();

        return confirmation;
    };

    beforeEach(() => {
        storeMock = createStoreMock();
        dialogClosed = new Subject<DotExperimentsAddVariantDialogResult | undefined>();
        dialogServiceMock = { open: jest.fn().mockReturnValue({ onClose: dialogClosed }) };
        spectator = createComponent();
        dispatch = jest.spyOn(spectator.inject(Dispatcher), 'dispatch');
        const confirmationService = spectator.inject(ConfirmationService, true);
        confirm = jest
            .spyOn(confirmationService, 'confirm')
            .mockReturnValue(confirmationService) as jest.SpyInstance;
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    describe('rows', () => {
        it('should render one row per variant', () => {
            spectator.detectChanges();

            expect(rows().length).toBe(2);
        });

        it('should render the control as static text with its chip, and never as an editor', () => {
            spectator.detectChanges();

            expect(queryIn(0, 'variant-control-name')?.textContent).toContain(CONTROL_VARIANT.name);
            expect(queryIn(0, 'variant-control-chip')?.textContent).toContain('CONTROL');
            expect(queryIn(0, 'variant-name-inplace')).toBeNull();
        });

        it('should render every other variant through the inplace editor', () => {
            spectator.detectChanges();

            expect(queryIn(1, 'variant-control-chip')).toBeNull();
            expect(queryIn(1, 'variant-name-inplace')?.textContent).toContain(SECOND_VARIANT.name);
        });

        it('should tell the control and the variants apart in the meta line', () => {
            spectator.detectChanges();

            expect(queryIn(0, 'variant-meta')?.textContent).toContain('Original page content');
            expect(queryIn(1, 'variant-meta')?.textContent).toContain('Copy of the original page');
        });

        it('should give each row its own colour dot', () => {
            spectator.detectChanges();

            const dots = rows().map(
                (row) => (row.querySelector('span[aria-hidden="true"]') as HTMLElement).style
            );

            expect(dots[0].background).not.toBe('');
            expect(dots[1].background).not.toBe('');
            expect(dots[0].background).not.toBe(dots[1].background);
        });

        it('should render each weight in its own input', () => {
            spectator.detectChanges();

            const weights = spectator
                .queryAll<HTMLInputElement>(byTestId('variant-weight-input'))
                .map(({ value }) => value);

            expect(weights).toEqual([
                String(CONTROL_VARIANT.weight),
                String(SECOND_VARIANT.weight)
            ]);
        });

        it('should count the variants against the cap', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('variants-count'))?.textContent).toContain(
                `2/${MAX_VARIANTS_ALLOWED}`
            );
        });
    });

    describe('deleting', () => {
        it('should not offer a delete control on the control variant', () => {
            spectator.detectChanges();

            expect(queryIn(0, 'variant-delete-btn')).toBeNull();
            expect(queryIn(1, 'variant-delete-btn')).not.toBeNull();
        });

        it('should not offer a delete control while the card is disabled', () => {
            storeMock.$disabledTooltipKey.mockReturnValue(EXP_CONFIG_ERROR_LABEL_CANT_EDIT);
            spectator.detectChanges();

            expect(spectator.query(byTestId('variant-delete-btn'))).toBeNull();
        });

        it('should confirm on the shell dialog before dispatching variantDeleted', () => {
            spectator.detectChanges();

            clickButton('variant-delete-btn', rows()[1]);

            expect(confirm).toHaveBeenCalledTimes(1);
            expect(confirm.mock.calls[0][0].key).toBe(CONFIGURATION_CONFIRM_DIALOG_KEY);
            expect(dispatchedEvents()).not.toContainEqual(
                dotExperimentsConfigurePageEvents.variantDeleted(SECOND_VARIANT.id)
            );

            acceptConfirmation();

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsConfigurePageEvents.variantDeleted(SECOND_VARIANT.id)
            );
        });
    });

    describe('weights', () => {
        const changeWeight = (rowIndex: number, value: string) => {
            const input = spectator.queryAll<HTMLInputElement>(byTestId('variant-weight-input'))[
                rowIndex
            ];
            input.value = value;
            spectator.dispatchFakeEvent(input, 'change');
            spectator.detectChanges();
        };

        /** The last `trafficProportionChanged` payload, which is what reaches the PATCH. */
        const lastProportion = () => {
            const events = dispatchedEvents().filter(({ type }) =>
                type.includes('trafficProportionChanged')
            );

            return events[events.length - 1]?.payload;
        };

        it('should send the whole proportion, not just the row that changed', () => {
            spectator.detectChanges();

            changeWeight(1, '30');

            expect(lastProportion()).toEqual({
                type: TrafficProportionTypes.CUSTOM_PERCENTAGES,
                variants: [CONTROL_VARIANT, { ...SECOND_VARIANT, weight: 30 }]
            });
        });

        it('should clamp a weight above 100', () => {
            spectator.detectChanges();

            changeWeight(1, '250');

            expect(lastProportion().variants[1].weight).toBe(100);
        });

        it('should clamp a negative weight to zero', () => {
            spectator.detectChanges();

            changeWeight(1, '-10');

            expect(lastProportion().variants[1].weight).toBe(0);
        });

        it('should read a cleared input as zero rather than NaN', () => {
            spectator.detectChanges();

            changeWeight(1, '');

            expect(lastProportion().variants[1].weight).toBe(0);
        });

        it('should dispatch splitEvenly when Split Evenly is pressed', () => {
            spectator.detectChanges();

            clickButton('variants-split-evenly-btn');

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsConfigurePageEvents.splitEvenly()
            );
        });

        it('should disable Split Evenly while the card is disabled', () => {
            storeMock.$disabledTooltipKey.mockReturnValue(EXP_CONFIG_ERROR_LABEL_CANT_EDIT);
            spectator.detectChanges();

            expect(isButtonDisabled('variants-split-evenly-btn')).toBe(true);
        });

        it('should render the total of the weights', () => {
            storeMock.$totalWeight.mockReturnValue(90);
            spectator.detectChanges();

            expect(spectator.query(byTestId('variants-total-weight'))?.textContent).toContain(
                '90%'
            );
        });
    });

    describe('weights warning', () => {
        const renderWith = (totalWeight: number, validationErrors: string[] = []) => {
            storeMock.$totalWeight.mockReturnValue(totalWeight);
            storeMock.$hasInvalidWeights.mockReturnValue(totalWeight !== 100);
            storeMock.validationErrors.mockReturnValue(validationErrors);
            spectator.detectChanges();
        };

        it('should not warn while the weights add up', () => {
            renderWith(100);

            expect(spectator.query(byTestId('variants-weight-warning'))).toBeNull();
        });

        it('should warn as soon as the weights do not add up, before any Start press', () => {
            renderWith(90);

            const warning = spectator.query(byTestId('variants-weight-warning'));

            expect(warning?.textContent).toContain('The weights add up to 90%');
            // Amber, and not a scroll target: nothing has failed validation yet (AC25/AC28).
            expect(warning).toHaveClass('bg-orange-50');
            expect(warning?.getAttribute('data-error')).toBeNull();
        });

        it('should turn the warning into an error only once weightsTotal failed validation', () => {
            renderWith(90, ['weightsTotal']);

            const warning = spectator.query(byTestId('variants-weight-warning'));

            expect(warning).toHaveClass('bg-red-50');
            expect(warning).not.toHaveClass('bg-orange-50');
            expect(warning?.getAttribute('data-error')).toBe('1');
        });
    });

    describe('minimum variants error', () => {
        it('should stay quiet until minVariants has failed validation', () => {
            storeMock.$variants.mockReturnValue([CONTROL_VARIANT]);
            spectator.detectChanges();

            expect(spectator.query(byTestId('variants-min-error'))).toBeNull();
            expect(spectator.query(byTestId('variants-hint'))).not.toBeNull();
        });

        it('should mark itself as a scroll target once minVariants failed validation', () => {
            storeMock.$variants.mockReturnValue([CONTROL_VARIANT]);
            storeMock.validationErrors.mockReturnValue(['minVariants']);
            spectator.detectChanges();

            const error = spectator.query(byTestId('variants-min-error'));

            expect(error?.textContent).toContain('Add at least one variant');
            expect(error?.getAttribute('data-error')).toBe('1');
            // The hint gives way to the error.
            expect(spectator.query(byTestId('variants-hint'))).toBeNull();
        });
    });

    describe('adding a variant', () => {
        it('should open the dialog with the names already in use', () => {
            spectator.detectChanges();

            clickButton('variants-add-btn');

            expect(dialogServiceMock.open).toHaveBeenCalledWith(
                DotExperimentsAddVariantDialogComponent,
                expect.objectContaining({
                    header: ADD_DIALOG_HEADER,
                    width: ADD_VARIANT_DIALOG_WIDTH,
                    data: { existingNames: [CONTROL_VARIANT.name, SECOND_VARIANT.name] }
                })
            );
        });

        it('should dispatch variantAdded with the name the dialog closed with', () => {
            spectator.detectChanges();
            clickButton('variants-add-btn');

            dialogClosed.next({ name: 'variant b' });

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsConfigurePageEvents.variantAdded('variant b')
            );
        });

        it('should change nothing when the dialog is cancelled', () => {
            spectator.detectChanges();
            clickButton('variants-add-btn');

            dialogClosed.next(undefined);

            expect(dispatchedEvents().some(({ type }) => type.includes('variantAdded'))).toBe(
                false
            );
        });

        it('should re-split the weights once the variant has been created', () => {
            // Keyed off the *succeeded* event: only then does the store hold the new list (AC24).
            spectator.detectChanges();

            spectator
                .inject(Dispatcher)
                .dispatch(dotExperimentsConfigureApiEvents.addVariantSucceeded(EXPERIMENT));

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsConfigurePageEvents.splitEvenly()
            );
        });

        it('should not offer the Add control while the card is disabled', () => {
            storeMock.$disabledTooltipKey.mockReturnValue(EXP_CONFIG_ERROR_LABEL_CANT_EDIT);
            spectator.detectChanges();

            expect(spectator.query(byTestId('variants-add-btn'))).toBeNull();
        });

        it('should disable Add and say why once the cap is reached', () => {
            storeMock.$variants.mockReturnValue([CONTROL_VARIANT, SECOND_VARIANT, THIRD_VARIANT]);
            spectator.detectChanges();

            expect(isButtonDisabled('variants-add-btn')).toBe(true);

            const tooltip = tooltipOf('variants-add-btn');

            expect(tooltip.content).toBe(CAP_REACHED_COPY);
            expect(tooltip.disabled).toBe(false);
        });

        it('should keep Add enabled below the cap, with no tooltip to explain', () => {
            spectator.detectChanges();

            expect(isButtonDisabled('variants-add-btn')).toBe(false);
            expect(tooltipOf('variants-add-btn').disabled).toBe(true);
        });
    });

    describe('preview URL', () => {
        it('should offer a copy control carrying the variant name', () => {
            spectator.detectChanges();

            const copyButtons = spectator.queryAll(DotCopyButtonComponent);

            expect(copyButtons.length).toBe(2);
            expect(copyButtons[1].copy()).toBe(
                `${window.location.origin}${SELECTED_PAGE.path}?disabledNavigateMode=true&mode=LIVE&variantName=${SECOND_VARIANT.id}`
            );
        });

        it('should offer no copy control while no page is known', () => {
            storeMock.selectedPage.mockReturnValue(null);
            spectator.detectChanges();

            expect(spectator.query(byTestId('variant-copy-url'))).toBeNull();
        });
    });

    describe('editing content', () => {
        it('should render the control row as a disabled Preview, with a reason', () => {
            spectator.detectChanges();

            expect(queryIn(0, 'variant-edit-content-btn')?.textContent).toContain('Preview');
            expect(isRowButtonDisabled(0, 'variant-edit-content-btn')).toBe(true);
            expect(tooltipsOf('variant-edit-content-tooltip')[0].content).toBe(
                EDIT_CONTENT_UNAVAILABLE_COPY
            );
        });

        it('should render every other row as a disabled Edit Content, with the same reason', () => {
            // The UVE round-trip is out of scope for every row, control or not (AC-Var-Edit).
            spectator.detectChanges();

            expect(queryIn(1, 'variant-edit-content-btn')?.textContent).toContain('Edit Content');
            expect(isRowButtonDisabled(1, 'variant-edit-content-btn')).toBe(true);
            expect(tooltipsOf('variant-edit-content-tooltip')[1].content).toBe(
                EDIT_CONTENT_UNAVAILABLE_COPY
            );
        });
    });

    describe('locked card', () => {
        beforeEach(() => {
            storeMock.$disabledTooltipKey.mockReturnValue(EXP_CONFIG_ERROR_LABEL_CANT_EDIT);
            spectator.detectChanges();
        });

        it('should freeze the weight inputs and explain why', () => {
            const input = spectator.query<HTMLInputElement>(byTestId('variant-weight-input'));

            expect(input?.disabled).toBe(true);
            expect(tooltipOf('variant-weight-tooltip').content).toBe(CANT_EDIT_COPY);
        });

        it('should render the names as static text', () => {
            expect(spectator.query(byTestId('variant-name-edit-btn'))).toBeNull();
        });
    });
});
