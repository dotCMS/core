import { TiptapEditorDirective } from 'ngx-tiptap';

import { ChangeDetectionStrategy, Component, OnDestroy, inject, signal } from '@angular/core';

import { Editor } from '@tiptap/core';

import { ImageDialogComponent } from './components/image/image-dialog.component';
import { ImageDialogService } from './components/image/image-dialog.service';
import { LinkDialogComponent } from './components/link/link-dialog.component';
import { LinkDialogService } from './components/link/link-dialog.service';
import { TableDialogComponent } from './components/table/table-dialog.component';
import { VideoDialogComponent } from './components/video/video-dialog.component';
import { syncCharacterStatsFromEditor } from './editor-character-stats';
import { handleEditorProseMirrorClick } from './editor-chrome-click';
import { EDITOR_DEMO_CONTENT } from './editor-demo-content';
import { handleMediaDrop } from './editor.utils';
import { EmojiPickerComponent } from './emoji-menu/emoji-picker.component';
import { createEditorExtensions } from './extensions/editor-extensions';
import { DotCmsUploadService } from './services/dot-cms-upload.service';
import { SlashMenuComponent } from './slash-menu/slash-menu.component';
import { SlashMenuService } from './slash-menu/slash-menu.service';
import { ToolbarComponent } from './toolbar/toolbar.component';

@Component({
    selector: 'dot-block-editor',
    changeDetection: ChangeDetectionStrategy.OnPush,
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
        <div class="relative mx-auto mt-8 max-w-3xl rounded-lg border border-gray-200">
            <dot-block-editor-toolbar [editor]="editor" />
            <div>
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
                <span>{{ charCount() }} {{ charCount() === 1 ? 'character' : 'characters' }}</span>
                <span>{{ readingTime() }} min read</span>
            </div>

            <dot-block-editor-slash-menu />
            <dot-block-editor-emoji-picker />
            <dot-block-editor-table-dialog />
            <dot-block-editor-image-dialog />
            <dot-block-editor-video-dialog />
            <dot-block-editor-link-dialog />
        </div>
    `,
    styles: `
        :host ::ng-deep .ProseMirror {
            outline: none;
            min-height: 550px;
        }
    `
})
export class EditorComponent implements OnDestroy {
    protected readonly menuService = inject(SlashMenuService);
    private readonly linkDialogService = inject(LinkDialogService);
    private readonly imageDialogService = inject(ImageDialogService);
    private readonly dotCmsUpload = inject(DotCmsUploadService);

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
        onUpdate: ({ editor }) => syncCharacterStatsFromEditor(editor, this.stats),
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
        extensions: createEditorExtensions(this.menuService),
        content: EDITOR_DEMO_CONTENT
    });

    onClick(event: MouseEvent): void {
        handleEditorProseMirrorClick(
            event,
            this.editor,
            this.imageDialogService,
            this.linkDialogService
        );
    }

    ngOnDestroy(): void {
        this.editor.destroy();
    }
}
