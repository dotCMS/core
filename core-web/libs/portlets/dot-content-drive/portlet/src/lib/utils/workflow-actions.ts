import { DotContentDriveItem } from '@dotcms/dotcms-models';

export enum ENUM_WORKFLOW_ACTIONS {
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

export interface ActionVisibilityConditions {
    contentSelected?: boolean;
    multiSelection?: boolean;
    assetsOnly?: boolean;
    archived?: boolean;
    lived?: boolean;
    working?: boolean;
}

export interface WorkflowAction {
    name: string;
    id: ENUM_WORKFLOW_ACTIONS;
    hideWhen?: ActionVisibilityConditions;
}

const EDIT_WORKFLOW_ACTIONS: WorkflowAction = {
    name: 'edit',
    id: ENUM_WORKFLOW_ACTIONS.EDIT,
    hideWhen: {
        multiSelection: true
    }
};

const PUBLISH_WORKFLOW_ACTIONS: WorkflowAction = {
    name: 'publish',
    id: ENUM_WORKFLOW_ACTIONS.PUBLISH,
    hideWhen: {
        archived: true
    }
};

const UNPUBLISH_WORKFLOW_ACTIONS: WorkflowAction = {
    name: 'unpublish',
    id: ENUM_WORKFLOW_ACTIONS.UNPUBLISH,
    hideWhen: {
        lived: false,
        archived: true
    }
};

const ARCHIVE_WORKFLOW_ACTIONS: WorkflowAction = {
    name: 'archive',
    id: ENUM_WORKFLOW_ACTIONS.ARCHIVE,
    hideWhen: {
        archived: true // Hide when already archived
    }
};

const UNARCHIVE_WORKFLOW_ACTIONS: WorkflowAction = {
    name: 'unarchive',
    id: ENUM_WORKFLOW_ACTIONS.UNARCHIVE,
    hideWhen: {
        archived: false // Hide when NOT archived (only show when archived)
    }
};

const DELETE_WORKFLOW_ACTIONS: WorkflowAction = {
    name: 'delete',
    id: ENUM_WORKFLOW_ACTIONS.DELETE,
    hideWhen: {}
};

const DESTROY_WORKFLOW_ACTIONS: WorkflowAction = {
    name: 'destroy',
    id: ENUM_WORKFLOW_ACTIONS.DESTROY,
    hideWhen: {
        archived: false // Hide when NOT archived (only show when archived)
    }
};

const RENAME_WORKFLOW_ACTIONS: WorkflowAction = {
    name: 'rename',
    id: ENUM_WORKFLOW_ACTIONS.RENAME,
    hideWhen: {
        multiSelection: true
    }
};

const DOWNLOAD_WORKFLOW_ACTIONS: WorkflowAction = {
    name: 'download-assets',
    id: ENUM_WORKFLOW_ACTIONS.DOWNLOAD,
    hideWhen: {
        assetsOnly: false
    }
};

export const DEFAULT_WORKFLOW_ACTIONS = [
    EDIT_WORKFLOW_ACTIONS,
    RENAME_WORKFLOW_ACTIONS,
    PUBLISH_WORKFLOW_ACTIONS,
    UNPUBLISH_WORKFLOW_ACTIONS,
    DOWNLOAD_WORKFLOW_ACTIONS,
    ARCHIVE_WORKFLOW_ACTIONS,
    UNARCHIVE_WORKFLOW_ACTIONS,
    DELETE_WORKFLOW_ACTIONS,
    DESTROY_WORKFLOW_ACTIONS
];

export const getActionVisibilityConditions = (
    selectedItems: DotContentDriveItem[]
): ActionVisibilityConditions => {
    return {
        contentSelected: selectedItems.length > 0,
        multiSelection: selectedItems.length > 1,
        archived: selectedItems.some((item) => item.archived),
        lived: selectedItems.some((item) => item.live),
        working: selectedItems.some((item) => item.working),
        assetsOnly: selectedItems.every(
            (item) => item.baseType === 'FILEASSET' || item.baseType === 'DOTASSET'
        )
    };
};
