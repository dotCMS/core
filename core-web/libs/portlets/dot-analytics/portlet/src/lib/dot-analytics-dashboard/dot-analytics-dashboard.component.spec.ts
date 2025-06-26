import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotAnalyticsDashboardComponent } from './dot-analytics-dashboard.component';

describe('DotAnalyticsDashboardComponent', () => {
    let component: DotAnalyticsDashboardComponent;
    let fixture: ComponentFixture<DotAnalyticsDashboardComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotAnalyticsDashboardComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotAnalyticsDashboardComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
