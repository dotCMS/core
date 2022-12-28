import { DotLayoutColumn } from './dot-layout-column.model';

export interface DotLayoutRow {
    styleClass?: string;
    columns: DotLayoutColumn[];
}
