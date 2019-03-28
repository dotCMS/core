import { Component, OnInit, forwardRef, ViewChild } from '@angular/core';
import * as _ from 'lodash';
import { DotAlertConfirmService } from '@services/dot-alert-confirm/dot-alert-confirm.service';
import { DotMessageService } from '@services/dot-messages-service';
import { DotLayoutBody } from '../../../shared/models/dot-layout-body.model';
import { DotEditLayoutService } from '../../../shared/services/dot-edit-layout.service';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormGroup, FormBuilder } from '@angular/forms';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { NgGrid, NgGridConfig } from 'dot-layout-grid';
import { DotDialogActions } from '@components/dot-dialog/dot-dialog.component';
import { DotLayoutGrid, DOT_LAYOUT_GRID_MAX_COLUMNS } from '@portlets/dot-edit-page/shared/models/dot-layout-grid.model';

/**
 * Component in charge of update the model that will be used be the NgGrid to display containers
 *
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-edit-layout-grid',
    templateUrl: './dot-edit-layout-grid.component.html',
    styleUrls: ['./dot-edit-layout-grid.component.scss'],
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotEditLayoutGridComponent)
        }
    ]
})
export class DotEditLayoutGridComponent implements OnInit, ControlValueAccessor {
    @ViewChild(NgGrid)
    ngGrid: NgGrid;

    form: FormGroup;
    value: DotLayoutBody;
    grid: DotLayoutGrid;

    showAddClassDialog = false;
    dialogActions: DotDialogActions;

    gridConfig: NgGridConfig = <NgGridConfig>{
        margins: [0, 8, 8, 0],
        draggable: true,
        resizable: true,
        max_cols: DOT_LAYOUT_GRID_MAX_COLUMNS,
        max_rows: 0,
        visible_cols: DOT_LAYOUT_GRID_MAX_COLUMNS,
        min_cols: 1,
        min_rows: 1,
        col_width: 90,
        row_height: 206,
        cascade: 'up',
        min_width: 40,
        min_height: 206,
        fix_to_grid: true,
        auto_style: true,
        auto_resize: true,
        maintain_ratio: false,
        prefer_new: false,
        zoom_on_drag: false,
        limit_to_screen: true
    };

    constructor(
        private dotDialogService: DotAlertConfirmService,
        private dotEditLayoutService: DotEditLayoutService,
        public dotMessageService: DotMessageService,
        private dotEventsService: DotEventsService,
        public fb: FormBuilder,
    ) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'editpage.confirm.header',
                'editpage.confirm.message.delete',
                'editpage.confirm.message.delete.warning',
                'editpage.action.cancel',
                'editpage.action.delete',
                'editpage.action.save',
                'dot.common.dialog.accept',
                'dot.common.dialog.reject'
            ])
            .subscribe();

        this.dotEventsService.listen('dot-side-nav-toggle').subscribe(() => {
            this.resizeGrid(200);
        });

        this.dotEventsService.listen('layout-sidebar-change').subscribe(() => {
            this.resizeGrid();
        });

        // needed it because the transition between content & layout.
        this.resizeGrid();

        this.form = this.fb.group({
            classToAdd: ''
        });

        this.form.valueChanges.subscribe(() => {
            this.dialogActions = {
                cancel: {
                    ...this.dialogActions.cancel
                },
                accept: {
                    ...this.dialogActions.accept,
                    disabled: !this.form.valid
                }
            };
        });
    }

    /**
     * Add new Box to the gridBoxes Arrray.
     *
     * @memberof DotEditLayoutGridComponent
     */
    addBox(): void {
        this.grid.addBox();
        this.propagateGridLayoutChange();
    }

    /**
     * Return ng-grid model.
     *
     * @returns DotLayoutBody
     * @memberof DotEditLayoutGridComponent
     */
    getModel(): DotLayoutBody {
        return this.dotEditLayoutService.getDotLayoutBody(this.grid);
    }

    /**
     * Event fired when the drag or resize of a container ends, remove empty rows if any.
     *
     * @memberof DotEditLayoutGridComponent
     */
    updateModel(): void {
        this.deleteEmptyRows();
        this.propagateGridLayoutChange();
    }

    /**
     * Removes the given index to the gridBoxes Array after the user confirms.
     *
     * @param number index
     * @memberof DotEditLayoutGridComponent
     */
    onRemoveContainer(index: number): void {
        if (this.grid.boxes[index].containers.length) {
            this.dotDialogService.confirm({
                accept: () => {
                    this.removeContainer(index);
                },
                header: this.dotMessageService.get('editpage.confirm.header'),
                message: `${this.dotMessageService.get(
                    'editpage.confirm.message.delete'
                )} <span>${this.dotMessageService.get(
                    'editpage.confirm.message.delete.warning'
                )}</span>`,
                footerLabel: {
                    accept: this.dotMessageService.get('editpage.action.delete'),
                    reject: this.dotMessageService.get('editpage.action.cancel')
                }
            });
        } else {
            this.removeContainer(index);
        }
    }

    // tslint:disable-next-line:no-shadowed-variable
    propagateChange = (_: any) => {};

    /**
     * Set the function to be called when the control receives a change event.
     *
     * @param * fn
     * @memberof DotEditLayoutGridComponent
     */
    registerOnChange(fn: any): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {}

    /**
     * Update the model when the grid is changed
     *
     * @memberof DotEditLayoutGridComponent
     */
    propagateGridLayoutChange(): void {
        this.propagateChange(this.getModel());
    }

    /**
     * Write a new value to the element
     *
     * @param DotLayoutBody value
     * @memberof DotEditLayoutGridComponent
     */
    writeValue(value: DotLayoutBody): void {
        if (value) {
            this.value = value || null;
            this.setGridValue();
        }
    }

    /**
     * Add style class to a column
     * @param index column index into {@link DotLayoutGrid#boxes}
     */
    addColumnClass(index: number): void {
        this.addClass(
            () => this.grid.boxes[index].config.payload ? this.grid.boxes[index].config.payload.styleClass || null : null,
            (value: string) => {
                if (!this.grid.boxes[index].config.payload) {
                    this.grid.boxes[index].config.payload = {
                        styleClass: value
                    };
                } else {
                    this.grid.boxes[index].config.payload.styleClass = value;
                }
            }
        );
    }

    /**
     * Add style class to a row
     * @param index row index
     */
    addRowClass(index: number): void {
        this.addClass(
            () => this.grid.getRowClass(index) || '',
            (value) => this.grid.setRowClass(value, index)
        );
    }

    private addClass(getterFunc: () => string, setterFunc: (value: string) => void): void {
        this.form.setValue(
            {
                classToAdd: getterFunc.bind(this)()
            },
            {
                emitEvent: false
            }
        );

        this.dialogActions = {
            accept: {
                action: (dialog?: any) => {
                    setterFunc.bind(this)(this.form.get('classToAdd').value);
                    this.propagateGridLayoutChange();
                    dialog.close();
                },
                label: 'Ok',
                disabled: true
            },
            cancel: {
                label: 'Cancel'
            }
        };

        this.showAddClassDialog = true;
    }

    private setGridValue(): void {
        this.grid = this.isHaveRows()
            ? this.dotEditLayoutService.getDotLayoutGridBox(this.value)
            : DotLayoutGrid.getDefaultGrid();
    }

    private removeContainer(index: number): void {
        if (this.grid.boxes[index]) {
            this.grid.removeContainer(index);
            this.propagateGridLayoutChange();
        }
    }

    private deleteEmptyRows(): void {
        // TODO: Find a solution to remove setTimeout
        setTimeout(() => {
            this.grid.deleteEmptyRows();
        }, 0);
    }

    private isHaveRows(): boolean {
        return !!(this.value && this.value.rows && this.value.rows.length);
    }

    private resizeGrid(timeOut?): void {
        setTimeout(() => {
            this.ngGrid.triggerResize();
        }, timeOut ? timeOut : 0);
    }
}
