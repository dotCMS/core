import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotAgentMessageComponent } from './dot-agent-message.component';

import { AgentMessage } from '../../models/agent-message';

const MESSAGE: AgentMessage = {
    id: 1,
    icon: 'check',
    text: 'Fixed alt text',
    sub: 'image-alt · hero.vtl',
    tone: 'success'
};

describe('DotAgentMessageComponent', () => {
    let spectator: Spectator<DotAgentMessageComponent>;

    const createComponent = createComponentFactory(DotAgentMessageComponent);

    /** The tone accent dot-color-icon resolved onto its host custom property. */
    const chipColor = () =>
        spectator
            .query(byTestId('agent-message-chip'))
            ?.getAttribute('style')
            ?.match(/--dot-color-icon-color:\s*([^;]+)/)?.[1]
            ?.trim();

    beforeEach(() => {
        spectator = createComponent({ props: { message: MESSAGE } });
    });

    it('renders the message text and sub', () => {
        expect(spectator.query(byTestId('agent-message-text'))).toHaveText('Fixed alt text');
        expect(spectator.query(byTestId('agent-message-sub'))).toHaveText('image-alt · hero.vtl');
    });

    it('renders the icon as a material symbol ligature', () => {
        const icon = spectator.query(byTestId('agent-message-icon'));
        expect(icon).toBeTruthy();
        expect(icon).toHaveText('check');
    });

    it('keeps the timeline-dot size override on the chip host', () => {
        const chip = spectator.query(byTestId('agent-message-chip'));
        expect(chip).toHaveClass('size-7.5!');
        expect(chip).toHaveClass('rounded-lg!');
    });

    it('tints the icon chip by tone', () => {
        expect(chipColor()).toBe('var(--p-green-500)');
    });

    it('maps each tone to its accent color', () => {
        spectator.setInput('message', { ...MESSAGE, tone: 'warning' });
        expect(chipColor()).toBe('var(--p-orange-500)');
        spectator.setInput('message', { ...MESSAGE, tone: 'info' });
        expect(chipColor()).toBe('var(--p-primary-500)');
        spectator.setInput('message', { ...MESSAGE, tone: 'danger' });
        expect(chipColor()).toBe('var(--p-red-500)');
    });

    it('omits the sub-line when absent', () => {
        spectator.setInput('message', {
            id: 2,
            icon: 'search',
            text: 'Scanning',
            tone: 'info'
        });
        expect(spectator.query(byTestId('agent-message-sub'))).toBeNull();
    });

    it('hides the connector on the last bubble and shows it otherwise', () => {
        // Default: standalone/last → no connector.
        expect(spectator.query(byTestId('agent-message-connector'))).toBeNull();
        spectator.setInput('last', false);
        expect(spectator.query(byTestId('agent-message-connector'))).not.toBeNull();
    });

    it('renders the settled message icon (never a spinner)', () => {
        expect(spectator.query(byTestId('agent-message-icon'))).toHaveText('check');
        expect(spectator.query(byTestId('agent-thinking-spinner'))).toBeNull();
    });
});
