import { Injectable } from '@angular/core';
import { DotLayoutBody } from '../../shared/models/dot-layout-body.model';
import { DotLayoutGridBox } from '../../shared/models/dot-layout-grid-box.model';
import { DotLayoutRow } from '../../shared/models/dot-layout-row.model';
import { DotLayoutColumn } from '../../shared/models/dot-layout-column.model';
import * as _ from 'lodash';
import { TemplateContainersCacheService } from '../../template-containers-cache.service';
import { DotPageContainer } from '../models/dot-page-container.model';
import { DotContainerColumnBox } from '../models/dot-container-column-box.model';
import { DotLayoutGrid } from '../models/dot-layout-grid.model';
import { DotLayoutGridRow } from '../models/dot-layout-grid-row.model';

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
     * @param DotLayoutBody dotLayoutBody
     * @returns DotLayoutGridBox[]
     */
    getDotLayoutGridBox(dotLayoutBody: DotLayoutBody): DotLayoutGrid {
        const grid: DotLayoutGridBox[] = [];

        dotLayoutBody.rows.forEach((row, rowIndex) => {
            row.columns.forEach(column => {
                const payload: any = column.styleClass ?
                    {
                        payload: {
                            styleClass: column.styleClass
                        }
                    } : {};

                grid.push({
                    containers: this.getDotContainerColumnBoxFromDotPageContainer(
                        column.containers
                    ),
                    config: Object.assign(DotLayoutGrid.getDefaultConfig(), {
                        sizex: column.width,
                        col: column.leftOffset,
                        row: rowIndex + 1,
                        ...payload
                    })
                });
            });
        });

        return new DotLayoutGrid(grid, dotLayoutBody.rows.map(row => row.styleClass));
    }

    /**
     * Take an array of DotLayoutGridBox and return a DotLayoutBody.
     *
     * @param DotLayoutGridBox[] grid
     * @returns DotLayoutBody
     */
    getDotLayoutBody(grid: DotLayoutGrid): DotLayoutBody {
        return <DotLayoutBody> {
            rows: grid.getRows().map(row => this.getLayoutRowFromLayoutGridBoxes(row))
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

    private getDotContainerColumnBoxFromDotPageContainer(
        containers: DotPageContainer[]
    ): DotContainerColumnBox[] {
        return containers
            .filter(dotPageContainer =>
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
            ...dotLayoutGridRow.styleClass ? { styleClass: dotLayoutGridRow.styleClass } : {},
            columns: dotLayoutGridRow.boxes.map(
                (layoutGridBox: DotLayoutGridBox) =>
                     <DotLayoutColumn>{
                        leftOffset: layoutGridBox.config.col,
                        width: layoutGridBox.config.sizex,
                        ...this.getPayload(layoutGridBox),
                        containers: layoutGridBox.containers
                            .filter(dotContainersColumnBox => dotContainersColumnBox.container)
                            .map(
                                (dotContainersColumnBox: DotContainerColumnBox) =>
                                    <DotPageContainer>{
                                        identifier: this.templateContainersCacheService.getContainerReference(
                                            dotContainersColumnBox.container
                                        ),
                                        uuid: dotContainersColumnBox.uuid
                                    }
                            ),
                        }
            )
        };
    }

    private getPayload(layoutGridBox: DotLayoutGridBox): any {
        return layoutGridBox.config.payload && layoutGridBox.config.payload.styleClass
            ? {styleClass: layoutGridBox.config.payload.styleClass }
            : {};
    }
}
