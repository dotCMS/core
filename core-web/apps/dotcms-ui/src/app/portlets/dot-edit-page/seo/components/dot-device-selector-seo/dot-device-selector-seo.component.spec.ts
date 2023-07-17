import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CUSTOM_ELEMENTS_SCHEMA, Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotDevicesService, DotMessageService } from '@dotcms/data-access';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import {
    DotDevicesServiceMock,
    MockDotMessageService,
    mockDotDevices
} from '@dotcms/utils-testing';

import { DotDeviceSelectorSeoComponent } from './dot-device-selector-seo.component';

@Component({
    selector: 'dot-test-host-component',
    template: ` <dot-device-selector-seo></dot-device-selector-seo> `
})
class TestHostComponent {}

describe('DotDeviceSelectorSeoComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let deHost: DebugElement;
    let component: DotDeviceSelectorSeoComponent;
    let de: DebugElement;

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
                BrowserAnimationsModule
            ],
            providers: [
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
                }
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
        spyOn(component, 'getOptions').and.returnValue(of(mockDotDevices));
    });

    it('should emit selected device on change', async () => {
        const selectorButton: DebugElement = de.query(
            By.css('[data-testId="device-selector-button"]')
        );

        selectorButton.nativeElement.click();

        await fixtureHost.whenStable();
        fixtureHost.detectChanges();
        spyOn(component.selected, 'emit');
        const selectorOptions = fixtureHost.debugElement.queryAll(
            By.css('[data-testId="device-selector-option"] > .device-list__button')
        );

        selectorOptions[0].nativeElement.click();

        fixtureHost.detectChanges();

        expect(component.selected.emit).toHaveBeenCalled();
    });

    it('should set user devices', async () => {
        const selectorButton: DebugElement = de.query(
            By.css('[data-testId="device-selector-button"]')
        );

        selectorButton.nativeElement.click();
        await fixtureHost.whenStable();
        fixtureHost.detectChanges();

        const options = fixtureHost.debugElement.queryAll(
            By.css('[data-testId="device-selector-option"]')
        );
        expect(options.length).toBe(mockDotDevices.length);
    });

    it('should open the overlayPanel', () => {
        const selectorButton: DebugElement = de.query(
            By.css('[data-testId="device-selector-button"]')
        );
        const devicesSelector: DebugElement = de.query(
            By.css('[data-testId="dot-devices-selector"]')
        );

        selectorButton.nativeElement.click();

        fixtureHost.detectChanges();
        expect(devicesSelector).toBeDefined();
    });

    it('should close the overlayPanel', () => {
        const selectorButton: DebugElement = de.query(
            By.css('[data-testId="device-selector-button"]')
        );
        const devicesSelector: DebugElement = de.query(
            By.css('[data-testId="dot-devices-selector"]')
        );

        selectorButton.nativeElement.click();

        fixtureHost.detectChanges();
        expect(devicesSelector).toBeNull();
    });
});
