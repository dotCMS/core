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
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotBrowserSelectorComponent, DotMessagePipe } from '@dotcms/ui';

import {
    MAX_TRAFFIC_ALLOCATION,
    MIN_TRAFFIC_ALLOCATION,
    SELECT_PAGE_BROWSER_PARAMS,
    SELECT_PAGE_DIALOG_MAX_WIDTH,
    SELECT_PAGE_DIALOG_WIDTH
} from '../../../shared/constants';
import { DotExperimentConfigurePage } from '../../../shared/models';
import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';

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

    /**
     * Whatever the form says is wrong with the allocation, message included: the rules and their copy
     * live in the shell's schema, so the card renders them without knowing which ones exist.
     */
    protected readonly $trafficErrors = computed(() => this.$field()().errors());

    readonly #dispatch = injectDispatch(dotExperimentsConfigurePageEvents);
    readonly #dialogService = inject(DialogService);
    readonly #globalStore = inject(GlobalStore);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #destroyRef = inject(DestroyRef);

    /** Opens the picker and reports the chosen page. Cancelling leaves the card as it was. */
    /**
     * Picks the page through the shared site browser — the same dialog the file fields and the block
     * editor open, asked for pages only. It answers with the chosen contentlet, and the three fields
     * the experiment needs are on it.
     */
    protected openPageSelector(): void {
        this.#dialogService
            .open(DotBrowserSelectorComponent, {
                header: this.#dotMessageService.get(SELECT_PAGE_DIALOG_HEADER_KEY),
                appendTo: 'body',
                closable: true,
                closeOnEscape: true,
                draggable: false,
                keepInViewport: false,
                maskStyleClass: 'p-dialog-mask-dynamic',
                resizable: false,
                modal: true,
                width: SELECT_PAGE_DIALOG_WIDTH,
                style: { 'max-width': SELECT_PAGE_DIALOG_MAX_WIDTH },
                data: {
                    ...SELECT_PAGE_BROWSER_PARAMS,
                    // Without it the browser opens on System Host, which holds no pages.
                    hostFolderId: this.#globalStore.currentSiteId()
                }
            })
            .onClose.pipe(take(1), takeUntilDestroyed(this.#destroyRef))
            .subscribe((page: DotCMSContentlet | undefined) => {
                if (!page) {
                    return;
                }

                this.#dispatch.pageSelected({
                    pageId: page.identifier,
                    title: page.title,
                    path: page.url
                });
            });
    }
}
