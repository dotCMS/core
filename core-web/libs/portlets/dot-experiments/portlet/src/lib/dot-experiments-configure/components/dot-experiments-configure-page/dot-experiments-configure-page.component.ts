import { injectDispatch } from '@ngrx/signals/events';

import { Component, computed, DestroyRef, inject, input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FieldTree, FormField } from '@angular/forms/signals';

import { ButtonModule } from 'primeng/button';
import { Card } from 'primeng/card';
import { DialogService } from 'primeng/dynamicdialog';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { InputTextModule } from 'primeng/inputtext';
import { SliderModule } from 'primeng/slider';
import { TooltipModule } from 'primeng/tooltip';

import { take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import {
    MAX_TRAFFIC_ALLOCATION,
    MIN_TRAFFIC_ALLOCATION,
    SELECT_PAGE_DIALOG_SIZE
} from '../../../shared/constants';
import { DotExperimentConfigurePage } from '../../../shared/models';
import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';
import { DotExperimentsSelectPageDialogComponent } from '../dot-experiments-select-page-dialog/dot-experiments-select-page-dialog.component';
import { SelectPageDialogViewRow } from '../dot-experiments-select-page-dialog/dot-experiments-select-page-dialog.models';

/** Title of the Select A Page dialog. Its chrome belongs to the caller, not to the dialog. */
const SELECT_PAGE_DIALOG_HEADER_KEY = 'experiments.configure.select-page.header';

/** Stands in for the page path in the traffic helper copy while no page is selected. */
const TRAFFIC_HELP_FALLBACK_PAGE_KEY = 'experiments.configure.page.traffic.help.fallback-page';

/** Explains the Select button being disabled once the experiment exists. */
const PAGE_IMMUTABLE_TOOLTIP_KEY = 'experiments.configure.page.select.immutable.tooltip';

/**
 * Page card of the Configure screen: which page the experiment runs on, and how much of that
 * page's traffic enters it.
 *
 * The page itself is picked once. `PATCH /api/v1/experiments/{id}` does not accept `pageId`, so
 * the moment the draft exists the Select button is disabled with a tooltip saying why, rather
 * than offering a choice the backend would ignore. Picking one is therefore reported to the store
 * directly — it is not a form value.
 *
 * The allocation is: it is a leaf of the shell's root form, handed over as a field tree and bound
 * to both the slider and the number input, which is what keeps the two in step. Its 1–100 rule and
 * its read-only state live in the shell's schema.
 *
 * Nothing here validates as the user types: the required-page message only appears once Start has
 * been pressed and the store has published `page` among its validation errors (AC28). The inline
 * prefill message is separate — it reports a `?pageId=`/`?url=` that did not resolve, which
 * happened before the user touched anything (AC15).
 */
@Component({
    selector: 'dot-experiments-configure-page',
    imports: [
        Card,
        FormField,
        ButtonModule,
        InputGroupModule,
        InputGroupAddonModule,
        InputTextModule,
        SliderModule,
        TooltipModule,
        DotMessagePipe
    ],
    templateUrl: './dot-experiments-configure-page.component.html',
    // The dialog is opened from here, so this component owns its service instance.
    providers: [DialogService]
})
export class DotExperimentsConfigurePageComponent {
    /** Traffic-allocation leaf of the root form, carrying its 1–100 rule. */
    readonly $field = input.required<FieldTree<number>>({ alias: 'field' });

    readonly store = inject(DotExperimentsConfigureStore);

    readonly MIN_TRAFFIC_ALLOCATION = MIN_TRAFFIC_ALLOCATION;
    readonly MAX_TRAFFIC_ALLOCATION = MAX_TRAFFIC_ALLOCATION;

    /** The page the experiment runs on, whether it was picked here or resolved from the URL. */
    protected readonly $selectedPage = computed<DotExperimentConfigurePage | null>(() =>
        this.store.selectedPage()
    );

    /**
     * The page is fixed at creation time, so after that the button explains itself instead of
     * offering a choice that cannot be persisted.
     */
    protected readonly $isPageImmutable = computed<boolean>(() => !!this.store.experiment());

    protected readonly $isSelectDisabled = computed<boolean>(
        () => this.store.$isLocked() || this.$isPageImmutable()
    );

    /** Locked wins over immutability: it is the stronger reason and the one the screen leads with. */
    protected readonly $selectTooltipKey = computed<string | null>(() => {
        if (this.store.$isLocked()) {
            return this.store.$disabledTooltipKey();
        }

        return this.$isPageImmutable() ? PAGE_IMMUTABLE_TOOLTIP_KEY : null;
    });

    /** Revealed by a Start press only — never while the user is still filling the screen in. */
    protected readonly $hasPageError = computed<boolean>(() =>
        this.store.validationErrors().includes('page')
    );

    /** Arguments of the traffic helper copy: the share, and the page it is a share of. */
    protected readonly $trafficHelpArgs = computed<string[]>(() => [
        String(this.$field()().value()),
        this.$selectedPage()?.path ?? this.#dotMessageService.get(TRAFFIC_HELP_FALLBACK_PAGE_KEY)
    ]);

    /** True while the allocation is out of bounds, which is shown here rather than sent. */
    protected readonly $hasTrafficAllocationError = computed<boolean>(() =>
        this.$field()().invalid()
    );

    readonly #dispatch = injectDispatch(dotExperimentsConfigurePageEvents);
    readonly #dialogService = inject(DialogService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #destroyRef = inject(DestroyRef);

    /** Opens the picker and reports the chosen page. Cancelling leaves the card as it was. */
    protected openPageSelector(): void {
        this.#dialogService
            .open(DotExperimentsSelectPageDialogComponent, {
                header: this.#dotMessageService.get(SELECT_PAGE_DIALOG_HEADER_KEY),
                width: SELECT_PAGE_DIALOG_SIZE.width,
                height: SELECT_PAGE_DIALOG_SIZE.height,
                closable: true,
                closeOnEscape: true,
                modal: true
            })
            .onClose.pipe(take(1), takeUntilDestroyed(this.#destroyRef))
            .subscribe((row: SelectPageDialogViewRow | undefined) => {
                if (!row) {
                    return;
                }

                this.#dispatch.pageSelected({
                    pageId: row.pageId,
                    title: row.title,
                    path: row.url
                });
            });
    }
}
