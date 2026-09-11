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
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ButtonModule } from 'primeng/button';
import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotCMSContentlet, ComponentStatus } from '@dotcms/dotcms-models';
import { pushFormBridge, popFormBridge } from '@dotcms/edit-content-bridge';
import { ASSET_PICKER_LAUNCHER, AngularAssetPickerLauncher, DotMessagePipe } from '@dotcms/ui';

import {
    AngularImageEditorLauncher,
    IMAGE_EDITOR_LAUNCHER
} from '../../fields/shared/image-editor-launcher';
import { EditContentDialogData } from '../../models/dot-edit-content-dialog.interface';
import { EDIT_CONTENT_HOST } from '../../services/host/edit-content-host.model';
import { OverlayEditContentHost } from '../../services/host/overlay-edit-content-host';
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
    imports: [DotEditContentLayoutComponent, DotMessagePipe, ButtonModule],
    providers: [
        // Overlay presentation: identity comes from the dialog config, navigation
        // is in-place, and chrome updates are no-ops. Inherited by the layout
        // rendered in the template and its store. The concrete class is provided so
        // this component can read `saved$`; the layout/store see it via the token.
        OverlayEditContentHost,
        { provide: EDIT_CONTENT_HOST, useExisting: OverlayEditContentHost },
        // This is an Angular Edit Content host too — opened by UVE and by the Relationship
        // field — so the new AssetPicker belongs here as much as in the shell and the side
        // panel. Without it the three asset-selection entry points would silently fall back to
        // the legacy picker in both of those flows. The launcher borrows the caller's
        // `DialogService`, so it needs no provider of its own — but the image editor launcher
        // below does, which is why `DialogService` is provided here as well.
        { provide: ASSET_PICKER_LAUNCHER, useClass: AngularAssetPickerLauncher },
        // Same host-capability reasoning for the image editor, and the same pair the shell and the
        // side panel provide. Without them the file field's `inject(IMAGE_EDITOR_LAUNCHER,
        // { optional: true })` resolved to `undefined` and Image/File fields opened the legacy Dojo
        // editor instead of the new one — no error, just the wrong editor (#37398).
        DialogService,
        { provide: IMAGE_EDITOR_LAUNCHER, useClass: AngularImageEditorLauncher }
    ],
    templateUrl: './dot-edit-content-dialog.component.html',
    styleUrls: ['./dot-edit-content-dialog.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentDialogComponent implements OnInit, OnDestroy {
    readonly #dialogRef = inject(DynamicDialogRef);
    readonly #dialogConfig = inject(DynamicDialogConfig);
    readonly #host = inject(OverlayEditContentHost);

    readonly editContentLayout = viewChild<DotEditContentLayoutComponent>('editContentLayout');

    // Dialog-specific state
    protected readonly state = signal<ComponentStatus>(ComponentStatus.INIT);
    protected readonly error = signal<string | null>(null);

    // Track content changes for callback when dialog closes
    readonly #savedContentlet = signal<DotCMSContentlet | null>(null);
    readonly #hasContentBeenSaved = signal<boolean>(false);

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
        this.state.set(ComponentStatus.LOADED);
    }

    constructor() {
        pushFormBridge();

        // Track saves reported by the editor through the host (replaces the old
        // (contentSaved) output binding). The callback fires on close.
        this.#host.saved$.pipe(takeUntilDestroyed()).subscribe((contentlet) => {
            this.#savedContentlet.set(contentlet);
            this.#hasContentBeenSaved.set(true);
        });

        // Single source of truth for callbacks — only fires when the close actually completes.
        // This prevents callbacks from firing if the dirty-close guard cancels the close.
        this.#dialogRef.onClose.pipe(takeUntilDestroyed()).subscribe(() => {
            this.#handleDialogClose();
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
     * Fires all close callbacks. Called only from the onClose subscription so
     * callbacks never run if the dirty-close guard cancels the close.
     */
    #handleDialogClose(): void {
        const data: EditContentDialogData = this.#dialogConfig.data;
        const savedContent = this.#savedContentlet();
        const hasBeenSaved = this.#hasContentBeenSaved();

        if (hasBeenSaved && savedContent && data.onContentSaved) {
            data.onContentSaved(savedContent);
        }

        if (data.onCancel) {
            data.onCancel();
        }
    }

    /**
     * Requests the dialog to close. Callbacks fire only if the dirty-close guard
     * does not cancel the close (i.e. from #handleDialogClose via onClose).
     */
    closeDialog(): void {
        const hasBeenSaved = this.#hasContentBeenSaved();
        const savedContent = this.#savedContentlet();
        this.#dialogRef.close(hasBeenSaved ? savedContent : null);
    }

    ngOnDestroy(): void {
        popFormBridge();
    }
}
