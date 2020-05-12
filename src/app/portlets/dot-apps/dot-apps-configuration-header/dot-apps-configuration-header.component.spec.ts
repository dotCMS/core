import { async, ComponentFixture } from '@angular/core/testing';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-messages-service';
import { DOTTestBed } from '@tests/dot-test-bed';
import { CommonModule } from '@angular/common';
import { By } from '@angular/platform-browser';
import { DotAppsConfigurationHeaderComponent } from './dot-apps-configuration-header.component';
import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { NgxMdModule } from 'ngx-md';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DebugElement } from '@angular/core';

const messages = {
    'apps.configurations': 'Configurations',
    'apps.no.configurations': 'No Configurations',
    'apps.key': 'Key',
    'apps.confirmation.description.show.less': 'Show Less',
    'apps.confirmation.description.show.more': 'Show More'
};

const appData = {
    allowExtraParams: true,
    configurationsCount: 2,
    key: 'google-calendar',
    name: 'Google Calendar',
    description: `It is a tool to keep track of your life\'s events, also this descriptions is long and long
    and long and long and long and long and long and long and long and long and long and long and long and
    long and long and long and long and long and long and long and long and long and long and long and long
    and long and long and long and long and long and long`,
    iconUrl: '/dA/d948d85c-3bc8-4d85-b0aa-0e989b9ae235/photo/surfer-profile.jpg',
    sites: []
};

describe('DotAppsConfigurationHeaderComponent', () => {
    let component: DotAppsConfigurationHeaderComponent;
    let fixture: ComponentFixture<DotAppsConfigurationHeaderComponent>;
    let routerService: DotRouterService;
    let de: DebugElement;

    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            imports: [CommonModule, DotAvatarModule, DotCopyButtonModule, NgxMdModule],
            declarations: [DotAppsConfigurationHeaderComponent],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                {
                    provide: DotRouterService,
                    useClass: MockDotRouterService
                }
            ]
        });
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotAppsConfigurationHeaderComponent);
        de = fixture.debugElement;
        component = fixture.debugElement.componentInstance;
        routerService = fixture.debugElement.injector.get(DotRouterService);
        component.app = appData;
        fixture.detectChanges();
    });

    it('should set messages/values in DOM correctly', () => {
        fixture.whenStable().then(() => {
            expect(
                de.query(By.css('.dot-apps-configuration__service-name')).nativeElement.outerText
            ).toBe(component.app.name);
            expect(
                de.query(By.css('.dot-apps-configuration__service-key')).nativeElement.outerText
            ).toContain(messages['apps.key']);
            expect(
                de.query(By.css('.dot-apps-configuration__configurations')).nativeElement.outerText
            ).toContain(`${appData.configurationsCount} ${messages['apps.configurations']}`);
            const description = component.app.description
                .replace(/\n/gi, '')
                .replace(/\r/gi, '')
                .replace(/   /gi, '');
            expect(
                de.query(By.css('.dot-apps-configuration__description')).nativeElement.outerText
            ).toContain(description);
            expect(
                de.query(By.css('.dot-apps-configuration__description__link_show-more'))
                    .nativeElement.outerText
            ).toBe(component.messagesKey['apps.confirmation.description.show.more']);
        });
    });

    it('should DotCopy & DotAvatar with right properties', () => {
        const dotAvatar = de.query(By.css('dot-avatar')).componentInstance;
        const dotCopy = de.query(By.css('dot-copy-button')).componentInstance;
        expect(dotAvatar.label).toBe(component.app.name);
        expect(dotAvatar.size).toBe(112);
        expect(dotAvatar.url).toBe(component.app.iconUrl);
        expect(dotCopy.label).toBe(component.app.key);
        expect(dotCopy.copy).toBe(component.app.key);
    });

    it('should redirect to detail configuration list page when app Card clicked', () => {
        const dotAvatar = de.query(By.css('dot-avatar'));
        dotAvatar.triggerEventHandler('click', { key: appData.key });
        expect(routerService.goToAppsConfiguration).toHaveBeenCalledWith(component.app.key);
        const title = de.query(By.css('.dot-apps-configuration__service-name'));
        title.triggerEventHandler('click', { key: appData.key });
        expect(routerService.goToAppsConfiguration).toHaveBeenCalledWith(component.app.key);
    });

    it('should show right message and no "Show More" link when no configurations and description short', () => {
        component.app.description = 'test';
        component.app.configurationsCount = 0;
        fixture.detectChanges();
        fixture.whenStable().then(() => {
            expect(
                de.query(By.css('.dot-apps-configuration__configurations')).nativeElement.outerText
            ).toContain(messages['apps.no.configurations']);
            expect(
                de.query(By.css('.dot-apps-configuration__description__link_show-more'))
            ).toBeFalsy();
        });
    });
});
