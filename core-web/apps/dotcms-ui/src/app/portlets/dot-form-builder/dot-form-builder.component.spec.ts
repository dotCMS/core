import { of } from 'rxjs';

import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { DotLicenseService, DotMessageService } from '@dotcms/data-access';
import { DotNotLicenseComponent } from '@dotcms/ui';

import { DotFormBuilderComponent } from './dot-form-builder.component';

describe('DotFormBuilderComponent', () => {
    let fixture: ComponentFixture<DotFormBuilderComponent>;
    let de: DebugElement;
    let router: ActivatedRoute;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [DotNotLicenseComponent],
            declarations: [DotFormBuilderComponent],
            providers: [
                {
                    provide: ActivatedRoute,
                    useValue: {
                        get data() {
                            return '';
                        }
                    }
                },
                {
                    provide: DotLicenseService,
                    useValue: {
                        unlicenseData: of({}),
                        isEnterprise: () => of(true),
                        canAccessEnterprisePortlet: () => of(true)
                    }
                },
                {
                    provide: DotMessageService,
                    useValue: {
                        get: () => ''
                    }
                }
            ]
        });
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotFormBuilderComponent);
        de = fixture.debugElement;
        router = TestBed.inject(ActivatedRoute);
    });

    it('should show unlicense portlet', () => {
        Object.defineProperty(router, 'data', {
            value: of({
                haveLicense: false
            }),
            writable: true
        });
        fixture.detectChanges();
        const unlicensed = de.query(By.css('[data-testId="not-license"]'));
        const contentTypes = de.query(By.css('[data-testId="content-types"]'));
        expect(unlicensed).toBeDefined();
        expect(contentTypes).toBeNull();
    });

    it('should show dot-content-types', () => {
        Object.defineProperty(router, 'data', {
            value: of({
                haveLicense: true
            }),
            writable: true
        });
        fixture.detectChanges();
        const unlicensed = de.query(By.css('[data-testId="not-license"]'));
        const contentTypes = de.query(By.css('[data-testId="content-types"]'));
        expect(unlicensed).toBeNull();
        expect(contentTypes).toBeDefined();
    });
});
