import {
    Component,
    SimpleChanges,
    Input,
    Output,
    EventEmitter,
    OnInit,
    OnChanges,
    OnDestroy,
    ViewChild
} from '@angular/core';
import { FieldDragDropService, DropFieldData } from '../service';
import { DotContentTypeField, FieldType, DotContentTypeLayoutDivider } from '../models';
import { ContentTypeFieldsPropertiesFormComponent } from '../content-type-fields-properties-form';
import { DotMessageService } from '@services/dot-messages-service';
import { FieldUtil } from '../util/field-util';
import { FieldPropertyService } from '../service/field-properties.service';
import { DotDialogActions } from '@components/dot-dialog/dot-dialog.component';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { takeUntil, take  } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { DotLoadingIndicatorService } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.service';
import * as _ from 'lodash';

/**
 * Display all the Field Types
 *
 * @export
 * @class ContentTypeFieldsDropZoneComponent
 */
@Component({
    selector: 'dot-content-type-fields-drop-zone',
    styleUrls: ['./content-type-fields-drop-zone.component.scss'],
    templateUrl: './content-type-fields-drop-zone.component.html'
})
export class ContentTypeFieldsDropZoneComponent implements OnInit, OnChanges, OnDestroy {
    readonly OVERVIEW_TAB_INDEX = 0;

    dialogActiveTab: number;
    displayDialog = false;
    currentField: DotContentTypeField;
    currentFieldType: FieldType;
    dialogActions: DotDialogActions;
    fieldRows: DotContentTypeLayoutDivider[];

    @ViewChild('fieldPropertiesForm')
    propertiesForm: ContentTypeFieldsPropertiesFormComponent;

    @Input()
    layout: DotContentTypeLayoutDivider[];

    @Output()
    saveFields = new EventEmitter<DotContentTypeLayoutDivider[]>();
    @Output()
    editField = new EventEmitter<DotContentTypeField>();
    @Output()
    removeFields = new EventEmitter<DotContentTypeField[]>();

    hideButtons = false;

    i18nMessages: {
        [key: string]: string;
    } = {};

    private _loading: boolean;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        public dotMessageService: DotMessageService,
        private fieldDragDropService: FieldDragDropService,
        private fieldPropertyService: FieldPropertyService,
        private dotEventsService: DotEventsService,
        private dotLoadingIndicatorService: DotLoadingIndicatorService
    ) {}

    ngOnInit(): void {
        this.dotMessageService
            .getMessages([
                'contenttypes.dropzone.action.save',
                'contenttypes.dropzone.action.cancel',
                'contenttypes.dropzone.action.edit',
                'contenttypes.dropzone.action.create.field',
                'contenttypes.dropzone.empty.message',
                'contenttypes.dropzone.tab.overview',
                'contenttypes.dropzone.tab.variables',
                'contenttypes.dropzone.empty.message'
            ])
            .pipe(take(1))
            .subscribe((messages: { [key: string]: string }) => {
                this.i18nMessages = messages;
                this.dialogActions = {
                    accept: {
                        action: () => {
                            this.propertiesForm.saveFieldProperties();
                        },
                        label: this.i18nMessages['contenttypes.dropzone.action.save'],
                        disabled: true
                    },
                    cancel: {
                        label: this.i18nMessages['contenttypes.dropzone.action.cancel'],
                        action: () => {}
                    }
                };
            });

        this.fieldDragDropService.fieldDropFromSource$
            .pipe(takeUntil(this.destroy$))
            .subscribe((data: DropFieldData) => {
                this.setDroppedField(data.item);
                this.toggleDialog();
            });


        this.fieldDragDropService.fieldDropFromTarget$
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                setTimeout(() => {
                    this.saveFields.emit(this.fieldRows);
                }, 0);
            });

        this.fieldDragDropService.fieldRowDropFromTarget$
            .pipe(takeUntil(this.destroy$))
            .subscribe((fieldRows: DotContentTypeLayoutDivider[]) => {
                this.fieldRows = fieldRows;
                this.saveFields.emit(fieldRows);
            });

        this.dotEventsService
            .listen('add-row')
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                document.querySelector('dot-add-rows').scrollIntoView({
                    behavior: 'smooth'
                });
            });

        this.dotEventsService
            .listen('add-tab-divider')
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                const fieldTab: DotContentTypeLayoutDivider = FieldUtil.createFieldTabDivider();
                this.fieldRows.push(fieldTab);
                this.setDroppedField(fieldTab.divider);
                this.toggleDialog();
            });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.layout && changes.layout.currentValue) {
            this.fieldRows = _.cloneDeep(changes.layout.currentValue);
        }
    }

    @Input()
    set loading(loading: boolean) {
        this._loading = loading;

        if (loading) {
            this.dotLoadingIndicatorService.show();
        } else {
            this.dotLoadingIndicatorService.hide();
        }
    }

    get loading(): boolean {
        return this._loading;
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Adds row to the layout of content type
     * @param number columns new row's number of columns
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    addRow(columns: number): void {
        const newRow = FieldUtil.createFieldRow(columns);
        this.fieldRows.push(newRow);
    }

    /**
     * Emit the saveField event
     * @param DotContentTypeField fieldToSave
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    saveFieldsHandler(fieldToSave: DotContentTypeField): void {
        if (!this.currentField) {
            const tabDividerFields = FieldUtil.getTabDividerFields(this.fieldRows);
            this.currentField = tabDividerFields.find((field: DotContentTypeField) => fieldToSave.id === field.id);
        }

        Object.assign(this.currentField, fieldToSave);

        if (!!fieldToSave.id) {
            this.editField.emit(this.currentField);
        } else {
            this.saveFields.emit(this.fieldRows);
        }

        this.toggleDialog();
    }

    /**
     * Get the field to be edited
     * @param DotContentTypeField fieldToEdit
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    editFieldHandler(fieldToEdit: DotContentTypeField): void {
        if (!this.fieldDragDropService.isDraggedEventStarted()) {
            const fields = FieldUtil.getFieldsWithoutLayout(this.fieldRows);
            this.currentField = fields.find((field: DotContentTypeField) => fieldToEdit.id === field.id);
            this.currentFieldType = this.fieldPropertyService.getFieldType(this.currentField.clazz);
            this.toggleDialog();
        }
    }

    /**
     * Removes the last dropped field added without ID
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    removeFieldsWithoutId(): void {
        const fieldRows: any = this.fieldRows;

        // TODO needs an improvement for performance reasons
        fieldRows.forEach((row, rowIndex) => {
            if (row.columns) {
                row.columns.forEach((col, colIndex) => {
                    col.fields.forEach((field, fieldIndex) => {
                        if (!field.id) {
                            row.columns[colIndex].fields.splice(fieldIndex, 1);
                        }
                    });
                });
            } else if (!row.divider.name) {
                this.fieldRows.splice(rowIndex, 1);
            }
        });
        this.hideButtons = false;
        this.displayDialog = false;
        this.currentField = null;
        this.dialogActiveTab = null;
        this.setDialogOkButtonState(false);
    }

    /**
     * Trigger the removeFields event with fieldToDelete
     * @param {DotContentTypeField} fieldToDelete
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    removeField(fieldToDelete: DotContentTypeField): void {
        this.removeFields.emit([fieldToDelete]);
    }

    /**
     * Trigger the removeFields event with all the fields in fieldRow
     * @param {DotContentTypeLayoutDivider} fieldRow
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    removeFieldRow(fieldRow: DotContentTypeLayoutDivider): void {
        this.fieldRows.splice(this.fieldRows.indexOf(fieldRow), 1);
        const fieldsToDelete: DotContentTypeField[] = [];

        if (!FieldUtil.isNewField(fieldRow.divider)) {
            fieldsToDelete.push(fieldRow.divider);
            fieldRow.columns.forEach((fieldColumn) => {
                fieldsToDelete.push(fieldColumn.columnDivider);
                fieldColumn.fields.forEach((field) => fieldsToDelete.push(field));
            });
            this.removeFields.emit(fieldsToDelete);
        }
    }

    /**
     * Trigger the removeFields event with the tab to be removed
     * @param {DotContentTypeLayoutDivider} fieldTab
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    removeTab(fieldTab: DotContentTypeLayoutDivider): void {
        this.fieldRows.splice(this.fieldRows.indexOf(fieldTab), 1);
        this.removeFields.emit([fieldTab.divider]);
    }

    /**
     * Cancel the last drag and drop operation
     *
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    cancelLastDragAndDrop(): void {
        this.fieldRows = _.cloneDeep(this.layout);
    }

    /**
     * Set the state for the ok action for the dialog
     *
     * @param {boolean} $event
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    setDialogOkButtonState(formChanged: boolean): void {
        this.dialogActions = {
            ...this.dialogActions,
            accept: {
                ...this.dialogActions.accept,
                disabled: !formChanged
            }
        };
    }

     /**
      * Hide or show the 'save' and 'hide' buttons according to the field tab selected
      * 
      * @param index
      */
    handleTabChange(index: number): void {
        this.hideButtons = index !== this.OVERVIEW_TAB_INDEX;
    }

    private setDroppedField(droppedField: DotContentTypeField): void {
        this.currentField = droppedField;
        this.currentFieldType = this.fieldPropertyService.getFieldType(this.currentField.clazz);
    }

    private toggleDialog(): void {
        this.displayDialog = !this.displayDialog;
        this.dialogActiveTab = 0;
    }
}
