import { describe, expect, beforeEach } from '@jest/globals';
import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DialogModule } from 'primeng/dialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotPageToolUrlParams } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService, mockPageTools } from '@dotcms/utils-testing';

import { DotPageToolsSeoComponent } from './dot-page-tools-seo.component';
import { DotPageToolsSeoStore } from './store/dot-page-tools-seo.store';

describe('DotPageToolsSeoComponent', () => {
    let pageToolUrlParamsTest: DotPageToolUrlParams;
    let spectator: Spectator<DotPageToolsSeoComponent>;
    const createComponent = createComponentFactory({
        component: DotPageToolsSeoComponent,
        imports: [HttpClientTestingModule, DialogModule],
        providers: [
            DotPageToolsSeoStore,
            {
                provide: HttpClient,
                useValue: {
                    get: () => of(mockPageTools),
                    request: () => of(mockPageTools)
                }
            },
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'editpage.toolbar.nav.page.tools': 'Page Tools'
                })
            },
            DotMessagePipe
        ]
    });

    beforeEach(() => {
        pageToolUrlParamsTest = {
            currentUrl: '/blogTest',
            requestHostName: 'http://localhost',
            siteId: '123',
            languageId: 1
        };
        spectator = createComponent({
            props: {
                currentPageUrlParams: pageToolUrlParamsTest
            }
        });
    });

    it('should have page tool list', async () => {
        spectator.component.toggleDialog();
        spectator.detectChanges();
        const menuListItems = spectator.queryAll('[data-testId="page-tools-list-item"]');

        expect(menuListItems.length).toEqual(3);
    });

    it('should have correct href values in links', () => {
        spectator.component.toggleDialog();
        spectator.detectChanges();
        const tools = mockPageTools.pageTools;

        const anchorElements = spectator.queryAll('[data-testId="page-tools-list-link"]');

        expect(anchorElements.length).toEqual(3);

        anchorElements.forEach((anchorElement, index) => {
            const href = anchorElement.getAttribute('href');
            expect(href).toEqual(tools[index].runnableLink);
        });
    });
});
