import { DotContentDriveItem } from '@dotcms/dotcms-models';

import { isFolder } from './functions';

export const WORKFLOW_ACTION_ID = {
    NEW: 'NEW',
    GOT_TO_EDIT_CONTENTLET: 'GOT_TO_EDIT_CONTENTLET',
    GOT_TO_EDIT_PAGE: 'GOT_TO_EDIT_PAGE',
    PUBLISH: 'PUBLISH',
    UNPUBLISH: 'UNPUBLISH',
    ARCHIVE: 'ARCHIVE',
    UNARCHIVE: 'UNARCHIVE',
    DELETE: 'DELETE',
    DESTROY: 'DESTROY',
    COPY: 'COPY',
    MOVE: 'MOVE',
    RENAME: 'RENAME',
    DOWNLOAD: 'DOWNLOAD'
} as const;

export type WORKFLOW_ACTION_ID = (typeof WORKFLOW_ACTION_ID)[keyof typeof WORKFLOW_ACTION_ID];

type SelectionStats = {
    total: number;
    folders: number;
    archived: number;
    live: number;
    working: number;
    locked: number;
    assets: number;
    pages: number;
    contentlets: number;
};

export interface ActionShowConditions {
    hasSelection?: boolean;
    isSingleSelection?: boolean;
    allAreAssets?: boolean;
    allAreFolders?: boolean;
    allArchived?: boolean;
    allLive?: boolean;
    allWorking?: boolean;
    allLocked?: boolean;
    noneArchived?: boolean;
    noneLive?: boolean;
    noneWorking?: boolean;
    noneLocked?: boolean;
    noneFolder?: boolean;
    isPage?: boolean;
    isContentlet?: boolean;
    isFolder?: boolean;
}

export interface ContentDriveWorkflowAction {
    name: string;
    id: WORKFLOW_ACTION_ID;
    showWhen?: ActionShowConditions;
}

const GOT_TO_EDIT_CONTENTLET_ACTION: ContentDriveWorkflowAction = {
    name: 'content.drive.worflow.action.edit-content',
    id: WORKFLOW_ACTION_ID.GOT_TO_EDIT_CONTENTLET,
    showWhen: {
        isSingleSelection: true,
        noneArchived: true,
        isContentlet: true,
        noneFolder: true
    }
};

const GOT_TO_EDIT_PAGE_ACTION: ContentDriveWorkflowAction = {
    name: 'content.drive.worflow.action.edit-page',
    id: WORKFLOW_ACTION_ID.GOT_TO_EDIT_PAGE,
    showWhen: {
        isSingleSelection: true,
        noneArchived: true,
        isPage: true,
        noneFolder: true
    }
};

/*
 * Publish, Unpublish, Archive, Unarchive and Delete deliberately no longer live here. They are
 * offered by the Workflow Center dialog's Quick Actions, which shows how many of the selected items
 * each one applies to — something a flat toolbar button cannot express. Keeping them in both places
 * meant the same action appeared twice, reached by two different code paths.
 *
 * The toolbar keeps the actions the dialog does not cover: the two Edit entries, Rename and Download.
 */

const RENAME_ACTION: ContentDriveWorkflowAction = {
    name: 'content.drive.worflow.action.rename',
    id: WORKFLOW_ACTION_ID.RENAME,
    showWhen: {
        isSingleSelection: true,
        noneArchived: true,
        noneFolder: true
    }
};

const DOWNLOAD_ACTION: ContentDriveWorkflowAction = {
    name: 'download',
    id: WORKFLOW_ACTION_ID.DOWNLOAD,
    showWhen: {
        allAreAssets: true,
        isSingleSelection: true,
        noneFolder: true
    }
};

/**
 * Actions shown as flat buttons in the toolbar when a selection is active.
 *
 * The publication-lifecycle and removal actions are intentionally absent — the Workflow Center
 * dialog owns those now. See the note above `RENAME_ACTION`.
 */
export const DEFAULT_WORKFLOW_ACTIONS = [
    // Edit actions (most frequent)
    GOT_TO_EDIT_CONTENTLET_ACTION,
    GOT_TO_EDIT_PAGE_ACTION,
    RENAME_ACTION,
    // Asset operations
    DOWNLOAD_ACTION
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
            allLocked: false,
            noneArchived: false,
            noneLive: false,
            noneWorking: false,
            noneLocked: false,
            allAreAssets: false,
            isPage: false,
            isContentlet: false,
            allAreFolders: false,
            isFolder: false,
            noneFolder: false
        };
    }

    // For "none" properties, only set to true if there are no folders AND the counter is 0
    // Folders don't have archived/live/working/locked properties, so if folders exist, these should be false
    const nonFolderCount = stats.total - stats.folders;
    const noneArchived = stats.folders === 0 && stats.archived === 0;
    const noneLive = stats.folders === 0 && stats.live === 0;
    const noneWorking = stats.folders === 0 && stats.working === 0;
    const noneLocked = stats.folders === 0 && stats.locked === 0;

    return {
        hasSelection: true,
        isSingleSelection: stats.total === 1,
        allAreFolders: stats.folders === stats.total,
        allArchived: nonFolderCount > 0 && stats.archived === nonFolderCount,
        allLive: nonFolderCount > 0 && stats.live === nonFolderCount,
        allWorking: nonFolderCount > 0 && stats.working === nonFolderCount,
        allLocked: nonFolderCount > 0 && stats.locked === nonFolderCount,
        noneArchived,
        noneLive,
        noneWorking,
        noneLocked,
        allAreAssets: stats.assets === stats.total,
        isPage: stats.pages === stats.total,
        isContentlet: stats.contentlets === stats.total,
        isFolder: stats.folders === stats.total,
        noneFolder: stats.folders === 0
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
            if (isFolder(item)) {
                acc.folders++;
                return acc;
            }
            if (item.archived) acc.archived++;
            if (item.live) acc.live++;
            if (item.working) acc.working++;
            if (item.locked) acc.locked++;
            if (item.baseType === 'HTMLPAGE') acc.pages++;
            if (item.baseType === 'CONTENT') acc.contentlets++;
            if (['FILEASSET', 'DOTASSET'].includes(item.baseType)) acc.assets++;
            return acc;
        },
        {
            archived: 0,
            live: 0,
            working: 0,
            locked: 0,
            assets: 0,
            pages: 0,
            contentlets: 0,
            folders: 0
        }
    );

    return { total, ...counters };
};
