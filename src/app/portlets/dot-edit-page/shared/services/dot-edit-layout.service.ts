import { Injectable } from '@angular/core';
import { DotLayoutBody } from '../../shared/models/dot-layout-body.model';
import { DotLayoutGridBox } from '../../shared/models/dot-layout-grid-box.model';
import { DotLayoutRow } from '../../shared/models/dot-layout-row.model';
import { DotLayoutColumn } from '../../shared/models/dot-layout-column.model';
import * as _ from 'lodash';
import { DotPageView } from '../../shared/models/dot-page-view.model';
import { NgGridItemConfig } from 'angular2-grid';
import { DOT_LAYOUT_GRID_NEW_ROW_TEMPLATE } from '../../shared/models/dot-layout.const';
import { TemplateContainersCacheService } from '../../template-containers-cache.service';

/**
 * Provide methods to transform NgGrid model into PageView model and viceversa.
 *
 * @class DotEditLayoutService
 */
@Injectable()
export class DotEditLayoutService {
    constructor(private templateContainersCacheService: TemplateContainersCacheService) {}

    /**
     * Take an DotPageView and return an array of DotLayoutGridBox
     *
     * @param {DotLayoutBody} dotLayoutBody
     * @returns {DotLayoutGridBox[]}
     */
    getDotLayoutGridBox(dotLayoutBody: DotLayoutBody): DotLayoutGridBox[] {
        const grid: DotLayoutGridBox[] = [];

        dotLayoutBody.rows.forEach((row, rowIndex) => {
            row.columns.forEach(column => {
                grid.push({
                    containers: column.containers.map(containerId =>
                        this.templateContainersCacheService.get(containerId)
                    ),
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

    /**
     * Remove the extra attributes that come from the server and are not longer needed in DotLayoutBody.
     * @param {DotLayoutBody} grid
     * @returns {DotLayoutBody}
     */
    cleanupDotLayoutBody(grid: DotLayoutBody): DotLayoutBody {
        return this.getDotLayoutBody(this.getDotLayoutGridBox(grid));
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
