import { MonacoEditorLoaderService } from '@materia-ui/ngx-monaco-editor';

import {
    ChangeDetectionStrategy,
    Component,
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

import { filter, take } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { PageDiffFile } from '../models/page-render-sources.models';

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
 * Read-only side-by-side diff (live → working) for one source file, filling the run
 * screen's right pane in place of the preview.
 *
 * Purely presentational: the file comes in via {@link file} and the close action goes
 * back out via {@link closed} — the run screen owns which of preview/diff is showing,
 * and the changed-files accordion in the left panel owns the selection.
 *
 * The Monaco diff editor is created imperatively against the `monaco` global (the app
 * registers `MonacoEditorModule` in app.config, so the AMD loader is configured);
 * `@materia-ui/ngx-monaco-editor` only exposes the plain editor, so the diff editor
 * has no Angular wrapper.
 */
@Component({
    selector: 'dot-a11y-diff-viewer',
    standalone: true,
    imports: [ButtonModule, DotMessagePipe],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex min-h-0 flex-col bg-surface-100' },
    templateUrl: './a11y-diff-viewer.component.html'
})
export class DotA11yDiffViewerComponent {
    private readonly monacoLoader = inject(MonacoEditorLoaderService);
    private readonly destroyRef = inject(DestroyRef);

    /** The file to diff. */
    readonly file = input<PageDiffFile | null>(null);

    /** The user asked to go back to the preview. */
    readonly closed = output<void>();

    private readonly diffHost = viewChild<ElementRef<HTMLDivElement>>('diffHost');

    /** True once the monaco global has loaded. */
    private readonly monacoReady = signal(false);

    private editor: MonacoDiffEditor | null = null;

    constructor() {
        // Wait for the AMD-loaded monaco global before creating the editor.
        this.monacoLoader.isMonacoLoaded$
            .pipe(
                filter((loaded) => loaded),
                take(1),
                takeUntilDestroyed()
            )
            .subscribe(() => this.monacoReady.set(true));

        // (Re)build the models whenever monaco is ready, a file is set, and the host
        // element exists. Depending on the `diffHost` viewChild signal matters: this
        // component is mounted inside an @if, so on the first pass the effect runs
        // before the view renders and the host is still undefined — reading it here
        // makes the effect re-run once it appears, instead of silently bailing out.
        effect(() => {
            const ready = this.monacoReady();
            const file = this.file();
            const host = this.diffHost()?.nativeElement;
            untracked(() => {
                if (ready && file && host) {
                    this.renderDiff(host, file);
                }
            });
        });

        this.destroyRef.onDestroy(() => this.disposeEditor());
    }

    /** Monaco language id for a file, from its extension. */
    private languageFor(extension: string): string {
        return LANGUAGE_BY_EXTENSION[extension.toLowerCase()] ?? 'plaintext';
    }

    /**
     * Create (once) and populate the diff editor with the file's live (original) vs
     * working (modified) text. Read-only side-by-side.
     */
    private renderDiff(host: HTMLElement, file: PageDiffFile): void {
        const monaco = getMonaco();
        if (!monaco) {
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
