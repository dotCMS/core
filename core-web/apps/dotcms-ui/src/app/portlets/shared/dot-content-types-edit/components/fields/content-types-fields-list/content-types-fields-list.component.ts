import { Component, Input, OnInit } from '@angular/core';
import { filter, flatMap, toArray, take } from 'rxjs/operators';
import { FieldType } from '..';
import { FieldService } from '../service';
import { FieldUtil } from '../util/field-util';
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

    fieldTypes: { clazz: string; name: string }[];

    private dotFormFields = [
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

    constructor(public fieldService: FieldService) {}

    ngOnInit(): void {
        this.fieldService
            .loadFieldTypes()
            .pipe(
                flatMap((fields: FieldType[]) => fields),
                filter((field: FieldType) => field.id !== 'tab_divider'),
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
                this.fieldTypes = [COLUMN_BREAK_FIELD, LINE_DIVIDER, ...fieldsFiltered];
            });
    }

    private isFormField(field: { clazz: string; name: string }): boolean {
        return this.dotFormFields.includes(field.clazz);
    }
}
