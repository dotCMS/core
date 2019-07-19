import { Component, OnInit } from '@angular/core';
import { filter, flatMap, toArray, take } from 'rxjs/operators';
import { FieldType } from '../';
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
    fieldTypes: { clazz: string; name: string }[];

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

                const fieldsFiltered = mappedFields.filter(
                    (field) => field.clazz !== LIVE_DIVIDER_CLAZZ
                );

                const LINE_DIVIDER = mappedFields.find(
                    (field) => field.clazz === LIVE_DIVIDER_CLAZZ
                );

                const COLUMN_BREAK_FIELD = FieldUtil.createColumnBreak();

                this.fieldTypes = [COLUMN_BREAK_FIELD, LINE_DIVIDER, ...fieldsFiltered];
            });
    }
}
