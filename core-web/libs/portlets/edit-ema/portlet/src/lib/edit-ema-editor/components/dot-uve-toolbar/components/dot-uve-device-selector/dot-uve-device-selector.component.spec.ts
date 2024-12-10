import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotUveDeviceSelectorComponent } from './dot-uve-device-selector.component';

describe('DotUveDeviceSelectorComponent', () => {
    let component: DotUveDeviceSelectorComponent;
    let fixture: ComponentFixture<DotUveDeviceSelectorComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotUveDeviceSelectorComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotUveDeviceSelectorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
