import { Events, injectDispatch } from '@ngrx/signals/events';

import { Component, computed, DestroyRef, inject, input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FieldTree, FormField } from '@angular/forms/signals';

import { ButtonModule } from 'primeng/button';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes, DotCMSContentlet } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { AngularAssetPickerLauncher, DotMessagePipe } from '@dotcms/ui';

import {
    CHANGE_PAGE_DIALOG_WIDTH,
    MAX_TRAFFIC_ALLOCATION,
    SELECT_PAGE_BROWSER_PARAMS,
    VARIANT_COLORS
} from '../../../shared/constants';
import { DotExperimentConfigurePage } from '../../../shared/models';
import { dotExperimentsConfigureApiEvents } from '../../../store/dot-experiments-configure-api.events';
import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';
import { toConfigurePage } from '../../../util/dot-experiments-configure.util';
import {
    DotExperimentsChangePageDialogComponent,
    DotExperimentsChangePageDialogInputs,
    DotExperimentsChangePageDialogResult,
    DotExperimentsChangePageDialogVariant
} from '../dot-experiments-change-page-dialog/dot-experiments-change-page-dialog.component';

/** Title of the Select A Page dialog, resolved here: the picker takes it already translated. */
const SELECT_PAGE_DIALOG_HEADER_KEY = 'experiments.configure.select-page.header';

/** How the allocation reads at 100%, where there is no remainder to mention. */
const TRAFFIC_HELP_ALL_KEY = 'experiments.configure.page.traffic.help.all';

/** And below it, where the share left over goes to the Original. */
const TRAFFIC_HELP_PARTIAL_KEY = 'experiments.configure.page.traffic.help.partial';

/** Title of the Change Page confirmation. Its chrome belongs to the caller too. */
const CHANGE_PAGE_DIALOG_HEADER_KEY = 'experiments.configure.page.change.header';

/**
 * Page card of the Configure screen: which page the experiment runs on, and how much of that
 * page's traffic enters it.
 *
 * The page itself is not a form value: picking one is reported to the store directly. It can be
 * changed for as long as the experiment is a draft; past that the button is disabled and says why.
 *
 * A draft that already has non-control variants can change its page too, but not for free: each of
 * those variants holds a copy of *this* page, and the server refuses the change while they exist. So
 * Change Page confirms first — naming the page and every variant it would delete — and only opens
 * the picker once the store reports them gone.
 *
 * The confirmation is a view: this card owns its dialog reference, hands it the wait and the failure
 * as store signals, and closes it on `deleteVariantsSucceeded`. Both dialogs go through
 * `DialogService` so the handover is one chain — the confirmation's `onClose` is what opens the
 * picker.
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
    // The dialogs are opened from here, so this component owns its service instance. The launcher
    // is stateless and carries no `providedIn`, so it is provided alongside it.
    providers: [DialogService, AngularAssetPickerLauncher]
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
     * The variants a page change would delete, as the dialog lists them.
     *
     * Their colours are taken from their position in the *whole* variant list, so a variant is the
     * same colour here as it is on the Variants card the user just read.
     */
    protected readonly $deletableVariants = computed<DotExperimentsChangePageDialogVariant[]>(
        () => {
            const variants = this.store.$variants();

            return this.store.$deletableVariants().map((variant) => ({
                id: variant.id,
                name: variant.name,
                color: VARIANT_COLORS[
                    variants.findIndex(({ id }) => id === variant.id) % VARIANT_COLORS.length
                ]
            }));
        }
    );

    /**
     * Only a draft may point at a different page — the rule the server enforces on `save()`.
     *
     * Existing variants deliberately do *not* disable it: they are a step on the way rather than a
     * wall, and the confirmation is what turns them into one the user can take.
     *
     * What does disable it is a refusal no deletion could lift. `save()` accepts a page change only
     * when the experiment carries exactly one variant and it is the control
     * (`hasOnlyTheControlVariant`, `variants.size() == 1`), so a draft carrying *none* is refused
     * with nothing the user could act on. Unreachable while the creation POST always makes the
     * control — but a click that silently does nothing is a worse way to meet that than a button
     * that is plainly off.
     */
    protected readonly $isPageActionDisabled = computed<boolean>(
        () =>
            this.store.$isLocked() ||
            (!this.store.$canChangePage() && !this.$deletableVariants().length)
    );

    /**
     * Why the button is disabled, or `null` while it is not.
     *
     * There is only one reason left to give: an experiment past draft, which is also what freezes
     * every other field, so it reads with the same copy they do rather than one of its own.
     */
    protected readonly $pageActionTooltipKey = computed<string | null>(() =>
        this.store.$isLocked() ? this.store.$disabledTooltipKey() : null
    );

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
    readonly #events = inject(Events);
    readonly #dialogService = inject(DialogService);
    readonly #assetPickerLauncher = inject(AngularAssetPickerLauncher);
    readonly #globalStore = inject(GlobalStore);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #destroyRef = inject(DestroyRef);

    /** The confirmation while it is open, so it can be closed from outside itself. */
    #changePageRef: DynamicDialogRef | null = null;

    /**
     * Whether a page change *this card* asked for is still waiting on the store.
     *
     * The success event is a global one, and the card's reaction to it is to open a picker. Today
     * only this dialog can produce it, so the flag changes nothing — it is here so that the
     * invariant is written down rather than left resting on there being a single producer. Without
     * it, the day anything else clears variants, a page picker appears on a screen the user was not
     * interacting with.
     */
    #awaitingPageChange = false;

    /**
     * Why the experiment's page could not be resolved, or `null` when there is nothing to report.
     *
     * The store has always recorded this; nothing rendered it, so a page that had been deleted
     * fell through to the card's empty state and read as "no page was ever chosen" (#37005).
     */
    protected readonly $pageUnresolved = computed<string | null>(() =>
        this.store.selectedPage() ? null : this.store.pagePrefillError()
    );

    constructor() {
        this.#closeConfirmationWhenVariantsAreGone();
    }

    /**
     * Change Page: straight to the picker, or through the confirmation the variants earn first.
     *
     * `$canChangePage` is the server's own rule, so a page it would accept as it stands needs
     * nothing explaining — that covers a creation screen with no experiment behind it as well as a
     * draft whose only variant is the control. Everything else has variants to delete, and the
     * dialog is what says so.
     *
     * The site is checked here and not only where the picker opens, because between the two sits an
     * irreversible deletion. Falling through would delete every variant and *then* find nothing to
     * open the picker against — the user pays the whole cost and gets none of the screen they asked
     * for. With no site there is nothing worth starting, so the press is the same no-op the picker
     * itself falls back to.
     */
    protected onChangePage(): void {
        if (!this.#globalStore.siteDetails()) {
            return;
        }

        if (this.store.$canChangePage()) {
            this.openPageSelector();

            return;
        }

        // Nothing to delete and still refused means the experiment is past draft, which the
        // disabled button already covers. Reachable only if the status moved under the click.
        if (!this.$deletableVariants().length) {
            return;
        }

        this.#openChangePageDialog();
    }

    /**
     * Asks for the confirmation, and opens the picker if it closes with the go-ahead.
     *
     * Everything that can move — the list, the wait, the failure — travels as the store's own
     * signals rather than as values, because `inputValues` reaches the dialog once at creation (see
     * the dialog's own docs). The press is reported first so the dialog opens on a clean slate
     * instead of on the error a cancelled attempt left behind.
     *
     * `closable` and `closeOnEscape` are getters, not booleans: PrimeNG reads both off this config
     * on every change detection, so a getter is what lets them close for as long as a deletion is
     * on the wire. It matters because the deletions do not stop when the dialog does — dismissing
     * it mid-run would let a half-finished cascade finish with nothing on screen reporting it, and
     * would leave this card holding a reference it can no longer close.
     *
     * Only `true` opens the picker, and only this card ever closes the dialog with it: a cancelled
     * dialog, or one closed after a rejection, leaves the page as it is.
     */
    #openChangePageDialog(): void {
        this.#dispatch.pageChangeRequested();
        this.#awaitingPageChange = true;

        const inputValues: DotExperimentsChangePageDialogInputs = {
            pageTitle: this.$selectedPage()?.title ?? '',
            variants: this.$deletableVariants,
            deleting: this.store.deletingVariants,
            failed: this.store.deleteVariantsFailed
        };

        const isDeleting = () => this.store.deletingVariants();

        this.#changePageRef = this.#dialogService.open(DotExperimentsChangePageDialogComponent, {
            header: this.#dotMessageService.get(CHANGE_PAGE_DIALOG_HEADER_KEY),
            width: CHANGE_PAGE_DIALOG_WIDTH,
            get closable() {
                return !isDeleting();
            },
            get closeOnEscape() {
                return !isDeleting();
            },
            modal: true,
            inputValues
        });

        this.#changePageRef.onClose
            .pipe(take(1), takeUntilDestroyed(this.#destroyRef))
            .subscribe((result?: DotExperimentsChangePageDialogResult) => {
                this.#changePageRef = null;

                if (result) {
                    this.openPageSelector();

                    return;
                }

                // Closed with nothing: no run of ours is outstanding any more.
                this.#awaitingPageChange = false;
            });
    }

    /**
     * Hands over to the picker the moment the store reports the variants deleted.
     *
     * Here rather than inside the dialog: the dialog is created outside this card's injector and so
     * cannot reach the store at all — listening to the event from in there would be listening to a
     * global bus for an answer that may not even be this screen's. This card holds the store *and*
     * the reference, so it is the one place that can tie the two together.
     *
     * The picker opens either way. Closing the dialog is what normally leads to it, through the
     * `onClose` above; but the deletions outlive the dialog, so an answer that arrives with no
     * dialog left to close still has a promise to keep.
     *
     * Only for a run this card started, though — see `#awaitingPageChange`.
     */
    #closeConfirmationWhenVariantsAreGone(): void {
        this.#events
            .on(dotExperimentsConfigureApiEvents.deleteVariantsSucceeded)
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe(() => {
                if (!this.#awaitingPageChange) {
                    return;
                }

                this.#awaitingPageChange = false;

                const confirmation = this.#changePageRef;

                if (confirmation) {
                    // `onClose` takes it from here: it nulls the reference and opens the picker.
                    confirmation.close(true);

                    return;
                }

                this.openPageSelector();
            });
    }

    /**
     * Picks the page through the AssetPicker — the same dialog the file fields and the block editor
     * open, in browse mode narrowed to pages. It answers with the chosen contentlet, and the three
     * fields the experiment needs are on it. Cancelling leaves the card as it was.
     */
    protected openPageSelector(): void {
        const site = this.#globalStore.siteDetails();

        // The picker browses a `DotSite` and, unlike the legacy browser it replaced, has no System
        // Host to fall back on: with no site resolved yet there is nothing to open it against.
        if (!site) {
            return;
        }

        this.#assetPickerLauncher
            .open(this.#dialogService, {
                mode: 'browse',
                site,
                title: this.#dotMessageService.get(SELECT_PAGE_DIALOG_HEADER_KEY),
                allowedBaseTypes: [DotCMSBaseTypesContentTypes.HTMLPAGE],
                browse: SELECT_PAGE_BROWSER_PARAMS
            })
            .onClose.pipe(take(1), takeUntilDestroyed(this.#destroyRef))
            .subscribe((page: DotCMSContentlet | undefined) => {
                if (!page) {
                    return;
                }

                // Via `toConfigurePage` rather than an inline literal so the picker's page and a
                // prefilled/loaded one are narrowed the same way — including `languageId`, which
                // the variant deep link needs and which is easy to forget in a second mapping.
                this.#dispatch.pageSelected(toConfigurePage(page));
            });
    }
}
