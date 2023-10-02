import { Observable } from 'rxjs';

import { Component, OnInit, inject } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { map } from 'rxjs/operators';

import { DotAddVariableStore } from '@dotcms/app/portlets/dot-containers/dot-container-create/dot-container-code/dot-add-variable/store/dot-add-variable.store';
import { DotMessageService } from '@dotcms/data-access';

import { DotVariableContent, DotVariableList, FilteredFieldTypes } from './dot-add-variable.models';

@Component({
    selector: 'dot-add-variable',
    templateUrl: './dot-add-variable.component.html',
    styleUrls: ['./dot-add-variable.component.scss'],
    providers: [DotAddVariableStore]
})
export class DotAddVariableComponent implements OnInit {
    private readonly dotMessage = inject(DotMessageService);
    private readonly store = inject(DotAddVariableStore);
    private readonly config = inject(DynamicDialogConfig);
    private readonly ref = inject(DynamicDialogRef);

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
                name: this.dotMessage.get('Content-Identifier-value'),
                variable: 'ContentIdentifier'
            });

            return { variables };
        })
    );

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
