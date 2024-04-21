import { Spectator, createComponentFactory } from '@ngneat/spectator';
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
        },
        {
            archived: false,
            creationDate: 1713718887000,
            defaultScheme: false,
            description: '',
            entryActionId: null,
            id: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
            mandatory: false,
            modDate: 1713700998153,
            name: 'System Workflow',
            system: true
        }
    ]
};

describe('DotContentAsideWorkflowComponent', () => {
    let spectator: Spectator<DotContentAsideWorkflowComponent>;
    const createComponent = createComponentFactory({
        component: DotContentAsideWorkflowComponent,
        imports: [HttpClientTestingModule, DotMessagePipe, SkeletonModule],
        providers: [
            {
                provide: DotWorkflowService,
                useValue: {
                    getWorkflowStatus: () => of({ status: 'APPROVED' }),
                    getSchemaContentType: () => of(workflowSchemaMock)
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                contentType: dotcmsContentTypeBasicMock
            },
            detectChanges: false
        });
    });

    it('should create', () => {
        spectator.detectChanges();
        expect(spectator.component).toBeTruthy();
    });
});
