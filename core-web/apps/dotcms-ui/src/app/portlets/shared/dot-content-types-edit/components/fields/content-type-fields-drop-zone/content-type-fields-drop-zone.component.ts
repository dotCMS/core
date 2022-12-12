import {
    Component,
    SimpleChanges,
    Input,
    Output,
    EventEmitter,
    OnInit,
    OnChanges,
    OnDestroy,
    ViewChild,
    ElementRef,
    Renderer2
} from '@angular/core';
import { FieldDragDropService, DropFieldData } from '../service';
import { FieldType } from '../models';
import {
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow,
    DotCMSContentTypeLayoutColumn,
    DotCMSContentType
} from '@dotcms/dotcms-models';
import { ContentTypeFieldsPropertiesFormComponent } from '../content-type-fields-properties-form';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { FieldUtil } from '../util/field-util';
import { FieldPropertyService } from '../service/field-properties.service';
import { DotDialogActions } from '@components/dot-dialog/dot-dialog.component';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { DotLoadingIndicatorService } from '@components/_common/iframe/dot-loading-indicator/dot-loading-indicator.service';
import * as _ from 'lodash';
import { DragulaService } from 'ng2-dragula';
import * as autoScroll from 'dom-autoscroller';

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
    readonly BLOCK_EDITOR_SETTINGS_TAB_INDEX = 1;

    displayDialog = false;
    currentField: DotCMSContentTypeField;
    currentFieldType: FieldType;
    dialogActions: DotDialogActions;
    defaultDialogActions: DotDialogActions;
    fieldRows: DotCMSContentTypeLayoutRow[];
    hideButtons = false;
    activeTab = 0;

    @ViewChild('fieldPropertiesForm', { static: true })
    propertiesForm: ContentTypeFieldsPropertiesFormComponent;

    @Input()
    layout: DotCMSContentTypeLayoutRow[];

    @Input()
    contentType: DotCMSContentType;

    @Output()
    saveFields = new EventEmitter<DotCMSContentTypeLayoutRow[]>();

    @Output()
    editField = new EventEmitter<DotCMSContentTypeField>();

    @Output()
    removeFields = new EventEmitter<DotCMSContentTypeField[]>();

    private _loading: boolean;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    get isBlockEditorField() {
        return (
            this.currentFieldType?.clazz ===
            'com.dotcms.contenttype.model.field.ImmutableStoryBlockField'
        );
    }

    constructor(
        private dotMessageService: DotMessageService,
        private fieldDragDropService: FieldDragDropService,
        private fieldPropertyService: FieldPropertyService,
        private dotEventsService: DotEventsService,
        private dotLoadingIndicatorService: DotLoadingIndicatorService,
        private dragulaService: DragulaService,
        private elRef: ElementRef,
        private rendered: Renderer2
    ) {}

    private static findColumnBreakIndex(fields: DotCMSContentTypeField[]): number {
        return fields.findIndex((item: DotCMSContentTypeField) => {
            return FieldUtil.isColumnBreak(item.clazz);
        });
    }

    private static splitColumnsInRows(
        fieldRows: DotCMSContentTypeLayoutRow[]
    ): DotCMSContentTypeLayoutRow[] {
        return fieldRows.map((row: DotCMSContentTypeLayoutRow) => {
            return {
                ...row,
                columns: row.columns.reduce(
                    (
                        acc: DotCMSContentTypeLayoutColumn[],
                        current: DotCMSContentTypeLayoutColumn
                    ) => {
                        const indexOfBreak =
                            ContentTypeFieldsDropZoneComponent.findColumnBreakIndex(current.fields);
                        if (indexOfBreak > -1) {
                            const firstColFields = current.fields.slice(0, indexOfBreak);
                            const secondColFields = current.fields.slice(indexOfBreak + 1);

                            const firstCol = {
                                ...current,
                                fields: firstColFields
                            };

                            const secondCol = FieldUtil.createFieldColumn(secondColFields);

                            return [...acc, firstCol, secondCol];
                        } else {
                            return acc.concat(current);
                        }
                    },
                    []
                )
            };
        });
    }

    ngOnInit(): void {
        this.defaultDialogActions = {
            accept: {
                action: () => {
                    this.propertiesForm.saveFieldProperties();
                },
                label: this.dotMessageService.get('contenttypes.dropzone.action.save'),
                disabled: true
            },
            cancel: {
                label: this.dotMessageService.get('contenttypes.dropzone.action.cancel')
            }
        };

        this.dialogActions = this.defaultDialogActions;

        this.fieldDragDropService.fieldDropFromSource$
            .pipe(takeUntil(this.destroy$))
            .subscribe((data: DropFieldData) => {
                if (FieldUtil.isColumnBreak(data.item.clazz)) {
                    setTimeout(() => {
                        this.emitSaveFields(
                            ContentTypeFieldsDropZoneComponent.splitColumnsInRows(this.fieldRows)
                        );
                    }, 0);
                } else {
                    this.setDroppedField(data.item);
                    this.toggleDialog();
                }
            });

        this.fieldDragDropService.fieldDropFromTarget$
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                setTimeout(() => {
                    this.emitSaveFields(this.fieldRows);
                }, 0);
            });

        this.fieldDragDropService.fieldRowDropFromTarget$
            .pipe(takeUntil(this.destroy$))
            .subscribe((fieldRows: DotCMSContentTypeLayoutRow[]) => {
                this.fieldRows = fieldRows;
                this.emitSaveFields(fieldRows);
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
                const fieldTab: DotCMSContentTypeLayoutRow = FieldUtil.createFieldTabDivider();
                this.fieldRows.push(fieldTab);
                this.setDroppedField(fieldTab.divider);
                this.toggleDialog();
            });

        this.setUpDragulaScroll();
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
     * Convert WYSIWYG field to Block Field
     *
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    convertWysiwygToBlock() {
        this.editField.emit({
            ...this.currentField,
            clazz: 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
            fieldType: 'Story-Block'
        });
        this.toggleDialog();
    }

    /**
     * Emit the saveField event
     * @param DotContentTypeField fieldToSave
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    saveFieldsHandler(fieldToSave: DotCMSContentTypeField): void {
        if (!this.currentField) {
            const tabDividerFields = FieldUtil.getTabDividerFields(this.fieldRows);
            this.currentField = tabDividerFields.find(
                (field: DotCMSContentTypeField) => fieldToSave.id === field.id
            );
        }

        Object.assign(this.currentField, fieldToSave);

        if (fieldToSave.id) {
            this.editField.emit(this.currentField);
        } else {
            this.emitSaveFields(this.fieldRows);
        }

        this.toggleDialog();
    }

    /**
     * Get the field to be edited
     * @param DotContentTypeField fieldToEdit
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    editFieldHandler(fieldToEdit: DotCMSContentTypeField): void {
        if (!this.fieldDragDropService.isDraggedEventStarted()) {
            const fields = FieldUtil.getFieldsWithoutLayout(this.fieldRows);
            this.currentField = fields.find(
                (field: DotCMSContentTypeField) => fieldToEdit.id === field.id
            );
            this.currentFieldType = this.fieldPropertyService.getFieldType(this.currentField.clazz);
            this.toggleDialog();
        }
    }

    /**
     * Removes the last dropped field added without ID
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    removeFieldsWithoutId(): void {
        const fieldRows: DotCMSContentTypeLayoutRow[] = this.fieldRows;

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
        this.activeTab = 0;
        this.displayDialog = false;
        this.currentField = null;
        this.setDialogOkButtonState(false);
    }

    /**
     * Trigger the removeFields event with fieldToDelete
     * @param {DotCMSContentTypeField} fieldToDelete
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    removeField(fieldToDelete: DotCMSContentTypeField): void {
        this.removeFields.emit([fieldToDelete]);
    }

    /**
     * Trigger the removeFields event with all the fields in fieldRow
     * @param {DotCMSContentTypeLayoutRow} fieldRow
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    removeFieldRow(fieldRow: DotCMSContentTypeLayoutRow, index: number): void {
        this.fieldRows.splice(index, 1);
        const fieldsToDelete: DotCMSContentTypeField[] = [];

        if (!FieldUtil.isNewField(fieldRow.divider)) {
            fieldsToDelete.push(fieldRow.divider);
            fieldRow.columns.forEach((fieldColumn: DotCMSContentTypeLayoutColumn) => {
                fieldsToDelete.push(fieldColumn.columnDivider);
                fieldColumn.fields.forEach((field) => fieldsToDelete.push(field));
            });
            this.removeFields.emit(fieldsToDelete);
        }
    }

    /**
     * Trigger the removeFields event with the tab to be removed
     * @param {DotCMSContentTypeLayoutRow} fieldTab
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    removeTab(fieldTab: DotCMSContentTypeLayoutRow, index: number): void {
        this.fieldRows.splice(index, 1);
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
        if (index === this.OVERVIEW_TAB_INDEX) {
            this.dialogActions = this.defaultDialogActions;
        }

        this.hideButtons =
            index !== this.OVERVIEW_TAB_INDEX &&
            !(index === this.BLOCK_EDITOR_SETTINGS_TAB_INDEX && this.isBlockEditorField);
    }

    /**
     * Scroll into convert to block section
     *
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    scrollTo() {
        const el = this.rendered.selectRootElement('dot-convert-wysiwyg-to-block', true);

        el.scrollIntoView({
            behavior: 'smooth',
            block: 'start',
            inline: 'nearest'
        });
    }

    /**
     * Change dialogActions
     *
     * @param {DotDialogActions} controls
     * @memberof ContentTypeFieldsDropZoneComponent
     */
    changesDialogActions(controls: DotDialogActions) {
        this.dialogActions = controls;
    }

    private setDroppedField(droppedField: DotCMSContentTypeField): void {
        this.currentField = droppedField;
        this.currentFieldType = this.fieldPropertyService.getFieldType(this.currentField.clazz);
    }

    private toggleDialog(): void {
        this.dialogActions = this.defaultDialogActions;
        this.activeTab = this.OVERVIEW_TAB_INDEX;
        this.displayDialog = !this.displayDialog;
    }

    private emitSaveFields(layout: DotCMSContentTypeLayoutRow[]): void {
        this.saveFields.emit(layout);
    }

    private setUpDragulaScroll(): void {
        const drake = this.dragulaService.find('fields-bag')?.drake;
        autoScroll([this.elRef.nativeElement.parentElement], {
            margin: 100,
            maxSpeed: 60,
            scrollWhenOutside: true,
            autoScroll() {
                return this.down && drake.dragging;
            }
        });
    }
}
