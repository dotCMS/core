import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BubbleFormComponent } from './bubble-form.component';

describe('BubbleFormComponent', () => {
    let component: BubbleFormComponent;
    let fixture: ComponentFixture<BubbleFormComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [BubbleFormComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(BubbleFormComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
