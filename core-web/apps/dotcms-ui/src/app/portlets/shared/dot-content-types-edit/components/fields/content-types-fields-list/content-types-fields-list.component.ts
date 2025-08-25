import { Component, inject, Input, OnInit, signal } from '@angular/core';

import { filter, mergeMap, take, toArray } from 'rxjs/operators';

import { DotCMSClazz, DotCMSClazzes } from '@dotcms/dotcms-models';
import { FieldUtil } from '@dotcms/utils';

import { FIELD_ICONS } from './content-types-fields-icon-map';

import { FieldType } from '..';
import { FieldService } from '../service';
/**
 * Show all the Field Types
 *
 * @export
 * @class FieldTypesContainerComponent
 */
@Component({
    selector: 'dot-content-types-fields-list',
    styleUrls: ['./content-types-fields-list.component.scss'],
    templateUrl: './content-types-fields-list.component.html',
    standalone: false
})
export class ContentTypesFieldsListComponent implements OnInit {
    @Input() baseType: string;

    $fieldTypes = signal<{ clazz: string; name: string }[]>([]);
    fieldIcons = FIELD_ICONS;

    #dotFormFields: DotCMSClazz[] = [
        DotCMSClazzes.BINARY,
        DotCMSClazzes.CHECKBOX,
        DotCMSClazzes.DATE,
        DotCMSClazzes.DATE_AND_TIME,
        DotCMSClazzes.TIME,
        DotCMSClazzes.KEY_VALUE,
        DotCMSClazzes.MULTI_SELECT,
        DotCMSClazzes.RADIO,
        DotCMSClazzes.SELECT,
        DotCMSClazzes.TAG,
        DotCMSClazzes.TEXTAREA,
        DotCMSClazzes.TEXT
    ];

    #backListFields = ['relationships_tab', 'permissions_tab', 'tab_divider'];

    readonly #fieldService = inject(FieldService);

    ngOnInit(): void {
        this.#fieldService
            .loadFieldTypes()
            .pipe(
                mergeMap((fields: FieldType[]) => fields),
                filter((field: FieldType) => !this.#backListFields.includes(field.id)),
                toArray(),
                take(1)
            )
            .subscribe((fields: FieldType[]) => {
                const mappedFields = fields.map((fieldType: FieldType) => {
                    return {
                        clazz: fieldType.clazz,
                        name: fieldType.label
                    };
                });
                let fieldsFiltered = mappedFields.filter(
                    (field) => field.clazz !== DotCMSClazzes.LINE_DIVIDER
                );
                if (this.baseType === 'FORM') {
                    fieldsFiltered = fieldsFiltered.filter((field) => this.isFormField(field));
                }

                const LINE_DIVIDER = mappedFields.find(
                    (field) => field.clazz === DotCMSClazzes.LINE_DIVIDER
                );

                const COLUMN_BREAK_FIELD = FieldUtil.createColumnBreak();
                this.$fieldTypes.set([COLUMN_BREAK_FIELD, LINE_DIVIDER, ...fieldsFiltered]);
            });
    }

    private isFormField(field: { clazz: string; name: string }): boolean {
        return this.#dotFormFields.includes(field.clazz as DotCMSClazz);
    }
}
