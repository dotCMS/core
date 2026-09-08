import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { MarkdownModule } from 'ngx-markdown';

import { DotAiPromptInputComponent } from '@dotcms/ai-ui';
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
        // Real, not stubs: the answers are markdown and the composer is the shared
        // dot-ai-prompt-input, and the tests below drive both. MarkdownModule.forRoot()
        // supplies MarkdownService, which the app provides globally in app.config.ts.
        imports: [MarkdownModule.forRoot(), DotAiPromptInputComponent],
        // Echoes the key: the error line pipes through `dm`, and a server message is not a
        // key, so an echoing mock is the only one that shows both paths honestly.
        providers: [mockProvider(DotMessageService, { get: (key: string) => key })],
        shallow: true
    });

    beforeEach(() => {
        jest.clearAllMocks();
        storeMock.chatAnswer.mockReturnValue(null);
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

    it('should put the thinking indicator in the same column as the answer', async () => {
        // Both live inside the 70ch column, so the indicator starts where the text will and
        // nothing shifts sideways when one replaces the other.
        withAnswer(answer({ content: '' }));
        spectator = createComponent();

        const column = spectator.query(byTestId('dotai-chat-column'));

        expect(column).toBeTruthy();
        expect(column.contains(spectator.query(byTestId('dotai-chat-thinking')))).toBe(true);

        withAnswer(answer({ content: 'text', state: 'complete' }));
        spectator = createComponent();
        await settle();

        expect(
            spectator
                .query(byTestId('dotai-chat-column'))
                .contains(spectator.query(byTestId('dotai-chat-answer-text')))
        ).toBe(true);
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

    it('should put the answer above the composer, so focus order matches reading order', () => {
        withAnswer(answer({ content: 'an answer', state: 'complete' }));
        spectator = createComponent();

        const composer = spectator.query(byTestId('ai-prompt-input'));
        const region = spectator.query(byTestId('dotai-chat-answer'));

        expect(composer).toBeTruthy();
        expect(region).toBeTruthy();
        expect(
            region.compareDocumentPosition(composer) & Node.DOCUMENT_POSITION_FOLLOWING
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

    it('should stop generation when Stop is clicked (FR-012)', () => {
        // The behaviour is covered in the store spec; this is the template wiring, which the
        // presence assertion above does not touch.
        storeMock.isStreaming.mockReturnValue(true);
        withAnswer(answer());
        spectator = createComponent();

        spectator.click(
            spectator.query(byTestId('dotai-chat-stop'))?.querySelector('button') as HTMLElement
        );

        expect(storeMock.stopChat).toHaveBeenCalled();
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

    it('should cancel an in-flight answer when the tab is left (FR-015)', () => {
        // The store is provided on the shell, above the tab routes, so its own onDestroy does
        // not fire on a tab switch. Only the component's teardown can cover this.
        storeMock.isStreaming.mockReturnValue(true);
        withAnswer(answer());
        spectator = createComponent();

        expect(storeMock.stopChat).not.toHaveBeenCalled();

        spectator.fixture.destroy();

        expect(storeMock.stopChat).toHaveBeenCalled();
    });

    describe('composer', () => {
        // A real KeyboardEvent: spectator's helper needs initKeyboardEvent, which this jsdom
        // does not provide.
        const enter = (el: Element) =>
            el.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));

        const type = (value: string) => {
            const input = spectator.query(byTestId('ai-prompt-input')) as HTMLTextAreaElement;
            spectator.typeInElement(value, input);

            return input;
        };

        it('should send on Enter and keep the question for editing (FR-011)', () => {
            spectator = createComponent();
            const input = type('a question');

            enter(input);

            expect(storeMock.sendChat).toHaveBeenCalledWith('a question');
        });

        it('should keep the question after sending so it can be asked again (FR-011)', () => {
            // The question lives only in the composer now, and re-asking is the usual next
            // step. Asserting the textarea's value would only measure ngModel's view-write
            // timing under jsdom; sending twice proves the draft itself survived.
            spectator = createComponent();
            const input = type('a question');

            enter(input);
            enter(input);

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

            enter(input);

            expect(storeMock.sendChat).not.toHaveBeenCalled();
        });

        it('should not send while unconfigured (FR-047)', () => {
            storeMock.isConfigured.mockReturnValue(false);
            spectator = createComponent();
            const input = type('a question');

            enter(input);

            expect(storeMock.sendChat).not.toHaveBeenCalled();
        });
    });

    it('should draw no box around the composer, since dot-ai-prompt-input has one', () => {
        spectator = createComponent();
        const composer = spectator.query('dot-ai-prompt-input')?.parentElement as HTMLElement;

        expect(composer.className).not.toContain('border-t');
        expect(composer.className).not.toContain('border-surface-200');
    });

    describe('the streaming indicator on Stop', () => {
        beforeEach(() => {
            storeMock.isStreaming.mockReturnValue(true);
            withAnswer(answer());
            spectator = createComponent();
        });

        it('should spin while an answer is streaming', () => {
            const spinner = spectator.query(byTestId('dotai-chat-stop-spinner')) as HTMLElement;

            expect(spinner).toBeTruthy();
            expect(spinner.className).toContain('animate-spin');
            expect(spinner.textContent?.trim()).toBe('progress_activity');
        });

        it('should leave Stop clickable, which PrimeNG loading would not', () => {
            // `[loading]` renders `[disabled]="disabled || loading"`, so using it here would
            // grey out the only control that cancels the request (FR-012). This is the
            // assertion that catches someone swapping the hand-rolled spinner for it.
            const button = spectator
                .query(byTestId('dotai-chat-stop'))
                ?.querySelector('button') as HTMLButtonElement;

            expect(button.disabled).toBe(false);

            spectator.click(button);

            expect(storeMock.stopChat).toHaveBeenCalled();
        });

        it('should show no spinner once streaming ends', () => {
            storeMock.isStreaming.mockReturnValue(false);
            spectator = createComponent();

            expect(spectator.query(byTestId('dotai-chat-stop-spinner'))).toBeFalsy();
        });
    });
});
