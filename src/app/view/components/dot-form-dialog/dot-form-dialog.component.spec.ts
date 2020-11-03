import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotFormDialogComponent } from './dot-form-dialog.component';

xdescribe('DotFormDialogComponent', () => {
    let component: DotFormDialogComponent;
    let fixture: ComponentFixture<DotFormDialogComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotFormDialogComponent]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotFormDialogComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
