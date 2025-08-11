import { DOMOutputSpec } from 'prosemirror-model';

import { Injector } from '@angular/core';

import { Node, NodeViewRenderer } from '@tiptap/core';

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
            type ContentletData = {
                title?: string;
                identifier?: string;
                language?: string | number;
                languageId?: string | number;
                hasTitleImage?: boolean;
                image?: string;
            };

            const rawData: ContentletData =
                (HTMLAttributes as { data?: ContentletData })?.data ?? {};

            const titleText = rawData.title ?? '';
            const identifierText = rawData.identifier ?? '';
            const languageText = rawData.language ?? rawData.languageId ?? '';
            const hasImage = Boolean(rawData.hasTitleImage && rawData.image);

            const children: DOMOutputSpec[] = [
                ['h3', String(titleText)],
                ['div', String(identifierText)],
                ['div', {}, String(languageText)]
            ];

            if (hasImage) {
                // Insert image after identifier block
                children.splice(2, 0, ['img', { src: String(rawData.image) }]);
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
