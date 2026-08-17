import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { Component, signal } from '@angular/core';

import { DotRadioGroupComponent } from './dot-radio-group.component';

import { DotRadioCardComponent } from '../dot-radio-card/dot-radio-card.component';

const OPTIONS = ['REACH_PAGE', 'BOUNCE_RATE', 'EXIT_RATE'] as const;

/**
 * Asserted over real `dot-radio-card`s rather than a stub: what matters is that a radio joins the group
 * through the member token and that the group moves the selection, which only a real radio can show.
 */
@Component({
    selector: 'dot-test-radio-group-host',
    imports: [DotRadioGroupComponent, DotRadioCardComponent],
    template: `
        <dot-radio-group data-testid="group">
            @for (option of options; track option) {
                <dot-radio-card
                    [option]="option"
                    [label]="option"
                    [value]="$picked()"
                    [disabled]="$disabledOptions().includes(option)"
                    (valueChange)="$picked.set($event)"
                    [attr.data-testid]="'card-' + option" />
            }
        </dot-radio-group>
    `
})
class RadioGroupHostComponent {
    readonly options = OPTIONS;
    readonly $picked = signal<string>('');
    readonly $disabledOptions = signal<string[]>([]);
}

describe('DotRadioGroupComponent', () => {
    let spectator: Spectator<RadioGroupHostComponent>;

    const createHost = createComponentFactory(RadioGroupHostComponent);
    const card = (option: string) => spectator.query(byTestId(`card-${option}`)) as HTMLElement;
    // Spectator's keyboard helper builds the event with the removed `initKeyboardEvent` API.
    const pressOn = (option: string, key: string) => {
        card(option).dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true }));
        spectator.detectChanges();
    };

    beforeEach(() => {
        spectator = createHost();
    });

    it('should read as a radiogroup', () => {
        expect(spectator.query(byTestId('group'))?.getAttribute('role')).toBe('radiogroup');
    });

    it('should leave each radio in the tab order, since it manages no roving tabindex', () => {
        expect(OPTIONS.map((option) => card(option).getAttribute('tabindex')).join(' ')).toBe(
            '0 0 0'
        );
    });

    it.each([
        ['ArrowDown', 'BOUNCE_RATE'],
        ['ArrowRight', 'BOUNCE_RATE'],
        ['ArrowUp', 'EXIT_RATE'],
        ['ArrowLeft', 'EXIT_RATE'],
        ['End', 'EXIT_RATE'],
        ['Home', 'REACH_PAGE']
    ])('should pick %s -> %s', (key, expected) => {
        spectator.component.$picked.set('REACH_PAGE');
        spectator.detectChanges();

        pressOn('REACH_PAGE', key);

        expect(spectator.component.$picked()).toBe(expected);
    });

    it('should wrap around past the last radio', () => {
        spectator.component.$picked.set('EXIT_RATE');
        spectator.detectChanges();

        pressOn('EXIT_RATE', 'ArrowDown');

        expect(spectator.component.$picked()).toBe('REACH_PAGE');
    });

    it('should move the focus along with the selection', () => {
        pressOn('REACH_PAGE', 'ArrowDown');

        expect(document.activeElement).toBe(card('BOUNCE_RATE'));
    });

    it('should skip a disabled radio', () => {
        spectator.component.$disabledOptions.set(['BOUNCE_RATE']);
        spectator.component.$picked.set('REACH_PAGE');
        spectator.detectChanges();

        pressOn('REACH_PAGE', 'ArrowDown');

        expect(spectator.component.$picked()).toBe('EXIT_RATE');
    });

    it('should leave keys it does not own alone', () => {
        spectator.component.$picked.set('REACH_PAGE');
        spectator.detectChanges();

        pressOn('REACH_PAGE', 'PageDown');

        expect(spectator.component.$picked()).toBe('REACH_PAGE');
    });
});
