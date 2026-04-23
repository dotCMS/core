import {
    ChangeDetectionStrategy,
    Component,
    OnDestroy,
    computed,
    effect,
    inject,
    input,
    output
} from '@angular/core';

import { Tooltip } from 'primeng/tooltip';

import { Editor } from '@tiptap/core';

import { EditorToolbarStateService } from './editor-toolbar-state.service';

import { ImageDialogService } from '../components/image/image-dialog.service';
import { LinkDialogService } from '../components/link/link-dialog.service';
import { TableDialogService } from '../components/table/table-dialog.service';
import { VideoDialogService } from '../components/video/video-dialog.service';
import { EmojiPickerService } from '../emoji-menu/emoji-picker.service';
import { DOT_IMAGE_NODE_NAME } from '../extensions/image.extension';
import {
    insertUploadPlaceholders,
    replacePlaceholder,
    removePlaceholder
} from '../extensions/upload-placeholder.extension';
import { DOT_VIDEO_NODE_NAME } from '../extensions/video.extension';
import { EditorStore } from '../store/editor.store';

import type { ContentletEditEvent } from '../extensions/contentlet.extension';

@Component({
    selector: 'dot-block-editor-toolbar',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [Tooltip],
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
            pTooltip="Undo"
            tooltipPosition="bottom"
            [class]="btnClass(false)"
            (click)="undo()">
            <span aria-hidden="true" class="material-symbols-outlined">undo</span>
        </button>
        <button
            type="button"
            [disabled]="!state.canRedo()"
            [attr.aria-disabled]="!state.canRedo()"
            aria-label="Redo"
            pTooltip="Redo"
            tooltipPosition="bottom"
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
            @if (isAllowed('heading')) {
                <option value="h1">Heading 1</option>
                <option value="h2">Heading 2</option>
                <option value="h3">Heading 3</option>
            }
        </select>

        <span aria-hidden="true" class="mx-1 h-5 w-px shrink-0 bg-gray-200"></span>

        <!-- Group 3: Inline marks -->
        <button
            type="button"
            [attr.aria-pressed]="state.isBold()"
            aria-label="Bold"
            pTooltip="Bold"
            tooltipPosition="bottom"
            [class]="btnClass(state.isBold())"
            (click)="toggleBold()">
            <span aria-hidden="true" class="material-symbols-outlined">format_bold</span>
        </button>
        <button
            type="button"
            [attr.aria-pressed]="state.isItalic()"
            aria-label="Italic"
            pTooltip="Italic"
            tooltipPosition="bottom"
            [class]="btnClass(state.isItalic())"
            (click)="toggleItalic()">
            <span aria-hidden="true" class="material-symbols-outlined">format_italic</span>
        </button>
        <button
            type="button"
            [attr.aria-pressed]="state.isStrike()"
            aria-label="Strikethrough"
            pTooltip="Strikethrough"
            tooltipPosition="bottom"
            [class]="btnClass(state.isStrike())"
            (click)="toggleStrike()">
            <span aria-hidden="true" class="material-symbols-outlined">format_strikethrough</span>
        </button>
        <button
            type="button"
            [attr.aria-pressed]="state.isCode()"
            aria-label="Inline code"
            pTooltip="Inline code"
            tooltipPosition="bottom"
            [class]="btnClass(state.isCode())"
            (click)="toggleCode()">
            <span aria-hidden="true" class="material-symbols-outlined">code</span>
        </button>
        <button
            type="button"
            [attr.aria-pressed]="state.isSuperscript()"
            aria-label="Superscript"
            pTooltip="Superscript"
            tooltipPosition="bottom"
            [class]="btnClass(state.isSuperscript())"
            (click)="toggleSuperscript()">
            <span aria-hidden="true" class="material-symbols-outlined">superscript</span>
        </button>
        <button
            type="button"
            [attr.aria-pressed]="state.isSubscript()"
            aria-label="Subscript"
            pTooltip="Subscript"
            tooltipPosition="bottom"
            [class]="btnClass(state.isSubscript())"
            (click)="toggleSubscript()">
            <span aria-hidden="true" class="material-symbols-outlined">subscript</span>
        </button>

        <span aria-hidden="true" class="mx-1 h-5 w-px shrink-0 bg-gray-200"></span>

        <!-- Group: Text alignment -->
        <button
            type="button"
            [attr.aria-pressed]="state.textAlign() === 'left'"
            aria-label="Align left"
            pTooltip="Align left"
            tooltipPosition="bottom"
            [class]="btnClass(state.textAlign() === 'left')"
            (click)="setTextAlign('left')">
            <span aria-hidden="true" class="material-symbols-outlined">format_align_left</span>
        </button>
        <button
            type="button"
            [attr.aria-pressed]="state.textAlign() === 'center'"
            aria-label="Align center"
            pTooltip="Align center"
            tooltipPosition="bottom"
            [class]="btnClass(state.textAlign() === 'center')"
            (click)="setTextAlign('center')">
            <span aria-hidden="true" class="material-symbols-outlined">format_align_center</span>
        </button>
        <button
            type="button"
            [attr.aria-pressed]="state.textAlign() === 'right'"
            aria-label="Align right"
            pTooltip="Align right"
            tooltipPosition="bottom"
            [class]="btnClass(state.textAlign() === 'right')"
            (click)="setTextAlign('right')">
            <span aria-hidden="true" class="material-symbols-outlined">format_align_right</span>
        </button>
        <button
            type="button"
            [attr.aria-pressed]="state.textAlign() === 'justify'"
            aria-label="Justify"
            pTooltip="Justify"
            tooltipPosition="bottom"
            [class]="btnClass(state.textAlign() === 'justify')"
            (click)="setTextAlign('justify')">
            <span aria-hidden="true" class="material-symbols-outlined">format_align_justify</span>
        </button>

        <button
            type="button"
            aria-label="Wrap text left"
            pTooltip="Wrap text left"
            tooltipPosition="bottom"
            data-testid="toolbar-wrap-left"
            [disabled]="!state.isImageSelected()"
            [attr.aria-disabled]="!state.isImageSelected()"
            [class]="btnClass(state.imageTextWrap() === 'left')"
            (mousedown)="$event.preventDefault(); setImageWrap('left')">
            <span aria-hidden="true" class="material-symbols-outlined">format_image_left</span>
        </button>
        <button
            type="button"
            aria-label="Wrap text right"
            pTooltip="Wrap text right"
            tooltipPosition="bottom"
            data-testid="toolbar-wrap-right"
            [disabled]="!state.isImageSelected()"
            [attr.aria-disabled]="!state.isImageSelected()"
            [class]="btnClass(state.imageTextWrap() === 'right')"
            (mousedown)="$event.preventDefault(); setImageWrap('right')">
            <span aria-hidden="true" class="material-symbols-outlined">format_image_right</span>
        </button>
        <button
            type="button"
            aria-label="Edit image properties"
            pTooltip="Edit image properties"
            tooltipPosition="bottom"
            data-testid="toolbar-edit-image"
            [disabled]="!state.isImageSelected()"
            [attr.aria-disabled]="!state.isImageSelected()"
            [class]="btnClass(false)"
            (mousedown)="openImagePropertiesDialog($event)">
            <span aria-hidden="true" class="material-symbols-outlined">tune</span>
        </button>
        <button
            type="button"
            aria-label="Edit contentlet"
            pTooltip="Edit contentlet"
            tooltipPosition="bottom"
            data-testid="toolbar-edit-contentlet"
            [disabled]="!state.selectedContentlet()"
            [attr.aria-disabled]="!state.selectedContentlet()"
            [class]="btnClass(false)"
            (mousedown)="$event.preventDefault(); editContentlet()">
            <span aria-hidden="true" class="material-symbols-outlined">edit</span>
        </button>

        @if (showBlockFormatsGroup()) {
            <span aria-hidden="true" class="mx-1 h-5 w-px shrink-0 bg-gray-200"></span>

            <!-- Group 4: Block formats -->
            @if (isAllowed('bulletList')) {
                <button
                    type="button"
                    [attr.aria-pressed]="state.isBulletList()"
                    aria-label="Bullet list"
                    pTooltip="Bullet list"
                    tooltipPosition="bottom"
                    [class]="btnClass(state.isBulletList())"
                    (click)="toggleBulletList()">
                    <span aria-hidden="true" class="material-symbols-outlined">
                        format_list_bulleted
                    </span>
                </button>
            }
            @if (isAllowed('orderedList')) {
                <button
                    type="button"
                    [attr.aria-pressed]="state.isOrderedList()"
                    aria-label="Ordered list"
                    pTooltip="Ordered list"
                    tooltipPosition="bottom"
                    [class]="btnClass(state.isOrderedList())"
                    (click)="toggleOrderedList()">
                    <span aria-hidden="true" class="material-symbols-outlined">
                        format_list_numbered
                    </span>
                </button>
            }
            @if (isAllowed('blockquote')) {
                <button
                    type="button"
                    [attr.aria-pressed]="state.isBlockquote()"
                    aria-label="Blockquote"
                    pTooltip="Blockquote"
                    tooltipPosition="bottom"
                    [class]="btnClass(state.isBlockquote())"
                    (click)="toggleBlockquote()">
                    <span aria-hidden="true" class="material-symbols-outlined">format_quote</span>
                </button>
            }
            @if (isAllowed('codeBlock')) {
                <button
                    type="button"
                    [attr.aria-pressed]="state.isCodeBlock()"
                    aria-label="Code block"
                    pTooltip="Code block"
                    tooltipPosition="bottom"
                    [class]="btnClass(state.isCodeBlock())"
                    (click)="toggleCodeBlock()">
                    <span aria-hidden="true" class="material-symbols-outlined">code_blocks</span>
                </button>
            }
        }

        <span aria-hidden="true" class="mx-1 h-5 w-px shrink-0 bg-gray-200"></span>

        <!-- Group 5: Indent / Outdent / Clear format -->
        <button
            type="button"
            [disabled]="!state.canOutdent()"
            [attr.aria-disabled]="!state.canOutdent()"
            aria-label="Outdent"
            pTooltip="Outdent"
            tooltipPosition="bottom"
            [class]="btnClass(false)"
            (click)="outdent()">
            <span aria-hidden="true" class="material-symbols-outlined">format_indent_decrease</span>
        </button>
        <button
            type="button"
            [disabled]="!state.canIndent()"
            [attr.aria-disabled]="!state.canIndent()"
            aria-label="Indent"
            pTooltip="Indent"
            tooltipPosition="bottom"
            [class]="btnClass(false)"
            (click)="indent()">
            <span aria-hidden="true" class="material-symbols-outlined">format_indent_increase</span>
        </button>
        <button
            type="button"
            aria-label="Clear formatting"
            pTooltip="Clear formatting"
            tooltipPosition="bottom"
            [class]="btnClass(false)"
            (click)="clearFormat()">
            <span aria-hidden="true" class="material-symbols-outlined">format_clear</span>
        </button>

        <span aria-hidden="true" class="mx-1 h-5 w-px shrink-0 bg-gray-200"></span>

        <!-- Group 7: Horizontal rule -->
        @if (isAllowed('horizontalRule')) {
            <button
                type="button"
                aria-label="Horizontal rule"
                pTooltip="Horizontal rule"
                tooltipPosition="bottom"
                [class]="btnClass(false)"
                (click)="insertHR()">
                <span aria-hidden="true" class="material-symbols-outlined">horizontal_rule</span>
            </button>
        }

        @if (showInsertGroup()) {
            <span aria-hidden="true" class="mx-1 h-5 w-px shrink-0 bg-gray-200"></span>

            <!-- Group 8: Insert dialogs -->
            @if (isAllowed('link')) {
                <button
                    type="button"
                    aria-label="Insert link"
                    pTooltip="Insert link"
                    tooltipPosition="bottom"
                    [class]="btnClass(state.isLink())"
                    (mousedown)="openLinkDialog($event)">
                    <span aria-hidden="true" class="material-symbols-outlined">link</span>
                </button>
            }
            @if (isAllowed('image')) {
                <button
                    type="button"
                    aria-label="Insert image"
                    pTooltip="Insert image"
                    tooltipPosition="bottom"
                    [class]="btnClass(false)"
                    (mousedown)="openImageDialog($event)">
                    <span aria-hidden="true" class="material-symbols-outlined">image</span>
                </button>
            }
            @if (isAllowed('video')) {
                <button
                    type="button"
                    aria-label="Insert video"
                    pTooltip="Insert video"
                    tooltipPosition="bottom"
                    [class]="btnClass(false)"
                    (mousedown)="openVideoDialog($event)">
                    <span aria-hidden="true" class="material-symbols-outlined">videocam</span>
                </button>
            }
            @if (isAllowed('table')) {
                <button
                    type="button"
                    aria-label="Insert table"
                    pTooltip="Insert table"
                    tooltipPosition="bottom"
                    [class]="btnClass(false)"
                    (mousedown)="openTableDialog($event)">
                    <span aria-hidden="true" class="material-symbols-outlined">table</span>
                </button>
            }
            @if (isAllowed('emoji')) {
                <button
                    type="button"
                    aria-label="Insert emoji"
                    pTooltip="Insert emoji"
                    tooltipPosition="bottom"
                    [class]="btnClass(false)"
                    (mousedown)="openEmojiPicker($event)">
                    <span aria-hidden="true" class="material-symbols-outlined">emoji_emotions</span>
                </button>
            }
        }

        <span aria-hidden="true" class="mx-1 h-5 w-px shrink-0 bg-gray-200"></span>
        <button
            type="button"
            [attr.aria-pressed]="isFullscreen()"
            [attr.aria-label]="isFullscreen() ? 'Exit full screen' : 'Full screen'"
            [pTooltip]="isFullscreen() ? 'Exit full screen' : 'Full screen'"
            tooltipPosition="bottom"
            [class]="btnClass(isFullscreen())"
            data-testid="toolbar-fullscreen"
            (click)="fullscreenToggle.emit()">
            <span aria-hidden="true" class="material-symbols-outlined">
                {{ isFullscreen() ? 'fullscreen_exit' : 'fullscreen' }}
            </span>
        </button>
    `
})
export class ToolbarComponent implements OnDestroy {
    protected readonly state = inject(EditorToolbarStateService);
    protected readonly store = inject(EditorStore);
    private readonly imageDialogService = inject(ImageDialogService);
    private readonly linkDialogService = inject(LinkDialogService);
    private readonly tableDialogService = inject(TableDialogService);
    private readonly videoDialogService = inject(VideoDialogService);
    private readonly emojiPickerService = inject(EmojiPickerService);

    readonly editor = input.required<Editor>();
    readonly isFullscreen = input<boolean>(false);
    readonly fullscreenToggle = output<void>();
    readonly contentletEdit = output<ContentletEditEvent>();

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
            'flex h-7 w-7 cursor-pointer items-center justify-center rounded text-sm transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:ring-offset-1 disabled:opacity-40 disabled:cursor-not-allowed';
        return active
            ? `${base} bg-indigo-100 text-indigo-700`
            : `${base} text-gray-600 hover:bg-gray-100 hover:text-gray-900`;
    }

    protected readonly blockTypeValue = computed(() => {
        const level = this.state.headingLevel();
        return level === null ? 'paragraph' : `h${level}`;
    });

    // ── allowedBlocks helpers ────────────────────────────────────────────────

    protected isAllowed(block: string): boolean {
        return this.store.isAllowed(block);
    }

    protected readonly showBlockFormatsGroup = computed(
        () =>
            this.store.isAllowed('bulletList') ||
            this.store.isAllowed('orderedList') ||
            this.store.isAllowed('blockquote') ||
            this.store.isAllowed('codeBlock')
    );

    protected readonly showInsertGroup = computed(
        () =>
            this.store.isAllowed('link') ||
            this.store.isAllowed('image') ||
            this.store.isAllowed('video') ||
            this.store.isAllowed('table') ||
            this.store.isAllowed('emoji')
    );

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

    // ── Close all dialogs helper (B5) ────────────────────────────────────────

    private closeAllDialogs(): void {
        this.imageDialogService.close();
        this.linkDialogService.close();
        this.videoDialogService.close();
        this.tableDialogService.close();
        this.emojiPickerService.close();
    }

    // ── Dialog openers ────────────────────────────────────────────────────────

    protected openLinkDialog(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        if (this.linkDialogService.isOpen()) {
            this.linkDialogService.close();
            return;
        }
        this.closeAllDialogs();
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
                (newHref, newDisplayText, openInNewTab) => {
                    editor
                        .chain()
                        .focus()
                        .setTextSelection(anchorPos)
                        .extendMarkRange('link')
                        .insertContent({
                            type: 'text',
                            text: newDisplayText ?? newHref,
                            marks: [
                                {
                                    type: 'link',
                                    attrs: { href: newHref, target: openInNewTab ? '_blank' : null }
                                }
                            ]
                        })
                        .run();
                },
                () => linkEl.getBoundingClientRect(),
                { href, displayText, target: linkMark.attrs['target'] ?? null },
                linkEl
            );
        } else {
            // Insert mode — anchor to the toolbar button
            const selectedText = empty ? '' : editor.state.doc.textBetween(from, to);
            this.linkDialogService.open(
                (href, displayText, openInNewTab) => {
                    editor
                        .chain()
                        .focus()
                        .insertContent({
                            type: 'text',
                            text: displayText ?? href,
                            marks: [
                                {
                                    type: 'link',
                                    attrs: { href, target: openInNewTab ? '_blank' : null }
                                }
                            ]
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
        this.closeAllDialogs();
        const btn = event.currentTarget as HTMLElement;
        const editor = this.editor();
        this.imageDialogService.open(
            (src, title, alt, data) => {
                editor
                    .chain()
                    .focus()
                    .insertContent({
                        type: DOT_IMAGE_NODE_NAME,
                        attrs: { src, title: title || null, alt: alt || null, data: data ?? null }
                    })
                    .run();
            },
            () => btn.getBoundingClientRect(),
            {
                uploadCallbacks: {
                    onStart: () => {
                        const pos = editor.state.selection.from;
                        const id = `img-upload-${Date.now()}`;
                        insertUploadPlaceholders(editor, pos, [{ id, mediaType: 'image' }]);
                        return id;
                    },
                    onFinish: (id, attrs) => {
                        replacePlaceholder(editor, id, {
                            type: DOT_IMAGE_NODE_NAME,
                            attrs: { ...attrs, title: null }
                        });
                    },
                    onCancel: (id) => {
                        removePlaceholder(editor, id);
                    }
                }
            }
        );
    }

    protected openVideoDialog(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        if (this.videoDialogService.isOpen()) {
            this.videoDialogService.close();
            return;
        }
        this.closeAllDialogs();
        const btn = event.currentTarget as HTMLElement;
        const editor = this.editor();
        this.videoDialogService.open(
            (src, title) => {
                editor
                    .chain()
                    .focus()
                    .insertContent({
                        type: DOT_VIDEO_NODE_NAME,
                        attrs: { src, title: title ?? null }
                    })
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
        this.closeAllDialogs();
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
        this.closeAllDialogs();
        const btn = event.currentTarget as HTMLElement;
        this.emojiPickerService.open(
            (emoji) => this.editor().chain().focus().insertContent(emoji).run(),
            () => btn.getBoundingClientRect()
        );
    }

    // ── Text alignment ───────────────────────────────────────────────────────

    protected setTextAlign(align: string): void {
        this.editor().chain().focus().setTextAlign(align).run();
    }

    // ── Superscript / Subscript ──────────────────────────────────────────────

    protected toggleSuperscript(): void {
        this.editor().chain().focus().unsetSubscript().toggleSuperscript().run();
    }

    protected toggleSubscript(): void {
        this.editor().chain().focus().unsetSuperscript().toggleSubscript().run();
    }

    // ── Edit contentlet ──────────────────────────────────────────────────────

    protected editContentlet(): void {
        const data = this.state.selectedContentlet();
        if (data) this.contentletEdit.emit(data);
    }

    // ── Image text wrap ──────────────────────────────────────────────────────

    protected setImageWrap(value: 'left' | 'right'): void {
        this.editor().chain().focus().setImageTextWrap(value).run();
    }

    // ── Edit image properties (F1) ───────────────────────────────────────────

    protected openImagePropertiesDialog(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        const editor = this.editor();
        if (!editor) return;

        const { from } = editor.state.selection;
        const node = editor.state.doc.nodeAt(from);
        if (!node || node.type.name !== 'dotImage') return;

        const btn = event.currentTarget as HTMLElement;
        this.closeAllDialogs();
        this.imageDialogService.open(
            (src, title, alt) => {
                editor
                    .chain()
                    .focus()
                    .updateAttributes('dotImage', {
                        src,
                        title: title || null,
                        alt: alt || null
                    })
                    .run();
            },
            () => btn.getBoundingClientRect(),
            {
                initialValues: {
                    src: node.attrs['src'],
                    title: node.attrs['title'] ?? '',
                    alt: node.attrs['alt'] ?? ''
                }
            }
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
