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
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import { DotCMSContentlet, ComponentStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { FormValues } from '../../models/dot-edit-content-form.interface';
import { DotEditContentService } from '../../services/dot-edit-content.service';
import { DotEditContentStore } from '../../store/edit-content.store';
import { DotEditContentFormComponent } from '../dot-edit-content-form/dot-edit-content-form.component';
import { DotEditContentSidebarComponent } from '../dot-edit-content-sidebar/dot-edit-content-sidebar.component';

/**
 * Component that displays the edit content layout.
 * Can be used both in route-based contexts and dialog contexts.
 *
 * When used in route-based contexts, it provides its own store instance.
 * When used in dialog contexts with contentTypeId, it automatically handles initialization.
 *
 * @export
 * @class EditContentLayoutComponent
 */
@Component({
    selector: 'dot-edit-content-form-layout',
    standalone: true,
    imports: [
        DotMessagePipe,
        ButtonModule,
        ToastModule,
        MessagesModule,
        DotEditContentFormComponent,
        DotEditContentSidebarComponent,
        ConfirmDialogModule
    ],
    providers: [
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
     * Whether this component is being used in a dialog context.
     * When true, it will inherit the store from the parent dialog component.
     * When false, it will provide its own store instance.
     */
    readonly isDialogMode = input<boolean>(false);

    /**
     * Content type ID for dialog mode initialization.
     * When provided, the component will automatically enable dialog mode
     * and initialize new content for the specified content type.
     */
    readonly contentTypeId = input<string>();

    /**
     * Contentlet inode for dialog mode initialization.
     * When provided, the component will automatically enable dialog mode
     * and initialize existing content for the specified inode.
     */
    readonly contentletInode = input<string>();

    /**
     * Event emitted when content is successfully saved/updated through workflow actions.
     * Only emitted in dialog mode to notify parent components of content changes.
     */
    readonly contentSaved = output<DotCMSContentlet>();

    /**
     * The store instance.
     * Will be injected from parent when in dialog mode, or from component-level provider when in route mode.
     *
     * @type {InstanceType<typeof DotEditContentStore>}
     * @memberof EditContentLayoutComponent
     */
    readonly $store: InstanceType<typeof DotEditContentStore> = inject(DotEditContentStore);

    constructor() {
        // Effect to handle dialog mode and initialization when inputs change
        effect(
            () => {
                const contentTypeId = this.contentTypeId();
                const contentletInode = this.contentletInode();
                const isDialogMode = this.isDialogMode();

                console.log('🔧 [DotEditContentLayoutComponent] contentTypeId:', contentTypeId);

                // Check if we're in dialog mode based on inputs or explicit flag
                const shouldUseDialogMode = isDialogMode || !!contentTypeId || !!contentletInode;

                if (shouldUseDialogMode) {
                    console.log('🔧 [DotEditContentLayoutComponent] Running in DIALOG mode');
                    this.$store.enableDialogMode();

                    // Initialize based on provided inputs
                    if (contentTypeId) {
                        console.log(
                            '🔧 [DotEditContentLayoutComponent] Initializing new content for type:',
                            contentTypeId
                        );
                        this.$store.initializeNewContent(contentTypeId);
                    } else if (contentletInode) {
                        console.log(
                            '🔧 [DotEditContentLayoutComponent] Initializing existing content for inode:',
                            contentletInode
                        );
                        this.$store.initializeExistingContent({
                            inode: contentletInode,
                            depth: '2'
                        });
                    }
                } else {
                    console.log(
                        '🔧 [DotEditContentLayoutComponent] Running in ROUTE mode - using normal initialization'
                    );
                    // Store will handle route-based initialization automatically in its onInit hook
                }

                // Log store instance ID for debugging
                console.log(
                    '🔧 [DotEditContentLayoutComponent] Store instance ID:',
                    (this.$store as any)._id || 'no-id'
                );
            },
            { allowSignalWrites: true }
        );

        // Effect to monitor workflow action success and emit content saved event in dialog mode
        effect(() => {
            const isDialogMode = this.$store.isDialogMode();
            const workflowActionSuccess = this.$store.workflowActionSuccess();

            // Only emit in dialog mode when a workflow action has been successfully executed
            if (isDialogMode && workflowActionSuccess) {
                console.log(
                    '🔧 [DotEditContentLayoutComponent] Workflow action succeeded in dialog mode, emitting event:',
                    workflowActionSuccess
                );
                this.contentSaved.emit(workflowActionSuccess);

                // Reset the success signal to prevent duplicate emissions
                this.$store.clearWorkflowActionSuccess();
            }
        });
    }

    /**
     * Whether the select workflow dialog should be shown.
     *
     * @type {boolean}
     * @memberof EditContentLayoutComponent
     */
    readonly $showDialog = model<boolean>(false);

    /**
     * Emits an event to show the select workflow dialog.
     *
     * @memberof EditContentLayoutComponent
     */
    selectWorkflow() {
        this.$showDialog.set(true);
    }

    /**
     * Handles the form change event.
     *
     * @param {Record<string, string>} value
     * @memberof EditContentLayoutComponent
     */
    onFormChange(value: FormValues) {
        this.$store.onFormChange(value);
    }

    /**
     * Closes the beta message.
     *
     * @memberof EditContentLayoutComponent
     */
    closeMessage(message: 'betaMessage') {
        if (message === 'betaMessage') {
            // We need to store this in the store to persist the state
            this.$store.toggleBetaMessage();
        }
    }
}
