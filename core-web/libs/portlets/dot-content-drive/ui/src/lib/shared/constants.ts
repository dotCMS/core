import { DotFolderListViewColumn } from './models';

export const HEADER_COLUMNS: DotFolderListViewColumn[] = [
    { field: 'title', header: 'title', width: '45%' },
    { field: 'live', header: 'status', width: '10%' },
    { field: 'baseType', header: 'type', sortable: true, width: '10%' },
    { field: 'modUserName', header: 'Edited-By', width: '15%' },
    { field: 'modDate', header: 'Last-Edited', sortable: true, width: '15%' }
];
