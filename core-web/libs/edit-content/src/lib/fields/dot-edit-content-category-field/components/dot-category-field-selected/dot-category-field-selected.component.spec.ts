import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotCategoryFieldSelectedComponent } from './dot-category-field-selected.component';

describe('DotCategoryFieldSelectedComponent', () => {
    let component: DotCategoryFieldSelectedComponent;
    let fixture: ComponentFixture<DotCategoryFieldSelectedComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotCategoryFieldSelectedComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotCategoryFieldSelectedComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
