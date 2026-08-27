import { injectDispatch } from '@ngrx/signals/events';

import { Component, computed, DestroyRef, inject, input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FieldTree, FormField } from '@angular/forms/signals';

import { ButtonModule } from 'primeng/button';
import { DialogService } from 'primeng/dynamicdialog';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotExperimentStatus } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotBrowserSelectorComponent, DotMessagePipe } from '@dotcms/ui';

import {
    MAX_TRAFFIC_ALLOCATION,
    SELECT_PAGE_BROWSER_PARAMS,
    SELECT_PAGE_DIALOG_MAX_WIDTH,
    SELECT_PAGE_DIALOG_WIDTH
} from '../../../shared/constants';
import { DotExperimentConfigurePage } from '../../../shared/models';
import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';

/** Title of the Select A Page dialog. Its chrome belongs to the caller, not to the dialog. */
const SELECT_PAGE_DIALOG_HEADER_KEY = 'experiments.configure.select-page.header';

/** How the allocation reads at 100%, where there is no remainder to mention. */
const TRAFFIC_HELP_ALL_KEY = 'experiments.configure.page.traffic.help.all';

/** And below it, where the share left over goes to the Original. */
const TRAFFIC_HELP_PARTIAL_KEY = 'experiments.configure.page.traffic.help.partial';

/** Why the page is fixed on an experiment that is no longer a draft. */
const PAGE_IMMUTABLE_TOOLTIP_KEY = 'experiments.configure.page.select.immutable.tooltip';

/**
 * Why the page is fixed on a draft that already has variants.
 *
 * A different message from the one above on purpose: this one names something the user can undo.
 */
const PAGE_HAS_VARIANTS_TOOLTIP_KEY = 'experiments.configure.page.select.has-variants.tooltip';

/**
 * Page card of the Configure screen: which page the experiment runs on, and how much of that
 * page's traffic enters it.
 *
 * The page itself is not a form value: picking one is reported to the store directly. It can be
 * changed only while the experiment is a draft whose variants are the control alone — beyond that
 * a non-control variant holds a copy of this page, so the Select button is disabled with a tooltip
 * saying which of the two rules is in the way rather than offering a choice the backend refuses.
 *
 * The allocation is: it is a leaf of the shell's root form, handed over as a field tree and bound
 * to the number input. Its 1–100 rule and its read-only state live in the shell's schema.
 *
 * Nothing here validates as the user types: the required-page message only appears once Start has
 * been pressed and the store has published `page` among its validation errors (AC28).
 */
@Component({
    selector: 'dot-experiments-configure-page',
    // `display: contents` so the two field groups become direct children of the Details card's
    // column: projected content otherwise arrives wrapped in this host, and the card's gap would
    // apply to the wrapper instead of to each field.
    host: { class: 'contents' },
    imports: [
        FormField,
        ButtonModule,
        InputGroupModule,
        InputGroupAddonModule,
        InputTextModule,
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

    /** The page the experiment runs on, whether it was picked here or resolved from the URL. */
    protected readonly $selectedPage = computed<DotExperimentConfigurePage | null>(() =>
        this.store.selectedPage()
    );

    /**
     * The page can be changed while the experiment is a draft whose only variant is the control —
     * the rule the server enforces. Outside it the button explains itself rather than offering a
     * choice that would come back a 400.
     */
    protected readonly $canChangePage = computed<boolean>(() => this.store.$canChangePage());

    protected readonly $isSelectDisabled = computed<boolean>(
        () => this.store.$isLocked() || !this.$canChangePage()
    );

    /**
     * Why the button is disabled, most specific reason last.
     *
     * Locked leads, as the strongest and broadest reason. Below it the two halves of the page rule
     * get different copy on purpose: a non-draft experiment is simply past the point of changing,
     * while a draft with variants names something the user can act on — delete them and come back.
     */
    protected readonly $selectTooltipKey = computed<string | null>(() => {
        if (this.store.$isLocked()) {
            return this.store.$disabledTooltipKey();
        }

        if (this.$canChangePage()) {
            return null;
        }

        return this.store.$status() === DotExperimentStatus.DRAFT
            ? PAGE_HAS_VARIANTS_TOOLTIP_KEY
            : PAGE_IMMUTABLE_TOOLTIP_KEY;
    });

    /** Revealed by a Start press, and gone as soon as a page is picked. */
    protected readonly $hasPageError = computed<boolean>(() =>
        this.store.$validationErrors().includes('page')
    );

    /** At 100% there is no remainder, so the copy that mentions one would be wrong. */
    protected readonly $isWholePage = computed<boolean>(
        () => this.$field()().value() === MAX_TRAFFIC_ALLOCATION
    );

    protected readonly $trafficHelpKey = computed<string>(() =>
        this.$isWholePage() ? TRAFFIC_HELP_ALL_KEY : TRAFFIC_HELP_PARTIAL_KEY
    );

    /** The whole-page wording names only the path; the other one leads with the share. */
    protected readonly $trafficHelpArgs = computed<string[]>(() => {
        const path = this.$selectedPage()?.path ?? '';

        return this.$isWholePage() ? [path] : [String(this.$field()().value()), path];
    });
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

    /** Empties the picker so a different page can be chosen; same rule as picking one. */
    protected clearPage(): void {
        this.#dispatch.pageCleared();
    }

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
