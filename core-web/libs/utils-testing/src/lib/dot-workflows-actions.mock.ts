import { DotCMSWorkflowAction } from '@dotcms/dotcms-models';

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
