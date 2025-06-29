import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotBubbleMenuComponent } from './dot-bubble-menu.component';

describe('DotBubbleMenuComponent', () => {
    let component: DotBubbleMenuComponent;
    let fixture: ComponentFixture<DotBubbleMenuComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotBubbleMenuComponent],
            teardown: { destroyAfterEach: false }
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotBubbleMenuComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
