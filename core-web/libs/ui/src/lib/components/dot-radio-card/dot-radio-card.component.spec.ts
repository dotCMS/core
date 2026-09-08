import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { Component, signal } from '@angular/core';
import { disabled, form } from '@angular/forms/signals';

import { DotRadioCardComponent } from './dot-radio-card.component';

const OPTIONS = ['REACH_PAGE', 'BOUNCE_RATE'] as const;

/**
 * Asserted on a real form host rather than on inputs and outputs: the card's whole job is to put a
 * `p-radioButton` in a `<label>` and let the field, the label and the native radio group do the rest,
 * so what matters is what a user gets out of the arrangement.
 */
@Component({
    selector: 'dot-test-radio-card-host',
    imports: [DotRadioCardComponent],
    template: `
        <div role="radiogroup">
            @for (option of options; track option) {
                <dot-radio-card
                    [field]="form.type"
                    [option]="option"
                    [label]="option"
                    description="what this option means"
                    (picked)="picked.push($event)"
                    [attr.data-testid]="'card-' + option" />
            }
        </div>
    `
})
class RadioCardHostComponent {
    readonly options = OPTIONS;
    readonly picked: string[] = [];
    readonly $isLocked = signal(false);
    readonly model = signal<{ type: string }>({ type: '' });
    readonly form = form(this.model, (path) => {
        disabled(path.type, { when: () => this.$isLocked() });
    });
}

describe('DotRadioCardComponent', () => {
    let spectator: Spectator<RadioCardHostComponent>;

    const createHost = createComponentFactory(RadioCardHostComponent);
    const card = (option: string) => spectator.query(byTestId(`card-${option}`)) as HTMLElement;
    const radio = (option: string) => card(option).querySelector('input') as HTMLInputElement;
    const labelOf = (option: string) =>
        card(option).querySelector('[data-testid="radio-card-label"]') as HTMLElement;

    beforeEach(() => {
        spectator = createHost();
    });

    it('should render the label and the description', () => {
        expect(labelOf('REACH_PAGE').textContent?.trim()).toBe('REACH_PAGE');
        expect(
            card('REACH_PAGE')
                .querySelector('[data-testid="radio-card-description"]')
                ?.textContent?.trim()
        ).toBe('what this option means');
    });

    it('should render the card as a p-card so the theme owns its chrome', () => {
        const surface = card('REACH_PAGE').querySelector('[data-testid="radio-card-surface"]');

        expect(surface?.tagName.toLowerCase()).toBe('p-card');
        expect(surface?.className).toContain('p-card');
    });

    it('should write the picked option into the field', () => {
        radio('BOUNCE_RATE').click();
        spectator.detectChanges();

        expect(spectator.component.model().type).toBe('BOUNCE_RATE');
    });

    // The reason the card is a `<label>`: the whole card is the radio's hit area, for free.
    it('should pick the option when the card body is clicked', () => {
        labelOf('BOUNCE_RATE').click();
        spectator.detectChanges();

        expect(spectator.component.model().type).toBe('BOUNCE_RATE');
    });

    it('should check the card the field already holds, and only that one', () => {
        spectator.component.model.set({ type: 'REACH_PAGE' });
        spectator.detectChanges();

        expect(radio('REACH_PAGE').checked).toBe(true);
        expect(radio('BOUNCE_RATE').checked).toBe(false);
    });

    // What the arrow keys, roving focus and Home/End are inherited from: one native radio group.
    it('should share one native group name across the cards of a field', () => {
        const name = radio('REACH_PAGE').getAttribute('name');

        expect(name).toBeTruthy();
        expect(radio('BOUNCE_RATE').getAttribute('name')).toBe(name);
    });

    it('should report the pick, so a choice can write more than the option', () => {
        radio('BOUNCE_RATE').click();
        spectator.detectChanges();

        expect(spectator.component.picked).toEqual(['BOUNCE_RATE']);
    });

    it('should report nothing when the checked card is clicked again', () => {
        spectator.component.model.set({ type: 'BOUNCE_RATE' });
        spectator.detectChanges();

        radio('BOUNCE_RATE').click();
        spectator.detectChanges();

        expect(spectator.component.picked).toEqual([]);
    });

    describe('a disabled field', () => {
        beforeEach(() => {
            spectator.component.$isLocked.set(true);
            spectator.detectChanges();
        });

        it('should disable the radio', () => {
            expect(radio('BOUNCE_RATE').disabled).toBe(true);
        });

        it('should not be pickable, and should report nothing', () => {
            radio('BOUNCE_RATE').click();
            labelOf('BOUNCE_RATE').click();
            spectator.detectChanges();

            expect(spectator.component.model().type).toBe('');
            expect(spectator.component.picked).toEqual([]);
        });
    });
});
