import { Events, injectDispatch } from '@ngrx/signals/events';

import { Component, computed, DestroyRef, inject, input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FieldTree, FormField } from '@angular/forms/signals';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Card } from 'primeng/card';
import { DialogService } from 'primeng/dynamicdialog';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import {
    CONFIGURATION_CONFIRM_DIALOG_KEY,
    DEFAULT_VARIANT_ID,
    DEFAULT_VARIANT_NAME,
    MAX_INPUT_TITLE_LENGTH,
    TrafficProportionTypes,
    Variant
} from '@dotcms/dotcms-models';
import { DotCopyButtonComponent, DotMessagePipe } from '@dotcms/ui';

import { DotExperimentsVariantNameInplaceComponent } from './dot-experiments-variant-name-inplace.component';

import {
    ADD_VARIANT_DIALOG_WIDTH,
    MAX_VARIANTS_ALLOWED,
    TOTAL_WEIGHT,
    WEIGHTS_TOTAL_ERROR_KIND
} from '../../../shared/constants';
import { VariantRowViewModel, VariantWeightFormRow } from '../../../shared/models';
import { dotExperimentsConfigureApiEvents } from '../../../store/dot-experiments-configure-api.events';
import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';
import {
    mergeVariantWeights,
    toVariantWeightRows
} from '../../../util/dot-experiments-configure-form.util';
import { splitWeightsEvenly, totalWeight } from '../../../util/dot-experiments-configure.util';
import {
    DotExperimentsAddVariantDialogComponent,
    DotExperimentsAddVariantDialogData,
    DotExperimentsAddVariantDialogResult
} from '../dot-experiments-add-variant-dialog/dot-experiments-add-variant-dialog.component';

/**
 * Row colours, in the order the rows are drawn. Defined here rather than in `shared/`: the palette
 * is this card's presentation, and the Results screen has its own.
 */
export const VARIANT_COLORS: readonly string[] = [
    '#0ea5e9',
    '#a855f7',
    '#fb923c',
    '#22c55e',
    '#f43f5e'
];

/** Query string every variant preview URL carries, before `&variantName=`. */
const PREVIEW_URL_PARAMS = 'disabledNavigateMode=true&mode=LIVE';

/**
 * The single row drawn while no experiment exists yet (#37003).
 *
 * The creation POST has the backend auto-create the `DEFAULT` variant at 100%, so the card states
 * that up front rather than drawing headers over nothing. It carries no server entity, which is why
 * it renders as the control does *and* frozen: no rename, no weight, no delete, no preview URL. It
 * is replaced by the real rows the moment `createSucceeded` puts the experiment in the store.
 */
const CONTROL_ROW_BEFORE_CREATION: VariantRowViewModel = {
    id: DEFAULT_VARIANT_ID,
    name: DEFAULT_VARIANT_NAME,
    weight: TOTAL_WEIGHT,
    isControl: true,
    color: VARIANT_COLORS[0],
    copyUrl: null,
    disabled: true,
    // Nothing to explain: the row is frozen because it does not exist yet, not because of a lock.
    disabledTooltipKey: null
};

/**
 * Variants card of the Configure screen.
 *
 * Owns everything about the variant list: renaming inplace, per-row weights, Split Evenly, adding
 * through the 440px dialog and deleting behind the shell's confirm dialog. Adding, renaming and
 * deleting leave as dispatched commands, each with an endpoint of its own; the weights are a slice of
 * the shell's root form, so the card edits them the way every other card edits its own fields and
 * the shell's binding is what persists them.
 *
 * On the creation screen there is no experiment to derive rows from, so the card draws
 * `CONTROL_ROW_BEFORE_CREATION` — the Original row the POST is about to create — and freezes every
 * action that would need a server entity behind it.
 *
 * Two things are deliberately *not* live here. The `[data-error]` markers only appear once
 * Start/Schedule has been pressed (AC28), which is what `validationErrors` records; and the
 * Edit Content / Preview button renders disabled, because the UVE round-trip is out of scope
 * (AC-Var-Edit).
 */
@Component({
    selector: 'dot-experiments-configure-variants',
    imports: [
        Card,
        FormField,
        ButtonModule,
        InputGroupModule,
        InputGroupAddonModule,
        InputTextModule,
        TooltipModule,
        DotCopyButtonComponent,
        DotExperimentsVariantNameInplaceComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-experiments-configure-variants.component.html',
    // Opened from this card only, so the dialog's lifetime is the card's.
    providers: [DialogService]
})
export class DotExperimentsConfigureVariantsComponent {
    /**
     * The weights slice of the root form: one row per persisted variant, carrying its range rule and
     * the cross-field rule over the set of them.
     */
    readonly $field = input.required<FieldTree<VariantWeightFormRow[]>>({ alias: 'field' });

    readonly store = inject(DotExperimentsConfigureStore);

    readonly MAX_VARIANTS_ALLOWED = MAX_VARIANTS_ALLOWED;
    readonly MAX_INPUT_TITLE_LENGTH = MAX_INPUT_TITLE_LENGTH;

    /** True on the creation screen: the experiment has not been POSTed, so nothing persists yet. */
    readonly $isBeforeCreation = computed<boolean>(() => !this.store.experiment());

    /** Rows as drawn, with everything the template would otherwise have to derive per row. */
    readonly $rows = computed<VariantRowViewModel[]>(() => {
        if (this.$isBeforeCreation()) {
            return [CONTROL_ROW_BEFORE_CREATION];
        }

        const disabledTooltipKey = this.store.$disabledTooltipKey();
        const previewUrl = this.#previewUrl();

        return this.store.$variants().map((variant, index) => ({
            id: variant.id,
            name: variant.name,
            weight: variant.weight ?? 0,
            isControl: isControlVariant(variant),
            color: VARIANT_COLORS[index % VARIANT_COLORS.length],
            copyUrl: previewUrl ? `${previewUrl}&variantName=${variant.id}` : null,
            disabled: !!disabledTooltipKey,
            disabledTooltipKey
        }));
    });

    /** True while nothing on the card may be changed: not a draft, or the page is locked. */
    readonly $isDisabled = computed<boolean>(() => !!this.store.$disabledTooltipKey());

    /** The cap is a backend setting, so the counter reads `n/max` rather than a fixed sentence. */
    readonly $isAtVariantCap = computed<boolean>(() => this.$rows().length >= MAX_VARIANTS_ALLOWED);

    /** Adding needs an experiment id to add the variant to, so it waits for the creation POST. */
    readonly $isAddDisabled = computed<boolean>(
        () => this.$isAtVariantCap() || this.$isBeforeCreation()
    );

    /** There is nothing to split while the only row is the control the POST will create. */
    readonly $isSplitEvenlyDisabled = computed<boolean>(
        () => this.$isDisabled() || this.$isBeforeCreation()
    );

    /**
     * The input each row's weight is bound to, by variant id.
     *
     * A row without one renders its weight as read-only text: that is the Original row drawn before
     * the experiment exists, which no form field stands behind.
     */
    readonly $weightFieldById = computed<Map<string, FieldTree<number | null>>>(() => {
        const weights = this.$field();

        return new Map(
            weights()
                .value()
                .map(({ id }, index) => [id, weights[index].weight])
        );
    });

    /** Reads 100% before creation: that is the proportion the POST will have written. */
    readonly $totalWeight = computed<number>(() =>
        this.$isBeforeCreation() ? TOTAL_WEIGHT : totalWeight(this.$field()().value())
    );

    /**
     * Live, per AC25: the weights not adding up is a fact about what is on screen, not a validation
     * result, so it is stated as soon as it is true rather than waiting for a Start press.
     *
     * Read off the slice's own errors, message included — the rule and its copy live in the schema,
     * so the bar and the form can never disagree about the total. A row's own range error is that
     * input's problem and stays on the row, so these are only ever about the total.
     */
    readonly $weightsErrors = computed(() => this.$field()().errors());

    readonly $hasWeightWarning = computed<boolean>(() => this.$weightsErrors().length > 0);

    /** Both of these turn a message into a scroll target, which only a Start press may do (AC28). */
    readonly $showMinVariantsError = computed<boolean>(() =>
        this.store.validationErrors().includes('minVariants')
    );

    readonly $showWeightsError = computed<boolean>(() =>
        this.store.validationErrors().includes(WEIGHTS_TOTAL_ERROR_KIND)
    );

    /** The hint gives way to the error, exactly as the design has it. */
    readonly $isHintVisible = computed<boolean>(() => !this.$showMinVariantsError());

    /** Live URL of the page under experiment, or `null` while no page is known. */
    readonly #previewUrl = computed<string | null>(() => {
        const path = this.store.selectedPage()?.path;

        if (!path) {
            return null;
        }

        const separator = path.includes('?') ? '&' : '?';

        return `${window.location.origin}${path.startsWith('/') ? '' : '/'}${path}${separator}${PREVIEW_URL_PARAMS}`;
    });

    readonly #dispatch = injectDispatch(dotExperimentsConfigurePageEvents);
    readonly #events = inject(Events);
    readonly #destroyRef = inject(DestroyRef);
    readonly #dialogService = inject(DialogService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotMessageService = inject(DotMessageService);

    constructor() {
        this.#resplitWeightsAfterAdd();
    }

    /** Persists a renamed variant. The variant endpoint takes the name on its own. */
    onVariantRenamed(variantId: string, name: string): void {
        this.#dispatch.variantRenamed({ variantId, name });
    }

    /** `floor(100/n)` each, with the first row absorbing the remainder (AC23). */
    onSplitEvenly(): void {
        this.#splitWeightsEvenly(this.store.$variants());
    }

    /** Opens the Add Variant dialog; a cancelled dialog closes with nothing and changes nothing. */
    onAddVariant(): void {
        const data: DotExperimentsAddVariantDialogData = {
            existingNames: this.store.$variants().map(({ name }) => name)
        };

        const dialogRef = this.#dialogService.open(DotExperimentsAddVariantDialogComponent, {
            header: this.#dotMessageService.get('experiments.configure.variants.add-dialog.header'),
            width: ADD_VARIANT_DIALOG_WIDTH,
            closable: true,
            closeOnEscape: true,
            data
        });

        dialogRef.onClose
            .pipe(take(1), takeUntilDestroyed(this.#destroyRef))
            .subscribe((result?: DotExperimentsAddVariantDialogResult) => {
                if (result?.name) {
                    this.#dispatch.variantAdded(result.name);
                }
            });
    }

    /** Deleting is irreversible, so it goes through the shell's confirm dialog first. */
    onDeleteVariant(row: VariantRowViewModel): void {
        this.#confirmationService.confirm({
            key: CONFIGURATION_CONFIRM_DIALOG_KEY,
            header: this.#dotMessageService.get(
                'experiments.configure.variant.delete.confirm-title'
            ),
            message: this.#dotMessageService.get('experiments.configure.variant.delete.confirm'),
            acceptLabel: this.#dotMessageService.get('delete'),
            rejectLabel: this.#dotMessageService.get('dot.common.dialog.reject'),
            rejectButtonStyleClass: 'p-button-secondary',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => this.#dispatch.variantDeleted(row.id)
        });
    }

    /**
     * A new variant arrives with the weights the backend chose, which AC24 wants re-split evenly.
     * Split over the list the response carries, so it does not matter whether the store has already
     * folded it in.
     */
    #resplitWeightsAfterAdd(): void {
        this.#events
            .on(dotExperimentsConfigureApiEvents.addVariantSucceeded)
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe(({ payload }) =>
                this.#splitWeightsEvenly(payload.trafficProportion?.variants ?? [])
            );
    }

    /**
     * Writes an even split into the slice, and reports it as the SPLIT_EVENLY proportion it is.
     *
     * The rows are the edit — they are what the inputs redraw from, and what the shell's binding
     * persists. The dispatch is here for the one thing the form cannot carry: the proportion's
     * *type*. A weight says what a share is, not whether the user asked for an even split, and the
     * backend redistributes a later variant only while the type is SPLIT_EVENLY
     * (`ExperimentsAPIImpl.addVariant`). Naming it here leaves the shell's binding with nothing to
     * report — it compares against what the store already holds — so the edit still travels once.
     */
    #splitWeightsEvenly(variants: Variant[]): void {
        if (!variants.length) {
            return;
        }

        const rows = splitWeightsEvenly(toVariantWeightRows(variants));

        this.$field()().value.set(rows);

        this.#dispatch.formEdited({
            trafficProportion: {
                type: TrafficProportionTypes.SPLIT_EVENLY,
                variants: mergeVariantWeights(variants, rows)
            }
        });
    }
}

/** The control is the `DEFAULT` variant; older experiments identify it by its name instead. */
function isControlVariant(variant: Variant): boolean {
    return variant.id === DEFAULT_VARIANT_ID || variant.name === DEFAULT_VARIANT_NAME;
}
