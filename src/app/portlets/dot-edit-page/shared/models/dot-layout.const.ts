import { NgGridItemConfig } from 'angular2-grid';
import { DotLayoutGridBox } from './dot-layout-grid-box.model';
import { DotContainer } from './dot-container.model';

export const DOT_LAYOUT_GRID_MAX_COLUMNS = 12;
export const DOT_LAYOUT_GRID_NEW_ROW_TEMPLATE: NgGridItemConfig = { fixed: true, sizex: 3, maxCols: 12, maxRows: 1 };
export const DOT_LAYOUT_GRID_DEFAULT_EMPTY_GRID_ROWS: NgGridItemConfig = {
    fixed: true,
    sizex: 12,
    maxCols: 12,
    maxRows: 1,
    col: 1,
    row: 1
};

export const DOT_LAYOUT_DEFAULT_GRID: DotLayoutGridBox[] = [
    {
        config: <NgGridItemConfig>Object.assign({}, DOT_LAYOUT_GRID_DEFAULT_EMPTY_GRID_ROWS),
        containers: <DotContainer[]>[]
    }
];
