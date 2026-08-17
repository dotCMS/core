import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotAgentThinkingComponent } from './dot-agent-thinking.component';

describe('DotAgentThinkingComponent', () => {
    let spectator: Spectator<DotAgentThinkingComponent>;

    const createComponent = createComponentFactory(DotAgentThinkingComponent);

    beforeEach(() => {
        spectator = createComponent({ props: { text: 'Thinking…' } });
    });

    it('renders a spinner + gradient-shimmer label', () => {
        // Spinner is the clear motion cue…
        expect(spectator.query(byTestId('agent-thinking-spinner'))).toBeTruthy();
        // …and the label carries the shimmer class (styling is component CSS).
        expect(spectator.query(byTestId('agent-thinking-text'))).toBeTruthy();
    });

    it('renders the primary text in the shimmer label', () => {
        expect(spectator.query(byTestId('agent-thinking-text'))).toHaveText('Thinking…');
    });

    it('renders the sub-line when provided, omits it otherwise', () => {
        expect(spectator.element).not.toHaveText('12s');
        spectator.setInput('sub', '12s');
        expect(spectator.element).toHaveText('12s');
    });
});
