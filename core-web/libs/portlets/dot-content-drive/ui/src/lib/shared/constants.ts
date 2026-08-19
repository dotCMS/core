import { LOAD_MORE_NODE_TYPE } from '@dotcms/dotcms-models';

import {
    DotFolderListViewColumn,
    DotFolderListViewColumnField,
    DotFolderTreeNodeItem
} from './models';

export { LOAD_MORE_NODE_TYPE };

export type DotFolderListViewFixedColumn = DotFolderListViewColumn & {
    field: DotFolderListViewColumnField;
};

const FIXED_COLUMNS: DotFolderListViewFixedColumn[] = [
    { field: 'title', header: 'name', width: '32%', order: 1, sortable: true },
    { field: 'live', header: 'status', width: '10%', order: 2 },
    { field: 'languageId', header: 'locale', width: '10%', order: 3, sortable: true },
    { field: 'contentType', header: 'type', sortable: true, width: '15%', order: 4 },
    { field: 'modUser', header: 'Edited-By', width: '15%', order: 5, sortable: true },
    { field: 'modDate', header: 'Last-Edited', sortable: true, width: '13%', order: 6 },
    { field: 'actions', header: '', width: '5%', order: 7 }
];

// Sorted by order so the columns render in the intended sequence. Kept off the literal above:
// calling `.sort()` on an annotated array literal drops the contextual typing, widening each
// `field` back to `string`.
export const HEADER_COLUMNS: DotFolderListViewFixedColumn[] = [...FIXED_COLUMNS].sort(
    (a, b) => a.order - b.order
);

export const SYSTEM_HOST_ID = 'SYSTEM_HOST';

/** i18n key for the "Load more" node label. */
export const LOAD_MORE_LABEL_KEY = 'content-drive.tree.load-more';

/**
 * @export
 * @type DOT_DRAG_ITEM
 */
export const DOT_DRAG_ITEM = 'dotcms/item';

/**
 * @export
 * @type ALL_FOLDER
 * @description All folder node
 */
export const ALL_FOLDER: DotFolderTreeNodeItem = {
    key: 'ALL_FOLDER',
    label: 'content-drive.all-folder.label',
    loading: false,
    data: {
        type: 'folder',
        path: '',
        hostname: '',
        id: '',
        inode: ''
    },
    icon: 'pi pi-folder',
    leaf: false,
    expanded: true
};
