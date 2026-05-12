import { createLowlight } from 'lowlight';
import { AngularNodeViewRenderer } from 'ngx-tiptap';

import { Injector } from '@angular/core';

import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight';

import { DotCodeBlockNodeViewComponent } from './code-block.component';

type Lowlight = ReturnType<typeof createLowlight>;

/**
 * CodeBlockLowlight with an Angular node view that adds a per-block language selector.
 * Same node name (`codeBlock`) and JSON shape as StarterKit's default — legacy content
 * without a `language` attribute renders fine; lowlight auto-detects.
 */
export function createCodeBlock(injector: Injector, lowlight: Lowlight) {
    return CodeBlockLowlight.extend({
        addNodeView() {
            return AngularNodeViewRenderer(DotCodeBlockNodeViewComponent, { injector });
        }
    }).configure({ lowlight });
}
