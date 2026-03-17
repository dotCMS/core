import { DotCMSContentletWorkflowActions, DotCMSWorkflowAction } from '@dotcms/dotcms-models';

export const mockWorkflowsActions: DotCMSWorkflowAction[] = [
    {
        assignable: true,
        commentable: true,
        condition: '',
        icon: 'workflowIcon',
        id: '44d4d4cd-c812-49db-adb1-1030be73e69a',
        name: 'Assign Workflow',
        nextAssign: 'db0d2bca-5da5-4c18-b5d7-87f02ba58eb6',
        nextStep: '43e16aac-5799-46d0-945c-83753af39426',
        nextStepCurrentStep: false,
        order: 0,
        roleHierarchyForAssign: true,
        schemeId: '85c1515c-c4f3-463c-bac2-860b8fcacc34',
        showOn: ['UNLOCKED', 'LOCKED'],
        actionInputs: [
            {
                body: {},
                id: 'assignable'
            },
            {
                body: {},
                id: 'commentable'
            },
            {
                body: {},
                id: 'pushPublish'
            },
            { body: {}, id: 'moveable' }
        ]
    },
    {
        assignable: false,
        commentable: false,
        condition: '',
        icon: 'workflowIcon',
        id: 'ceca71a0-deee-4999-bd47-b01baa1bcfc8',
        name: 'Save',
        nextAssign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
        nextStep: 'ee24a4cb-2d15-4c98-b1bd-6327126451f3',
        nextStepCurrentStep: false,
        order: 0,
        roleHierarchyForAssign: false,
        schemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
        showOn: ['LOCKED'],
        actionInputs: []
    },
    {
        assignable: false,
        commentable: false,
        condition: '',
        icon: 'workflowIcon',
        id: 'b9d89c80-3d88-4311-8365-187323c96436',
        name: 'Save / Publish',
        nextAssign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
        nextStep: 'dc3c9cd0-8467-404b-bf95-cb7df3fbc293',
        nextStepCurrentStep: false,
        order: 0,
        roleHierarchyForAssign: false,
        schemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
        showOn: ['LOCKED'],
        actionInputs: []
    }
];

export const mockWorkflowsActionsWithMove: DotCMSWorkflowAction[] = [
    {
        assignable: true,
        commentable: true,
        condition: '',
        icon: 'workflowIcon',
        id: '44d4d4cd-c812-49db-adb1-1030be73e69a',
        name: 'Assign Workflow',
        nextAssign: 'db0d2bca-5da5-4c18-b5d7-87f02ba58eb6',
        nextStep: '43e16aac-5799-46d0-945c-83753af39426',
        nextStepCurrentStep: false,
        order: 0,
        roleHierarchyForAssign: true,
        schemeId: '85c1515c-c4f3-463c-bac2-860b8fcacc34',
        showOn: ['UNLOCKED', 'LOCKED'],
        actionInputs: [
            {
                body: {},
                id: 'assignable'
            },
            {
                body: {},
                id: 'commentable'
            },
            {
                body: {},
                id: 'pushPublish'
            },
            { body: {}, id: 'moveable' }
        ]
    },
    {
        assignable: false,
        commentable: false,
        condition: '',
        icon: 'workflowIcon',
        id: 'ceca71a0-deee-4999-bd47-b01baa1bcfc8',
        name: 'Save',
        nextAssign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
        nextStep: 'ee24a4cb-2d15-4c98-b1bd-6327126451f3',
        nextStepCurrentStep: false,
        order: 0,
        roleHierarchyForAssign: false,
        schemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
        showOn: ['LOCKED'],
        actionInputs: []
    },
    {
        assignable: false,
        commentable: false,
        condition: '',
        icon: 'workflowIcon',
        id: 'b9d89c80-3d88-4311-8365-187323c96436',
        name: 'Save / Publish',
        nextAssign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
        nextStep: 'dc3c9cd0-8467-404b-bf95-cb7df3fbc293',
        nextStepCurrentStep: false,
        order: 0,
        roleHierarchyForAssign: false,
        schemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
        showOn: ['LOCKED'],
        actionInputs: []
    },
    {
        assignable: false,
        commentable: false,
        condition: '',
        icon: 'workflowIcon',
        id: 'dd4c4b7c-e9d3-4dc0-8fbf-36102f9c6324',
        name: 'Move',
        nextAssign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
        nextStep: '',
        nextStepCurrentStep: true,
        order: 0,
        roleHierarchyForAssign: true,
        schemeId: '85c1515c-c4f3-463c-bac2-860b8fcacc34',
        showOn: ['UNLOCKED', 'LOCKED'],
        actionInputs: [
            {
                body: {},
                id: 'moveable'
            }
        ]
    }
];

export const mockPublishAction = {
    assignable: false,
    commentable: false,
    condition: '',
    icon: 'workflowIcon',
    id: 'b9d89c80-3d88-4311-8365-187323c96436',
    name: 'Publish',
    nextAssign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
    nextStep: 'dc3c9cd0-8467-404b-bf95-cb7df3fbc293',
    nextStepCurrentStep: false,
    order: 0,
    owner: null,
    roleHierarchyForAssign: false,
    schemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
    showOn: ['LOCKED'],
    actionInputs: []
};

/**
 * Mock of a ContentType with single workflow actions
 */
export const MOCK_SINGLE_WORKFLOW_ACTIONS: DotCMSContentletWorkflowActions[] = [
    {
        scheme: {
            id: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
            archived: false,
            name: 'System Workflow',
            creationDate: new Date(1731532550440),
            defaultScheme: false,
            description: '',
            entryActionId: null,
            mandatory: false,
            modDate: new Date(1730906400422),
            system: true,
            variableName: 'SystemWorkflow'
        },
        action: {
            id: 'ceca71a0-deee-4999-bd47-b01baa1bcfc8',
            name: 'Save',
            icon: 'workflowIcon',
            showOn: ['EDITING', 'PUBLISHED', 'UNPUBLISHED', 'LOCKED', 'NEW'],
            assignable: false,
            commentable: false,
            condition: '',
            hasArchiveActionlet: false,
            hasCommentActionlet: false,
            hasDeleteActionlet: false,
            hasDestroyActionlet: false,
            hasMoveActionletActionlet: false,
            hasMoveActionletHasPathActionlet: false,
            hasOnlyBatchActionlet: false,
            hasPublishActionlet: false,
            hasPushPublishActionlet: false,
            hasResetActionlet: false,
            hasSaveActionlet: false,
            hasUnarchiveActionlet: false,
            hasUnpublishActionlet: false,
            metadata: undefined,
            nextAssign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
            nextStep: 'ee24a4cb-2d15-4c98-b1bd-6327126451f3',
            nextStepCurrentStep: false,
            order: 0,
            owner: undefined,
            roleHierarchyForAssign: false,
            schemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
            actionInputs: []
        },
        firstStep: {
            id: '6cb7e3bd-1710-4eed-8838-d3db60f78f19',
            name: 'New',
            creationDate: 1731595862064,
            enableEscalation: false,
            escalationAction: null,
            escalationTime: 0,
            myOrder: 0,
            resolved: false,
            schemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2'
        }
    },
    {
        scheme: {
            name: 'System Workflow',
            id: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
            system: true,
            creationDate: new Date(1731532550440),
            archived: false,
            defaultScheme: false,
            description: '',
            entryActionId: null,
            mandatory: false,
            modDate: new Date(1730906400422),
            variableName: 'SystemWorkflow'
        },
        action: {
            name: 'Translate',
            id: '8d567403-a201-42de-9a48-10cea8a7bdb2',
            icon: 'workflowIcon',
            showOn: ['EDITING', 'PUBLISHED', 'UNPUBLISHED', 'LOCKED', 'LISTING'],
            assignable: false,
            commentable: false,
            condition: '',
            hasArchiveActionlet: false,
            hasCommentActionlet: false,
            hasDeleteActionlet: false,
            hasDestroyActionlet: false,
            hasMoveActionletActionlet: false,
            hasMoveActionletHasPathActionlet: false,
            hasOnlyBatchActionlet: false,
            hasPublishActionlet: false,
            hasPushPublishActionlet: false,
            hasResetActionlet: false,
            hasSaveActionlet: false,
            hasUnarchiveActionlet: false,
            hasUnpublishActionlet: false,
            metadata: undefined,
            nextAssign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
            nextStep: 'f43c5d5a-fc51-4c67-a750-cc8f8e4a87f7',
            nextStepCurrentStep: false,
            order: 0,
            owner: undefined,
            roleHierarchyForAssign: false,
            schemeId: '2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd',
            actionInputs: []
        },
        firstStep: {
            name: 'New',
            resolved: false,
            id: '6cb7e3bd-1710-4eed-8838-d3db60f78f19',
            creationDate: 1731595862064,
            enableEscalation: false,
            escalationAction: null,
            escalationTime: 0,
            myOrder: 0,
            schemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2'
        }
    }
];

/**
 * Mock of a ContentType with multiple workflow actions
 */
export const MOCK_MULTIPLE_WORKFLOW_ACTIONS: DotCMSContentletWorkflowActions[] = [
    {
        scheme: {
            archived: false,
            creationDate: new Date(1731532550440),
            defaultScheme: false,
            description: '',
            entryActionId: null,
            id: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
            mandatory: false,
            modDate: new Date(1730906400422),
            name: 'System Workflow',
            system: true,
            variableName: 'SystemWorkflow'
        },
        firstStep: {
            creationDate: 1731595862064,
            enableEscalation: false,
            escalationAction: null,
            escalationTime: 0,
            id: '6cb7e3bd-1710-4eed-8838-d3db60f78f19',
            myOrder: 0,
            name: 'New',
            resolved: false,
            schemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2'
        },
        action: {
            id: 'ceca71a0-deee-4999-bd47-b01baa1bcfc8',
            name: 'Save',
            showOn: ['EDITING', 'PUBLISHED', 'UNPUBLISHED', 'LOCKED', 'NEW'],
            assignable: false,
            commentable: false,
            condition: '',
            hasArchiveActionlet: false,
            hasCommentActionlet: false,
            hasDeleteActionlet: false,
            hasDestroyActionlet: false,
            hasMoveActionletActionlet: false,
            hasMoveActionletHasPathActionlet: false,
            hasOnlyBatchActionlet: false,
            hasPublishActionlet: false,
            hasPushPublishActionlet: false,
            hasResetActionlet: false,
            hasSaveActionlet: false,
            hasUnarchiveActionlet: false,
            hasUnpublishActionlet: false,
            icon: 'workflowIcon',
            metadata: undefined,
            nextAssign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
            nextStep: 'ee24a4cb-2d15-4c98-b1bd-6327126451f3',
            nextStepCurrentStep: false,
            order: 0,
            owner: undefined,
            roleHierarchyForAssign: false,
            schemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
            actionInputs: []
        }
    },
    {
        firstStep: {
            creationDate: 1731595862066,
            enableEscalation: false,
            escalationAction: null,
            escalationTime: 0,
            id: '5865d447-5df7-4fa8-81c8-f8f183f3d1a2',
            myOrder: 0,
            name: 'Editing',
            resolved: false,
            schemeId: '2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd'
        },
        action: {
            name: 'Translate',
            icon: 'workflowIcon',
            actionInputs: [],
            assignable: false,
            commentable: false,
            condition: '',
            hasArchiveActionlet: false,
            hasCommentActionlet: false,
            hasDeleteActionlet: false,
            hasDestroyActionlet: false,
            hasMoveActionletActionlet: false,
            hasMoveActionletHasPathActionlet: false,
            hasOnlyBatchActionlet: false,
            hasPublishActionlet: false,
            hasPushPublishActionlet: false,
            hasResetActionlet: false,
            hasSaveActionlet: false,
            hasUnarchiveActionlet: false,
            hasUnpublishActionlet: false,
            id: '8d567403-a201-42de-9a48-10cea8a7bdb2',
            metadata: undefined,
            nextAssign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
            nextStep: 'f43c5d5a-fc51-4c67-a750-cc8f8e4a87f7',
            nextStepCurrentStep: false,
            order: 0,
            owner: undefined,
            roleHierarchyForAssign: false,
            schemeId: '2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd',
            showOn: ['EDITING', 'PUBLISHED', 'UNPUBLISHED', 'LOCKED', 'LISTING']
        },
        scheme: {
            archived: false,
            creationDate: new Date(1731532550430),
            defaultScheme: false,
            description: '',
            entryActionId: null,
            id: '2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd',
            mandatory: false,
            modDate: new Date(1730906400420),
            name: 'Blogs',
            system: false,
            variableName: 'Blogs'
        }
    }
];
