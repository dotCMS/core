import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

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

    it('hides the now-doing banner when not working', () => {
        spectator.setInput({ messages: MESSAGES, working: false });
        expect(spectator.query(byTestId('agent-now-doing'))).toBeNull();
    });

    it('shows the active message in the now-doing banner while working', () => {
        spectator.setInput({ messages: MESSAGES, working: true, activeMessage: MESSAGES[0] });
        const banner = spectator.query(byTestId('agent-now-doing'));
        expect(banner).not.toBeNull();
        expect(banner).toHaveText('Scanning page');
    });

    it('falls back to the working key when there is no active message', () => {
        spectator.setInput({
            messages: [],
            working: true,
            activeMessage: null,
            workingFallbackKey: 'my.working.key'
        });
        expect(spectator.query(byTestId('agent-now-doing'))).toHaveText('my.working.key');
    });
});
