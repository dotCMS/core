import { FieldService } from '../service';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { filter, flatMap, toArray, takeUntil } from 'rxjs/operators';

import { ContentTypeField, FieldType } from '../';
import { Subject } from 'rxjs/internal/Subject';

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
export class ContentTypesFieldsListComponent implements OnInit, OnDestroy {
    fieldTypes: ContentTypeField[];
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(public fieldService: FieldService) {}

    ngOnInit(): void {

        this.fieldService
            .loadFieldTypes()
            .pipe(
                flatMap((fields: FieldType[]) => fields),
                filter((field: FieldType) => field.id !== 'tab_divider'),
                toArray(),
                takeUntil(this.destroy$))
            .subscribe((fields: FieldType[]) => this.fieldTypes = fields.map(fieldType => {
                return {
                    clazz: fieldType.clazz,
                    name: fieldType.label
                };
            }));
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }
}
