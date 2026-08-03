import { MonacoEditorLoaderService } from '@materia-ui/ngx-monaco-editor';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    ElementRef,
    inject,
    input,
    signal,
    untracked,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { filter, switchMap, take } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { PageDiffFile } from '../models/page-render-sources.models';
import { DotPageSourcesService } from '../services/dot-page-sources.service';
import { AccessibilityStudioStore } from '../store/accessibility-studio.store';

/** Load status of the diff panel. */
type DiffStatus = 'loading' | 'loaded' | 'error';

/** Monaco language id per source extension — everything else falls back to plaintext. */
const LANGUAGE_BY_EXTENSION: Record<string, string> = {
    vtl: 'html',
    html: 'html',
    css: 'css',
    scss: 'scss',
    sass: 'scss',
    js: 'javascript',
    ts: 'typescript',
    json: 'json'
};

/**
 * The "working vs live" file diff — an inline panel that fills the run screen's
 * "Code" tab (beside the "Preview" tab), so the visual before/after and the
 * source-code before/after share the same space.
 *
 * Lists the page's source files that DIFFER between the working (unpublished) and
 * live (published) versions beside a read-only Monaco side-by-side diff of the
 * selected file.
 *
 * It's a presentational child of {@link DotA11yRunComponent}: the page context
 * comes from the shared {@link AccessibilityStudioStore} (already hydrated by the
 * run screen), so there's no routing/rehydration here. The {@link active} input
 * tells it the Code tab is selected — it lazy-loads the diff and (re)lays out the
 * Monaco editor only then (the host has no size while the Preview tab is showing).
 *
 * Data path (see {@link DotPageSourcesService}):
 *   `_render-sources` → flatten to file assets → per file, fetch working + live
 *   text via each version's `/dA/<inode>/…` URL → keep only the ones that differ.
 *
 * The Monaco diff editor is created imperatively against the `monaco` global
 * (the app registers `MonacoEditorModule` in app.config, so the AMD loader is
 * configured); `@materia-ui/ngx-monaco-editor` only exposes the plain editor, so
 * the diff editor has no Angular wrapper.
 */
@Component({
    selector: 'dot-a11y-diff',
    standalone: true,
    imports: [ButtonModule, TooltipModule, DotMessagePipe],
    templateUrl: './a11y-diff.component.html',
    providers: [DotPageSourcesService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'grid h-full min-h-0 grid-cols-[300px_1fr] bg-surface-100' }
})
export class DotA11yDiffComponent {
    readonly store = inject(AccessibilityStudioStore);

    private readonly sourcesService = inject(DotPageSourcesService);
    private readonly monacoLoader = inject(MonacoEditorLoaderService);
    private readonly destroyRef = inject(DestroyRef);

    /**
     * Whether the Code tab is currently selected. Drives the lazy data load and
     * the Monaco (re)layout — while the Preview tab is showing, this panel is
     * hidden (zero-size), so the editor mustn't try to size itself.
     */
    readonly active = input<boolean>(false);

    /** The Monaco diff editor host element. */
    private readonly diffHost = viewChild<ElementRef<HTMLDivElement>>('diffHost');

    /** Changed files (working ≠ live); empty until the first load resolves. */
    readonly files = signal<PageDiffFile[]>([]);
    /** Identifier of the file shown in the diff editor. */
    readonly selectedId = signal<string | null>(null);
    readonly status = signal<DiffStatus>('loading');

    /** The currently selected diff file. */
    readonly selected = computed<PageDiffFile | null>(() => {
        const id = this.selectedId();

        return this.files().find((f) => f.identifier === id) ?? null;
    });

    /** True once loaded and there are no changed files to show. */
    readonly empty = computed(() => this.status() === 'loaded' && this.files().length === 0);

    /**
     * Whether the run is awaiting review (done phase) — drives the toolbar's
     * Discard/Publish actions. Before then there's nothing to publish.
     */
    readonly awaitingReview = computed(() => this.store.isDone());

    /**
     * True once the user has opened at least one file's diff. Publishing is gated
     * on this so the user can't promote to live without actually reviewing a change.
     */
    readonly reviewed = signal(false);

    /**
     * Publish is allowed only while awaiting review, once there is at least one
     * changed file AND the user has viewed one. Promotes the whole working version
     * (all diffs shown — this run's fixes plus any prior-run / manual edits).
     */
    readonly canPublish = computed(
        () => this.awaitingReview() && this.files().length > 0 && this.reviewed()
    );

    /** The live Monaco diff editor, disposed on destroy. */
    private editor: MonacoDiffEditor | null = null;
    /** True once the monaco global has loaded. */
    private readonly monacoReady = signal(false);
    /**
     * Cache key of the last-loaded diff: page identifier + a revision that bumps
     * whenever the working render changes (each run, re-scan, publish). Reloading
     * on the revision — not just the page — means a new run's fixes (and any prior
     * or manual working edits) show up when the user re-opens the Code tab.
     */
    private loadedKey: string | null = null;

    constructor() {
        // Lazily (re)load the diff when the Code tab is open. The run screen keeps
        // this component mounted across tab switches, so we key off `active` + the
        // selected page + the working-render revision rather than a lifecycle hook,
        // and skip the reload when nothing changed — so re-opening the tab is instant.
        effect(() => {
            const isActive = this.active();
            const page = this.store.selected();
            const revision = this.store.previewRevision();
            if (!isActive || !page) {
                return;
            }
            const key = `${page.identifier}#${revision}`;
            untracked(() => {
                if (this.loadedKey !== key) {
                    this.loadedKey = key;
                    // A fresh diff means the prior review no longer applies.
                    this.reviewed.set(false);
                    this.loadDiff(page.path, page.hostId, page.languageId);
                }
            });
        });

        // Wait for the AMD-loaded monaco global before creating the editor.
        this.monacoLoader.isMonacoLoaded$
            .pipe(
                filter((loaded) => loaded),
                take(1),
                takeUntilDestroyed()
            )
            .subscribe(() => this.monacoReady.set(true));

        // (Re)build the editor's model whenever the Code tab is active, monaco is
        // ready, and a file is selected. Gating on `active()` ensures the host has a
        // real size (it's display:none / zero under the Preview tab); a microtask
        // lets the just-shown host commit its dimensions before Monaco measures.
        effect(() => {
            const ready = this.monacoReady();
            const isActive = this.active();
            const file = this.selected();
            untracked(() => {
                if (ready && isActive && file) {
                    queueMicrotask(() => this.renderDiff(file));
                }
            });
        });

        this.destroyRef.onDestroy(() => this.disposeEditor());
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
                    // Drop a stale selection, but DON'T auto-select the first file:
                    // publishing is gated on the user opening a file themselves, so
                    // landing on the tab must not count as a review.
                    if (!files.some((f) => f.identifier === this.selectedId())) {
                        this.selectedId.set(null);
                    }
                    this.status.set('loaded');
                },
                error: () => this.status.set('error')
            });
    }

    /** Select a file to show in the diff editor — and mark the diff as reviewed. */
    selectFile(identifier: string): void {
        this.selectedId.set(identifier);
        this.reviewed.set(true);
    }

    /**
     * Promote the reviewed working version to live. Guarded by {@link canPublish}
     * (done phase + at least one file reviewed), so this can't run blind. The store
     * transition drives the rest of the UI (phase → published).
     */
    publish(): void {
        if (!this.canPublish()) {
            return;
        }
        this.store.publish();
    }

    /** Discard the working fixes → back to the scanned state (drops this run's edits). */
    discard(): void {
        if (!this.awaitingReview()) {
            return;
        }
        this.store.discard();
    }

    /** Monaco language id for a file, from its extension. */
    private languageFor(extension: string): string {
        return LANGUAGE_BY_EXTENSION[extension.toLowerCase()] ?? 'plaintext';
    }

    /**
     * Create (once) and populate the Monaco diff editor with the selected file's
     * live (original) vs working (modified) text. Read-only side-by-side.
     */
    private renderDiff(file: PageDiffFile): void {
        const host = this.diffHost()?.nativeElement;
        const monaco = getMonaco();
        if (!host || !monaco) {
            return;
        }

        if (!this.editor) {
            this.editor = monaco.editor.createDiffEditor(host, {
                theme: 'vs',
                readOnly: true,
                originalEditable: false,
                renderSideBySide: true,
                automaticLayout: true,
                minimap: { enabled: false },
                scrollBeyondLastLine: false,
                fontSize: 13,
                fontFamily: 'JetBrains Mono, Fira Code, Consolas, monospace'
            });
        }

        const language = this.languageFor(file.extension);
        // Dispose the previous models before swapping so we don't leak them.
        const previous = this.editor.getModel();
        const original = monaco.editor.createModel(file.live, language);
        const modified = monaco.editor.createModel(file.working, language);
        this.editor.setModel({ original, modified });
        previous?.original?.dispose();
        previous?.modified?.dispose();
    }

    private disposeEditor(): void {
        const model = this.editor?.getModel();
        model?.original?.dispose();
        model?.modified?.dispose();
        this.editor?.dispose();
        this.editor = null;
    }
}

// ── Minimal structural types for the imperative Monaco diff editor ──────────
// `@materia-ui/ngx-monaco-editor` exposes only the plain editor, so the diff
// editor is untyped through it. The `monaco` global carries full types via the
// ambient `monaco-editor` declarations; we narrow to just what we call to avoid a
// hard `monaco-editor` import (which would pull the full editor into this chunk).

interface MonacoTextModel {
    dispose(): void;
}
interface MonacoDiffModel {
    original: MonacoTextModel;
    modified: MonacoTextModel;
}
interface MonacoDiffEditor {
    getModel(): MonacoDiffModel | null;
    setModel(model: MonacoDiffModel): void;
    dispose(): void;
}
interface MonacoGlobal {
    editor: {
        createDiffEditor(host: HTMLElement, options: Record<string, unknown>): MonacoDiffEditor;
        createModel(value: string, language: string): MonacoTextModel;
    };
}

/** The AMD-loaded monaco global; null before the loader finishes. */
function getMonaco(): MonacoGlobal | null {
    return (window as unknown as { monaco?: MonacoGlobal }).monaco ?? null;
}
