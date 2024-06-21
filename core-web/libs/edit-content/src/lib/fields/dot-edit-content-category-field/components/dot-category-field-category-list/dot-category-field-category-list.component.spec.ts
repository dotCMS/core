import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotCategoryFieldCategoryListComponent } from './dot-category-field-category-list.component';

describe('DotCategoryFieldCategoryListComponent', () => {
    let component: DotCategoryFieldCategoryListComponent;
    let fixture: ComponentFixture<DotCategoryFieldCategoryListComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotCategoryFieldCategoryListComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotCategoryFieldCategoryListComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
