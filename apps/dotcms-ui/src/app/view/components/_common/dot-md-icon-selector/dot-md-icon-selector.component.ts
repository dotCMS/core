import { Component, forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
    selector: 'dot-md-icon-selector',
    templateUrl: './dot-md-icon-selector.component.html',
    styleUrls: ['./dot-md-icon-selector.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotMdIconSelectorComponent),
            multi: true
        }
    ]
})
export class DotMdIconSelectorComponent implements ControlValueAccessor {
    value = '';

    constructor() {}

    onTouched = () => {};
    onChange = (_) => {};

    onBlur() {
        this.onTouched();
    }

    registerOnTouched(fn: any) {
        this.onTouched = fn;
    }

    registerOnChange(fn: any) {
        this.onChange = fn;
    }

    writeValue(value: string) {
        this.value = value;
    }

    /**
     * Handle web component icon selection
     *
     * @param {CustomEvent<string>} e
     * @memberof DotMdIconSelectorComponent
     */
    onSelect(e: CustomEvent<string>) {
        this.onChange((e.target as HTMLInputElement).value);
    }
}
