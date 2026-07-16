import { DotFolderListViewColumn, DotFolderTreeNodeItem } from './models';

export const HEADER_COLUMNS: DotFolderListViewColumn[] = [
    { field: 'title', header: 'name', width: '32%', order: 1, sortable: true },
    { field: 'live', header: 'status', width: '10%', order: 2 },
    { field: 'languageId', header: 'locale', width: '10%', order: 3, sortable: true },
    { field: 'contentType', header: 'type', sortable: true, width: '15%', order: 4 },
    { field: 'modUser', header: 'Edited-By', width: '15%', order: 5, sortable: true },
    { field: 'modDate', header: 'Last-Edited', sortable: true, width: '13%', order: 6 },
    { field: 'actions', header: '', width: '5%', order: 7 }
].sort((a, b) => a.order - b.order); // Sort the columns by order, so the columns are in the correct order in the UI

export const SYSTEM_HOST_ID = 'SYSTEM_HOST';

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
    leaf: false,
    expanded: true
};

/**
 * Pass-through styling for the popover that hosts a chip-filter listbox.
 * Removes default content padding and rounds the corners.
 */
export const CHIP_FILTER_POPOVER_PT = {
    root: { class: '!rounded-lg overflow-hidden' },
    content: { class: '!p-0' }
};

/**
 * Pass-through styling for the listbox rendered inside a chip-filter popover.
 * Strips the listbox's own chrome, applies palette colors for selection/hover,
 * and sizes option padding + checkbox to the content-drive design spec.
 */
export const CHIP_FILTER_LISTBOX_PT = {
    root: {
        class: [
            '!border-0 !rounded-none !shadow-none',
            '[--p-listbox-option-padding:0_1rem]',
            '[--p-listbox-option-focus-background:var(--p-slate-50)]',
            '[--p-listbox-option-selected-color:var(--p-primary-700)]',
            '[--p-listbox-option-selected-focus-color:var(--p-primary-700)]',
            '[--p-listbox-option-selected-focus-background:var(--p-listbox-option-selected-background)]',
            '[--p-checkbox-width:16px] [--p-checkbox-height:16px]'
        ].join(' ')
    }
};
