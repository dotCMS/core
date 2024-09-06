import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';

import { SkeletonModule } from 'primeng/skeleton';

import { DotWorkflowService } from '@dotcms/data-access';
import {
    DotMessagePipe,
    WORKFLOW_SCHEMA_MOCK,
    WORKFLOW_STATUS_MOCK,
    dotcmsContentTypeBasicMock
} from '@dotcms/utils-testing';

import { DotContentAsideWorkflowComponent } from './dot-content-aside-workflow.component';

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
                    getSchemaContentType: () => of(WORKFLOW_SCHEMA_MOCK)
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
