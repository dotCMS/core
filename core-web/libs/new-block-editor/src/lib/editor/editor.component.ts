import { TiptapEditorDirective } from 'ngx-tiptap';

import { DOCUMENT } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    OnDestroy,
    computed,
    effect,
    forwardRef,
    inject,
    input,
    output,
    signal
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { Editor, type JSONContent } from '@tiptap/core';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { EmojiPickerComponent } from './components/emoji-menu/emoji-picker.component';
import { ImageDialogComponent } from './components/image/image-dialog.component';
import { LinkDialogComponent } from './components/link/link-dialog.component';
import { TableDialogComponent } from './components/table/table-dialog.component';
import { VideoDialogComponent } from './components/video/video-dialog.component';
import { syncCharacterStatsFromEditor } from './editor-character-stats';
import { handleEditorProseMirrorClick } from './editor-chrome-click';
import { handleMediaDrop } from './editor.utils';
import { createEditorExtensions } from './extensions/editor-extensions';
import { SELECTION_PRESERVE_KEY } from './extensions/selection-preserve.extension';
import { DotCmsUploadService } from './services/dot-cms-upload.service';
import { EditorDialogManagerService } from './services/editor-dialog-manager.service';
import { SlashMenuComponent } from './slash-menu/slash-menu.component';
import { SlashMenuService } from './slash-menu/slash-menu.service';
import { EditorStore } from './store/editor.store';
import { ToolbarComponent } from './toolbar/toolbar.component';

import type { ContentletEditEvent } from './extensions/nodes/contentlet.extension';

/** Stringifies the editor document for form output (plain ProseMirror JSON, no extra attrs). */
function editorDocumentJsonText(editor: Editor): string {
    return JSON.stringify(editor.getJSON());
}

/** True when {@link parsed} represents the same document already in {@link editor}. */
function editorContentMatchesParsed(editor: Editor, parsed: string | JSONContent): boolean {
    const currentJson = editorDocumentJsonText(editor);
    if (typeof parsed === 'string') {
        const trimmed = parsed.trimStart();
        if (trimmed.startsWith('{')) {
            try {
                return JSON.stringify(JSON.parse(parsed)) === currentJson;
            } catch {
                return false;
            }
        }
        return parsed === editor.getHTML();
    }
    return JSON.stringify(parsed) === currentJson;
}

/**
 * Normalizes incoming editor content to either an HTML string or a JSONContent object.
 * dotCMS stores block editor fields as ProseMirror JSON (object or stringified).
 * TipTap's setContent accepts both formats natively.
 */
function normalizeEditorContent(
    content: string | JSONContent | null | undefined
): string | JSONContent {
    if (!content) return '';
    if (typeof content !== 'string') return content;
    const trimmed = content.trimStart();
    if (trimmed.startsWith('{')) {
        try {
            return JSON.parse(content) as JSONContent;
        } catch {
            // fall through to HTML
        }
    }
    return content;
}

/**
 * DotCMS block editor shell: TipTap surface, toolbar, slash menu, floating dialogs
 * (table, image, video, link, emoji), media drag-and-drop, optional fullscreen overlay,
 * live document stats, and Angular {@link ControlValueAccessor} for two-way JSON-text binding
 * ({@link Editor.getJSON} stringified, same storage shape as the legacy block editor minus doc count attrs).
 *
 * Registers {@link EditorStore} and {@link SlashMenuService} at component scope so each
 * editor instance has isolated menu and shared UI state.
 */
@Component({
    selector: 'dot-block-editor',
    changeDetection: ChangeDetectionStrategy.OnPush,
    styleUrls: ['./editor.component.css'],
    providers: [
        EditorStore,
        SlashMenuService,
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotCMSEditorComponent),
            multi: true
        }
    ],
    imports: [
        TiptapEditorDirective,
        SlashMenuComponent,
        EmojiPickerComponent,
        TableDialogComponent,
        ImageDialogComponent,
        VideoDialogComponent,
        LinkDialogComponent,
        ToolbarComponent
    ],
    template: `
        <div [class]="wrapperClass()">
            <div [class]="panelClass()">
                <dot-toolbar
                    [editor]="editor"
                    [isFullscreen]="isFullscreen()"
                    (fullscreenToggle)="toggleFullscreen()"
                    (contentletEdit)="contentletEdit.emit($event)" />
                <div
                    class="overflow-y-auto overscroll-contain"
                    [style]="
                        isFullscreen()
                            ? 'flex: 1; min-height: 0;'
                            : 'height: 500px; resize: vertical; min-height: 200px;'
                    ">
                    <div
                        tiptap
                        [editor]="editor"
                        class="prose max-w-none"
                        role="textbox"
                        aria-multiline="true"
                        aria-label="Rich text editor"
                        aria-haspopup="listbox"
                        aria-controls="slash-command-menu"
                        [attr.aria-expanded]="menuService.isOpen()"
                        [attr.aria-activedescendant]="menuService.activeOptionId()"
                        (click)="onClick($event)"></div>
                </div>

                <div
                    class="flex items-center gap-4 border-t border-gray-100 px-8 py-3 text-sm text-gray-500"
                    aria-live="polite"
                    aria-label="Document statistics">
                    <span>{{ wordCount() }} {{ wordCount() === 1 ? 'word' : 'words' }}</span>
                    <span>
                        {{ charCount() }} {{ charCount() === 1 ? 'character' : 'characters' }}
                    </span>
                    <span>{{ readingTime() }} min read</span>
                </div>

                <dot-slash-menu />
                <dot-emoji-picker [editor]="editor" />
                <dot-table-dialog [editor]="editor" />
                <dot-image-dialog [editor]="editor" />
                <dot-video-dialog [editor]="editor" />
                <dot-link-dialog [editor]="editor" />
            </div>
        </div>
    `
})
export class DotCMSEditorComponent implements OnDestroy, ControlValueAccessor {
    /** Slash menu state; used by the template for ARIA on the ProseMirror surface. */
    protected readonly menuService = inject(SlashMenuService);

    /** Field-scoped UI state (e.g. allowed blocks, language for API calls). */
    protected readonly store = inject(EditorStore);

    /** Opens/closes floating dialogs and supplies payloads (e.g. link edit context). */
    private readonly dialogManager = inject(EditorDialogManagerService);

    /** Uploads user-dropped image and video files to dotCMS. */
    private readonly dotCmsUpload = inject(DotCmsUploadService);

    /** Document root for fullscreen scroll lock and global key listeners. */
    private readonly document = inject(DOCUMENT);

    /**
     * TipTap node names or block identifiers allowed in this field (slash menu, toolbar).
     * When omitted, {@link createEditorExtensions} uses its default set.
     */
    readonly allowedBlocks = input<string[]>();

    /**
     * Initial editor content: JSON text (ProseMirror / TipTap doc) or HTML.
     * Required for Web Component usage where Angular's ControlValueAccessor is not available
     * and content must be set via attribute or property binding from outside Angular.
     * Inside Angular, prefer binding through ngModel or a reactive form control instead.
     */
    readonly value = input<string>('');

    /**
     * DotCMS content type field definition.
     * Drives editor configuration via fieldVariables: allowed blocks, allowed content types,
     * character limit, custom styles, count bar visibility, and custom remote extensions.
     */
    readonly field = input<DotCMSContentTypeField | undefined>(undefined);

    /**
     * The DotCMS contentlet currently being edited.
     * Used to resolve the active language ID and contentlet identifier
     * passed to the editor config extension for API-scoped queries.
     * When provided, its languageId takes precedence over the `languageId` input.
     */
    readonly contentlet = input<DotCMSContentlet | undefined>(undefined);

    /**
     * Language ID used for dotCMS API queries (content type search, contentlet insertion).
     * Falls back to this value when no `contentlet` input is provided.
     * Defaults to language 1 (English).
     */
    readonly languageId = input<number>(1);

    /**
     * When true, applies error styling to the editor wrapper.
     * Set by the parent when the field's form control has validation errors
     * originating outside the editor (e.g. required, custom validators).
     */
    readonly hasError = input<boolean>(false);

    /**
     * Emits stringified ProseMirror JSON on every editor change (same value as the CVA writes).
     * Intended for non-Angular consumers and Web Component hosts that cannot use
     * ControlValueAccessor. Angular form consumers should use ngModel or formControl instead.
     */
    readonly valueChange = output<string>();

    /**
     * Emits when the user chooses to edit an embedded DotCMS contentlet from the document.
     */
    readonly contentletEdit = output<ContentletEditEvent>();

    /** Current word count shown in the footer stats bar. */
    readonly wordCount = signal(0);

    /** Current character count shown in the footer stats bar. */
    readonly charCount = signal(0);

    /** Estimated whole-minute reading time for the footer stats bar. */
    readonly readingTime = signal(0);

    /** Signals updated by {@link syncCharacterStatsFromEditor} on create and update. */
    private readonly stats = {
        wordCount: this.wordCount,
        charCount: this.charCount,
        readingTime: this.readingTime
    };

    /**
     * Shared TipTap {@link Editor} for the host template and all child editor components.
     * Configured with dotCMS extensions, drop handling, and stats sync.
     */
    readonly editor: Editor = new Editor({
        onCreate: ({ editor }) => syncCharacterStatsFromEditor(editor, this.stats),
        onUpdate: ({ editor }) => {
            syncCharacterStatsFromEditor(editor, this.stats);
            const jsonText = editorDocumentJsonText(editor);
            this.onChange(jsonText);
            this.valueChange.emit(jsonText);
        },
        onBlur: () => {
            this.onTouched();
        },
        editorProps: {
            handleDrop: (view, event, slice, moved) =>
                handleMediaDrop(
                    this.editor,
                    view,
                    event as DragEvent,
                    slice,
                    moved,
                    (file) => this.dotCmsUpload.uploadImage(file),
                    (file) => this.dotCmsUpload.uploadVideo(file)
                )
        },
        extensions: createEditorExtensions(this.menuService, this.allowedBlocks()),
        content: ''
    });

    /** When true, renders the editor in a modal-style fullscreen overlay. */
    readonly isFullscreen = signal(false);

    /**
     * True while any managed dialog or the slash menu is open; drives selection-preservation
     * meta so the user still sees what range they were editing.
     */
    private readonly anyDialogOpen = computed(
        () => this.dialogManager.activeDialog() !== null || this.menuService.isOpen()
    );

    /** Toggles {@link isFullscreen} from the toolbar control. */
    protected toggleFullscreen(): void {
        this.isFullscreen.update((v) => !v);
    }

    /** Backdrop/layout classes for the outer wrapper (fullscreen dimmer vs inline). */
    protected readonly wrapperClass = computed(() =>
        this.isFullscreen()
            ? 'fixed inset-0 z-[9998] flex items-center justify-center bg-black/50'
            : ''
    );

    /** Inner panel sizing and chrome classes (fullscreen vs default card layout). */
    protected readonly panelClass = computed(() =>
        this.isFullscreen()
            ? 'relative flex flex-col w-[90vw] h-[90vh] rounded-lg border border-gray-200 bg-white overflow-hidden'
            : 'relative rounded-lg border border-gray-200'
    );

    /**
     * Subscribes inputs to {@link EditorStore} and the TipTap document, applies selection
     * preservation while overlays are open, and locks document scroll + Escape-to-exit while
     * {@link isFullscreen} is active.
     */
    constructor() {
        // Sync allowedBlocks input → store
        effect(() => {
            this.store.setAllowedBlocks(this.allowedBlocks() ?? []);
        });

        // Sync languageId input → store (contentlet.languageId takes precedence)
        effect(() => {
            const id = this.contentlet()?.languageId ?? this.languageId();
            this.store.setLanguageId(id);
        });

        // Sync value input → editor (for web component / non-CVA usage).
        // Guard: skip when value is empty to avoid overriding CVA-set content on init;
        // skip when unchanged so two-way [value] + (valueChange) does not reset the cursor.
        effect(() => {
            const v = this.value();
            if (!v) return;
            const parsed = normalizeEditorContent(v);
            if (editorContentMatchesParsed(this.editor, parsed)) return;
            this.editor.commands.setContent(parsed, { emitUpdate: false });
        });

        // Preserve selection highlight while any dialog is open
        effect(() => {
            const open = this.anyDialogOpen();
            this.editor.view.dispatch(
                this.editor.state.tr.setMeta(SELECTION_PRESERVE_KEY, { active: open })
            );
        });

        // Fullscreen: body scroll lock + Escape closes overlay when no dialog/menu is open
        effect((onCleanup) => {
            if (!this.isFullscreen()) return;
            this.document.body.style.overflow = 'hidden';

            const onKey = (e: KeyboardEvent) => {
                if (e.key !== 'Escape') return;
                const anyDialogOpen =
                    this.dialogManager.activeDialog() !== null || this.menuService.isOpen();
                if (!anyDialogOpen) this.isFullscreen.set(false);
            };

            this.document.addEventListener('keydown', onKey);
            onCleanup(() => {
                this.document.removeEventListener('keydown', onKey);
                this.document.body.style.overflow = '';
            });
        });
    }

    /**
     * Delegates ProseMirror clicks to shared chrome logic (e.g. opening dialogs for nodes).
     *
     * @param event - Native click from the TipTap host element.
     */
    onClick(event: MouseEvent): void {
        handleEditorProseMirrorClick(event, this.editor, this.dialogManager);
    }

    /** Restores body scroll and destroys the TipTap instance. */
    ngOnDestroy(): void {
        this.document.body.style.overflow = '';
        this.editor.destroy();
    }

    /** Bound in {@link registerOnChange}; forwards stringified {@link Editor.getJSON} to the form control. */
    private onChange: (value: string) => void = (_value: string) => {
        // Implementation provided by registerOnChange
    };

    /** Bound in {@link registerOnTouched}; marks the control touched on editor blur. */
    private onTouched: () => void = () => {
        // Implementation provided by registerOnTouched
    };

    /** @inheritdoc */
    writeValue(content: string | null): void {
        const parsed = normalizeEditorContent(content);
        if (editorContentMatchesParsed(this.editor, parsed)) return;
        this.editor.commands.setContent(parsed, { emitUpdate: false });
    }

    /** @inheritdoc */
    registerOnChange(fn: (value: string) => void): void {
        this.onChange = fn;
    }

    /** @inheritdoc */
    registerOnTouched(fn: () => void): void {
        this.onTouched = fn;
    }

    /** @inheritdoc */
    setDisabledState(isDisabled: boolean): void {
        this.editor.setEditable(!isDisabled);
    }
}
