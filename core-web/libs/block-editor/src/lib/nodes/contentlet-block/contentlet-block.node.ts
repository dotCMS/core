import { DOMOutputSpec, ParseRule } from 'prosemirror-model';

import { Injector } from '@angular/core';

import { mergeAttributes, Node, NodeViewRenderer } from '@tiptap/core';

import { ContentletBlockComponent } from './contentlet-block.component';

import { AngularNodeViewRenderer } from '../../NodeViewRenderer';
import { AngularRenderer } from '../../AngularRenderer';

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

        renderHTML(props): DOMOutputSpec {
            console.log('props', props);
            const renderer = new AngularRenderer(ContentletBlockComponent, injector, {
                width: '94',
                height: '94',
                iconSize: "'72px'",
                contentlet: props.HTMLAttributes.data
            });
            console.log('renderer', renderer.dom);
            console.log('HTMLAttributes', props.HTMLAttributes);

            return [
                'dot-contentlet-thumbnail',
                {
                    width: '94',
                    height: '94',
                    iconSize: "'72px'",
                    contentlet: props.HTMLAttributes.data
                }
            ];
        },

        addNodeView(): NodeViewRenderer {
            return AngularNodeViewRenderer(ContentletBlockComponent, { injector });
        }
    });
};
