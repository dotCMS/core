import { Dispatcher } from '@ngrx/signals/events';
import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';
import { Subject } from 'rxjs';

import { Injector, WritableSignal, signal } from '@angular/core';
import { applyEach, disabled, FieldTree, form, max, min, validate } from '@angular/forms/signals';

import { Confirmation, ConfirmationService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { Tooltip } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import {
    CONFIGURATION_CONFIRM_DIALOG_KEY,
    DEFAULT_VARIANT_NAME,
    DotExperiment,
    EXP_CONFIG_ERROR_LABEL_CANT_EDIT,
    MAX_VARIANTS_ALLOWED,
    TrafficProportionTypes,
    Variant
} from '@dotcms/dotcms-models';
import { DotCopyButtonComponent } from '@dotcms/ui';
import { getExperimentMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsConfigureVariantsComponent } from './dot-experiments-configure-variants.component';

import {
    ADD_VARIANT_DIALOG_WIDTH,
    TOTAL_WEIGHT,
    WEIGHTS_TOTAL_ERROR_KIND
} from '../../../shared/constants';
import { DotExperimentConfigurePage, VariantWeightFormRow } from '../../../shared/models';
import { dotExperimentsConfigureApiEvents } from '../../../store/dot-experiments-configure-api.events';
import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';
import { toVariantWeightRows } from '../../../util/dot-experiments-configure-form.util';
import { totalWeight } from '../../../util/dot-experiments-configure.util';
import {
    DotExperimentsAddVariantDialogComponent,
    DotExperimentsAddVariantDialogResult
} from '../dot-experiments-add-variant-dialog/dot-experiments-add-variant-dialog.component';

/** The two-variant experiment of the shared mocks: `Original` (control) plus one variant. */
const EXPERIMENT: DotExperiment = getExperimentMock(1);
const [CONTROL_VARIANT, SECOND_VARIANT] = EXPERIMENT.trafficProportion.variants;
const THIRD_VARIANT: Variant = { ...SECOND_VARIANT, id: '222', name: 'variant b', weight: 0 };

/** What the store answers with once a third variant has been created, weights and all. */
const THREE_VARIANT_EXPERIMENT: DotExperiment = {
    ...EXPERIMENT,
    trafficProportion: {
        ...EXPERIMENT.trafficProportion,
        variants: [CONTROL_VARIANT, SECOND_VARIANT, THIRD_VARIANT]
    }
};

const SELECTED_PAGE: DotExperimentConfigurePage = {
    pageId: EXPERIMENT.pageId,
    title: 'Blog',
    path: '/blog/index'
};

const ADD_DIALOG_HEADER = 'Add Variant';
const CAP_REACHED_COPY = 'Maximum number of variants reached';
const EDIT_CONTENT_UNAVAILABLE_COPY = 'Available soon';
const CANT_EDIT_COPY = 'Only a draft experiment can be edited';
const HINT_COPY = 'Up to {0} variants';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.variants.add-dialog.header': ADD_DIALOG_HEADER,
    'experiments.configure.variants.cap-reached': CAP_REACHED_COPY,
    'experiments.configure.variants.edit-content.unavailable': EDIT_CONTENT_UNAVAILABLE_COPY,
    'experiments.configure.variants.control-chip': 'CONTROL',
    'experiments.configure.variants.action.preview': 'Preview',
    'experiments.configure.variants.action.edit': 'Edit',
    'experiments.configure.variants.error.min': 'Add at least one variant',
    'experiments.configure.variants.weights.warning': 'The weights add up to {0}%',
    'experiments.configure.variants.hint': HINT_COPY,
    'experiments.configure.variants.hint.locked': 'Locked',
    'experiments.configure.variants.share-of-all': '{0}%',
    [EXP_CONFIG_ERROR_LABEL_CANT_EDIT]: CANT_EDIT_COPY
});

/**
 * The card reads the store and never writes to it, so every signal is a plain `jest.fn()` whose
 * value each test decides before the component is created. The weights are not among them: they
 * arrive through the `weights` input, as a real slice of a real form.
 */
const createStoreMock = () => ({
    experiment: jest.fn().mockReturnValue(EXPERIMENT),
    $variants: jest.fn().mockReturnValue([CONTROL_VARIANT, SECOND_VARIANT]),
    $disabledTooltipKey: jest.fn().mockReturnValue(null),
    $validationErrors: jest.fn().mockReturnValue([]),
    selectedPage: jest.fn().mockReturnValue(SELECTED_PAGE),
    // What the "of all traffic" column multiplies each split against.
    $trafficAllocation: jest.fn().mockReturnValue(100)
});

describe('DotExperimentsConfigureVariantsComponent', () => {
    let spectator: Spectator<DotExperimentsConfigureVariantsComponent>;
    let storeMock: ReturnType<typeof createStoreMock>;
    let dispatch: jest.SpyInstance;
    let confirm: jest.SpyInstance;
    let dialogClosed: Subject<DotExperimentsAddVariantDialogResult | undefined>;
    let dialogServiceMock: { open: jest.Mock };
    let weights: WritableSignal<VariantWeightFormRow[]>;
    let weightsField: FieldTree<VariantWeightFormRow[]>;

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

    /**
     * Renders the card on a real weights slice carrying the card's own schema — the same rules the
     * shell applies to `path.variantWeights`, read-only state included, so the range rules and the
     * total are the real ones.
     *
     * The rows default to the weights the store holds, which is what the shell seeds them from.
     */
    let proportionType: WritableSignal<TrafficProportionTypes>;
    let typeField: FieldTree<TrafficProportionTypes>;

    const render = (rows: VariantWeightFormRow[] = toVariantWeightRows(storeMock.$variants())) => {
        weights = signal(rows);
        // The shell's rules for this slice, restated — including that the weights follow the page's
        // lock and not only the status. The card is only meaningful on a slice that carries them.
        weightsField = form(
            weights,
            (path) => {
                const isLocked = () => !!storeMock.$disabledTooltipKey();

                applyEach(path, (row) => {
                    min(row.weight, 0);
                    max(row.weight, TOTAL_WEIGHT);
                    disabled(row, { when: isLocked });
                });

                validate(path, ({ value }) => {
                    const rows = value();

                    if (!rows.length || totalWeight(rows) === TOTAL_WEIGHT) {
                        return undefined;
                    }

                    return {
                        kind: WEIGHTS_TOTAL_ERROR_KIND,
                        message: messageServiceMock.get(
                            'experiments.configure.variants.weights.warning',
                            String(totalWeight(rows))
                        )
                    };
                });
            },
            { injector: spectator.inject(Injector) }
        );

        // The proportion's type is its own slice of the shell's form; the card writes it when the
        // user picks a split, so the spec supplies a real field rather than a stub.
        proportionType = signal(TrafficProportionTypes.SPLIT_EVENLY);
        typeField = form(proportionType, { injector: spectator.inject(Injector) });

        spectator.setInput('field', weightsField);
        spectator.setInput('typeField', typeField);
        spectator.detectChanges();
    };

    /**
     * The slice as plain rows. Signal forms brand the items it writes back with a tracking symbol,
     * which a deep comparison would otherwise report as a difference.
     */
    const weightRows = (): VariantWeightFormRow[] =>
        weights().map(({ id, weight }) => ({ id, weight }));

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
            render();

            expect(rows().length).toBe(2);
        });

        it('should render the control as static text with its chip, and never as an editor', () => {
            render();

            expect(queryIn(0, 'variant-control-name')?.textContent).toContain(CONTROL_VARIANT.name);
            expect(queryIn(0, 'variant-control-chip')?.textContent).toContain('CONTROL');
            expect(queryIn(0, 'variant-name-inplace')).toBeNull();
        });

        it('should render every other variant through the inplace editor', () => {
            render();

            expect(queryIn(1, 'variant-control-chip')).toBeNull();
            expect(queryIn(1, 'variant-name-inplace')?.textContent).toContain(SECOND_VARIANT.name);
        });

        it('should give each row its own colour dot', () => {
            render();

            const dots = rows().map(
                (row) => (row.querySelector('span[aria-hidden="true"]') as HTMLElement).style
            );

            expect(dots[0].background).not.toBe('');
            expect(dots[1].background).not.toBe('');
            expect(dots[0].background).not.toBe(dots[1].background);
        });

        it('should render each weight in its own input, bound to its row of the slice', () => {
            render();

            const rendered = spectator
                .queryAll<HTMLInputElement>(byTestId('variant-weight-input'))
                .map(({ value }) => value);

            expect(rendered).toEqual([
                String(CONTROL_VARIANT.weight),
                String(SECOND_VARIANT.weight)
            ]);
        });
    });

    /**
     * On `/experiments/new` there is no experiment to derive rows from, but the creation POST will
     * have the backend create the Original variant at 100% — so the card states that instead of
     * rendering headers over nothing (#37003).
     */
    describe('creation screen, before the experiment exists', () => {
        /** The store as the creation screen leaves it: no experiment, so no variants either. */
        const renderBeforeCreation = () => {
            storeMock.experiment.mockReturnValue(null);
            storeMock.$variants.mockReturnValue([]);
            render();
        };

        it('should render the Original row the POST will create, and nothing else', () => {
            renderBeforeCreation();

            expect(rows().length).toBe(1);
            expect(queryIn(0, 'variant-control-name')?.textContent).toContain(DEFAULT_VARIANT_NAME);
            expect(queryIn(0, 'variant-control-chip')?.textContent).toContain('CONTROL');
        });

        it('should state the weight as the whole of the traffic, and freeze it', () => {
            renderBeforeCreation();

            const input = spectator.query<HTMLInputElement>(byTestId('variant-weight-input'));

            // No form field stands behind the row, so the weight is read-only text in an input.
            expect(input?.value).toBe('100');
            expect(input?.disabled).toBe(true);
            // Frozen because there is nothing to persist to yet, which is not a lock to explain.
            expect(tooltipOf('variant-weight-tooltip').disabled).toBe(true);
        });

        it('should total the whole of the traffic without warning about it', () => {
            renderBeforeCreation();

            expect(spectator.query(byTestId('variants-total-weight'))?.textContent).toContain(
                '100%'
            );
            expect(spectator.query(byTestId('variants-weight-warning'))).toBeNull();
        });

        it('should offer no action that would need a server entity behind the row', () => {
            renderBeforeCreation();

            expect(queryIn(0, 'variant-name-inplace')).toBeNull();
            expect(queryIn(0, 'variant-actions-btn')).toBeNull();
            expect(queryIn(0, 'variant-copy-url')).toBeNull();
        });

        it('should render the row action as a disabled Preview, as the real control row does', () => {
            renderBeforeCreation();

            expect(queryIn(0, 'variant-edit-content-btn')?.textContent).toContain('Preview');
            expect(isRowButtonDisabled(0, 'variant-edit-content-btn')).toBe(true);
        });

        it('should disable Split Evenly: there is nothing to split yet', () => {
            renderBeforeCreation();

            expect(isButtonDisabled('variants-split-evenly-btn')).toBe(true);
        });

        it('should disable Add, leaving the n/max hint to give the context', () => {
            renderBeforeCreation();

            expect(isButtonDisabled('variants-add-btn')).toBe(true);
            // No cap has been reached, so the cap tooltip stays out of the way.
            expect(tooltipOf('variants-add-btn').disabled).toBe(true);
            expect(spectator.query(byTestId('variants-hint'))?.textContent).toContain(
                `Up to ${MAX_VARIANTS_ALLOWED} variants`
            );
        });

        it('should never draw the row alongside the real ones once the experiment exists', () => {
            // Default mocks: the created experiment. Its control is `Original` too, so the row is
            // told apart by the persisted weight it carries and by being editable.
            render();

            expect(rows().length).toBe(2);

            const controlWeight = spectator.query<HTMLInputElement>(
                byTestId('variant-weight-input')
            );

            expect(controlWeight?.value).toBe(String(CONTROL_VARIANT.weight));
            expect(controlWeight?.disabled).toBe(false);
            expect(isButtonDisabled('variants-add-btn')).toBe(false);
            expect(isButtonDisabled('variants-split-evenly-btn')).toBe(false);
        });
    });

    describe('deleting', () => {
        /** The kebab is where Delete lives, so its entries are what the tests reach for. */
        const openRowMenu = (rowIndex: number) => {
            clickButton('variant-actions-btn', rows()[rowIndex]);

            return spectator.component.$rowMenuItems();
        };

        it('should offer no kebab on the control variant, which is never deleted', () => {
            render();

            expect(queryIn(0, 'variant-actions-btn')).toBeNull();
            expect(queryIn(1, 'variant-actions-btn')).not.toBeNull();
        });

        it('should offer no kebab while the card is disabled', () => {
            storeMock.$disabledTooltipKey.mockReturnValue(EXP_CONFIG_ERROR_LABEL_CANT_EDIT);
            render();

            expect(spectator.query(byTestId('variant-actions-btn'))).toBeNull();
        });

        it('should offer Preview and Delete, with Preview waiting on the editor', () => {
            render();

            const items = openRowMenu(1);

            expect(items.map(({ id }) => id)).toEqual(['variant-preview', 'variant-delete']);
            expect(items[0].disabled).toBe(true);
        });

        it('should confirm on the shell dialog before dispatching variantDeleted', () => {
            render();

            const items = openRowMenu(1);
            items.find(({ id }) => id === 'variant-delete')?.command?.({} as never);
            spectator.detectChanges();

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

    /**
     * The weights are a slice of the shell's form, so what the card is responsible for is the two
     * directions of that binding: the inputs write the slice, and everything the card renders about
     * the weights is read back from it — including the rules, which live in its schema.
     */
    describe('weights', () => {
        const weightInput = (rowIndex: number): HTMLInputElement =>
            spectator.queryAll<HTMLInputElement>(byTestId('variant-weight-input'))[rowIndex];

        const typeWeight = (rowIndex: number, value: string) => {
            const input = weightInput(rowIndex);
            input.value = value;
            spectator.dispatchFakeEvent(input, 'input');
            spectator.detectChanges();
        };

        /**
         * The server stores an even split across three as 33.33/33.33/33.34, and the inputs are
         * whole percentages — so the rows are rounded on the way in.
         */
        describe('a stored split with decimals', () => {
            const thirds = (a: number, b: number, c: number): Variant[] => [
                { ...CONTROL_VARIANT, weight: a },
                { ...SECOND_VARIANT, weight: b },
                { ...THIRD_VARIANT, weight: c }
            ];

            it('should read as whole percentages', () => {
                storeMock.$variants.mockReturnValue(thirds(33.33, 33.33, 33.34));
                render();

                expect([0, 1, 2].map((row) => weightInput(row).value)).toEqual(['34', '33', '33']);
            });

            it('should still add up to 100, so nothing is flagged', () => {
                // Rounding each of them alone gives 99, and the card would warn about a split the
                // user never touched.
                storeMock.$variants.mockReturnValue(thirds(33.33, 33.33, 33.34));
                render();

                expect(weightRows().reduce((sum, { weight }) => sum + (weight ?? 0), 0)).toBe(100);
                expect(spectator.query(byTestId('variants-weight-warning'))).toBeNull();
            });

            it('should leave a proportion that never added up reading as broken', () => {
                storeMock.$variants.mockReturnValue(thirds(20, 20, 20));
                render();

                expect([0, 1, 2].map((row) => weightInput(row).value)).toEqual(['20', '20', '20']);
                expect(spectator.query(byTestId('variants-weight-warning'))).not.toBeNull();
            });
        });

        it('should write a typed weight into its row of the slice', () => {
            render();

            typeWeight(1, '30');

            expect(weightRows()).toEqual([
                { id: CONTROL_VARIANT.id, weight: CONTROL_VARIANT.weight },
                { id: SECOND_VARIANT.id, weight: 30 }
            ]);
        });

        it('should leave every other row alone', () => {
            render();

            typeWeight(0, '70');

            expect(weightInput(1).value).toBe(String(SECOND_VARIANT.weight));
        });

        /**
         * The weights only ever travel as a set that adds up to 100 — the backend rejects anything
         * else on construction — so the row just committed decides its own share and the rest is
         * spread over the others, rather than left for the user to work out.
         */
        describe('completing the split on commit', () => {
            const commitWeight = (rowIndex: number, value: string) => {
                typeWeight(rowIndex, value);
                spectator.dispatchFakeEvent(weightInput(rowIndex), 'blur');
                spectator.detectChanges();
            };

            it('should give the rest to the only other row', () => {
                render();

                commitWeight(0, '20');

                expect(weightRows()).toEqual([
                    { id: CONTROL_VARIANT.id, weight: 20 },
                    { id: SECOND_VARIANT.id, weight: 80 }
                ]);
            });

            it('should spread the rest over the others in the proportion they had', () => {
                storeMock.experiment.mockReturnValue(THREE_VARIANT_EXPERIMENT);
                storeMock.$variants.mockReturnValue(
                    THREE_VARIANT_EXPERIMENT.trafficProportion.variants
                );
                render([
                    { id: CONTROL_VARIANT.id, weight: 50 },
                    { id: SECOND_VARIANT.id, weight: 30 },
                    { id: THIRD_VARIANT.id, weight: 20 }
                ]);

                commitWeight(0, '20');

                // The other two held 30 and 20 of 50, so they split the 80 left as 48 and 32.
                expect(weightRows()).toEqual([
                    { id: CONTROL_VARIANT.id, weight: 20 },
                    { id: SECOND_VARIANT.id, weight: 48 },
                    { id: THIRD_VARIANT.id, weight: 32 }
                ]);
            });

            it('should still add up to exactly 100 when the shares do not divide', () => {
                storeMock.experiment.mockReturnValue(THREE_VARIANT_EXPERIMENT);
                storeMock.$variants.mockReturnValue(
                    THREE_VARIANT_EXPERIMENT.trafficProportion.variants
                );
                render([
                    { id: CONTROL_VARIANT.id, weight: 40 },
                    { id: SECOND_VARIANT.id, weight: 30 },
                    { id: THIRD_VARIANT.id, weight: 30 }
                ]);

                commitWeight(0, '33');

                expect(weightRows().reduce((sum, { weight }) => sum + (weight ?? 0), 0)).toBe(
                    TOTAL_WEIGHT
                );
            });

            it('should share the rest evenly when the others hold nothing', () => {
                storeMock.experiment.mockReturnValue(THREE_VARIANT_EXPERIMENT);
                storeMock.$variants.mockReturnValue(
                    THREE_VARIANT_EXPERIMENT.trafficProportion.variants
                );
                render([
                    { id: CONTROL_VARIANT.id, weight: 100 },
                    { id: SECOND_VARIANT.id, weight: 0 },
                    { id: THIRD_VARIANT.id, weight: 0 }
                ]);

                commitWeight(0, '50');

                expect(weightRows()).toEqual([
                    { id: CONTROL_VARIANT.id, weight: 50 },
                    { id: SECOND_VARIANT.id, weight: 25 },
                    { id: THIRD_VARIANT.id, weight: 25 }
                ]);
            });

            /** Original 15, then a variant 50: the 15 was a decision, so the third row takes the 35. */
            it('should spare a row the user already set and take the rest from one they did not', () => {
                storeMock.experiment.mockReturnValue(THREE_VARIANT_EXPERIMENT);
                storeMock.$variants.mockReturnValue(
                    THREE_VARIANT_EXPERIMENT.trafficProportion.variants
                );
                render([
                    { id: CONTROL_VARIANT.id, weight: 100 },
                    { id: SECOND_VARIANT.id, weight: 0 },
                    { id: THIRD_VARIANT.id, weight: 0 }
                ]);

                commitWeight(0, '15');
                commitWeight(1, '50');

                expect(weightRows()).toEqual([
                    { id: CONTROL_VARIANT.id, weight: 15 },
                    { id: SECOND_VARIANT.id, weight: 50 },
                    { id: THIRD_VARIANT.id, weight: 35 }
                ]);
            });

            it('should move the row set longest ago once every row has been set', () => {
                storeMock.experiment.mockReturnValue(THREE_VARIANT_EXPERIMENT);
                storeMock.$variants.mockReturnValue(
                    THREE_VARIANT_EXPERIMENT.trafficProportion.variants
                );
                render([
                    { id: CONTROL_VARIANT.id, weight: 100 },
                    { id: SECOND_VARIANT.id, weight: 0 },
                    { id: THIRD_VARIANT.id, weight: 0 }
                ]);

                commitWeight(0, '15');
                commitWeight(1, '50');
                // Every row is a decision now, so the oldest of them — Original — gives way.
                commitWeight(2, '20');

                expect(weightRows()).toEqual([
                    { id: CONTROL_VARIANT.id, weight: 30 },
                    { id: SECOND_VARIANT.id, weight: 50 },
                    { id: THIRD_VARIANT.id, weight: 20 }
                ]);
            });

            it('should forget every decision once the weights are split evenly', () => {
                storeMock.experiment.mockReturnValue(THREE_VARIANT_EXPERIMENT);
                storeMock.$variants.mockReturnValue(
                    THREE_VARIANT_EXPERIMENT.trafficProportion.variants
                );
                render([
                    { id: CONTROL_VARIANT.id, weight: 100 },
                    { id: SECOND_VARIANT.id, weight: 0 },
                    { id: THIRD_VARIANT.id, weight: 0 }
                ]);

                commitWeight(0, '15');
                clickButton('variants-split-evenly-btn');
                commitWeight(1, '50');

                // With the 15 overridden by the split, the two rows left share the remainder.
                expect(weightRows()).toEqual([
                    { id: CONTROL_VARIANT.id, weight: 25 },
                    { id: SECOND_VARIANT.id, weight: 50 },
                    { id: THIRD_VARIANT.id, weight: 25 }
                ]);
            });

            it('should leave the others alone while the total already adds up', () => {
                render();

                commitWeight(0, String(CONTROL_VARIANT.weight));

                expect(weightRows()).toEqual([
                    { id: CONTROL_VARIANT.id, weight: CONTROL_VARIANT.weight },
                    { id: SECOND_VARIANT.id, weight: SECOND_VARIANT.weight }
                ]);
            });

            it('should leave the others alone on a value the range rules reject', () => {
                render();

                commitWeight(0, '120');

                expect(weightRows()[1]).toEqual({
                    id: SECOND_VARIANT.id,
                    weight: SECOND_VARIANT.weight
                });
            });

            it('should leave the others alone on a cleared row, which is not a decision yet', () => {
                render();

                commitWeight(0, '');

                expect(weightRows()[1]).toEqual({
                    id: SECOND_VARIANT.id,
                    weight: SECOND_VARIANT.weight
                });
            });
        });

        it('should carry the range of a single weight as the input own attributes', () => {
            // The schema's `min`/`max` reach the DOM through `[formField]`, so the template states
            // neither and the two can never disagree.
            render();

            expect(weightInput(0).getAttribute('min')).toBe('0');
            expect(weightInput(0).getAttribute('max')).toBe(String(TOTAL_WEIGHT));
        });

        it('should report a weight above 100 as that row failing, not the total', () => {
            render();

            typeWeight(1, '250');

            expect(weightsField[1].weight().invalid()).toBe(true);
            expect(spectator.query(byTestId('variants-weight-warning'))).not.toBeNull();
        });

        it('should report a negative weight as that row failing too', () => {
            render();

            typeWeight(1, '-10');

            expect(weightsField[1].weight().invalid()).toBe(true);
        });

        it('should keep a cleared input empty rather than snapping it to zero', () => {
            // The row is mid-edit: forcing a `0` in would fight whatever is typed next, and the
            // total no longer adding up is exactly what the warning bar is there to say.
            render();

            typeWeight(1, '');

            expect(weights()[1].weight).toBeNull();
            expect(weightInput(1).value).toBe('');
        });

        it('should disable Split Evenly while the card is disabled', () => {
            storeMock.$disabledTooltipKey.mockReturnValue(EXP_CONFIG_ERROR_LABEL_CANT_EDIT);
            render();

            expect(isButtonDisabled('variants-split-evenly-btn')).toBe(true);
        });

        it('should total what the slice holds, not what was last persisted', () => {
            render([
                { id: CONTROL_VARIANT.id, weight: 50 },
                { id: SECOND_VARIANT.id, weight: 40 }
            ]);

            expect(spectator.query(byTestId('variants-total-weight'))?.textContent).toContain(
                '90%'
            );
        });
    });

    describe('split evenly (AC23)', () => {
        /** The card as it stands with three variants, which is where the remainder shows. */
        const renderThreeVariants = () => {
            storeMock.experiment.mockReturnValue(THREE_VARIANT_EXPERIMENT);
            storeMock.$variants.mockReturnValue(
                THREE_VARIANT_EXPERIMENT.trafficProportion.variants
            );
            render();
        };

        it('should write the split into the slice, remainder on the first row', () => {
            renderThreeVariants();

            clickButton('variants-split-evenly-btn');

            expect(weightRows()).toEqual([
                { id: CONTROL_VARIANT.id, weight: 34 },
                { id: SECOND_VARIANT.id, weight: 33 },
                { id: THIRD_VARIANT.id, weight: 33 }
            ]);
        });

        it('should redraw the inputs from the slice it wrote', () => {
            renderThreeVariants();

            clickButton('variants-split-evenly-btn');

            expect(
                spectator
                    .queryAll<HTMLInputElement>(byTestId('variant-weight-input'))
                    .map(({ value }) => value)
            ).toEqual(['34', '33', '33']);
        });

        it('should record the split as SPLIT_EVENLY, which the weights alone cannot say', () => {
            // The backend redistributes a later variant only while the type is SPLIT_EVENLY
            // (`ExperimentsAPIImpl.addVariant`), and a weight alone does not say a split was asked
            // for — so the press writes the type into its own slice of the form.
            renderThreeVariants();
            proportionType.set(TrafficProportionTypes.CUSTOM_PERCENTAGES);

            clickButton('variants-split-evenly-btn');

            expect(proportionType()).toBe(TrafficProportionTypes.SPLIT_EVENLY);
        });

        it('should record a hand-typed weight as CUSTOM_PERCENTAGES', () => {
            renderThreeVariants();

            spectator.component.onWeightCommitted(CONTROL_VARIANT.id);

            expect(proportionType()).toBe(TrafficProportionTypes.CUSTOM_PERCENTAGES);
        });

        it('should leave the total adding up, whatever it was before', () => {
            storeMock.experiment.mockReturnValue(THREE_VARIANT_EXPERIMENT);
            storeMock.$variants.mockReturnValue(
                THREE_VARIANT_EXPERIMENT.trafficProportion.variants
            );
            render([
                { id: CONTROL_VARIANT.id, weight: 10 },
                { id: SECOND_VARIANT.id, weight: 10 },
                { id: THIRD_VARIANT.id, weight: 10 }
            ]);

            expect(spectator.query(byTestId('variants-weight-warning'))).not.toBeNull();

            clickButton('variants-split-evenly-btn');

            expect(spectator.query(byTestId('variants-total-weight'))?.textContent).toContain(
                '100%'
            );
            expect(spectator.query(byTestId('variants-weight-warning'))).toBeNull();
        });
    });

    /**
     * The bar reads the slice's own cross-field error, so it and the form can never disagree about
     * whether the total is wrong. Only its *colour* waits for a Start press (AC25/AC28).
     */
    describe('weights warning', () => {
        const renderWith = (weightOfSecondRow: number, $validationErrors: string[] = []) => {
            storeMock.$validationErrors.mockReturnValue($validationErrors);
            render([
                { id: CONTROL_VARIANT.id, weight: 50 },
                { id: SECOND_VARIANT.id, weight: weightOfSecondRow }
            ]);
        };

        it('should not warn while the weights add up', () => {
            renderWith(50);

            expect(spectator.query(byTestId('variants-weight-warning'))).toBeNull();
        });

        it('should warn as soon as the weights do not add up, before any Start press', () => {
            renderWith(40);

            const warning = spectator.query(byTestId('variants-weight-warning'));

            expect(warning?.textContent).toContain('The weights add up to 90%');
            // Amber, and not a scroll target: nothing has failed validation yet (AC25/AC28).
            expect(warning).toHaveClass('bg-orange-50');
            expect(warning?.getAttribute('data-error')).toBeNull();
        });

        it('should turn the warning into an error only once weightsTotal failed validation', () => {
            renderWith(40, ['weightsTotal']);

            const warning = spectator.query(byTestId('variants-weight-warning'));

            expect(warning).toHaveClass('bg-red-50');
            expect(warning).not.toHaveClass('bg-orange-50');
            expect(warning?.getAttribute('data-error')).toBe('1');
        });

        it('should not warn about an empty slice, which is what the creation screen holds', () => {
            storeMock.experiment.mockReturnValue(null);
            storeMock.$variants.mockReturnValue([]);
            render([]);

            expect(spectator.query(byTestId('variants-weight-warning'))).toBeNull();
        });
    });

    describe('minimum variants error', () => {
        it('should stay quiet until minVariants has failed validation', () => {
            storeMock.$variants.mockReturnValue([CONTROL_VARIANT]);
            render();

            expect(spectator.query(byTestId('variants-min-error'))).toBeNull();
            expect(spectator.query(byTestId('variants-hint'))).not.toBeNull();
        });

        it('should mark itself as a scroll target once minVariants failed validation', () => {
            storeMock.$variants.mockReturnValue([CONTROL_VARIANT]);
            storeMock.$validationErrors.mockReturnValue(['minVariants']);
            render();

            const error = spectator.query(byTestId('variants-min-error'));

            expect(error?.textContent).toContain('Add at least one variant');
            expect(error?.getAttribute('data-error')).toBe('1');
            // The hint gives way to the error.
            expect(spectator.query(byTestId('variants-hint'))).toBeNull();
        });
    });

    describe('adding a variant', () => {
        it('should open the dialog with the names already in use', () => {
            render();

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
            render();
            clickButton('variants-add-btn');

            dialogClosed.next({ name: 'variant b' });

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsConfigurePageEvents.variantAdded('variant b')
            );
        });

        it('should change nothing when the dialog is cancelled', () => {
            render();
            clickButton('variants-add-btn');

            dialogClosed.next(undefined);

            expect(dispatchedEvents().some(({ type }) => type.includes('variantAdded'))).toBe(
                false
            );
        });

        it('should re-split the weights once the variant has been created', () => {
            // Keyed off the *succeeded* event, and split over the list it answered with: the
            // backend chose weights of its own for the new variant, which AC24 wants evened out.
            render();

            spectator
                .inject(Dispatcher)
                .dispatch(
                    dotExperimentsConfigureApiEvents.addVariantSucceeded(THREE_VARIANT_EXPERIMENT)
                );
            spectator.detectChanges();

            expect(weightRows()).toEqual([
                { id: CONTROL_VARIANT.id, weight: 34 },
                { id: SECOND_VARIANT.id, weight: 33 },
                { id: THIRD_VARIANT.id, weight: 33 }
            ]);
        });

        it('should record the automatic re-split as SPLIT_EVENLY', () => {
            render();
            proportionType.set(TrafficProportionTypes.CUSTOM_PERCENTAGES);

            spectator
                .inject(Dispatcher)
                .dispatch(
                    dotExperimentsConfigureApiEvents.addVariantSucceeded(THREE_VARIANT_EXPERIMENT)
                );

            // Adding a variant re-splits the rows, and the type has to follow: the backend only
            // redistributes the *next* one while the proportion still says SPLIT_EVENLY.
            expect(proportionType()).toBe(TrafficProportionTypes.SPLIT_EVENLY);
        });

        it('should keep Add on screen while the card is disabled, and say why', () => {
            // Removing it would leave an absence with nowhere to read the reason, which is what
            // the legacy screen avoided by disabling rather than hiding.
            storeMock.$disabledTooltipKey.mockReturnValue(EXP_CONFIG_ERROR_LABEL_CANT_EDIT);
            render();

            expect(spectator.query(byTestId('variants-add-btn'))).not.toBeNull();
            expect(isButtonDisabled('variants-add-btn')).toBe(true);

            // The tooltip hangs off the wrapper: a disabled button emits no pointer events, so one
            // bound to the button itself could never open.
            const tooltip = tooltipOf('variants-add-tooltip');

            expect(tooltip.content).toBe(CANT_EDIT_COPY);
            expect(tooltip.disabled).toBe(false);
        });

        it('should disable Add and say why once the cap is reached', () => {
            storeMock.$variants.mockReturnValue([CONTROL_VARIANT, SECOND_VARIANT, THIRD_VARIANT]);
            render();

            expect(isButtonDisabled('variants-add-btn')).toBe(true);

            const tooltip = tooltipOf('variants-add-tooltip');

            expect(tooltip.content).toBe(CAP_REACHED_COPY);
            expect(tooltip.disabled).toBe(false);
        });

        it('should keep Add enabled below the cap, with no tooltip to explain', () => {
            render();

            expect(isButtonDisabled('variants-add-btn')).toBe(false);
            expect(tooltipOf('variants-add-btn').disabled).toBe(true);
        });
    });

    describe('preview URL', () => {
        it('should offer a copy control carrying the variant name', () => {
            render();

            const copyButtons = spectator.queryAll(DotCopyButtonComponent);

            expect(copyButtons.length).toBe(2);
            expect(copyButtons[1].copy()).toBe(
                `${window.location.origin}${SELECTED_PAGE.path}?disabledNavigateMode=true&mode=LIVE&variantName=${SECOND_VARIANT.id}`
            );
        });

        it('should offer no copy control while no page is known', () => {
            storeMock.selectedPage.mockReturnValue(null);
            render();

            expect(spectator.query(byTestId('variant-copy-url'))).toBeNull();
        });
    });

    describe('editing content', () => {
        it('should render the control row as a disabled Preview, with a reason', () => {
            render();

            expect(queryIn(0, 'variant-edit-content-btn')?.textContent).toContain('Preview');
            expect(isRowButtonDisabled(0, 'variant-edit-content-btn')).toBe(true);
            expect(tooltipsOf('variant-edit-content-tooltip')[0].content).toBe(
                EDIT_CONTENT_UNAVAILABLE_COPY
            );
        });

        it('should render every other row as a disabled Edit, with the same reason', () => {
            // The UVE round-trip is out of scope for every row, control or not (AC-Var-Edit).
            render();

            expect(queryIn(1, 'variant-edit-content-btn')?.textContent).toContain('Edit');
            expect(isRowButtonDisabled(1, 'variant-edit-content-btn')).toBe(true);
            expect(tooltipsOf('variant-edit-content-tooltip')[1].content).toBe(
                EDIT_CONTENT_UNAVAILABLE_COPY
            );
        });
    });

    describe('locked card', () => {
        beforeEach(() => {
            storeMock.$disabledTooltipKey.mockReturnValue(EXP_CONFIG_ERROR_LABEL_CANT_EDIT);
            render();
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

    /**
     * The save gate: this card writes through `PATCH /experiments/{id}`, so until the draft exists
     * there is nowhere for anything here to go.
     */
    describe('behind the save gate', () => {
        const panel = () => spectator.query(byTestId('variants-card'));

        it('should mask the card and take it out of the tab order', () => {
            render();
            spectator.setInput('gated', true);
            spectator.detectChanges();

            // The mask stops the pointer; `inert` is what stops the keyboard reaching the fields
            // underneath it.
            expect(spectator.query('.p-blockui-mask')).not.toBeNull();
            expect(panel()?.hasAttribute('inert')).toBe(true);
        });

        it('should lift both once the draft exists', () => {
            render();
            spectator.setInput('gated', false);
            spectator.detectChanges();

            expect(panel()?.hasAttribute('inert')).toBe(false);
        });
    });

    /**
     * The one thing the Split column cannot say: a weight is a share of the traffic that *enters*
     * the experiment, and the page sends only as much of its own as the allocation allows.
     */
    describe('the share of the page traffic', () => {
        const shareOf = (rowIndex: number) =>
            queryIn(rowIndex, 'variant-share-of-all')?.textContent?.trim();

        it('should read as the weight itself while the whole page enters', () => {
            storeMock.$trafficAllocation.mockReturnValue(100);
            render();

            expect(shareOf(0)).toBe(`${CONTROL_VARIANT.weight}%`);
        });

        it('should scale the weight by the allocation once part of the page is held back', () => {
            // 50% of the 92% that enters is 46% of everyone who visits the page.
            storeMock.$trafficAllocation.mockReturnValue(92);
            render([
                { id: CONTROL_VARIANT.id, weight: 50 },
                { id: SECOND_VARIANT.id, weight: 50 }
            ]);

            expect(shareOf(0)).toBe('46%');
            expect(shareOf(1)).toBe('46%');
        });
    });
});
