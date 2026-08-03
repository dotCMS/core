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
    output,
    signal,
    untracked,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ButtonModule } from 'primeng/button';
import { DrawerModule } from 'primeng/drawer';
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
 * The "working vs live" file diff **side panel** — a slide-over that overlays the
 * run screen's preview area so the user can inspect what the agent changed
 * WITHOUT navigating away (the scan/run UI state stays fully intact underneath).
 *
 * Lists the page's source files that DIFFER between the working (unpublished) and
 * live (published) versions beside a read-only Monaco side-by-side diff of the
 * selected file.
 *
 * It's a presentational child of {@link DotA11yRunComponent}: the page context
 * comes from the shared {@link AccessibilityStudioStore} (already hydrated by the
 * run screen), so there's no routing/rehydration here. Visibility is driven by
 * the {@link open} input; {@link close} asks the host to hide it.
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
    imports: [ButtonModule, DrawerModule, TooltipModule, DotMessagePipe],
    templateUrl: './a11y-diff.component.html',
    providers: [DotPageSourcesService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotA11yDiffComponent {
    readonly store = inject(AccessibilityStudioStore);

    private readonly sourcesService = inject(DotPageSourcesService);
    private readonly monacoLoader = inject(MonacoEditorLoaderService);
    private readonly destroyRef = inject(DestroyRef);

    /** Whether the panel should be open. Drives the drawer + the (lazy) data load. */
    readonly open = input<boolean>(false);
    /** Emitted when the drawer is dismissed (X button / backdrop / Esc). */
    readonly close = output<void>();

    /**
     * Local drawer visibility, two-way bound to `p-drawer`'s `[(visible)]`. Kept in
     * sync with the `open` input via an effect so the parent controls it, while the
     * drawer's own dismiss paths (X / backdrop / Esc) can still flip it — those fire
     * `onHide`, which emits {@link close} so the parent updates its own flag.
     */
    readonly visible = signal(false);

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

    /** The live Monaco diff editor, disposed on destroy. */
    private editor: MonacoDiffEditor | null = null;
    /** True once the monaco global has loaded. */
    private readonly monacoReady = signal(false);
    /**
     * True while the drawer's content is actually mounted (between `onShow` and
     * `onHide`). The Monaco host lives inside the drawer's headless template, so it
     * only exists in this window — the render effect gates on it.
     */
    private readonly drawerShown = signal(false);
    /** The page identifier the current file list was loaded for (avoids reloads). */
    private loadedForIdentifier: string | null = null;

    constructor() {
        // Mirror the `open` input onto the drawer's local `visible` signal so the
        // parent opens/closes it; the drawer's own dismiss paths write `visible`
        // back and emit `close` (see onDrawerHide).
        effect(() => this.visible.set(this.open()));

        // Lazily (re)load the diff whenever the panel is opened for a page — the
        // run screen keeps this component mounted, so we key off open + the
        // selected page rather than a lifecycle hook. Reload only when the page
        // changed since the last load, so re-opening the panel is instant.
        effect(() => {
            const isOpen = this.open();
            const page = this.store.selected();
            if (!isOpen || !page) {
                return;
            }
            untracked(() => {
                if (this.loadedForIdentifier !== page.identifier) {
                    this.loadedForIdentifier = page.identifier;
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

        // (Re)build the editor's model when the selection changes WHILE the drawer
        // is already shown. The very first render (when the drawer opens) is driven
        // by the drawer's `onShow` — its content, incl. the #diffHost, only mounts
        // then. Gate on `drawerShown` so this doesn't fire before the host exists.
        effect(() => {
            const ready = this.monacoReady();
            const shown = this.drawerShown();
            const file = this.selected();
            untracked(() => {
                if (ready && shown && file) {
                    this.renderDiff(file);
                }
            });
        });

        this.destroyRef.onDestroy(() => this.disposeEditor());
    }

    /** Drawer finished opening — its content (incl. #diffHost) is now in the DOM. */
    onDrawerShow(): void {
        this.drawerShown.set(true);
        const file = this.selected();
        if (this.monacoReady() && file) {
            this.renderDiff(file);
        }
    }

    /** Drawer closed (X / backdrop / Esc): tear the editor down + tell the host. */
    onDrawerHide(): void {
        this.drawerShown.set(false);
        this.disposeEditor();
        this.close.emit();
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

    /** X button — dismiss the drawer; the drawer's `onHide` then emits `close`. */
    requestClose(): void {
        this.visible.set(false);
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
