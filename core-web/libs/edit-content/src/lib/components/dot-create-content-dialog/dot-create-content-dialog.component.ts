import {
    ChangeDetectionStrategy,
    Component,
    inject,
    OnInit,
    OnDestroy,
    effect,
    viewChild,
    signal
} from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotCMSContentlet, ComponentStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { EditContentDialogData } from '../../models/dot-edit-content-dialog.interface';
import { DotEditContentLayoutComponent } from '../dot-edit-content-layout/dot-edit-content.layout.component';

/**
 * Edit Content Dialog Component
 *
 * A modal dialog for creating new content or editing existing content using the Angular-based editor.
 *
 * ## Usage
 *
 * **Create new content:**
 * ```typescript
 * const data: EditContentDialogData = {
 *   mode: 'new',
 *   contentTypeId: 'blog-post'
 * };
 * ```
 *
 * **Edit existing content:**
 * ```typescript
 * const data: EditContentDialogData = {
 *   mode: 'edit',
 *   contentletInode: 'abc123'
 * };
 * ```
 */
@Component({
    selector: 'dot-edit-content-dialog',
    imports: [DotEditContentLayoutComponent, DotMessagePipe],
    templateUrl: './dot-edit-content-dialog.component.html',
    styleUrls: ['./dot-edit-content-dialog.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentDialogComponent implements OnInit, OnDestroy {
    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #dialogConfig = inject(DynamicDialogConfig);

    readonly editContentLayout = viewChild<DotEditContentLayoutComponent>('editContentLayout');

    // Dialog-specific state
    protected readonly state = signal<ComponentStatus>(ComponentStatus.INIT);
    protected readonly error = signal<string | null>(null);

    // Track content changes for callback when dialog closes
    readonly #savedContentlet = signal<DotCMSContentlet | null>(null);
    readonly #hasContentBeenSaved = signal<boolean>(false);
    #isClosing = false;

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
        // Set state to loaded since this dialog only supports the new editor
        this.state.set(ComponentStatus.LOADED);
    }

    constructor() {
        // Subscribe to dialog close events to handle callback when dialog is closed by any means
        this.#dialogRef.onClose.subscribe(() => {
            if (!this.#isClosing) {
                this.#handleDialogClose();
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
    #handleDialogClose(): void {
        const data: EditContentDialogData = this.#dialogConfig.data;

        // Check if content was saved during the dialog session
        const savedContent = this.#savedContentlet();
        const hasBeenSaved = this.#hasContentBeenSaved();

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
        this.#savedContentlet.set(contentlet);
        this.#hasContentBeenSaved.set(true);
    }

    /**
     * Handles dialog cancellation and cleanup
     */
    closeDialog(): void {
        this.#isClosing = true;

        const data: EditContentDialogData = this.#dialogConfig.data;

        // Check if content was saved during the dialog session
        const savedContent = this.#savedContentlet();
        const hasBeenSaved = this.#hasContentBeenSaved();

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
        this.#isClosing = true;
    }
}
