import {
    Component,
    forwardRef,
    CUSTOM_ELEMENTS_SCHEMA,
    ChangeDetectionStrategy
} from '@angular/core';
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
    imports: [],
    changeDetection: ChangeDetectionStrategy.Eager,
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class DotMdIconSelectorComponent implements ControlValueAccessor {
    value = '';

    onTouched = () => {
        //
    };
    onChange = (_: string) => {
        /* */
    };

    onBlur() {
        this.onTouched();
    }

    registerOnTouched(fn: () => void) {
        this.onTouched = fn;
    }

    registerOnChange(fn: (value: string) => void) {
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
    onSelect(e: CustomEvent<{ name: string; value: string; colorValue: string }>) {
        this.onChange((e.target as HTMLInputElement).value);
    }
}
