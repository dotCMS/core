import { Extension, isNodeEmpty } from '@tiptap/core';
import { Plugin, PluginKey } from '@tiptap/pm/state';
import { Decoration, DecorationSet } from '@tiptap/pm/view';

export const DotCMSPlusButton = Extension.create({
    name: 'dotCMSPlusButton',

    addProseMirrorPlugins() {
        return [
            new Plugin({
                key: new PluginKey('dotCMSPlusButton'),
                props: {
                    decorations: ({ doc, selection }) => {
                        const decorations: Decoration[] = [];
                        const { anchor } = selection;
                        const active = this.editor.isEditable;

                        if (!active) return null;

                        doc.descendants((node, pos) => {
                            const isEmpty = !node.isLeaf && isNodeEmpty(node) && !node.childCount;
                            const hasAnchor = anchor >= pos && anchor <= pos + node.nodeSize;

                            if (isEmpty && hasAnchor) {
                                const widget = Decoration.widget(
                                    pos + 1,
                                    () => {
                                        const button = document.createElement('button');
                                        button.textContent = '+';
                                        button.classList.add('add-button');

                                        button.addEventListener(
                                            'mousedown',
                                            (e) => {
                                                e.preventDefault();
                                                this.editor
                                                    .chain()
                                                    .focus()
                                                    .insertContent('/')
                                                    .run();
                                            },
                                            { once: true }
                                        );

                                        return button;
                                    },
                                    { side: -1 }
                                );

                                decorations.push(widget);
                            }

                            return false;
                        });

                        return DecorationSet.create(doc, decorations);
                    }
                }
            })
        ];
    }
});
