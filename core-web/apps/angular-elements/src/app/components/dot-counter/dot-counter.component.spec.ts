import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotCounterComponent } from './dot-counter.component';

describe('DotCounterComponent', () => {
    let component: DotCounterComponent;
    let fixture: ComponentFixture<DotCounterComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotCounterComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotCounterComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
