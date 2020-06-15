import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';

import { DotCMSContentTypeField, DotCMSContentTypeLayoutRow } from 'dotcms-models';
import { DotMessageService } from '@services/dot-messages-service';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { FieldUtil } from '../util/field-util';

/**
 * Display all the Field Types
 *
 * @export
 * @class FieldTypesContainerComponent
 */
@Component({
    selector: 'dot-content-type-fields-row',
    styleUrls: ['./content-type-fields-row.component.scss'],
    templateUrl: './content-type-fields-row.component.html'
})
export class ContentTypeFieldsRowComponent implements OnInit {
    @Input()
    fieldRow: DotCMSContentTypeLayoutRow;

    @Output()
    editField: EventEmitter<DotCMSContentTypeField> = new EventEmitter();

    @Output()
    removeField: EventEmitter<DotCMSContentTypeField> = new EventEmitter();

    @Output()
    removeRow: EventEmitter<DotCMSContentTypeLayoutRow> = new EventEmitter();

    constructor(
        private dotMessageService: DotMessageService,
        private dotDialogService: DotAlertConfirmService
    ) {}

    ngOnInit() {
        document
            .querySelector('html')
            .style.setProperty(
            '--empty-message',
            `"${this.dotMessageService.get('contenttypes.dropzone.rows.empty.message')}"`
        );
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
            header: `${this.dotMessageService.get('contenttypes.action.delete')} ${
                this.dotMessageService.get('contenttypes.content.field')
            }`,
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
