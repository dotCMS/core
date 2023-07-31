import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';
import { of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DialogModule } from 'primeng/dialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotPageToolUrlParams } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { getRunnableLink } from '@dotcms/utils';
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
            requestHostName: 'localhost',
            siteId: '123',
            languageId: 1
        };
        spectator = createComponent({
            props: {
                visible: true,
                currentPageUrlParams: pageToolUrlParamsTest
            }
        });
    });

    it('should have page tool list', () => {
        spectator.detectChanges();
        const menuListItems = spectator.queryAll(byTestId('page-tools-list-item'));

        expect(menuListItems.length).toEqual(3);
    });

    it('should have correct href values in links', () => {
        const tools = mockPageTools.pageTools.map((tool) => {
            return {
                ...tool,
                runnableLink: getRunnableLink(tool.runnableLink, pageToolUrlParamsTest)
            };
        });
        spectator.detectChanges();

        const anchorElements = spectator.queryAll(byTestId('page-tools-list-link'));

        expect(anchorElements.length).toEqual(3);

        anchorElements.forEach((anchorElement, index) => {
            const href = anchorElement.getAttribute('href');
            expect(href).toEqual(tools[index].runnableLink);
        });
    });
});
