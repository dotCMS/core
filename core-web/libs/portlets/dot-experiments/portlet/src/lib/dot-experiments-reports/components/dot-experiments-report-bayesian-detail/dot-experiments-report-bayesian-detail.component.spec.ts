import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotExperimentsReportBayesianDetailComponent } from './dot-experiments-report-bayesian-detail.component';

describe('DotExperimentsReportBayesianDetailComponent', () => {
    let component: DotExperimentsReportBayesianDetailComponent;
    let fixture: ComponentFixture<DotExperimentsReportBayesianDetailComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotExperimentsReportBayesianDetailComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotExperimentsReportBayesianDetailComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
