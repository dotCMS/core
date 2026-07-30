import { MonacoEditorLoaderService } from '@materia-ui/ngx-monaco-editor';

import {
    afterNextRender,
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    ElementRef,
    inject,
    signal,
    untracked,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { filter, map, switchMap, take } from 'rxjs/operators';

import { GlobalStore } from '@dotcms/store';
import { DotMessagePipe } from '@dotcms/ui';

import { PageDiffFile } from '../models/page-render-sources.models';
import { DotPageSourcesService } from '../services/dot-page-sources.service';
import { AccessibilityStudioStore } from '../store/accessibility-studio.store';

/** Load status of the diff screen. */
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
 * The "working vs live" file diff screen (`agents/a11y/<path>/diff`).
 *
 * Lists the page's source files that DIFFER between the working (unpublished)
 * and live (published) versions — i.e. exactly what the agent changed but hasn't
 * published — beside a read-only Monaco side-by-side diff of the selected file.
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

    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    private readonly globalStore = inject(GlobalStore);
    private readonly sourcesService = inject(DotPageSourcesService);
    private readonly monacoLoader = inject(MonacoEditorLoaderService);
    private readonly destroyRef = inject(DestroyRef);

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
     * The page path this diff is opened against — reconstructed from the route
     * segments, dropping the trailing `diff` marker (e.g.
     * `['blog','post','hello','diff']` → `/blog/post/hello`). Mirrors the run
     * screen's URI reconstruction.
     */
    private readonly pageUri = toSignal(
        this.route.url.pipe(
            map((segments) => {
                const parts = segments.map((s) => s.path);
                if (parts[parts.length - 1] === 'diff') {
                    parts.pop();
                }

                return parts.length ? `/${parts.join('/')}` : null;
            })
        )
    );

    /** The live Monaco diff editor, disposed on destroy. */
    private editor: MonacoDiffEditor | null = null;
    /** True once the monaco global has loaded and the host is available. */
    private readonly monacoReady = signal(false);

    constructor() {
        // Rehydrate the selected page from the URL so a cold load / shared link
        // lands here with the page in context (no-op when already selected). The
        // lookup is host-scoped, so wait for the site to resolve.
        effect(() => {
            const uri = this.pageUri();
            const siteId = this.globalStore.currentSiteId();
            if (uri && siteId) {
                untracked(() => this.store.openPageByUri(uri));
            }
        });

        // A deep link to a page that no longer resolves → back to the picker.
        effect(() => {
            if (this.store.rehydrateStatus() === 'not-found') {
                untracked(() => this.toPicker());
            }
        });

        // Load the diff once the selected page is known.
        effect(() => {
            const page = this.store.selected();
            if (page) {
                untracked(() => this.loadDiff(page.path, page.hostId, page.languageId));
            }
        });

        // Wait for the AMD-loaded monaco global before creating the editor.
        this.monacoLoader.isMonacoLoaded$
            .pipe(
                filter((loaded) => loaded),
                take(1),
                takeUntilDestroyed()
            )
            .subscribe(() => this.monacoReady.set(true));

        // (Re)build the editor's model whenever the ready flag, the selection, or
        // the file set changes. afterNextRender guarantees the host <div> exists.
        effect(() => {
            const ready = this.monacoReady();
            const file = this.selected();
            untracked(() => {
                if (ready && file) {
                    this.renderDiff(file);
                }
            });
        });

        afterNextRender(() => {
            // If monaco was already loaded before the view rendered, kick a render.
            if (this.monacoReady() && this.selected()) {
                this.renderDiff(this.selected() as PageDiffFile);
            }
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
                    // Keep the current selection if it's still present, else pick the first.
                    const keep = files.some((f) => f.identifier === this.selectedId());
                    this.selectedId.set(keep ? this.selectedId() : (files[0]?.identifier ?? null));
                    this.status.set('loaded');
                },
                error: () => this.status.set('error')
            });
    }

    /** Select a file to show in the diff editor. */
    selectFile(identifier: string): void {
        this.selectedId.set(identifier);
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

    /** Back to the run screen for this page (drops the trailing `/diff` segment). */
    backToRun(): void {
        const uri = this.pageUri();
        if (uri) {
            this.router.navigate(['/agents/a11y', ...uri.split('/').filter(Boolean)]);
        } else {
            this.toPicker();
        }
    }

    private toPicker(): void {
        this.router.navigate(['/agents/a11y']);
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
