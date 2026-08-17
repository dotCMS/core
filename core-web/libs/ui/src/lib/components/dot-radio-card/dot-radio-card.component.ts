import {
    booleanAttribute,
    ChangeDetectionStrategy,
    Component,
    computed,
    forwardRef,
    input,
    model,
    signal
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { Card } from 'primeng/card';

/**
 * A radio rendered as a whole clickable `p-card`: radio circle, bold label and muted description.
 *
 * Semantics are `p-radioButton`'s, not a select's: **each card is one radio**, and several cards
 * sharing one value form the group — a card is checked while that value equals its own `option`.
 *
 * It is a `ControlValueAccessor`, which is the one implementation that serves every caller: reactive
 * forms bind it with `[formControl]` or `[formControlName]`, templates with `[(ngModel)]`, and signal
 * forms with `[formField]` through the interop bridge — the same path every PrimeNG control takes, and
 * the reason `p-slider` works under `[formField]` in this app. Without a form, `[(value)]` drives it
 * directly, or `[value]` plus `(valueChange)` when picking has to write more than the option itself.
 *
 * A `FormValueControl` would be the signal-forms-native alternative, but it is signal-forms-only, and
 * `FormField` resolves a value accessor before a custom control, so the two cannot be combined: adding
 * a CVA replaces the native path rather than adding to it.
 *
 * The circle is drawn in the stylesheet rather than being a `p-radioButton` nested inside: that would
 * put a second focusable radio in a host that is already `role="radio"`. It is drawn from the tokens
 * Lara's radiobutton maps to, so it tracks the theme.
 *
 * A radiogroup's roving tabindex and arrow keys belong to a future `dot-radio-group` and are
 * deliberately absent: a group knows the cards' order, and a card only knows itself.
 */
@Component({
    selector: 'dot-radio-card',
    imports: [Card],
    templateUrl: './dot-radio-card.component.html',
    styleUrl: './dot-radio-card.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotRadioCardComponent),
            multi: true
        }
    ],
    host: {
        role: 'radio',
        '[attr.aria-checked]': '$isChecked()',
        '[attr.aria-disabled]': '$isDisabled()',
        '[attr.tabindex]': '$isDisabled() ? -1 : 0',
        '(click)': 'select()',
        '(keydown.enter)': 'onKeydown($event)',
        '(keydown.space)': 'onKeydown($event)',
        '(blur)': 'onBlur()'
    }
})
export class DotRadioCardComponent implements ControlValueAccessor {
    /** The group's value: a bound control writes it here, and `[(value)]` does the same without one. */
    readonly value = model<string>('');

    /** The option this card stands for. */
    readonly option = input.required<string>();

    readonly label = input.required<string>();

    /** Muted body under the title. Project content instead when it is not plain text. */
    readonly description = input('');

    /** Disables the card on its own. A bound control disables it through `setDisabledState`. */
    readonly disabled = input(false, { transform: booleanAttribute });

    readonly #disabledByControl = signal(false);
    #notifyChange: (value: string) => void = () => {
        /* replaced by the bound control, absent without one */
    };
    #notifyTouched: () => void = () => {
        /* replaced by the bound control, absent without one */
    };

    protected readonly $isDisabled = computed<boolean>(
        () => this.disabled() || this.#disabledByControl()
    );

    protected readonly $isChecked = computed<boolean>(() => this.value() === this.option());

    writeValue(value: string | null): void {
        // Not reported back: this is the control telling the card, not the user picking.
        this.value.set(value ?? '');
    }

    registerOnChange(fn: (value: string) => void): void {
        this.#notifyChange = fn;
    }

    registerOnTouched(fn: () => void): void {
        this.#notifyTouched = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        this.#disabledByControl.set(isDisabled);
    }

    protected select(): void {
        // A radio cannot be unchecked, so re-picking the checked card reports nothing.
        if (this.$isDisabled() || this.$isChecked()) {
            return;
        }

        this.value.set(this.option());
        this.#notifyChange(this.option());
    }

    protected onBlur(): void {
        this.#notifyTouched();
    }

    protected onKeydown(event: Event): void {
        // Ignore keydowns that bubbled from projected content (e.g. a link in the description).
        if (event.target !== event.currentTarget) {
            return;
        }

        // Space would scroll the page, and Enter would submit the form the group sits in.
        event.preventDefault();
        this.select();
    }
}
