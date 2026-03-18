import { Subject } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import {
    Component,
    OnChanges,
    OnDestroy,
    SimpleChanges,
    inject,
    input,
    signal
} from '@angular/core';

import { take, takeUntil } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotCMSClazzes, DotCMSContentTypeField, DotFieldVariable } from '@dotcms/dotcms-models';
import { DotKeyValueComponent } from '@dotcms/ui';

import { DotFieldVariablesService } from './services/dot-field-variables.service';

import { DotKeyValue } from '../../../../../../shared/models/dot-key-value-ng/dot-key-value-ng.model';

@Component({
    selector: 'dot-content-type-fields-variables',
    templateUrl: './dot-content-type-fields-variables.component.html',
    imports: [DotKeyValueComponent],
    providers: [DotFieldVariablesService]
})
export class DotContentTypeFieldsVariablesComponent implements OnChanges, OnDestroy {
    private dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private fieldVariablesService = inject(DotFieldVariablesService);

    readonly $field = input<DotCMSContentTypeField>(undefined, { alias: 'field' });
    readonly $showTable = input<boolean>(true, { alias: 'showTable' });

    /** Local copy of field for access */
    field: DotCMSContentTypeField;

    $fieldVariables = signal<DotFieldVariable[]>([]);

    blackList = {
        'com.dotcms.contenttype.model.field.ImmutableStoryBlockField': {
            allowedBlocks: true
        },
        'com.dotcms.contenttype.model.field.ImmutableBinaryField': {
            accept: true,
            systemOptions: true
        },
        [DotCMSClazzes.CUSTOM_FIELD]: {
            customFieldOptions: true
        }
    };

    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.$field?.currentValue) {
            this.field = this.$field();
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
                    this.$fieldVariables.update((vars) =>
                        vars.filter((item: DotFieldVariable) => item.key !== variable.key)
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
                    this.$fieldVariables.set(this.updateVariableCollection(savedVariable));
                },
                (err: HttpErrorResponse) => {
                    this.dotHttpErrorManagerService.handle(err).pipe(take(1)).subscribe();
                }
            );
    }

    private initTableData(): void {
        if (!this.field?.contentTypeId || !this.field?.id) {
            this.$fieldVariables.set([]);
            return;
        }

        this.fieldVariablesService
            .load(this.field)
            .pipe(takeUntil(this.destroy$))
            .subscribe((loadedVariables: DotFieldVariable[]) => {
                const loadedKeys = new Set(loadedVariables.map((v) => v.key));
                const fromField = (this.field.fieldVariables || []).filter(
                    (v) => !loadedKeys.has(v.key)
                );
                const combined = [...loadedVariables, ...fromField];
                const filtered = combined.filter((item) => {
                    const fieldBlackList = this.blackList[this.field.clazz];
                    if (fieldBlackList) {
                        return !fieldBlackList[item?.key];
                    }

                    return true;
                });

                this.$fieldVariables.set(filtered);
            });
    }

    private updateVariableCollection(savedVariable: DotFieldVariable): DotFieldVariable[] {
        const current = this.$fieldVariables();
        const variableExist = current.find((item: DotKeyValue) => item.key === savedVariable.key);

        return variableExist
            ? current.map((item: DotFieldVariable) =>
                  item.key === savedVariable.key ? savedVariable : item
              )
            : [savedVariable, ...current];
    }
}
