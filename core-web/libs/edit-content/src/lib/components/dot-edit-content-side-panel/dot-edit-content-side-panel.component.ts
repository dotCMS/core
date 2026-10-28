import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    Injector,
    OnDestroy,
    afterNextRender,
    computed,
    forwardRef,
    inject,
    input,
    output,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { DialogService, DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { popFormBridge, pushFormBridge } from '@dotcms/edit-content-bridge';
import {
    ASSET_PICKER_LAUNCHER,
    AngularAssetPickerLauncher,
    DotSidePanelContent,
    DotSidePanelRef
} from '@dotcms/ui';

import {
    AngularImageEditorLauncher,
    IMAGE_EDITOR_LAUNCHER
} from '../../fields/shared/image-editor-launcher';
import { EditContentDialogData } from '../../models/dot-edit-content-dialog.interface';
import { EDIT_CONTENT_HOST } from '../../services/host/edit-content-host.model';
import { OverlayEditContentHost } from '../../services/host/overlay-edit-content-host';
import { DotEditContentLayoutComponent } from '../dot-edit-content-layout/dot-edit-content.layout.component';

/**
 * The new Edit Content editor as a side panel's content, as an alternative to the full-screen route
 * or the centered dialog. Openers open it through `DotSidePanelService` (or `injectSidePanel`), with
 * the content's title as the heading; it closes the panel through its `DotSidePanelRef`.
 *
 * It reuses the overlay editor plumbing: it provides {@link OverlayEditContentHost} (identity from
 * the dialog config, in-place navigation, chrome no-ops) and, since it is not opened through
 * `DialogService`, supplies the {@link DynamicDialogConfig} the host reads identity from — built
 * from the {@link data} input.
 *
 * It also self-provides `DialogService` and {@link IMAGE_EDITOR_LAUNCHER} — the same pair
 * `EditContentShellComponent` provides for the full-screen route. None of this panel's openers
 * (Content Drive, Query Tool, UVE) provide them, so without this the file field's `IMAGE_EDITOR_LAUNCHER`
 * injection (`{ optional: true }`) silently resolves to `undefined`: the "Edit image" action
 * disappears for Image/File fields, and Binary falls back to the legacy Dojo editor instead of the
 * new one. Providing both here — rather than in each opener — fixes it for all three at once.
 */
@Component({
    selector: 'dot-edit-content-side-panel',
    imports: [DotEditContentLayoutComponent],
    // Fills the drawer, so the editor layout's `height: 100%` resolves against it.
    host: { class: 'block h-full' },
    providers: [
        OverlayEditContentHost,
        { provide: EDIT_CONTENT_HOST, useExisting: OverlayEditContentHost },
        // Required by AngularImageEditorLauncher to open the new image editor as a modal.
        DialogService,
        { provide: IMAGE_EDITOR_LAUNCHER, useClass: AngularImageEditorLauncher },
        // Same reasoning for the AssetPicker: without this the three asset-selection entry
        // points would fall back to the legacy picker inside this panel.
        { provide: ASSET_PICKER_LAUNCHER, useClass: AngularAssetPickerLauncher },
        {
            // The overlay host reads the content identity from the dialog config; this panel is not
            // opened through DialogService, so feed it from the `data` input. The `data` getter is
            // lazy on purpose: it defers reading the input until the host actually resolves the
            // identity, by which point Angular has applied the input.
            provide: DynamicDialogConfig,
            useFactory: (panel: DotEditContentSidePanelComponent) => ({
                get data() {
                    return panel.data();
                }
            }),
            deps: [forwardRef(() => DotEditContentSidePanelComponent)]
        }
    ],
    templateUrl: './dot-edit-content-side-panel.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidePanelComponent implements DotSidePanelContent, OnDestroy {
    readonly #injector = inject(Injector);
    readonly #destroyRef = inject(DestroyRef);
    readonly #panel = inject(DotSidePanelRef);

    /** The hosted editor; used to run its unsaved-changes guard before closing. */
    protected readonly $layout = viewChild(DotEditContentLayoutComponent);

    /** Identity of the content to create or edit. The editor is created once it is set. */
    readonly data = input<EditContentDialogData | null>(null);

    /** Emitted on each successful save, so the opener can refresh its view. */
    readonly saved = output<DotCMSContentlet>();

    /**
     * The author switched language inside the editor, which reloads in place. The opener can name
     * the language now open in its URL (#37759, FR-020).
     */
    readonly languageChanged = output<number>();

    /** Last successfully-saved contentlet, forwarded to `data.onContentSaved` when the panel closes. */
    #lastSaved: DotCMSContentlet | null = null;

    /**
     * `@for` source: a single-item list. Rendering the editor through `@for` (instead of directly)
     * defers its creation until the input has a value — the editor resolves its identity
     * synchronously on construction, so it must not be created before `data` is applied.
     */
    protected readonly $items = computed(() => {
        const data = this.data();

        return data ? [data] : [];
    });

    constructor() {
        // Give the editor a clean form-bridge slot; restore the previous one on close.
        pushFormBridge();

        // Forward each save to the opener so it can refresh its view. The overlay host is resolved
        // AFTER construction (afterNextRender) on purpose: resolving it in the constructor would
        // cycle through its `DynamicDialogConfig` factory, which depends on this component.
        afterNextRender(() => {
            const host = this.#injector.get(OverlayEditContentHost);

            // The panel header draws the chip; the editor reports its status to the overlay host and
            // the panel's ref follows that signal.
            this.#panel.bindTitleStatus(host.titleStatus);

            host.saved$.pipe(takeUntilDestroyed(this.#destroyRef)).subscribe((contentlet) => {
                this.#lastSaved = contentlet;
                this.saved.emit(contentlet);
            });

            // The content was deleted: close without the unsaved-changes guard (nothing is left to
            // keep) and without reporting a save, so the opener never receives deleted content.
            host.left$.pipe(takeUntilDestroyed(this.#destroyRef)).subscribe(() => {
                this.#lastSaved = null;
                this.#fireCloseCallbacks();
                this.#panel.close();
            });

            host.languageChanged$
                .pipe(takeUntilDestroyed(this.#destroyRef))
                .subscribe((languageId) => this.languageChanged.emit(languageId));
        });
    }

    /**
     * Close intent (X button, ESC, a mask click, or browser Back routed by the opener). Routes through
     * the editor's unsaved-changes guard so the user is prompted when the form is dirty, and closes
     * the panel only once it is safe. On close it fires the `data` callbacks (`onContentSaved` with
     * the last save, then `onCancel`), mirroring the dialog's contract so dialog-based openers can
     * switch to the panel unchanged.
     */
    requestClose(): void {
        const layout = this.$layout();
        const proceed = () => {
            this.#fireCloseCallbacks();
            this.#panel.close();
        };

        if (layout) {
            layout.confirmClose(proceed);
        } else {
            proceed();
        }
    }

    /** Fires the `data` lifecycle callbacks on close, matching {@link DotEditContentDialogComponent}. */
    #fireCloseCallbacks(): void {
        const data = this.data();

        if (this.#lastSaved) {
            data?.onContentSaved?.(this.#lastSaved);
        }

        data?.onCancel?.();
    }

    ngOnDestroy(): void {
        // The drawer releases its own registrations; only the form bridge is this content's.
        popFormBridge();
    }
}
