import { Component, OnInit, Input, forwardRef, ViewChild } from '@angular/core';
import { NgGrid, NgGridConfig, NgGridItemConfig } from 'angular2-grid';
import * as _ from 'lodash';
import { DotConfirmationService } from '../../../../api/services/dot-confirmation/dot-confirmation.service';
import { MessageService } from '../../../../api/services/messages-service';
import { DotLayoutGridBox } from '../../shared/models/dot-layout-grid-box.model';
import {
    DOT_LAYOUT_GRID_MAX_COLUMNS,
    DOT_LAYOUT_GRID_NEW_ROW_TEMPLATE,
    DOT_LAYOUT_DEFAULT_GRID
} from '../../shared/models/dot-layout.const';
import { DotPageView } from '../../shared/models/dot-page-view.model';
import { DotLayoutBody } from '../../shared/models/dot-layout-body.model';
import { DotEditLayoutService } from '../../shared/services/dot-edit-layout.service';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { DotEventsService } from '../../../../api/services/dot-events/dot-events.service';
import { DotEvent } from '../../../../shared/models/dot-event/dot-event';

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
    @ViewChild(NgGrid) ngGrid: NgGrid;

    value: DotLayoutBody;
    grid: DotLayoutGridBox[];

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

    private i18nKeys = [
        'editpage.confirm.header',
        'editpage.confirm.message.delete',
        'editpage.confirm.message.delete.warning',
        'editpage.action.cancel',
        'editpage.action.delete',
        'editpage.action.save'
    ];

    constructor(
        private dotConfirmationService: DotConfirmationService,
        private dotEditLayoutService: DotEditLayoutService,
        public messageService: MessageService,
        private dotEventsService: DotEventsService
    ) {}

    ngOnInit() {
        this.messageService.getMessages(this.i18nKeys).subscribe();
        this.dotEventsService.listen('dot-side-nav-toggle').subscribe((event: DotEvent) => {
            // setTimeout is need it because the side nav animation time.
            setTimeout(() => {
                this.ngGrid.triggerResize();
            }, 200);
        });
        this.dotEventsService.listen('layout-sidebar-change').subscribe((event: DotEvent) => {
            // We need to "wait" until the template remove the sidebar div.
            setTimeout(() => {
                this.ngGrid.triggerResize();
            }, 0);
        });
    }

    /**
     * Add new Box to the gridBoxes Arrray.
     *
     * @memberof DotEditLayoutGridComponent
     */
    addBox(): void {
        const conf: NgGridItemConfig = this.setConfigOfNewContainer();
        this.grid.push({ config: conf, containers: [] });
        this.propagateChange(this.getModel());
    }

    /**
     * Return ng-grid model.
     *
     * @returns {DotLayoutBody}
     * @memberof DotEditLayoutGridComponent
     */
    getModel(): DotLayoutBody {
        return this.dotEditLayoutService.getDotLayoutBody(this.grid);
    }

    /**
     * Event fired when the drag of a container ends, remove empty rows if any.
     *
     * @memberof DotEditLayoutGridComponent
     */
    onDragStop(): void {
        this.deleteEmptyRows();
        this.propagateChange(this.getModel());
    }

    /**
     * Removes the given index to the gridBoxes Array after the user confirms.
     *
     * @param {number} index
     * @memberof DotEditLayoutGridComponent
     */
    onRemoveContainer(index: number): void {
        this.dotConfirmationService.confirm({
            accept: () => {
                this.removeContainer(index);
            },
            header: this.messageService.get('editpage.confirm.header'),
            message: `${this.messageService.get('editpage.confirm.message.delete')} <span>${this.messageService.get(
                'editpage.confirm.message.delete.warning'
            )}</span>`,
            footerLabel: {
                acceptLabel: this.messageService.get('editpage.action.delete'),
                rejectLabel: this.messageService.get('editpage.action.cancel')
            }
        });
    }

    propagateChange = (_: any) => {};

    /**
     * Set the function to be called when the control receives a change event.
     *
     * @param {*} fn
     * @memberof DotEditLayoutGridComponent
     */
    registerOnChange(fn: any): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {}

    /**
     * Update the model when a container is added to a box
     *
     * @memberof DotEditLayoutGridComponent
     */
    updateContainers(): void {
        this.propagateChange(this.getModel());
    }

    /**
     * Write a new value to the element
     *
     * @param {DotLayoutBody} value
     * @memberof DotEditLayoutGridComponent
     */
    writeValue(value: DotLayoutBody): void {
        if (value) {
            this.value = value || null;
            this.setGridValue();
        }
    }

    private setGridValue(): void {
        this.grid = this.isHaveRows()
            ? this.dotEditLayoutService.getDotLayoutGridBox(this.value)
            : [...DOT_LAYOUT_DEFAULT_GRID];
    }

    private removeContainer(index: number): void {
        if (this.grid[index]) {
            this.grid.splice(index, 1);
            this.deleteEmptyRows();
            this.propagateChange(this.getModel());
        }
    }

    private setConfigOfNewContainer(): NgGridItemConfig {
        const newRow: NgGridItemConfig = Object.assign({}, DOT_LAYOUT_GRID_NEW_ROW_TEMPLATE);

        if (this.grid.length) {
            const lastContainer = _.chain(this.grid)
                .groupBy('config.row')
                .values()
                .last()
                .maxBy('config.col')
                .value();

            let busyColumns: number = DOT_LAYOUT_GRID_NEW_ROW_TEMPLATE.sizex;

            busyColumns += lastContainer.config.col - 1 + lastContainer.config.sizex;

            if (busyColumns <= DOT_LAYOUT_GRID_MAX_COLUMNS) {
                newRow.row = lastContainer.config.row;
                newRow.col = lastContainer.config.col + lastContainer.config.sizex;
            } else {
                newRow.row = lastContainer.config.row + 1;
                newRow.col = 1;
            }
        }

        return newRow;
    }

    private deleteEmptyRows(): void {
        // TODO: Find a solution to remove setTimeout
        setTimeout(() => {
            this.grid = _.chain(this.grid)
                .sortBy('config.row')
                .groupBy('config.row')
                .values()
                .map(this.updateContainerIndex)
                .flatten()
                .value();
        }, 0);
    }

    private updateContainerIndex(rowArray, index) {
        if (rowArray[0].row !== index + 1) {
            return rowArray.map(container => {
                container.config.row = index + 1;
                return container;
            });
        }
        return rowArray;
    }

    private isHaveRows(): boolean {
        return !!(
            this.value &&
            this.value.rows &&
            this.value.rows.length
        );
    }
}
