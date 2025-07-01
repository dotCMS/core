import { Editor, Extension } from '@tiptap/core';
import { Plugin, PluginKey } from '@tiptap/pm/state';
import { Decoration, DecorationSet } from '@tiptap/pm/view';

import { NodeTypes } from '../bubble-menu/models';

export interface DotPlusButtonOptions {
    showOnlyWhenEditable: boolean;
    showOnlyCurrent: boolean;
    includeChildren: boolean;
}

export enum PositionHeadings {
    TOP_INITIAL = '40px',
    TOP_CURRENT = '26px'
}

const addPlusButton = (pos: number, node, editor: Editor) => {
    const button = document.createElement('button');
    button.classList.add('add-button');
    button.style.position = 'absolute';
    button.style.left = '-45px';
    button.style.top = '-2px';
    const div = document.createElement('div');
    div.style.position = 'relative';
    div.setAttribute('draggable', 'false');

    if (pos === 0 && node.type.name === NodeTypes.HEADING) {
        button.style.top = PositionHeadings.TOP_INITIAL;
    }

    if (pos !== 0 && node.type.name === NodeTypes.HEADING) {
        button.style.top = PositionHeadings.TOP_CURRENT;
    }

    button.innerHTML = '<i class="pi pi-plus"></i>';
    button.setAttribute('draggable', 'false');
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

export const DotCMSPlusButton = Extension.create<DotPlusButtonOptions>({
    name: 'dotCMSPlusButton',

    addOptions() {
        return {
            showOnlyWhenEditable: true,
            showOnlyCurrent: true,
            includeChildren: false
        };
    },

    addProseMirrorPlugins() {
        return [
            new Plugin({
                key: new PluginKey('dotCMSPlusButton'),
                props: {
                    decorations: ({ doc, selection }) => {
                        const active = this.editor.isEditable || !this.options.showOnlyWhenEditable;

                        if (!active) {
                            return null;
                        }

                        const { anchor } = selection;
                        const decorations: Decoration[] = [];

                        doc.descendants((node, pos) => {
                            const hasAnchor = anchor >= pos && anchor <= pos + node.nodeSize;
                            const isEmpty = !node.isLeaf && !node.childCount;

                            if ((hasAnchor || !this.options.showOnlyCurrent) && isEmpty) {
                                const decoration = Decoration.widget(
                                    pos,
                                    addPlusButton(pos, node, this.editor)
                                );
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
