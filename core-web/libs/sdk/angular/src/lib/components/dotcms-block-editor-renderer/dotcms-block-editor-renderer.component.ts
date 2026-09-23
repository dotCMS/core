import { Component, signal, ChangeDetectionStrategy, input } from '@angular/core';

import { UVE_MODE, BlockEditorNode } from '@dotcms/types';
import { BlockEditorState } from '@dotcms/types/internal';
import { getUVEState } from '@dotcms/uve';
import { isValidBlocks } from '@dotcms/uve/internal';

import { DotCMSBlockEditorItemComponent } from './item/dotcms-block-editor-item.component';

import { DynamicComponentEntity } from '../../models';

/**
 * Represents a Custom Renderer used by the Block Editor Component
 *
 * @export
 * @interface CustomRenderer
 */
export type CustomRenderer = Record<string, DynamicComponentEntity>;

/**
 * A component that renders content from DotCMS's Block Editor field.
 *
 * This component provides an easy way to render Block Editor content in your Angular applications.
 * It handles the rendering of standard blocks and allows customization through custom renderers.
 *
 * For more information about Block Editor, see {@link https://dev.dotcms.com/docs/block-editor}
 *
 * @example
 * ```html
 * <dotcms-block-editor-renderer
 *   [blocks]="myBlockEditorContent"
 *   [customRenderers]="myCustomRenderers">
 * </dotcms-block-editor-renderer>
 * ```
 *
 * @deprecated Use {@link DotCMSBlockEditorRendererNativeComponent}
 * (`<dotcms-block-editor-renderer-native>`) for accessible, semantic DOM output.
 * This component wraps every semantic tag in a custom element (e.g. a dispatcher
 * element sits between `<ul>` and its `<li>` children), which breaks the
 * `list → listitem` relationship required by the HTML spec and assistive technology.
 * The native renderer keeps the identical public input API — migration is just
 * swapping the tag and import. This component is retained for backward compatibility
 * and will be removed in a future major version.
 */
@Component({
    selector: 'dotcms-block-editor-renderer',
    templateUrl: './dotcms-block-editor-renderer.component.html',
    styleUrls: ['./dotcms-block-editor-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [DotCMSBlockEditorItemComponent]
})
export class DotCMSBlockEditorRendererComponent {
    readonly blocks = input<BlockEditorNode>();
    readonly customRenderers = input<CustomRenderer>();
    readonly class = input<string>();
    readonly style = input<string | Record<string, string>>();

    $blockEditorState = signal<BlockEditorState>({ error: null });
    $isInEditMode = signal(getUVEState()?.mode === UVE_MODE.EDIT);

    ngOnInit() {
        // `isValidBlocks` declares `blocks: BlockEditorNode` but its first guard
        // handles `undefined`, and the component renders its error branch for a
        // missing value — same treatment as the -native renderer.
        const state = isValidBlocks(this.blocks() as BlockEditorNode);

        if (state.error) {
            console.error('Error in dotcms-block-editor-renderer: ', state.error);
        }

        this.$blockEditorState.set(state);
    }
}
