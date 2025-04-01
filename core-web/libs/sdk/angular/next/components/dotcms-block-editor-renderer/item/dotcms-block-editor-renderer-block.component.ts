import { AsyncPipe, NgComponentOutlet, NgTemplateOutlet } from '@angular/common';
import { Component, Input } from '@angular/core';

import { Blocks, ContentNode } from '@dotcms/uve/internal';

import {
    DotCMSBlockEditorRendererCodeBlockComponent,
    DotCMSBlockEditorRendererBlockQuoteComponent
} from '../blocks/code.component';
import { DotCMSBlockEditorRendererContentlet } from '../blocks/contentlet.component';
import { DotCMSBlockEditorRendererImageComponent } from '../blocks/image.component';
import {
    DotCMSBlockEditorRendererBulletListComponent,
    DotCMSBlockEditorRendererOrderedListComponent,
    DotCMSBlockEditorRendererListItemComponent
} from '../blocks/list.component';
import { DotCMSBlockEditorRendererTableComponent } from '../blocks/table.component';
import {
    DotCMSBlockEditorRendererParagraphComponent,
    DotCMSBlockEditorRendererTextComponent,
    DotCMSBlockEditorRendererHeadingComponent
} from '../blocks/text.component';
import { DotCMSBlockEditorRendererVideoComponent } from '../blocks/video.components';
import { CustomRenderer } from '../dotcms-block-editor-renderer.component';

@Component({
    selector: 'dotcms-block-editor-renderer-block',
    standalone: true,
    templateUrl: './dotcms-block-editor-renderer-block.component.html',
    styleUrl: './dotcms-block-editor-renderer-block.component.scss',
    imports: [
        NgTemplateOutlet,
        NgComponentOutlet,
        AsyncPipe,
        DotCMSBlockEditorRendererParagraphComponent,
        DotCMSBlockEditorRendererTextComponent,
        DotCMSBlockEditorRendererHeadingComponent,
        DotCMSBlockEditorRendererBulletListComponent,
        DotCMSBlockEditorRendererOrderedListComponent,
        DotCMSBlockEditorRendererListItemComponent,
        DotCMSBlockEditorRendererCodeBlockComponent,
        DotCMSBlockEditorRendererBlockQuoteComponent,
        DotCMSBlockEditorRendererImageComponent,
        DotCMSBlockEditorRendererVideoComponent,
        DotCMSBlockEditorRendererTableComponent,
        DotCMSBlockEditorRendererContentlet
    ]
})
export class DotCMSBlockEditorRendererBlockComponent {
    @Input() content: ContentNode[] | undefined;
    @Input() customRenderers: CustomRenderer | undefined;

    BLOCKS = Blocks;
}
