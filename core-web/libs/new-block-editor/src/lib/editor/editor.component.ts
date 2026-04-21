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

import { Editor } from '@tiptap/core';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { ImageDialogComponent } from './components/image/image-dialog.component';
import { ImageDialogService } from './components/image/image-dialog.service';
import { LinkDialogComponent } from './components/link/link-dialog.component';
import { LinkDialogService } from './components/link/link-dialog.service';
import { TableDialogComponent } from './components/table/table-dialog.component';
import { TableDialogService } from './components/table/table-dialog.service';
import { VideoDialogComponent } from './components/video/video-dialog.component';
import { VideoDialogService } from './components/video/video-dialog.service';
import { syncCharacterStatsFromEditor } from './editor-character-stats';
import { handleEditorProseMirrorClick } from './editor-chrome-click';
import { handleMediaDrop } from './editor.utils';
import { EmojiPickerComponent } from './emoji-menu/emoji-picker.component';
import { createEditorExtensions } from './extensions/editor-extensions';
import { DotCmsUploadService } from './services/dot-cms-upload.service';
import { SlashMenuComponent } from './slash-menu/slash-menu.component';
import { SlashMenuService } from './slash-menu/slash-menu.service';
import { EditorStore } from './store/editor.store';
import { ToolbarComponent } from './toolbar/toolbar.component';

@Component({
    selector: 'dot-block-editor',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        EditorStore,
        SlashMenuService,
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => EditorComponent),
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
                <dot-block-editor-toolbar
                    [editor]="editor"
                    [isFullscreen]="isFullscreen()"
                    (fullscreenToggle)="toggleFullscreen()" />
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
                    class="flex items-center gap-4 border-t border-gray-100 px-8 py-2 text-xs text-gray-400"
                    aria-live="polite"
                    aria-label="Document statistics">
                    <span>{{ wordCount() }} {{ wordCount() === 1 ? 'word' : 'words' }}</span>
                    <span>
                        {{ charCount() }} {{ charCount() === 1 ? 'character' : 'characters' }}
                    </span>
                    <span>{{ readingTime() }} min read</span>
                </div>

                <dot-block-editor-slash-menu />
                <dot-block-editor-emoji-picker />
                <dot-block-editor-table-dialog />
                <dot-block-editor-image-dialog />
                <dot-block-editor-video-dialog />
                <dot-block-editor-link-dialog />
            </div>
        </div>
    `,
    styles: `
        :host ::ng-deep .ProseMirror {
            outline: none;
            min-height: 200px;
        }

        :host ::ng-deep .ProseMirror figure {
            display: block;
            margin: 0;
        }

        :host ::ng-deep .ProseMirror figure.image-wrap-left {
            float: left;
            width: 50%;
            margin: 0 1rem 1rem 0;
        }

        :host ::ng-deep .ProseMirror figure.image-wrap-right {
            float: right;
            width: 50%;
            margin: 0 0 1rem 1rem;
        }

        :host ::ng-deep .ProseMirror figure img {
            display: block;
            max-width: 100%;
            height: auto;
        }

        /* Selected node ring */
        :host ::ng-deep .ProseMirror figure.is-selected img,
        :host ::ng-deep .ProseMirror video.is-selected,
        :host ::ng-deep .ProseMirror [data-type='dot-contentlet'].is-selected {
            outline: 2px solid #6366f1;
            outline-offset: 2px;
            border-radius: 2px;
        }

        /* Grid block — fr-based columns, position:relative for resize handle overlay */
        :host ::ng-deep .ProseMirror .grid-block {
            display: grid;
            gap: 1rem;
            margin: 1rem 0;
            position: relative;
        }
        /* display:contents lets gridColumn cells participate in the parent CSS Grid */
        :host ::ng-deep .ProseMirror .grid-block__grid {
            display: contents;
        }
        :host ::ng-deep .ProseMirror .grid-block__column {
            min-width: 0;
        }
        :host ::ng-deep .ProseMirror .grid-block__column-content {
            padding: 0.5rem;
            border: 1px dashed #d1d5db;
            border-radius: 0.375rem;
            min-height: 3rem;
        }
        :host ::ng-deep .ProseMirror .grid-block__column-content:focus-within {
            border-color: #a5b4fc;
            background: color-mix(in srgb, #6366f1 8%, transparent);
        }

        /* Resize handle */
        :host ::ng-deep .grid-block__resize-handle {
            position: absolute;
            width: 0.75rem;
            cursor: col-resize;
            z-index: 10;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        :host ::ng-deep .grid-block__resize-handle::after {
            content: '';
            width: 1px;
            height: 2rem;
            background: #9ca3af;
            border-radius: 9999px;
            transition:
                background 0.15s,
                height 0.15s;
        }
        :host ::ng-deep .grid-block__resize-handle:hover::after {
            background: #6366f1;
            height: 100%;
        }
        :host ::ng-deep .grid-block__resize-handle--active::after {
            background: #6366f1;
            height: 100%;
            transition: none;
        }

        /* Drag preview overlay */
        :host ::ng-deep .grid-block__drag-preview {
            position: absolute;
            display: flex;
            z-index: 5;
            pointer-events: none;
            border-radius: 0.5rem;
        }
        :host ::ng-deep .grid-block__drag-preview-col {
            border-radius: 0.5rem;
            border: 2px dashed #818cf8;
            background: color-mix(in srgb, #6366f1 8%, transparent);
        }
    `
})
export class EditorComponent implements OnDestroy, ControlValueAccessor {
    protected readonly menuService = inject(SlashMenuService);
    protected readonly store = inject(EditorStore);
    private readonly linkDialogService = inject(LinkDialogService);
    private readonly imageDialogService = inject(ImageDialogService);
    private readonly videoDialogService = inject(VideoDialogService);
    private readonly tableDialogService = inject(TableDialogService);
    private readonly dotCmsUpload = inject(DotCmsUploadService);
    private readonly document = inject(DOCUMENT);

    readonly allowedBlocks = input<string[]>();

    /**
     * Initial HTML content for the editor.
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
     * Emits the updated HTML content on every editor change.
     * Intended for non-Angular consumers and Web Component hosts that cannot use
     * ControlValueAccessor. Angular form consumers should use ngModel or formControl instead.
     */
    readonly valueChange = output<string>();

    readonly wordCount = signal(0);
    readonly charCount = signal(0);
    readonly readingTime = signal(0);

    private readonly stats = {
        wordCount: this.wordCount,
        charCount: this.charCount,
        readingTime: this.readingTime
    };

    readonly editor: Editor = new Editor({
        onCreate: ({ editor }) => syncCharacterStatsFromEditor(editor, this.stats),
        onUpdate: ({ editor }) => {
            syncCharacterStatsFromEditor(editor, this.stats);
            this.onChange(editor.getHTML());
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

    // ── Fullscreen (F3) ──────────────────────────────────────────────────────

    readonly isFullscreen = signal(false);

    protected toggleFullscreen(): void {
        this.isFullscreen.update((v) => !v);
    }

    protected readonly wrapperClass = computed(() =>
        this.isFullscreen()
            ? 'fixed inset-0 z-[9998] flex items-center justify-center bg-black/50'
            : ''
    );

    protected readonly panelClass = computed(() =>
        this.isFullscreen()
            ? 'relative flex flex-col w-[90vw] h-[90vh] rounded-lg border border-gray-200 bg-white overflow-hidden'
            : 'relative mx-auto mt-8 max-w-3xl rounded-lg border border-gray-200'
    );

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

        // F3: Escape key + scroll lock for fullscreen
        effect((onCleanup) => {
            if (!this.isFullscreen()) return;
            this.document.body.style.overflow = 'hidden';

            const onKey = (e: KeyboardEvent) => {
                if (e.key !== 'Escape') return;
                const anyDialogOpen =
                    this.imageDialogService.isOpen() ||
                    this.linkDialogService.isOpen() ||
                    this.videoDialogService.isOpen() ||
                    this.tableDialogService.isOpen() ||
                    this.menuService.isOpen();
                if (!anyDialogOpen) this.isFullscreen.set(false);
            };

            this.document.addEventListener('keydown', onKey);
            onCleanup(() => {
                this.document.removeEventListener('keydown', onKey);
                this.document.body.style.overflow = '';
            });
        });
    }

    onClick(event: MouseEvent): void {
        handleEditorProseMirrorClick(
            event,
            this.editor,
            this.imageDialogService,
            this.linkDialogService
        );
    }

    ngOnDestroy(): void {
        this.document.body.style.overflow = '';
        this.editor.destroy();
    }

    private onChange: (value: string) => void = (_value: string) => {
        // Implementation provided by registerOnChange
    };
    private onTouched: () => void = () => {
        // Implementation provided by registerOnTouched
    };

    writeValue(content: string | null): void {
        const html = content ?? '';
        if (html !== this.editor.getHTML()) {
            this.editor.commands.setContent(html, false);
       
        }
    }

    registerOnChange(fn: (value: string) => void): void {
        this.onChange = fn;
    }

    registerOnTouched(fn: () => void): void {
        this.onTouched = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        this.editor.setEditable(!isDisabled);
    }
}
