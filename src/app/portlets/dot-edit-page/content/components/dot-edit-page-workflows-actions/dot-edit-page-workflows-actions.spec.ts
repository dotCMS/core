import { async, ComponentFixture } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { SplitButton } from 'primeng/primeng';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotWorkflowServiceMock } from '../../../../../test/dot-workflow-service.mock';
import { Observable } from 'rxjs/Observable';
import { DotWorkflowAction } from '../../../../../shared/models/dot-workflow-action/dot-workflow-action.model';
import { DotWorkflowService } from '../../../../../api/services/dot-workflow/dot-workflow.service';
import { DotEditPageWorkflowsActionsComponent } from './dot-edit-page-workflows-actions.component';
import { CommonModule } from '@angular/common';
import { SplitButtonModule } from 'primeng/components/splitbutton/splitbutton';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

describe('DotEditPageWorkflowsActionsComponent', () => {
    let component: DotEditPageWorkflowsActionsComponent;
    let fixture: ComponentFixture<DotEditPageWorkflowsActionsComponent>;
    let de: DebugElement;
    let testbed;

    beforeEach(
        async(() => {
            testbed = DOTTestBed.configureTestingModule({
                imports: [RouterTestingModule, BrowserAnimationsModule],
                declarations: [DotEditPageWorkflowsActionsComponent],
                providers: [
                    {
                        provide: DotWorkflowService,
                        useClass: DotWorkflowServiceMock
                    }
                ]
            });
        })
    );

    beforeEach(() => {
        fixture = testbed.createComponent(DotEditPageWorkflowsActionsComponent);
        component = fixture.componentInstance;
        component.label = 'ACTIONS';
        component.inode = 'cc2cdf9c-a20d-4862-9454-2a76c1132123';
        de = fixture.debugElement;
    });

    it('should have a workflow actions element', () => {
        expect(de.query(By.css('.edit-page-toolbar__actions'))).toBeTruthy();
    });

    it('should have a workflow actions with label "ACTIONS"', () => {
        const btnLabel: HTMLElement = de.query(By.css('.edit-page-toolbar__actions')).nativeElement;
        fixture.detectChanges();
        expect(btnLabel.textContent).toContain('ACTIONS');
    });

    it('should set action split buttons params', () => {
        fixture.detectChanges();
        const actionsButton: SplitButton = de.query(By.css('.edit-page-toolbar__actions')).componentInstance;
        expect(actionsButton.model).toEqual([{ label: 'Assign Workflow' }, { label: 'Save' }, { label: 'Save / Publish' }]);
    });
});
