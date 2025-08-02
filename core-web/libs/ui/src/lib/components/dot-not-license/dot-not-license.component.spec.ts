import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ButtonModule } from 'primeng/button';

import {
    DotLicenseService,
    DotMessageService,
    DotUnlicensedPortletData
} from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotNotLicenseComponent } from './dot-not-license.component';

import { DotIconModule } from '../../dot-icon/dot-icon.module';
import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

const messageServiceMock = new MockDotMessageService({
    'portlet.title': 'Enterprise Portlet',
    'request.a.trial.license': 'Request License',
    'contact-us-for-more-information': 'Contact Us',
    'Learn-more-about-dotCMS-Enterprise': 'Learn More',
    'only-available-in': 'is only available in',
    'dotcms-enterprise-edition': 'dotCMS Enterprise Editions',
    'for-more-information': 'For more information'
});

const portletData: DotUnlicensedPortletData = {
    icon: 'update',
    titleKey: 'portlet.title',
    url: '/rules'
};

describe('DotNotLicenseComponent', () => {
    let spectator: Spectator<DotNotLicenseComponent>;
    const createComponent = createComponentFactory({
        component: DotNotLicenseComponent,
        imports: [ButtonModule, DotIconModule, DotMessagePipe],
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
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should set labels and attributes on Html elements', () => {
        expect(spectator.query(byTestId('icon')).classList).toContain('pi');
        expect(spectator.query(byTestId('icon')).classList).toContain('pi-update');
        expect(spectator.query(byTestId('title')).textContent).toBe(
            messageServiceMock.get('portlet.title')
        );
        expect(
            spectator.query(byTestId('description')).textContent.replace(/\s+/g, ' ').trim()
        ).toBe(
            'Enterprise Portlet is only available in dotCMS Enterprise Editions. For more information:'
        );
        expect(spectator.query(byTestId('contact-us')).getAttribute('href')).toBe(
            'https://dotcms.com/contact-us/'
        );
        expect(spectator.query(byTestId('request-a-trial')).getAttribute('href')).toBe(
            'https://dotcms.com/licensing/request-a-license-3/index'
        );
    });
});
