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
        expect(thinking?.querySelector('.agent-shimmer')).not.toBeNull();
    });

    it('renders the supplied workingMessage text + sub in the thinking indicator', () => {
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
        expect(thinking?.querySelector('.agent-shimmer')).not.toBeNull();
    });
});
