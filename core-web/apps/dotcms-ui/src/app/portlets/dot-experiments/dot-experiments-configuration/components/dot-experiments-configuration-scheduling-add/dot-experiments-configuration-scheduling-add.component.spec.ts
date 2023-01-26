import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotExperimentsConfigurationSchedulingAddComponent } from './dot-experiments-configuration-scheduling-add.component';

describe('DotExperimentsConfigurationSchedulingAddComponent', () => {
    let component: DotExperimentsConfigurationSchedulingAddComponent;
    let fixture: ComponentFixture<DotExperimentsConfigurationSchedulingAddComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotExperimentsConfigurationSchedulingAddComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotExperimentsConfigurationSchedulingAddComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
