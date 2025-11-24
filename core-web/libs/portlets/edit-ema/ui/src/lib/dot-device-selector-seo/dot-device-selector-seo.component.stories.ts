import { action } from '@storybook/addon-actions';
import { moduleMetadata, StoryObj, Meta, argsToTemplate } from '@storybook/angular';
import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { PanelModule } from 'primeng/panel';
import { PopoverModule } from 'primeng/popover';
import { SelectModule } from 'primeng/select';

import { DotCurrentUserService, DotDevicesService, DotMessageService } from '@dotcms/data-access';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { DotIconModule, DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService, mockDotDevices } from '@dotcms/utils-testing';

import { DotDeviceSelectorSeoComponent } from './dot-device-selector-seo.component';

const messageServiceMock = new MockDotMessageService({
    'editpage.device.selector.title': 'Devices',
    'editpage.device.selector.media.tile': 'Social Media Tiles',
    'editpage.device.selector.search.engine': 'Search Engine Results Pages',
    'editpage.device.selector.new.tab': 'Open Published Version',
    'editpage.device.selector.mobile.portrait': 'Mobile Portrait',
    'editpage.device.selector.mobile.landscape': 'Mobile Landscape',
    'editpage.device.selector.hd.monitor': 'HD Monitor',
    'editpage.device.selector.4k.monitor': '4K Monitor',
    'editpage.device.selector.tablet.portrait': 'Tablet Portrait',
    'editpage.device.selector.tablet.landscape': 'Tablet Landscape'
});

const mockActivatedRoute = {
    snapshot: {}
};

const meta: Meta<DotDeviceSelectorSeoComponent> = {
    title: 'dotcms/Device Selector SEO',
    component: DotDeviceSelectorSeoComponent,
    decorators: [
        moduleMetadata({
            imports: [
                CommonModule,
                SelectModule,
                FormsModule,
                DotIconModule,
                ButtonModule,
                PopoverModule,
                PanelModule,
                DividerModule,
                DotMessagePipe,
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
                },
                { provide: ActivatedRoute, useValue: mockActivatedRoute },
                {
                    provide: DotCurrentUserService,
                    useValue: {
                        getCurrentUser: () =>
                            of({
                                admin: false,
                                email: 'admin@adminc.com',
                                givenName: 'admin',
                                roleId: '1',
                                surname: 'admin',
                                userId: '1'
                            })
                    }
                }
            ]
        })
    ],
    render: (args) => {
        return {
            props: {
                ...args,
                selected: action('selected')
            },
            template: `
                <p-button label="Open Selector" class="p-button-outlined" (click)="op.openMenu($event)"></p-button>
                <dot-device-selector-seo #op ${argsToTemplate(args)} />
            `
        };
    }
};
export default meta;

type Story = StoryObj<DotDeviceSelectorSeoComponent>;

export const Default: Story = {};
