import { Component, Input, signal } from '@angular/core';

import { getUVEState } from '@dotcms/uve';
import { Block, BlockEditorState, isValidBlocks } from '@dotcms/uve/internal';
import { UVE_MODE } from '@dotcms/uve/types';

import { DotCMSBlockEditorItemComponent } from './item/dotcms-block-editor-item.component';

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
    styleUrls: ['./dotcms-block-editor-renderer.component.scss'],
    imports: [DotCMSBlockEditorItemComponent]
})
export class DotCMSBlockEditorRendererComponent {
    @Input() blocks!: Block;
    @Input() customRenderers: CustomRenderer | undefined;

    $blockEditorState = signal<BlockEditorState>({ error: null });
    $isInEditMode = signal(getUVEState()?.mode === UVE_MODE.EDIT);

    ngOnInit() {
        const state = isValidBlocks(this.blocks);

        if (state.error) {
            console.error('Error in dotcms-block-editor-renderer: ', state.error);
        }

        this.$blockEditorState.set(isValidBlocks(this.blocks));
    }
}
