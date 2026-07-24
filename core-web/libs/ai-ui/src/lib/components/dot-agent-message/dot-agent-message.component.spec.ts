import { createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotAgentMessageComponent } from './dot-agent-message.component';

import { AgentMessage } from '../../models/agent-message';

const MESSAGE: AgentMessage = {
    id: 1,
    icon: 'pi pi-check',
    text: 'Fixed alt text',
    sub: 'image-alt · hero.vtl',
    tone: 'success'
};

describe('DotAgentMessageComponent', () => {
    let spectator: Spectator<DotAgentMessageComponent>;

    const createComponent = createComponentFactory(DotAgentMessageComponent);

    beforeEach(() => {
        spectator = createComponent({ props: { message: MESSAGE } });
    });

    it('renders the message text and sub', () => {
        expect(spectator.element).toHaveText('Fixed alt text');
        expect(spectator.element).toHaveText('image-alt · hero.vtl');
    });

    it('renders the icon', () => {
        expect(spectator.query('i.pi-check')).toBeTruthy();
    });

    it('tints the icon chip by tone', () => {
        const chip = spectator.query('.rounded-lg');
        expect(chip).toHaveClass('bg-green-50');
        expect(chip).toHaveClass('text-green-600');
    });

    it('maps each tone to its chip classes', () => {
        spectator.setInput('message', { ...MESSAGE, tone: 'warning' });
        expect(spectator.query('.rounded-lg')).toHaveClass('text-orange-700');
        spectator.setInput('message', { ...MESSAGE, tone: 'info' });
        expect(spectator.query('.rounded-lg')).toHaveClass('text-primary');
        spectator.setInput('message', { ...MESSAGE, tone: 'danger' });
        expect(spectator.query('.rounded-lg')).toHaveClass('text-red-600');
    });

    it('omits the sub-line when absent', () => {
        spectator.setInput('message', { id: 2, icon: 'pi pi-search', text: 'Scanning', tone: 'info' });
        expect(spectator.element).not.toHaveText('·');
    });

    it('hides the connector on the last bubble and shows it otherwise', () => {
        // Default: standalone/last → no connector.
        expect(spectator.query('.w-0\\.5')).toBeNull();
        spectator.setInput('last', false);
        expect(spectator.query('.w-0\\.5')).not.toBeNull();
    });
});
