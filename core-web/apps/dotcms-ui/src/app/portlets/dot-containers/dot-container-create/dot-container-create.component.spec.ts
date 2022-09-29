import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotContainerCreateComponent } from './dot-container-create.component';

describe('ContainerCreateComponent', () => {
    let component: DotContainerCreateComponent;
    let fixture: ComponentFixture<DotContainerCreateComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotContainerCreateComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotContainerCreateComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
