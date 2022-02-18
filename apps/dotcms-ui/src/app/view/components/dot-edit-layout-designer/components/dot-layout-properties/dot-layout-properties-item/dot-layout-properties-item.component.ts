import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Component, Input, Output, EventEmitter, forwardRef, HostListener } from '@angular/core';

@Component({
    selector: 'dot-layout-properties-item',
    templateUrl: './dot-layout-properties-item.component.html',
    styleUrls: ['./dot-layout-properties-item.component.scss'],
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotLayoutPropertiesItemComponent)
        }
    ]
})
export class DotLayoutPropertiesItemComponent implements ControlValueAccessor {
    @Input()
    label: string;

    @Output()
    switch: EventEmitter<boolean> = new EventEmitter();

    value: boolean;

    propagateChange = (_: unknown) => {
        /**/
    };

    @HostListener('click', ['$event'])
    onClick() {
        this.value = !this.value;
        this.propagateChange(this.value);
        this.switch.emit(this.value);
    }

    /**
     * Write a new value to the property item
     * @param boolean value
     * @memberof DotLayoutPropertiesItemComponent
     */
    writeValue(value: boolean): void {
        if (value) {
            this.value = value;
        }
    }

    /**
     * Check item and set value to true
     * @memberof DotLayoutPropertiesItemComponent
     */
    setChecked() {
        this.value = true;
    }

    /**
     * Uncheck item and set value to false
     * @memberof DotLayoutPropertiesItemComponent
     */
    setUnchecked() {
        this.value = false;
    }

    /**
     * Set the function to be called when the control receives a change event
     * @param ()=> {} fn
     * @memberof DotLayoutPropertiesItemComponent
     */
    registerOnChange(
        fn: () => {
            /**/
        }
    ): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {
        /**/
    }
}
