import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    model,
    signal,
    untracked,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ConfirmationService, ConfirmEventType, MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import {
    DialogService,
    DynamicDialog,
    DynamicDialogModule,
    DynamicDialogRef
} from 'primeng/dynamicdialog';
import { MessageModule } from 'primeng/message';
import { ToastModule } from 'primeng/toast';

import {
    DotContentletService,
    DotLanguagesService,
    DotMessageService,
    DotVersionableService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import { DotCMSWorkflowAction, DotContentletDepths } from '@dotcms/dotcms-models';
import { DotCollapseBreadcrumbComponent, DotMessagePipe } from '@dotcms/ui';

import { FormValues } from '../../models/dot-edit-content-form.interface';
import { DotEditContentService } from '../../services/dot-edit-content.service';
import {
    EDIT_CONTENT_HOST,
    InPlaceNavigationRequest
} from '../../services/host/edit-content-host.model';
import { DotRelatedContentNavigationStore } from '../../store/dot-related-content-navigation.store';
import { DotEditContentStore } from '../../store/edit-content.store';
import { DotEditContentCompareComponent } from '../dot-edit-content-compare/dot-edit-content-compare.component';
import { DotEditContentFormComponent } from '../dot-edit-content-form/dot-edit-content-form.component';
import { DotEditContentSidebarComponent } from '../dot-edit-content-sidebar/dot-edit-content-sidebar.component';

/**
 * Edit Content Layout Component
 *
 * The main layout for content editing. It is **presentation-agnostic**: it does
 * not know whether it is shown full-screen, in a dialog, or in a side panel.
 * Everything that used to branch on a "dialog mode" now goes through the injected
 * {@link EditContentHost} port:
 *
 * - **Identity** — the store calls `host.resolveIdentity()` to know which content
 *   to open (route params in full-screen, dialog config in overlay).
 * - **Navigation / chrome** — related-content navigation, locale switch, title and
 *   breadcrumb are delegated to the host.
 * - **Result** — a successful save is reported via `host.reportSaved()`.
 *
 * To mount it in a new presentation, provide an `EDIT_CONTENT_HOST` adapter above
 * this component; the component itself does not change.
 */
@Component({
    selector: 'dot-edit-content-form-layout',
    imports: [
        DotMessagePipe,
        ButtonModule,
        ToastModule,
        MessageModule,
        DynamicDialogModule,
        DotEditContentFormComponent,
        DotEditContentSidebarComponent,
        ConfirmDialogModule,
        DotEditContentCompareComponent,
        DotCollapseBreadcrumbComponent
    ],
    providers: [
        DotContentletService,
        DotLanguagesService,
        DotVersionableService,
        DotWorkflowsActionsService,
        DotWorkflowActionsFireService,
        DotEditContentService,
        DotWorkflowService,
        DotEditContentStore,
        DialogService,
        // Scoped to this component so the unsaved-changes guard and the
        // template's `<p-confirmDialog />` share the exact same instance.
        // Without this, `inject(ConfirmationService)` from the route guard
        // would resolve to the root provider while the dialog subscribed to
        // a different one, and the confirm emission would never reach it.
        ConfirmationService
    ],
    host: {
        '[class.edit-content--with-sidebar]': '$store.isSidebarOpen()',
        '(window:beforeunload)': 'onBeforeUnload($event)'
    },
    templateUrl: './dot-edit-content.layout.component.html',
    styleUrls: ['./dot-edit-content.layout.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentLayoutComponent {
    /**
     * Controls the visibility of the workflow selection dialog.
     */
    readonly $showDialog = model<boolean>(false);

    /**
     * The store instance for managing component state.
     * Each component instance gets its own isolated store for complete state independence.
     */
    readonly $store = inject(DotEditContentStore);

    /**
     * The PrimeNG `ConfirmationService` provided at this component's level.
     * Exposed as a public field so the unsaved-changes route guard — which
     * runs in the route's environment injector and cannot reach our
     * component-level provider via `inject()` — can route its confirm
     * request through the same instance the template's `<p-confirmDialog />`
     * subscribes to.
     */
    readonly confirmationService = inject(ConfirmationService);

    readonly #dotMessageService = inject(DotMessageService);

    /**
     * Related-content navigation trail store. The trail is URL-driven in
     * full-screen and in-memory in the dialog; the breadcrumb below is
     * presentation-agnostic and reads it the same way in both.
     */
    readonly #relatedNav = inject(DotRelatedContentNavigationStore);

    /**
     * Presentation port. Decides how navigation happens (router vs in-place) and
     * whether chrome side-effects apply. The breadcrumb and the in-place reload
     * subscription below are driven by it instead of by an `isDialogMode` check.
     */
    readonly #host = inject(EDIT_CONTENT_HOST);

    /**
     * Keeps the in-place reload overlay up until BOTH the new content and the
     * sidebar's data have finished loading — so navigating related content never
     * reveals the new content while the sidebar is still loading (the "two
     * loading timings" problem). Armed when a reload starts (`isReloading`) and
     * disarmed only once everything has settled (`isFullyLoaded`). Only affects
     * in-place reloads; the initial load is unchanged.
     */
    readonly #reloadWait = signal(false);

    /** Whether the in-place reload overlay should be shown. */
    readonly $showReloadOverlay = computed(() => this.#reloadWait());

    /**
     * "Relating content" breadcrumb model built from the navigation trail.
     *
     * The current (last) crumb is a plain label. Earlier crumbs are a `command`
     * that asks the host to navigate to that crumb, in both presentations:
     * - Full-screen: the host updates the URL (`rc` query param) and the reused
     *   route reloads via `identityChanges$`.
     * - Dialog: the host reloads the editor in place (a router link would
     *   navigate the host page behind the dialog).
     *
     * A `command` (not a declarative `routerLink`) is used even full-screen so the
     * unsaved-changes prompt runs at the source — a reused route no longer fires
     * `canDeactivate` on `:id → :id`, which a `routerLink` would silently bypass.
     */
    readonly $relatedNavItems = computed<MenuItem[]>(() => {
        const trail = this.#host.trail();

        return trail.map((crumb, index) => {
            const isCurrent = index === trail.length - 1;
            if (isCurrent) {
                return { label: crumb.title };
            }

            // Trail trimmed to (and including) this crumb.
            const inodes = trail.slice(0, index + 1).map((c) => c.inode);

            return {
                label: crumb.title,
                styleClass: 'cursor-pointer',
                command: () => this.#host.goToCrumb(crumb.inode, inodes)
            };
        });
    });

    /**
     * Present when rendered inside a PrimeNG DynamicDialog; null otherwise (e.g.
     * full-screen). Used to intercept dialog close events for the dirty-content guard.
     */
    readonly #dialogRef = inject(DynamicDialogRef, { optional: true });

    /**
     * The DynamicDialog host component. Injected optionally to access its inner
     * p-dialog instance (via `dialog`) so we can override `p-dialog.close()` and
     * prevent the hide animation from starting when there are unsaved changes.
     */
    readonly #dynamicDialog = inject(DynamicDialog, { optional: true });

    readonly $editContentForm = viewChild(DotEditContentFormComponent);

    constructor() {
        if (this.#dialogRef) {
            this.#interceptDirtyClose();
        }

        // In-place hosts (dialog) request related-content navigation without a
        // route change; reload the editor here, confirming unsaved changes first.
        const inPlaceNavigation$ = this.#host.inPlaceNavigation$;
        if (inPlaceNavigation$) {
            inPlaceNavigation$
                .pipe(takeUntilDestroyed())
                .subscribe((request) => this.#reloadInPlace(request));
        }

        // URL-driven hosts (full-screen) reuse the component across content
        // navigations, so re-run initialization on each identity change instead of
        // relying on a fresh component per navigation. The previous content stays
        // rendered until the new one loads (see the `isReloading` gate + overlay in
        // the template). This also covers browser back/forward and deep links.
        const identityChanges$ = this.#host.identityChanges$;
        if (identityChanges$) {
            identityChanges$.pipe(takeUntilDestroyed()).subscribe(() => this.$store.initialize());
        }

        // A reused route no longer fires `canDeactivate` on `:id → :id`, so enforce
        // the unsaved-changes prompt before the host performs such a navigation
        // (breadcrumb, locale switch, version restore, related content). In-place
        // hosts leave this unset — their reload path already confirms via
        // `#reloadInPlace`.
        this.#host.setNavigationGuard?.((proceed) => {
            if (this.hasUnsavedChanges()) {
                this.#confirmIfDirty(proceed, () => undefined);
            } else {
                proceed();
            }
        });

        // Cache the current content's title so the "Relating content" breadcrumb
        // can label its crumb. The trail itself lives in the URL (rc query param)
        // or in memory (dialog), so there is nothing to reconcile.
        effect(() => {
            const contentlet = this.$store.contentlet();
            if (!contentlet?.inode) {
                return;
            }

            untracked(() => this.#relatedNav.registerTitle(contentlet.inode, contentlet.title));
        });

        // Initialize from the identity resolved by the host (route params in
        // full-screen, dialog config in overlay). Presentation-agnostic — the
        // editor no longer branches on a "dialog mode".
        this.$store.initialize();

        // Hold the in-place reload overlay until BOTH the new content and the
        // sidebar's data have loaded. Arm when a reload starts (`isReloading`), and
        // disarm only once everything has settled (`isFullyLoaded`) — the window in
        // between (content loaded, sidebar still loading) keeps the overlay up so
        // the new content is not revealed before the sidebar. Only the in-place
        // reload path arms this; the initial load never sets `isReloading`.
        effect(() => {
            if (this.$store.isReloading()) {
                untracked(() => this.#reloadWait.set(true));
            } else if (this.$store.isFullyLoaded()) {
                untracked(() => this.#reloadWait.set(false));
            }
        });

        // After a successful save: mark the form pristine so the navigation
        // guard does not re-prompt, and clear the signal so the same contentlet
        // can trigger another save event.
        effect(() => {
            const success = this.$store.workflowActionSuccess();
            if (!success) {
                return;
            }

            this.markFormPristine();

            // Report the save to the host: full-screen ignores it (URL is the
            // source of truth), the dialog forwards it to whoever opened it.
            this.#host.reportSaved(success);

            this.$store.clearWorkflowActionSuccess();
        });
    }

    /**
     * Sets up two intercepts for dirty-content confirmation when rendered inside a dialog:
     *
     * 1. pDialog.close override — catches all UI-triggered closes (X button, ESC
     *    key, mask click). p-dialog.close() sets _visible = false synchronously
     *    before emitting visibleChange, so we must intercept here — before the
     *    internal state changes — to prevent the hide animation from starting.
     *
     * 2. dialogRef.close override — catches programmatic closes (dialogRef.close()
     *    called directly from code, e.g. DotEditContentDialogComponent.closeDialog()).
     */
    #interceptDirtyClose(): void {
        const dialogRef = this.#dialogRef!;
        const originalClose = dialogRef.close.bind(dialogRef);

        // --- Programmatic close path ---
        dialogRef.close = (value?: unknown) => {
            if (!this.hasUnsavedChanges() || this.$store.workflowActionSuccess()) {
                originalClose(value);

                return;
            }

            this.#confirmIfDirty(
                () => originalClose(value),
                () => undefined
            );
        };

        // --- UI close path (X button, ESC, mask click) ---
        // Access p-dialog directly via DynamicDialog.dialog to intercept close()
        // before it sets _visible = false and starts the hide animation.
        const pDialog = this.#dynamicDialog?.dialog;
        if (pDialog) {
            const originalPDialogClose = pDialog.close.bind(pDialog);
            pDialog.close = (event: Event) => {
                if (!this.hasUnsavedChanges() || this.$store.workflowActionSuccess()) {
                    originalPDialogClose(event);

                    return;
                }

                event?.preventDefault();
                this.#confirmIfDirty(
                    () => originalPDialogClose(event),
                    () => undefined
                );
            };
        }
    }

    /**
     * Shows the unsaved-changes confirmation dialog.
     * Calls onConfirm when the user chooses "Discard changes",
     * calls onCancel when the user chooses "Keep editing" or dismisses.
     */
    #confirmIfDirty(onConfirm: () => void, onCancel: () => void): void {
        this.confirmationService.confirm({
            header: this.#dotMessageService.get('edit.content.unsaved.changes.title'),
            message: this.#dotMessageService.get('edit.content.unsaved.changes.message'),
            acceptLabel: this.#dotMessageService.get('edit.content.unsaved.changes.keep'),
            rejectLabel: this.#dotMessageService.get('edit.content.unsaved.changes.discard'),
            acceptIcon: 'hidden',
            rejectIcon: 'hidden',
            rejectButtonStyleClass: 'p-button-outlined',
            // "Keep editing" → cancel action
            accept: () => onCancel(),
            reject: (type?: ConfirmEventType) => {
                if (type === ConfirmEventType.REJECT) {
                    // "Discard changes" → proceed
                    this.markFormPristine();
                    onConfirm();
                } else {
                    // X / ESC on the confirm dialog itself → keep editing
                    onCancel();
                }
            }
        });
    }

    /**
     * Reloads the editor in place with a different content (in-place navigation
     * hosts only), prompting to discard unsaved changes first. Full-screen hosts
     * never call this — they reload through a route change.
     *
     * The request's `trail` (when present) is committed to the host **only if the
     * reload actually proceeds** — so choosing "Keep editing" leaves the breadcrumb
     * untouched rather than showing a content that never opened.
     */
    #reloadInPlace(request: InPlaceNavigationRequest): void {
        const reload = () => {
            if (request.trail) {
                this.#host.setTrail(request.trail);
            }
            this.$store.initializeExistingContent({
                inode: request.inode,
                depth: DotContentletDepths.TWO
            });
        };

        if (this.hasUnsavedChanges()) {
            this.#confirmIfDirty(reload, () => undefined);
        } else {
            reload();
        }
    }

    hasUnsavedChanges(): boolean {
        return this.$editContentForm()?.form?.dirty ?? false;
    }

    markFormPristine(): void {
        this.$editContentForm()?.form?.markAsPristine();
    }

    /**
     * Triggers the browser's native unload-confirmation dialog when the form has
     * unsaved changes. Covers cases the Angular `CanDeactivate` guard cannot
     * intercept: tab close, refresh, window close, manual URL change, bookmarks
     * and any external link that changes `window.location`. The dialog text is
     * controlled by the browser and cannot be customized.
     *
     * `preventDefault()` triggers the prompt in modern Chrome / Firefox /
     * Edge. The legacy `returnValue` assignment must be a non-empty string —
     * the empty string is treated as "no prompt" by the spec — so older
     * Chrome (<119), Safari, and some embedded WebViews actually show the
     * dialog. The string itself is ignored; browsers render their own copy.
     */
    onBeforeUnload(event: BeforeUnloadEvent): void {
        if (this.hasUnsavedChanges()) {
            event.preventDefault();
            event.returnValue = 'unsaved-changes';
        }
    }

    /**
     * Opens the workflow selection dialog.
     */
    selectWorkflow() {
        this.$showDialog.set(true);
    }

    /**
     * Handles a workflow action fired from the sidebar by delegating to the form.
     *
     * Builds the workflow action params from the current store state and forwards
     * them to the embedded form via the `$editContentForm` viewChild. The optional
     * chaining guards the compare view, where the form is not rendered.
     *
     * @param workflow - The workflow action to execute
     */
    onWorkflowActionFired(workflow: DotCMSWorkflowAction): void {
        const currentLocale = this.$store.currentLocale();
        // NOTE: inode is intentionally optional — new (unsaved) content has no inode yet and
        // the create flow relies on that. Do NOT add an `if (!inode) return` guard here: it
        // silently blocks saving brand-new content (the workflow action never fires).
        this.$editContentForm()?.fireWorkflowAction({
            workflow,
            inode: this.$store.contentlet()?.inode,
            contentType: this.$store.contentType().variable,
            languageId: currentLocale ? currentLocale.id.toString() : '',
            identifier: this.$store.currentIdentifier()
        });
    }

    /**
     * Handles form value changes and updates the store.
     *
     * @param value - The updated form values
     */
    onFormChange(value: FormValues) {
        this.$store.onFormChange(value);
    }

    /**
     * Closes beta feature messages.
     *
     * @param message - The type of message to close
     */
    closeMessage(message: 'betaMessage') {
        if (message === 'betaMessage') {
            // We need to store this in the store to persist the state
            this.$store.toggleBetaMessage();
        }
    }
}
