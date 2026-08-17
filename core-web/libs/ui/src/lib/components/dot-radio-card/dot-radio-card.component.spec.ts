import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { Component, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { disabled, form, FormField } from '@angular/forms/signals';

import { DotRadioCardComponent } from './dot-radio-card.component';

const OPTIONS = ['REACH_PAGE', 'BOUNCE_RATE'] as const;

describe('DotRadioCardComponent', () => {
    describe('on its own', () => {
        let spectator: Spectator<DotRadioCardComponent>;

        const createComponent = createComponentFactory({
            component: DotRadioCardComponent,
            detectChanges: false
        });

        const mount = (props: Record<string, unknown> = {}) => {
            spectator = createComponent({
                props: {
                    option: 'REACH_PAGE',
                    label: 'Reach a page',
                    description: 'A visitor lands on a page',
                    ...props
                }
            });
            spectator.detectChanges();
        };

        const host = () => spectator.element;
        const surface = () => spectator.query(byTestId('radio-card-surface')) as HTMLElement;

        // The model's `valueChange` output is reached through the model itself: Spectator's
        // `output()` helper only knows `@Output`s and `output()`s.
        const watchPicks = () => {
            const picked = jest.fn();
            spectator.component.value.subscribe(picked);

            return picked;
        };

        // Spectator's keyboard helper builds the event with the removed `initKeyboardEvent` API.
        const pressKey = (key: string) => {
            host().dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true }));
            spectator.detectChanges();
        };

        it('should render the label and the description', () => {
            mount();

            expect(spectator.query(byTestId('radio-card-label'))?.textContent?.trim()).toBe(
                'Reach a page'
            );
            expect(spectator.query(byTestId('radio-card-description'))?.textContent?.trim()).toBe(
                'A visitor lands on a page'
            );
        });

        it('should render no description when none is given', () => {
            mount({ description: '' });

            expect(spectator.query(byTestId('radio-card-description'))).toBeNull();
        });

        it('should read as an unchecked, focusable radio', () => {
            mount();

            expect(host().getAttribute('role')).toBe('radio');
            expect(host().getAttribute('aria-checked')).toBe('false');
            expect(host().getAttribute('tabindex')).toBe('0');
        });

        it('should report the option it stands for when clicked', () => {
            mount();
            const picked = watchPicks();

            spectator.click(host());

            expect(picked).toHaveBeenCalledWith('REACH_PAGE');
        });

        it('should report nothing when the checked card is clicked again', () => {
            mount({ value: 'REACH_PAGE' });
            const picked = watchPicks();

            spectator.click(host());

            expect(picked).not.toHaveBeenCalled();
        });

        it.each(['Enter', ' '])('should pick the card on "%s"', (key) => {
            mount();
            const picked = watchPicks();

            pressKey(key);

            expect(picked).toHaveBeenCalledWith('REACH_PAGE');
        });

        it('should read as checked while the group holds its option', () => {
            mount({ value: 'REACH_PAGE' });

            expect(host().getAttribute('aria-checked')).toBe('true');
        });

        // The look — accent, radio circle, transitions — is CSS keyed on `aria-checked`, so asserting
        // it here would only restate the attribute above: jsdom computes no styles. What the browser
        // renders is checked there.
        it('should render the card as a p-card so the theme owns its chrome', () => {
            mount();

            expect(surface().tagName.toLowerCase()).toBe('p-card');
            expect(surface().className).toContain('p-card');
        });

        it('should read as unchecked while the group holds another option', () => {
            mount({ value: 'BOUNCE_RATE' });

            expect(host().getAttribute('aria-checked')).toBe('false');
        });

        it('should follow the group being pointed at another card', () => {
            mount({ value: 'REACH_PAGE' });

            spectator.setInput('value', 'BOUNCE_RATE');
            spectator.detectChanges();

            expect(host().getAttribute('aria-checked')).toBe('false');
        });

        describe('disabled', () => {
            beforeEach(() => mount({ disabled: true }));

            it('should read as a disabled radio out of the tab order', () => {
                expect(host().getAttribute('aria-disabled')).toBe('true');
                expect(host().getAttribute('tabindex')).toBe('-1');
            });

            it('should not be pickable by click', () => {
                const picked = watchPicks();

                spectator.click(host());

                expect(picked).not.toHaveBeenCalled();
                expect(host().getAttribute('aria-checked')).toBe('false');
            });

            it('should not be pickable by keyboard', () => {
                const picked = watchPicks();

                pressKey('Enter');

                expect(picked).not.toHaveBeenCalled();
            });
        });
    });

    /**
     * Both forms APIs are asserted on real hosts rather than on the `ControlValueAccessor` members:
     * what matters is that each one recognises the card and drives it — value both ways, disabled
     * state, and touched. Signal forms reaches it through its interop bridge, the same path every
     * PrimeNG control takes, so this is the guard against that path regressing.
     */
    describe('bound to a reactive form control', () => {
        @Component({
            selector: 'dot-test-reactive-form-host',
            imports: [DotRadioCardComponent, ReactiveFormsModule],
            template: `
                <div role="radiogroup">
                    @for (option of options; track option) {
                        <dot-radio-card
                            [formControl]="control"
                            [option]="option"
                            [label]="option"
                            [attr.data-testid]="'card-' + option" />
                    }
                </div>
            `
        })
        class ReactiveFormHostComponent {
            readonly options = OPTIONS;
            readonly control = new FormControl('', { nonNullable: true });
        }

        let spectator: Spectator<ReactiveFormHostComponent>;

        const createHost = createComponentFactory(ReactiveFormHostComponent);
        const card = (option: string) => spectator.query(byTestId(`card-${option}`)) as HTMLElement;

        beforeEach(() => {
            spectator = createHost();
        });

        it('should write the picked option into the control', () => {
            spectator.click(card('BOUNCE_RATE'));
            spectator.detectChanges();

            expect(spectator.component.control.value).toBe('BOUNCE_RATE');
        });

        it('should check the card the control already holds', () => {
            spectator.component.control.setValue('REACH_PAGE');
            spectator.detectChanges();

            expect(card('REACH_PAGE').getAttribute('aria-checked')).toBe('true');
            expect(card('BOUNCE_RATE').getAttribute('aria-checked')).toBe('false');
        });

        it('should follow the control being disabled', () => {
            spectator.component.control.disable();
            spectator.detectChanges();

            spectator.click(card('BOUNCE_RATE'));
            spectator.detectChanges();

            expect(card('BOUNCE_RATE').getAttribute('aria-disabled')).toBe('true');
            expect(spectator.component.control.value).toBe('');
        });

        it('should mark the control as touched on blur', () => {
            expect(spectator.component.control.touched).toBe(false);

            spectator.dispatchFakeEvent(card('REACH_PAGE'), 'blur');

            expect(spectator.component.control.touched).toBe(true);
        });
    });

    describe('bound to a signal form field', () => {
        @Component({
            selector: 'dot-test-signal-form-host',
            imports: [DotRadioCardComponent, FormField],
            template: `
                <div role="radiogroup">
                    @for (option of options; track option) {
                        <dot-radio-card
                            [formField]="field.type"
                            [option]="option"
                            [label]="option"
                            [attr.data-testid]="'card-' + option" />
                    }
                </div>
            `
        })
        class SignalFormHostComponent {
            readonly options = OPTIONS;
            readonly $isLocked = signal(false);
            readonly model = signal({ type: '' });
            readonly field = form(this.model, (path) => {
                disabled(path.type, { when: () => this.$isLocked() });
            });
        }

        let spectator: Spectator<SignalFormHostComponent>;

        const createHost = createComponentFactory(SignalFormHostComponent);
        const card = (option: string) => spectator.query(byTestId(`card-${option}`)) as HTMLElement;

        beforeEach(() => {
            spectator = createHost();
        });

        it('should write the picked option into the field', () => {
            spectator.click(card('BOUNCE_RATE'));
            spectator.detectChanges();

            expect(spectator.component.model().type).toBe('BOUNCE_RATE');
        });

        it('should check the card the field already holds', () => {
            spectator.component.model.set({ type: 'REACH_PAGE' });
            spectator.detectChanges();

            expect(card('REACH_PAGE').getAttribute('aria-checked')).toBe('true');
            expect(card('BOUNCE_RATE').getAttribute('aria-checked')).toBe('false');
        });

        it('should keep the cards of one field in sync with each other', () => {
            spectator.click(card('REACH_PAGE'));
            spectator.detectChanges();

            spectator.click(card('BOUNCE_RATE'));
            spectator.detectChanges();

            // The field writes its new value to every binding, so no card is left checked behind.
            expect(card('BOUNCE_RATE').getAttribute('aria-checked')).toBe('true');
            expect(card('REACH_PAGE').getAttribute('aria-checked')).toBe('false');
        });

        it('should mark the field as touched on blur', () => {
            expect(spectator.component.field.type().touched()).toBe(false);

            spectator.dispatchFakeEvent(card('REACH_PAGE'), 'blur');
            spectator.detectChanges();

            expect(spectator.component.field.type().touched()).toBe(true);
        });

        it('should follow the field being disabled', () => {
            spectator.component.$isLocked.set(true);
            spectator.detectChanges();

            spectator.click(card('BOUNCE_RATE'));
            spectator.detectChanges();

            expect(card('BOUNCE_RATE').getAttribute('aria-disabled')).toBe('true');
            expect(spectator.component.model().type).toBe('');
        });
    });
});
