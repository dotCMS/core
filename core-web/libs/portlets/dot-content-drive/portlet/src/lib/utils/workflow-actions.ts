import { DotContentDriveItem } from '@dotcms/dotcms-models';

export enum WORKFLOW_ACTION_ID {
    NEW = 'NEW',
    EDIT = 'EDIT',
    PUBLISH = 'PUBLISH',
    UNPUBLISH = 'UNPUBLISH',
    ARCHIVE = 'ARCHIVE',
    UNARCHIVE = 'UNARCHIVE',
    DELETE = 'DELETE',
    DESTROY = 'DESTROY',
    RENAME = 'RENAME',
    DOWNLOAD = 'DOWNLOAD'
}

type SelectionStats = {
    total: number;
    archived: number;
    live: number;
    working: number;
    assets: number;
};

export interface ActionShowConditions {
    hasSelection?: boolean;
    isSingleSelection?: boolean;
    allAreAssets?: boolean;
    allArchived?: boolean;
    allLive?: boolean;
    allWorking?: boolean;
    noneArchived?: boolean;
    noneLive?: boolean;
    noneWorking?: boolean;
}

export interface ContentDriveWorkflowAction {
    name: string;
    id: WORKFLOW_ACTION_ID;
    showWhen?: ActionShowConditions;
}

const EDIT_WORKFLOW_ACTION: ContentDriveWorkflowAction = {
    name: 'edit',
    id: WORKFLOW_ACTION_ID.EDIT,
    showWhen: {
        isSingleSelection: true,
        noneArchived: true
    }
};

const PUBLISH_WORKFLOW_ACTION: ContentDriveWorkflowAction = {
    name: 'publish',
    id: WORKFLOW_ACTION_ID.PUBLISH,
    showWhen: {
        noneArchived: true,
        noneLive: true
    }
};

const UNPUBLISH_WORKFLOW_ACTION: ContentDriveWorkflowAction = {
    name: 'unpublish',
    id: WORKFLOW_ACTION_ID.UNPUBLISH,
    showWhen: {
        noneArchived: true,
        allLive: true
    }
};

const ARCHIVE_WORKFLOW_ACTION: ContentDriveWorkflowAction = {
    name: 'archive',
    id: WORKFLOW_ACTION_ID.ARCHIVE,
    showWhen: {
        noneArchived: true
    }
};

const UNARCHIVE_WORKFLOW_ACTION: ContentDriveWorkflowAction = {
    name: 'unarchive',
    id: WORKFLOW_ACTION_ID.UNARCHIVE,
    showWhen: {
        allArchived: true
    }
};

const DELETE_WORKFLOW_ACTION: ContentDriveWorkflowAction = {
    name: 'delete',
    id: WORKFLOW_ACTION_ID.DELETE,
    showWhen: {
        allArchived: true
    }
};

const RENAME_WORKFLOW_ACTION: ContentDriveWorkflowAction = {
    name: 'rename',
    id: WORKFLOW_ACTION_ID.RENAME,
    showWhen: {
        isSingleSelection: true,
        noneArchived: true
    }
};

const DOWNLOAD_WORKFLOW_ACTION: ContentDriveWorkflowAction = {
    name: 'download-assets',
    id: WORKFLOW_ACTION_ID.DOWNLOAD,
    showWhen: {
        allAreAssets: true
    }
};

export const DEFAULT_WORKFLOW_ACTIONS = [
    EDIT_WORKFLOW_ACTION,
    RENAME_WORKFLOW_ACTION,
    PUBLISH_WORKFLOW_ACTION,
    UNPUBLISH_WORKFLOW_ACTION,
    DOWNLOAD_WORKFLOW_ACTION,
    ARCHIVE_WORKFLOW_ACTION,
    UNARCHIVE_WORKFLOW_ACTION,
    DELETE_WORKFLOW_ACTION
];

export const getActionConditions = (selectedItems: DotContentDriveItem[]): ActionShowConditions => {
    const stats = countSelectionStats(selectedItems);

    if (stats.total === 0) {
        return {
            hasSelection: false,
            isSingleSelection: false,
            allArchived: false,
            allLive: false,
            allWorking: false,
            noneArchived: false,
            noneLive: false,
            noneWorking: false,
            allAreAssets: false
        };
    }

    return {
        hasSelection: true,
        isSingleSelection: stats.total === 1,
        allArchived: stats.archived === stats.total,
        allLive: stats.live === stats.total,
        allWorking: stats.working === stats.total,
        noneArchived: stats.archived === 0,
        noneLive: stats.live === 0,
        noneWorking: stats.working === 0,
        allAreAssets: stats.assets === stats.total
    };
};

const countSelectionStats = (items: DotContentDriveItem[]): SelectionStats => {
    const total = items.length;

    const counters = items.reduce(
        (acc, item) => {
            if (item.archived) acc.archived++;
            if (item.live) acc.live++;
            if (item.working) acc.working++;
            if (['FILEASSET', 'DOTASSET'].includes(item.baseType)) acc.assets++;
            return acc;
        },
        { archived: 0, live: 0, working: 0, assets: 0 }
    );

    return { total, ...counters };
};
