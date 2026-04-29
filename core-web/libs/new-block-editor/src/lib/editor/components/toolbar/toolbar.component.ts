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
import { FormsModule } from '@angular/forms';

import type { TooltipOptions } from 'primeng/api';
import { Select } from 'primeng/select';
import { Tooltip } from 'primeng/tooltip';

import { Editor } from '@tiptap/core';
import { DOMSerializer } from '@tiptap/pm/model';

import { EditorToolbarStateService } from './editor-toolbar-state.service';
import { htmlToMarkdown, markdownToHtml } from './markdown.utils';

import { BLOCK_TARGET_KEY } from '../../extensions/selection-preserve.extension';
import { EditorDialogManagerService } from '../../services/editor-dialog-manager.service';
import { EditorStore } from '../../store/editor.store';

import type { ContentletEditEvent } from '../../extensions/nodes/contentlet/contentlet.extension';

@Component({
    selector: 'dot-toolbar',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [FormsModule, Select, Tooltip],
    host: {
        role: 'toolbar',
        'aria-label': 'Text formatting',
        'aria-orientation': 'horizontal',
        class: 'flex flex-wrap items-center gap-0.5 border-b border-gray-200 bg-gray-50 px-2 py-2 rounded-t-lg',
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
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
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
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
            [class]="btnClass(false)"
            (click)="redo()">
            <span aria-hidden="true" class="material-symbols-outlined">redo</span>
        </button>

        <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-gray-200"></span>

        <!-- Group 2: Block type -->
        <label for="toolbar-block-type" class="sr-only">Block type</label>
        <p-select
            inputId="toolbar-block-type"
            appendTo="body"
            [options]="blockTypeOptions()"
            [ngModel]="blockTypeValue()"
            (onChange)="setBlockType($event.value)"
            (onShow)="setBlockTargetActive(true)"
            (onHide)="setBlockTargetActive(false)"
            [pt]="selectPt" />

        <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-gray-200"></span>

        <!-- Group 3: Inline marks -->
        <button
            type="button"
            [attr.aria-pressed]="state.isBold()"
            aria-label="Bold"
            pTooltip="Bold"
            tooltipPosition="bottom"
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
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
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
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
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
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
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
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
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
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
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
            [class]="btnClass(state.isSubscript())"
            (click)="toggleSubscript()">
            <span aria-hidden="true" class="material-symbols-outlined">subscript</span>
        </button>

        <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-gray-200"></span>

        <!-- Group: Text alignment -->
        <button
            type="button"
            [attr.aria-pressed]="effectiveAlign() === 'left'"
            aria-label="Align left"
            pTooltip="Align left"
            tooltipPosition="bottom"
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
            [class]="btnClass(effectiveAlign() === 'left')"
            (click)="setTextAlign('left')">
            <span aria-hidden="true" class="material-symbols-outlined">format_align_left</span>
        </button>
        <button
            type="button"
            [attr.aria-pressed]="effectiveAlign() === 'center'"
            aria-label="Align center"
            pTooltip="Align center"
            tooltipPosition="bottom"
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
            [class]="btnClass(effectiveAlign() === 'center')"
            (click)="setTextAlign('center')">
            <span aria-hidden="true" class="material-symbols-outlined">format_align_center</span>
        </button>
        <button
            type="button"
            [attr.aria-pressed]="effectiveAlign() === 'right'"
            aria-label="Align right"
            pTooltip="Align right"
            tooltipPosition="bottom"
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
            [class]="btnClass(effectiveAlign() === 'right')"
            (click)="setTextAlign('right')">
            <span aria-hidden="true" class="material-symbols-outlined">format_align_right</span>
        </button>
        <button
            type="button"
            [disabled]="state.isImageSelected()"
            [attr.aria-disabled]="state.isImageSelected()"
            [attr.aria-pressed]="effectiveAlign() === 'justify'"
            aria-label="Justify"
            pTooltip="Justify"
            tooltipPosition="bottom"
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
            [class]="btnClass(effectiveAlign() === 'justify')"
            (click)="setTextAlign('justify')">
            <span aria-hidden="true" class="material-symbols-outlined">format_align_justify</span>
        </button>

        <button
            type="button"
            aria-label="Wrap text left"
            pTooltip="Wrap text left"
            tooltipPosition="bottom"
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
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
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
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
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
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
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
            data-testid="toolbar-edit-contentlet"
            [disabled]="!state.selectedContentlet()"
            [attr.aria-disabled]="!state.selectedContentlet()"
            [class]="btnClass(false)"
            (mousedown)="$event.preventDefault(); editContentlet()">
            <span aria-hidden="true" class="material-symbols-outlined">edit</span>
        </button>

        @if (showBlockFormatsGroup()) {
            <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-gray-200"></span>

            <!-- Group 4: Block formats -->
            @if (isAllowed('bulletList')) {
                <button
                    type="button"
                    [attr.aria-pressed]="state.isBulletList()"
                    aria-label="Bullet list"
                    pTooltip="Bullet list"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
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
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
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
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
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
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [class]="btnClass(state.isCodeBlock())"
                    (click)="toggleCodeBlock()">
                    <span aria-hidden="true" class="material-symbols-outlined">code_blocks</span>
                </button>
            }
        }

        <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-gray-200"></span>

        <!-- Group 5: Indent / Outdent / Clear format -->
        <button
            type="button"
            [disabled]="!state.canOutdent()"
            [attr.aria-disabled]="!state.canOutdent()"
            aria-label="Outdent"
            pTooltip="Outdent"
            tooltipPosition="bottom"
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
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
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
            [class]="btnClass(false)"
            (click)="indent()">
            <span aria-hidden="true" class="material-symbols-outlined">format_indent_increase</span>
        </button>
        <button
            type="button"
            aria-label="Clear formatting"
            pTooltip="Clear formatting"
            tooltipPosition="bottom"
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
            [class]="btnClass(false)"
            (click)="clearFormat()">
            <span aria-hidden="true" class="material-symbols-outlined">format_clear</span>
        </button>

        <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-gray-200"></span>

        <!-- Group 7: Horizontal rule -->
        @if (isAllowed('horizontalRule')) {
            <button
                type="button"
                aria-label="Horizontal rule"
                pTooltip="Horizontal rule"
                tooltipPosition="bottom"
                [tooltipOptions]="overlayTooltipOptions()"
                showDelay="350"
                [class]="btnClass(false)"
                (click)="insertHR()">
                <span aria-hidden="true" class="material-symbols-outlined">horizontal_rule</span>
            </button>
        }

        @if (showInsertGroup()) {
            <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-gray-200"></span>

            <!-- Group 8: Insert dialogs -->
            @if (isAllowed('link')) {
                <button
                    type="button"
                    aria-label="Insert link"
                    pTooltip="Insert link"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
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
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
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
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
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
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [class]="btnClass(false)"
                    (mousedown)="openTableDialog($event)">
                    <span aria-hidden="true" class="material-symbols-outlined">table</span>
                </button>
            }

            @if (isAllowed('table')) {
                <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-gray-200"></span>

                <!-- Insert row / column -->
                <button
                    type="button"
                    aria-label="Insert row above"
                    pTooltip="Insert row above"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [disabled]="!state.isInTable()"
                    [attr.aria-disabled]="!state.isInTable()"
                    [class]="btnClass(false)"
                    (click)="tableInsertRowAbove()">
                    <span aria-hidden="true" class="material-symbols-outlined">arrow_upward</span>
                </button>
                <button
                    type="button"
                    aria-label="Insert row below"
                    pTooltip="Insert row below"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [disabled]="!state.isInTable()"
                    [attr.aria-disabled]="!state.isInTable()"
                    [class]="btnClass(false)"
                    (click)="tableInsertRowBelow()">
                    <span aria-hidden="true" class="material-symbols-outlined">arrow_downward</span>
                </button>
                <button
                    type="button"
                    aria-label="Insert column left"
                    pTooltip="Insert column left"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [disabled]="!state.isInTable()"
                    [attr.aria-disabled]="!state.isInTable()"
                    [class]="btnClass(false)"
                    (click)="tableInsertColLeft()">
                    <span aria-hidden="true" class="material-symbols-outlined">arrow_back</span>
                </button>
                <button
                    type="button"
                    aria-label="Insert column right"
                    pTooltip="Insert column right"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [disabled]="!state.isInTable()"
                    [attr.aria-disabled]="!state.isInTable()"
                    [class]="btnClass(false)"
                    (click)="tableInsertColRight()">
                    <span aria-hidden="true" class="material-symbols-outlined">arrow_forward</span>
                </button>

                <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-gray-200"></span>

                <!-- Merge / split -->
                <button
                    type="button"
                    aria-label="Merge cells"
                    pTooltip="Merge cells"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [disabled]="!state.isInTable() || !state.canMergeCells()"
                    [attr.aria-disabled]="!state.isInTable() || !state.canMergeCells()"
                    [class]="btnClass(false)"
                    (click)="tableMerge()">
                    <span aria-hidden="true" class="material-symbols-outlined">call_merge</span>
                </button>
                <button
                    type="button"
                    aria-label="Split cell"
                    pTooltip="Split cell"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [disabled]="!state.isInTable() || !state.canSplitCell()"
                    [attr.aria-disabled]="!state.isInTable() || !state.canSplitCell()"
                    [class]="btnClass(false)"
                    (click)="tableSplit()">
                    <span aria-hidden="true" class="material-symbols-outlined">call_split</span>
                </button>

                <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-gray-200"></span>

                <!-- Header toggles -->
                <button
                    type="button"
                    aria-label="Toggle row header"
                    pTooltip="Toggle row header"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [disabled]="!state.isInTable()"
                    [attr.aria-disabled]="!state.isInTable()"
                    [class]="btnClass(false)"
                    (click)="tableToggleRowHeader()">
                    <span aria-hidden="true" class="material-symbols-outlined">table_rows</span>
                </button>
                <button
                    type="button"
                    aria-label="Toggle column header"
                    pTooltip="Toggle column header"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [disabled]="!state.isInTable()"
                    [attr.aria-disabled]="!state.isInTable()"
                    [class]="btnClass(false)"
                    (click)="tableToggleColHeader()">
                    <span aria-hidden="true" class="material-symbols-outlined">view_column</span>
                </button>

                <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-gray-200"></span>

                <!-- Delete -->
                <button
                    type="button"
                    aria-label="Delete row"
                    pTooltip="Delete row"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [disabled]="!state.isInTable()"
                    [attr.aria-disabled]="!state.isInTable()"
                    [class]="btnClass(false)"
                    (click)="tableDeleteRow()">
                    <span aria-hidden="true" class="material-symbols-outlined">delete_sweep</span>
                </button>
                <button
                    type="button"
                    aria-label="Delete column"
                    pTooltip="Delete column"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [disabled]="!state.isInTable()"
                    [attr.aria-disabled]="!state.isInTable()"
                    [class]="btnClass(false)"
                    (click)="tableDeleteCol()">
                    <span aria-hidden="true" class="material-symbols-outlined">delete_outline</span>
                </button>
                <button
                    type="button"
                    aria-label="Delete table"
                    pTooltip="Delete table"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [disabled]="!state.isInTable()"
                    [attr.aria-disabled]="!state.isInTable()"
                    [class]="btnClass(false)"
                    (click)="tableDeleteTable()">
                    <span aria-hidden="true" class="material-symbols-outlined">delete</span>
                </button>
            }
            @if (isAllowed('emoji')) {
                <button
                    type="button"
                    aria-label="Insert emoji"
                    pTooltip="Insert emoji"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [class]="btnClass(false)"
                    (mousedown)="openEmojiPicker($event)">
                    <span aria-hidden="true" class="material-symbols-outlined">emoji_emotions</span>
                </button>
            }
        }

        <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-gray-200"></span>

        <!-- Markdown copy / paste -->
        <button
            type="button"
            aria-label="Copy as Markdown"
            pTooltip="Copy as Markdown"
            tooltipPosition="bottom"
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
            data-testid="toolbar-copy-markdown"
            [class]="btnClass(false)"
            (click)="copyAsMarkdown()">
            <span aria-hidden="true" class="material-symbols-outlined">markdown_copy</span>
        </button>
        <button
            type="button"
            aria-label="Paste from Markdown"
            pTooltip="Paste from Markdown"
            tooltipPosition="bottom"
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
            data-testid="toolbar-paste-markdown"
            [class]="btnClass(false)"
            (click)="pasteFromMarkdown()">
            <span aria-hidden="true" class="material-symbols-outlined">markdown_paste</span>
        </button>

        <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-gray-200"></span>
        <button
            type="button"
            [attr.aria-pressed]="isFullscreen()"
            [attr.aria-label]="isFullscreen() ? 'Exit full screen' : 'Full screen'"
            [pTooltip]="isFullscreen() ? 'Exit full screen' : 'Full screen'"
            tooltipPosition="bottom"
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
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
    private readonly dialogManager = inject(EditorDialogManagerService);

    readonly editor = input.required<Editor>();
    readonly isFullscreen = input<boolean>(false);

    /**
     * Fullscreen editor shell uses `z-[9998]` on its backdrop; PrimeNG tooltips append to `document.body`
     * with a much lower default z-index, so they render under the overlay. Bump only while fullscreen.
     */
    protected readonly overlayTooltipOptions = computed(
        (): TooltipOptions =>
            this.isFullscreen() ? { tooltipZIndex: '10050' } : { tooltipZIndex: 'auto' }
    );

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
            'flex h-9 w-9 cursor-pointer items-center justify-center rounded text-sm transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:ring-offset-1 disabled:opacity-40 disabled:cursor-not-allowed';
        return active
            ? `${base} bg-indigo-100 text-indigo-700`
            : `${base} text-gray-600 hover:bg-gray-100 hover:text-gray-900`;
    }

    protected readonly blockTypeValue = computed(() => {
        const level = this.state.headingLevel();
        return level === null ? 'paragraph' : `h${level}`;
    });

    /**
     * Flattens p-select to fit the toolbar's icon-button aesthetic.
     * Uses PrimeNG design tokens (--p-select-*) instead of !important — tokens
     * win against unlayered component CSS without specificity hacks.
     */
    protected readonly selectPt = {
        root: {
            class: 'h-9 rounded hover:bg-gray-100',
            style: {
                '--p-select-border-color': 'transparent',
                '--p-select-hover-border-color': 'transparent',
                '--p-select-focus-border-color': 'transparent',
                '--p-select-background': 'transparent',
                '--p-select-hover-background': 'transparent',
                '--p-select-shadow': 'none'
            }
        },
        label: { class: 'flex items-center text-sm text-gray-700 leading-none' },
        dropdown: { class: 'flex items-center text-gray-500' }
    };

    protected readonly blockTypeOptions = computed(() => {
        const opts: { label: string; value: string }[] = [
            { label: 'Paragraph', value: 'paragraph' }
        ];
        if (this.store.isAllowed('heading')) {
            opts.push(
                { label: 'Heading 1', value: 'h1' },
                { label: 'Heading 2', value: 'h2' },
                { label: 'Heading 3', value: 'h3' }
            );
        }
        return opts;
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

    // When an image is selected, the alignment buttons reflect the image's textAlign
    // (defaulting to 'left' when unset, matching paragraph behavior). Otherwise they
    // reflect the standard text-align state from the TextAlign extension.
    protected readonly effectiveAlign = computed(() =>
        this.state.isImageSelected()
            ? (this.state.imageTextAlign() ?? 'left')
            : this.state.textAlign()
    );

    // ── History ──────────────────────────────────────────────────────────────

    protected undo(): void {
        this.editor().chain().focus().undo().run();
    }

    protected redo(): void {
        this.editor().chain().focus().redo().run();
    }

    // ── Block type ───────────────────────────────────────────────────────────

    protected setBlockType(value: string): void {
        const editor = this.editor();
        if (value === 'paragraph') {
            editor.chain().focus().setParagraph().run();
        } else {
            const level = Number(value.replace('h', '')) as 1 | 2 | 3;
            editor.chain().focus().setHeading({ level }).run();
        }
    }

    /** Highlights the cursor's block while the block-type select is open. */
    protected setBlockTargetActive(active: boolean): void {
        const editor = this.editor();
        editor.view.dispatch(editor.state.tr.setMeta(BLOCK_TARGET_KEY, { active }));
    }

    // ── Markdown copy / paste ────────────────────────────────────────────────

    /** Copies the selection (or whole doc if no selection) as Markdown. */
    protected async copyAsMarkdown(): Promise<void> {
        const editor = this.editor();
        const html = this.getSelectedHtmlOrAll(editor);
        if (!html) return;
        try {
            await navigator.clipboard.writeText(htmlToMarkdown(html));
        } catch (err) {
            console.warn('Copy as Markdown failed', err);
        } finally {
            editor.view.focus();
        }
    }

    /** Reads Markdown from the clipboard and inserts it as rich content at the cursor. */
    protected async pasteFromMarkdown(): Promise<void> {
        const editor = this.editor();
        try {
            const text = await navigator.clipboard.readText();
            if (!text) return;
            editor.chain().focus().insertContent(markdownToHtml(text)).run();
        } catch (err) {
            console.warn('Paste from Markdown failed', err);
        }
    }

    /** Returns the selection's HTML, or the entire document's HTML when the selection is empty. */
    private getSelectedHtmlOrAll(editor: Editor): string {
        const { from, to, empty } = editor.state.selection;
        if (empty) return editor.getHTML();
        const slice = editor.state.doc.cut(from, to);
        const fragment = DOMSerializer.fromSchema(editor.schema).serializeFragment(slice.content);
        const div = document.createElement('div');
        div.appendChild(fragment);
        return div.innerHTML;
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

        if (this.dialogManager.isOpen('link')) {
            this.dialogManager.close();
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

            this.dialogManager.openLink(() => linkEl.getBoundingClientRect(), {
                initialValues: { href, displayText, target: linkMark.attrs['target'] ?? null },
                linkEl,
                anchorPos
            });
        } else {
            // Insert mode — anchor to the toolbar button
            const selectedText = empty ? '' : editor.state.doc.textBetween(from, to);
            this.dialogManager.openLink(
                () => btn.getBoundingClientRect(),
                selectedText
                    ? { initialValues: { href: '', displayText: selectedText } }
                    : undefined
            );
        }
    }

    protected openImageDialog(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        if (this.dialogManager.isOpen('image')) {
            this.dialogManager.close();
            return;
        }
        const btn = event.currentTarget as HTMLElement;
        this.dialogManager.openImage(() => btn.getBoundingClientRect());
    }

    protected openVideoDialog(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        if (this.dialogManager.isOpen('video')) {
            this.dialogManager.close();
            return;
        }
        const btn = event.currentTarget as HTMLElement;
        this.dialogManager.open('video', () => btn.getBoundingClientRect());
    }

    protected openTableDialog(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        if (this.dialogManager.isOpen('table')) {
            this.dialogManager.close();
            return;
        }
        const btn = event.currentTarget as HTMLElement;
        this.dialogManager.open('table', () => btn.getBoundingClientRect());
    }

    protected openEmojiPicker(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        const btn = event.currentTarget as HTMLElement;
        this.dialogManager.toggle('emoji', () => btn.getBoundingClientRect());
    }

    // ── Text alignment ───────────────────────────────────────────────────────

    protected setTextAlign(align: 'left' | 'center' | 'right' | 'justify'): void {
        const editor = this.editor();
        if (this.state.isImageSelected()) {
            // Justify isn't meaningful for an image; mirror the old node's behavior
            if (align === 'justify') return;
            editor.chain().focus().setImageTextAlign(align).run();
            return;
        }
        editor.chain().focus().setTextAlign(align).run();
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

    // ── Edit image properties ────────────────────────────────────────────────

    protected openImagePropertiesDialog(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        const editor = this.editor();
        if (!editor) return;

        const { from } = editor.state.selection;
        const node = editor.state.doc.nodeAt(from);
        if (!node || node.type.name !== 'dotImage') return;

        const btn = event.currentTarget as HTMLElement;
        this.dialogManager.openImage(() => btn.getBoundingClientRect(), {
            initialValues: {
                src: node.attrs['src'],
                title: node.attrs['title'] ?? '',
                alt: node.attrs['alt'] ?? ''
            }
        });
    }

    // ── Table actions ────────────────────────────────────────────────────────

    protected tableInsertRowAbove(): void {
        this.editor().chain().focus().addRowBefore().run();
    }

    protected tableInsertRowBelow(): void {
        this.editor().chain().focus().addRowAfter().run();
    }

    protected tableInsertColLeft(): void {
        this.editor().chain().focus().addColumnBefore().run();
    }

    protected tableInsertColRight(): void {
        this.editor().chain().focus().addColumnAfter().run();
    }

    protected tableMerge(): void {
        this.editor().chain().focus().mergeCells().run();
    }

    protected tableSplit(): void {
        this.editor().chain().focus().splitCell().run();
    }

    protected tableToggleRowHeader(): void {
        this.editor().chain().focus().toggleHeaderRow().run();
    }

    protected tableToggleColHeader(): void {
        this.editor().chain().focus().toggleHeaderColumn().run();
    }

    protected tableDeleteRow(): void {
        this.editor().chain().focus().deleteRow().run();
    }

    protected tableDeleteCol(): void {
        this.editor().chain().focus().deleteColumn().run();
    }

    protected tableDeleteTable(): void {
        this.editor().chain().focus().deleteTable().run();
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
