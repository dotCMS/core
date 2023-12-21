import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EmaFormSelectorComponent } from './ema-form-selector.component';

describe('EmaFormSelectorComponent', () => {
    let component: EmaFormSelectorComponent;
    let fixture: ComponentFixture<EmaFormSelectorComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [EmaFormSelectorComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(EmaFormSelectorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
