import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotExperimentsConfigurationGoalSelectComponent } from './dot-experiments-configuration-goal-select.component';

describe('DotExperimentsConfigurationGoalSelectComponent', () => {
    let component: DotExperimentsConfigurationGoalSelectComponent;
    let fixture: ComponentFixture<DotExperimentsConfigurationGoalSelectComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotExperimentsConfigurationGoalSelectComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotExperimentsConfigurationGoalSelectComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
