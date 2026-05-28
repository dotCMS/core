import { AsyncPipe, NgComponentOutlet, NgTemplateOutlet } from '@angular/common';
import { Component, input, OnInit, signal } from '@angular/core';

import { UVE_MODE, BlockEditorNode, BlockEditorMark } from '@dotcms/types';
import { BlockEditorState, BlockEditorDefaultBlocks } from '@dotcms/types/internal';
import { getUVEState } from '@dotcms/uve';
import { isValidBlocks } from '@dotcms/uve/internal';

import {
    DotSemanticBlockQuote,
    DotSemanticBulletList,
    DotSemanticCodeBlock,
    DotSemanticListItem,
    DotSemanticOrderedList,
    DotSemanticParagraph
} from './blocks/semantic-blocks.component';

import { DotContentletBlock } from '../dotcms-block-editor-renderer/blocks/dot-contentlet.component';
import { DotUnknownBlockComponent } from '../dotcms-block-editor-renderer/blocks/unknown.component';
import { CustomRenderer } from '../dotcms-block-editor-renderer/dotcms-block-editor-renderer.component';

/**
 * An accessible component that renders content from DotCMS's Block Editor field.
 *
 * This is the semantic-DOM successor to {@link DotCMSBlockEditorRendererComponent}.
 * It emits clean semantic HTML — `<ul><li><p>…</p></li></ul>` — with no custom
 * wrapper elements between semantic tags, so the `list → listitem` relationship
 * required by the HTML spec and assistive technology is preserved. The recursive
 * dispatch is performed with `ng-template` outlets, whose host `<ng-container>`s
 * render as HTML comment nodes (invisible to the accessibility tree).
 *
 * It exposes the **identical public input API** and the same `customRenderers`
 * contract as the deprecated renderer, so migration is just swapping the tag and
 * import.
 *
 * For more information about Block Editor, see {@link https://dev.dotcms.com/docs/block-editor}
 *
 * @example
 * ```html
 * <dotcms-block-editor-renderer-native
 *   [blocks]="myBlockEditorContent"
 *   [customRenderers]="myCustomRenderers">
 * </dotcms-block-editor-renderer-native>
 * ```
 */
@Component({
    selector: 'dotcms-block-editor-renderer-native',
    templateUrl: './dotcms-block-editor-renderer-native.component.html',
    imports: [
        NgTemplateOutlet,
        NgComponentOutlet,
        AsyncPipe,
        DotSemanticParagraph,
        DotSemanticBulletList,
        DotSemanticOrderedList,
        DotSemanticListItem,
        DotSemanticBlockQuote,
        DotSemanticCodeBlock,
        DotContentletBlock,
        DotUnknownBlockComponent
    ]
})
export class DotCMSBlockEditorRendererNativeComponent implements OnInit {
    /** The Block Editor `doc` node to render. */
    readonly blocks = input<BlockEditorNode>();
    /** Map of `node.type` → component to override the built-in render path. */
    readonly customRenderers = input<CustomRenderer | undefined>(undefined);
    /**
     * CSS class on the wrapper element. Aliased as `class` so consumers can
     * pass `[class]="…"` like a normal Angular class binding.
     */
    readonly cssClass = input<string | undefined>(undefined, { alias: 'class' });
    /** Inline style on the wrapper element. */
    readonly style = input<string | Record<string, string> | undefined>(undefined);

    $blockEditorState = signal<BlockEditorState>({ error: null });
    $isInEditMode = signal(getUVEState()?.mode === UVE_MODE.EDIT);

    protected readonly BLOCKS = BlockEditorDefaultBlocks;

    ngOnInit() {
        // `isValidBlocks` declares `blocks: BlockEditorNode` but its first guard
        // handles `undefined` — the cast lines up the types without changing the
        // published `@dotcms/uve` signature.
        const state = isValidBlocks(this.blocks() as BlockEditorNode);

        if (state.error) {
            console.error('Error in dotcms-block-editor-renderer-native: ', state.error);
        }

        this.$blockEditorState.set(state);
    }

    /**
     * Normalizes a heading `level` attribute (which may be a number such as `6`
     * or a string such as `'6'`) to a string for use in the heading `@switch`.
     * Returns `''` for missing or out-of-range levels so the template falls
     * through to the safe `@default` case (`<h2>`).
     */
    asLevel(level: number | string | undefined): string {
        const normalized = level != null ? String(level) : '';
        return /^[1-6]$/.test(normalized) ? normalized : '';
    }

    /** The marks after the current (outermost) one — used to recurse inward. */
    restMarks(marks: BlockEditorMark[] | undefined): BlockEditorMark[] {
        return marks?.slice(1) ?? [];
    }

    /** The attributes of the current (outermost) mark. */
    markAttrs(marks: BlockEditorMark[] | undefined): Record<string, string> {
        return marks?.[0]?.attrs ?? {};
    }

    /**
     * Wrapper style for a `dotImage` `<figure>`, derived from the node's
     * `textWrap` (float left/right) or `textAlign` attribute.
     */
    imageStyle(attrs: BlockEditorNode['attrs']): Record<string, string> {
        const textWrap = attrs?.['textWrap'];
        const textAlign = attrs?.['textAlign'];

        if (textWrap === 'left') {
            return { float: 'left', width: '50%', margin: '0 1rem 1rem 0' };
        }
        if (textWrap === 'right') {
            return { float: 'right', width: '50%', margin: '0 0 1rem 1rem' };
        }
        if (textAlign) {
            return { 'text-align': textAlign };
        }

        return {};
    }

    /** Poster URL for a `dotVideo` `<video>` (from `attrs.data.thumbnail`). */
    videoPoster(attrs: BlockEditorNode['attrs']): string | undefined {
        return attrs?.['data']?.['thumbnail'];
    }

    /**
     * The column span (1–12) for a grid column. Falls back to `6` for malformed
     * `columns` attrs, matching the legacy renderer.
     */
    columnSpan(attrs: BlockEditorNode['attrs'], index: number): number {
        const rawCols = Array.isArray(attrs?.['columns']) ? attrs['columns'] : [6, 6];
        const valid =
            rawCols.length === 2 &&
            rawCols.every((v: unknown) => typeof v === 'number' && Number.isFinite(v));
        return valid ? rawCols[index] : 6;
    }
}
