import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BubbleLinkFormComponent } from './bubble-link-form.component';

describe('BubbleLinkFormComponent', () => {
    let component: BubbleLinkFormComponent;
    let fixture: ComponentFixture<BubbleLinkFormComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [BubbleLinkFormComponent],
            teardown: { destroyAfterEach: false }
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(BubbleLinkFormComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
