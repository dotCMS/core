import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotAiPromptInputComponent } from './dot-ai-prompt-input.component';

describe('DotAiPromptInputComponent', () => {
    let spectator: Spectator<DotAiPromptInputComponent>;

    const createComponent = createComponentFactory(DotAiPromptInputComponent);

    const textarea = () => spectator.query(byTestId('ai-prompt-input')) as HTMLTextAreaElement;

    beforeEach(() => {
        spectator = createComponent({
            props: { placeholder: 'Describe it', ariaLabel: 'Prompt' }
        });
    });

    it('should expose the placeholder and accessible name it was given', () => {
        expect(textarea().placeholder).toBe('Describe it');
        expect(textarea().getAttribute('aria-label')).toBe('Prompt');
    });

    it('should render the card border on the wrapper, not the textarea', () => {
        // The design is one box: a bordered card around a borderless field. If the textarea
        // kept its own border you would see a box within a box.
        const styles = textarea().className;

        expect(styles).toContain('border-0!');
        expect(spectator.query('div.rounded-lg')).toBeTruthy();
    });

    it('should not clip its own content, so a projected overlay can escape the card', () => {
        // overflow-hidden here previously cropped a projected select's dropdown.
        const card = spectator.query('div.rounded-lg') as HTMLElement;

        expect(card.className).not.toContain('overflow-hidden');
    });

    it('should emit typed text', () => {
        const output = jest.fn();
        spectator.output('valueChange').subscribe(output);

        spectator.typeInElement('a prompt', textarea());

        expect(output).toHaveBeenCalledWith('a prompt');
    });

    it('should submit on Enter', () => {
        const output = jest.fn();
        spectator.output('submitted').subscribe(output);

        textarea().dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));

        expect(output).toHaveBeenCalledTimes(1);
    });

    it('should insert a newline on Shift+Enter instead of submitting', () => {
        const output = jest.fn();
        spectator.output('submitted').subscribe(output);

        textarea().dispatchEvent(
            new KeyboardEvent('keydown', { key: 'Enter', shiftKey: true, bubbles: true })
        );

        expect(output).not.toHaveBeenCalled();
    });

    it('should disable the field when asked', () => {
        spectator.setInput('disabled', true);

        expect(textarea().disabled).toBe(true);
    });
});
