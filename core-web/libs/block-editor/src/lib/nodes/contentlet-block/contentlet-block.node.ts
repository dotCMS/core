import { DOMOutputSpec } from 'prosemirror-model';

import { Injector } from '@angular/core';

import { Node, NodeViewRenderer } from '@tiptap/core';

import { ContentletBlockComponent } from './contentlet-block.component';

import { AngularNodeViewRenderer } from '../../NodeViewRenderer';

export type ContentletBlockOptions = {
    HTMLAttributes: Record<string, unknown>;
};

/**
 * Remove nested editor fields from the contentlet object
 *
 * @param {*} obj
 * @return {*}
 */
function removeNestedEditorSchema(obj) {
    for (const key in obj) {
        if (typeof obj[key] === 'object' && obj[key]?.type === 'doc') {
            delete obj[key];
        }
    }

    return obj;
}

export const ContentletBlock = (injector: Injector): Node<ContentletBlockOptions> => {
    return Node.create({
        name: 'dotContent',
        group: 'block',
        inline: false,
        draggable: true,

        addAttributes() {
            return {
                data: {
                    default: null,
                    rendered: true,
                    parseHTML: (element) => {
                        return {
                            data: element.getAttribute('data')
                        };
                    },
                    renderHTML: ({ data }) => {
                        return { data: removeNestedEditorSchema(data) };
                    }
                }
            };
        },

        renderHTML({ HTMLAttributes }): DOMOutputSpec {
            const { data } = HTMLAttributes;
            const img = data.hasTitleImage ? ['img', { src: data.image }] : ['span', {}];

            return [
                'div',
                ['h3', data.title],
                ['div', data.identifier],
                img,
                ['div', {}, data.language]
            ];
        },

        addNodeView(): NodeViewRenderer {
            return AngularNodeViewRenderer(ContentletBlockComponent, { injector });
        }
    });
};
