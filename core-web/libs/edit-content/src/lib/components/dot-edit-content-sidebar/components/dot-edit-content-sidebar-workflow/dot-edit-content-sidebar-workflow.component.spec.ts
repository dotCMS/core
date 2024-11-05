import { Spectator, byTestId, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';

import { SkeletonModule } from 'primeng/skeleton';

import { DotMessageService } from '../../../../../../../data-access/src';
import { DotMessagePipe } from '../../../../../../../utils-testing/src/lib/dot-message-mock.pipe';
import { EXISTING_WORKFLOW_MOCK, NEW_WORKFLOW_MOCK } from '../../../../utils/mocks';
import { DotEditContentSidebarWorkflowComponent } from './dot-edit-content-sidebar-workflow.component';

describe('DotEditContentSidebarWorkflowComponent', () => {
    let spectator: Spectator<DotEditContentSidebarWorkflowComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentSidebarWorkflowComponent,
        imports: [DotMessagePipe, SkeletonModule],
        providers: [mockProvider(DotMessageService)]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });
    });

    describe('Workflow data', () => {
        it('should render existing workflow data', () => {
            spectator.setInput('isLoading', false);
            spectator.setInput('workflow', EXISTING_WORKFLOW_MOCK);
            spectator.detectChanges();

            expect(spectator.query(byTestId('workflow-name')).textContent.trim()).toBe('Blogs');
            expect(spectator.query(byTestId('workflow-step')).textContent.trim()).toBe('QA');
            expect(spectator.query(byTestId('workflow-assigned')).textContent.trim()).toBe(
                'Admin User'
            );
        });

        it('should render loading state', () => {
            spectator.setInput('isLoading', true);
            spectator.detectChanges();

            const skeletons = spectator.queryAll('p-skeleton');
            expect(skeletons.length).toBeGreaterThan(0);
        });

        it('should render new workflow state', () => {
            spectator.setInput('isLoading', false);
            spectator.setInput('workflow', NEW_WORKFLOW_MOCK);
            spectator.detectChanges();

            expect(spectator.query(byTestId('workflow-name')).textContent.trim()).toBe('Blogs');
            expect(spectator.query(byTestId('workflow-step')).textContent.trim()).toBe('New');
            expect(spectator.query(byTestId('workflow-assigned'))).toBeFalsy();
        });
    });
});
