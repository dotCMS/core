import { DOMOutputSpec } from 'prosemirror-model';

import { Injector } from '@angular/core';

import { Node, NodeViewRenderer } from '@tiptap/core';

import { ContentletBlockComponent } from './contentlet-block.component';

import { AngularNodeViewRenderer, toJSONFn } from '../../NodeViewRenderer';

export type ContentletBlockOptions = {
    HTMLAttributes: Record<string, unknown>;
};

/**
 * Set Custom JSON for this type of Node
 * For this JSON we are going to only add the `contentlet` identifier to the backend
 *
 * @param {*} this
 * @return {*}
 */
const toJSON: toJSONFn = function () {
    const { attrs, type } = this?.node || {}; // Add null check for this.node
    const { data } = attrs;
    const customAttrs = {
        ...attrs,
        data: {
            identifier: data.identifier
        }
    };

    return {
        type: type.name,
        attrs: customAttrs
    };
};

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
                        return { data };
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
            return AngularNodeViewRenderer(ContentletBlockComponent, { injector, toJSON });
        }
    });
};
