import { Component, forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { DotEditLayoutService } from '@dotcms/app/api/services/dot-edit-layout/dot-edit-layout.service';
import { DotTemplateContainersCacheService } from '@dotcms/app/api/services/dot-template-containers-cache/dot-template-containers-cache.service';
import { DotContainerColumnBox } from '@dotcms/app/shared/models/dot-edit-layout-designer';
import { DotLayoutSideBar } from '@dotcms/dotcms-models';

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
export class DotEditLayoutSidebarComponent implements ControlValueAccessor {
    containers: DotContainerColumnBox[];
    value: DotLayoutSideBar;

    constructor(
        private dotEditLayoutService: DotEditLayoutService,
        private templateContainersCacheService: DotTemplateContainersCacheService
    ) {}

    /**
     * Returns DotContainerColumnBox model.
     *
     * @param DotContainerColumnBox[] containers
     * @returns DotLayoutSideBar
     * @memberof DotEditLayoutSidebarComponent
     */
    getModel(containers: DotContainerColumnBox[]): DotLayoutSideBar {
        if (containers) {
            this.value.containers = containers.map((item) => {
                return {
                    identifier: this.templateContainersCacheService.getContainerReference(
                        item.container
                    ),
                    uuid: item.uuid
                };
            });
        }

        return this.value;
    }

    propagateChange = (_: unknown) => {
        /**/
    };

    /**
     * Set the function to be called when the control receives a change event.
     *
     * @param ()=>{} fn
     * @memberof DotEditLayoutSidebarComponent
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

    /**
     * Update model and propagate changes
     *
     * @param DotContainerColumnBox[] containers
     * @memberof DotEditLayoutSidebarComponent
     */
    updateAndPropagate(containers?: DotContainerColumnBox[]): void {
        this.propagateChange(containers ? this.getModel(containers) : this.value);
    }

    /**
     * Write a new value to the element
     *
     * @param DotLayoutSideBar value
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
