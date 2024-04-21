import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { SkeletonModule } from 'primeng/skeleton';

import { DotWorkflowService } from '@dotcms/data-access';
import { DotMessagePipe, dotcmsContentTypeBasicMock } from '@dotcms/utils-testing';

import { DotContentAsideWorkflowComponent } from './dot-content-aside-workflow.component';

const workflowSchemaMock = {
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

const WORKFLOW_STATUS_MOCK = {
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

const CONTENTTYPE_MOCK = {
    ...dotcmsContentTypeBasicMock,
    name: 'Blogs'
};

describe('DotContentAsideWorkflowComponent', () => {
    let spectator: Spectator<DotContentAsideWorkflowComponent>;
    let dotWorkflowService: DotWorkflowService;

    const createComponent = createComponentFactory({
        component: DotContentAsideWorkflowComponent,
        imports: [HttpClientTestingModule, DotMessagePipe, SkeletonModule],
        componentProviders: [
            {
                provide: DotWorkflowService,
                useValue: {
                    getWorkflowStatus: () => of(WORKFLOW_STATUS_MOCK),
                    getSchemaContentType: () => of(workflowSchemaMock)
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                contentType: CONTENTTYPE_MOCK
            },
            detectChanges: false
        });

        dotWorkflowService = spectator.inject(DotWorkflowService, true);
    });

    describe('New contentlet', () => {
        it('should call getSchemaContentType', () => {
            const spyWorkflow = jest.spyOn(dotWorkflowService, 'getSchemaContentType');
            spectator.detectChanges();
            expect(spyWorkflow).toHaveBeenCalledWith(CONTENTTYPE_MOCK.id);
        });

        it('should render aside workflow data', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('workflow-name')).textContent.trim()).toBe('Blogs');
            expect(spectator.query(byTestId('workflow-step')).textContent.trim()).toBe('New');
            expect(spectator.query(byTestId('workflow-content-type')).textContent.trim()).toBe(
                CONTENTTYPE_MOCK.name
            );
        });

        it('should not render assigned to', () => {
            spectator.detectChanges();
            expect(spectator.query(byTestId('workflow-assigned'))).toBeNull();
        });
    });

    describe('Existing contentlet', () => {
        let spyWorkflow: jest.SpyInstance;

        beforeEach(() => {
            spyWorkflow = jest.spyOn(dotWorkflowService, 'getWorkflowStatus');
            spectator.setInput('inode', '123');
            spectator.detectChanges();
        });

        it('should call getWorkflowStatus', () => {
            expect(spyWorkflow).toHaveBeenCalledWith('123');
        });

        it('should render aside workflow data', () => {
            expect(spectator.query(byTestId('workflow-name')).textContent.trim()).toBe(
                'System Workflow'
            );
            expect(spectator.query(byTestId('workflow-step')).textContent.trim()).toBe('Published');
            expect(spectator.query(byTestId('workflow-assigned')).textContent.trim()).toBe(
                'Admin User'
            );
            expect(spectator.query(byTestId('workflow-content-type')).textContent.trim()).toBe(
                CONTENTTYPE_MOCK.name
            );
        });
    });
});
