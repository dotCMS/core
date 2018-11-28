import { Component, Input, SimpleChanges, OnChanges, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { DotFieldVariablesService, DotFieldVariableParams } from '../service/dot-field-variables.service';
import { DotHttpErrorManagerService } from '../../../../api/services/dot-http-error-manager/dot-http-error-manager.service';
import { ResponseView } from 'dotcms-js';
import { take, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs/internal/Subject';
import { Table } from 'primeng/table';
import * as _ from 'lodash';
import { DotContentTypeFieldsVariablesTableRowComponent } from
 './dot-content-type-fields-variables-table-row/dot-content-type-fields-variables-table-row.component';

export interface FieldVariable {
    id?: string;
    clazz?: string;
    fieldId?: string;
    key: string;
    value: string;
}

@Component({
    selector: 'dot-content-type-fields-variables',
    styleUrls: ['./dot-content-type-fields-variables.component.scss'],
    templateUrl: './dot-content-type-fields-variables.component.html'
})
export class DotContentTypeFieldsVariablesComponent implements OnInit, OnChanges, OnDestroy {
    @ViewChild('table')
    table: Table;
    @ViewChild('tableRow') tableRow: DotContentTypeFieldsVariablesTableRowComponent;

    @Input()
    field: DotFieldVariableParams;

    fieldVariables: FieldVariable[] = [];
    fieldVariablesBackup: FieldVariable[] = [];
    messages: { [key: string]: string } = {};
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        public dotMessageService: DotMessageService,
        private fieldVariablesService: DotFieldVariablesService
    ) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'contenttypes.field.variables.key_header.label',
                'contenttypes.field.variables.value_header.label',
                'contenttypes.field.variables.actions_header.label',
                'contenttypes.field.variables.value_no_rows.label'
            ])
            .pipe(take(1))
            .subscribe((messages: { [key: string]: string }) => {
                this.messages = messages;
                this.initTableData();
            });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.field.currentValue && !changes.field.firstChange) {
            this.initTableData();
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Handle keydown event from the table component
     *
     * @param {KeyboardEvent} $event
     * @param {FieldVariable} variable
     * @param {Number} index
     * @memberof ContentTypeFieldsVariablesComponent
     */
    // tslint:disable-next-line:cyclomatic-complexity
    onKeyDown($event: KeyboardEvent, fieldVariable: FieldVariable, index: number): void {
        $event.stopPropagation();
        if ($event.key === 'Escape') {
            this.cancelVariableAction(index);
        } else if ($event.key === 'Enter' && !this.isFieldDisabled(fieldVariable)) {
            // this.saveVariable(fieldVariable, index);
        }
    }

    /**
     * Handle Delete event
     * @param {FieldVariable} variable
     * @param {Number} fieldIndex
     * @memberof ContentTypeFieldsVariablesComponent
     */
    deleteVariable(fieldVariable: FieldVariable, fieldIndex: number): void {
        if (fieldVariable.id) {
            this.deleteExistingVariable(fieldIndex);
        } else {
            this.deleteEmptyVariable(fieldIndex);
        }
    }

    /**
     * Handle Edit button event
     * @param {Number} fieldIndex
     * @memberof ContentTypeFieldsVariablesComponent
     */
    editVariableAction(fieldIndex: number): void {
        const key = document.getElementById(`content-type-fields__variables-key-${fieldIndex}`);
        key.click();
        this.showEditVariableAction(fieldIndex);
    }

    /**
     * Handle Cancel button event
     * @param {Number} fieldIndex
     * @memberof ContentTypeFieldsVariablesComponent
     */
    cancelVariableAction(fieldIndex: number): void {
        const editMenu = document.getElementById(`content-type-fields__variables-actions-edit-${fieldIndex}`);
        const mainMenu = document.getElementById(`content-type-fields__variables-actions-main-${fieldIndex}`);
        mainMenu.style.display = 'block';
        editMenu.style.display = 'none';

        if (this.fieldVariablesBackup[fieldIndex].key === '' && this.fieldVariablesBackup[fieldIndex].value === '') {
            this.deleteVariable(this.fieldVariablesBackup[fieldIndex], fieldIndex);
        } else {
            this.fieldVariablesBackup[fieldIndex] = _.cloneDeep(this.fieldVariables[fieldIndex]);
        }
        this.table.closeCellEdit();
    }

    /**
     * Handle Save event
     * @param {FieldVariable} variable
     * @param {Number} variableIndex
     * @memberof ContentTypeFieldsVariablesComponent
     */
    saveVariable(fieldIndex: number): void {
        if (typeof fieldIndex === 'number') {
            this.updateExistingVariable(this.fieldVariablesBackup[fieldIndex], fieldIndex);
        } else {
            this.addEmptyVariable();
        }
    }

    onDelete(fieldIndex: number): void {
        if (this.fieldVariables[fieldIndex].id) {
            this.fieldVariablesBackup[fieldIndex] = _.cloneDeep(this.fieldVariables[fieldIndex]);
        } else {
            this.deleteVariable(this.fieldVariables[fieldIndex], fieldIndex);
        }
    }

    onCancel(fieldIndex: number): void {
        if (this.fieldVariables[fieldIndex].id) {
            this.fieldVariablesBackup[fieldIndex] = _.cloneDeep(this.fieldVariables[fieldIndex]);
        } else {
            this.deleteVariable(this.fieldVariables[fieldIndex], fieldIndex);
        }
    }

    /**
     * Handle onFocus event on Variable's input fields
     * @param {FieldVariable} fieldVariable
     * @param {Number} index
     * @memberof ContentTypeFieldsVariablesComponent
     */
    editFieldInit(fieldVariable: FieldVariable, index: number): void {
        this.showEditVariableAction(index, this.isFieldDisabled(fieldVariable));
    }

    private showEditVariableAction(fieldIndex: number, disabled?: boolean): void {
        const editMenu = document.getElementById(`content-type-fields__variables-actions-edit-${fieldIndex}`);
        const mainMenu = document.getElementById(`content-type-fields__variables-actions-main-${fieldIndex}`);
        editMenu.style.display = 'block';
        mainMenu.style.display = 'none';
        if (disabled) {
            editMenu.getElementsByTagName('button')[1].disabled = true;
        } else {
            editMenu.getElementsByTagName('button')[1].disabled = false;
        }
    }

    private isFieldDisabled(fieldVariable: FieldVariable): boolean {
        return fieldVariable.key === '' || fieldVariable.value === '' ? true : false;
    }

    private initTableData(): void {
        const params: DotFieldVariableParams = {
            contentTypeId: this.field.contentTypeId,
            fieldId: this.field.fieldId
        };
        this.fieldVariablesService.load(params).pipe(takeUntil(this.destroy$)).subscribe((fieldVariables: FieldVariable[]) => {
            this.fieldVariables = fieldVariables;
            this.fieldVariablesBackup = _.cloneDeep(fieldVariables);
        });
    }

    private deleteExistingVariable(fieldIndex: number): void {
        const params: DotFieldVariableParams = {
            contentTypeId: this.field.contentTypeId,
            fieldId: this.field.fieldId,
            variable: this.fieldVariables[fieldIndex]
        };
        this.fieldVariablesService.delete(params).pipe(take(1)).subscribe(
            () => {
                [this.fieldVariables, this.fieldVariablesBackup] = [this.fieldVariables, this.fieldVariablesBackup]
                    .map((variables: FieldVariable[]) => {
                        return variables.filter(
                            (_item: FieldVariable, index: number) => index !== fieldIndex
                        );
                    });
            },
            (err: ResponseView) => {
                this.dotHttpErrorManagerService.handle(err).pipe(take(1)).subscribe();
            }
        );
    }

    private deleteEmptyVariable(fieldIndex: number): void {
        [this.fieldVariables, this.fieldVariablesBackup] = [this.fieldVariables, this.fieldVariablesBackup]
                .map((variables: FieldVariable[]) => {
                    return variables.filter(
                        (_item: FieldVariable, index: number) => index !== fieldIndex
                    );
                });
    }

    private updateExistingVariable(variable: FieldVariable, variableIndex: number): void {
        const params: DotFieldVariableParams = {
            contentTypeId: this.field.contentTypeId,
            fieldId: this.field.fieldId,
            variable: variable
        };
        this.fieldVariablesService.save(params).pipe(take(1)).subscribe(
            (savedVariable: FieldVariable) => {
                    this.fieldVariables = this.updateVariableCollection(
                        savedVariable,
                        variableIndex
                    );
                this.fieldVariablesBackup = _.cloneDeep(this.fieldVariables);
            },
            (err: ResponseView) => {
                this.dotHttpErrorManagerService.handle(err).pipe(take(1)).subscribe();
            }
        );
    }

    // tslint:disable-next-line:cyclomatic-complexity
    private addEmptyVariable(): void {
        if (this.fieldVariables.length === 0 || (this.fieldVariables.length > 0 && this.fieldVariables[0].key !== '')) {
            const emptyVariable: FieldVariable = {
                key: '',
                value: ''
            };
            this.fieldVariables = [].concat(emptyVariable, this.fieldVariables);
            this.fieldVariablesBackup = [].concat(emptyVariable, this.fieldVariablesBackup);
        }
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
