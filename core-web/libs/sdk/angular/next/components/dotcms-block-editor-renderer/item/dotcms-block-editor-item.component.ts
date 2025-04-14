import { AsyncPipe, NgComponentOutlet, NgTemplateOutlet } from '@angular/common';
import { Component, Input } from '@angular/core';

import { Blocks } from '@dotcms/uve/internal';
import { ContentNode } from '@dotcms/uve/types';

import { DotCodeBlock, DotBlockQuote } from '../blocks/code.component';
import { DotContentletBlock } from '../blocks/contentlet.component';
import { DotImageBlock } from '../blocks/image.component';
import { DotBulletList, DotOrdererList, DotListItem } from '../blocks/list.component';
import { DotTableBlock } from '../blocks/table.component';
import { DotParagraphBlock, DotTextBlock, DotHeadingBlock } from '../blocks/text.component';
import { DotVideoBlock } from '../blocks/video.components';
import { CustomRenderer } from '../dotcms-block-editor-renderer.component';

@Component({
    selector: 'dotcms-block-editor-renderer-block',
    standalone: true,
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
        DotContentletBlock
    ]
})
export class DotCMSBlockEditorItemComponent {
    @Input() content: ContentNode[] | undefined;
    @Input() customRenderers: CustomRenderer | undefined;

    BLOCKS = Blocks;
}
