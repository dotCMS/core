import { Component, Input, signal } from '@angular/core';

import { getUVEState } from '@dotcms/uve';
import { Block, BlockEditorState, isValidBlocks } from '@dotcms/uve/internal';
import { UVE_MODE } from '@dotcms/uve/types';

import { DotCMSBlockEditorRendererBlockComponent } from './item/dotcms-block-editor-renderer-block.component';

import { DynamicComponentEntity } from '../../models';

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
export class DotCMSBlockEditorRendererComponent {
    @Input() blocks!: Block;
    @Input() customRenderers: CustomRenderer | undefined;

    blockEditorState = signal<BlockEditorState>({ error: null });
    isInEditMode = signal(getUVEState()?.mode === UVE_MODE.EDIT);

    ngOnInit() {
        this.blockEditorState.set(isValidBlocks(this.blocks));
    }
}
