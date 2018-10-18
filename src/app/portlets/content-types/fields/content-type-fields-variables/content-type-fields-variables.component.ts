import { Component, Input, SimpleChanges, OnChanges, OnInit } from '@angular/core';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { FieldVariablesService, FieldVariableParams } from '../service/';
import { DotHttpErrorManagerService } from '../../../../api/services/dot-http-error-manager/dot-http-error-manager.service';
import { ResponseView } from 'dotcms-js/dotcms-js';

export interface FieldVariable {
    id?: string;
    clazz?: string;
    fieldId?: string;
    key: string;
    value: string;
}

@Component({
    selector: 'dot-content-type-fields-variables',
    styleUrls: ['./content-type-fields-variables.component.scss'],
    templateUrl: './content-type-fields-variables.component.html'
})
export class ContentTypeFieldsVariablesComponent implements OnInit, OnChanges {
    @Input() field: FieldVariableParams;

    fieldVariables: FieldVariable[] = [];
    messages: {[key: string]: string} = {};
    canSaveFields: boolean[] | null[] = [];

    constructor(
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        public dotMessageService: DotMessageService,
        private fieldVariablesService: FieldVariablesService,
    ) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'contenttypes.field.variables.key_header.label',
                'contenttypes.field.variables.value_header.label',
                'contenttypes.field.variables.actions_header.label',
                'contenttypes.field.variables.value_no_rows.label'
            ])
            .subscribe((messages: {[key: string]: string}) => {
                this.messages = messages;
                this.initTableData();
            });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.field.currentValue && !changes.field.firstChange) {
            this.initTableData();
        }
    }

    /**
     * Handle Delete event
     * @param {FieldVariable} variable
     * @memberof ContentTypeFieldsVariablesComponent
     */
    deleteVariable(fieldIndex: number): void {
        const params: FieldVariableParams = {
            contentTypeId: this.field.contentTypeId,
            fieldId: this.field.fieldId,
            variable: this.fieldVariables[fieldIndex]
        };
        this.fieldVariablesService.delete(params)
            .subscribe(() => {
                this.fieldVariables = this.fieldVariables.filter((_item: FieldVariable, index: number) => index !== fieldIndex);
                this.canSaveFields.splice(fieldIndex, 1);
            }, (err: ResponseView) => {
                this.dotHttpErrorManagerService.handle(err).subscribe();
            });
    }

    /**
     * Handle Save event
     * @param {FieldVariable} variable
     * @memberof ContentTypeFieldsVariablesComponent
     */
    saveVariable(variable: FieldVariable, variableIndex?: number): void {
        const params: FieldVariableParams = {
            contentTypeId: this.field.contentTypeId,
            fieldId: this.field.fieldId,
            variable: variable
        };
        this.fieldVariablesService.save(params)
            .subscribe((savedVariable: FieldVariable) => {
                if (typeof variableIndex !== 'undefined') {
                    this.fieldVariables = this.updateVariableCollection(savedVariable, variableIndex);
                } else {
                    this.fieldVariables = [].concat(savedVariable, this.fieldVariables);
                    this.canSaveFields = [].concat(null, this.canSaveFields);
                }
            }, (err: ResponseView) => {
                this.dotHttpErrorManagerService.handle(err).subscribe();
            });
    }

    /**
     * Holds status if a existing variable meets conditions to be updated
     * @param {any} event
     * @memberof ContentTypeFieldsVariablesComponent
     */
    updateSaveButtons(event: any): void {
        this.canSaveFields[event.index] = event.data.key.length > 0 && event.data.value.length > 0 ? null : true;
    }

    private initTableData(): void {
        const params: FieldVariableParams = {
            contentTypeId: this.field.contentTypeId,
            fieldId: this.field.fieldId
        };
        this.fieldVariablesService.load(params).subscribe((fieldVariables: FieldVariable[]) => {
            this.fieldVariables = fieldVariables;
        });
    }

    private updateVariableCollection(savedVariable: FieldVariable, variableIndex?: number): FieldVariable[] {
        return this.fieldVariables.map((item, index) => {
            if (index === variableIndex) {
                item = savedVariable;
            }
            return item;
        });
    }

}
