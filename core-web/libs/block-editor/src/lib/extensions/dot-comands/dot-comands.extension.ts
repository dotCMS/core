import { EditorView } from 'prosemirror-view';

import { Extension } from '@tiptap/core';

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        DotComands: {
            isNodeRegistered: (nodeName: string) => ReturnType;
        };
    }
}

/**
 * This extension is used to add dotCMS specific custom commands to share between extensions
 * @type {*}
 */
export const DotComands = Extension.create({
    name: 'dotComands',

    addCommands() {
        return {
            isNodeRegistered:
                (nodeName: string) =>
                ({ view }: { view: EditorView }) => {
                    const { schema } = view.state;
                    const { nodes } = schema;

                    return Boolean(nodes[nodeName]);
                }
        };
    }
});
