import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { MarkdownModule } from 'ngx-markdown';

import { DotMessageService } from '@dotcms/data-access';
import { DOT_AI_ANSWER_STATE, DotAiChatAnswer } from '@dotcms/dotcms-models';

import DotAiChatComponent from './dot-ai-chat.component';

import { DotAiStore } from '../../store/dot-ai.store';

const answer = (overrides: Partial<DotAiChatAnswer> = {}): DotAiChatAnswer => ({
    content: '',
    state: DOT_AI_ANSWER_STATE.STREAMING,
    ...overrides
});

describe('DotAiChatComponent', () => {
    let spectator: Spectator<DotAiChatComponent>;

    const storeMock = {
        chatAnswer: jest.fn().mockReturnValue(null),
        hasAnswer: jest.fn().mockReturnValue(false),
        isStreaming: jest.fn().mockReturnValue(false),
        isConfigured: jest.fn().mockReturnValue(true),
        showNotConfigured: jest.fn().mockReturnValue(false),
        sendChat: jest.fn(),
        stopChat: jest.fn(),
        // Read by the settings panel, a real child of this component.
        indexesForbidden: jest.fn().mockReturnValue(false),
        indexOptions: jest.fn().mockReturnValue([]),
        chatModels: jest.fn().mockReturnValue([]),
        settingsIndexName: jest.fn().mockReturnValue('default'),
        settingsThreshold: jest.fn().mockReturnValue(0.75),
        settingsOperator: jest.fn().mockReturnValue('cosine'),
        settingsModel: jest.fn().mockReturnValue(''),
        settingsTemperature: jest.fn().mockReturnValue(0),
        settingsResponseLength: jest.fn().mockReturnValue(1024),
        settingsContentTypes: jest.fn().mockReturnValue(''),
        settingsSite: jest.fn().mockReturnValue(null),
        setSettings: jest.fn()
    };

    const createComponent = createComponentFactory({
        component: DotAiChatComponent,
        componentProviders: [{ provide: DotAiStore, useValue: storeMock }],
        // The real renderer, not a stub: answers are markdown and the tests below assert the
        // rendered text. MarkdownModule.forRoot() supplies MarkdownService, which the app
        // provides globally in app.config.ts.
        imports: [MarkdownModule.forRoot()],
        providers: [mockProvider(DotMessageService)],
        shallow: true
    });

    beforeEach(() => {
        jest.clearAllMocks();
        storeMock.chatAnswer.mockReturnValue(null);
        storeMock.hasAnswer.mockReturnValue(false);
        storeMock.isStreaming.mockReturnValue(false);
        storeMock.isConfigured.mockReturnValue(true);
    });

    /** ngx-markdown renders asynchronously, so the DOM lands a microtask after creation. */
    const settle = async () => {
        await spectator.fixture.whenStable();
        spectator.detectChanges();
    };

    const withAnswer = (current: DotAiChatAnswer | null) => {
        storeMock.chatAnswer.mockReturnValue(current);
        storeMock.hasAnswer.mockReturnValue(current !== null);
    };

    it('should show the empty state before any answer', () => {
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-chat-empty'))).toBeTruthy();
    });

    it('should render the current answer, with no transcript around it', async () => {
        withAnswer(answer({ content: 'hi there', state: 'complete' }));
        spectator = createComponent();
        await settle();

        expect(spectator.query(byTestId('dotai-chat-answer-text'))).toHaveText('hi there');
        // Nothing echoes the question back as a chat turn — it stays in the composer.
        expect(spectator.query(byTestId('dotai-chat-user-message'))).toBeFalsy();
    });

    it('should render the answer as markdown rather than literal syntax', async () => {
        withAnswer(
            answer({ content: '## Costa Rica\n\n- rainforests\n- **beaches**', state: 'complete' })
        );
        spectator = createComponent();
        await settle();

        const region = spectator.query(byTestId('dotai-chat-answer-text'));

        expect(region.querySelector('h2')).toHaveText('Costa Rica');
        expect(region.querySelectorAll('li')).toHaveLength(2);
        expect(region.querySelector('strong')).toHaveText('beaches');
        // The raw syntax must not survive as text.
        expect(region.textContent).not.toContain('##');
        expect(region.textContent).not.toContain('**');
    });

    it('should put the composer above the answer so each submit reads as its own request', () => {
        withAnswer(answer({ content: 'an answer', state: 'complete' }));
        spectator = createComponent();

        const composer = spectator.query(byTestId('dotai-chat-input'));
        const region = spectator.query(byTestId('dotai-chat-answer'));

        expect(composer).toBeTruthy();
        expect(region).toBeTruthy();
        expect(
            composer.compareDocumentPosition(region) & Node.DOCUMENT_POSITION_FOLLOWING
        ).toBeTruthy();
    });

    it('should show the thinking indicator only until the first delta lands', async () => {
        withAnswer(answer({ content: '' }));
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-chat-thinking'))).toBeTruthy();

        withAnswer(answer({ content: 'partial' }));
        spectator = createComponent();
        await settle();

        expect(spectator.query(byTestId('dotai-chat-thinking'))).toBeFalsy();
        expect(spectator.query(byTestId('dotai-chat-answer-text'))).toHaveText('partial');
    });

    it('should swap Send for Stop while streaming (FR-012)', () => {
        storeMock.isStreaming.mockReturnValue(true);
        withAnswer(answer());
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-chat-stop'))).toBeTruthy();
        expect(spectator.query(byTestId('dotai-chat-send'))).toBeFalsy();
    });

    it('should mark a stopped answer rather than implying it finished', async () => {
        withAnswer(answer({ content: 'partial', state: DOT_AI_ANSWER_STATE.STOPPED }));
        spectator = createComponent();
        await settle();

        expect(spectator.query(byTestId('dotai-chat-stopped'))).toBeTruthy();
        // The partial answer stays readable.
        expect(spectator.query(byTestId('dotai-chat-answer-text'))).toContainText('partial');
    });

    it('should render a failure inline, never as a dialog (FR-014)', () => {
        withAnswer(answer({ state: DOT_AI_ANSWER_STATE.ERROR, error: 'rate limited' }));
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-chat-error'))).toHaveText('rate limited');
    });

    describe('composer', () => {
        const type = (value: string) => {
            const input = spectator.query(byTestId('dotai-chat-input')) as HTMLTextAreaElement;
            spectator.typeInElement(value, input);

            return input;
        };

        it('should send on Enter and keep the question for editing (FR-011)', () => {
            spectator = createComponent();
            const input = type('a question');

            spectator.dispatchKeyboardEvent(input, 'keydown', 'Enter');

            expect(storeMock.sendChat).toHaveBeenCalledWith('a question');
        });

        it('should keep the question after sending so it can be asked again (FR-011)', () => {
            // The question lives only in the composer now, and re-asking is the usual next
            // step. Asserting the textarea's value would only measure ngModel's view-write
            // timing under jsdom; sending twice proves the draft itself survived.
            spectator = createComponent();
            const input = type('a question');

            spectator.dispatchKeyboardEvent(input, 'keydown', 'Enter');
            spectator.dispatchKeyboardEvent(input, 'keydown', 'Enter');

            expect(storeMock.sendChat).toHaveBeenCalledTimes(2);
            expect(storeMock.sendChat).toHaveBeenLastCalledWith('a question');
        });

        it('should insert a newline on Shift+Enter instead of sending', () => {
            spectator = createComponent();
            const input = type('a question');

            // Spectator's helper takes a key string, so it cannot set modifiers — dispatch
            // the real event to exercise the Shift+Enter branch.
            input.dispatchEvent(
                new KeyboardEvent('keydown', { key: 'Enter', shiftKey: true, bubbles: true })
            );
            spectator.detectChanges();

            expect(storeMock.sendChat).not.toHaveBeenCalled();
        });

        it('should not send an empty draft', () => {
            spectator = createComponent();
            const input = type('   ');

            spectator.dispatchKeyboardEvent(input, 'keydown', 'Enter');

            expect(storeMock.sendChat).not.toHaveBeenCalled();
        });

        it('should not send while unconfigured (FR-047)', () => {
            storeMock.isConfigured.mockReturnValue(false);
            spectator = createComponent();
            const input = type('a question');

            spectator.dispatchKeyboardEvent(input, 'keydown', 'Enter');

            expect(storeMock.sendChat).not.toHaveBeenCalled();
        });
    });
});
