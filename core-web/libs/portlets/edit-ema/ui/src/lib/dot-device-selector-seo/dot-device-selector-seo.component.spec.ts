import { describe, expect, it } from '@jest/globals';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CUSTOM_ELEMENTS_SCHEMA, Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotCurrentUserService, DotDevicesService, DotMessageService } from '@dotcms/data-access';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { WINDOW } from '@dotcms/utils';
import {
    CurrentUserDataMock,
    DotCurrentUserServiceMock,
    DotDevicesServiceMock,
    MockDotMessageService,
    mockDotDevices
} from '@dotcms/utils-testing';

import { DotDeviceSelectorSeoComponent } from './dot-device-selector-seo.component';

@Component({
    standalone: false,
    selector: 'dot-test-host-component',
    template: `
        <button (click)="op.openMenu($event)" type="text">Open</button>
        <dot-device-selector-seo [apiLink]="apiLink" #op></dot-device-selector-seo>
    `
})
class TestHostComponent {
    apiLink = 'api/v1/page/render/an/url/test?language_id=1';
    linkToAddDevice = '/c/c_Devices';
}

describe('DotDeviceSelectorSeoComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let deHost: DebugElement;
    let component: DotDeviceSelectorSeoComponent;
    let de: DebugElement;
    let dotCurrentUserService: DotCurrentUserService;
    const messageServiceMock = new MockDotMessageService({
        'editpage.device.selector.title': 'Devices',
        'editpage.device.selector.media.tile': 'Social Media Tiles',
        'editpage.device.selector.search.engine': 'Search Engine Results Pages',
        'editpage.device.selector.new.tab': 'Open in New Tab',
        'editpage.device.selector.mobile.portrait': 'Mobile Portrait',
        'editpage.device.selector.mobile.landscape': 'Mobile Landscape',
        'editpage.device.selector.hd.monitor': 'HD Monitor',
        'editpage.device.selector.4k.monitor': '4K Monitor',
        'editpage.device.selector.tablet.portrait': 'Tablet Portrait',
        'editpage.device.selector.tablet.landscape': 'Tablet Landscape'
    });

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [TestHostComponent],
            imports: [
                DotDeviceSelectorSeoComponent,
                HttpClientTestingModule,
                OverlayPanelModule,
                NoopAnimationsModule,
                RouterTestingModule
            ],
            providers: [
                {
                    provide: WINDOW,
                    useValue: window
                },
                {
                    provide: DotDevicesService,
                    useClass: DotDevicesServiceMock
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: CoreWebService,
                    useClass: CoreWebServiceMock
                },
                { provide: DotCurrentUserService, useClass: DotCurrentUserServiceMock }
            ],
            schemas: [CUSTOM_ELEMENTS_SCHEMA]
        }).compileComponents();
    });

    beforeEach(() => {
        fixtureHost = TestBed.createComponent(TestHostComponent);
        deHost = fixtureHost.debugElement;
        de = deHost.query(By.css('dot-device-selector-seo'));
        component = de.componentInstance;
        TestBed.inject(DotDevicesService);
        jest.spyOn(component, 'getOptions').mockReturnValue(of(mockDotDevices));

        dotCurrentUserService = de.injector.get(DotCurrentUserService);

        fixtureHost.detectChanges();
        const buttonEl = fixtureHost.debugElement.query(By.css('button')).nativeElement;
        buttonEl.click();
    });

    it('should emit selected device on change', async () => {
        await fixtureHost.whenStable();
        fixtureHost.detectChanges();
        jest.spyOn(component.selected, 'emit');
        const selectorOptions = fixtureHost.debugElement.queryAll(
            By.css('[data-testId="device-selector-option"] > .device-list__button')
        );

        selectorOptions[0].nativeElement.click();

        fixtureHost.detectChanges();

        expect(component.selected.emit).toHaveBeenCalled();
    });

    it('should set user devices', async () => {
        await fixtureHost.whenStable();
        fixtureHost.detectChanges();

        const options = fixtureHost.debugElement.queryAll(
            By.css('[data-testId="device-selector-option"]')
        );
        expect(options.length).toBe(mockDotDevices.length);
    });

    it('should open the overlayPanel', () => {
        const buttonEl = fixtureHost.debugElement.query(By.css('button')).nativeElement;
        buttonEl.click();

        const devicesSelector: DebugElement = de.query(
            By.css('[data-testId="dot-devices-selector"]')
        );

        fixtureHost.detectChanges();
        expect(devicesSelector).toBeDefined();
    });

    it('should close the overlayPanel', () => {
        const devicesSelector: DebugElement = de.query(
            By.css('[data-testId="dot-devices-selector"]')
        );
        expect(devicesSelector).toBeNull();
    });

    it('should have link to open in a new tab when origin is specified', () => {
        fixtureHost.componentInstance.apiLink =
            'https://the-chosen-one-page.com.es/api/v1/page/render/an/url/test?language_id=1';
        fixtureHost.detectChanges();

        const addContent: DebugElement = de.query(
            By.css('[data-testId="dot-device-selector-link"]')
        );
        expect(addContent.nativeElement.href).toBe(
            'https://the-chosen-one-page.com.es/an/url/test?language_id=1&disabledNavigateMode=true&mode=LIVE'
        );
    });
    it('should have link to open in a new tab when origin is not specified', () => {
        fixtureHost.detectChanges();

        const addContent: DebugElement = de.query(
            By.css('[data-testId="dot-device-selector-link"]')
        );
        expect(addContent.nativeElement.href).toBe(
            'http://localhost/an/url/test?language_id=1&disabledNavigateMode=true&mode=LIVE'
        );
    });

    it('should have a link to add device', () => {
        fixtureHost.detectChanges();

        const link = de.query(By.css('[data-testId="dot-device-link-add"]'));
        expect(link.properties['href']).toContain('/c/content');
    });

    it('should not have a link to add device', async () => {
        jest.spyOn(dotCurrentUserService, 'getCurrentUser').mockReturnValue(
            of(CurrentUserDataMock)
        );

        const link = de.query(By.css('[data-testId="dot-device-link-add"]'));
        expect(link).toBeNull();
    });

    it('should trigger the changeSeoMedia', () => {
        jest.spyOn(component, 'changeSeoMediaEvent');
        fixtureHost.detectChanges();

        const buttonMedia = de.query(By.css('[data-testId="device-list-button-media"]'));

        buttonMedia.triggerEventHandler('click', 'Google');

        expect(component.changeSeoMediaEvent).toHaveBeenCalled();
    });

    it('should emit hideOverlayPanel event when onHideDeviceSelector is called', () => {
        jest.spyOn(component.hideOverlayPanel, 'emit');
        component.onHideDeviceSelector();
        expect(component.hideOverlayPanel.emit).toHaveBeenCalled();
    });

    it('should have the mask to being able to close when the user click outside', () => {
        fixtureHost.detectChanges();

        const selectorMask: DebugElement = de.query(By.css('[data-testId="selector-mask"]'));

        expect(selectorMask).toBeDefined();
    });

    it('should hide the media tiles and show the secondary link when hideSocialMedia is true', () => {
        component.hideSocialMedia = true;
        fixtureHost.detectChanges();

        const link = de.query(By.css('[data-testId="dot-device-selector-link-secondary"]'));
        const mediaTiles = de.query(By.css('[data-testId="social-media-tiles"]'));

        expect(link).not.toBeNull();
        expect(link.nativeElement.href).toContain(
            '/an/url/test?language_id=1&disabledNavigateMode=true&mode=LIVE'
        );
        expect(mediaTiles).toBeNull();
    });
});
