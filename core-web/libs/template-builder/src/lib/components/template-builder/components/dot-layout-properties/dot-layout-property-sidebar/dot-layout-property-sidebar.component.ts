import { Component, forwardRef, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';

import { DotLayoutSideBar } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotLayoutPropertiesItemComponent } from '../dot-layout-properties-item/dot-layout-properties-item.component';

@Component({
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'dot-layout-property-sidebar',
    templateUrl: './dot-layout-property-sidebar.component.html',
    imports: [DotLayoutPropertiesItemComponent, FormsModule, DotMessagePipe],
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotLayoutSidebarComponent)
        }
    ]
})
export class DotLayoutSidebarComponent implements ControlValueAccessor {
    @ViewChild('propertyItemLeft', { static: true })
    propertyItemLeft: DotLayoutPropertiesItemComponent;

    @ViewChild('propertyItemRight', { static: true })
    propertyItemRight: DotLayoutPropertiesItemComponent;

    value: DotLayoutSideBar;

    propagateChange = (_: unknown) => {
        /* */
    };

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
     * @param () => {} fn
     * @memberof DotLayoutSidebarComponent
     */
    registerOnChange(
        // eslint-disable-next-line @typescript-eslint/no-empty-object-type
        fn: () => {
            /* */
        }
    ): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {
        /* */
    }
}
