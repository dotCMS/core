import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotBinarySettingsComponent } from './dot-binary-settings.component';

describe('DotBinarySettingsComponent', () => {
    let component: DotBinarySettingsComponent;
    let fixture: ComponentFixture<DotBinarySettingsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotBinarySettingsComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotBinarySettingsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
