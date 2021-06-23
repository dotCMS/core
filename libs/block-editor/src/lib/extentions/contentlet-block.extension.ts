import { Node, mergeAttributes, Editor, Range, NodeViewRenderer } from '@tiptap/core';
import { DOMOutputSpec, ParseRule } from 'prosemirror-model';
import { ComponentFactoryResolver, Injector } from '@angular/core';
import Suggestion, { SuggestionOptions, SuggestionProps } from '@tiptap/suggestion';
import tippy from 'tippy.js';

import { SuggestionsComponent } from '../suggestions/suggestions.component';
import { ContentletBlockComponent } from './contentlet-block/contentlet-block.component';
import { AngularNodeViewRenderer } from '../NodeViewRenderer';

export type ContentletBlockOptions = {
    HTMLAttributes: Record<string, unknown>;
    suggestion: Omit<SuggestionOptions, 'editor'>;
};

export const ContentletBlock = (
    injector: Injector,
    resolver: ComponentFactoryResolver
): Node<ContentletBlockOptions> => {
    return Node.create({
        name: 'dotContent',
        defaultOptions: {
            HTMLAttributes: {},
            suggestion: {
                char: '/c',
                allowSpaces: true,
                startOfLine: true,
                command: ({
                    editor,
                    range,
                    props
                }: {
                    editor: Editor;
                    range: Range;
                    props: unknown;
                }) => {
                    editor
                        .chain()
                        .focus()
                        .insertContentAt(range, {
                            type: 'dotContent',
                            attrs: {
                                data: props
                            }
                        })
                        .run();
                },
                allow: ({ editor, range }: SuggestionProps) => {
                    return editor.can().insertContentAt(range, { type: 'dotContent' });
                },
                items: (param) => {
                    console.log({ param });
                    return [];
                },
                render: () => {
                    let myTippy;

                    return {
                        onStart: (props: SuggestionProps) => {
                            const factory = resolver.resolveComponentFactory(SuggestionsComponent);
                            const component = factory.create(injector);
                            component.instance.command = props.command;
                            component.changeDetectorRef.detectChanges();

                            myTippy = tippy(props.editor.view.dom, {
                                appendTo: document.body,
                                content: component.location.nativeElement,
                                placement: 'auto-start',
                                getReferenceClientRect: props.clientRect,
                                showOnCreate: true,
                                interactive: true,
                                trigger: 'manual'
                            });
                        },
                        onExit: () => {
                            myTippy.destroy();
                        }
                    };
                }
            }
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

        parseHTML(): ParseRule[] {
            console.log('parseHTML');
            return [{ tag: 'dotcms-contentlet-block' }];
        },

        renderHTML({ HTMLAttributes }): DOMOutputSpec {
            console.log('renderHTML');
            return ['dotcms-contentlet-block', mergeAttributes(HTMLAttributes)];
        },

        addNodeView(): NodeViewRenderer {
            console.log('addNodeView');
            return AngularNodeViewRenderer(ContentletBlockComponent, { injector });
        },

        addProseMirrorPlugins() {
            return [
                Suggestion({
                    editor: this.editor,
                    ...this.options.suggestion
                })
            ];
        }
    });
};
