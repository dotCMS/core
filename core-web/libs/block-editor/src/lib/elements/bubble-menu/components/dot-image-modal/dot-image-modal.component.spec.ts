import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotImageModalComponent } from './dot-image-modal.component';

describe('DotImageModalComponent', () => {
    let component: DotImageModalComponent;
    let fixture: ComponentFixture<DotImageModalComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotImageModalComponent],
            teardown: { destroyAfterEach: false }
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotImageModalComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
