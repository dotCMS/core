import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotEditContentSidebarSectionComponent } from './dot-edit-content-sidebar-section.component';

describe('DotEditContentSidebarSectionComponent', () => {
    let component: DotEditContentSidebarSectionComponent;
    let fixture: ComponentFixture<DotEditContentSidebarSectionComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotEditContentSidebarSectionComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotEditContentSidebarSectionComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
