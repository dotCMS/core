import {
    ChangeDetectionStrategy,
    Component,
    effect,
    inject,
    input,
    model,
    output,
    untracked,
    viewChild
} from '@angular/core';

import { ConfirmationService, ConfirmEventType } from 'primeng/api';
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
import { DotCMSContentlet, DotCMSWorkflowAction } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { FormValues } from '../../models/dot-edit-content-form.interface';
import { DotEditContentService } from '../../services/dot-edit-content.service';
import { DotEditContentStore } from '../../store/edit-content.store';
import { DotEditContentCompareComponent } from '../dot-edit-content-compare/dot-edit-content-compare.component';
import { DotEditContentFormComponent } from '../dot-edit-content-form/dot-edit-content-form.component';
import { DotEditContentSidebarComponent } from '../dot-edit-content-sidebar/dot-edit-content-sidebar.component';

/**
 * Edit Content Layout Component
 *
 * A flexible component that provides the main layout for content editing functionality.
 * Supports both route-based and dialog-based usage patterns with automatic mode detection.
 *
 * ## Features
 * - **Dual Mode Support**: Works in both route and dialog contexts
 * - **Automatic Mode Detection**: Intelligently switches modes based on input parameters
 * - **Isolated State Management**: Each instance maintains its own store
 * - **Workflow Integration**: Handles content workflow actions and notifications
 *
 * ## Usage Modes
 *
 * ### Route Mode
 * Used when embedded in route-based pages. Initializes from route parameters.
 * ```html
 * <dot-edit-content-form-layout></dot-edit-content-form-layout>
 * ```
 *
 * ### Dialog Mode - New Content
 * Used in dialogs to create new content of a specific type.
 * ```html
 * <dot-edit-content-form-layout
 *   [contentTypeId]="'blog-post'"
 *   (contentSaved)="onContentSaved($event)">
 * </dot-edit-content-form-layout>
 * ```
 *
 * ### Dialog Mode - Edit Existing Content
 * Used in dialogs to edit existing content by inode.
 * ```html
 * <dot-edit-content-form-layout
 *   [contentletInode]="'abc123'"
 *   (contentSaved)="onContentSaved($event)">
 * </dot-edit-content-form-layout>
 * ```
 *
 * ## Inputs
 * - `contentTypeId`: String identifier for content type (dialog mode only)
 * - `contentletInode`: String identifier for existing content (dialog mode only)
 *
 * ## Outputs
 * - `contentSaved`: Emitted when content is successfully saved (dialog mode only)
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
        DotEditContentCompareComponent
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
     * Content type ID for dialog mode initialization.
     * When provided, enables dialog mode and initializes new content for the specified type.
     */
    readonly $contentTypeId = input<string>('', { alias: 'contentTypeId' });

    /**
     * Contentlet inode for dialog mode initialization.
     * When provided, enables dialog mode and loads existing content for editing.
     */
    readonly $contentletInode = input<string>('', { alias: 'contentletInode' });

    /**
     * Emitted when content is successfully saved through workflow actions.
     * Only fires in dialog mode to notify parent components of content changes.
     */
    readonly contentSaved = output<DotCMSContentlet>();

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
     * Present when rendered inside a PrimeNG DynamicDialog; null in route mode.
     * Used to intercept dialog close events for the dirty-content guard.
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

        // When switchLocale sets a pendingLocaleInode, ask the user to confirm
        // discarding unsaved changes before reloading for the new locale.
        // If the form is clean, switch immediately without prompting.
        effect(() => {
            const pendingInode = this.$store.pendingLocaleInode();
            if (!pendingInode) return;

            untracked(() => {
                if (this.hasUnsavedChanges()) {
                    this.#confirmIfDirty(
                        () => this.$store.confirmPendingLocaleSwitch(),
                        () => this.$store.cancelPendingLocaleSwitch()
                    );
                } else {
                    this.$store.confirmPendingLocaleSwitch();
                }
            });
        });

        // Initialize component based on input parameters
        effect(() => {
            const contentTypeId = this.$contentTypeId();
            const contentletInode = this.$contentletInode();

            if (contentTypeId || contentletInode) {
                // Dialog mode: Initialize with provided parameters
                this.$store.initializeDialogMode({
                    contentTypeId,
                    contentletInode
                });
            } else {
                // Route mode: Initialize from route parameters
                this.$store.initializeAsPortlet();
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

            if (this.$store.isDialogMode()) {
                this.contentSaved.emit(success);
            }

            this.$store.clearWorkflowActionSuccess();
        });
    }

    /**
     * Sets up two intercepts for dirty-content confirmation in dialog mode:
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
