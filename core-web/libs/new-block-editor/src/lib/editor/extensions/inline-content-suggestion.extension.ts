import { Extension } from '@tiptap/core';
import { PluginKey } from '@tiptap/pm/state';
import Suggestion, { SuggestionKeyDownProps, SuggestionProps } from '@tiptap/suggestion';

import type { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DOT_INLINE_CONTENT_NODE_NAME } from './nodes/inline-content/inline-content.extension';

import type { InlineContentSuggestionService } from '../services/inline-content-suggestion.service';

/**
 * Dedicated plugin key so this `@`-mention Suggestion plugin coexists with the slash-command
 * Suggestion plugin (which uses the default `SuggestionPluginKey`) in the same editor.
 */
export const InlineContentSuggestionPluginKey = new PluginKey('inlineContentSuggestion');

/**
 * `@`-mention extension for inline contentlet references. Mirrors the slash-command extension
 * but uses a SEPARATE {@link Suggestion} instance (`char: '@'`) and an async, debounced live
 * search owned by {@link InlineContentSuggestionService}. Selecting a result inserts a
 * `dotInlineContent` node carrying the full contentlet; the node's `renderHTML` strips it to a
 * skinny `{ identifier, languageId }` ref when the document is serialised for storage.
 *
 * Results are driven through the service (not TipTap's synchronous `items`), so `items` returns
 * an empty array — the picker list and keyboard routing come from the service signals.
 *
 * @param service Per-editor picker state (provided at the editor-component scope).
 * @param getLanguageId Active editor language, used as the inserted node's fallback `languageId`.
 */
export function createInlineContentSuggestionExtension(
    service: InlineContentSuggestionService,
    getLanguageId: () => number
) {
    return Extension.create({
        name: 'inlineContentSuggestion',

        onDestroy() {
            service.detachEditor();
        },

        addProseMirrorPlugins() {
            service.attachEditor(this.editor);
            return [
                Suggestion<DotCMSContentlet>({
                    editor: this.editor,
                    pluginKey: InlineContentSuggestionPluginKey,
                    char: '@',
                    startOfLine: false,
                    allowSpaces: false,

                    // Results are resolved asynchronously by the service; TipTap's synchronous
                    // item list is unused.
                    items: () => [],

                    command: ({ editor, range, props }) => {
                        editor
                            .chain()
                            .focus()
                            .deleteRange(range)
                            .insertContent({
                                type: DOT_INLINE_CONTENT_NODE_NAME,
                                attrs: {
                                    // Full contentlet at runtime; the JSON-strip helper reduces it
                                    // to { identifier, languageId } when serialised for storage.
                                    data: {
                                        ...props,
                                        languageId:
                                            (props as { languageId?: number }).languageId ??
                                            getLanguageId()
                                    }
                                }
                            })
                            .run();
                    },

                    render: () => ({
                        onStart: (props: SuggestionProps<DotCMSContentlet>) => {
                            service.open(props.query, props.clientRect ?? null, props.command);
                        },
                        onUpdate: (props: SuggestionProps<DotCMSContentlet>) => {
                            service.update(props.query, props.clientRect ?? null, props.command);
                        },
                        onExit: (props: SuggestionProps<DotCMSContentlet>) => {
                            // Suggestion fires onExit for moved-and-changed transitions while the
                            // match is still active; only tear down once the plugin deactivated.
                            const state = InlineContentSuggestionPluginKey.getState(
                                props.editor.state
                            );
                            if (state?.active) {
                                return;
                            }
                            service.close();
                        },
                        onKeyDown: ({ event }: SuggestionKeyDownProps) =>
                            service.handleKeyDown(event)
                    })
                })
            ];
        }
    });
}
