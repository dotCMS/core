import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotCategoryFieldSearchListComponent } from './dot-category-field-search-list.component';

describe('DotCategoryFieldSearchListComponent', () => {
    let component: DotCategoryFieldSearchListComponent;
    let fixture: ComponentFixture<DotCategoryFieldSearchListComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotCategoryFieldSearchListComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotCategoryFieldSearchListComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
