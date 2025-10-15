export enum ENUM_WORKFLOW_ACTIONS {
    NEW = 'NEW',
    EDIT = 'EDIT',
    PUBLISH = 'PUBLISH',
    UNPUBLISH = 'UNPUBLISH',
    ARCHIVE = 'ARCHIVE',
    UNARCHIVE = 'UNARCHIVE',
    DELETE = 'DELETE',
    DESTROY = 'DESTROY'
}

export interface WorkflowActionVisibilityRules {
    emptySelection?: boolean; // Hide when there is no selection
    contentSelected?: boolean; // Hide when content is selected
    multiSelection?: boolean; // Hide in multi-selection mode
    archived?: boolean; // Hide when content is archived (true)
    published?: boolean; // Hide when content is published (true)
}

export interface WorkflowAction {
    name: string;
    id: ENUM_WORKFLOW_ACTIONS;
    hideWhen?: WorkflowActionVisibilityRules;
}

const NEW_WORKFLOW_ACTIONS: WorkflowAction = {
    name: 'new',
    id: ENUM_WORKFLOW_ACTIONS.NEW,
    hideWhen: {
        contentSelected: true
    }
};

const EDIT_WORKFLOW_ACTIONS: WorkflowAction = {
    name: 'edit',
    id: ENUM_WORKFLOW_ACTIONS.EDIT,
    hideWhen: {
        multiSelection: true, // Edit should not be displayed in multi-selection
        emptySelection: true // Edit should not be displayed when there is no selection
    }
};

const PUBLISH_WORKFLOW_ACTIONS: WorkflowAction = {
    name: 'publish',
    id: ENUM_WORKFLOW_ACTIONS.PUBLISH,
    hideWhen: {
        archived: true, // Hide when archived
        emptySelection: true, // Hide when there is no selection
        published: true // Hide when already published (avoid duplication)
    }
};

const UNPUBLISH_WORKFLOW_ACTIONS: WorkflowAction = {
    name: 'unpublish',
    id: ENUM_WORKFLOW_ACTIONS.UNPUBLISH,
    hideWhen: {
        archived: true, // Hide when archived
        published: false, // Hide when unpublished (avoid duplication)
        emptySelection: true // Hide when there is no selection
    }
};

const ARCHIVE_WORKFLOW_ACTIONS: WorkflowAction = {
    name: 'archive',
    id: ENUM_WORKFLOW_ACTIONS.ARCHIVE,
    hideWhen: {
        archived: true, // Hide when already archived
        emptySelection: true // Hide when there is no selection
    }
};

const UNARCHIVE_WORKFLOW_ACTIONS: WorkflowAction = {
    name: 'unarchive',
    id: ENUM_WORKFLOW_ACTIONS.UNARCHIVE,
    hideWhen: {
        archived: false, // Hide when NOT archived (only show when archived)
        emptySelection: true // Hide when there is no selection
    }
};

const DELETE_WORKFLOW_ACTIONS: WorkflowAction = {
    name: 'delete',
    id: ENUM_WORKFLOW_ACTIONS.DELETE,
    hideWhen: {
        archived: false, // Hide when NOT archived (only show when archived)
        emptySelection: true // Hide when there is no selection
    }
};

const DESTROY_WORKFLOW_ACTIONS: WorkflowAction = {
    name: 'destroy',
    id: ENUM_WORKFLOW_ACTIONS.DESTROY,
    hideWhen: {
        archived: false, // Hide when NOT archived (only show when archived)
        emptySelection: true // Hide when there is no selection
    }
};

export const DEFAULT_WORKFLOW_ACTIONS = [
    NEW_WORKFLOW_ACTIONS,
    EDIT_WORKFLOW_ACTIONS,
    PUBLISH_WORKFLOW_ACTIONS,
    UNPUBLISH_WORKFLOW_ACTIONS,
    ARCHIVE_WORKFLOW_ACTIONS,
    UNARCHIVE_WORKFLOW_ACTIONS,
    DELETE_WORKFLOW_ACTIONS,
    DESTROY_WORKFLOW_ACTIONS
];
