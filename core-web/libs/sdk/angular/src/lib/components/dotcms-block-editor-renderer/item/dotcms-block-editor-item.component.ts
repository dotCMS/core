import { AsyncPipe, NgComponentOutlet, NgTemplateOutlet } from '@angular/common';
import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

import { groupLinkRuns, resolveEmoji, type EmojiWarnScope } from '@dotcms/client/internal';
import { BlockEditorMark, BlockEditorNode } from '@dotcms/types';
import { BlockEditorDefaultBlocks } from '@dotcms/types/internal';

import { DotAudioBlock } from '../blocks/audio.component';
import { DotCodeBlock, DotBlockQuote } from '../blocks/code.component';
import { DotContentletBlock } from '../blocks/dot-contentlet.component';
import { DotGridBlock } from '../blocks/grid-block.component';
import { DotImageBlock } from '../blocks/image.component';
import { DotBulletList, DotOrdererList, DotListItem } from '../blocks/list.component';
import { DotTableBlock } from '../blocks/table.component';
import { DotParagraphBlock, DotTextBlock, DotHeadingBlock } from '../blocks/text.component';
import { DotUnknownBlockComponent } from '../blocks/unknown.component';
import { DotVideoBlock } from '../blocks/video.component';
import { CustomRenderer } from '../dotcms-block-editor-renderer.component';

@Component({
    selector: 'dotcms-block-editor-renderer-block',
    templateUrl: './dotcms-block-editor-item.component.html',
    styleUrls: ['./dotcms-block-editor-item.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [
        NgTemplateOutlet,
        NgComponentOutlet,
        AsyncPipe,
        DotParagraphBlock,
        DotTextBlock,
        DotHeadingBlock,
        DotBulletList,
        DotOrdererList,
        DotListItem,
        DotCodeBlock,
        DotBlockQuote,
        DotImageBlock,
        DotVideoBlock,
        DotAudioBlock,
        DotTableBlock,
        DotGridBlock,
        DotContentletBlock,
        DotUnknownBlockComponent
    ]
})
export class DotCMSBlockEditorItemComponent {
    /**
     * Siblings grouped into link runs (#37340).
     *
     * Rendering node-by-node emits one `<a>` per text node, so a link split by a legacy `emoji`
     * node became two anchors: two tab stops and two screen-reader entries for one logical link.
     * The `link` mark is stripped from a run's children so the text component does not emit a
     * second, nested anchor inside the one the template already provides.
     */
    groups: Array<{ link?: BlockEditorMark; nodes: BlockEditorNode[] }> = [];

    /** One scope per component so a repeated unresolvable emoji warns once. */
    private readonly warned: EmojiWarnScope = new Set<string>();

    private contentValue: BlockEditorNode[] | undefined;

    @Input()
    set content(value: BlockEditorNode[] | undefined) {
        this.contentValue = value;
        this.groups = groupLinkRuns(value ?? []).map((group) => ({
            link: group.link,
            nodes: group.link
                ? group.nodes.map((node) => ({
                      ...node,
                      marks: node.marks?.filter((mark) => mark.type !== 'link')
                  }))
                : group.nodes
        }));
    }

    get content(): BlockEditorNode[] | undefined {
        return this.contentValue;
    }

    @Input() customRenderers: CustomRenderer | undefined;

    /** Resolves a legacy `emoji` node to its character; see the render contract. */
    emojiText(node: BlockEditorNode): string {
        return resolveEmoji(node, this.warned);
    }

    BLOCKS = BlockEditorDefaultBlocks;
}
