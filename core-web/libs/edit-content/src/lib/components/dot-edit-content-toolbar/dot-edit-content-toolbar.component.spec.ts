import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ToolbarModule } from 'primeng/toolbar';

import { DotWorkflowActionsComponent } from '@dotcms/ui';
import { mockWorkflowsActions } from '@dotcms/utils-testing';

import { DotEditContentToolbarComponent } from './dot-edit-content-toolbar.component';

const WORKFLOW_ACTIONS_MOCK = [...mockWorkflowsActions, ...mockWorkflowsActions];

describe('DotEditContentToolbarComponent', () => {
    let spectator: Spectator<DotEditContentToolbarComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentToolbarComponent,
        imports: [ToolbarModule, DotWorkflowActionsComponent],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                actions: WORKFLOW_ACTIONS_MOCK
            } as unknown
        });
        spectator.detectComponentChanges();
    });

    it('should dot-workflow-actions component with the correct input', () => {
        const component = spectator.query(DotWorkflowActionsComponent);
        expect(component).toBeTruthy();
        expect(component.actions()).toEqual(WORKFLOW_ACTIONS_MOCK);
        expect(component.groupActions()).toBeTruthy();
        expect(component.size()).toBe('normal');
    });

    it('should emit the action dot-workflow-actions emits the fired action', () => {
        const spy = jest.spyOn(spectator.component.$actionFired, 'emit');
        const component = spectator.query(DotWorkflowActionsComponent);

        component.actionFired.emit(WORKFLOW_ACTIONS_MOCK[0]);

        expect(component).toBeTruthy();
        expect(spy).toHaveBeenCalledWith(WORKFLOW_ACTIONS_MOCK[0]);
    });
});
