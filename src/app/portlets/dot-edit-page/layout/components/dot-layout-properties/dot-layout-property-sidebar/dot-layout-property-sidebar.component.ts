import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Component, forwardRef, ViewChild } from '@angular/core';
import { DotLayoutPropertiesItemComponent } from '../dot-layout-properties-item/dot-layout-properties-item.component';
import { DotLayoutSideBar } from '../../../../shared/models/dot-layout-sidebar.model';

@Component({
    selector: 'dot-layout-property-sidebar',
    templateUrl: './dot-layout-property-sidebar.component.html',
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotLayoutSidebarComponent)
        }
    ]
})
export class DotLayoutSidebarComponent implements ControlValueAccessor {
    @ViewChild('propertyItemLeft') propertyItemLeft: DotLayoutPropertiesItemComponent;

    @ViewChild('propertyItemRight') propertyItemRight: DotLayoutPropertiesItemComponent;

    value: DotLayoutSideBar;

    constructor() {}

    propagateChange = (_: any) => {};

    /**
     * Write a new value to the property item
     * @param any DotLayoutSideBar
     * @memberof DotLayoutSidebarComponent
     */
    writeValue(value): void {
        if (value) {
            this.value = value;
        }
    }

    /**
     * Handle sidebar left/right check and propagate a value
     * @param boolean value
     * @param string location
     * @memberof DotLayoutSidebarComponent
     */
    setValue(value: boolean, location: string): void {
        if (value && location === 'left') {
            this.propertyItemLeft.setChecked();
            this.propertyItemRight.setUnchecked();
        } else if (value && location === 'right') {
            this.propertyItemLeft.setUnchecked();
            this.propertyItemRight.setChecked();
        } else {
            this.value.containers = [];
        }

        this.value.location = value ? location : '';
        this.propagateChange(this.value);
    }

    /**
     * Set the function to be called when the control receives a change event
     * @param any fn
     * @memberof DotLayoutSidebarComponent
     */
    registerOnChange(fn: any): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {}
}
