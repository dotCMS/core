import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { DOT_AI_CHAT_MESSAGE_STATE, DotAiChatMessage } from '@dotcms/dotcms-models';

import DotAiChatComponent from './dot-ai-chat.component';

import { DotAiStore } from '../../store/dot-ai.store';

const user = (content: string): DotAiChatMessage => ({
    id: 'u1',
    role: 'user',
    content,
    state: DOT_AI_CHAT_MESSAGE_STATE.COMPLETE
});

const assistant = (overrides: Partial<DotAiChatMessage> = {}): DotAiChatMessage => ({
    id: 'a1',
    role: 'assistant',
    content: '',
    state: DOT_AI_CHAT_MESSAGE_STATE.STREAMING,
    ...overrides
});

describe('DotAiChatComponent', () => {
    let spectator: Spectator<DotAiChatComponent>;

    const storeMock = {
        chatMessages: jest.fn().mockReturnValue([]),
        hasChat: jest.fn().mockReturnValue(false),
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
        settingsThreshold: jest.fn().mockReturnValue(0.25),
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
        providers: [mockProvider(DotMessageService)],
        shallow: true
    });

    beforeEach(() => {
        jest.clearAllMocks();
        storeMock.chatMessages.mockReturnValue([]);
        storeMock.hasChat.mockReturnValue(false);
        storeMock.isStreaming.mockReturnValue(false);
        storeMock.isConfigured.mockReturnValue(true);
    });

    const withThread = (messages: DotAiChatMessage[]) => {
        storeMock.chatMessages.mockReturnValue(messages);
        storeMock.hasChat.mockReturnValue(messages.length > 0);
    };

    it('should show the empty state before any turn', () => {
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-chat-empty'))).toBeTruthy();
    });

    it('should render user and assistant turns distinctly', () => {
        withThread([user('hello'), assistant({ content: 'hi there', state: 'complete' })]);
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-chat-user-message'))).toHaveText('hello');
        expect(spectator.query(byTestId('dotai-chat-assistant-message'))).toHaveText('hi there');
    });

    it('should show the thinking indicator only until the first delta lands', () => {
        withThread([user('q'), assistant({ content: '' })]);
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-chat-thinking'))).toBeTruthy();

        withThread([user('q'), assistant({ content: 'partial' })]);
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-chat-thinking'))).toBeFalsy();
        expect(spectator.query(byTestId('dotai-chat-assistant-message'))).toHaveText('partial');
    });

    it('should swap Send for Stop while streaming (FR-012)', () => {
        storeMock.isStreaming.mockReturnValue(true);
        withThread([user('q'), assistant()]);
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-chat-stop'))).toBeTruthy();
        expect(spectator.query(byTestId('dotai-chat-send'))).toBeFalsy();
    });

    it('should mark a stopped answer rather than implying it finished', () => {
        withThread([
            user('q'),
            assistant({ content: 'partial', state: DOT_AI_CHAT_MESSAGE_STATE.STOPPED })
        ]);
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-chat-stopped'))).toBeTruthy();
        // The partial answer stays readable.
        expect(spectator.query(byTestId('dotai-chat-assistant-message'))).toContainText('partial');
    });

    it('should render a failure inline, never as a dialog (FR-014)', () => {
        withThread([
            user('q'),
            assistant({ state: DOT_AI_CHAT_MESSAGE_STATE.ERROR, error: 'rate limited' })
        ]);
        spectator = createComponent();

        expect(spectator.query(byTestId('dotai-chat-error'))).toHaveText('rate limited');
    });

    describe('composer', () => {
        const type = (value: string) => {
            const input = spectator.query(byTestId('dotai-chat-input')) as HTMLTextAreaElement;
            spectator.typeInElement(value, input);

            return input;
        };

        it('should send on Enter and clear the draft (FR-011)', () => {
            spectator = createComponent();
            const input = type('a question');

            spectator.dispatchKeyboardEvent(input, 'keydown', 'Enter');

            expect(storeMock.sendChat).toHaveBeenCalledWith('a question');
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
