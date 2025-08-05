import { Subject } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Component, Input, OnChanges, OnDestroy, SimpleChanges, inject } from '@angular/core';

import { take, takeUntil } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCMSContentTypeField, DotFieldVariable } from '@dotcms/dotcms-models';

import { DotFieldVariablesService } from './services/dot-field-variables.service';

import { DotKeyValue } from '../../../../../../shared/models/dot-key-value-ng/dot-key-value-ng.model';

@Component({
    selector: 'dot-content-type-fields-variables',
    styleUrls: ['./dot-content-type-fields-variables.component.scss'],
    templateUrl: './dot-content-type-fields-variables.component.html',
    standalone: false
})
export class DotContentTypeFieldsVariablesComponent implements OnChanges, OnDestroy {
    private dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private fieldVariablesService = inject(DotFieldVariablesService);

    @Input() field: DotCMSContentTypeField;
    @Input() showTable = true;

    fieldVariables: DotFieldVariable[] = [];
    blackList = {
        'com.dotcms.contenttype.model.field.ImmutableStoryBlockField': {
            allowedBlocks: true
            // contentAssets: true
        },
        'com.dotcms.contenttype.model.field.ImmutableBinaryField': {
            accept: true,
            systemOptions: true
        }
    };

    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.field?.currentValue) {
            this.initTableData();
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Handle Delete event doing a Delete to the Backend
     * @param {DotKeyValue} variable
     * @memberof DotContentTypeFieldsVariablesComponent
     */
    deleteFieldVariable(variable: DotKeyValue): void {
        this.fieldVariablesService
            .delete(this.field, variable)
            .pipe(take(1))
            .subscribe(
                () => {
                    this.fieldVariables = this.fieldVariables.filter(
                        (item: DotFieldVariable) => item.key !== variable.key
                    );
                },
                (err: HttpErrorResponse) => {
                    this.dotHttpErrorManagerService.handle(err).pipe(take(1)).subscribe();
                }
            );
    }

    /**
     * Handle Save event doing a Post to the Backend
     * @param {DotKeyValue} variable
     * @memberof DotContentTypeFieldsVariablesComponent
     */
    updateFieldVariable(variable: DotKeyValue): void {
        this.fieldVariablesService
            .save(this.field, variable)
            .pipe(take(1))
            .subscribe(
                (savedVariable: DotFieldVariable) => {
                    this.fieldVariables = this.updateVariableCollection(savedVariable);
                },
                (err: HttpErrorResponse) => {
                    this.dotHttpErrorManagerService.handle(err).pipe(take(1)).subscribe();
                }
            );
    }

    private initTableData(): void {
        this.fieldVariablesService
            .load(this.field)
            .pipe(takeUntil(this.destroy$))
            .subscribe((fieldVariables: DotFieldVariable[]) => {
                this.fieldVariables = fieldVariables.filter((item) => {
                    const fieldBlackList = this.blackList[this.field.clazz];
                    if (fieldBlackList) {
                        return !fieldBlackList[item?.key];
                    }

                    return true;
                });
            });
    }

    private updateVariableCollection(savedVariable: DotFieldVariable): DotFieldVariable[] {
        const variableExist = this.fieldVariables.find(
            (item: DotKeyValue) => item.key === savedVariable.key
        );

        return variableExist
            ? this.fieldVariables.map((item: DotFieldVariable) => {
                  if (item.key === savedVariable.key) {
                      item = savedVariable;
                  }

                  return item;
              })
            : [savedVariable, ...this.fieldVariables];
    }
}
