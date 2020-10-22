import { NgGridItemConfig } from 'dot-layout-grid/public_api';
import { DotLayoutGridBox } from './dot-layout-grid-box.model';
import * as _ from 'lodash';
import { DotLayoutGridRow } from './dot-layout-grid-row.model';
import { DotContainerColumnBox } from './dot-container-column-box.model';

export const DOT_LAYOUT_GRID_MAX_COLUMNS = 12;
const DOT_LAYOUT_GRID_DEFAULT_EMPTY_GRID_ROWS: any = {
    fixed: true,
    sizex: 12,
    maxCols: 12,
    maxRows: 1,
    col: 1,
    row: 1,
    payload: {
        styleClass: ''
    }
};
const DEFAULT_CONFIG_FOR_NOT_EMPTY_GRID_TEMPLATE: any = {
    fixed: true,
    sizex: 3,
    maxCols: 12,
    maxRows: 1
};

/**
 * Layout using NgGrid box and DotLayoutGridBox
 *
 */

export class DotLayoutGrid {
    constructor(private dotLayoutGridBoxs: DotLayoutGridBox[], private rowClasses: string[]) {}

    static getDefaultConfig(): NgGridItemConfig {
        return {
            ...DEFAULT_CONFIG_FOR_NOT_EMPTY_GRID_TEMPLATE,
            ...{
                payload: {
                    styleClass: ''
                }
            }
        };
    }

    static getDefaultGrid(): DotLayoutGrid {
        const defaultBox: DotLayoutGridBox[] = [
            {
                config: <NgGridItemConfig>(
                    Object.assign({}, DOT_LAYOUT_GRID_DEFAULT_EMPTY_GRID_ROWS)
                ),
                containers: <DotContainerColumnBox[]>[]
            }
        ];
        return new DotLayoutGrid(defaultBox, ['']);
    }

    get boxes() {
        return this.dotLayoutGridBoxs;
    }

    getRows(): DotLayoutGridRow[] {
        return _.chain(this.boxes)
            .sortBy('config.row')
            .sortBy('config.col')
            .groupBy('config.row')
            .values()
            .map((dotLayoutGridBox: DotLayoutGridBox[], index: number) => {
                return {
                    boxes: dotLayoutGridBox,
                    styleClass: this.rowClasses[index]
                };
            })
            .value();
    }

    box(index: number): DotLayoutGridBox {
        return this.dotLayoutGridBoxs[index];
    }

    addBox(): void {
        const conf: NgGridItemConfig = this.getConfigOfNewBox();
        this.dotLayoutGridBoxs.push({ config: conf, containers: [] });
        this.setRowClases();
    }

    deleteEmptyRows(): void {
        this.dotLayoutGridBoxs = _.chain(this.dotLayoutGridBoxs)
            .sortBy('config.row')
            .groupBy('config.row')
            .values()
            .map(this.updateContainerIndex)
            .flatten()
            .value();
    }

    getRowClass(index: number): string {
        return this.rowClasses[index];
    }

    getAllRowClass(): string[] {
        return this.rowClasses;
    }

    setRowClass(value: string, index: number): void {
        this.rowClasses[index] = value;
    }

    removeContainer(index: number): void {
        this.dotLayoutGridBoxs.splice(index, 1);
        this.deleteEmptyRows();
        this.setRowClases();
    }

    private setRowClases(): void {
        const newNRows = this.dotLayoutGridBoxs
            .map((box: DotLayoutGridBox) => box.config.row)
            .reduce((before: number, current: number) => {
                return before > current ? before : current;
            }, 0);

        if (this.rowClasses.length > newNRows) {
            this.rowClasses = this.rowClasses.splice(0, this.rowClasses.length - newNRows);
        } else {
            this.rowClasses = [
                ...this.rowClasses,
                ...Array(newNRows - this.rowClasses.length).fill(null)
            ];
        }
    }

    private getConfigOfNewBox(): NgGridItemConfig {
        const newRow: NgGridItemConfig = DotLayoutGrid.getDefaultConfig();

        if (this.dotLayoutGridBoxs.length) {
            const lastContainer = _.chain(this.dotLayoutGridBoxs)
                .groupBy('config.row')
                .values()
                .last()
                .maxBy('config.col')
                .value();

            let busyColumns: number = DEFAULT_CONFIG_FOR_NOT_EMPTY_GRID_TEMPLATE.sizex;
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

    private updateContainerIndex(rowArray, index) {
        if (rowArray[0].row !== index + 1) {
            return rowArray.map((container) => {
                container.config.row = index + 1;
                return container;
            });
        }
        return rowArray;
    }
}
