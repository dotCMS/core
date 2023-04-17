import { Editor, Extension } from '@tiptap/core';
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

export enum PositionHeadings {
    TOP_INITIAL = '40px',
    TOP_CURRENT = '26px'
}

function toTitleCase(str) {
    return str.replace(/\p{L}+('\p{L}+)?/gu, function (txt) {
        return txt.charAt(0).toUpperCase() + txt.slice(1);
    });
}

const addPlusButton = (pos: number, node, editor: Editor) => {
    const button = document.createElement('button');
    button.classList.add('add-button');
    button.style.position = 'absolute';
    button.style.left = '-45px';
    button.style.top = '-2px';
    const div = document.createElement('div');
    div.style.position = 'relative';
    div.setAttribute('dragable', 'false');
    if (pos === 0 && node.type.name === NodeTypes.HEADING) {
        button.style.top = PositionHeadings.TOP_INITIAL;
    }

    if (pos !== 0 && node.type.name === NodeTypes.HEADING) {
        button.style.top = PositionHeadings.TOP_CURRENT;
    }

    button.innerHTML = '<i class="pi pi-plus"></i>';
    button.setAttribute('dragable', 'false');
    button.addEventListener(
        'mousedown',
        (e) => {
            e.preventDefault();
            editor.chain().insertContent('/').run();
        },
        { once: true }
    );

    div.appendChild(button);

    return div;
};

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

                                const decoration = Decoration.widget(
                                    pos,
                                    addPlusButton(pos, node, this.editor)
                                );

                                const decorationContent = Decoration.node(
                                    pos,
                                    pos + node.nodeSize,
                                    {
                                        class: classes.join(' '),
                                        'data-placeholder':
                                            node.type.name === NodeTypes.HEADING
                                                ? `${toTitleCase(node.type.name)} ${
                                                      node.attrs.level
                                                  }`
                                                : this.options.placeholder
                                    }
                                );

                                decorations.push(decorationContent);
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
