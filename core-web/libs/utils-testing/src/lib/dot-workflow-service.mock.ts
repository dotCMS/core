import { of as observableOf, Observable } from 'rxjs';

import { DotCMSWorkflow } from '@dotcms/dotcms-models';

export const mockWorkflows: DotCMSWorkflow[] = [
    {
        id: '85c1515c-c4f3-463c-bac2-860b8fcacc34',
        creationDate: new Date(1522938093320),
        name: 'Default Scheme',
        description: 'This is the default workflow scheme that will be applied to all content',
        archived: false,
        mandatory: false,
        defaultScheme: true,
        modDate: new Date(1522881184568),
        entryActionId: null,
        system: false
    },
    {
        id: '77a9bf3f-a402-4c56-9b1f-1050b9d345dc',
        creationDate: new Date(1522938093321),
        name: 'Document Management',
        description: 'Default workflow for documents',
        archived: true,
        mandatory: false,
        defaultScheme: false,
        modDate: new Date(1522794958967),
        entryActionId: null,
        system: false
    },
    {
        id: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
        creationDate: new Date(1522938093321),
        name: 'System Workflow',
        description: '',
        archived: false,
        mandatory: false,
        defaultScheme: false,
        modDate: new Date(1522794958958),
        entryActionId: null,
        system: true
    }
];

export const WORKFLOW_SCHEMA_MOCK = {
    contentTypeSchemes: [
        {
            archived: false,
            creationDate: 1713712903527,
            defaultScheme: false,
            description: '',
            entryActionId: null,
            id: '2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd',
            mandatory: false,
            modDate: 1713700998143,
            name: 'Blogs',
            system: false
        }
    ],
    schemes: [
        {
            archived: false,
            creationDate: 1713718887000,
            defaultScheme: false,
            description: '',
            entryActionId: null,
            id: '2a4e1d2e-5342-4b46-be3d-80d3a2d9c0dd',
            mandatory: false,
            modDate: 1713700998143,
            name: 'Blogs',
            system: false
        }
    ]
};

export const WORKFLOW_STATUS_MOCK = {
    scheme: {
        archived: false,
        creationDate: 1713718841367,
        defaultScheme: false,
        description: '',
        entryActionId: null,
        id: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
        mandatory: false,
        modDate: 1713700998153,
        name: 'System Workflow',
        system: true
    },
    step: {
        creationDate: 1713713102111,
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
        creationDate: 1564530075838,
        description: '',
        dueDate: null,
        id: '26e58222-2c79-4879-93cb-982df8f84a7d',
        inode: '26e58222-2c79-4879-93cb-982df8f84a7d',
        languageId: 1,
        modDate: 1700505024201,
        new: false,
        status: 'dc3c9cd0-8467-404b-bf95-cb7df3fbc293',
        title: 'Snow',
        webasset: '684a7b76-315a-48af-9ea8-967cce78ee98'
    }
};

export class DotWorkflowServiceMock {
    get(): Observable<DotCMSWorkflow[]> {
        return observableOf(structuredClone(mockWorkflows));
    }

    getSystem(): Observable<DotCMSWorkflow> {
        const systemWorkflow = mockWorkflows.filter((workflow: DotCMSWorkflow) => workflow.system);

        return observableOf(structuredClone(systemWorkflow[0]));
    }
}
