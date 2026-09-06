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
    DOT_AI_ANSWER_STATE,
    DotAiAnswerState,
    DotAiChatAnswer,
    DotAiRetrievalPayload
} from '@dotcms/dotcms-models';
import { SubscriptionSlot } from '@dotcms/store';

import { DotAiPortletState } from '../../models/dot-ai-portlet.models';

/**
 * Chat: one streamed answer at a time, stoppable mid-flight.
 *
 * **Holds a single answer, not a transcript.** The completions endpoint takes one `prompt` and
 * keeps no conversation, so every submit is independent — asking again replaces the answer
 * rather than adding a turn. A running transcript would look like memory the endpoint cannot
 * provide, and the question itself stays in the composer where it can be edited and resent.
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
            hasAnswer: computed(() => store.chatAnswer() !== null)
        })),
        withMethods((store) => {
            const streamService = inject(DotAiCompletionsStreamService);
            const slot = new SubscriptionSlot();

            /** Rewrites the current answer; every stream event lands through here. */
            const patchAnswer = (change: Partial<DotAiChatAnswer>, onlyWhileStreaming = true) => {
                const current = store.chatAnswer();

                if (!current) {
                    return;
                }

                // After a stop, late frames from a stream still winding down must not
                // resurrect the answer.
                if (onlyWhileStreaming && current.state !== DOT_AI_ANSWER_STATE.STREAMING) {
                    return;
                }

                patchState(store, { chatAnswer: { ...current, ...change } });
            };

            const finish = (state: DotAiAnswerState, error?: string) => {
                patchAnswer({ state, ...(error ? { error } : {}) });
                patchState(store, { chatStreaming: false });
            };

            return {
                sendChat(prompt: string): void {
                    const trimmed = prompt.trim();

                    if (!trimmed) {
                        return;
                    }

                    // Replaces whatever was on screen. Each submit is its own request.
                    patchState(store, {
                        chatAnswer: { content: '', state: DOT_AI_ANSWER_STATE.STREAMING },
                        chatStreaming: true
                    });

                    // Taking the slot cancels any earlier stream, so a late delta from an
                    // abandoned answer cannot bleed into this one (FR-013).
                    slot.set(
                        streamService
                            .stream({ ...store.retrievalPayload(), prompt: trimmed, stream: true })
                            .subscribe({
                                next: (event: DotAiStreamEvent) => {
                                    if (event.type === 'error') {
                                        finish(DOT_AI_ANSWER_STATE.ERROR, event.message);

                                        return;
                                    }

                                    const current = store.chatAnswer();

                                    if (!current) {
                                        return;
                                    }

                                    patchAnswer({ content: current.content + event.content });
                                },
                                error: (error: unknown) =>
                                    finish(
                                        DOT_AI_ANSWER_STATE.ERROR,
                                        error instanceof Error ? error.message : String(error)
                                    ),
                                complete: () => finish(DOT_AI_ANSWER_STATE.COMPLETE)
                            })
                    );
                },

                /** Stops generation. Unsubscribing is what aborts the fetch (FR-012). */
                stopChat(): void {
                    slot.cancel();
                    finish(DOT_AI_ANSWER_STATE.STOPPED);
                },

                clearChat(): void {
                    slot.cancel();
                    patchState(store, { chatAnswer: null, chatStreaming: false });
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
