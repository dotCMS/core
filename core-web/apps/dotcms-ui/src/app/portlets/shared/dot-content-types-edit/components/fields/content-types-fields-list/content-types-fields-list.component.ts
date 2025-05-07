import { Component, inject, Input, OnInit, signal } from '@angular/core';

import { filter, mergeMap, take, toArray } from 'rxjs/operators';

import { FieldUtil } from '@dotcms/utils-testing';
import { FIELD_ICONS } from '@portlets/shared/dot-content-types-edit/components/fields/content-types-fields-list/content-types-fields-icon-map';

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
    templateUrl: './content-types-fields-list.component.html'
})
export class ContentTypesFieldsListComponent implements OnInit {
    @Input() baseType: string;

    $fieldTypes = signal<{ clazz: string; name: string }[]>([]);
    fieldIcons = FIELD_ICONS;

    #dotFormFields = [
        'com.dotcms.contenttype.model.field.ImmutableBinaryField',
        'com.dotcms.contenttype.model.field.ImmutableCheckboxField',
        'com.dotcms.contenttype.model.field.ImmutableDateField',
        'com.dotcms.contenttype.model.field.ImmutableDateTimeField',
        'com.dotcms.contenttype.model.field.ImmutableTimeField',
        'com.dotcms.contenttype.model.field.ImmutableKeyValueField',
        'com.dotcms.contenttype.model.field.ImmutableMultiSelectField',
        'com.dotcms.contenttype.model.field.ImmutableRadioField',
        'com.dotcms.contenttype.model.field.ImmutableSelectField',
        'com.dotcms.contenttype.model.field.ImmutableTagField',
        'com.dotcms.contenttype.model.field.ImmutableTextAreaField',
        'com.dotcms.contenttype.model.field.ImmutableTextField'
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
                const LIVE_DIVIDER_CLAZZ =
                    'com.dotcms.contenttype.model.field.ImmutableLineDividerField';

                const mappedFields = fields.map((fieldType: FieldType) => {
                    return {
                        clazz: fieldType.clazz,
                        name: fieldType.label
                    };
                });
                let fieldsFiltered = mappedFields.filter(
                    (field) => field.clazz !== LIVE_DIVIDER_CLAZZ
                );
                if (this.baseType === 'FORM') {
                    fieldsFiltered = fieldsFiltered.filter((field) => this.isFormField(field));
                }

                const LINE_DIVIDER = mappedFields.find(
                    (field) => field.clazz === LIVE_DIVIDER_CLAZZ
                );

                const COLUMN_BREAK_FIELD = FieldUtil.createColumnBreak();
                this.$fieldTypes.set([COLUMN_BREAK_FIELD, LINE_DIVIDER, ...fieldsFiltered]);
            });
    }

    private isFormField(field: { clazz: string; name: string }): boolean {
        return this.#dotFormFields.includes(field.clazz);
    }
}
