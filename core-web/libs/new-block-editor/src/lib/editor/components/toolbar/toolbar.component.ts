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

import { ConfirmationService, type TooltipOptions } from 'primeng/api';
import { Select } from 'primeng/select';
import { Tooltip } from 'primeng/tooltip';

import { Editor } from '@tiptap/core';
import { DOMSerializer } from '@tiptap/pm/model';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { EditorToolbarStateService } from './editor-toolbar-state.service';

import { BLOCK_TARGET_KEY } from '../../extensions/selection-preserve.extension';
import { ContentletEditUrlService } from '../../services/contentlet-edit-url.service';
import { EditorModalService } from '../../services/editor-modal.service';
import { EditorPopoverService } from '../../services/editor-popover.service';
import { EditorStore } from '../../store/editor.store';
import { writeRelationshipReturnBreadcrumb } from '../../utils/breadcrumb.utils';
import { htmlToMarkdown, markdownToHtml } from '../../utils/markdown.utils';

import type { ContentletEditEvent } from '../../extensions/nodes/contentlet/contentlet.extension';

@Component({
    selector: 'dot-toolbar',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [FormsModule, Select, Tooltip, DotMessagePipe],
    host: {
        role: 'toolbar',
        '[attr.aria-label]': 'toolbarAriaLabel',
        'aria-orientation': 'horizontal',
        class: 'flex flex-wrap items-center gap-0.5 p-1.5 bg-indigo-50 border border-b-0 border-indigo-100 rounded-t-lg',
        '(keydown)': 'onToolbarKeyDown($event)'
    },
    template: `
        <!-- Group 1: History -->
        <button
            type="button"
            [disabled]="!state.canUndo()"
            [attr.aria-disabled]="!state.canUndo()"
            [attr.aria-label]="'dot.block.editor.toolbar.undo' | dm"
            [pTooltip]="'dot.block.editor.toolbar.undo' | dm"
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
            [attr.aria-label]="'dot.block.editor.toolbar.redo' | dm"
            [pTooltip]="'dot.block.editor.toolbar.redo' | dm"
            tooltipPosition="bottom"
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
            [class]="btnClass(false)"
            (click)="redo()">
            <span aria-hidden="true" class="material-symbols-outlined">redo</span>
        </button>

        <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-indigo-200"></span>

        <!-- Group 2: Block type -->
        <label for="toolbar-block-type" class="sr-only">
            {{ 'dot.block.editor.toolbar.block-type' | dm }}
        </label>
        <p-select
            inputId="toolbar-block-type"
            appendTo="body"
            [size]="'small'"
            [options]="blockTypeOptions()"
            [ngModel]="blockTypeValue()"
            (onChange)="setBlockType($event.value)"
            (onShow)="setBlockTargetActive(true)"
            (onHide)="setBlockTargetActive(false)"
            [pt]="selectPt" />

        <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-indigo-200"></span>

        <!-- Group 3: Inline marks -->
        <button
            type="button"
            [attr.aria-pressed]="state.isBold()"
            [attr.aria-label]="'dot.block.editor.toolbar.bold' | dm"
            [pTooltip]="'dot.block.editor.toolbar.bold' | dm"
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
            [attr.aria-label]="'dot.block.editor.toolbar.italic' | dm"
            [pTooltip]="'dot.block.editor.toolbar.italic' | dm"
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
            [attr.aria-label]="'dot.block.editor.toolbar.strikethrough' | dm"
            [pTooltip]="'dot.block.editor.toolbar.strikethrough' | dm"
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
            [attr.aria-label]="'dot.block.editor.toolbar.inline-code' | dm"
            [pTooltip]="'dot.block.editor.toolbar.inline-code' | dm"
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
            [attr.aria-label]="'dot.block.editor.toolbar.superscript' | dm"
            [pTooltip]="'dot.block.editor.toolbar.superscript' | dm"
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
            [attr.aria-label]="'dot.block.editor.toolbar.subscript' | dm"
            [pTooltip]="'dot.block.editor.toolbar.subscript' | dm"
            tooltipPosition="bottom"
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
            [class]="btnClass(state.isSubscript())"
            (click)="toggleSubscript()">
            <span aria-hidden="true" class="material-symbols-outlined">subscript</span>
        </button>

        <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-indigo-200"></span>

        <!-- Group: Text alignment -->
        <button
            type="button"
            [attr.aria-pressed]="effectiveAlign() === 'left'"
            [attr.aria-label]="'dot.block.editor.toolbar.align-left' | dm"
            [pTooltip]="'dot.block.editor.toolbar.align-left' | dm"
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
            [attr.aria-label]="'dot.block.editor.toolbar.align-center' | dm"
            [pTooltip]="'dot.block.editor.toolbar.align-center' | dm"
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
            [attr.aria-label]="'dot.block.editor.toolbar.align-right' | dm"
            [pTooltip]="'dot.block.editor.toolbar.align-right' | dm"
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
            [attr.aria-label]="'dot.block.editor.toolbar.justify' | dm"
            [pTooltip]="'dot.block.editor.toolbar.justify' | dm"
            tooltipPosition="bottom"
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
            [class]="btnClass(effectiveAlign() === 'justify')"
            (click)="setTextAlign('justify')">
            <span aria-hidden="true" class="material-symbols-outlined">format_align_justify</span>
        </button>

        <button
            type="button"
            [attr.aria-label]="'dot.block.editor.toolbar.wrap-text-left' | dm"
            [pTooltip]="'dot.block.editor.toolbar.wrap-text-left' | dm"
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
            [attr.aria-label]="'dot.block.editor.toolbar.wrap-text-right' | dm"
            [pTooltip]="'dot.block.editor.toolbar.wrap-text-right' | dm"
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
            [attr.aria-label]="'dot.block.editor.toolbar.edit-image-properties' | dm"
            [pTooltip]="'dot.block.editor.toolbar.edit-image-properties' | dm"
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
            [attr.aria-label]="'block-editor.bubble-menu.contentlet.edit' | dm"
            [pTooltip]="'block-editor.bubble-menu.contentlet.edit' | dm"
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
            <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-indigo-200"></span>

            <!-- Group 4: Block formats -->
            @if (isAllowed('bulletList')) {
                <button
                    type="button"
                    [attr.aria-pressed]="state.isBulletList()"
                    [attr.aria-label]="'dot.block.editor.toolbar.bullet-list' | dm"
                    [pTooltip]="'dot.block.editor.toolbar.bullet-list' | dm"
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
                    [attr.aria-label]="'dot.block.editor.toolbar.ordered-list' | dm"
                    [pTooltip]="'dot.block.editor.toolbar.ordered-list' | dm"
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
                    [attr.aria-label]="'dot.block.editor.toolbar.blockquote' | dm"
                    [pTooltip]="'dot.block.editor.toolbar.blockquote' | dm"
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
                    [attr.aria-label]="'dot.block.editor.toolbar.code-block' | dm"
                    [pTooltip]="'dot.block.editor.toolbar.code-block' | dm"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [class]="btnClass(state.isCodeBlock())"
                    (click)="toggleCodeBlock()">
                    <span aria-hidden="true" class="material-symbols-outlined">code_blocks</span>
                </button>
            }
        }

        <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-indigo-200"></span>

        <!-- Group 5: Indent / Outdent / Clear format -->
        <button
            type="button"
            [disabled]="!state.canOutdent()"
            [attr.aria-disabled]="!state.canOutdent()"
            [attr.aria-label]="'dot.block.editor.toolbar.outdent' | dm"
            [pTooltip]="'dot.block.editor.toolbar.outdent' | dm"
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
            [attr.aria-label]="'dot.block.editor.toolbar.indent' | dm"
            [pTooltip]="'dot.block.editor.toolbar.indent' | dm"
            tooltipPosition="bottom"
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
            [class]="btnClass(false)"
            (click)="indent()">
            <span aria-hidden="true" class="material-symbols-outlined">format_indent_increase</span>
        </button>
        <button
            type="button"
            [attr.aria-label]="'dot.block.editor.toolbar.clear-formatting' | dm"
            [pTooltip]="'dot.block.editor.toolbar.clear-formatting' | dm"
            tooltipPosition="bottom"
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
            [class]="btnClass(false)"
            (click)="clearFormat()">
            <span aria-hidden="true" class="material-symbols-outlined">format_clear</span>
        </button>

        @if (isAllowed('horizontalRule') || showInsertGroup()) {
            <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-indigo-200"></span>
        }

        <!-- Group 7: Horizontal rule -->
        @if (isAllowed('horizontalRule')) {
            <button
                type="button"
                [attr.aria-label]="'dot.block.editor.toolbar.horizontal-rule' | dm"
                [pTooltip]="'dot.block.editor.toolbar.horizontal-rule' | dm"
                tooltipPosition="bottom"
                [tooltipOptions]="overlayTooltipOptions()"
                showDelay="350"
                [class]="btnClass(false)"
                (click)="insertHR()">
                <span aria-hidden="true" class="material-symbols-outlined">horizontal_rule</span>
            </button>
        }

        @if (showInsertGroup()) {
            <!-- Separate HR from the insert group only when HR rendered;
                 the divider above already covers the indent → insert transition. -->
            @if (isAllowed('horizontalRule')) {
                <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-indigo-200"></span>
            }

            <!-- Group 8: Insert dialogs -->
            @if (isAllowed('link')) {
                <button
                    type="button"
                    [attr.aria-label]="'dot.block.editor.toolbar.insert-link' | dm"
                    [pTooltip]="'dot.block.editor.toolbar.insert-link' | dm"
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
                    [attr.aria-label]="'dot.block.editor.toolbar.insert-image' | dm"
                    [pTooltip]="'dot.block.editor.toolbar.insert-image' | dm"
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
                    [attr.aria-label]="'dot.block.editor.toolbar.insert-video' | dm"
                    [pTooltip]="'dot.block.editor.toolbar.insert-video' | dm"
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
                    [attr.aria-label]="'dot.block.editor.toolbar.insert-table' | dm"
                    [pTooltip]="'dot.block.editor.toolbar.insert-table' | dm"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [class]="btnClass(false)"
                    (mousedown)="openTableDialog($event)">
                    <span aria-hidden="true" class="material-symbols-outlined">table</span>
                </button>
            }

            @if (isAllowed('table')) {
                <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-indigo-200"></span>

                <!-- Insert row / column -->
                <button
                    type="button"
                    [attr.aria-label]="'dot.block.editor.toolbar.table.insert-row-above' | dm"
                    [pTooltip]="'dot.block.editor.toolbar.table.insert-row-above' | dm"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [disabled]="!state.isInTable()"
                    [attr.aria-disabled]="!state.isInTable()"
                    [class]="btnClass(false)"
                    (click)="tableInsertRowAbove()">
                    <span aria-hidden="true" class="material-symbols-outlined">add_row_above</span>
                </button>
                <button
                    type="button"
                    [attr.aria-label]="'dot.block.editor.toolbar.table.insert-row-below' | dm"
                    [pTooltip]="'dot.block.editor.toolbar.table.insert-row-below' | dm"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [disabled]="!state.isInTable()"
                    [attr.aria-disabled]="!state.isInTable()"
                    [class]="btnClass(false)"
                    (click)="tableInsertRowBelow()">
                    <span aria-hidden="true" class="material-symbols-outlined">add_row_below</span>
                </button>
                <button
                    type="button"
                    [attr.aria-label]="'dot.block.editor.toolbar.table.insert-column-left' | dm"
                    [pTooltip]="'dot.block.editor.toolbar.table.insert-column-left' | dm"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [disabled]="!state.isInTable()"
                    [attr.aria-disabled]="!state.isInTable()"
                    [class]="btnClass(false)"
                    (click)="tableInsertColLeft()">
                    <span aria-hidden="true" class="material-symbols-outlined">
                        add_column_left
                    </span>
                </button>
                <button
                    type="button"
                    [attr.aria-label]="'dot.block.editor.toolbar.table.insert-column-right' | dm"
                    [pTooltip]="'dot.block.editor.toolbar.table.insert-column-right' | dm"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [disabled]="!state.isInTable()"
                    [attr.aria-disabled]="!state.isInTable()"
                    [class]="btnClass(false)"
                    (click)="tableInsertColRight()">
                    <span aria-hidden="true" class="material-symbols-outlined">
                        add_column_right
                    </span>
                </button>

                <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-indigo-200"></span>

                <!-- Merge / split -->
                <button
                    type="button"
                    [attr.aria-label]="'dot.block.editor.toolbar.table.merge-cells' | dm"
                    [pTooltip]="'dot.block.editor.toolbar.table.merge-cells' | dm"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [disabled]="!state.isInTable() || !state.canMergeCells()"
                    [attr.aria-disabled]="!state.isInTable() || !state.canMergeCells()"
                    [class]="btnClass(false)"
                    (click)="tableMerge()">
                    <span aria-hidden="true" class="material-symbols-outlined">cell_merge</span>
                </button>
                <button
                    type="button"
                    [attr.aria-label]="'dot.block.editor.toolbar.table.split-cell' | dm"
                    [pTooltip]="'dot.block.editor.toolbar.table.split-cell' | dm"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [disabled]="!state.isInTable() || !state.canSplitCell()"
                    [attr.aria-disabled]="!state.isInTable() || !state.canSplitCell()"
                    [class]="btnClass(false)"
                    (click)="tableSplit()">
                    <span aria-hidden="true" class="material-symbols-outlined">call_split</span>
                </button>

                <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-indigo-200"></span>

                <!-- Header toggles -->
                <button
                    type="button"
                    [attr.aria-label]="'dot.block.editor.toolbar.table.toggle-row-header' | dm"
                    [pTooltip]="'dot.block.editor.toolbar.table.toggle-row-header' | dm"
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
                    [attr.aria-label]="'dot.block.editor.toolbar.table.toggle-column-header' | dm"
                    [pTooltip]="'dot.block.editor.toolbar.table.toggle-column-header' | dm"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [disabled]="!state.isInTable()"
                    [attr.aria-disabled]="!state.isInTable()"
                    [class]="btnClass(false)"
                    (click)="tableToggleColHeader()">
                    <span aria-hidden="true" class="material-symbols-outlined">view_column</span>
                </button>

                <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-indigo-200"></span>

                <!-- Delete -->
                <button
                    type="button"
                    [attr.aria-label]="'dot.block.editor.toolbar.table.delete-row' | dm"
                    [pTooltip]="'dot.block.editor.toolbar.table.delete-row' | dm"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [disabled]="!state.isInTable()"
                    [attr.aria-disabled]="!state.isInTable()"
                    [class]="btnClass(false)"
                    (click)="tableDeleteRow()">
                    <span aria-hidden="true" class="material-symbols-outlined">
                        border_horizontal
                    </span>
                </button>
                <button
                    type="button"
                    [attr.aria-label]="'dot.block.editor.toolbar.table.delete-column' | dm"
                    [pTooltip]="'dot.block.editor.toolbar.table.delete-column' | dm"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [disabled]="!state.isInTable()"
                    [attr.aria-disabled]="!state.isInTable()"
                    [class]="btnClass(false)"
                    (click)="tableDeleteCol()">
                    <span aria-hidden="true" class="material-symbols-outlined">
                        border_vertical
                    </span>
                </button>
                <button
                    type="button"
                    [attr.aria-label]="'dot.block.editor.toolbar.table.delete-table' | dm"
                    [pTooltip]="'dot.block.editor.toolbar.table.delete-table' | dm"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [disabled]="!state.isInTable()"
                    [attr.aria-disabled]="!state.isInTable()"
                    [class]="btnClass(false)"
                    (click)="tableDeleteTable()">
                    <span aria-hidden="true" class="material-symbols-outlined">border_clear</span>
                </button>
            }
            @if (isAllowed('emoji')) {
                <button
                    type="button"
                    [attr.aria-label]="'dot.block.editor.toolbar.insert-emoji' | dm"
                    [pTooltip]="'dot.block.editor.toolbar.insert-emoji' | dm"
                    tooltipPosition="bottom"
                    [tooltipOptions]="overlayTooltipOptions()"
                    showDelay="350"
                    [class]="btnClass(false)"
                    (mousedown)="openEmojiPicker($event)">
                    <span aria-hidden="true" class="material-symbols-outlined">emoji_emotions</span>
                </button>
            }
        }

        <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-indigo-200"></span>

        <!-- Markdown copy / paste -->
        <button
            type="button"
            [attr.aria-label]="'block-editor.common.copy-markdown' | dm"
            [pTooltip]="'block-editor.common.copy-markdown' | dm"
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
            [attr.aria-label]="'block-editor.common.paste-markdown' | dm"
            [pTooltip]="'block-editor.common.paste-markdown' | dm"
            tooltipPosition="bottom"
            [tooltipOptions]="overlayTooltipOptions()"
            showDelay="350"
            data-testid="toolbar-paste-markdown"
            [class]="btnClass(false)"
            (click)="pasteFromMarkdown()">
            <span aria-hidden="true" class="material-symbols-outlined">markdown_paste</span>
        </button>

        <span aria-hidden="true" class="mx-1 h-6 w-px shrink-0 bg-indigo-200"></span>
        <button
            type="button"
            [attr.aria-pressed]="isFullscreen()"
            [attr.aria-label]="
                (isFullscreen()
                    ? 'dot.block.editor.toolbar.exit-full-screen'
                    : 'dot.block.editor.toolbar.full-screen'
                ) | dm
            "
            [pTooltip]="
                (isFullscreen()
                    ? 'dot.block.editor.toolbar.exit-full-screen'
                    : 'dot.block.editor.toolbar.full-screen'
                ) | dm
            "
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
    private readonly popovers = inject(EditorPopoverService);
    private readonly editorModal = inject(EditorModalService);
    private readonly contentletEditUrl = inject(ContentletEditUrlService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotMessageService = inject(DotMessageService);

    /** Resolved at construction so the host's `[attr.aria-label]` reads a static string. */
    protected readonly toolbarAriaLabel = this.dotMessageService.get(
        'dot.block.editor.toolbar.aria-label'
    );

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

    private readonly BTN_BASE =
        'inline-flex items-center justify-center w-8 h-8 rounded-md border border-transparent transition-all cursor-pointer disabled:cursor-not-allowed';

    /**
     * Tailwind compiles utility rules into the stylesheet by utility identity, not by the
     * order they appear in the className attribute. Mixing `hover:text-indigo-700` and
     * `hover:text-white` in the same list lets whichever Tailwind emits last in CSS win,
     * which made the active button's white icon flip to indigo-on-indigo on hover (the
     * icon visibly disappeared). Splitting the inactive and active branches into mutually
     * exclusive class strings — no shared `hover:text-*` — avoids the collision entirely.
     */
    protected btnClass(active: boolean): string {
        return [
            this.BTN_BASE,
            'disabled:text-indigo-300 disabled:hover:bg-transparent disabled:hover:shadow-none',
            active
                ? 'bg-indigo-600 text-white hover:bg-indigo-700 hover:shadow-none'
                : 'hover:bg-white/85 hover:shadow-sm'
        ].join(' ');
    }

    protected readonly blockTypeValue = computed(() => {
        const level = this.state.headingLevel();
        return level === null ? 'paragraph' : `h${level}`;
    });

    protected readonly selectPt = {
        root: 'bg-white border border-indigo-200 rounded-md text-sm text-indigo-900 hover:border-indigo-300 transition-colors',
        label: '!text-indigo-900',
        dropdown: 'w-7 text-indigo-500',
        panel: 'bg-white border border-indigo-200 rounded-md shadow-lg mt-1',
        list: 'p-1',
        item: 'px-3 py-1.5 text-sm text-slate-700 rounded hover:bg-indigo-50 hover:text-indigo-700 aria-selected:bg-indigo-600 aria-selected:text-white'
    };

    protected readonly blockTypeOptions = computed(() => {
        const t = (key: string) => this.dotMessageService.get(key);
        const opts: { label: string; value: string }[] = [
            { label: t('dot.block.editor.toolbar.paragraph'), value: 'paragraph' }
        ];
        if (this.store.isAllowed('heading1')) {
            opts.push({ label: t('dot.block.editor.toolbar.heading-1'), value: 'h1' });
        }
        if (this.store.isAllowed('heading2')) {
            opts.push({ label: t('dot.block.editor.toolbar.heading-2'), value: 'h2' });
        }
        if (this.store.isAllowed('heading3')) {
            opts.push({ label: t('dot.block.editor.toolbar.heading-3'), value: 'h3' });
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

        if (this.popovers.isOpen('link')) {
            this.popovers.close();
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

            this.popovers.openLink(() => linkEl.getBoundingClientRect(), {
                initialValues: {
                    href,
                    displayText,
                    target: linkMark.attrs['target'] ?? null,
                    title: linkMark.attrs['title'] ?? null,
                    ariaLabel: linkMark.attrs['aria-label'] ?? null,
                    rel: linkMark.attrs['rel'] ?? null
                },
                linkEl,
                anchorPos
            });
        } else {
            // Insert mode — anchor to the toolbar button
            const selectedText = empty ? '' : editor.state.doc.textBetween(from, to);
            this.popovers.openLink(
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
        this.editorModal.openImagePicker(this.editor());
    }

    protected openVideoDialog(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        this.editorModal.openVideoPicker(this.editor());
    }

    protected openTableDialog(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        if (this.popovers.isOpen('table')) {
            this.popovers.close();
            return;
        }
        const btn = event.currentTarget as HTMLElement;
        this.popovers.open('table', () => btn.getBoundingClientRect());
    }

    protected openEmojiPicker(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        const btn = event.currentTarget as HTMLElement;
        this.popovers.toggle('emoji', () => btn.getBoundingClientRect());
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

    /**
     * Mirrors the legacy bubble-menu behaviour: warn the user about unsaved changes,
     * resolve the destination URL via {@link ContentletEditUrlService} (handles the
     * legacy vs new content-editor flag per content type), drop a `relationshipReturnValue`
     * breadcrumb in localStorage so the destination editor can navigate back, then push
     * the parent window to the resolved URL. The `contentletEdit` output still fires for
     * hosts that want to observe (analytics, custom logging) — it does not gate navigation.
     */
    protected editContentlet(): void {
        const data = this.state.selectedContentlet();
        if (!data) return;

        this.contentletEdit.emit(data);

        this.confirmationService.confirm({
            message: this.dotMessageService.get('message.contentlet.lose.unsaved.changes'),
            header: this.dotMessageService.get('dot.block.editor.contentlet.edit.confirm.header'),
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: this.dotMessageService.get('dot.common.continue'),
            rejectLabel: this.dotMessageService.get('dot.common.cancel'),
            accept: () => this.navigateToContentEditor(data)
        });
    }

    private navigateToContentEditor(data: ContentletEditEvent): void {
        this.contentletEditUrl
            .resolveEditUrl({ inode: data.inode, contentType: data.contentType })
            .subscribe((url) => {
                writeRelationshipReturnBreadcrumb(data.inode);
                if (window.parent) {
                    window.parent.location.href = url;
                }
            });
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
        this.popovers.openImageProperties(() => btn.getBoundingClientRect(), {
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
