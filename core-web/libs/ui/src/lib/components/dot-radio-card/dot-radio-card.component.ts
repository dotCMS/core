import { Component, input, output } from '@angular/core';
import { Field, FormField } from '@angular/forms/signals';

import { Card } from 'primeng/card';
import { RadioButton, RadioButtonClickEvent } from 'primeng/radiobutton';

/**
 * A radio rendered as a whole clickable `p-card`: the radio, a bold label and a muted description.
 *
 * The radio is a real `p-radioButton` bound to the field with `[formField]`, and the card is a
 * `<label>` around it, which is what makes all of this the browser's problem rather than ours: the
 * whole card is clickable because that is what a label does, the radio carries its own semantics and
 * focus ring, and radios bound to one field share the `name` the interop derives from the field's
 * path — so arrow keys, roving focus and Home/End are the native radio group's, not a widget of ours.
 *
 * It is therefore not a form control and takes no value: the field goes straight to the radio inside.
 * `(picked)` exists for the case where choosing an option has to write more than the option — the
 * radio has already written it by then, so a handler only adds the rest.
 *
 * @example
 * ```html
 * @for (item of items; track item.value) {
 *   <dot-radio-card [field]="field.type" [option]="item.value" [label]="item.label" />
 * }
 * ```
 */
@Component({
    selector: 'dot-radio-card',
    imports: [Card, RadioButton, FormField],
    templateUrl: './dot-radio-card.component.html',
    styleUrl: './dot-radio-card.component.scss'
})
export class DotRadioCardComponent<T> {
    /** The field every card of the group is bound to. The radio inside is its control. */
    readonly $field = input.required<Field<T>>({ alias: 'field' });

    /** The option this card stands for: the value the field takes when this card is picked. */
    readonly $option = input.required<T>({ alias: 'option' });

    readonly $label = input.required<string>({ alias: 'label' });

    /** Muted body under the label. Project content instead when it is not plain text. */
    readonly $description = input('', { alias: 'description' });

    /** Reports a pick *after* the radio has written it, for whatever else the choice implies. */
    readonly picked = output<T>();

    protected onClick(event: RadioButtonClickEvent): void {
        this.picked.emit(event.value as T);
    }
}
