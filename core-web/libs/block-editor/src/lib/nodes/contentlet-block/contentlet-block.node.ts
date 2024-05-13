import { DOMOutputSpec, TagParseRule } from 'prosemirror-model';

import { Injector } from '@angular/core';

import { Node, NodeViewRenderer } from '@tiptap/core';

import { ContentletBlockComponent } from './contentlet-block.component';

import { AngularNodeViewRenderer } from '../../NodeViewRenderer';

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

        parseHTML(): readonly TagParseRule[] {
            return [{ tag: 'dotcms-contentlet-block' }];
        },

        renderHTML({ HTMLAttributes }): DOMOutputSpec {
            let img = ['span', {}];
            if (HTMLAttributes.data.hasTitleImage) {
                img = ['img', { src: HTMLAttributes.data.image }];
            }

            return [
                'div',
                ['h3', { class: HTMLAttributes.data.title }, HTMLAttributes.data.title],
                ['div', HTMLAttributes.data.identifier],
                img,
                ['div', {}, HTMLAttributes.data.language]
            ];
        },

        addNodeView(): NodeViewRenderer {
            return AngularNodeViewRenderer(ContentletBlockComponent, { injector });
        }
    });
};
