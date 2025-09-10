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
    ],
    standalone: false
})
export class DotMdIconSelectorComponent implements ControlValueAccessor {
    value = '';

    onTouched = () => {
        //
    };
    onChange = (_) => {
        /* */
    };

    onBlur() {
        this.onTouched();
    }

    registerOnTouched(fn: () => void) {
        this.onTouched = fn;
    }

    registerOnChange(fn: () => void) {
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
