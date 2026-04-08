import {
    ChangeDetectionStrategy,
    Component,
    OnDestroy,
    computed,
    effect,
    inject,
    input
} from '@angular/core';

import { Editor } from '@tiptap/core';

import { EditorToolbarStateService } from './editor-toolbar-state.service';

import { ImageDialogService } from '../blocks/image/image-dialog.service';
import { LinkDialogService } from '../blocks/link/link-dialog.service';
import { TableDialogService } from '../blocks/table/table-dialog.service';
import { VideoDialogService } from '../blocks/video/video-dialog.service';
import { EmojiPickerService } from '../emoji-menu/emoji-picker.service';

@Component({
    selector: 'dot-block-editor-toolbar',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        role: 'toolbar',
        'aria-label': 'Text formatting',
        'aria-orientation': 'horizontal',
        class: 'flex flex-wrap items-center gap-0.5 border-b border-gray-200 bg-gray-50 px-2 py-1.5 rounded-t-lg',
        '(keydown)': 'onToolbarKeyDown($event)'
    },
    template: `
        <!-- Group 1: History -->
        <button
            type="button"
            [disabled]="!state.canUndo()"
            [attr.aria-disabled]="!state.canUndo()"
            aria-label="Undo"
            [class]="btnClass(false)"
            (click)="undo()">
            <span aria-hidden="true" class="material-symbols-outlined">undo</span>
        </button>
        <button
            type="button"
            [disabled]="!state.canRedo()"
            [attr.aria-disabled]="!state.canRedo()"
            aria-label="Redo"
            [class]="btnClass(false)"
            (click)="redo()">
            <span aria-hidden="true" class="material-symbols-outlined">redo</span>
        </button>

        <span aria-hidden="true" class="mx-1 h-5 w-px shrink-0 bg-gray-200"></span>

        <!-- Group 2: Block type -->
        <label for="toolbar-block-type" class="sr-only">Block type</label>
        <select
            id="toolbar-block-type"
            [value]="blockTypeValue()"
            (change)="setBlockType($event)"
            class="h-7 cursor-pointer rounded border-0 bg-transparent py-0 pl-1 pr-6 text-sm text-gray-700 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:ring-offset-1">
            <option value="paragraph">Paragraph</option>
            <option value="h1">Heading 1</option>
            <option value="h2">Heading 2</option>
            <option value="h3">Heading 3</option>
        </select>

        <span aria-hidden="true" class="mx-1 h-5 w-px shrink-0 bg-gray-200"></span>

        <!-- Group 3: Inline marks -->
        <button
            type="button"
            [attr.aria-pressed]="state.isBold()"
            aria-label="Bold"
            [class]="btnClass(state.isBold())"
            (click)="toggleBold()">
            <span aria-hidden="true" class="material-symbols-outlined">format_bold</span>
        </button>
        <button
            type="button"
            [attr.aria-pressed]="state.isItalic()"
            aria-label="Italic"
            [class]="btnClass(state.isItalic())"
            (click)="toggleItalic()">
            <span aria-hidden="true" class="material-symbols-outlined">format_italic</span>
        </button>
        <button
            type="button"
            [attr.aria-pressed]="state.isStrike()"
            aria-label="Strikethrough"
            [class]="btnClass(state.isStrike())"
            (click)="toggleStrike()">
            <span aria-hidden="true" class="material-symbols-outlined">format_strikethrough</span>
        </button>
        <button
            type="button"
            [attr.aria-pressed]="state.isCode()"
            aria-label="Inline code"
            [class]="btnClass(state.isCode())"
            (click)="toggleCode()">
            <span aria-hidden="true" class="material-symbols-outlined">code</span>
        </button>

        <span aria-hidden="true" class="mx-1 h-5 w-px shrink-0 bg-gray-200"></span>

        <!-- Group 4: Block formats -->
        <button
            type="button"
            [attr.aria-pressed]="state.isBulletList()"
            aria-label="Bullet list"
            [class]="btnClass(state.isBulletList())"
            (click)="toggleBulletList()">
            <span aria-hidden="true" class="material-symbols-outlined">format_list_bulleted</span>
        </button>
        <button
            type="button"
            [attr.aria-pressed]="state.isOrderedList()"
            aria-label="Ordered list"
            [class]="btnClass(state.isOrderedList())"
            (click)="toggleOrderedList()">
            <span aria-hidden="true" class="material-symbols-outlined">format_list_numbered</span>
        </button>
        <button
            type="button"
            [attr.aria-pressed]="state.isBlockquote()"
            aria-label="Blockquote"
            [class]="btnClass(state.isBlockquote())"
            (click)="toggleBlockquote()">
            <span aria-hidden="true" class="material-symbols-outlined">format_quote</span>
        </button>
        <button
            type="button"
            [attr.aria-pressed]="state.isCodeBlock()"
            aria-label="Code block"
            [class]="btnClass(state.isCodeBlock())"
            (click)="toggleCodeBlock()">
            <span aria-hidden="true" class="material-symbols-outlined">code_blocks</span>
        </button>

        <span aria-hidden="true" class="mx-1 h-5 w-px shrink-0 bg-gray-200"></span>

        <!-- Group 5: Indent / Outdent / Clear format -->
        <button
            type="button"
            [disabled]="!state.canOutdent()"
            [attr.aria-disabled]="!state.canOutdent()"
            aria-label="Outdent"
            [class]="btnClass(false)"
            (click)="outdent()">
            <span aria-hidden="true" class="material-symbols-outlined">format_indent_decrease</span>
        </button>
        <button
            type="button"
            [disabled]="!state.canIndent()"
            [attr.aria-disabled]="!state.canIndent()"
            aria-label="Indent"
            [class]="btnClass(false)"
            (click)="indent()">
            <span aria-hidden="true" class="material-symbols-outlined">format_indent_increase</span>
        </button>
        <button
            type="button"
            aria-label="Clear formatting"
            [class]="btnClass(false)"
            (click)="clearFormat()">
            <span aria-hidden="true" class="material-symbols-outlined">format_clear</span>
        </button>

        <span aria-hidden="true" class="mx-1 h-5 w-px shrink-0 bg-gray-200"></span>

        <!-- Group 7: Horizontal rule -->
        <button
            type="button"
            aria-label="Horizontal rule"
            [class]="btnClass(false)"
            (click)="insertHR()">
            <span aria-hidden="true" class="material-symbols-outlined">horizontal_rule</span>
        </button>

        <span aria-hidden="true" class="mx-1 h-5 w-px shrink-0 bg-gray-200"></span>

        <!-- Group 8: Insert dialogs -->
        <button
            type="button"
            aria-label="Insert link"
            [class]="btnClass(state.isLink())"
            (mousedown)="openLinkDialog($event)">
            <span aria-hidden="true" class="material-symbols-outlined">link</span>
        </button>
        <button
            type="button"
            aria-label="Insert image"
            [class]="btnClass(false)"
            (mousedown)="openImageDialog($event)">
            <span aria-hidden="true" class="material-symbols-outlined">image</span>
        </button>
        <button
            type="button"
            aria-label="Insert video"
            [class]="btnClass(false)"
            (mousedown)="openVideoDialog($event)">
            <span aria-hidden="true" class="material-symbols-outlined">videocam</span>
        </button>
        <button
            type="button"
            aria-label="Insert table"
            [class]="btnClass(false)"
            (mousedown)="openTableDialog($event)">
            <span aria-hidden="true" class="material-symbols-outlined">table</span>
        </button>
        <button
            type="button"
            aria-label="Insert emoji"
            [class]="btnClass(false)"
            (mousedown)="openEmojiPicker($event)">
            <span aria-hidden="true" class="material-symbols-outlined">emoji_emotions</span>
        </button>
    `
})
export class ToolbarComponent implements OnDestroy {
    protected readonly state = inject(EditorToolbarStateService);
    private readonly imageDialogService = inject(ImageDialogService);
    private readonly linkDialogService = inject(LinkDialogService);
    private readonly tableDialogService = inject(TableDialogService);
    private readonly videoDialogService = inject(VideoDialogService);
    private readonly emojiPickerService = inject(EmojiPickerService);

    readonly editor = input.required<Editor>();

    private cleanupFn: (() => void) | null = null;

    constructor() {
        effect(() => {
            this.cleanupFn?.();
            this.cleanupFn = this.state.connect(this.editor());
        });
    }

    ngOnDestroy(): void {
        this.cleanupFn?.();
    }

    protected btnClass(active: boolean): string {
        const base =
            'flex h-7 w-7 items-center justify-center rounded text-sm transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:ring-offset-1 disabled:opacity-40 disabled:cursor-not-allowed';
        return active
            ? `${base} bg-indigo-100 text-indigo-700`
            : `${base} text-gray-600 hover:bg-gray-100 hover:text-gray-900`;
    }

    protected readonly blockTypeValue = computed(() => {
        const level = this.state.headingLevel();
        return level === null ? 'paragraph' : `h${level}`;
    });

    // ── History ──────────────────────────────────────────────────────────────

    protected undo(): void {
        this.editor().chain().focus().undo().run();
    }

    protected redo(): void {
        this.editor().chain().focus().redo().run();
    }

    // ── Block type ───────────────────────────────────────────────────────────

    protected setBlockType(event: Event): void {
        const value = (event.target as HTMLSelectElement).value;
        const editor = this.editor();
        if (value === 'paragraph') {
            editor.chain().focus().setParagraph().run();
        } else {
            const level = Number(value.replace('h', '')) as 1 | 2 | 3;
            editor.chain().focus().setHeading({ level }).run();
        }
    }

    // ── Inline marks ─────────────────────────────────────────────────────────

    protected toggleBold(): void {
        this.editor().chain().focus().toggleBold().run();
    }

    protected toggleItalic(): void {
        this.editor().chain().focus().toggleItalic().run();
    }

    protected toggleStrike(): void {
        this.editor().chain().focus().toggleStrike().run();
    }

    protected toggleCode(): void {
        this.editor().chain().focus().toggleCode().run();
    }

    // ── Block formats ────────────────────────────────────────────────────────

    protected toggleBulletList(): void {
        this.editor().chain().focus().toggleBulletList().run();
    }

    protected toggleOrderedList(): void {
        this.editor().chain().focus().toggleOrderedList().run();
    }

    protected toggleBlockquote(): void {
        this.editor().chain().focus().toggleBlockquote().run();
    }

    protected toggleCodeBlock(): void {
        this.editor().chain().focus().toggleCodeBlock().run();
    }

    protected insertHR(): void {
        this.editor().chain().focus().setHorizontalRule().run();
    }

    protected indent(): void {
        this.editor().chain().focus().sinkListItem('listItem').run();
    }

    protected outdent(): void {
        this.editor().chain().focus().liftListItem('listItem').run();
    }

    protected clearFormat(): void {
        this.editor().chain().focus().unsetAllMarks().clearNodes().run();
    }

    // ── Dialog openers ────────────────────────────────────────────────────────

    protected openLinkDialog(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        if (this.linkDialogService.isOpen()) {
            this.linkDialogService.close();
            return;
        }
        const editor = this.editor();
        const { from, to, empty } = editor.state.selection;
        const btn = event.currentTarget as HTMLElement;

        // Check if cursor/selection is inside an existing link
        const linkMark = editor.state.doc
            .resolve(from)
            .marks()
            .find((m) => m.type.name === 'link');
        const linkEl = linkMark
            ? ((editor.view.domAtPos(from).node as HTMLElement).closest?.(
                  'a[href]'
              ) as HTMLElement | null)
            : null;

        if (linkMark && linkEl) {
            // Edit mode — anchor to the link element itself
            const href = linkMark.attrs['href'] ?? '';
            const displayText = linkEl.textContent?.trim() ?? '';
            const anchorPos = editor.view.posAtDOM(linkEl, 0);

            this.linkDialogService.open(
                (newHref, newDisplayText) => {
                    editor
                        .chain()
                        .focus()
                        .setTextSelection(anchorPos)
                        .extendMarkRange('link')
                        .insertContent({
                            type: 'text',
                            text: newDisplayText ?? newHref,
                            marks: [{ type: 'link', attrs: { href: newHref } }]
                        })
                        .run();
                },
                () => linkEl.getBoundingClientRect(),
                { href, displayText },
                linkEl
            );
        } else {
            // Insert mode — anchor to the toolbar button
            const selectedText = empty ? '' : editor.state.doc.textBetween(from, to);
            this.linkDialogService.open(
                (href, displayText) => {
                    editor
                        .chain()
                        .focus()
                        .insertContent({
                            type: 'text',
                            text: displayText ?? href,
                            marks: [{ type: 'link', attrs: { href } }]
                        })
                        .run();
                },
                () => btn.getBoundingClientRect(),
                selectedText ? { href: '', displayText: selectedText } : undefined
            );
        }
    }

    protected openImageDialog(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        if (this.imageDialogService.isOpen()) {
            this.imageDialogService.close();
            return;
        }
        const btn = event.currentTarget as HTMLElement;
        const editor = this.editor();
        this.imageDialogService.open(
            (src, title, alt) => {
                editor
                    .chain()
                    .focus()
                    .setImage({ src, title: title || undefined, alt: alt || undefined })
                    .run();
            },
            () => btn.getBoundingClientRect()
        );
    }

    protected openVideoDialog(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        if (this.videoDialogService.isOpen()) {
            this.videoDialogService.close();
            return;
        }
        const btn = event.currentTarget as HTMLElement;
        const editor = this.editor();
        this.videoDialogService.open(
            (src, title) => {
                editor
                    .chain()
                    .focus()
                    .insertContent({ type: 'video', attrs: { src, title: title ?? null } })
                    .run();
            },
            () => btn.getBoundingClientRect()
        );
    }

    protected openTableDialog(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        if (this.tableDialogService.isOpen()) {
            this.tableDialogService.close();
            return;
        }
        const btn = event.currentTarget as HTMLElement;
        const editor = this.editor();
        this.tableDialogService.open(
            (config) => {
                editor.chain().focus().insertTable(config).run();
            },
            () => btn.getBoundingClientRect()
        );
    }

    protected openEmojiPicker(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        if (this.emojiPickerService.isOpen()) {
            this.emojiPickerService.close();
            return;
        }
        const btn = event.currentTarget as HTMLElement;
        this.emojiPickerService.open(
            (emoji) => this.editor().chain().focus().insertContent(emoji).run(),
            () => btn.getBoundingClientRect()
        );
    }

    // ── Keyboard navigation (roving tabindex) ────────────────────────────────

    protected onToolbarKeyDown(event: KeyboardEvent): void {
        if (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft') return;
        const els = Array.from(
            (event.currentTarget as HTMLElement).querySelectorAll<HTMLElement>(
                'button:not([disabled]), select'
            )
        );
        const idx = els.indexOf(document.activeElement as HTMLElement);
        if (idx === -1) return;
        event.preventDefault();
        const next =
            event.key === 'ArrowRight'
                ? (idx + 1) % els.length
                : (idx - 1 + els.length) % els.length;
        els[next]?.focus();
    }
}
