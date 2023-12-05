import { Node } from '@tiptap/core';

import { NodeTypes } from '../../extensions';
import { findNodeByType } from '../../shared';

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        AIContentNode: {
            insertAINode: (content?: string) => ReturnType;
        };
    }
}

export const AIContentNode = Node.create({
    name: NodeTypes.AI_CONTENT,

    addAttributes() {
        return {
            content: {
                default: ''
            }
        };
    },

    parseHTML() {
        return [
            {
                tag: 'div[ai-content]'
            }
        ];
    },

    addOptions() {
        return {
            inline: false
        };
    },

    inline() {
        return this.options.inline;
    },

    group() {
        return 'block';
    },

    addCommands() {
        /**
         * Add commands to handle AI_CONTENT node insertion or content replacement in the Tiptap editor.
         *
         * @returns {Object} An object containing commands for the AIContentNode:
         *   - `insertAINode`: A command to insert a new AI_CONTENT node or replace the content of an existing one.
         *                      If an AI_CONTENT node is found in the document, its content is replaced.
         *                      If not found, a new AI_CONTENT node is inserted with the specified content.
         *                      Returns `true` on success.
         *
         * @param {string} content - The content to set for the AI_CONTENT node.
         * @param {EditorCommands} commands - Tiptap commands object for handling editor operations.
         * @param {Editor} editor - The Tiptap editor instance.
         * @param {Transaction} tr - The ProseMirror transaction object representing the editor state.
         */
        return {
            ...this.parent?.(),
            insertAINode:
                (content?: string) =>
                ({ commands, editor, tr }) => {
                    // Check if the AI_CONTENT node exists in the document.
                    const nodeInformation = findNodeByType(editor, NodeTypes.AI_CONTENT);

                    // If an AI_CONTENT node is found, replace its content.
                    if (nodeInformation) {
                        tr.setNodeMarkup(nodeInformation.from, undefined, {
                            content: content
                        });
                        // Set the node selection to the beginning of the replaced content.
                        commands.setNodeSelection(nodeInformation.from);

                        return true;
                    }

                    // If no AI_CONTENT node is found, insert a new one with the specified content.
                    return commands.insertContent({
                        type: this.name,
                        attrs: { content: content }
                    });
                }
        };
    },

    renderHTML() {
        return ['div[ai-content]'];
    },

    addNodeView() {
        return ({ node }) => {
            const dom = document.createElement('div');
            const div = document.createElement('div');

            div.innerHTML = node.attrs.content || '';

            dom.contentEditable = 'true';
            dom.classList.add('ai-content-container');
            dom.append(div);

            return { dom };
        };
    }
});
