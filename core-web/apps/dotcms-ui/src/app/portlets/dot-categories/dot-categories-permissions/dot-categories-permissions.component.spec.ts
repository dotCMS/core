import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotCategoriesPermissionsComponent } from './dot-categories-permissions.component';

describe('CategoriesPermissionsComponent', () => {
    let component: DotCategoriesPermissionsComponent;
    let fixture: ComponentFixture<DotCategoriesPermissionsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotCategoriesPermissionsComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotCategoriesPermissionsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
