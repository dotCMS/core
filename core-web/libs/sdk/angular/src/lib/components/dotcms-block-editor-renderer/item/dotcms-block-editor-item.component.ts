import { AsyncPipe, NgComponentOutlet, NgTemplateOutlet } from '@angular/common';
import { Component, Input } from '@angular/core';

import { BlockEditorNode } from '@dotcms/types';
import { BlockEditorDefaultBlocks } from '@dotcms/types/internal';

import { DotCodeBlock, DotBlockQuote } from '../blocks/code.component';
import { DotContentletBlock } from '../blocks/dot-contentlet.component';
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
        DotTableBlock,
        DotContentletBlock,
        DotUnknownBlockComponent
    ]
})
export class DotCMSBlockEditorItemComponent {
    @Input() content: BlockEditorNode[] | undefined;
    @Input() customRenderers: CustomRenderer | undefined;

    BLOCKS = BlockEditorDefaultBlocks;
}
