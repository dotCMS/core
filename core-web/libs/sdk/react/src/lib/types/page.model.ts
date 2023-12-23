export type RowModel = {
  columns: ColumnModel[];
};

export type ColumnModel = {
  leftOffset: number;
  width: number;
  containers: ContainerModel[];
};

export type ContainerModel = {
  id: string;
  type: string;
  content: string;
};

export interface PageModel {
  rows: RowModel[];
}
