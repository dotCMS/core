import {
    ChangeDetectionStrategy,
    Component,
    inject,
    OnInit,
    effect,
    computed,
    viewChild
} from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotContentTypeService } from '@dotcms/data-access';
import { DotCMSContentlet, FeaturedFlags, ComponentStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotEditContentStore } from '../../store/edit-content.store';
import { DotEditContentLayoutComponent } from '../dot-edit-content-layout/dot-edit-content.layout.component';

/**
 * Configuration data passed to the create content dialog
 */
export interface CreateContentDialogData {
    /**
     * The content type variable name or ID for which to create new content
     */
    contentTypeId: string;

    /**
     * Optional relationship information when creating content for relationships
     */
    relationshipInfo?: {
        parentContentletId: string;
        relationshipName: string;
        isParent: boolean;
    };

    /**
     * Optional callback for when content is successfully created
     */
    onContentCreated?: (contentlet: DotCMSContentlet) => void;
}

/**
 * Create Content Mode enum to distinguish between new and legacy editors
 */
export enum CreateContentMode {
    NEW_EDITOR = 'new',
    LEGACY_EDITOR = 'legacy'
}

/**
 * DotCreateContentDialogComponent
 *
 * A dialog wrapper component that embeds the full DotEditContentLayoutComponent
 * for creating new content. This component provides complete isolation from
 * other content editing instances in the application.
 *
 * ## Key Features:
 * - **Component-scoped store**: Each dialog instance gets its own isolated DotEditContentStore
 * - Content type initialization for new content creation
 * - Dialog lifecycle management
 * - Content creation completion and callback handling
 * - Fallback for content types that don't support the new editor
 *
 * ## Store Isolation:
 * The component provides DotEditContentStore in its providers array, creating a
 * component-scoped instance that is isolated from other dialog instances and
 * the main content editor pages. The layout component and its children will
 * inherit this dialog-scoped store instance.
 *
 * This isolation ensures that:
 * - Multiple dialogs can be open simultaneously without interference
 * - Form state doesn't leak between dialog instances
 * - Each dialog maintains its own workflow state
 * - Store state is automatically cleaned up when the dialog closes
 *
 * @example
 * ```typescript
 * this.dialogService.open(DotCreateContentDialogComponent, {
 *   data: {
 *     contentTypeId: 'blog-post',
 *     onContentCreated: (contentlet) => {
 *       console.log('Created:', contentlet);
 *     }
 *   },
 *   header: 'Create Blog Post',
 *   width: '95%',
 *   height: '95%'
 * });
 * ```
 */
@Component({
    selector: 'dot-create-content-dialog',
    standalone: true,
    imports: [DotEditContentLayoutComponent, DotMessagePipe],
    providers: [
        // This creates a component-scoped instance of DotEditContentStore
        // that will be isolated from other instances in the app
        DotEditContentStore
    ],
    templateUrl: './dot-create-content-dialog.component.html',
    styleUrls: ['./dot-create-content-dialog.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCreateContentDialogComponent implements OnInit {
    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #dialogConfig = inject(DynamicDialogConfig);
    readonly #dotContentTypeService = inject(DotContentTypeService);
    // This injects the component-scoped store instance that is isolated from other app instances
    readonly store = inject(DotEditContentStore);
    readonly editContentLayout = viewChild<DotEditContentLayoutComponent>('editContentLayout');

    /**
     * Computed property to check if this content type is compatible with the new editor in dialog mode
     */
    protected readonly isCompatible = computed(() => {
        const contentType = this.store.contentType();
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

    ngOnInit(): void {
        const data: CreateContentDialogData = this.#dialogConfig.data;

        if (!data?.contentTypeId) {
            throw new Error('Content type ID is required for creating content');
        }

        // this.store.enableDialogMode();

        // Initialize the store for new content creation
        //this.store.initializeNewContent(data.contentTypeId);
        //this.editContentLayout().$store.enableDialogMode();
    }

    constructor() {
        // Effect to handle when content is successfully saved
        // effect(() => {
        //     const contentlet = this.store.contentlet();
        //     const formValues = this.store.formValues();
        //     const state = this.store.state();
        //     const data: CreateContentDialogData = this.#dialogConfig.data;
        //     // Check if we have a new contentlet that was just created
        //     // We can detect this by checking if we have form values but the contentlet
        //     // has an inode (meaning it was saved)
        //     if (
        //         contentlet?.inode &&
        //         state === ComponentStatus.LOADED &&
        //         Object.keys(formValues).length > 0
        //     ) {
        //         // Call the callback if provided
        //         if (data.onContentCreated) {
        //             data.onContentCreated(contentlet);
        //         }
        //         // Close dialog and return the created contentlet
        //         this.#dialogRef.close(contentlet);
        //     }
        // });
        // // Effect to handle errors and non-compatible content types
        // effect(() => {
        //     const error = this.store.error();
        //     const isCompatible = this.isCompatible();
        //     if (error) {
        //         console.error('Error in create content dialog:', error);
        //         // Could show a toast or handle error state
        //     }
        //     // If not compatible, we could potentially redirect or show alternative options
        //     if (!isCompatible) {
        //         console.warn('Content type not compatible with new editor in dialog mode');
        //     }
        // });
    }
}
