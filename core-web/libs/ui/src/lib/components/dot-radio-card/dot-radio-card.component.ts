import {
    booleanAttribute,
    ChangeDetectionStrategy,
    Component,
    computed,
    input,
    model,
    output
} from '@angular/core';
import { FormValueControl } from '@angular/forms/signals';

import { Card } from 'primeng/card';

/** The host is only the radio: `p-card` carries `.p-card` on its own element, one level in. */
const HOST_BASE_CLASSES = 'block text-left';

/**
 * The accent needs `!` to land: PrimeNG injects component CSS outside any cascade layer, Tailwind's
 * utilities live in `@layer utilities`, and unlayered declarations win regardless of specificity.
 */
const SURFACE_BASE_CLASSES =
    'transition-colors duration-(--p-transition-duration) ease-[ease] motion-reduce:transition-none';
const SURFACE_CHECKED_CLASSES = 'border-primary! bg-primary-50!';

/** `p-radioButton`'s own tokens and its rendered geometry, measured in the running app. */
const INDICATOR_CHECKED_CLASSES = 'border-primary bg-primary';
const INDICATOR_UNCHECKED_CLASSES = 'border-(--p-form-field-border-color) bg-surface-0';
const ENABLED_CLASSES = 'cursor-pointer';
const DISABLED_CLASSES = 'cursor-default opacity-60';

/**
 * A radio rendered as a whole clickable `p-card`: radio circle, bold label and muted description.
 *
 * Semantics are `p-radioButton`'s, not a select's: **each card is one radio**, and several cards
 * sharing one value form the group — a card is checked while that value equals its own `option`.
 *
 * Signal forms only, by design: it implements `FormValueControl<string>` and no
 * `ControlValueAccessor`, so `[formField]` drives it natively and nothing here reaches into reactive
 * forms. Without a form, use `[(value)]` — or `[value]` plus `(valueChange)` when picking an option
 * has to write more than the option itself.
 *
 * The circle is drawn here rather than being a `p-radioButton`, which cannot be mounted
 * presentationally: its `onInit` resolves `NgControl` without `optional`, so one rendered with no
 * `ngModel` or `formControl` throws `NG0201`. Reusing it would mean an internal reactive-forms
 * control existing to paint a circle, and a second focusable radio nested in this host. Worth
 * revisiting if PrimeNG ever makes that injection optional.
 *
 * A radiogroup's roving tabindex and arrow keys belong to a future `dot-radio-group` and are
 * deliberately absent: a group knows the cards' order, and a card only knows itself.
 */
@Component({
    selector: 'dot-radio-card',
    imports: [Card],
    templateUrl: './dot-radio-card.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        role: 'radio',
        '[class]': '$stateClasses()',
        '[attr.aria-checked]': '$isChecked()',
        '[attr.aria-disabled]': 'disabled()',
        '[attr.tabindex]': 'disabled() ? -1 : 0',
        '(click)': 'select()',
        '(keydown.enter)': 'onKeydown($event)',
        '(keydown.space)': 'onKeydown($event)',
        '(blur)': 'touch.emit()'
    }
})
export class DotRadioCardComponent implements FormValueControl<string> {
    /** The group's value, named by the control contract. Any value no card carries leaves it unpicked. */
    readonly value = model<string>('');

    /** The option this card stands for. */
    readonly option = input.required<string>();

    readonly label = input.required<string>();

    /** Muted body under the title. Project content instead when it is not plain text. */
    readonly description = input('');

    /** Part of the control contract: a bound field owns it and `[formField]` writes it here. */
    readonly disabled = input(false, { transform: booleanAttribute });

    /** Marks the bound field touched. */
    readonly touch = output<void>();

    protected readonly $isChecked = computed<boolean>(() => this.value() === this.option());

    protected readonly $indicatorClasses = computed<string>(() =>
        this.$isChecked() ? INDICATOR_CHECKED_CLASSES : INDICATOR_UNCHECKED_CLASSES
    );

    protected readonly $surfaceClasses = computed<string>(() =>
        this.$isChecked()
            ? `${SURFACE_BASE_CLASSES} ${SURFACE_CHECKED_CLASSES}`
            : SURFACE_BASE_CLASSES
    );

    protected readonly $stateClasses = computed<string>(() => {
        const interaction = this.disabled() ? DISABLED_CLASSES : ENABLED_CLASSES;

        return `${HOST_BASE_CLASSES} ${interaction}`;
    });

    protected select(): void {
        // A radio cannot be unchecked, so re-picking the checked card reports nothing.
        if (this.disabled() || this.$isChecked()) {
            return;
        }

        this.value.set(this.option());
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
