import { Events, injectDispatch } from '@ngrx/signals/events';

import { Component, computed, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogService } from 'primeng/dynamicdialog';
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

import { ADD_VARIANT_DIALOG_WIDTH, MAX_VARIANTS_ALLOWED } from '../../../shared/constants';
import { VariantRowViewModel } from '../../../shared/models';
import { dotExperimentsConfigureApiEvents } from '../../../store/dot-experiments-configure-api.events';
import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';
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

/** The weights must add up to this, and no single one may exceed it. */
const TOTAL_WEIGHT = 100;

/** Query string every variant preview URL carries, before `&variantName=`. */
const PREVIEW_URL_PARAMS = 'disabledNavigateMode=true&mode=LIVE';

/**
 * Variants card of the Configure screen.
 *
 * Owns everything about the variant list: renaming inplace, per-row weights, Split Evenly, adding
 * through the 440px dialog and deleting behind the shell's confirm dialog. Every change leaves as
 * a dispatched event — the card holds no state of its own beyond the rows it derives from the
 * store, so what it draws is always what was last persisted.
 *
 * Two things are deliberately *not* live here. The `[data-error]` markers only appear once
 * Start/Schedule has been pressed (AC28), which is what `validationErrors` records; and the
 * Edit Content / Preview button renders disabled, because the UVE round-trip is out of scope
 * (AC-Var-Edit).
 */
@Component({
    selector: 'dot-experiments-configure-variants',
    imports: [
        ButtonModule,
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
    readonly store = inject(DotExperimentsConfigureStore);

    readonly MAX_VARIANTS_ALLOWED = MAX_VARIANTS_ALLOWED;
    readonly MAX_INPUT_TITLE_LENGTH = MAX_INPUT_TITLE_LENGTH;
    readonly TOTAL_WEIGHT = TOTAL_WEIGHT;

    /** Rows as drawn, with everything the template would otherwise have to derive per row. */
    readonly $rows = computed<VariantRowViewModel[]>(() => {
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
    readonly $isAtVariantCap = computed<boolean>(
        () => this.store.$variants().length >= MAX_VARIANTS_ALLOWED
    );

    /**
     * Live, per AC25: the weights not adding up is a fact about the card, not a validation result,
     * so it is stated as soon as it is true rather than waiting for a Start press.
     */
    readonly $hasWeightWarning = computed<boolean>(() => this.store.$hasInvalidWeights());

    /** Both of these turn a message into a scroll target, which only a Start press may do (AC28). */
    readonly $showMinVariantsError = computed<boolean>(() =>
        this.store.validationErrors().includes('minVariants')
    );

    readonly $showWeightsError = computed<boolean>(() =>
        this.store.validationErrors().includes('weightsTotal')
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

    /**
     * Sends the whole proportion, not the single weight that changed: the PATCH body replaces it
     * wholesale, so the other rows have to travel with it.
     */
    onWeightChanged(variantId: string, value: string | number): void {
        const weight = clampWeight(Number(value));
        const variants = this.store
            .$variants()
            .map((variant) => (variant.id === variantId ? { ...variant, weight } : variant));

        this.#dispatch.trafficProportionChanged({
            type: TrafficProportionTypes.CUSTOM_PERCENTAGES,
            variants
        });
    }

    /** `floor(100/n)` each with the first row absorbing the remainder — the store does the maths. */
    onSplitEvenly(): void {
        this.#dispatch.splitEvenly();
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
     * Keyed off the *succeeded* event rather than the request: only then does the store hold the
     * variant list the split has to be computed over.
     */
    #resplitWeightsAfterAdd(): void {
        this.#events
            .on(dotExperimentsConfigureApiEvents.addVariantSucceeded)
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe(() => this.#dispatch.splitEvenly());
    }
}

/** The control is the `DEFAULT` variant; older experiments identify it by its name instead. */
function isControlVariant(variant: Variant): boolean {
    return variant.id === DEFAULT_VARIANT_ID || variant.name === DEFAULT_VARIANT_NAME;
}

/** A typed weight is only ever a percentage, and a cleared input reads as zero rather than NaN. */
function clampWeight(value: number): number {
    if (!Number.isFinite(value)) {
        return 0;
    }

    return Math.min(TOTAL_WEIGHT, Math.max(0, value));
}
