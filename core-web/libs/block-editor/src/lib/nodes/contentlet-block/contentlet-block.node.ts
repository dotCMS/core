import { DOMOutputSpec, ParseRule } from 'prosemirror-model';

import { Injector } from '@angular/core';

import { Editor, generateHTML, mergeAttributes, Node, NodeViewRenderer } from '@tiptap/core';

import { ContentletBlockComponent } from './contentlet-block.component';

import { AngularRenderer } from '../../AngularRenderer';
import { AngularNodeViewRenderer } from '../../NodeViewRenderer';

export type ContentletBlockOptions = {
    HTMLAttributes: Record<string, unknown>;
};

export const ContentletBlock = (injector: Injector, editor: Editor): Node<ContentletBlockOptions> => {
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

        renderHTML( props): DOMOutputSpec {

           // const renderer = new AngularRenderer(ContentletBlockComponent, injector, props);
            
            //const renderer = new AngularRenderer(DotContentletThumbnail, injector, props);
          //  console.log('renderer dom 2:', renderer.dom);
    
         //   console.log('editor.view.dom', editor.view.dom);
            //console.log('HTMLAttributes:', props.HTMLAttributes);
           // const parser = new DOMParser;
         
             /*  parser.parseFromString("<div></div>", 'text/html');
            
          //  return  parser.parseFromString("<div></div>", 'text/html');
        /*      return [
                'dot-contentlet-thumbnail',
                {
                    width: '94',
                    height: '94',
                    iconSize: "'72px'",
                    contentlet: props.HTMLAttributes.data
                }
            ]; */

            // eslint-disable-next-line no-console
            //console.log("getJSON",editor.getJSON())
           // console.log("getHTML", generateHTML(editor.getJSON(), editor.))
           // console.log("renderer", renderer.dom)
            
            //return renderer.dom;

            return ['dotcms-contentlet-block', mergeAttributes(props.HTMLAttributes)];
        },

        addNodeView(): NodeViewRenderer {
            return AngularNodeViewRenderer(ContentletBlockComponent, { injector });
        }
    });
};
