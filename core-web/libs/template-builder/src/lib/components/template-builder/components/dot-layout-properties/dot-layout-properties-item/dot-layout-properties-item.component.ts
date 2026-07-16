import { Component, EventEmitter, forwardRef, HostListener, Input, Output } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
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

    value: boolean;

    @Output()
    switch: EventEmitter<boolean> = new EventEmitter();

    propagateChange = (_: unknown) => {
        /**/
    };

    @HostListener('click')
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
        if (typeof value === 'boolean') {
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
        // eslint-disable-next-line @typescript-eslint/no-empty-object-type
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
