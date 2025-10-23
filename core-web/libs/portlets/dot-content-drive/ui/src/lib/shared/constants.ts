import { DotFolderListViewColumn } from './models';

export const HEADER_COLUMNS: DotFolderListViewColumn[] = [
    { field: 'title', header: 'title', width: '40%', order: 1, sortable: true },
    { field: 'live', header: 'status', width: '5%', order: 2 },
    { field: 'languageId', header: 'locale', width: '10%', order: 3, sortable: true },
    { field: 'contentType', header: 'type', sortable: true, width: '15%', order: 4 },
    { field: 'modUser', header: 'Edited-By', width: '15%', order: 5, sortable: true },
    { field: 'modDate', header: 'Last-Edited', sortable: true, width: '5%', order: 6 },
    { field: 'actions', header: '', width: '5%', order: 7 }
].sort((a, b) => a.order - b.order); // Sort the columns by order, so the columns are in the correct order in the UI

export const SYSTEM_HOST_ID = 'SYSTEM_HOST';

/**
 * @export
 * @type DOT_DRAG_ITEM
 */
export const DOT_DRAG_ITEM = 'dotcms/item';
