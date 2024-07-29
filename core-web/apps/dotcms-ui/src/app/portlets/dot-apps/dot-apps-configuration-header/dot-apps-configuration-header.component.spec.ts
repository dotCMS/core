import { MarkdownService } from 'ngx-markdown';

import { CommonModule } from '@angular/common';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { AvatarModule } from 'primeng/avatar';

import { DotCopyLinkModule } from '@components/dot-copy-link/dot-copy-link.module';
import { DotMessageService, DotRouterService } from '@dotcms/data-access';
import { DotApp } from '@dotcms/dotcms-models';
import { DotAvatarDirective, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';
import { MockDotMessageService, MockDotRouterService } from '@dotcms/utils-testing';

import { DotAppsConfigurationHeaderComponent } from './dot-apps-configuration-header.component';

@Component({
    template: `
        <dot-apps-configuration-header [app]="app"></dot-apps-configuration-header>
    `
})
class TestHostComponent {
    app: DotApp;
}

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
    description: `It is a tool to keep track of your life's events, also this descriptions is long and long and long and long and long and long and long and long and long and long and long and long and long and long and long and long and long and long and long and long and long and long and long and long and long and long and long and long and long and long and long`,
    iconUrl: '/dA/d948d85c-3bc8-4d85-b0aa-0e989b9ae235/photo/surfer-profile.jpg',
    sites: []
};

describe('DotAppsConfigurationHeaderComponent', () => {
    let component: TestHostComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let routerService: DotRouterService;
    let de: DebugElement;

    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [
                CommonModule,
                DotCopyLinkModule,
                DotSafeHtmlPipe,
                DotMessagePipe,
                DotAvatarDirective,
                AvatarModule
            ],
            declarations: [DotAppsConfigurationHeaderComponent, TestHostComponent],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                {
                    provide: DotRouterService,
                    useClass: MockDotRouterService
                },
                MarkdownService
            ]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(TestHostComponent);
        de = fixture.debugElement;
        component = fixture.componentInstance;
        routerService = fixture.debugElement.injector.get(DotRouterService);
        component.app = appData;
        fixture.detectChanges();
    });

    xit('should set messages/values in DOM correctly', async () => {
        await fixture.whenStable();
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
            .replace(/ {3}/gi, '');
        expect(
            de.query(By.css('.dot-apps-configuration__description')).nativeElement.outerText
        ).toBe(description);
        expect(
            de.query(By.css('.dot-apps-configuration__description__link_show-more')).nativeElement
                .outerText
        ).toBe(messageServiceMock.get('apps.confirmation.description.show.more'));
    });

    it('should DotCopy & p-avatar with right properties', () => {
        const dotCopy = de.query(By.css('dot-copy-link')).componentInstance;
        const avatar = fixture.debugElement.query(By.css('p-avatar'));

        const { image, size } = avatar.componentInstance;

        expect(image).toBe(component.app.iconUrl);
        expect(size).toBe('xlarge');
        expect(avatar.attributes['ng-reflect-text']).toBe(component.app.name);

        expect(dotCopy.label).toBe(component.app.key);
        expect(dotCopy.copy).toBe(component.app.key);
    });

    it('should redirect to detail configuration list page when app Card clicked', () => {
        const avatar = de.query(By.css('p-avatar'));
        avatar.triggerEventHandler('click', { key: appData.key });
        expect(routerService.goToAppsConfiguration).toHaveBeenCalledWith(component.app.key);
        const title = de.query(By.css('.dot-apps-configuration__service-name'));
        title.triggerEventHandler('click', { key: appData.key });
        expect(routerService.goToAppsConfiguration).toHaveBeenCalledWith(component.app.key);
    });

    it('should show right message and no "Show More" link when no configurations and description short', async () => {
        component.app.description = 'test';
        component.app.configurationsCount = 0;
        fixture.detectChanges();
        await fixture.whenStable();
        expect(
            de.query(By.css('.dot-apps-configuration__configurations')).nativeElement.outerText
        ).toContain(messages['apps.no.configurations']);
        expect(
            de.query(By.css('.dot-apps-configuration__description__link_show-more'))
        ).toBeFalsy();
    });
});
