import { Component, Input } from "@angular/core";

import { DotCMSBlockEditorRendererBlockComponent } from "./item/dotcms-block-editor-renderer-block.component";
import { Block } from "./models/block-editor-renderer.models";

import { DynamicComponentEntity } from "../../models";

/**
 * Represents a Custom Renderer used by the Block Editor Component
 *
 * @export
 * @interface CustomRenderer
 */
export type CustomRenderer = Record<string, DynamicComponentEntity>;

@Component({
    selector: 'dotcms-block-editor-renderer',
    standalone: true,
    templateUrl: './dotcms-block-editor-renderer.component.html',
    styleUrl: './dotcms-block-editor-renderer.component.scss',
    imports: [DotCMSBlockEditorRendererBlockComponent]
})
export class DotCMSBlockEditorRendererComponent  {
    @Input() blocks!: Block;
    @Input() customRenderers!: CustomRenderer;
}