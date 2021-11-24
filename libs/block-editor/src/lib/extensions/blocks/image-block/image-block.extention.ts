import { Node, mergeAttributes, NodeViewRenderer } from '@tiptap/core';
import { Injector } from '@angular/core';
import { DOMOutputSpec, ParseRule } from 'prosemirror-model';
import { ImageBlockComponent } from './image-block.component';
import { AngularNodeViewRenderer } from '../../../NodeViewRenderer';

export const ImageBlock = (injector: Injector): Node => {
    return Node.create({
        name: 'dotImage',
        group: 'block',
        inline: false,
        draggable: true,

        addAttributes() {
            return {
                data: {
                    default: null,
                    parseHTML: (element) => {
                        data: element.getAttribute('data');
                    },
                    renderHTML: (attributes) => {
                        return { data: attributes.data };
                    }
                }
            };
        },

        parseHTML(): ParseRule[] {
            return [{ tag: 'dotcms-image-block' }];
        },

        renderHTML({ HTMLAttributes }): DOMOutputSpec {
            return ['dotcms-image-block', mergeAttributes(HTMLAttributes)];
        },

        addNodeView(): NodeViewRenderer {
            return AngularNodeViewRenderer(ImageBlockComponent, { injector });
        }
    });
};
