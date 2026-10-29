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

import { take } from 'rxjs/operators';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { type FormBridgeFrame, popFormBridge, pushFormBridge } from '@dotcms/edit-content-bridge';
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
import { DotPageEditorRequest } from '../../models/dot-edit-content-panel-events.model';
import { DotEditContentRequest } from '../../models/dot-edit-content-request.model';
import { DotOpenContentEvent } from '../../models/dot-open-content-event.model';
import { EDIT_CONTENT_HOST } from '../../services/host/edit-content-host.model';
import { OverlayEditContentHost } from '../../services/host/overlay-edit-content-host';
import { DotEditContentLayoutComponent } from '../dot-edit-content-layout/dot-edit-content.layout.component';

/**
 * The new Edit Content editor as a side panel's content, as an alternative to the full-screen route.
 * Surfaces open it through `DotEditContentOpenerService`, with the content's title as the heading;
 * it closes the panel through its `DotSidePanelRef` and reports through its outputs only.
 *
 * It reuses the overlay editor plumbing: it provides {@link OverlayEditContentHost} (identity from
 * the request, in-place navigation, chrome no-ops) and, since it is not opened through
 * `DialogService`, supplies the {@link DynamicDialogConfig} the host reads the request from.
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
            // opened through DialogService, so its `data` is the request. The getter is lazy on
            // purpose: it defers reading the input until the host actually resolves the identity,
            // by which point Angular has applied it.
            provide: DynamicDialogConfig,
            useFactory: (panel: DotEditContentSidePanelComponent) => ({
                get data() {
                    return panel.request();
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

    /** What to create or edit (#38303). The editor is created once it is set. */
    readonly request = input<DotEditContentRequest | null>(null);

    /** Emitted on each successful save, so the opener can refresh its view. */
    readonly saved = output<DotCMSContentlet>();

    /**
     * The author switched language inside the editor, which reloads in place. The opener can name
     * the language now open in its URL (#37759, FR-020).
     */
    readonly languageChanged = output<number>();

    /** The content was deleted from inside the editor. The panel closes right after. */
    readonly deleted = output<void>();

    /** The panel closed, for any reason. Emitted once. */
    readonly closed = output<void>();

    /**
     * Part of the event set both editor panels share, so the opener binds the same outputs to
     * either. The new editor never asks to open the page editor, so this never emits.
     */
    readonly pageEditorRequested = output<DotPageEditorRequest>();

    /**
     * Content a field in this editor asked to open, for the opener to stack. Part of the shared
     * event set; the fields start asking through it in a later layer of #38303.
     */
    readonly openContent = output<DotOpenContentEvent>();

    /**
     * `@for` source: a single-item list. Rendering the editor through `@for` (instead of directly)
     * defers its creation until the request has a value — the editor resolves its identity
     * synchronously on construction, so it must not be created before `request` is applied.
     */
    protected readonly $items = computed(() => {
        const request = this.request();

        return request ? [request] : [];
    });

    /**
     * This panel's place on the form bridge's stack. Popped by handle, so closing a panel while
     * another is still stacked over it removes this panel's frame, not the stacked one's (#38303).
     */
    readonly #formBridgeFrame: FormBridgeFrame;

    constructor() {
        // Give the editor a clean form-bridge slot; restore the previous one on close.
        this.#formBridgeFrame = pushFormBridge();

        this.#panel.closed.pipe(take(1), takeUntilDestroyed()).subscribe(() => this.closed.emit());

        // Forward each save to the opener so it can refresh its view. The overlay host is resolved
        // AFTER construction (afterNextRender) on purpose: resolving it in the constructor would
        // cycle through its `DynamicDialogConfig` factory, which depends on this component.
        afterNextRender(() => {
            const host = this.#injector.get(OverlayEditContentHost);

            host.saved$
                .pipe(takeUntilDestroyed(this.#destroyRef))
                .subscribe((contentlet) => this.saved.emit(contentlet));

            // The content was deleted: report it, then close without the unsaved-changes guard
            // (nothing is left to keep) and without reporting a save, so the opener never receives
            // deleted content.
            host.left$.pipe(takeUntilDestroyed(this.#destroyRef)).subscribe(() => {
                this.deleted.emit();
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
     * the panel only once it is safe. The opener hears about it through `closed`.
     */
    requestClose(): void {
        const layout = this.$layout();
        const proceed = () => this.#panel.close();

        if (layout) {
            layout.confirmClose(proceed);
        } else {
            proceed();
        }
    }

    /** Gives back this panel's form-bridge frame; the drawer releases its own registrations. */
    ngOnDestroy(): void {
        popFormBridge(this.#formBridgeFrame);
    }
}
