import { Component, OnInit, forwardRef } from '@angular/core';
import { DotEditLayoutService } from '../../../shared/services/dot-edit-layout.service';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { DotLayoutSideBar } from '../../../shared/models/dot-layout-sidebar.model';
import { DotContainerColumnBox } from '../../../shared/models/dot-container-column-box.model';
import { DotMessageService } from '../../../../../api/services/dot-messages-service';

/**
 * Component in charge of update the model that will be used in the sidebar display containers
 *
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-edit-layout-sidebar',
    templateUrl: './dot-edit-layout-sidebar.component.html',
    styleUrls: ['./dot-edit-layout-sidebar.component.scss'],
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotEditLayoutSidebarComponent)
        }
    ]
})
export class DotEditLayoutSidebarComponent implements OnInit, ControlValueAccessor {
    containers: DotContainerColumnBox[];
    value: DotLayoutSideBar;

    constructor(private dotEditLayoutService: DotEditLayoutService, public dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'editpage.layout.designer.sidebar'
            ])
            .subscribe();
    }

    /**
     * Returns DotContainerColumnBox model.
     *
     * @param {DotContainerColumnBox[]} containers
     * @returns {DotLayoutSideBar}
     * @memberof DotEditLayoutSidebarComponent
     */
    getModel(containers: DotContainerColumnBox[]): DotLayoutSideBar {
        if (containers) {
            this.value.containers = containers.map((item) => {
                return {
                    identifier: item.container.identifier,
                    uuid: item.uuid
                };
            });
        }

        return this.value;
    }

    propagateChange = (_: any) => {};

    /**
     * Set the function to be called when the control receives a change event.
     *
     * @param {*} fn
     * @memberof DotEditLayoutSidebarComponent
     */
    registerOnChange(fn: any): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {}

    /**
     * Update model and propagate changes
     *
     * @param {DotContainerColumnBox[]} containers
     * @memberof DotEditLayoutSidebarComponent
     */
    updateAndPropagate(containers?: DotContainerColumnBox[]): void {
        this.propagateChange(containers ? this.getModel(containers) : this.value);
    }

    /**
     * Write a new value to the element
     *
     * @param {DotLayoutSideBar} value
     * @memberof DotEditLayoutSidebarComponent
     */
    writeValue(value: DotLayoutSideBar): void {
        if (value) {
            this.value = value || null;
            this.setContainersValue();
        }
    }

    private setContainersValue(): void {
        this.containers = this.dotEditLayoutService.getDotLayoutSidebar(this.value.containers);
    }
}
