import { PluginKey, Plugin, Transaction, EditorState } from 'prosemirror-state';

import { Extension } from '@tiptap/core';

export const FREEZE_SCROLL_KEY = new PluginKey('freeze-scroll');

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        FreezeScroll: {
            freezeScroll: (value: boolean) => ReturnType;
        };
    }
}

interface PluginState {
    freezeScroll: boolean;
}

export const FreezeScroll = Extension.create({
    addCommands() {
        return {
            freezeScroll:
                (value) =>
                ({ chain }) => {
                    return chain()
                        .command(({ tr }) => {
                            tr.setMeta(FREEZE_SCROLL_KEY, { freezeScroll: value });

                            return true;
                        })
                        .run();
                }
        };
    },

    addProseMirrorPlugins() {
        return [FreezeScrollPlugin];
    }
});

export const FreezeScrollPlugin = new Plugin({
    key: FREEZE_SCROLL_KEY,
    state: {
        init(): PluginState {
            return {
                freezeScroll: false
            };
        },
        apply(transaction: Transaction, value: PluginState, oldState: EditorState): PluginState {
            const { freezeScroll } = transaction.getMeta(FREEZE_SCROLL_KEY) || {};
            const state = FREEZE_SCROLL_KEY?.getState(oldState);

            if (typeof freezeScroll === 'boolean') {
                return { freezeScroll };
            }

            // keep the old state in case we do not receive a new one.
            return state || value;
        }
    },
    props: {
        handleClick: (view) => {
            const state = FREEZE_SCROLL_KEY?.getState(view.state);
            if (state.freezeScroll) {
                view.dispatch(view.state.tr.setMeta(FREEZE_SCROLL_KEY, { freezeScroll: false }));
            }
        },
        handleKeyDown: (view, event) => {
            const state = FREEZE_SCROLL_KEY?.getState(view.state);
            const { code } = event;

            if (code === 'Escape' && state.freezeScroll) {
                view.dispatch(view.state.tr.setMeta(FREEZE_SCROLL_KEY, { freezeScroll: false }));
            }
        }
    }
});
