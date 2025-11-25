import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';

import {
    DotLicenseService,
    DotMessageService,
    DotUnlicensedPortletData
} from '@dotcms/data-access';
import { DotIconModule, DotMessagePipe, DotNotLicenseComponent } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

const messageServiceMock = new MockDotMessageService({
    'portlet.title': 'Enterprise Portlet',
    'request.a.trial.license': 'Request License',
    'contact-us-for-more-information': 'Contact Us',
    'Learn-more-about-dotCMS-Enterprise': 'Learn More',
    'only-available-in': 'is only available in',
    'dotcms-enterprise-edition': 'otCMS Enterprise Editions',
    'for-more-information': 'For more information'
});

const portletData: DotUnlicensedPortletData = {
    icon: 'update',
    titleKey: 'portlet.title',
    url: '/rules'
};

describe('DotNotLicenseComponent', () => {
    let fixture: ComponentFixture<DotNotLicenseComponent>;
    let de: DebugElement;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [CommonModule, ButtonModule, DotIconModule, DotMessagePipe],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: DotLicenseService,
                    useValue: {
                        unlicenseData: of(portletData)
                    }
                }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(DotNotLicenseComponent);
        de = fixture.debugElement;
        fixture.detectChanges();
    });

    it('should set labels and attributes on Html elements', () => {
        expect(de.query(By.css('[data-testId="icon"]')).classes).toEqual({
            pi: true,
            'pi-update': true
        });
        expect(de.query(By.css('[data-testId="title"]')).nativeElement.innerText).toEqual(
            messageServiceMock.get('portlet.title')
        );
        expect(de.query(By.css('[data-testId="description"]')).nativeElement.innerText).toEqual(
            'Enterprise Portlet is only available in otCMS Enterprise Editions. For more information:'
        );
        expect(de.query(By.css('[data-testId="contact-us"]')).nativeElement.href).toEqual(
            'https://dotcms.com/contact-us/'
        );
        expect(de.query(By.css('[data-testId="request-a-trial"]')).nativeElement.href).toEqual(
            'https://dotcms.com/licensing/request-a-license-3/index'
        );
    });
});
