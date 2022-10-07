import { Component } from '@angular/core';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { DotAddVariableStore } from '@portlets/dot-containers/dot-container-create/dot-content-editor/dot-add-variable/store/dot-add-variable.store';

@Component({
    selector: 'dot-add-variable',
    templateUrl: './dot-add-variable.component.html',
    providers: [DotAddVariableStore]
})
export class DotAddVariableComponent {
    vm$ = this.store.vm$;

    constructor(
        private store: DotAddVariableStore,
        private ref: DynamicDialogRef,
        private config: DynamicDialogConfig
    ) {
        this.store.getVariables(this.contentTypeVariable);
    }

    /**
     * Handle save button
     *
     * @param {string} variable
     * @param {number} activeTabIndex
     * @returns void
     * @memberof DotAddVariableComponent
     */
    onSave(variable: string): void {
        this.config.data?.onSave?.(this.applyMask(variable), this.config.data.activeTabIndex);
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
        return `$!{${variable}}`;
    }

    private get contentTypeVariable() {
        return this.config.data?.contentTypeVariable;
    }
}
