import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotExperimentsConfigurationVariantsAddComponent } from './dot-experiments-configuration-variants-add.component';

describe('DotExperimentsConfigurationVariantsAddComponent', () => {
    let component: DotExperimentsConfigurationVariantsAddComponent;
    let fixture: ComponentFixture<DotExperimentsConfigurationVariantsAddComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotExperimentsConfigurationVariantsAddComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotExperimentsConfigurationVariantsAddComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
