import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotExperimentsConfigurationTrafficAllocationAddComponent } from './dot-experiments-configuration-traffic-allocation-add.component';

describe('DotExperimentsConfigurationTrafficAllocationAddComponent', () => {
    let component: DotExperimentsConfigurationTrafficAllocationAddComponent;
    let fixture: ComponentFixture<DotExperimentsConfigurationTrafficAllocationAddComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotExperimentsConfigurationTrafficAllocationAddComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotExperimentsConfigurationTrafficAllocationAddComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
