import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotContentAnalyticsComponent } from './dot-content-analytics.component';

describe('DotContentAnalyticsComponent', () => {
    let component: DotContentAnalyticsComponent;
    let fixture: ComponentFixture<DotContentAnalyticsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotContentAnalyticsComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotContentAnalyticsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
