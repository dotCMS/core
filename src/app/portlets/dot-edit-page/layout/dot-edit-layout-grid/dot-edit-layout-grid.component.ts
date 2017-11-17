import { Component, OnInit, Input } from '@angular/core';
import { NgGridConfig, NgGridItemConfig } from 'angular2-grid';
import _ from 'lodash';

//Final Object need to be defined.
interface DotLayoutContainer {
    id: string;
    config: NgGridItemConfig;
}

/**
 * Component in charge of update the model that will be used be the NgGrid to display containers
 *
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-edit-layout-grid',
    templateUrl: './dot-edit-layout-grid.component.html',
    styleUrls: ['./dot-edit-layout-grid.component.scss']
})
export class DotEditLayoutGridComponent implements OnInit {
    @Input() gridContainers: DotLayoutContainer[];
    private static MAX_COLUMNS: number = 12;
    private static NEW_ROW_TEMPLATE: NgGridItemConfig = { fixed: true, sizex: 3, maxCols: 12, maxRows: 1 };
    private static DEFAULT_EMPTY_GRID_ROWS: NgGridItemConfig = {
        fixed: true,
        sizex: 12,
        maxCols: 12,
        maxRows: 1,
        col: 1,
        row: 1
    };
    gridConfig: NgGridConfig = <NgGridConfig>{
        margins: [6, 6, 0, 0],
        draggable: true,
        resizable: true,
        max_cols: DotEditLayoutGridComponent.MAX_COLUMNS,
        max_rows: 0,
        visible_cols: DotEditLayoutGridComponent.MAX_COLUMNS,
        // 'visible_rows': 12,
        min_cols: 1,
        min_rows: 1,
        col_width: 90,
        row_height: 200,
        cascade: 'up',
        min_width: 40,
        min_height: 190,
        fix_to_grid: true,
        auto_style: true,
        auto_resize: true,
        maintain_ratio: false,
        prefer_new: false,
        zoom_on_drag: false,
        limit_to_screen: true
    };

    constructor() {}

    ngOnInit() {
        if (!this.gridContainers) {
            this.gridContainers = [
                {
                    id: Math.random().toString(),
                    config: Object.assign({}, DotEditLayoutGridComponent.DEFAULT_EMPTY_GRID_ROWS)
                }
            ];
        }
    }

    /**
     * Add new container to the gridContainers Arrray.
     */
    addContainer(): () => void {
        //TODO: This will change when Action Button get fixed.
        return () => {
            let conf: NgGridItemConfig = this.setConfigOfNewContainer();
            this.gridContainers.push({ id: Math.random().toString(), config: conf });
        };
    }

    /**
     * Removes the given index to the gridContainers Array.
     * @param {number} index
     */
    removeContainer(index: number): void {
        if (this.gridContainers[index]) {
            this.gridContainers.splice(index, 1);
            this.deleteEmptyRows();
        }
    }

    /**
     * Event fired when the grad of a container ends, remove empty rows if any.
     * @constructor
     */
    OnDragStop(): void {
        this.deleteEmptyRows();
    }

    private setConfigOfNewContainer(): NgGridItemConfig {
        let lastContainer;
        let newRow: NgGridItemConfig = Object.assign({}, DotEditLayoutGridComponent.NEW_ROW_TEMPLATE);
        let busyColumns: number = DotEditLayoutGridComponent.NEW_ROW_TEMPLATE.sizex;
        if (this.gridContainers.length) {
            // check last row && last column in last row
            lastContainer = this.gridContainers.reduce(
                (currentContainer: DotLayoutContainer, nextContainer: DotLayoutContainer) => {
                    return currentContainer.config.row > currentContainer.config.row
                        ? currentContainer
                        : currentContainer.config.row == nextContainer.config.row
                          ? currentContainer.config.col > nextContainer.config.col ? currentContainer : nextContainer
                          : nextContainer;
                }
            );
            busyColumns += lastContainer.config.col - 1 + lastContainer.config.sizex;
            if (busyColumns <= DotEditLayoutGridComponent.MAX_COLUMNS) {
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
        //TODO: Find a solution to remove setTimeOut
        setTimeout(() => {
            this.gridContainers = _.chain(this.gridContainers)
                .sortBy('config.row')
                .groupBy('config.row')
                .values()
                .map(this.updateContainerIndex)
                .flatten()
                .value();
        }, 0);
    }

    private updateContainerIndex(rowArray, index) {
        if (rowArray[0].row != index + 1) {
            return rowArray.map(container => {
                container.config.row = index + 1;
                return container;
            });
        }
        return rowArray;
    }
}
