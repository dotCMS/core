import { Observable } from 'rxjs';

import { Component, OnInit } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { map } from 'rxjs/operators';

import { DotAddVariableStore } from '@dotcms/app/portlets/dot-containers/dot-container-create/dot-container-code/dot-add-variable/store/dot-add-variable.store';

import { DotVariableContent, DotVariableList, FilteredFieldTypes } from './dot-add-variable.models';

@Component({
    selector: 'dot-add-variable',
    templateUrl: './dot-add-variable.component.html',
    styleUrls: ['./dot-add-variable.component.scss'],
    providers: [DotAddVariableStore]
})
export class DotAddVariableComponent implements OnInit {
    vm$: Observable<DotVariableList> = this.store.vm$.pipe(
        map((res) => {
            const variables: DotVariableContent[] = res.variables
                .filter(
                    (variable) =>
                        variable.fieldType !== FilteredFieldTypes.Column &&
                        variable.fieldType !== FilteredFieldTypes.Row
                )
                .map((variable) => ({
                    name: variable.name,
                    variable: variable.variable,
                    fieldTypeLabel: variable.fieldTypeLabel
                }));
            variables.push({
                name: 'Content Identifier Value',
                variable: 'ContentIdentifier'
            });

            return { variables };
        })
    );

    constructor(
        private store: DotAddVariableStore,
        private config: DynamicDialogConfig,
        private ref: DynamicDialogRef
    ) {}

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
        this.ref.close();
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
