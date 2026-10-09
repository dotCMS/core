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

import { ButtonModule } from 'primeng/button';
import { Drawer, DrawerModule } from 'primeng/drawer';
import { DialogService, DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { popFormBridge, pushFormBridge } from '@dotcms/edit-content-bridge';
import { ASSET_PICKER_LAUNCHER, AngularAssetPickerLauncher, DotMessagePipe } from '@dotcms/ui';

import {
    AngularImageEditorLauncher,
    IMAGE_EDITOR_LAUNCHER
} from '../../fields/shared/image-editor-launcher';
import { EditContentDialogData } from '../../models/dot-edit-content-dialog.interface';
import { injectSidePanelChrome } from '../../services/dot-side-panel-chrome';
import { EDIT_CONTENT_HOST } from '../../services/host/edit-content-host.model';
import { OverlayEditContentHost } from '../../services/host/overlay-edit-content-host';
import { DotEditContentLayoutComponent } from '../dot-edit-content-layout/dot-edit-content.layout.component';

/**
 * Renders the new Edit Content editor inside a right-to-left slide-in panel (`p-drawer`), as an
 * alternative to the full-screen route or the centered dialog.
 *
 * It reuses the overlay editor plumbing: it provides {@link OverlayEditContentHost} (identity from
 * the dialog config, in-place navigation, chrome no-ops) and, since it is not opened through
 * `DialogService`, supplies the {@link DynamicDialogConfig} the host reads identity from — built
 * from the {@link data} input. The header shows the content title plus an expand toggle (80% ↔
 * full width) and a close button.
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
    imports: [DrawerModule, ButtonModule, DotEditContentLayoutComponent, DotMessagePipe],
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
export class DotEditContentSidePanelComponent implements OnDestroy {
    readonly #injector = inject(Injector);
    readonly #destroyRef = inject(DestroyRef);

    /** The hosted editor; used to run its unsaved-changes guard before closing. */
    protected readonly $layout = viewChild(DotEditContentLayoutComponent);

    /** This panel's own drawer: its mask and container are what the shared chrome checks. */
    protected readonly $drawer = viewChild(Drawer);

    /**
     * Escape, a click on the mask, the side-panel stack and the full-width toggle, shared with the
     * other side panels. Escape and the mask close through {@link requestClose}, so the editor's
     * unsaved-changes guard runs first.
     */
    readonly #chrome = injectSidePanelChrome({
        panel: this,
        drawer: this.$drawer,
        requestClose: () => this.requestClose(),
        escapeLabel: 'edit.content.side-panel.shortcut.close'
    });

    /** Identity (and header title) of the content to create/edit, or `null` when closed. */
    readonly data = input<EditContentDialogData | null>(null);

    /** Emitted when the user closes the panel, so the opener can clear its request. */
    readonly closed = output<void>();

    /** Emitted on each successful save, so the opener can refresh its view. */
    readonly saved = output<DotCMSContentlet>();

    /**
     * The author switched language inside the editor, which reloads in place. The opener can name
     * the language now open in its URL (#37759, FR-020).
     */
    readonly languageChanged = output<number>();

    /**
     * Whether the panel is expanded to the full viewport width (vs the default ~80%). Seeded from
     * the user's persisted preference so a panel opens in the mode last chosen.
     */
    protected readonly $expanded = this.#chrome.expanded;

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

            host.saved$.pipe(takeUntilDestroyed(this.#destroyRef)).subscribe((contentlet) => {
                this.#lastSaved = contentlet;
                this.saved.emit(contentlet);
            });

            // The content was deleted: close without the unsaved-changes guard (nothing is left to
            // keep) and without reporting a save, so the opener never receives deleted content.
            host.left$.pipe(takeUntilDestroyed(this.#destroyRef)).subscribe(() => {
                this.#lastSaved = null;
                this.#fireCloseCallbacks();
                this.closed.emit();
            });

            host.languageChanged$
                .pipe(takeUntilDestroyed(this.#destroyRef))
                .subscribe((languageId) => this.languageChanged.emit(languageId));
        });
    }

    /**
     * Close intent (X button, ESC, or browser Back routed by the opener). Routes through the
     * editor's unsaved-changes guard so the user is prompted when the form is dirty; only closes
     * once it is safe. On close it fires the `data` callbacks (`onContentSaved` with the last save,
     * then `onCancel`) — mirroring the dialog's contract so dialog-based openers can switch to the
     * panel unchanged — and emits `closed` for openers that drive it via a signal.
     *
     * Public so an opener (e.g. Content Drive on browser Back) can route its own close intent
     * through the same guard instead of tearing the panel down and losing unsaved edits silently.
     */
    requestClose(): void {
        const layout = this.$layout();
        const proceed = () => {
            this.#fireCloseCallbacks();
            this.closed.emit();
        };

        if (layout) {
            layout.confirmClose(proceed);
        } else {
            proceed();
        }
    }

    /**
     * Toggles the expanded (full-width) state and persists it, so the next panel the user opens
     * starts in the same mode.
     */
    protected toggleExpanded(): void {
        this.#chrome.toggleExpanded();
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
        // The chrome releases its own registrations; only the form bridge is this panel's.
        popFormBridge();
    }
}
