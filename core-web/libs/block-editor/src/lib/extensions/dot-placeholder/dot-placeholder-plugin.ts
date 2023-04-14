import { Extension } from '@tiptap/core';
import { Plugin, PluginKey } from '@tiptap/pm/state';
import { Decoration, DecorationSet } from '@tiptap/pm/view';

import { NodeTypes } from '../bubble-menu/models';

export interface PlaceholderOptions {
    emptyEditorClass: string;
    emptyNodeClass: string;
    placeholder: string;
    showOnlyWhenEditable: boolean;
    showOnlyCurrent: boolean;
    includeChildren: boolean;
}

function toTitleCase(str) {
    return str.replace(/\p{L}+('\p{L}+)?/gu, function (txt) {
        return txt.charAt(0).toUpperCase() + txt.slice(1);
    });
}

export const DotPlaceholder = Extension.create<PlaceholderOptions>({
    name: 'dotPlaceholder',

    addOptions() {
        return {
            emptyEditorClass: 'is-editor-empty',
            emptyNodeClass: 'is-empty',
            placeholder: 'Write something …',
            showOnlyWhenEditable: true,
            showOnlyCurrent: true,
            includeChildren: false
        };
    },

    addProseMirrorPlugins() {
        return [
            new Plugin({
                key: new PluginKey('dotPlaceholder'),
                props: {
                    decorations: ({ doc, selection }) => {
                        const active = this.editor.isEditable || !this.options.showOnlyWhenEditable;

                        const { anchor } = selection;
                        const decorations: Decoration[] = [];

                        if (!active) {
                            return null;
                        }

                        // only calculate isEmpty once due to its performance impacts (see issue #3360)
                        const emptyDocInstance = doc.type.createAndFill();
                        const isEditorEmpty =
                            emptyDocInstance?.sameMarkup(doc) &&
                            emptyDocInstance.content.findDiffStart(doc.content) === null;

                        doc.descendants((node, pos) => {
                            const hasAnchor = anchor >= pos && anchor <= pos + node.nodeSize;
                            const isEmpty = !node.isLeaf && !node.childCount;

                            if ((hasAnchor || !this.options.showOnlyCurrent) && isEmpty) {
                                const classes = [this.options.emptyNodeClass];

                                if (isEditorEmpty) {
                                    classes.push(this.options.emptyEditorClass);
                                }

                                const decoration = Decoration.widget(pos, () => {
                                    const div = document.createElement('div');

                                    if (node.type.name === NodeTypes.HEADING) {
                                        const title = document.createElement(
                                            `h${node.attrs.level}`
                                        );
                                        title.dataset.placeholder = `${toTitleCase(
                                            node.type.name
                                        )} ${node.attrs.level}`;
                                        title.classList.add('is-empty');
                                        div.appendChild(title);
                                    } else {
                                        const paragraph = document.createElement('p');
                                        paragraph.dataset.placeholder = this.options.placeholder;
                                        paragraph.classList.add(...classes);
                                        div.appendChild(paragraph);
                                    }

                                    const button = document.createElement('button');
                                    button.classList.add('add-button');
                                    button.style.position = 'absolute';
                                    button.style.left = '20px';
                                    button.innerHTML = '<i class="material-icons">add</i>';
                                    button.addEventListener('mousedown', (e) => {
                                        e.preventDefault();
                                        this.editor.chain().insertContent('/').run();
                                    });

                                    div.appendChild(button);

                                    return div;
                                });

                                decorations.push(decoration);
                            }

                            return this.options.includeChildren;
                        });

                        return DecorationSet.create(doc, decorations);
                    }
                }
            })
        ];
    }
});
