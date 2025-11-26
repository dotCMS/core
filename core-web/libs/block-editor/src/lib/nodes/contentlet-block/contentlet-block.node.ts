import { DOMOutputSpec } from 'prosemirror-model';

import { Injector } from '@angular/core';

import { Node, NodeViewRenderer } from '@tiptap/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { ContentletBlockComponent } from './contentlet-block.component';

import { AngularNodeViewRenderer } from '../../NodeViewRenderer';
import { contentletToJSON } from '../../shared';

export type ContentletBlockOptions = {
    HTMLAttributes: Record<string, unknown>;
};

export const ContentletBlock = (injector: Injector): Node<ContentletBlockOptions> => {
    return Node.create({
        name: 'dotContent',
        group: 'block',
        inline: false,
        draggable: true,
        selectable: true,

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
            const rawData: DotCMSContentlet = data;

            if (!rawData) {
                return ['div', { 'data-dotCMS-contentlet': 'true' }];
            }

            const titleText = rawData.title ?? '';
            const identifierText = rawData.identifier ?? '';
            const languageText = rawData.language ?? rawData.languageId ?? '';
            const hasImage = Boolean(rawData.titleImage || rawData.image);
            const image = `/dA/${rawData.inode}`;

            const children: DOMOutputSpec[] = [
                ['h3', String(titleText)],
                ['div', String(identifierText)],
                ['div', {}, String(languageText)]
            ];

            if (hasImage) {
                children.splice(2, 0, ['img', { src: String(image) }]);
            }

            return ['div', { 'data-dotCMS-contentlet': 'true' }, ...children];
        },

        addNodeView(): NodeViewRenderer {
            return AngularNodeViewRenderer(ContentletBlockComponent, {
                injector,
                toJSON: contentletToJSON
            });
        }
    });
};
