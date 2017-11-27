import { Injectable } from '@angular/core';
import { DotLayoutBody } from '../../shared/models/dot-layout-body.model';
import { DotLayoutGridBox } from '../../shared/models/dot-layout-grid-box.model';
import { DotLayoutRow } from '../../shared/models/dot-layout-row.model';
import { DotLayoutColumn } from '../../shared/models/dot-layout-column.model';
import * as _ from 'lodash';
import { DotPageView } from '../../shared/models/dot-page-view.model';
import { NgGridItemConfig } from 'angular2-grid';
import { DOT_LAYOUT_GRID_NEW_ROW_TEMPLATE } from '../../shared/models/dot-layout.const';

/**
 * Provide methods to transform NgGrid model into PageView model and viceversa.
 *
 * @class DotEditLayoutService
 */
@Injectable()
export class DotEditLayoutService {
    constructor() {}

    /**
     * Take an DotPageView and return an array of DotLayoutGridBox
     *
     * @param {DotPageView} dotPageView
     * @param {NgGridItemConfig} newRow
     * @returns {DotLayoutGridBox[]}
     */
    getDotLayoutGridBox(dotPageView: DotPageView): DotLayoutGridBox[] {
        const grid: DotLayoutGridBox[] = [];

        dotPageView.layout.body.rows.forEach((row, rowIndex) => {
            row.columns.forEach(column => {
                grid.push({
                    containers: column.containers.map(containerId => dotPageView.containers[containerId].container),
                    config: Object.assign({}, DOT_LAYOUT_GRID_NEW_ROW_TEMPLATE, {
                        sizex: column.width,
                        col: column.leftOffset,
                        row: rowIndex + 1
                    })
                });
            });
        });

        return grid;
    }

    /**
     * Take an array of DotLayoutGridBox and return a DotLayoutBody.
     *
     * @param {DotLayoutGridBox[]} grid
     * @returns {DotLayoutBody}
     */
    getDotLayoutBody(grid: DotLayoutGridBox[]): DotLayoutBody {
        return <DotLayoutBody>{
            rows: _.chain(grid)
                .sortBy('config.row')
                .sortBy('config.col')
                .groupBy('config.row')
                .values()
                .map(this.getLayoutRowFromLayoutGridBoxes)
                .value()
        };
    }

    private getLayoutRowFromLayoutGridBoxes(gridBoxes: DotLayoutGridBox[]): DotLayoutRow {
        return {
            columns: gridBoxes.map(
                (layoutGridBox: DotLayoutGridBox) =>
                    <DotLayoutColumn>{
                        leftOffset: layoutGridBox.config.col,
                        width: layoutGridBox.config.sizex,
                        containers: layoutGridBox.containers.map(container => container.identifier)
                    }
            )
        };
    }
}
