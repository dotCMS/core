import { CommonModule } from '@angular/common';
import { DebugElement } from '@angular/core';
import { ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ButtonModule } from 'primeng/button';

import { DOTTestBed } from '@dotcms/app/test/dot-test-bed';
import {
    DotLicenseService,
    DotMessageService,
    DotUnlicensedPortletData
} from '@dotcms/data-access';
import { DotIconModule, DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { NotLicensedComponent } from './not-licensed.component';

const messageServiceMock = new MockDotMessageService({
    'portlet.title': 'Enterprise Portlet',
    'request.a.trial.license': 'Request License',
    'Contact-Us-for-more-Information': 'Contact Us',
    'Learn-more-about-dotCMS-Enterprise': 'Learn More',
    'only-available-in-enterprise': 'Only in Enterprise'
});

const portletData: DotUnlicensedPortletData = {
    icon: 'update',
    titleKey: 'portlet.title',
    url: '/rules'
};

describe('NotLicensedComponent', () => {
    let fixture: ComponentFixture<NotLicensedComponent>;
    let de: DebugElement;
    let dotLicenseService: DotLicenseService;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [NotLicensedComponent],
            imports: [CommonModule, ButtonModule, DotIconModule, DotMessagePipe],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                DotLicenseService
            ]
        });

        fixture = DOTTestBed.createComponent(NotLicensedComponent);
        de = fixture.debugElement;
        dotLicenseService = de.injector.get(DotLicenseService);
        dotLicenseService.unlicenseData.next(portletData);
        fixture.detectChanges();
    });

    it('should set labels and attributes on Html elements', () => {
        const links = de.queryAll(By.css('a'));
        expect(de.query(By.css('dot-icon')).componentInstance.name).toEqual(portletData.icon);
        expect(de.query(By.css('dot-icon')).componentInstance.size).toEqual(120);
        expect(de.query(By.css('h4')).nativeElement.innerText).toEqual(
            messageServiceMock.get('portlet.title')
        );
        expect(de.query(By.css('h4 ~ p')).nativeElement.innerText).toEqual(
            `${messageServiceMock.get('portlet.title')} ${messageServiceMock.get(
                'only-available-in-enterprise'
            )}`
        );
        expect(links[0].nativeElement.innerText).toEqual(
            messageServiceMock.get('Learn-more-about-dotCMS-Enterprise')
        );
        expect(links[0].nativeElement.href).toEqual(
            'https://dotcms.com/product/features/feature-list'
        );
        expect(links[1].nativeElement.innerText).toEqual(
            messageServiceMock.get('Contact-Us-for-more-Information')
        );
        expect(links[1].nativeElement.href).toEqual('https://dotcms.com/contact-us/');
        expect(links[2].nativeElement.innerText).toEqual(
            messageServiceMock.get('request.a.trial.license')
        );
        expect(links[2].nativeElement.href).toEqual(
            'https://dotcms.com/licensing/request-a-license-3/index'
        );
    });
});
