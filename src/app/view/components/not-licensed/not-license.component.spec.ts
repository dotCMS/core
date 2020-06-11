import { ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';

import { DOTTestBed } from '@tests/dot-test-bed';
import { MockDotMessageService } from '@tests/dot-message-service.mock';

import { DotMessageService } from '@services/dot-messages-service';
import { NotLicensedComponent } from './not-licensed.component';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotLicenseService } from '@services/dot-license/dot-license.service';

const messageServiceMock = new MockDotMessageService({
    'portlet.title': 'Enterprise Portlet',
    'request.a.trial.license': 'Request License',
    'Contact-Us-for-more-Information': 'Contact Us',
    'Learn-more-about-dotCMS-Enterprise': 'Learn More',
    'only-available-in-enterprise': 'Only in Enterprise'
});

const portletData = {
    icon: 'update',
    titleKey: 'portlet.title',
    url: '/rules'
};

describe('NotLicensedComponent', () => {
    let fixture: ComponentFixture<NotLicensedComponent>;
    let de: DebugElement;
    let dotLicenseService: DotLicenseService;
    let component: NotLicensedComponent;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [NotLicensedComponent],
            imports: [CommonModule, ButtonModule, DotIconModule],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                DotLicenseService
            ]
        }).compileComponents();

        fixture = DOTTestBed.createComponent(NotLicensedComponent);
        de = fixture.debugElement;
        dotLicenseService = de.injector.get(DotLicenseService);
        component = de.componentInstance;
        spyOn(dotLicenseService, 'unlicenseData');
        dotLicenseService.unlicenseData.next(portletData);
        fixture.detectChanges();
    });

    it('should set labels and attributes on Html elements', () => {
        const links = de.queryAll(By.css('a'));
        expect(de.query(By.css('dot-icon')).componentInstance.name).toEqual(portletData.icon);
        expect(de.query(By.css('dot-icon')).componentInstance.size).toEqual(120);
        expect(de.query(By.css('h4')).nativeElement.innerText).toEqual(
            component.messagesKey['portlet.title']
        );
        expect(de.query(By.css('h4 ~ p')).nativeElement.innerText).toEqual(
            `${component.messagesKey['portlet.title']} ${component.messagesKey['only-available-in-enterprise']}`
        );
        expect(links[0].nativeElement.innerText).toEqual(
            component.messagesKey['Learn-more-about-dotCMS-Enterprise']
        );
        expect(links[0].nativeElement.href).toEqual(
            'https://dotcms.com/product/features/feature-list'
        );
        expect(links[1].nativeElement.innerText).toEqual(
            component.messagesKey['Contact-Us-for-more-Information']
        );
        expect(links[1].nativeElement.href).toEqual('https://dotcms.com/contact-us/');
        expect(links[2].nativeElement.innerText).toEqual(
            component.messagesKey['request.a.trial.license'].toUpperCase()
        );
        expect(links[2].nativeElement.href).toEqual(
            'https://dotcms.com/licensing/request-a-license-3/index'
        );
    });
});
