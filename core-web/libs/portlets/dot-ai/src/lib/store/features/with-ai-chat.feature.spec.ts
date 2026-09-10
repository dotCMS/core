import { signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { Observable, Subject, throwError } from 'rxjs';

import { createEnvironmentInjector, EnvironmentInjector } from '@angular/core';

import {
    DotAiCompletionsStreamService,
    DotAiStreamEvent,
    DotHttpErrorManagerService
} from '@dotcms/data-access';
import { DOT_AI_ANSWER_STATE } from '@dotcms/dotcms-models';

import { withAiChat } from './with-ai-chat.feature';
import { withRetrievalSettings } from './with-retrieval-settings.feature';

import { DOT_AI_INITIAL_STATE, DotAiPortletState } from '../../models/dot-ai-portlet.models';

const TestStore = signalStore(
    { providedIn: 'root' },
    withState<DotAiPortletState>(DOT_AI_INITIAL_STATE),
    withRetrievalSettings(),
    withAiChat()
);

describe('withAiChat', () => {
    let spectator: SpectatorService<InstanceType<typeof TestStore>>;
    let store: InstanceType<typeof TestStore>;
    let stream$: Subject<DotAiStreamEvent>;

    const createService = createServiceFactory({
        service: TestStore,
        providers: [
            mockProvider(DotAiCompletionsStreamService),
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    /** Deltas are coalesced on a timer, so tests drive the clock rather than wait on it. */
    const flushDeltas = () => jest.advanceTimersByTime(100);

    beforeEach(() => {
        jest.useFakeTimers();
        spectator = createService();
        store = spectator.service;
        stream$ = new Subject<DotAiStreamEvent>();
        spectator.inject(DotAiCompletionsStreamService).stream = jest.fn().mockReturnValue(stream$);
    });

    afterEach(() => jest.useRealTimers());

    const assistant = () => store.chatAnswer();

    it('should open an empty streaming answer', () => {
        store.sendChat('what is dotCMS');

        expect(store.chatAnswer()).toEqual({
            content: '',
            state: DOT_AI_ANSWER_STATE.STREAMING
        });
    });

    it('should concatenate deltas in order', () => {
        store.sendChat('q');

        stream$.next({ type: 'delta', content: 'Hello' });
        stream$.next({ type: 'delta', content: ' world' });
        flushDeltas();

        expect(assistant()?.content).toBe('Hello world');
    });

    it('should complete the turn when the stream ends', () => {
        // No flush here on purpose: completing must land the buffered text itself, or the tail
        // of every answer would be dropped.
        store.sendChat('q');
        stream$.next({ type: 'delta', content: 'done' });
        stream$.complete();

        expect(assistant()?.content).toBe('done');

        expect(assistant()?.state).toBe(DOT_AI_ANSWER_STATE.COMPLETE);
        expect(store.isStreaming()).toBe(false);
    });

    it('should leave no armed flush timer behind after the turn ends', () => {
        // `flushDeltas` used to null the handle without clearing it, so a flush landed
        // directly from `finish` left a timer nothing could reach. Harmless only because the
        // late tick found an empty buffer — one line's change away from resurrecting an
        // answer after a stop.
        store.sendChat('q');
        stream$.next({ type: 'delta', content: 'done' });
        stream$.complete();

        expect(jest.getTimerCount()).toBe(0);
    });

    it('should send the shared retrieval payload with stream enabled', () => {
        const service = spectator.inject(DotAiCompletionsStreamService);
        store.setSettings({ settingsIndexName: 'blogs' });

        store.sendChat('q');

        expect(service.stream).toHaveBeenCalledWith(
            expect.objectContaining({ prompt: 'q', indexName: 'blogs', stream: true })
        );
    });

    it('should ignore an empty prompt', () => {
        store.sendChat('   ');

        expect(store.chatAnswer()).toBeNull();
        expect(spectator.inject(DotAiCompletionsStreamService).stream).not.toHaveBeenCalled();
    });

    it('should abort an in-flight stream when the store itself is destroyed (FR-015)', () => {
        // The backstop for the whole portlet unmounting. `{ providedIn: 'root' }` never fires
        // onDestroy, so this needs a scoped store in a child injector that can be destroyed —
        // the pattern a11y-run.store.spec.ts uses for the same reason.
        const teardown = jest.fn();
        const ScopedStore = signalStore(
            withState<DotAiPortletState>(DOT_AI_INITIAL_STATE),
            withRetrievalSettings(),
            withAiChat()
        );

        const parent = spectator.inject(EnvironmentInjector);
        const injector = createEnvironmentInjector([ScopedStore], parent);
        const store = injector.get(ScopedStore);

        // The child injector resolves the stream service from the parent, so this is the
        // same mock instance the rest of the suite drives.
        spectator.inject(DotAiCompletionsStreamService).stream = jest
            .fn()
            .mockReturnValue(new Observable(() => teardown));

        store.sendChat('q');
        expect(teardown).not.toHaveBeenCalled();

        injector.destroy();

        expect(teardown).toHaveBeenCalled();
    });

    describe('stop (FR-012)', () => {
        it('should halt generation and keep the partial answer', () => {
            store.sendChat('q');
            stream$.next({ type: 'delta', content: 'partial' });

            store.stopChat();

            expect(assistant()).toMatchObject({
                content: 'partial',
                state: DOT_AI_ANSWER_STATE.STOPPED
            });
            expect(store.isStreaming()).toBe(false);
        });

        it('should ignore deltas that arrive after the stop', () => {
            store.sendChat('q');
            stream$.next({ type: 'delta', content: 'partial' });
            store.stopChat();

            stream$.next({ type: 'delta', content: ' MORE' });
            flushDeltas();

            expect(assistant()?.content).toBe('partial');
        });
    });

    it('should replace the previous answer when a second question is sent (FR-013)', () => {
        const second$ = new Subject<DotAiStreamEvent>();
        const service = spectator.inject(DotAiCompletionsStreamService);

        store.sendChat('first');
        stream$.next({ type: 'delta', content: 'one' });

        service.stream = jest.fn().mockReturnValue(second$);
        store.sendChat('second');

        // The first stream is unsubscribed, so a late delta cannot bleed into the new turn.
        stream$.next({ type: 'delta', content: ' LATE' });
        second$.next({ type: 'delta', content: 'two' });
        flushDeltas();

        // Replaced, not appended: there is only ever one answer on screen.
        expect(assistant()?.content).toBe('two');
    });

    describe('errors (FR-014)', () => {
        it('should render an in-band error inline and not call the error manager', () => {
            store.sendChat('q');

            stream$.next({ type: 'error', message: 'rate limited' });

            expect(assistant()).toMatchObject({
                state: DOT_AI_ANSWER_STATE.ERROR,
                error: 'rate limited'
            });
            expect(spectator.inject(DotHttpErrorManagerService).handle).not.toHaveBeenCalled();
        });

        it('should render a transport failure inline too, never as a dialog', () => {
            spectator.inject(DotAiCompletionsStreamService).stream = jest
                .fn()
                .mockReturnValue(throwError(() => new Error('boom')));

            store.sendChat('q');

            expect(assistant()?.state).toBe(DOT_AI_ANSWER_STATE.ERROR);
            expect(spectator.inject(DotHttpErrorManagerService).handle).not.toHaveBeenCalled();
            expect(store.isStreaming()).toBe(false);
        });
    });
});
