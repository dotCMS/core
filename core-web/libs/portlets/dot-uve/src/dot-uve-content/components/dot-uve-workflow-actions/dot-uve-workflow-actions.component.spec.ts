import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotUveWorkflowActionsComponent } from './dot-uve-workflow-actions.component';

describe('DotUveWorkflowActionsComponent', () => {
    let component: DotUveWorkflowActionsComponent;
    let fixture: ComponentFixture<DotUveWorkflowActionsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotUveWorkflowActionsComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotUveWorkflowActionsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
