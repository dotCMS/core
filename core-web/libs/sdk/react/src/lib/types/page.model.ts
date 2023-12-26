export type RowModel = {
    columns: ColumnModel[];
};

export type ColumnModel = {
    leftOffset: number;
    width: number;
    containers: ContainerModel[];
};

export type ContainerModel = {
    identifier: string;
    uuid: string;
    content: string;
};

export interface PageModel {
    rows: RowModel[];
}
