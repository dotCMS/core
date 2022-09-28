import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CategoriesPermissionsComponent } from './categories-permissions.component';

describe('CategoriesPermissionsComponent', () => {
    let component: CategoriesPermissionsComponent;
    let fixture: ComponentFixture<CategoriesPermissionsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [CategoriesPermissionsComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(CategoriesPermissionsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
