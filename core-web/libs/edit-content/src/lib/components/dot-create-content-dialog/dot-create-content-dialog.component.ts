import {
    ChangeDetectionStrategy,
    Component,
    inject,
    OnInit,
    OnDestroy,
    effect,
    computed,
    viewChild,
    signal
} from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotContentTypeService } from '@dotcms/data-access';
import {
    DotCMSContentlet,
    FeaturedFlags,
    ComponentStatus,
    DotCMSContentType
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotEditContentLayoutComponent } from '../dot-edit-content-layout/dot-edit-content.layout.component';

/**
 * Configuration data passed to the edit content dialog
 */
export interface EditContentDialogData {
    /**
     * Mode determines whether we're creating new content or editing existing
     */
    mode: 'new' | 'edit';

    /**
     * For new content: The content type variable name or ID
     */
    contentTypeId?: string;

    /**
     * For edit content: The inode of the content to edit
     */
    contentletInode?: string;

    /**
     * Depth for loading existing content (defaults to TWO)
     */
    depth?: number;

    /**
     * Optional relationship information when creating content for relationships
     */
    relationshipInfo?: {
        parentContentletId: string;
        relationshipName: string;
        isParent: boolean;
    };

    /**
     * Optional callback for when content is successfully created or updated
     */
    onContentSaved?: (contentlet: DotCMSContentlet) => void;

    /**
     * Optional callback for when dialog is cancelled
     */
    onCancel?: () => void;
}

/**
 * Edit Content Dialog Component
 *
 * A dialog component that provides content editing functionality in a modal interface.
 * Supports both creating new content and editing existing content with automatic
 * compatibility checking and isolated state management.
 *
 * ## Features
 * - **Dual Mode Support**: Create new content or edit existing content
 * - **Compatibility Check**: Validates content type compatibility with the new editor
 * - **Isolated State**: Each dialog instance maintains its own independent state
 * - **Callback Support**: Provides callbacks for content save and cancellation events
 *
 * ## Usage Examples
 *
 * ### Creating New Content
 * ```typescript
 * const data: EditContentDialogData = {
 *   mode: 'new',
 *   contentTypeId: 'blog-post',
 *   onContentSaved: (contentlet) => {
 *     // Handle saved content
 *   }
 * };
 * ```
 *
 * ### Editing Existing Content
 * ```typescript
 * const data: EditContentDialogData = {
 *   mode: 'edit',
 *   contentletInode: 'abc123',
 *   onContentSaved: (contentlet) => {
 *     // Handle updated content
 *   }
 * };
 * ```
 */
@Component({
    selector: 'dot-edit-content-dialog',
    standalone: true,
    imports: [DotEditContentLayoutComponent, DotMessagePipe],
    templateUrl: './dot-edit-content-dialog.component.html',
    styleUrls: ['./dot-edit-content-dialog.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentDialogComponent implements OnInit, OnDestroy {
    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #dialogConfig = inject(DynamicDialogConfig);
    readonly #dotContentTypeService = inject(DotContentTypeService);

    readonly editContentLayout = viewChild<DotEditContentLayoutComponent>('editContentLayout');

    // Dialog-specific state
    protected readonly state = signal<ComponentStatus>(ComponentStatus.INIT);
    protected readonly error = signal<string | null>(null);
    protected readonly contentType = signal<DotCMSContentType | null>(null);

    // Track content changes for callback when dialog closes
    private savedContentlet = signal<DotCMSContentlet | null>(null);
    private hasContentBeenSaved = signal<boolean>(false);
    private isClosing = false;

    /**
     * Computed property to check if this content type is compatible with the new editor in dialog mode
     */
    protected readonly isCompatible = computed(() => {
        const contentType = this.contentType();
        if (!contentType) {
            return true; // Assume compatible until we know otherwise
        }

        // Check if the content type has the new editor enabled
        return contentType.metadata?.[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED] === true;
    });

    /**
     * Expose ComponentStatus enum to template
     */
    protected readonly ComponentStatus = ComponentStatus;

    /**
     * Expose dialog data to template
     */
    protected get data(): EditContentDialogData {
        return this.#dialogConfig.data;
    }

    ngOnInit(): void {
        const data: EditContentDialogData = this.#dialogConfig.data;

        if (!data) {
            throw new Error('Dialog data is required for edit content dialog');
        }

        if (data.mode === 'new' && !data.contentTypeId) {
            throw new Error('Content type ID is required when creating new content');
        }

        if (data.mode === 'edit' && !data.contentletInode) {
            throw new Error('Contentlet inode is required when editing existing content');
        }

        // Load content type for compatibility check if we're creating new content
        if (data.mode === 'new' && data.contentTypeId) {
            this.loadContentType(data.contentTypeId);
        } else {
            // For edit mode, we'll assume compatible and let the layout component handle it
            this.state.set(ComponentStatus.LOADED);
        }
    }

    constructor() {
        // Subscribe to dialog close events to handle callback when dialog is closed by any means
        this.#dialogRef.onClose.subscribe(() => {
            if (!this.isClosing) {
                this.handleDialogClose();
            }
        });

        // Effect to monitor layout component errors
        effect(() => {
            const layoutComponent = this.editContentLayout();
            if (!layoutComponent) {
                return;
            }

            const layoutError = layoutComponent.$store.error();

            if (layoutError) {
                this.error.set(layoutError);
                this.state.set(ComponentStatus.ERROR);
            }
        });
    }

    /**
     * Handles the dialog close event and executes callbacks if content was saved
     */
    private handleDialogClose(): void {
        const data: EditContentDialogData = this.#dialogConfig.data;

        // Check if content was saved during the dialog session
        const savedContent = this.savedContentlet();
        const hasBeenSaved = this.hasContentBeenSaved();

        if (hasBeenSaved && savedContent && data.onContentSaved) {
            data.onContentSaved(savedContent);
        }
    }

    /**
     * Handles content saved event from the layout component.
     * This tracks content changes but doesn't close the dialog immediately.
     * The callback will be called when the dialog is manually closed.
     */
    onContentSaved(contentlet: DotCMSContentlet): void {
        // Track the latest saved content and mark that content has been saved
        this.savedContentlet.set(contentlet);
        this.hasContentBeenSaved.set(true);
    }

    /**
     * Loads content type information for compatibility checking
     */
    private loadContentType(contentTypeId: string): void {
        this.state.set(ComponentStatus.LOADING);
        this.error.set(null);

        this.#dotContentTypeService.getContentType(contentTypeId).subscribe({
            next: (contentType) => {
                this.contentType.set(contentType);
                this.state.set(ComponentStatus.LOADED);
            },
            error: (error) => {
                this.error.set(`Failed to load content type: ${error.message}`);
                this.state.set(ComponentStatus.ERROR);
            }
        });
    }

    /**
     * Handles dialog cancellation and cleanup
     */
    closeDialog(): void {
        this.isClosing = true;

        const data: EditContentDialogData = this.#dialogConfig.data;

        // Check if content was saved during the dialog session
        const savedContent = this.savedContentlet();
        const hasBeenSaved = this.hasContentBeenSaved();

        if (hasBeenSaved && savedContent && data.onContentSaved) {
            data.onContentSaved(savedContent);
        }

        if (data.onCancel) {
            data.onCancel();
        }

        // Close dialog and return the final saved contentlet (or null if nothing was saved)
        this.#dialogRef.close(hasBeenSaved ? savedContent : null);
    }

    ngOnDestroy(): void {
        this.isClosing = true;
    }
}
