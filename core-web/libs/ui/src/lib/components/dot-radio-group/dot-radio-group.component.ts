import { ChangeDetectionStrategy, Component, ElementRef, inject } from '@angular/core';

/** How far each key moves through the group. `Home` and `End` are absolute, hence the sentinels. */
const KEY_MOVES: Record<string, number | 'first' | 'last'> = {
    ArrowDown: 1,
    ArrowRight: 1,
    ArrowUp: -1,
    ArrowLeft: -1,
    Home: 'first',
    End: 'last'
};

const RADIO = '[role="radio"]';

/**
 * Adds arrow-key navigation to a set of radios, which is what a group of them is missing when each one
 * only knows itself: the arrows move through them — wrapping around — and `Home` and `End` jump to the
 * ends, skipping any that are disabled. Reaching a radio picks it, the way a native radio group behaves.
 *
 * It reads its radios from the DOM at the moment of the keypress, by role and by `aria-disabled`, so it
 * is coupled to nothing: any `role="radio"` child joins, and a radio disabled by a form control counts
 * as disabled without having to say so. It holds no value and takes no form binding — the radios are
 * already bound, and a group that owned the value too would be a second source of truth for it.
 *
 * It deliberately stops there. The ARIA pattern also asks a radiogroup to be a single tab stop, which
 * needs every radio to know its group and defer its `tabindex` to it; that coupling costs more than it
 * pays here, so Tab still visits each radio.
 *
 * @example
 * ```html
 * <dot-radio-group class="grid grid-cols-2 gap-3">
 *   @for (item of items; track item.value) {
 *     <dot-radio-card [formField]="field.type" [option]="item.value" [label]="item.label" />
 *   }
 * </dot-radio-group>
 * ```
 */
@Component({
    selector: 'dot-radio-group',
    template: '<ng-content />',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        role: 'radiogroup',
        '(keydown)': 'onKeydown($event)'
    }
})
export class DotRadioGroupComponent {
    readonly #host: HTMLElement = inject(ElementRef).nativeElement;

    protected onKeydown(event: KeyboardEvent): void {
        const move = KEY_MOVES[event.key];

        if (move === undefined) {
            return;
        }

        const radios = [...this.#host.querySelectorAll<HTMLElement>(RADIO)].filter(
            (radio) => radio.getAttribute('aria-disabled') !== 'true'
        );
        // Only keys aimed at a radio of this group, not at a control projected inside one.
        const current = radios.indexOf(event.target as HTMLElement);

        if (current === -1) {
            return;
        }

        const target = this.#radioAt(radios, current, move);

        if (!target) {
            return;
        }

        // Both would otherwise reach the page: arrows scroll, and Home/End jump to the document ends.
        event.preventDefault();
        target.focus();
        // Picking is the radio's own business, so it is asked the way a user would ask.
        target.click();
    }

    #radioAt(
        radios: HTMLElement[],
        current: number,
        move: number | 'first' | 'last'
    ): HTMLElement | undefined {
        if (move === 'first') {
            return radios[0];
        }

        if (move === 'last') {
            return radios[radios.length - 1];
        }

        // Wraps, as a radio group does: past the last radio is the first one.
        return radios[(current + move + radios.length) % radios.length];
    }
}
