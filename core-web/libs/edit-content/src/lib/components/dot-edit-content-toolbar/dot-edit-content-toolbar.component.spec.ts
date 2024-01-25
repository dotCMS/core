import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Component, EventEmitter, Input, Output } from '@angular/core';

import { ToolbarModule } from 'primeng/toolbar';

import { DotCMSWorkflowAction } from '@dotcms/dotcms-models';
import { DotWorkflowActionsComponent } from '@dotcms/ui';
import { mockWorkflowsActions } from '@dotcms/utils-testing';

import { DotEditContentToolbarComponent } from './dot-edit-content-toolbar.component';

const WORKFLOW_ACTIONS_MOCK = [...mockWorkflowsActions, ...mockWorkflowsActions];

@Component({
    selector: 'dot-workflow-actions',
    template: '1',
    standalone: true
})
class MockDotWorkflowActionsComponent {
    @Input() actions: DotCMSWorkflowAction[];
    @Input() groupAction: boolean;
    @Output() actionFired = new EventEmitter();
}

describe('DotEditContentToolbarComponent', () => {
    let spectator: Spectator<DotEditContentToolbarComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentToolbarComponent,
        // OVERRIDE COMPONENT TO FIX JEST ISSUE WITH SIGNAL COMPONENTS
        overrideComponents: [
            [
                DotEditContentToolbarComponent,
                {
                    remove: { imports: [DotWorkflowActionsComponent] },
                    add: { imports: [MockDotWorkflowActionsComponent] }
                }
            ]
        ],
        imports: [ToolbarModule, MockDotWorkflowActionsComponent],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                actions: WORKFLOW_ACTIONS_MOCK
            }
        });
        spectator.detectComponentChanges();
    });

    it('should dot-workflow-actions component with the correct input', () => {
        const component = spectator.query(MockDotWorkflowActionsComponent);
        expect(component).toBeTruthy();
        expect(component.actions).toEqual(WORKFLOW_ACTIONS_MOCK);
        expect(component.groupAction).toBeTruthy();
    });

    it('should emit the action dot-workflow-actions emits the fired action', () => {
        const spy = jest.spyOn(spectator.component.actionFired, 'emit');
        const component = spectator.query(MockDotWorkflowActionsComponent);

        component.actionFired.emit(WORKFLOW_ACTIONS_MOCK[0]);

        expect(component).toBeTruthy();
        expect(spy).toHaveBeenCalledWith(WORKFLOW_ACTIONS_MOCK[0]);
    });
});
