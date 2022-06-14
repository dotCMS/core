import { Node, mergeAttributes, NodeViewRenderer } from '@tiptap/core';
import { Injector } from '@angular/core';
import { DOMOutputSpec, ParseRule } from 'prosemirror-model';
import { ImageBlockComponent } from './image-block.component';
import { AngularNodeViewRenderer } from '../../../NodeViewRenderer';

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        ImageBlock: {
            /**
             * Set Image Link mark
             */
            setImageLink: (attributes: { href: string }) => ReturnType;
            /**
             * Unset Image Link mark
             */
            unsetImageLink: () => ReturnType;
        };
    }
}

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
                    parseHTML: (element) => element.getAttribute('data'),
                    renderHTML: (attributes) => {
                        return { data: attributes.data };
                    }
                },
                href: {
                    default: null,
                    parseHTML: (element) => element.getAttribute('href'),
                    renderHTML: (attributes) => {
                        return { href: attributes.href };
                    }
                }
            };
        },

        addCommands() {
            return {
                setImageLink:
                    (attributes) =>
                    ({ commands }) => {
                        return commands.updateAttributes('dotImage', attributes);
                    },
                unsetImageLink:
                    () =>
                    ({ commands }) => {
                        return commands.updateAttributes('dotImage', { href: '' });
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
