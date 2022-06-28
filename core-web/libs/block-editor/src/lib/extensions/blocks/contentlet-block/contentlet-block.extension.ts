import { Node, mergeAttributes, NodeViewRenderer } from '@tiptap/core';
import { DOMOutputSpec, ParseRule } from 'prosemirror-model';
import { Injector } from '@angular/core';

import { ContentletBlockComponent } from '../contentlet-block/contentlet-block.component';
import { AngularNodeViewRenderer } from '../../../NodeViewRenderer';

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
