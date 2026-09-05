import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withHooks,
    withMethods
} from '@ngrx/signals';

import { computed, inject, Signal } from '@angular/core';

import { DotAiCompletionsStreamService, DotAiStreamEvent } from '@dotcms/data-access';
import {
    DOT_AI_CHAT_MESSAGE_STATE,
    DotAiChatMessage,
    DotAiChatMessageState,
    DotAiRetrievalPayload
} from '@dotcms/dotcms-models';
import { SubscriptionSlot } from '@dotcms/store';

import { DotAiPortletState } from '../../models/dot-ai-portlet.models';

/**
 * Chat: a streamed answer that can be stopped mid-flight.
 *
 * Deliberately **not** an `rxMethod`. Stop has to abort the underlying `fetch`, and the only
 * thing that does that is unsubscribing, so the subscription is held explicitly in a
 * `SubscriptionSlot` — which also gives FR-013 for free, since taking the slot cancels
 * whatever was in it.
 *
 * Errors render **inline** rather than through `DotHttpErrorManagerService` (FR-014): a modal
 * thrown over an answer the user is watching stream is the wrong shape for the failure, and
 * every stream failure is recoverable by just asking again. Same reasoning, and the same
 * precedent, as `runError` in the a11y run store.
 */
export function withAiChat() {
    return signalStoreFeature(
        type<{
            state: DotAiPortletState;
            props: { retrievalPayload: Signal<DotAiRetrievalPayload> };
        }>(),
        withComputed((store) => ({
            isStreaming: computed(() => store.chatStreaming()),
            hasChat: computed(() => store.chatMessages().length > 0)
        })),
        withMethods((store) => {
            const streamService = inject(DotAiCompletionsStreamService);
            const slot = new SubscriptionSlot();

            /** Rewrites the trailing assistant turn; every stream event lands through here. */
            const patchAssistant = (
                change: Partial<DotAiChatMessage>,
                onlyWhileStreaming = true
            ) => {
                const messages = store.chatMessages();
                const index = messages.map((m) => m.role).lastIndexOf('assistant');

                if (index === -1) {
                    return;
                }

                const current = messages[index];

                // After a stop, late frames from a stream still winding down must not
                // resurrect the turn.
                if (onlyWhileStreaming && current.state !== DOT_AI_CHAT_MESSAGE_STATE.STREAMING) {
                    return;
                }

                const next = [...messages];
                next[index] = { ...current, ...change };

                patchState(store, { chatMessages: next });
            };

            const finish = (state: DotAiChatMessageState, error?: string) => {
                patchAssistant({ state, ...(error ? { error } : {}) });
                patchState(store, { chatStreaming: false });
            };

            return {
                sendChat(prompt: string): void {
                    const trimmed = prompt.trim();

                    if (!trimmed) {
                        return;
                    }

                    const stamp = Date.now();
                    const assistantId = `assistant-${stamp}`;

                    patchState(store, {
                        chatMessages: [
                            ...store.chatMessages(),
                            {
                                id: `user-${stamp}`,
                                role: 'user',
                                content: trimmed,
                                state: DOT_AI_CHAT_MESSAGE_STATE.COMPLETE
                            },
                            {
                                id: assistantId,
                                role: 'assistant',
                                content: '',
                                state: DOT_AI_CHAT_MESSAGE_STATE.STREAMING
                            }
                        ],
                        chatStreaming: true
                    });

                    // Taking the slot cancels any earlier stream, so a late delta from an
                    // abandoned turn cannot bleed into this one (FR-013).
                    slot.set(
                        streamService
                            .stream({ ...store.retrievalPayload(), prompt: trimmed, stream: true })
                            .subscribe({
                                next: (event: DotAiStreamEvent) => {
                                    if (event.type === 'error') {
                                        finish(DOT_AI_CHAT_MESSAGE_STATE.ERROR, event.message);

                                        return;
                                    }

                                    const messages = store.chatMessages();
                                    const index = messages
                                        .map((m) => m.role)
                                        .lastIndexOf('assistant');

                                    if (index === -1) {
                                        return;
                                    }

                                    patchAssistant({
                                        content: messages[index].content + event.content
                                    });
                                },
                                error: (error: unknown) =>
                                    finish(
                                        DOT_AI_CHAT_MESSAGE_STATE.ERROR,
                                        error instanceof Error ? error.message : String(error)
                                    ),
                                complete: () => finish(DOT_AI_CHAT_MESSAGE_STATE.COMPLETE)
                            })
                    );
                },

                /** Stops generation. Unsubscribing is what aborts the fetch (FR-012). */
                stopChat(): void {
                    slot.cancel();
                    finish(DOT_AI_CHAT_MESSAGE_STATE.STOPPED);
                },

                clearChat(): void {
                    slot.cancel();
                    patchState(store, { chatMessages: [], chatStreaming: false });
                }
            };
        }),
        withHooks({
            onDestroy(store) {
                // Leaving the screen cancels an in-flight answer rather than leaving an open
                // stream running unseen (FR-015).
                store.stopChat();
            }
        })
    );
}
