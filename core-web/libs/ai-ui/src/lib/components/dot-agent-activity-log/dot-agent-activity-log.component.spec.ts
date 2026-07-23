import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';

import { DotAgentActivityLogComponent } from './dot-agent-activity-log.component';

import { AgentMessage } from '../../models/agent-message';

const MESSAGES: AgentMessage[] = [
    { id: 1, icon: 'pi pi-search', text: 'Scanning page', tone: 'info' },
    { id: 2, icon: 'pi pi-check', text: 'Fixed alt text', sub: 'image-alt · hero.vtl', tone: 'success' },
    { id: 3, icon: 'pi pi-flag', text: 'Reported contrast', tone: 'warning' }
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

    it('renders one message bubble per message', () => {
        spectator.setInput('messages', MESSAGES);
        expect(spectator.queryAll(byTestId('agent-message')).length).toBe(3);
    });

    it('passes each message through so its text renders', () => {
        spectator.setInput('messages', MESSAGES);
        const steps = spectator.queryAll(byTestId('agent-message'));
        expect(steps[1]).toHaveText('Fixed alt text');
    });

    it('renders no live bubble and no spinner when not working', () => {
        spectator.setInput({ messages: MESSAGES, working: false });
        expect(spectator.queryAll(byTestId('agent-message')).length).toBe(3);
        expect(spectator.query('.pi-spinner')).toBeNull();
    });

    it('appends one live working bubble (spinner) below the settled steps while working', () => {
        spectator.setInput({ messages: MESSAGES, working: true });
        const bubbles = spectator.queryAll(byTestId('agent-message'));
        // 3 settled + 1 appended live bubble.
        expect(bubbles.length).toBe(4);
        // Exactly one live spinner, on the appended (last) bubble.
        expect(spectator.queryAll('.pi-spinner').length).toBe(1);
        expect(bubbles[bubbles.length - 1].querySelector('.pi-spinner')).not.toBeNull();
    });

    it('renders the supplied workingMessage as the live bubble', () => {
        spectator.setInput({
            messages: MESSAGES,
            working: true,
            workingMessage: {
                id: 'agent-working',
                icon: 'pi pi-spin pi-spinner',
                text: 'Still working…',
                sub: '8s',
                tone: 'info'
            } as AgentMessage
        });
        const bubbles = spectator.queryAll(byTestId('agent-message'));
        expect(bubbles.length).toBe(4);
        expect(bubbles[bubbles.length - 1]).toHaveText('Still working…');
        expect(bubbles[bubbles.length - 1]).toHaveText('8s');
    });

    it('synthesizes a fallback working bubble when working with no workingMessage', () => {
        spectator.setInput({
            messages: [],
            working: true,
            workingMessage: null,
            workingFallbackKey: 'my.working.key'
        });
        const bubbles = spectator.queryAll(byTestId('agent-message'));
        expect(bubbles.length).toBe(1);
        expect(bubbles[0]).toHaveText('my.working.key');
        expect(bubbles[0].querySelector('.pi-spinner')).not.toBeNull();
    });
});
