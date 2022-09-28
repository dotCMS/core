import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotContainerListComponent } from './dot-container-list.component';

describe('ContainerListComponent', () => {
    let component: DotContainerListComponent;
    let fixture: ComponentFixture<DotContainerListComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotContainerListComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotContainerListComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
