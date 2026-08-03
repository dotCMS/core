import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    inject,
    input,
    output,
    signal,
    untracked
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { switchMap, take } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { PageDiffFile } from '../models/page-render-sources.models';
import { DotPageSourcesService } from '../services/dot-page-sources.service';
import { AccessibilityStudioStore } from '../store/accessibility-studio.store';

/** Load status of the file list. */
type DiffStatus = 'loading' | 'loaded' | 'error';

/**
 * The "working vs live" changed-file list — the body of the side panel's "Files"
 * accordion panel. The run screen owns the panel chrome (header, count badge,
 * Publish action); this is just the list.
 *
 * One row per source file that DIFFERS between the working (unpublished) and live
 * (published) versions. Selecting a file emits it upward: the run screen swaps its
 * RIGHT pane from the preview to that file's diff, so the narrow side panel stays a
 * list and the diff gets the full width. A "Back to preview" control here clears the
 * selection, so the user can leave the diff view without reaching into the right
 * pane. When nothing differs it says so rather than hiding.
 *
 * It's a presentational child of {@link DotA11yRunComponent}: the page context
 * comes from the shared {@link AccessibilityStudioStore} (already hydrated by the
 * run screen), so there's no routing/rehydration here. The list loads as soon as
 * the page is known — before any scan — so pre-existing working edits (an earlier
 * run, a manual change) are visible immediately, and reloads whenever the working
 * render changes (each run, re-scan, publish).
 *
 * Data path (see {@link DotPageSourcesService}):
 *   `_render-sources` → flatten to file assets → per file, fetch working + live
 *   text via each version's `/dA/<inode>/…` URL → keep only the ones that differ.
 */
@Component({
    selector: 'dot-a11y-diff',
    standalone: true,
    imports: [DotMessagePipe],
    templateUrl: './a11y-diff.component.html',
    providers: [DotPageSourcesService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'block' }
})
export class DotA11yDiffComponent {
    readonly store = inject(AccessibilityStudioStore);

    private readonly sourcesService = inject(DotPageSourcesService);
    private readonly destroyRef = inject(DestroyRef);

    /**
     * The file whose diff the right pane should show, or null for the preview.
     * The run screen owns the right pane, so the selection is published upward
     * rather than rendered here.
     */
    readonly fileSelected = output<PageDiffFile | null>();

    /**
     * Which file the right pane is currently diffing, owned by the run screen. Fed
     * back in so the highlighted row follows the pane — including when the pane is
     * closed from its own control rather than from this list.
     */
    readonly activeFileId = input<string | null>(null);

    /**
     * How many files differ, emitted on every (re)load. The run screen owns the
     * panel header's count badge and the Publish action, so it needs this rather
     * than reaching into the child.
     */
    readonly changedCount = output<number>();

    /** Changed files (working ≠ live); empty until the first load resolves. */
    readonly files = signal<PageDiffFile[]>([]);
    readonly status = signal<DiffStatus>('loading');

    /** Identifier of the file being diffed in the right pane; null → preview. */
    readonly selectedId = computed(() => this.activeFileId());

    /** The currently selected diff file. */
    readonly selected = computed<PageDiffFile | null>(() => {
        const id = this.selectedId();

        return this.files().find((f) => f.identifier === id) ?? null;
    });

    /** True once loaded and there are no changed files to show. */
    readonly empty = computed(() => this.status() === 'loaded' && this.files().length === 0);

    /**
     * Cache key of the last-loaded list: page identifier + a revision that bumps
     * whenever the working render changes (each run, re-scan, publish). Reloading
     * on the revision — not just the page — means a new run's fixes (and any prior
     * or manual working edits) show up as soon as they land.
     */
    private loadedKey: string | null = null;

    constructor() {
        // Load the file list as soon as the page is known — before any scan — and
        // reload whenever the working render changes (each run, re-scan, publish).
        // Keyed on page + revision so a no-op change doesn't refetch.
        effect(() => {
            const page = this.store.selected();
            const revision = this.store.previewRevision();
            if (!page) {
                return;
            }
            const key = `${page.identifier}#${revision}`;
            untracked(() => {
                if (this.loadedKey !== key) {
                    this.loadedKey = key;
                    this.loadDiff(page.path, page.hostId, page.languageId);
                }
            });
        });
    }

    /** Fetch the page's source files, resolve their working-vs-live diffs. */
    private loadDiff(path: string, hostId: string, languageId: number): void {
        this.status.set('loading');
        this.sourcesService
            .getPageSources(path, hostId, languageId)
            .pipe(
                switchMap((sources) => this.sourcesService.getDiffFiles(sources, languageId)),
                take(1),
                takeUntilDestroyed(this.destroyRef)
            )
            .subscribe({
                next: (files) => {
                    this.files.set(files);
                    // A reload can drop the file the right pane was showing (e.g. a
                    // publish makes working == live). Close the pane in that case so
                    // it isn't left diffing something no longer in the list.
                    const openId = this.selectedId();
                    if (openId && !files.some((f) => f.identifier === openId)) {
                        this.fileSelected.emit(null);
                    }
                    this.status.set('loaded');
                    this.changedCount.emit(files.length);
                },
                error: () => {
                    this.status.set('error');
                    this.changedCount.emit(0);
                }
            });
    }

    /** Open a file's diff in the right pane. */
    selectFile(identifier: string): void {
        this.fileSelected.emit(this.files().find((f) => f.identifier === identifier) ?? null);
    }

    /** Leave the diff view — the right pane goes back to the preview. */
    clearSelection(): void {
        this.fileSelected.emit(null);
    }
}
