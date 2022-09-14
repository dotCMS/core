/* eslint-disable @angular-eslint/component-selector */
import { Component, OnInit, forwardRef, ViewChild, OnDestroy } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormGroup, UntypedFormBuilder } from '@angular/forms';

import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { NgGrid, NgGridConfig } from '@dotcms/dot-layout-grid';

import { DotDialogActions } from '@components/dot-dialog/dot-dialog.component';

import { DotAlertConfirmService } from '@services/dot-alert-confirm/dot-alert-confirm.service';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

import { DotEditLayoutService } from '@services/dot-edit-layout/dot-edit-layout.service';

import {
    DotLayoutBody,
    DotLayoutGrid,
    DOT_LAYOUT_GRID_MAX_COLUMNS
} from '@models/dot-edit-layout-designer';

interface DotAddClass {
    setter: (string) => void;
    getter: () => void;
    title: string;
}

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
export class DotEditLayoutGridComponent implements OnInit, OnDestroy, ControlValueAccessor {
    @ViewChild(NgGrid, { static: true }) ngGrid: NgGrid;

    form: UntypedFormGroup;
    value: DotLayoutBody;
    grid: DotLayoutGrid;

    addClassDialogShow = false;
    addClassDialogActions: DotDialogActions;
    addClassDialogHeader: string;

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

    rowClass: string[] = [];
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotDialogService: DotAlertConfirmService,
        private dotEditLayoutService: DotEditLayoutService,
        private dotMessageService: DotMessageService,
        private dotEventsService: DotEventsService,
        public fb: UntypedFormBuilder
    ) {}

    ngOnInit() {
        this.dotEventsService
            .listen('dot-side-nav-toggle')
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                this.resizeGrid(200);
            });

        this.dotEventsService
            .listen('layout-sidebar-change')
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                this.resizeGrid();
            });

        // needed it because the transition between content & layout.
        this.resizeGrid();

        this.form = this.fb.group({
            classToAdd: ''
        });

        this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.addClassDialogActions = {
                cancel: {
                    ...this.addClassDialogActions.cancel
                },
                accept: {
                    ...this.addClassDialogActions.accept,
                    disabled: !this.form.valid
                }
            };
        });

        this.dotEditLayoutService
            .getBoxes()
            .pipe(takeUntil(this.destroy$))
            .subscribe(() => {
                this.addBox();
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
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

    propagateChange = (_: unknown) => {
        /**/
    };

    /**
     * Set the function to be called when the control receives a change event.
     *
     * @param * fn
     * @memberof DotEditLayoutGridComponent
     */
    registerOnChange(
        fn: () => {
            /**/
        }
    ): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {
        /**/
    }

    /**
     * Update the model when the grid is changed
     *
     * @memberof DotEditLayoutGridComponent
     */
    propagateGridLayoutChange(): void {
        this.propagateChange(this.getModel());
        this.rowClass = this.grid.getAllRowClass();
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
     *
     * @param {number} index
     * @param {string} title
     * @memberof DotEditLayoutGridComponent
     */
    addColumnClass(index: number): void {
        this.addClass({
            getter: () => {
                return this.grid.boxes[index].config.payload
                    ? this.grid.boxes[index].config.payload.styleClass || null
                    : null;
            },
            setter: (value: string) => {
                if (!this.grid.boxes[index].config.payload) {
                    this.grid.boxes[index].config.payload = {
                        styleClass: value
                    };
                } else {
                    this.grid.boxes[index].config.payload.styleClass = value;
                }
            },
            title: this.dotMessageService.get('editpage.layout.css.class.add.to.box')
        });
    }

    /**
     * Add style class to a row
     *
     * @param {number} index
     * @param {string} title
     * @memberof DotEditLayoutGridComponent
     */
    addRowClass(index: number): void {
        this.addClass({
            getter: () => this.grid.getRowClass(index) || '',
            setter: (value) => this.grid.setRowClass(value, index),
            title: this.dotMessageService.get('editpage.layout.css.class.add.to.row')
        });
    }

    /**
     * Handle hide event from add class dialog
     *
     * @memberof DotEditLayoutGridComponent
     */
    onAddClassDialogHide(): void {
        this.addClassDialogActions = null;
        this.addClassDialogShow = false;
        this.addClassDialogHeader = '';
    }

    private addClass(params: DotAddClass): void {
        this.form.setValue(
            {
                classToAdd: params.getter.bind(this)()
            },
            {
                emitEvent: false
            }
        );

        this.addClassDialogActions = {
            accept: {
                action: (dialog?: {
                    close: () => {
                        /**/
                    };
                }) => {
                    params.setter.bind(this)(this.form.get('classToAdd').value);
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

        this.addClassDialogShow = true;
        this.addClassDialogHeader = params.title;
    }

    private setGridValue(): void {
        this.grid = this.isHaveRows()
            ? this.dotEditLayoutService.getDotLayoutGridBox(this.value)
            : DotLayoutGrid.getDefaultGrid();

        this.rowClass = this.grid.getAllRowClass();
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
        setTimeout(
            () => {
                this.ngGrid.triggerResize();
            },
            timeOut ? timeOut : 0
        );
    }
}
