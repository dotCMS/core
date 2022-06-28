import { Component, OnInit, ViewChild, forwardRef, Output, EventEmitter } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotLayoutSideBar } from '@models/dot-edit-layout-designer';
import { OverlayPanel } from 'primeng/overlaypanel';

@Component({
    selector: 'dot-sidebar-properties',
    templateUrl: './dot-sidebar-properties.component.html',
    styleUrls: ['./dot-sidebar-properties.component.scss'],
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotSidebarPropertiesComponent)
        }
    ]
})
export class DotSidebarPropertiesComponent implements OnInit, ControlValueAccessor {
    value: DotLayoutSideBar;
    @ViewChild('overlay', { static: true }) overlay: OverlayPanel;
    @Output() switch: EventEmitter<string> = new EventEmitter();

    constructor(private dotEventsService: DotEventsService) {}

    propagateChange = (_: unknown) => {
        /**/
    };

    ngOnInit() {
        this.value = {
            containers: [],
            location: '',
            width: ''
        };
    }

    /**
     * Hides overlay panel and emits a notification to repainted the Grid
     *
     * @memberof DotSidebarPropertiesComponent
     */
    changeSidebarSize(): void {
        this.overlay.hide();
        this.dotEventsService.notify('layout-sidebar-change');
        this.switch.emit();
    }

    /**
     * Write a new value to the property item
     * @param DotLayoutSideBar value
     * @memberof DotSidebarPropertiesComponent
     */
    writeValue(value: DotLayoutSideBar): void {
        if (value) {
            this.value = value;
        }
    }

    /**
     * Set the function to be called when the control receives a change event
     * @param () => {} fn
     * @memberof DotSidebarPropertiesComponent
     */
    registerOnChange(
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
