import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotContentDriveWorkflowActionsComponent } from './dot-content-drive-workflow-actions.component';

describe('DotContentDriveWorkflowActionsComponent', () => {
    let component: DotContentDriveWorkflowActionsComponent;
    let fixture: ComponentFixture<DotContentDriveWorkflowActionsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotContentDriveWorkflowActionsComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotContentDriveWorkflowActionsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
