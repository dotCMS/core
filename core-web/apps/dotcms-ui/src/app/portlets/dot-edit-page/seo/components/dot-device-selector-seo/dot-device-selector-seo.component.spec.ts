import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CUSTOM_ELEMENTS_SCHEMA, Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

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
    template: `<button (click)="op.openMenu($event)" type="text">Open</button>
        <dot-device-selector-seo #op [apiLink]="apiLink"></dot-device-selector-seo> `
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
                BrowserAnimationsModule,
                RouterTestingModule
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

        fixtureHost.detectChanges();
        const buttonEl = fixtureHost.debugElement.query(By.css('button')).nativeElement;
        buttonEl.click();
    });

    it('should emit selected device on change', async () => {
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

    it('should have link to open in a new tab', () => {
        fixtureHost.detectChanges();

        const addContent: DebugElement = de.query(
            By.css('[data-testId="dot-device-selector-link"]')
        );
        expect(addContent.nativeElement.href).toContain(
            '/an/url/test?language_id=1&disabledNavigateMode=true'
        );
    });

    it('should have a link to add device', () => {
        fixtureHost.detectChanges();

        const link = de.query(By.css('[data-testId="dot-device-link-add"]'));
        expect(link.properties.href).toContain('/c/content');
    });
});
