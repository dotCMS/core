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

const BASE_CLASSES = 'flex items-start gap-3 rounded-md border p-4 text-left';
const CHECKED_CLASSES = 'border-primary-500 bg-primary-50';
const UNCHECKED_CLASSES = 'border-surface-200 bg-white';
const ENABLED_CLASSES = 'cursor-pointer';
const DISABLED_CLASSES = 'cursor-default opacity-60';

/**
 * A radio rendered as a whole clickable card: radio circle, bold label and muted description.
 *
 * Semantics are `p-radioButton`'s, not a select's: **each card is one radio**, and several cards
 * sharing one value form the group — a card is checked while that value equals its own `option`.
 *
 * It is a signal forms custom control (`FormValueControl<string>`), so `[formField]` binds
 * it natively: the directive keeps `value` in sync with the field in both directions and pushes the
 * field's `disabled` state into the input of the same name. Cards bound to one field therefore stay
 * in sync with each other — the field writes its new value to every binding, so the card that was
 * checked before is told it no longer is.
 *
 * Signal forms only, by design: no `ControlValueAccessor`, so nothing here reaches into reactive
 * forms. Without a form, drive it with `[(value)]` — or `[value]` plus `(valueChange)` when the
 * group's value is not writable, e.g. when picking an option writes more than the option itself.
 *
 * A11y: the host is the radio (`role="radio"`, `aria-checked`, `aria-disabled`), focusable while
 * enabled, and Space/Enter picks it. A radiogroup's roving tabindex and arrow-key navigation
 * belong to a future `dot-radio-group` wrapper and are deliberately not implemented here: a group
 * is what knows the cards' order, and each card only knows itself.
 *
 * @example
 * ```html
 * <div role="radiogroup">
 *   @for (item of items; track item.value) {
 *     <dot-radio-card
 *       [formField]="field.type"
 *       [option]="item.value"
 *       [label]="item.label"
 *       [description]="item.description" />
 *   }
 * </div>
 * ```
 */
@Component({
    selector: 'dot-radio-card',
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
    /**
     * The group's value, which is the whole point of the contract: `[formField]` keeps it in sync
     * with the bound field, and `[(value)]` does the same without a form. The empty string — like
     * any value no card carries — leaves the group unpicked.
     */
    readonly value = model<string>('');

    /** The option this card stands for: it reads as checked while the group holds this value. */
    readonly option = input.required<string>();

    /** Bold title of the card. */
    readonly label = input.required<string>();

    /** Muted body under the title. Project content instead when the body is not plain text. */
    readonly description = input('');

    /**
     * Whether the card can be picked. Part of the control contract rather than a private input: a
     * bound field owns it, and `[formField]` writes the field's disabled state here.
     */
    readonly disabled = input(false, { transform: booleanAttribute });

    /** Reports that the user is done with the card, which is what marks the bound field touched. */
    readonly touch = output<void>();

    protected readonly $isChecked = computed<boolean>(() => this.value() === this.option());

    protected readonly $stateClasses = computed<string>(() => {
        const state = this.$isChecked() ? CHECKED_CLASSES : UNCHECKED_CLASSES;
        const interaction = this.disabled() ? DISABLED_CLASSES : ENABLED_CLASSES;

        return `${BASE_CLASSES} ${state} ${interaction}`;
    });

    /** Picks this card. Re-picking the checked one reports nothing: a radio cannot be unchecked. */
    protected select(): void {
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
