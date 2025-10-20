import { DotContentDriveItem } from '@dotcms/dotcms-models';

export const WORKFLOW_ACTION_ID = {
    NEW: 'NEW',
    SAVE_AS_DRAFT: 'EDIT',
    GOT_TO_EDIT_CONTENTLET: 'GOT_TO_EDIT_CONTENTLET',
    GOT_TO_EDIT_PAGE: 'GOT_TO_EDIT_PAGE',
    PUBLISH: 'PUBLISH',
    UNPUBLISH: 'UNPUBLISH',
    ARCHIVE: 'ARCHIVE',
    UNARCHIVE: 'UNARCHIVE',
    DELETE: 'DELETE',
    DESTROY: 'DESTROY',
    RENAME: 'RENAME',
    DOWNLOAD: 'DOWNLOAD'
} as const;

export type WORKFLOW_ACTION_ID = (typeof WORKFLOW_ACTION_ID)[keyof typeof WORKFLOW_ACTION_ID];

type SelectionStats = {
    total: number;
    archived: number;
    live: number;
    working: number;
    assets: number;
    pages: number;
    contentlets: number;
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
    isPage?: boolean;
    isContentlet?: boolean;
}

export interface ContentDriveWorkflowAction {
    name: string;
    id: WORKFLOW_ACTION_ID;
    showWhen?: ActionShowConditions;
    /**
     * Optional confirmation message key to display before executing the action.
     * When present, the user will be prompted to confirm before the action is executed.
     * The value should be a message key that will be translated via DotMessageService.
     */
    confirmationMessage?: string;
}

const GOT_TO_EDIT_CONTENTLET_ACTION: ContentDriveWorkflowAction = {
    name: 'content.drive.worflow.action.edit-content',
    id: WORKFLOW_ACTION_ID.GOT_TO_EDIT_CONTENTLET,
    showWhen: {
        isSingleSelection: true,
        noneArchived: true,
        isContentlet: true
    }
};

const GOT_TO_EDIT_PAGE_ACTION: ContentDriveWorkflowAction = {
    name: 'content.drive.worflow.action.edit-page',
    id: WORKFLOW_ACTION_ID.GOT_TO_EDIT_PAGE,
    showWhen: {
        isSingleSelection: true,
        noneArchived: true,
        isPage: true
    }
};

const PUBLISH_ACTION: ContentDriveWorkflowAction = {
    name: 'Publish',
    id: WORKFLOW_ACTION_ID.PUBLISH,
    showWhen: {
        noneArchived: true,
        noneLive: true
    }
};

const UNPUBLISH_ACTION: ContentDriveWorkflowAction = {
    name: 'Unpublish',
    id: WORKFLOW_ACTION_ID.UNPUBLISH,
    showWhen: {
        noneArchived: true,
        allLive: true
    }
};

const SAVE_AS_DRAFT_ACTION: ContentDriveWorkflowAction = {
    name: 'content.drive.worflow.action.save-draft',
    id: WORKFLOW_ACTION_ID.SAVE_AS_DRAFT,
    showWhen: {
        noneArchived: true
    }
};

const ARCHIVE_ACTION: ContentDriveWorkflowAction = {
    name: 'Archive',
    id: WORKFLOW_ACTION_ID.ARCHIVE,
    showWhen: {
        noneArchived: true
    },
    confirmationMessage: 'content.drive.worflow.action.archive.confirm'
};

const UNARCHIVE_ACTION: ContentDriveWorkflowAction = {
    name: 'Unarchive',
    id: WORKFLOW_ACTION_ID.UNARCHIVE,
    showWhen: {
        allArchived: true
    },
    confirmationMessage: 'content.drive.worflow.action.unarchive.confirm'
};

const DELETE_ACTION: ContentDriveWorkflowAction = {
    name: 'Delete',
    id: WORKFLOW_ACTION_ID.DELETE,
    showWhen: {
        allArchived: true
    },
    confirmationMessage: 'content.drive.worflow.action.delete.confirm'
};

const RENAME_ACTION: ContentDriveWorkflowAction = {
    name: 'content.drive.worflow.action.rename',
    id: WORKFLOW_ACTION_ID.RENAME,
    showWhen: {
        isSingleSelection: true,
        noneArchived: true
    }
};

const DOWNLOAD_ACTION: ContentDriveWorkflowAction = {
    name: 'Download',
    id: WORKFLOW_ACTION_ID.DOWNLOAD,
    showWhen: {
        allAreAssets: true
    }
};

export const DEFAULT_WORKFLOW_ACTIONS = [
    // Edit actions (most frequent)
    GOT_TO_EDIT_CONTENTLET_ACTION,
    GOT_TO_EDIT_PAGE_ACTION,
    RENAME_ACTION,
    // Content state (publication lifecycle)
    SAVE_AS_DRAFT_ACTION,
    PUBLISH_ACTION,
    UNPUBLISH_ACTION,
    // Asset operations
    DOWNLOAD_ACTION,
    // Removal actions (increasing severity)
    ARCHIVE_ACTION,
    UNARCHIVE_ACTION,
    DELETE_ACTION
];

/**
 * Analyzes the selected items and returns conditions that determine
 * which workflow actions should be shown.
 *
 * @param selectedItems - Array of selected content drive items to analyze
 * @returns An object containing boolean conditions for action visibility
 */
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
            allAreAssets: false,
            isPage: false,
            isContentlet: false
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
        allAreAssets: stats.assets === stats.total,
        isPage: stats.pages === stats.total,
        isContentlet: stats.contentlets === stats.total
    };
};

/**
 * Counts and categorizes the selected items by their properties.
 * Tracks total count, archived status, publication states (live/working),
 * and base types (assets, pages, contentlets).
 *
 * @param items - Array of content drive items to analyze
 * @returns Statistics object with counts for each category
 */
const countSelectionStats = (items: DotContentDriveItem[]): SelectionStats => {
    const total = items.length;

    const counters = items.reduce(
        (acc, item) => {
            if (item.archived) acc.archived++;
            if (item.live) acc.live++;
            if (item.working) acc.working++;
            if (item.baseType === 'HTMLPAGE') acc.pages++;
            if (item.baseType === 'CONTENT') acc.contentlets++;
            if (['FILEASSET', 'DOTASSET'].includes(item.baseType)) acc.assets++;
            return acc;
        },
        { archived: 0, live: 0, working: 0, assets: 0, pages: 0, contentlets: 0 }
    );

    return { total, ...counters };
};
