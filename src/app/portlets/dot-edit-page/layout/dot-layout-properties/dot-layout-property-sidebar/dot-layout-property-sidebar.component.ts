import { MessageService } from '../../../../../api/services/messages-service';
import { DotLayoutPropertiesItemComponent } from '../dot-layout-properties-item/dot-layout-properties-item.component';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormGroup } from '@angular/forms';
import { Component, forwardRef, ViewEncapsulation, Input, group, ViewChild, OnInit } from '@angular/core';

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
export class DotLayoutSidebarComponent implements ControlValueAccessor, OnInit {
    @ViewChild('propertyItemLeft') propertyItemLeft: DotLayoutPropertiesItemComponent;
    @ViewChild('propertyItemRight') propertyItemRight: DotLayoutPropertiesItemComponent;
    value: string;

    constructor(public messageService: MessageService) {}

    ngOnInit() {
        this.messageService.getMessages([
            'editpage.layout.properties.sidebar.left',
            'editpage.layout.properties.sidebar.right',
        ]).subscribe();
    }

    propagateChange = (_: any) => {};

    /**
     * Write a new value to the property item
     * @param {any} value
     * @memberof DotLayoutSidebarComponent
     */
    writeValue(value): void {
        if (value) {
            this.value = value;
        }
    }

    /**
     * Handle sidebar left/right check and propagate a value
     * @param {boolean} value
     * @param {string} location
     * @memberof DotLayoutSidebarComponent
     */
    setValue(value: boolean, location: string): void {
        if (value && location === 'left') {
            this.propertyItemLeft.setChecked();
            this.propertyItemRight.setUnchecked();
        }

        if (value && location === 'right') {
            this.propertyItemLeft.setUnchecked();
            this.propertyItemRight.setChecked();
        }

        this.value = value ? location : '';
        this.propagateChange(this.value);
    }

    /**
     * Set the function to be called when the control receives a change event
     * @param {any} fn
     * @memberof DotLayoutSidebarComponent
     */
    registerOnChange(fn: any): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {}
 }
