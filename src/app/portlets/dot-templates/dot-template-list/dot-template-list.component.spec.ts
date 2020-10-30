import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotTemplateListComponent } from './dot-template-list.component';

xdescribe('DotTemplateListComponent', () => {
    let component: DotTemplateListComponent;
    let fixture: ComponentFixture<DotTemplateListComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotTemplateListComponent]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotTemplateListComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
