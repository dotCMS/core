import { Injector } from '@angular/core';
import { DOMOutputSpec, ParseRule } from 'prosemirror-model';
import { Node, mergeAttributes, NodeViewRenderer } from '@tiptap/core';

import { AngularNodeViewRenderer } from '@dotcms/block-editor';
import { ContentletBlockComponent } from './contentlet-block.component';

export type ContentletBlockOptions = {
    HTMLAttributes: Record<string, unknown>;
};

export const ContentletBlock = (injector: Injector): Node<ContentletBlockOptions> => {
    return Node.create({
        name: 'dotContent',
        group: 'block',
        inline: false,
        draggable: true,

        // ...configuration
        addAttributes() {
            return {
                data: {
                    default: null,
                    parseHTML: (element) => ({
                        data: element.getAttribute('data')
                    }),
                    renderHTML: (attributes) => {
                        return { data: attributes.data };
                    }
                }
            };
        },

        parseHTML(): ParseRule[] {
            return [{ tag: 'dotcms-contentlet-block' }];
        },

        renderHTML({ HTMLAttributes }): DOMOutputSpec {
            return ['dotcms-contentlet-block', mergeAttributes(HTMLAttributes)];
        },

        addNodeView(): NodeViewRenderer {
            return AngularNodeViewRenderer(ContentletBlockComponent, { injector });
        }
    });
};
