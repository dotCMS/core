import { signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { Subject, throwError } from 'rxjs';

import {
    DotAiCompletionsStreamService,
    DotAiStreamEvent,
    DotHttpErrorManagerService
} from '@dotcms/data-access';
import { DOT_AI_CHAT_MESSAGE_STATE } from '@dotcms/dotcms-models';

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

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        stream$ = new Subject<DotAiStreamEvent>();
        spectator.inject(DotAiCompletionsStreamService).stream = jest.fn().mockReturnValue(stream$);
    });

    const assistant = () =>
        store
            .chatMessages()
            .filter((m) => m.role === 'assistant')
            .at(-1);

    it('should append the user turn and an empty streaming assistant turn', () => {
        store.sendChat('what is dotCMS');

        expect(store.chatMessages()).toHaveLength(2);
        expect(store.chatMessages()[0]).toMatchObject({ role: 'user', content: 'what is dotCMS' });
        expect(assistant()).toMatchObject({
            role: 'assistant',
            content: '',
            state: DOT_AI_CHAT_MESSAGE_STATE.STREAMING
        });
    });

    it('should concatenate deltas in order', () => {
        store.sendChat('q');

        stream$.next({ type: 'delta', content: 'Hello' });
        stream$.next({ type: 'delta', content: ' world' });

        expect(assistant()?.content).toBe('Hello world');
    });

    it('should complete the turn when the stream ends', () => {
        store.sendChat('q');
        stream$.next({ type: 'delta', content: 'done' });
        stream$.complete();

        expect(assistant()?.state).toBe(DOT_AI_CHAT_MESSAGE_STATE.COMPLETE);
        expect(store.isStreaming()).toBe(false);
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

        expect(store.chatMessages()).toHaveLength(0);
        expect(spectator.inject(DotAiCompletionsStreamService).stream).not.toHaveBeenCalled();
    });

    describe('stop (FR-012)', () => {
        it('should halt generation and keep the partial answer', () => {
            store.sendChat('q');
            stream$.next({ type: 'delta', content: 'partial' });

            store.stopChat();

            expect(assistant()).toMatchObject({
                content: 'partial',
                state: DOT_AI_CHAT_MESSAGE_STATE.STOPPED
            });
            expect(store.isStreaming()).toBe(false);
        });

        it('should ignore deltas that arrive after the stop', () => {
            store.sendChat('q');
            stream$.next({ type: 'delta', content: 'partial' });
            store.stopChat();

            stream$.next({ type: 'delta', content: ' MORE' });

            expect(assistant()?.content).toBe('partial');
        });
    });

    it('should abandon the earlier turn when a second question is sent (FR-013)', () => {
        const second$ = new Subject<DotAiStreamEvent>();
        const service = spectator.inject(DotAiCompletionsStreamService);

        store.sendChat('first');
        stream$.next({ type: 'delta', content: 'one' });

        service.stream = jest.fn().mockReturnValue(second$);
        store.sendChat('second');

        // The first stream is unsubscribed, so a late delta cannot bleed into the new turn.
        stream$.next({ type: 'delta', content: ' LATE' });
        second$.next({ type: 'delta', content: 'two' });

        expect(assistant()?.content).toBe('two');
        expect(store.chatMessages().filter((m) => m.role === 'assistant')).toHaveLength(2);
    });

    describe('errors (FR-014)', () => {
        it('should render an in-band error inline and not call the error manager', () => {
            store.sendChat('q');

            stream$.next({ type: 'error', message: 'rate limited' });

            expect(assistant()).toMatchObject({
                state: DOT_AI_CHAT_MESSAGE_STATE.ERROR,
                error: 'rate limited'
            });
            expect(spectator.inject(DotHttpErrorManagerService).handle).not.toHaveBeenCalled();
        });

        it('should render a transport failure inline too, never as a dialog', () => {
            spectator.inject(DotAiCompletionsStreamService).stream = jest
                .fn()
                .mockReturnValue(throwError(() => new Error('boom')));

            store.sendChat('q');

            expect(assistant()?.state).toBe(DOT_AI_CHAT_MESSAGE_STATE.ERROR);
            expect(spectator.inject(DotHttpErrorManagerService).handle).not.toHaveBeenCalled();
            expect(store.isStreaming()).toBe(false);
        });
    });
});
