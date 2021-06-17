import { Injector } from '@angular/core';
import { Node, mergeAttributes } from '@tiptap/core';
import { AngularNodeViewRenderer } from '../NodeViewRenderer';
import { ContentletBlockComponent } from './contentlet-block/contentlet-block.component';

export const ContentletBlockExtension = (injector: Injector): Node => {
    console.log('ContentletBlockExtension');
    return Node.create({
        name: 'dotContent',
        defaultOptions: {
            HTMLAttributes: {},
            value: ''
        },
        group: 'block',
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

        parseHTML() {
            console.log('parseHTML');
            return [{ tag: 'dotcms-contentlet-block' }];
        },
        renderHTML({ HTMLAttributes }) {
            console.log('renderHTML');
            return ['dotcms-contentlet-block', mergeAttributes(HTMLAttributes)];
        },
        addNodeView() {
            console.log('addNodeView');
            return AngularNodeViewRenderer(ContentletBlockComponent, { injector });
        }
    });
};
