import { Component } from '@angular/core';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

@Component({
    selector: 'dot-add-variable',
    templateUrl: './dot-add-variable.component.html',
    styleUrls: ['./dot-add-variable.component.scss']
})
export class DotAddVariableComponent {
    variables = [1, 2, 3];
    constructor(private ref: DynamicDialogRef, private config: DynamicDialogConfig) {
        //
    }

    /**
     * Handle save button
     *
     * @memberof DotAddVariableComponent
     */
    onSave(variable: string): void {
        this.config.data?.onSave?.(variable);
    }
}
