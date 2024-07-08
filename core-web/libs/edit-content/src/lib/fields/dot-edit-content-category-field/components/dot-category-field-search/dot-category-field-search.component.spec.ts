import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotCategoryFieldSearchComponent } from './dot-category-field-search.component';

describe('DotCategoryFieldSearchComponent', () => {
    let component: DotCategoryFieldSearchComponent;
    let fixture: ComponentFixture<DotCategoryFieldSearchComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotCategoryFieldSearchComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotCategoryFieldSearchComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
