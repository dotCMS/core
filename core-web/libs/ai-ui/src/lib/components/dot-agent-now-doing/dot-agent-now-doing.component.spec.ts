import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';

import { DotAgentNowDoingComponent } from './dot-agent-now-doing.component';

import { AgentMessage } from '../../models/agent-message';

const ACTIVE: AgentMessage = { id: 9, icon: 'pi pi-wrench', text: 'Fixing .btn', tone: 'info' };

describe('DotAgentNowDoingComponent', () => {
    let spectator: Spectator<DotAgentNowDoingComponent>;

    const createComponent = createComponentFactory({
        component: DotAgentNowDoingComponent,
        providers: [mockProvider(DotMessageService, { get: (key: string) => key })]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('shows the active message text', () => {
        spectator.setInput('message', ACTIVE);
        expect(spectator.element).toHaveText('Fixing .btn');
    });

    it('falls back to the i18n key when there is no active message', () => {
        spectator.setInput({ message: null, fallbackKey: 'my.working.key' });
        expect(spectator.element).toHaveText('my.working.key');
    });

    it('renders a spinner', () => {
        expect(spectator.query('i.pi-spinner')).toBeTruthy();
    });
});
