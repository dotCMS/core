import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotTemplateDesignerComponent } from './dot-template-designer.component';

xdescribe('DotTemplateDesignerComponent', () => {
    let component: DotTemplateDesignerComponent;
    let fixture: ComponentFixture<DotTemplateDesignerComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotTemplateDesignerComponent]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotTemplateDesignerComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
