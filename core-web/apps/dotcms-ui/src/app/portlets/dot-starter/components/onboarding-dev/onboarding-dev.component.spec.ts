
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotOnboardingDevComponent } from './onboarding-dev.component';

describe('DotOnboardingDevComponent', () => {
    let component: DotOnboardingDevComponent;
    let fixture: ComponentFixture<DotOnboardingDevComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotOnboardingDevComponent],
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                { provide: DotMessageService, useValue: new MockDotMessageService({}) }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotOnboardingDevComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
