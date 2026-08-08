import { TreeNodeItem } from '@dotcms/dotcms-models';

/** Identifier of the synthetic "System Host" site. Not a browsable site. */
export const SYSTEM_HOST_ID = 'SYSTEM_HOST';

/**
 * Synthetic root node of a folder tree: selecting it means "everything on this site", not a folder.
 *
 * Consumers clone it per site to fill in `data.id` / `data.hostname` — see Content Drive's
 * `withSidebar` and the AssetPicker store. Shared across Content Drive and AssetPicker.
 *
 * The label keeps its `content-drive.*` i18n key: the string is already translated under that key in
 * `Language.properties`, and renaming keys is separate work.
 */
export const ALL_FOLDER: TreeNodeItem = {
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
