import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EmaContentletToolsComponent } from './ema-contentlet-tools.component';

describe('EmaContentletToolsComponent', () => {
    let component: EmaContentletToolsComponent;
    let fixture: ComponentFixture<EmaContentletToolsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [EmaContentletToolsComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(EmaContentletToolsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
