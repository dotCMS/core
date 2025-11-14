import {
    ChangeDetectionStrategy,
    Component,
    inject,
    input,
    model,
    effect,
    output
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { MessagesModule } from 'primeng/messages';
import { ToastModule } from 'primeng/toast';

import {
    DotContentletService,
    DotLanguagesService,
    DotVersionableService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
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
        MessagesModule,
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
        DialogService
    ],
    host: {
        '[class.edit-content--with-sidebar]': '$store.isSidebarOpen()'
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

    constructor() {
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

        // Handle workflow action success in dialog mode
        effect(() => {
            const isDialogMode = this.$store.isDialogMode();
            const workflowActionSuccess = this.$store.workflowActionSuccess();

            if (isDialogMode && workflowActionSuccess) {
                this.contentSaved.emit(workflowActionSuccess);
                this.$store.clearWorkflowActionSuccess();
            }
        });
    }

    /**
     * Opens the workflow selection dialog.
     */
    selectWorkflow() {
        this.$showDialog.set(true);
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
