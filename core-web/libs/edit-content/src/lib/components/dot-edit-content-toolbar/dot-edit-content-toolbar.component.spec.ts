import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
// import { MockComponent } from 'ng-mocks';

import { Component, EventEmitter, Input, Output } from '@angular/core';

import { ToolbarModule } from 'primeng/toolbar';

// import { DotMessageService } from '@dotcms/data-access';
import { DotWorkflowActionsComponent } from '@dotcms/ui';
import { mockWorkflowsActions } from '@dotcms/utils-testing';

import { DotEditContentToolbarComponent } from './dot-edit-content-toolbar.component';

const WORKFLOW_ACTIONS_MOCK = [...mockWorkflowsActions, ...mockWorkflowsActions];
// Mock DotWorkflowActionsComponent
@Component({
    selector: 'dot-workflow-actions',
    template: '1',
    standalone: true
})
class MockDotWorkflowActionsComponent {
    @Input() actions: [];
    @Input() groupAction: boolean;
    @Output() actionFired = new EventEmitter();
}

describe('DotEditContentToolbarComponent', () => {
    let spectator: Spectator<DotEditContentToolbarComponent>;

    const createComponent = createComponentFactory({
        component: DotEditContentToolbarComponent,
        componentMocks: [DotWorkflowActionsComponent],
        componentViewProvidersMocks: [DotWorkflowActionsComponent],
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
        const component = spectator.query(DotWorkflowActionsComponent);
        expect(component).toBeTruthy();
        expect(component.actions).toEqual(WORKFLOW_ACTIONS_MOCK);
        expect(component.groupAction).toBeTruthy();
    });

    it('should emit the action dot-workflow-actions emits the fired action', () => {
        const spy = jest.spyOn(spectator.component.actionFired, 'emit');
        const component = spectator.query(DotWorkflowActionsComponent);

        component.actionFired.emit(WORKFLOW_ACTIONS_MOCK[0]);

        expect(component).toBeTruthy();
        expect(spy).toHaveBeenCalledWith(WORKFLOW_ACTIONS_MOCK[0]);
    });
});

// import { Component, EventEmitter, Input, Output } from '@angular/core';
// import { ComponentFixture, TestBed } from '@angular/core/testing';
// import { By } from '@angular/platform-browser';

// import { ToolbarModule } from 'primeng/toolbar';

// import { DotMessageService } from '@dotcms/data-access';
// import { DotWorkflowActionsComponent } from '@dotcms/ui';
// import { mockWorkflowsActions } from '@dotcms/utils-testing';

// import { DotEditContentToolbarComponent } from './dot-edit-content-toolbar.component';

// // Mock DotWorkflowActionsComponent
// @Component({
//     selector: 'dot-workflow-actions',
//     template: '1',
//     standalone: true,
// })
// class MockDotWorkflowActionsComponent {
//     @Input() actions: [];
//     @Input() groupAction: boolean;
//     @Output() actionFired = new EventEmitter();
// }

// const WORKFLOW_ACTIONS_MOCK = [...mockWorkflowsActions, ...mockWorkflowsActions];

// describe('DotEditContentToolbarComponent', () => {
//     let component: DotEditContentToolbarComponent;
//     let fixture: ComponentFixture<DotEditContentToolbarComponent>;

//     beforeEach(async () => {
//         await TestBed.configureTestingModule({
//             imports: [DotEditContentToolbarComponent],
//         }).overrideComponent(DotEditContentToolbarComponent, {
//             remove: {
//               imports: [ToolbarModule, DotWorkflowActionsComponent],
//             },
//             add: {
//               imports: [MockDotWorkflowActionsComponent, ToolbarModule],
//             },
//           })
//         .compileComponents();

//         fixture = TestBed.createComponent(DotEditContentToolbarComponent);
//         component = fixture.componentInstance;
//         component.actions = WORKFLOW_ACTIONS_MOCK;
//         fixture.detectChanges();
//     });

//     it('should dot-workflow-actions component with the correct input', () => {
//         const dotWorkflowActionsComponent = fixture.debugElement.query(By.css('dot-workflow-actions')).componentInstance;
//         expect(dotWorkflowActionsComponent).toBeTruthy();
//         expect(dotWorkflowActionsComponent.actions).toEqual(WORKFLOW_ACTIONS_MOCK);
//         expect(dotWorkflowActionsComponent.groupAction).toBeTruthy();
//     });

//     it('should emit the action dot-workflow-actions emits the fired action', () => {
//         const spy = jest.spyOn(component.actionFired, 'emit');
//         const dotWorkflowActionsComponent = fixture.debugElement.query(By.css('dot-workflow-actions')).componentInstance;

//         dotWorkflowActionsComponent.actionFired.emit(WORKFLOW_ACTIONS_MOCK[0]);

//         expect(dotWorkflowActionsComponent).toBeTruthy();
//         expect(spy).toHaveBeenCalledWith(WORKFLOW_ACTIONS_MOCK[0]);
//     });
// });
