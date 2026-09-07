import { of } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    inject,
    input,
    output,
    signal
} from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';

import {
    catchError,
    debounceTime,
    distinctUntilChanged,
    filter,
    map,
    switchMap
} from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { PageDiffFile } from '../models/page-render-sources.models';
import { DotPageSourcesService } from '../services/dot-page-sources.service';
import { A11yRunStore } from '../store/a11y-run.store';

/** Load status of the file list. */
type DiffStatus = 'loading' | 'loaded' | 'error';

/**
 * How long to wait for `previewRevision` to settle before reloading the file list.
 * Long enough to collapse a burst of SSE progress frames into one load, short enough
 * that a terminal frame still updates the panel promptly.
 */
export const DIFF_RELOAD_DEBOUNCE_MS = 400;

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
 * comes from the run screen's {@link A11yRunStore} (which this injects up the DI
 * tree), so there's no routing/rehydration here. The list loads as soon as
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
    imports: [DotMessagePipe],
    templateUrl: './a11y-diff.component.html',
    providers: [DotPageSourcesService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'block' }
})
export class DotA11yDiffComponent {
    readonly store = inject(A11yRunStore);

    readonly #sourcesService = inject(DotPageSourcesService);
    readonly #destroyRef = inject(DestroyRef);

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
    readonly $files = signal<PageDiffFile[]>([]);
    readonly $status = signal<DiffStatus>('loading');

    /** Identifier of the file being diffed in the right pane; null → preview. */
    readonly $selectedId = computed(() => this.activeFileId());

    /** The currently selected diff file. */
    readonly $selected = computed<PageDiffFile | null>(() => {
        const id = this.$selectedId();

        return this.$files().find((f) => f.identifier === id) ?? null;
    });

    /** True once loaded and there are no changed files to show. */
    readonly $empty = computed(() => this.$status() === 'loaded' && this.$files().length === 0);

    constructor() {
        // Load the file list as soon as the page is known — before any scan — and reload
        // whenever the working render changes (each run, re-scan, publish).
        //
        // Built as a stream rather than an effect for two reasons, both about
        // `previewRevision` being a HOT key: it bumps on every SSE progress frame, and one
        // load is a `_render-sources` call plus two fetches per source file.
        //   - `debounceTime` collapses a burst of frames into a single load, instead of
        //     dozens of overlapping request sets per run.
        //   - `switchMap` makes the newest load the only one that can write. Previously
        //     nothing superseded an in-flight load, so a slow early response could land
        //     after a fast later one and set a stale `files`/`changedCount` — and since
        //     the Publish bar is gated on `changedFileCount`, that could flip
        //     `hasChangedFiles()` back to FALSE after the agent had written files,
        //     blocking publish until request ordering happened to favour it.
        toObservable(
            computed(() => {
                const page = this.store.selected();

                return page
                    ? {
                          key: `${page.identifier}#${this.store.previewRevision()}`,
                          path: page.path,
                          hostId: page.hostId,
                          languageId: page.languageId
                      }
                    : null;
            })
        )
            .pipe(
                filter((request) => request !== null),
                distinctUntilChanged((a, b) => a.key === b.key),
                debounceTime(DIFF_RELOAD_DEBOUNCE_MS),
                switchMap((request) => {
                    this.$status.set('loading');

                    return this.#sourcesService
                        .getPageSources(request.path, request.hostId, request.languageId)
                        .pipe(
                            switchMap((sources) =>
                                this.#sourcesService.getDiffFiles(sources, request.languageId)
                            ),
                            map((files) => ({ files, failed: false })),
                            catchError(() => of({ files: [] as PageDiffFile[], failed: true }))
                        );
                }),
                takeUntilDestroyed(this.#destroyRef)
            )
            .subscribe(({ files, failed }) => {
                if (failed) {
                    this.$status.set('error');
                    this.changedCount.emit(0);

                    return;
                }

                this.$files.set(files);
                // A reload can drop the file the right pane was showing (e.g. a
                // publish makes working == live). Close the pane in that case so
                // it isn't left diffing something no longer in the list.
                const openId = this.$selectedId();
                if (openId && !files.some((f) => f.identifier === openId)) {
                    this.fileSelected.emit(null);
                }
                this.$status.set('loaded');
                this.changedCount.emit(files.length);
            });
    }

    /** Open a file's diff in the right pane. */
    selectFile(identifier: string): void {
        this.fileSelected.emit(this.$files().find((f) => f.identifier === identifier) ?? null);
    }

    /** Leave the diff view — the right pane goes back to the preview. */
    clearSelection(): void {
        this.fileSelected.emit(null);
    }
}
