import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotAppsConfigurationDetailGeneratedStringFieldComponent } from './dot-apps-configuration-detail-generated-string-field.component';

describe('DotAppsConfigurationDetailGeneratedStringFieldComponent', () => {
    let component: DotAppsConfigurationDetailGeneratedStringFieldComponent;
    let fixture: ComponentFixture<DotAppsConfigurationDetailGeneratedStringFieldComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotAppsConfigurationDetailGeneratedStringFieldComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotAppsConfigurationDetailGeneratedStringFieldComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
