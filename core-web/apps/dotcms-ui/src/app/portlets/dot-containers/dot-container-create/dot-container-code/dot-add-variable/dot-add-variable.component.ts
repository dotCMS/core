import { Component, OnInit } from '@angular/core';
import { DynamicDialogConfig } from 'primeng/dynamicdialog';
import { DotAddVariableStore } from '@dotcms/app/portlets/dot-containers/dot-container-create/dot-container-code/dot-add-variable/store/dot-add-variable.store';

@Component({
    selector: 'dot-add-variable',
    templateUrl: './dot-add-variable.component.html',
    providers: [DotAddVariableStore]
})
export class DotAddVariableComponent implements OnInit {
    vm$ = this.store.vm$;

    constructor(private store: DotAddVariableStore, private config: DynamicDialogConfig) {}

    ngOnInit() {
        this.store.getVariables(this.config.data?.contentTypeVariable);
    }

    /**
     * handle save button
     * @param {string} variable
     * @returns void
     * @memberof DotAddVariableComponent
     */
    onSave(variable: string): void {
        this.config.data?.onSave?.(this.applyMask(variable));
    }

    /**
     * Applies variable mask to string
     *
     * @param {string} variable
     * @private
     * @returns string
     * @memberof DotAddVariableComponent
     */
    private applyMask(variable: string): string {
        return `$!{dotContentMap.${variable}}`;
    }
}
