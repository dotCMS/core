import { LOAD_MORE_NODE_TYPE } from '@dotcms/dotcms-models';

import { DotFolderTreeNodeItem } from './models';

export { LOAD_MORE_NODE_TYPE };

export const SYSTEM_HOST_ID = 'SYSTEM_HOST';

/** i18n key for the "Load more" node label. */
export const LOAD_MORE_LABEL_KEY = 'content-drive.tree.load-more';

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
