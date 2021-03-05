import { Injectable } from '@angular/core';

import { Subject, Observable } from 'rxjs';
import * as _ from 'lodash';

import {
    DotLayoutBody,
    DotLayoutGrid,
    DotLayoutGridBox,
    DotLayoutRow,
    DotLayoutColumn,
    DotLayoutGridRow,
    DotContainerColumnBox
} from '@shared/models/dot-edit-layout-designer';
import { DotTemplateContainersCacheService } from '@services/dot-template-containers-cache/dot-template-containers-cache.service';
import { DotPageContainer } from '@models/dot-page-container/dot-page-container.model';

/**
 * Provide methods to transform NgGrid model into PageView model and viceversa.
 *
 * @class DotEditLayoutService
 */
@Injectable()
export class DotEditLayoutService {
    private _addGridBox: Subject<boolean> = new Subject();

    constructor(private templateContainersCacheService: DotTemplateContainersCacheService) {}

    /**
     * Take an DotPageView and return an array of DotLayoutGridBox
     *
     * @param DotLayoutBody dotLayoutBody
     * @returns DotLayoutGridBox[]
     */
    getDotLayoutGridBox(dotLayoutBody: DotLayoutBody): DotLayoutGrid {
        const grid: DotLayoutGridBox[] = [];

        dotLayoutBody.rows.forEach((row: DotLayoutRow, rowIndex) => {
            row.columns.forEach((column: DotLayoutColumn) => {
                grid.push({
                    containers: this.getDotContainerColumnBoxFromDotPageContainer(
                        column.containers
                    ),
                    config: Object.assign(DotLayoutGrid.getDefaultConfig(), {
                        sizex: column.width,
                        col: column.leftOffset,
                        row: rowIndex + 1,
                        payload: column
                    })
                });
            });
        });

        return new DotLayoutGrid(
            grid,
            dotLayoutBody.rows.map((row: DotLayoutRow) => row.styleClass)
        );
    }

    /**
     * Take an array of DotLayoutGridBox and return a DotLayoutBody.
     *
     * @param DotLayoutGridBox[] grid
     * @returns DotLayoutBody
     */
    getDotLayoutBody(grid: DotLayoutGrid): DotLayoutBody {
        return {
            rows: grid
                .getRows()
                .map((row: DotLayoutGridRow) => this.getLayoutRowFromLayoutGridBoxes(row))
        };
    }

    /**
     * Take an array of DotPageContainer and return a DotContainerColumnBox.
     *
     * @param DotPageContainer[] containers
     * @returns DotContainerColumnBox[]
     */
    getDotLayoutSidebar(containers: DotPageContainer[]): DotContainerColumnBox[] {
        return this.getDotContainerColumnBoxFromDotPageContainer(containers);
    }

    /**
     * Add box to the grid system
     *
     * @memberof DotEditLayoutService
     */
    addBox(): void {
        this._addGridBox.next(true);
    }

    /**
     * Get notified when a box is added to the grid system
     *
     * @returns {Observable<boolean>}
     * @memberof DotEditLayoutService
     */
    getBoxes(): Observable<boolean> {
        return this._addGridBox.asObservable();
    }

    private getDotContainerColumnBoxFromDotPageContainer(
        containers: DotPageContainer[]
    ): DotContainerColumnBox[] {
        return containers
            .filter((dotPageContainer: DotPageContainer) =>
                this.templateContainersCacheService.get(dotPageContainer.identifier)
            )
            .map((dotPageContainer: DotPageContainer) => {
                return {
                    container: this.templateContainersCacheService.get(dotPageContainer.identifier),
                    uuid: dotPageContainer.uuid ? dotPageContainer.uuid : ''
                };
            });
    }

    private getLayoutRowFromLayoutGridBoxes(dotLayoutGridRow: DotLayoutGridRow): DotLayoutRow {
        return {
            styleClass: dotLayoutGridRow.styleClass,
            columns: dotLayoutGridRow.boxes.map(this.getColumn.bind(this))
        };
    }

    private getColumn(layoutGridBox: DotLayoutGridBox): DotLayoutColumn {
        return {
            styleClass: layoutGridBox.config.payload.styleClass,
            leftOffset: layoutGridBox.config.col,
            width: layoutGridBox.config.sizex,
            containers: layoutGridBox.containers
                .filter((dotContainersColumnBox) => dotContainersColumnBox.container)
                .map(this.getContainer.bind(this))
        };
    }

    private getContainer(dotContainersColumnBox: DotContainerColumnBox): DotPageContainer {
        return {
            identifier: this.templateContainersCacheService.getContainerReference(
                dotContainersColumnBox.container
            ),
            uuid: dotContainersColumnBox.uuid
        };
    }
}
