import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotAnalyticsSearchComponent } from './dot-analytics-search.component';

describe('DotAnalyticsSearchComponent', () => {
    let component: DotAnalyticsSearchComponent;
    let fixture: ComponentFixture<DotAnalyticsSearchComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotAnalyticsSearchComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotAnalyticsSearchComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
