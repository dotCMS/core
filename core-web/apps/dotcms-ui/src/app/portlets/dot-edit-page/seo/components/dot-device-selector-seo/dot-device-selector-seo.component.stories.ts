import { action } from '@storybook/addon-actions';
import { moduleMetadata } from '@storybook/angular';
import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { DropdownModule } from 'primeng/dropdown';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { PanelModule } from 'primeng/panel';

import { DotDevicesService, DotMessageService } from '@dotcms/data-access';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { DotIconModule, DotMessagePipeModule } from '@dotcms/ui';
import { MockDotMessageService, mockDotDevices } from '@dotcms/utils-testing';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotDeviceSelectorSeoComponent } from './dot-device-selector-seo.component';

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

export default {
    title: 'dotcms/Device Selector SEO',
    component: DotDeviceSelectorSeoComponent,
    decorators: [
        moduleMetadata({
            imports: [
                CommonModule,
                DropdownModule,
                FormsModule,
                DotIconModule,
                DotPipesModule,
                ButtonModule,
                OverlayPanelModule,
                PanelModule,
                DividerModule,
                DotMessagePipeModule,
                BrowserAnimationsModule
            ],
            providers: [
                DotDevicesService,
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                {
                    provide: HttpClient,
                    useValue: {
                        get: () => of(mockDotDevices),
                        request: () => of(mockDotDevices)
                    }
                }
            ]
        })
    ]
};

export const Default = () => ({
    component: DotDeviceSelectorSeoComponent,
    props: {
        selected: action('selected')
    }
});
