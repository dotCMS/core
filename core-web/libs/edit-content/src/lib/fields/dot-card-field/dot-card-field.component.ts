import {
    afterRenderEffect,
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    inject,
    input
} from '@angular/core';

/**
 * The shell every field in the new Edit Contentlet renders inside.
 *
 * The wrapper is the global `.field` from `apps/dotcms-ui/src/style.css`, not a hand-rolled
 * `flex flex-col gap-2`: the editor's root form now carries `.form`, so the global system supplies
 * the layout, the label typography and the hint/error colours that this library used to restate
 * or simply go without.
 *
 * The component survives that adoption because it provides three things the global system does not:
 * the `field-error-marker` anchor the form scrolls to, hiding an empty footer so a field with
 * neither hint nor error leaves no phantom gap, and telling assistive technology that a field is
 * mandatory.
 *
 * The label is projected as a `<label dotCardFieldLabel>` rather than an element of its own, so it
 * lands as a **direct child** of `.field` — which is what `.form .field > label` requires.
 */
@Component({
    selector: 'dot-card-field',
    template: `
        @if ($hasError()) {
            <div class="field-error-marker"></div>
        }

        <div class="field">
            <ng-content select="label[dotCardFieldLabel]" />
            <ng-content select="dot-card-field-content" />
            <ng-content select="dot-card-field-footer" />
        </div>
    `,
    styles: [
        `
            :host ::ng-deep dot-card-field-footer:empty {
                display: none;
            }
        `
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCardFieldComponent {
    $hasError = input.required<boolean>({ alias: 'hasError' });

    /**
     * Whether the content type marks this field mandatory.
     *
     * Drives `aria-required` on the projected control. The red asterisk cannot do that job: it is
     * `::after` content, deliberately outside the label's accessible name so screen readers do not
     * announce "star" — which leaves assistive technology with nothing at all unless the control
     * states it.
     */
    $isRequired = input<boolean>(false, { alias: 'isRequired' });

    readonly #el = inject(ElementRef<HTMLElement>);

    constructor() {
        // afterRenderEffect rather than a template binding: the control is projected content this
        // component never declares, and composite widgets render theirs asynchronously. Re-runs
        // whenever `isRequired` changes, so a conditionally-required field stays honest.
        afterRenderEffect(() => {
            const required = this.$isRequired();
            const host = this.#el.nativeElement as HTMLElement;
            const content = host.querySelector('dot-card-field-content');
            // The label's `for` is the declared association, so it is the authority on WHICH
            // element is the field's control. Taking the first focusable descendant instead marks
            // the wrong one on any field whose widget puts a control ahead of the real one — Text
            // Area and WYSIWYG both render an editor-mode dropdown above their textarea.
            const target = host.querySelector('label[dotCardFieldLabel]')?.getAttribute('for');
            // Only the element the label points at, never a guess. `[id="..."]` rather than
            // `#id` + CSS.escape, because the value is a content type variable that may start with
            // a digit, and CSS.escape is not available in every environment this runs in.
            //
            // Falling back to "the first focusable descendant" was tried and removed: Text Area and
            // WYSIWYG both render an editor-mode dropdown above their real control, so the fallback
            // marked the dropdown as the mandatory field. A field whose `for` resolves to nothing
            // has a broken label association — a defect to fix in that field, not to paper over by
            // marking whichever element happens to come first.
            const control = target ? content?.querySelector(`[id="${target}"]`) : null;

            if (!control) {
                return;
            }

            if (required) {
                control.setAttribute('aria-required', 'true');
            } else {
                control.removeAttribute('aria-required');
            }
        });
    }
}
