import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotExperimentsReportDailyDetailsComponent } from './dot-experiments-report-daily-details.component';

describe('DotExperimentsReportDailyDetailsComponent', () => {
    let component: DotExperimentsReportDailyDetailsComponent;
    let fixture: ComponentFixture<DotExperimentsReportDailyDetailsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotExperimentsReportDailyDetailsComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotExperimentsReportDailyDetailsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
