import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BubbleMenuButtonComponent } from './bubble-menu-button.component';

describe('BubbleMenuButtonComponent', () => {
    let component: BubbleMenuButtonComponent;
    let fixture: ComponentFixture<BubbleMenuButtonComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [BubbleMenuButtonComponent],
            teardown: { destroyAfterEach: false }
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(BubbleMenuButtonComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
