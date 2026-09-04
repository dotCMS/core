import { Directive, ElementRef, Input, Renderer2, effect, inject, input } from '@angular/core';
import { FormGroupDirective, Validators } from '@angular/forms';
import { Field } from '@angular/forms/signals';

/**
 * Marks a label as belonging to a mandatory field, by adding `p-label-input-required` — the class
 * the global stylesheet turns into the red asterisk.
 *
 * It answers the same question three ways, because a form can say "this is required" in three
 * places:
 *
 * 1. **Bare** — `<label dotFieldRequired>`. The label simply states the field is mandatory. No form
 *    of any kind is needed, which is what makes it usable on a screen that deliberately declares no
 *    `required` rule (see the Experiments Configure form, where a native `required` would paint an
 *    untouched Name red from first render).
 * 2. **Reactive forms** — `<label dotFieldRequired checkIsRequiredControl="name">`. Follows whether
 *    that control carries `Validators.required`.
 * 3. **Signal forms** — `<label [dotFieldRequired]="field">`. Follows the field's own `required()`,
 *    so a conditionally-required field marks and unmarks itself as the condition changes.
 *
 * `FormGroupDirective` is injected optionally: modes 1 and 3 have no `[formGroup]` above them, and
 * a hard injection made the bare form throw outside reactive forms.
 */
@Directive({
    selector: '[dotFieldRequired]'
})
export class DotFieldRequiredDirective {
    /**
     * The signal-forms field this label stands for, when there is one.
     *
     * `Field`, not `FieldTree`: a `FieldTree<T>` is callable *and* carries `T`'s subfield shape, so
     * `FieldTree<unknown>` is not a supertype — a `FieldTree<string>` does not assign to it. `Field`
     * is the callable half, which is all this reads, and it is the type `dot-radio-card` takes for
     * the same reason.
     *
     * Empty string rather than `undefined` as the default: the bare attribute binds no value, and
     * the alias makes `dotFieldRequired` both the selector and this input.
     */
    readonly $field = input<Field<unknown> | ''>('', { alias: 'dotFieldRequired' });

    readonly #el = inject(ElementRef);
    readonly #renderer = inject(Renderer2);
    readonly #formGroupDirective = inject(FormGroupDirective, { optional: true });

    constructor() {
        // Marked up front so the asterisk is there on first paint. The two following modes only
        // ever take it away, which keeps the bare case a no-op.
        this.#setRequired(true);

        effect(() => {
            const field = this.$field();

            if (field) {
                this.#setRequired(field().required());
            }
        });
    }

    /**
     * Remove Required Class if it is not required
     * @memberof DotFieldRequiredDirective
     * @param {string} controlName
     */
    @Input()
    set checkIsRequiredControl(controlName: string) {
        if (!this.isRequiredControl(controlName)) {
            this.#setRequired(false);
        }
    }

    #setRequired(isRequired: boolean): void {
        if (isRequired) {
            this.#renderer.addClass(this.#el.nativeElement, 'p-label-input-required');
        } else {
            this.#renderer.removeClass(this.#el.nativeElement, 'p-label-input-required');
        }
    }

    /**
     * Helper function for check control is required or not
     * @private
     * @param {string} controlName
     * @return {*}  {boolean}
     * @memberof DotFieldRequiredDirective
     */
    private isRequiredControl(controlName: string): boolean {
        const formControl = this.#formGroupDirective?.control?.get(controlName);

        return formControl && formControl.hasValidator(Validators.required) ? true : false;
    }
}
