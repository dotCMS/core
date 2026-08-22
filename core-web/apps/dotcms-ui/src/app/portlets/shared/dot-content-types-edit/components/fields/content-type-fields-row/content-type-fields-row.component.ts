import { Component, OnInit, inject, input, output, ChangeDetectionStrategy } from '@angular/core';

import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField, DotCMSContentTypeLayoutRow } from '@dotcms/dotcms-models';
import { FieldUtil } from '@dotcms/utils';

/**
 * A layout row that has columns.
 *
 * `DotCMSContentTypeLayoutRow.columns` is optional because a tab divider row has none (see
 * `FieldUtil.createFieldTabDivider`), but the parent renders this component only inside
 * `@if (row.columns && row.columns.length)` — a row without columns goes to
 * `dot-content-type-fields-tab` instead.
 */
type FieldRowWithColumns = DotCMSContentTypeLayoutRow &
    Required<Pick<DotCMSContentTypeLayoutRow, 'columns'>>;

/**
 * Display all the Field Types
 *
 * @export
 * @class FieldTypesContainerComponent
 */
@Component({
    selector: 'dot-content-type-fields-row',
    templateUrl: './content-type-fields-row.component.html',
    standalone: false,
    changeDetection: ChangeDetectionStrategy.Eager,
    host: {
        class: 'block relative mb-2 last:mb-0 transition-shadow duration-200'
    }
})
export class ContentTypeFieldsRowComponent implements OnInit {
    private dotMessageService = inject(DotMessageService);
    private dotDialogService = inject(DotAlertConfirmService);

    readonly $fieldRow = input.required<DotCMSContentTypeLayoutRow>({ alias: 'fieldRow' });

    readonly editField = output<DotCMSContentTypeField>();
    readonly removeField = output<DotCMSContentTypeField>();
    readonly removeRow = output<DotCMSContentTypeLayoutRow>();

    /** Local copy of fieldRow for mutations */
    fieldRow!: FieldRowWithColumns;

    emptyMessage = '';

    ngOnInit() {
        this.fieldRow = this.$fieldRow() as FieldRowWithColumns;
        this.emptyMessage = this.dotMessageService.get('contenttypes.dropzone.rows.empty.message');
    }

    /**
     * Remove a field
     *
     * @param DotContentTypeField field
     * @memberof ContentTypeFieldsRowComponent
     */
    onRemoveField(field: DotCMSContentTypeField): void {
        this.dotDialogService.confirm({
            accept: () => {
                this.removeField.emit(field);
            },
            header: `${this.dotMessageService.get(
                'contenttypes.action.delete'
            )} ${this.dotMessageService.get('contenttypes.content.field')}`,
            message: this.dotMessageService.get(
                'contenttypes.confirm.message.delete.field',
                field.name
            ),
            footerLabel: {
                accept: this.dotMessageService.get('contenttypes.action.delete'),
                reject: this.dotMessageService.get('contenttypes.action.cancel')
            }
        });
    }

    /**
     * Handle remove row event or remove column
     *
     * @param {DotCMSContentTypeLayoutColumn} [column]
     * @memberof ContentTypeFieldsRowComponent
     */
    remove(index: number): void {
        if (this.hasMoreThanOneColumn()) {
            this.removeColumn(index);
        } else {
            this.removeRow.emit(this.fieldRow);
        }
    }

    private removeColumn(index: number): void {
        const field = this.fieldRow.columns[index].columnDivider;

        if (FieldUtil.isNewField(field)) {
            this.removeLocalColumn(index);
        } else {
            this.removeField.emit(field);
        }
    }

    private removeLocalColumn(index: number): void {
        this.fieldRow.columns.splice(index, 1);
    }

    private hasMoreThanOneColumn(): boolean {
        return this.fieldRow.columns.length > 1;
    }
}
