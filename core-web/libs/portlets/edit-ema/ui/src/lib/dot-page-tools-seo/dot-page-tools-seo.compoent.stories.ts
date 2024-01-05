import { moduleMetadata } from '@storybook/angular';
import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DialogModule } from 'primeng/dialog';

import { DotMessageService, DotPageToolsService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService, mockPageTools } from '@dotcms/utils-testing';

import { DotPageToolsSeoComponent } from './dot-page-tools-seo.component';

const messageServiceMock = new MockDotMessageService({
    'editpage.toolbar.nav.page.tools': 'Page Tools'
});

export default {
    title: 'dotcms/Page Tools',
    component: DotPageToolsSeoComponent,
    decorators: [
        moduleMetadata({
            imports: [CommonModule, DotMessagePipe, BrowserAnimationsModule, DialogModule],
            providers: [
                DotPageToolsService,
                { provide: DotMessageService, useValue: messageServiceMock },
                {
                    provide: HttpClient,
                    useValue: {
                        get: () => of(mockPageTools),
                        request: () => of(mockPageTools)
                    }
                }
            ]
        })
    ]
};

export const Default = () => ({
    component: DotPageToolsSeoComponent,
    props: {
        visible: true,
        currentPageUrlParams: {
            currentUrl: '/index',
            requestHostName: 'demo.dotcms.com',
            siteId: '123',
            languageId: 1
        }
    }
});
