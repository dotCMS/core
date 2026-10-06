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
 * How long deltas accumulate before landing in the store. Fast enough to read as continuous,
 * slow enough that a long answer is parsed a few times a second rather than once per token.
 */
const DELTA_FLUSH_MS = 80;

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
            isStreaming: computed(() => store.chatStreaming())
        })),
        withMethods((store) => {
            const streamService = inject(DotAiCompletionsStreamService);
            const slot = new SubscriptionSlot();

            /** Buffered deltas, and the handle of the flush they are waiting on. */
            let pending = '';
            let flushHandle: ReturnType<typeof setTimeout> | null = null;

            /**
             * Rewrites the current answer.
             *
             * Only while streaming: after a stop, late frames from a stream still winding down
             * must not resurrect the answer.
             */
            const patchAnswer = (change: Partial<DotAiChatAnswer>) => {
                const current = store.chatAnswer();

                if (!current || current.state !== DOT_AI_ANSWER_STATE.STREAMING) {
                    return;
                }

                patchState(store, { chatAnswer: { ...current, ...change } });
            };

            const clearFlushTimer = () => {
                if (flushHandle) {
                    clearTimeout(flushHandle);
                    flushHandle = null;
                }
            };

            /**
             * Lands buffered deltas now.
             *
             * The timer is cleared *here* rather than by the caller, because this runs both as
             * the timer callback and directly from `finish`. Nulling the handle without
             * clearing it left the direct case with an armed timer no later `clearTimeout`
             * could reach — harmless only by accident, since the late tick found an empty
             * buffer.
             */
            const flushDeltas = () => {
                clearFlushTimer();

                if (!pending) {
                    return;
                }

                const text = pending;
                pending = '';

                patchAnswer({ content: (store.chatAnswer()?.content ?? '') + text });
            };

            /** Drops buffered deltas without writing them. */
            const discardDeltas = () => {
                clearFlushTimer();
                pending = '';
            };

            const finish = (state: DotAiAnswerState, error?: string) => {
                // Land whatever is buffered before the state change closes patchAnswer's guard.
                flushDeltas();

                patchAnswer({ state, ...(error ? { error } : {}) });
                patchState(store, { chatStreaming: false });
            };

            return {
                sendChat(prompt: string): void {
                    const trimmed = prompt.trim();

                    if (!trimmed) {
                        return;
                    }

                    discardDeltas();

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

                                    // Deltas are coalesced rather than written per frame: the
                                    // answer renders through ngx-markdown, which re-parses and
                                    // re-sanitises the whole string on every change. Writing per
                                    // token makes that O(n^2) in answer length and wipes any text
                                    // selection each frame.
                                    pending += event.content;
                                    flushHandle ??= setTimeout(flushDeltas, DELTA_FLUSH_MS);
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
                }
            };
        }),
        withHooks({
            onDestroy(store) {
                // Backstop for the whole portlet unmounting. This is NOT what satisfies FR-015
                // on a tab switch: the store is provided on the shell, so this hook only fires
                // when /dotai itself is left. DotAiChatComponent cancels on its own teardown.
                store.stopChat();
            }
        })
    );
}
