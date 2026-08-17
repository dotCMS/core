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

/**
 * The card is `p-card`, so the surface, radius, border and padding are whatever `components.card` in
 * `theme.config.ts` says a dotCMS card is, and stay that way when the preset changes. Nothing here
 * restates them.
 *
 * The host only carries the interaction state, because `p-card` puts its `.p-card` class on its own
 * host element: the radio semantics have to stay on `<dot-radio-card>`, one level out, which leaves the
 * host a bare block that the card fills.
 */
const HOST_BASE_CLASSES = 'block text-left';

/**
 * The selection accent is the only thing painted over the theme's card, and it needs `!` to land:
 * PrimeNG injects its component CSS outside any cascade layer while Tailwind's utilities live in
 * `@layer utilities`, and unlayered declarations win over layered ones no matter the specificity — so
 * `.p-card`'s own border and background would otherwise beat these. Verified in the browser.
 *
 * Unchecked deliberately adds nothing: an unselected card is just a card.
 */
const SURFACE_BASE_CLASSES =
    'transition-colors duration-(--p-transition-duration) ease-[ease] motion-reduce:transition-none';
const SURFACE_CHECKED_CLASSES = 'border-primary! bg-primary-50!';

/**
 * The circle is `p-radioButton`'s, rebuilt rather than reused, so a radio in a card looks like every
 * other radio in dotCMS: `1.5rem` box with a 1px border, filled with `primary` when checked, holding a
 * `1rem` `primary.contrast` dot that scales from `0.1` to `1` over `form.field.transition.duration`.
 * Those are the rendered values of a real one, measured in the running app, not a guess.
 *
 * Rebuilt because `p-radioButton` cannot be mounted presentationally: it resolves `NgControl` from the
 * injector without `optional`, so without a reactive or template form binding it throws `NG0201`.
 * Reusing it would mean pulling `NgControl` into a control that is signal forms only, and nesting a
 * second focusable radio inside a host that is already `role="radio"`.
 */
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
 * Gaining and losing the selection is animated in both directions, the way `p-radioButton` does it:
 * the dot stays in the DOM and scales between `0.1` and `1`, and the colours transition. Both are
 * dropped under `prefers-reduced-motion`.
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
