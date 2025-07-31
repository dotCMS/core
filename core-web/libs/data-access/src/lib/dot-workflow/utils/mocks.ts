import { DotCMSWorkflow, DotCMSWorkflowStatus } from '@dotcms/dotcms-models';

export const DotCMSWorkflowMock: DotCMSWorkflow[] = [
    {
        archived: false,
        creationDate: new Date(),
        defaultScheme: false,
        description: '',
        entryActionId: null,
        id: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
        mandatory: false,
        modDate: new Date(),
        name: 'System Workflow',
        system: true
    }
];
export const mockWorkflowstatus: DotCMSWorkflowStatus = {
    scheme: {
        archived: false,
        creationDate: new Date(),
        defaultScheme: false,
        description: '',
        entryActionId: null,
        id: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
        mandatory: false,
        modDate: new Date(),
        name: 'System Workflow',
        system: true
    },
    step: {
        creationDate: 1702312007455,
        enableEscalation: false,
        escalationAction: null,
        escalationTime: 0,
        id: 'dc3c9cd0-8467-404b-bf95-cb7df3fbc293',
        myOrder: 2,
        name: 'Published',
        resolved: true,
        schemeId: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2'
    },
    task: {
        assignedTo: 'Admin User',
        belongsTo: null,
        createdBy: 'e7d4e34e-5127-45fc-8123-d48b62d510e3',
        creationDate: 1702315744946,
        description: '',
        dueDate: null,
        id: '499b58f8-26cd-4931-9dca-677fd6040b31',
        inode: '499b58f8-26cd-4931-9dca-677fd6040b31',
        languageId: 1,
        modDate: 1702315744946,
        new: false,
        status: 'dc3c9cd0-8467-404b-bf95-cb7df3fbc293',
        title: 'Title',
        webasset: 'df97446ee21e84249e618c27a408e818'
    }
};
