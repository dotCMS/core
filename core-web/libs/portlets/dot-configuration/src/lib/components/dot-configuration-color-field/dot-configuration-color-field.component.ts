import { Component, computed, input, linkedSignal, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ColorPickerChangeEvent, ColorPickerModule } from 'primeng/colorpicker';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessagePipe } from '@dotcms/ui';

const HEX_COLOR = /^#([0-9a-f]{3}|[0-9a-f]{4}|[0-9a-f]{6}|[0-9a-f]{8})$/i;

/**
 * The six-digit form of a color the server accepts, which is the only form PrimeNG's picker
 * understands: `#abc` → `#aabbcc`; an alpha channel is dropped, since the picker has none.
 *
 * @param value - Color as typed.
 * @returns The six-digit hex, or `null` when `value` is not a color yet.
 */
export function toPickerHex(value: string): string | null {
    if (!HEX_COLOR.test(value)) {
        return null;
    }

    const digits = value.slice(1);
    const rgb =
        digits.length <= 4
            ? [...digits.slice(0, 3)].map((digit) => digit + digit).join('')
            : digits.slice(0, 6);

    return `#${rgb}`;
}

/**
 * Color setting: a swatch that opens PrimeNG's color picker, next to an editable hex input.
 * Typing any color the server accepts (`#abc`, `#abcd`, `#aabbcc`, `#aabbccdd`) moves the
 * picker; picking a color writes its six-digit code into the input. Both report through `valueChange`; validation comes from the page store.
 */
@Component({
    selector: 'dot-configuration-color-field',
    imports: [FormsModule, ColorPickerModule, InputTextModule, DotMessagePipe],
    templateUrl: './dot-configuration-color-field.component.html'
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
        computation: (value, previous) => toPickerHex(value) ?? previous?.value ?? null
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
