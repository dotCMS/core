import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ContainerPermissionsComponent } from './container-permissions.component';

describe('ContainerPermissionsComponent', () => {
    let component: ContainerPermissionsComponent;
    let fixture: ComponentFixture<ContainerPermissionsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [ContainerPermissionsComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(ContainerPermissionsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
