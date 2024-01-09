import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { SplitButton, SplitButtonModule } from 'primeng/splitbutton';
import { ToolbarModule } from 'primeng/toolbar';

import { DotCMSWorkflowAction } from '@dotcms/dotcms-models';
import { mockWorkflowsActions } from '@dotcms/utils-testing';

import { DotEditContentToolbarComponent } from './dot-edit-content-toolbar.component';

const WORKFLOW_ACTIONS_SEPARATOR_MOCK: DotCMSWorkflowAction = {
    assignable: true,
    commentable: true,
    condition: '',
    icon: 'workflowIcon',
    id: '44d4d4cd-c812-49db-adb1-1030be73e69a',
    name: 'SEPARATOR',
    nextAssign: 'db0d2bca-5da5-4c18-b5d7-87f02ba58eb6',
    nextStep: '43e16aac-5799-46d0-945c-83753af39426',
    nextStepCurrentStep: false,
    order: 0,
    owner: null,
    roleHierarchyForAssign: true,
    schemeId: '85c1515c-c4f3-463c-bac2-860b8fcacc34',
    showOn: ['UNLOCKED', 'LOCKED'],
    metadata: {
        subtype: 'SEPARATOR'
    },
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
};

const WORKFLOW_ACTIONS_MOCK = [
    ...mockWorkflowsActions,
    WORKFLOW_ACTIONS_SEPARATOR_MOCK,
    ...mockWorkflowsActions
];

describe('DotEditContentToolbarComponent', () => {
    let spectator: Spectator<DotEditContentToolbarComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentToolbarComponent,
        imports: [ToolbarModule, SplitButtonModule],
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

    it('should render an extra split button for each `SEPARATOR` Action', () => {
        const splitButtons = spectator.queryAll(SplitButton);
        expect(splitButtons.length).toBe(2);
    });

    it('should emit the action when click on a split button', () => {
        const spy = jest.spyOn(spectator.component.actionFired, 'emit');
        const splitButton = spectator.query('.p-splitbutton > button');
        splitButton.dispatchEvent(new Event('click'));

        expect(spy).toHaveBeenCalledWith(WORKFLOW_ACTIONS_MOCK[0]);
    });

    it('should render a normal button is a group has only one action', () => {
        spectator.setInput('actions', [WORKFLOW_ACTIONS_MOCK[0]]);
        spectator.detectChanges();

        const button = spectator.query('.p-button');
        expect(button).not.toBeNull();
    });
});
