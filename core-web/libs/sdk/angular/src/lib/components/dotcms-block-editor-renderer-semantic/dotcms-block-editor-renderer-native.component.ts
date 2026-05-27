import { AsyncPipe, NgComponentOutlet, NgTemplateOutlet } from '@angular/common';
import { Component, Input, signal } from '@angular/core';

import { UVE_MODE, BlockEditorNode } from '@dotcms/types';
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
import { DotGridBlock } from '../dotcms-block-editor-renderer/blocks/grid-block.component';
import { DotImageBlock } from '../dotcms-block-editor-renderer/blocks/image.component';
import { DotTableBlock } from '../dotcms-block-editor-renderer/blocks/table.component';
import { DotTextBlock } from '../dotcms-block-editor-renderer/blocks/text.component';
import { DotUnknownBlockComponent } from '../dotcms-block-editor-renderer/blocks/unknown.component';
import { DotVideoBlock } from '../dotcms-block-editor-renderer/blocks/video.component';
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
        DotTextBlock,
        DotImageBlock,
        DotVideoBlock,
        DotTableBlock,
        DotGridBlock,
        DotContentletBlock,
        DotUnknownBlockComponent
    ]
})
export class DotCMSBlockEditorRendererNativeComponent {
    @Input() blocks!: BlockEditorNode;
    @Input() customRenderers: CustomRenderer | undefined;
    @Input() class: string | undefined;
    @Input() style: string | Record<string, string> | undefined;

    $blockEditorState = signal<BlockEditorState>({ error: null });
    $isInEditMode = signal(getUVEState()?.mode === UVE_MODE.EDIT);

    BLOCKS = BlockEditorDefaultBlocks;

    ngOnInit() {
        const state = isValidBlocks(this.blocks);

        if (state.error) {
            console.error('Error in dotcms-block-editor-renderer-native: ', state.error);
        }

        this.$blockEditorState.set(state);
    }

    /**
     * Normalizes a heading `level` attribute (which may be a number such as `6`
     * or a string such as `'6'`) to a string for use in the heading `@switch`.
     */
    asLevel(level: number | string | undefined): string {
        return level != null ? String(level) : '1';
    }
}
