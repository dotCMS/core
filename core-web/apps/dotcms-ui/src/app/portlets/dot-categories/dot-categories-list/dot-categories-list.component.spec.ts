import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotCategoriesListComponent } from './dot-categories-list.component';

describe('CategoriesListComponent', () => {
    let component: DotCategoriesListComponent;
    let fixture: ComponentFixture<DotCategoriesListComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotCategoriesListComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotCategoriesListComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
