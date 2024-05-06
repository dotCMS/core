import { of } from 'rxjs';

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { DotLicenseService, DotMessageService } from '@dotcms/data-access';

import { NotLicenseComponent } from './not-license.component';

describe('NotLicenseComponent', () => {
    let fixture: ComponentFixture<NotLicenseComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [NotLicenseComponent],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: {
                        get: () => {
                            //
                        }
                    }
                },
                {
                    provide: DotLicenseService,
                    useValue: {
                        unlicenseData: of({})
                    }
                }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(NotLicenseComponent);
        fixture.detectChanges();
    });

    it('should have DotNotLicense component from UI', () => {
        expect(fixture.debugElement.query(By.css('dot-not-license'))).toBeDefined();
    });
});
