import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { ApplicationRef } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';

import { DotAgentActivityLogComponent } from './dot-agent-activity-log.component';

import { AgentMessage } from '../../models/agent-message';

const MESSAGES: AgentMessage[] = [
    { id: 1, icon: 'search', text: 'Scanning page', tone: 'info' },
    {
        id: 2,
        icon: 'check',
        text: 'Fixed alt text',
        sub: 'image-alt · hero.vtl',
        tone: 'success'
    },
    { id: 3, icon: 'flag', text: 'Reported contrast', tone: 'warning' }
];

describe('DotAgentActivityLogComponent', () => {
    let spectator: Spectator<DotAgentActivityLogComponent>;

    const createComponent = createComponentFactory({
        component: DotAgentActivityLogComponent,
        providers: [mockProvider(DotMessageService, { get: (key: string) => key })]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    describe('auto-scroll', () => {
        /**
         * Make the host itself the scroller with the given geometry, so the component's
         * `scrollParent` walk stops here. `scrollTop` is a real property on the element,
         * so the assertions read back whatever the component wrote (or didn't).
         */
        /** Run the component's `afterRenderEffect` — detectChanges alone does not. */
        function flushRender() {
            TestBed.inject(ApplicationRef).tick();
        }

        function makeScrollable({ scrollTop }: { scrollTop: number }) {
            const host = spectator.element as HTMLElement;
            // jsdom does no layout: it reports `overflowY` as undefined and both scroll
            // dimensions as 0, so the component's `scrollParent` walk would never find a
            // scroller. Stub the three things that walk reads.
            jest.spyOn(window, 'getComputedStyle').mockImplementation(
                (el) =>
                    (el === host
                        ? { overflowY: 'auto' }
                        : { overflowY: 'visible' }) as CSSStyleDeclaration
            );
            Object.defineProperty(host, 'scrollHeight', { value: 1000, configurable: true });
            Object.defineProperty(host, 'clientHeight', { value: 400, configurable: true });
            host.scrollTop = scrollTop;

            return host;
        }

        afterEach(() => {
            jest.restoreAllMocks();
        });

        it('pins to the bottom while the user is following along', () => {
            const host = makeScrollable({ scrollTop: 600 }); // 1000 - 600 - 400 = 0 from bottom
            spectator.setInput('messages', MESSAGES);
            flushRender();

            expect(host.scrollTop).toBe(1000);
        });

        it('leaves the scroll alone once the user has scrolled up', () => {
            // Scrolling up mid-run is an explicit "leave me here". The scroller is the
            // whole surrounding pane in real consumers, so yanking it back made the score
            // ring unreadable for the entire run.
            const host = makeScrollable({ scrollTop: 100 });
            spectator.setInput('messages', MESSAGES);
            flushRender();

            expect(host.scrollTop).toBe(100);
        });

        it('still pins when only a few px from the bottom (sub-pixel tolerance)', () => {
            const host = makeScrollable({ scrollTop: 580 }); // 20px from the bottom
            spectator.setInput('messages', MESSAGES);
            flushRender();

            expect(host.scrollTop).toBe(1000);
        });

        it('does not re-pin for a working-text change alone', () => {
            // `workingText` ticks every few seconds on a heartbeat with no new content;
            // tracking it was what dragged the pane back roughly every 5s.
            const host = makeScrollable({ scrollTop: 100 });
            spectator.setInput({ messages: MESSAGES, working: true });
            flushRender();
            // Re-pin so the only thing changing afterwards is the working text.
            host.scrollTop = 100;

            spectator.setInput('workingMessage', { id: 9, icon: '', text: 'tick 1' });
            flushRender();
            spectator.setInput('workingMessage', { id: 9, icon: '', text: 'tick 2' });
            flushRender();

            expect(host.scrollTop).toBe(100);
        });
    });

    it('renders one message bubble per message', () => {
        spectator.setInput('messages', MESSAGES);
        expect(spectator.queryAll(byTestId('agent-message')).length).toBe(3);
    });

    it('passes each message through so its text renders', () => {
        spectator.setInput('messages', MESSAGES);
        const steps = spectator.queryAll(byTestId('agent-message'));
        expect(steps[1]).toHaveText('Fixed alt text');
    });

    it('renders no thinking indicator when not working', () => {
        spectator.setInput({ messages: MESSAGES, working: false });
        expect(spectator.queryAll(byTestId('agent-message')).length).toBe(3);
        expect(spectator.query(byTestId('agent-thinking'))).toBeNull();
    });

    it('shows the thinking indicator (separate from the settled steps) while working', () => {
        spectator.setInput({ messages: MESSAGES, working: true });
        // Settled steps stay as message bubbles; the thinking item is its own node.
        expect(spectator.queryAll(byTestId('agent-message')).length).toBe(3);
        const thinking = spectator.query(byTestId('agent-thinking'));
        expect(thinking).not.toBeNull();
        expect(thinking?.querySelector('[data-testid="agent-thinking-text"]')).not.toBeNull();
    });

    it('renders the supplied workingMessage text + sub in the thinking indicator', () => {
        spectator.setInput({
            messages: MESSAGES,
            working: true,
            workingMessage: {
                id: 'agent-working',
                icon: '',
                text: 'Still working…',
                sub: '8s',
                tone: 'info'
            } as AgentMessage
        });
        const thinking = spectator.query(byTestId('agent-thinking'));
        expect(thinking).toHaveText('Still working…');
        expect(thinking).toHaveText('8s');
    });

    it('falls back to the working key in the thinking indicator when no workingMessage', () => {
        spectator.setInput({
            messages: [],
            working: true,
            workingMessage: null,
            workingFallbackKey: 'my.working.key'
        });
        expect(spectator.queryAll(byTestId('agent-message')).length).toBe(0);
        const thinking = spectator.query(byTestId('agent-thinking'));
        expect(thinking).toHaveText('my.working.key');
        expect(thinking?.querySelector('[data-testid="agent-thinking-text"]')).not.toBeNull();
    });
});
