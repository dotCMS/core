
import { AsyncPipe, NgComponentOutlet, NgTemplateOutlet } from "@angular/common";
import { Component, Input } from "@angular/core";


import { DotCMSBlockEditorRendererParagraphComponent, DotCMSBlockEditorRendererTextComponent, DotCMSBlockEditorRendererHeadingComponent } from "../blocks/text.component";
import { CustomRenderer } from "../dotcms-block-editor-renderer.component";
import { Blocks, ContentNode } from "../models/block-editor-renderer.models";

@Component({
    selector: 'dotcms-block-editor-renderer-block',
    standalone: true,
    templateUrl: './dotcms-block-editor-renderer-block.component.html',
    styleUrl: './dotcms-block-editor-renderer-block.component.css',
    imports: [NgTemplateOutlet, NgComponentOutlet, AsyncPipe, DotCMSBlockEditorRendererParagraphComponent, DotCMSBlockEditorRendererTextComponent, DotCMSBlockEditorRendererHeadingComponent]
})
export class DotCMSBlockEditorRendererBlockComponent {
    @Input() content!: ContentNode[];
    @Input() customRenderers: CustomRenderer | undefined;

    BLOCKS = Blocks;
}
