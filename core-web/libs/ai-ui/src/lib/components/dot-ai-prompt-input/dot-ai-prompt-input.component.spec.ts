import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { ApplicationRef } from '@angular/core';

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

    describe('growing with the content', () => {
        /** Runs the component's `afterRenderEffect` — detectChanges alone does not. */
        const flushRender = () => spectator.inject(ApplicationRef).tick();

        /**
         * jsdom does no layout, so `scrollHeight` and `clientHeight` are both 0 and the
         * wrap measurement can never fire. Stub the pair the component reads to describe a
         * field whose content needs `lines` rows.
         */
        const withContentLines = (lines: number) => {
            const el = textarea();
            Object.defineProperty(el, 'clientHeight', { value: 20, configurable: true });
            Object.defineProperty(el, 'scrollHeight', { value: 20 * lines, configurable: true });
        };

        it('should start as a single row, with the controls on the same line', () => {
            // `flex-1` is what shares the row; `basis-full` is what claims a line of its own.
            expect(textarea().className).toContain('flex-1');
            expect(textarea().className).not.toContain('basis-full');
        });

        it('should keep the field one row tall until it needs more', () => {
            expect(textarea().getAttribute('rows')).toBe('1');
        });

        it('should take a line of its own once the content wraps', () => {
            withContentLines(3);

            spectator.typeInElement('a prompt long enough to wrap', textarea());

            expect(textarea().className).toContain('basis-full');
            // order-first is what puts the field above the controls rather than beside them,
            // without reordering the DOM and re-projecting the slots.
            expect(textarea().className).toContain('order-first');
            expect(textarea().className).not.toContain('flex-1');
        });

        it('should expand on a newline whatever the width', () => {
            // The only signal that does not depend on layout, which is also why it is the
            // one this suite can assert without stubbing geometry.
            spectator.typeInElement('first\nsecond', textarea());

            expect(textarea().className).toContain('basis-full');
        });

        it('should write the measured height onto the field', () => {
            withContentLines(3);

            spectator.typeInElement('long', textarea());

            expect(textarea().style.height).toBe('60px');
        });

        it('should return to a single row when the prompt is cleared', () => {
            // Driven through `value` rather than the DOM, because that is the real flow: the
            // caller clears its own signal after a submit. Typing into the element directly
            // leaves the `[value]` binding believing it is still '', so setting '' again is a
            // no-op and the element keeps the old text.
            spectator.setInput('value', 'first\nsecond');
            flushRender();
            expect(textarea().className).toContain('basis-full');

            spectator.setInput('value', '');
            flushRender();

            expect(textarea().className).toContain('flex-1');
            expect(textarea().className).not.toContain('basis-full');
            // The inline height goes with it, so rows=1 governs again.
            expect(textarea().style.height).toBe('');
        });

        it('should not collapse while a shorter prompt is still in the field', () => {
            // The collapsed row is narrower, because the controls share it, so collapsing on
            // "it fits now" would wrap again immediately and flip on every keystroke.
            withContentLines(3);
            spectator.typeInElement('long enough to wrap', textarea());

            withContentLines(1);
            spectator.typeInElement('short', textarea());

            expect(textarea().className).toContain('basis-full');
        });

        it('should cap its height so a long prompt scrolls instead', () => {
            expect(textarea().className).toContain('max-h-40');
            expect(textarea().className).toContain('overflow-y-auto');
        });
    });
});
