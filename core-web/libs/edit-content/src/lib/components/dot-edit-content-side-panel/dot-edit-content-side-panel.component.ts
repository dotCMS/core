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
    signal,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ButtonModule } from 'primeng/button';
import { Drawer, DrawerModule } from 'primeng/drawer';
import { DialogService, DynamicDialogConfig } from 'primeng/dynamicdialog';
import { ZIndexUtils } from 'primeng/utils';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { popFormBridge, pushFormBridge } from '@dotcms/edit-content-bridge';
import { DotMessagePipe } from '@dotcms/ui';

import {
    AngularImageEditorLauncher,
    IMAGE_EDITOR_LAUNCHER
} from '../../fields/shared/image-editor-launcher';
import { EditContentDialogData } from '../../models/dot-edit-content-dialog.interface';
import { DotSidePanelNavController } from '../../services/dot-side-panel-nav.service';
import { EDIT_CONTENT_HOST } from '../../services/host/edit-content-host.model';
import { OverlayEditContentHost } from '../../services/host/overlay-edit-content-host';
import { DotEditContentLayoutComponent } from '../dot-edit-content-layout/dot-edit-content.layout.component';

/** localStorage key persisting the user's expanded (full-width) preference for the side panel. */
const EXPANDED_STORAGE_KEY = 'dot-edit-content-side-panel-expanded';

/**
 * Reads the persisted expanded preference. Best-effort: returns `false` when storage is
 * unavailable (SSR/tests) or the value is missing, so a read failure never breaks the panel.
 */
function readExpandedPreference(): boolean {
    try {
        return localStorage.getItem(EXPANDED_STORAGE_KEY) === 'true';
    } catch {
        return false;
    }
}

/** Persists the expanded preference; silently ignores storage failures. */
function writeExpandedPreference(expanded: boolean): void {
    try {
        localStorage.setItem(EXPANDED_STORAGE_KEY, String(expanded));
    } catch {
        // best-effort: quota errors / disabled storage must not break the panel.
    }
}

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
    changeDetection: ChangeDetectionStrategy.OnPush,
    // ESC and click-outside close the panel through the unsaved-changes guard. Both are bound at
    // document level because `appendTo="body"` moves the drawer (and its mask) out of this
    // component's DOM subtree, so template listeners would never receive the events.
    host: {
        '(document:keydown.escape)': 'onEscape()',
        '(document:click)': 'onMaskClick($event)'
    }
})
export class DotEditContentSidePanelComponent implements OnDestroy {
    readonly #injector = inject(Injector);
    readonly #destroyRef = inject(DestroyRef);
    readonly #navController = inject(DotSidePanelNavController);

    /** The hosted editor; used to run its unsaved-changes guard before closing. */
    protected readonly $layout = viewChild(DotEditContentLayoutComponent);

    /**
     * This panel's own drawer. Needed for its `mask` element: the click-outside handler compares
     * the clicked node against THIS mask, not against the shared `p-drawer-mask` class, so masks
     * belonging to other drawers in the document never close this panel (see {@link onMaskClick}).
     */
    protected readonly $drawer = viewChild(Drawer);

    /** Identity (and header title) of the content to create/edit, or `null` when closed. */
    readonly data = input<EditContentDialogData | null>(null);

    /** Emitted when the user closes the panel, so the opener can clear its request. */
    readonly closed = output<void>();

    /** Emitted on each successful save, so the opener can refresh its view. */
    readonly saved = output<DotCMSContentlet>();

    /**
     * Whether the panel is expanded to the full viewport width (vs the default ~80%). Seeded from
     * the user's persisted preference so a panel opens in the mode last chosen (see
     * {@link toggleExpanded}).
     */
    protected readonly $expanded = signal(readExpandedPreference());

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

        // Collapse the main navigation while the panel is open (restored on close), and register
        // this panel on the stack so ESC only closes the frontmost one. In afterNextRender so the
        // store mutation lands after the current render, not during it.
        afterNextRender(() => this.#navController.acquire(this));

        // Forward each save to the opener so it can refresh its view. The overlay host is resolved
        // AFTER construction (afterNextRender) on purpose: resolving it in the constructor would
        // cycle through its `DynamicDialogConfig` factory, which depends on this component.
        afterNextRender(() => {
            this.#injector
                .get(OverlayEditContentHost)
                .saved$.pipe(takeUntilDestroyed(this.#destroyRef))
                .subscribe((contentlet) => {
                    this.#lastSaved = contentlet;
                    this.saved.emit(contentlet);
                });
        });
    }

    /**
     * ESC handler. Only the frontmost stacked panel responds — both stacked panels share a
     * document-level listener, so without this guard a single ESC would close the whole stack
     * instead of one panel at a time.
     */
    protected onEscape(): void {
        if (this.#hasOverlayAbove()) {
            return;
        }

        if (this.#navController.isTop(this)) {
            this.requestClose();
        }
    }

    /**
     * Whether another PrimeNG overlay is stacked on top of this panel — the image editor dialog, a
     * confirm popup, an open select panel, or a second side panel.
     *
     * Such an overlay owns the ESC key, but it is appended to `body` outside this component's DOM,
     * so the document-level listener would still fire and close the panel underneath it. Every
     * PrimeNG overlay registers itself in the shared `ZIndexUtils` stack when it opens, so comparing
     * the top of that stack against this drawer's own z-index answers "is something above me?"
     * without matching on overlay class names, which differ per component and change across versions.
     */
    #hasOverlayAbove(): boolean {
        const container = this.$drawer()?.container;

        return !!container && ZIndexUtils.getCurrent() > ZIndexUtils.get(container);
    }

    /**
     * Click-outside handler: clicking the area behind the panel closes it with the exact same
     * semantics as ESC and the X button — i.e. through the unsaved-changes guard.
     *
     * The drawer's own `dismissible` is deliberately left off: it hides the drawer and emits
     * `visibleChange(false)` the moment the mask is clicked, which both bypasses the guard (unsaved
     * edits lost silently) and desyncs the one-way `[visible]` binding. Matching the mask ourselves
     * keeps the panel on screen until the guard says it is safe to close.
     *
     * The listener is document-wide because the mask is built imperatively into `document.body`, so
     * no template binding can reach it. It matches on the mask's IDENTITY rather than its
     * `p-drawer-mask` class: that class is shared by every modal drawer in the app (the UVE block
     * editor sidebar is one), and a class check would let a foreign drawer's mask close this panel
     * and pop the unsaved-changes prompt. Comparing the element also means a click inside the panel,
     * or a drag that starts inside and ends on the mask, resolves to a different node and is
     * ignored. As with ESC, only the frontmost stacked panel reacts.
     */
    protected onMaskClick(event: MouseEvent): void {
        if (event.target !== this.$drawer()?.mask) {
            return;
        }

        if (this.#navController.isTop(this)) {
            this.requestClose();
        }
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
        const next = !this.$expanded();
        this.$expanded.set(next);
        writeExpandedPreference(next);
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
        popFormBridge();
        // Restore the main navigation once the last panel closes.
        this.#navController.release(this);
    }
}
