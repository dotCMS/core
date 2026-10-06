export interface DataTableColumn {
    fieldName: string;
    format?: string;
    header: string;
    icon?: (rowData: { icon: string }) => string;
    sortable?: boolean;
    textAlign?: string;
    textContent?: string;
    width?: string;
}
