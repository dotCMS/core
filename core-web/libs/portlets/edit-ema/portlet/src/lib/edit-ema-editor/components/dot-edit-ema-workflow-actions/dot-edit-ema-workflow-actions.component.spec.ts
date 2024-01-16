import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotEditEmaWorkflowActionsComponent } from './dot-edit-ema-workflow-actions.component';

describe('DotEditEmaWorkflowActionsComponent', () => {
    let component: DotEditEmaWorkflowActionsComponent;
    let fixture: ComponentFixture<DotEditEmaWorkflowActionsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotEditEmaWorkflowActionsComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotEditEmaWorkflowActionsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
