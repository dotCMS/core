import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotEditContentAsideSectionComponent } from './dot-edit-content-sidebar-section.component';

describe('DotEditContentAsideSectionComponent', () => {
    let component: DotEditContentAsideSectionComponent;
    let fixture: ComponentFixture<DotEditContentAsideSectionComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotEditContentAsideSectionComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotEditContentAsideSectionComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
