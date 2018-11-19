import { Component, Input, SimpleChanges, OnChanges, OnInit, OnDestroy } from '@angular/core';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { FieldVariablesService, FieldVariableParams } from '../service/';
import { DotHttpErrorManagerService } from '../../../../api/services/dot-http-error-manager/dot-http-error-manager.service';
import { ResponseView } from 'dotcms-js';
import { take, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs/internal/Subject';

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
export class ContentTypeFieldsVariablesComponent implements OnInit, OnChanges, OnDestroy {
    @Input()
    field: FieldVariableParams;

    fieldVariables: FieldVariable[] = [];
    messages: { [key: string]: string } = {};
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        public dotMessageService: DotMessageService,
        private fieldVariablesService: FieldVariablesService
    ) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'contenttypes.field.variables.key_header.label',
                'contenttypes.field.variables.value_header.label',
                'contenttypes.field.variables.actions_header.label',
                'contenttypes.field.variables.value_no_rows.label',
                'contenttypes.action.save',
                'contenttypes.action.cancel'
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
     * Handle Delete event
     * @param {FieldVariable} variable
     * @memberof ContentTypeFieldsVariablesComponent
     */
    deleteVariable(fieldVariable: FieldVariable, fieldIndex: number): void {
        if (fieldVariable.key !== '') {
            const params: FieldVariableParams = {
                contentTypeId: this.field.contentTypeId,
                fieldId: this.field.fieldId,
                variable: this.fieldVariables[fieldIndex]
            };
            this.fieldVariablesService.delete(params).pipe(take(1)).subscribe(
                () => {
                    this.fieldVariables = this.fieldVariables.filter(
                        (_item: FieldVariable, index: number) => index !== fieldIndex
                    );
                },
                (err: ResponseView) => {
                    this.dotHttpErrorManagerService.handle(err).pipe(take(1)).subscribe();
                }
            );
        } else {
            this.fieldVariables = this.fieldVariables.filter(
                (_item: FieldVariable, index: number) => index !== fieldIndex
            );
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
        if (typeof variableIndex !== 'undefined') {
            this.fieldVariablesService.save(params).pipe(take(1)).subscribe(
                (savedVariable: FieldVariable) => {
                    if (typeof variableIndex !== 'undefined') {
                        this.fieldVariables = this.updateVariableCollection(
                            savedVariable,
                            variableIndex
                        );
                    } else {
                        this.fieldVariables = [].concat(savedVariable, this.fieldVariables);
                    }
                },
                (err: ResponseView) => {
                    this.dotHttpErrorManagerService.handle(err).pipe(take(1)).subscribe();
                }
            );
        } else {
            const emptyVariable: FieldVariable = {
                key: '',
                value: ''
            };
            this.fieldVariables = [].concat(emptyVariable, this.fieldVariables);
        }
    }

    /**
     * Handle Add new variable button event
     * @memberof ContentTypeFieldsVariablesComponent
     */
    addVariable(): void {
        this.saveVariable(null);
        setTimeout(() => {
            this.editVariableAction(0);
        });
    }

    /**
     * Handle onFocus event on Variable's input fields
     * @param {FieldVariable} fieldVariable
     * @param {Number} index
     * @memberof ContentTypeFieldsVariablesComponent
     */
    editFieldInit(fieldVariable: FieldVariable, index: number): void {
        const disabled = fieldVariable.key === '' || fieldVariable.value === '' ? true : false;
        this.showEditVariableAction(index, disabled);
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

    private initTableData(): void {
        const params: FieldVariableParams = {
            contentTypeId: this.field.contentTypeId,
            fieldId: this.field.fieldId
        };
        this.fieldVariablesService.load(params).pipe(takeUntil(this.destroy$)).subscribe((fieldVariables: FieldVariable[]) => {
            this.fieldVariables = fieldVariables;
        });
    }

    private updateVariableCollection(
        savedVariable: FieldVariable,
        variableIndex?: number
    ): FieldVariable[] {
        return this.fieldVariables.map((item, index) => {
            if (index === variableIndex) {
                item = savedVariable;
            }
            return item;
        });
    }
}
