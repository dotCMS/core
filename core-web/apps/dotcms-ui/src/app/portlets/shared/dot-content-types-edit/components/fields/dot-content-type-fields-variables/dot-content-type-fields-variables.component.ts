import { Subject } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
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
import {
    CUSTOM_FIELD_OPTIONS_KEY,
    DotCMSClazzes,
    DotCMSContentTypeField,
    DotFieldVariable,
    HIDE_LABEL_VARIABLE_KEY
} from '@dotcms/dotcms-models';
import { DotKeyValueComponent } from '@dotcms/ui';

import { DotFieldVariablesService } from './services/dot-field-variables.service';

import { DotKeyValue } from '../../../../../../shared/models/dot-key-value-ng/dot-key-value-ng.model';

/**
 * Displays and manages free-form field variables for a content-type field.
 * Filters out reserved keys that are managed by dedicated settings sections
 * (e.g. `customFieldOptions`, `hideLabel` for Custom Fields; `allowedBlocks` for Block Editor).
 */
@Component({
    selector: 'dot-content-type-fields-variables',
    templateUrl: './dot-content-type-fields-variables.component.html',
    imports: [DotKeyValueComponent],
    providers: [DotFieldVariablesService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentTypeFieldsVariablesComponent implements OnChanges, OnDestroy {
    private dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private fieldVariablesService = inject(DotFieldVariablesService);

    /** The content-type field whose variables are loaded and managed. */
    readonly $field = input<DotCMSContentTypeField>(undefined, { alias: 'field' });

    /** When `false`, hides the key-value table (used to embed without the table UI). */
    readonly $showTable = input<boolean>(true, { alias: 'showTable' });

    /** Local snapshot of the field, updated on every `$field` change. */
    field: DotCMSContentTypeField;

    /** Signal holding the list of variables currently shown in the table. */
    $fieldVariables = signal<DotFieldVariable[]>([]);

    /**
     * Per-field-type map of variable keys that must be hidden from the table.
     * These keys are owned by dedicated settings sections and should not be edited here.
     */
    blackList = {
        'com.dotcms.contenttype.model.field.ImmutableStoryBlockField': {
            allowedBlocks: true
            // contentAssets: true
        },
        'com.dotcms.contenttype.model.field.ImmutableBinaryField': {
            accept: true,
            systemOptions: true
        },
        [DotCMSClazzes.CUSTOM_FIELD]: {
            [CUSTOM_FIELD_OPTIONS_KEY]: true,
            [HIDE_LABEL_VARIABLE_KEY]: true
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
            .subscribe({
                next: () => {
                    this.$fieldVariables.set(
                        this.$fieldVariables().filter(
                            (item: DotFieldVariable) => item.key !== variable.key
                        )
                    );
                },
                error: (err: HttpErrorResponse) => {
                    this.dotHttpErrorManagerService.handle(err).pipe(take(1)).subscribe();
                }
            });
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
            .subscribe({
                next: (savedVariable: DotFieldVariable) => {
                    this.$fieldVariables.set(this.updateVariableCollection(savedVariable));
                },
                error: (err: HttpErrorResponse) => {
                    this.dotHttpErrorManagerService.handle(err).pipe(take(1)).subscribe();
                }
            });
    }

    private initTableData(): void {
        if (!this.field?.contentTypeId || !this.field?.id) {
            this.$fieldVariables.set([]);
            return;
        }

        this.fieldVariablesService
            .load(this.field)
            .pipe(takeUntil(this.destroy$))
            .subscribe(($fieldVariables: DotFieldVariable[]) => {
                this.$fieldVariables.set(
                    $fieldVariables.filter((item) => {
                        const fieldBlackList = this.blackList[this.field.clazz];
                        if (fieldBlackList) {
                            return !fieldBlackList[item?.key];
                        }

                        return true;
                    })
                );
            });
    }

    private updateVariableCollection(savedVariable: DotFieldVariable): DotFieldVariable[] {
        const current = this.$fieldVariables();
        const variableExist = current.find((item: DotKeyValue) => item.key === savedVariable.key);

        return variableExist
            ? current.map((item: DotFieldVariable) => {
                  if (item.key === savedVariable.key) {
                      item = savedVariable;
                  }

                  return item;
              })
            : [savedVariable, ...current];
    }
}
