import { Component, computed, input, linkedSignal, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ColorPickerChangeEvent, ColorPickerModule } from 'primeng/colorpicker';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessagePipe } from '@dotcms/ui';

// The picker only understands six-digit hex; shorter or alpha forms are still valid to type.
const SIX_DIGIT_HEX = /^#[0-9a-f]{6}$/i;

/**
 * Color setting: a swatch that opens PrimeNG's color picker, next to an editable hex input.
 * Typing a valid six-digit code moves the picker; picking a color writes its code into the
 * input. Both report through `valueChange`; validation comes from the page store.
 */
@Component({
    selector: 'dot-configuration-color-field',
    imports: [FormsModule, ColorPickerModule, InputTextModule, DotMessagePipe],
    templateUrl: './dot-configuration-color-field.component.html',
    styleUrls: ['./dot-configuration-color-field.component.scss']
})
export class DotConfigurationColorFieldComponent {
    readonly inputId = input.required<string>();
    readonly labelKey = input.required<string>();
    readonly value = input('');
    /** i18n key of the validation message, or `null` when the value is valid. */
    readonly errorKey = input<string | null | undefined>(null);
    readonly disabled = input(false);

    readonly valueChange = output<string>();

    /** What the picker shows; it keeps the last valid color while the input holds a partial code. */
    protected readonly $pickerValue = linkedSignal<string, string | null>({
        source: this.value,
        computation: (value, previous) =>
            SIX_DIGIT_HEX.test(value) ? value : (previous?.value ?? null)
    });
    protected readonly $errorId = computed(() => `${this.inputId()}-error`);

    onPickerChange(event: ColorPickerChangeEvent): void {
        if (typeof event.value === 'string') {
            this.valueChange.emit(event.value);
        }
    }

    onInputChange(value: string): void {
        this.valueChange.emit(value.trim());
    }
}
