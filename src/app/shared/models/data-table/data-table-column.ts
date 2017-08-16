export interface DataTableColumn {
    fieldName: string;
    format?: string;
    header: string;
    icon?: (any) => string;
    sortable?: boolean;
    textAlign?: string;
    width?: string;
}
