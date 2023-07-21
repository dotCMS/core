import { SpectatorHost, createHostFactory } from '@ngneat/spectator';
import { of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DialogModule } from 'primeng/dialog';

import { DotMessageService, DotPageToolsService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService, mockPageTools } from '@dotcms/utils-testing';

import { DotPageToolsSeoComponent } from './dot-page-tools-seo.component';

describe('DotPageToolsSeoComponent', () => {
    let spectator: SpectatorHost<DotPageToolsSeoComponent>;
    const createHost = createHostFactory({
        component: DotPageToolsSeoComponent,
        imports: [HttpClientTestingModule, DialogModule],
        providers: [
            DotPageToolsService,
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
        spectator = createHost(
            `<dot-page-tools-seo
            [visible]="visible"
            [currentPageUrl]="currentPageUrl"
        ></dot-page-tools-seo>`,
            {
                hostProps: {
                    visible: true,
                    currentPageUrl: 'https://demo.dotcms.com'
                }
            }
        );
    });

    it('should have page tool list', () => {
        spectator.detectChanges();
        const menuListItems = spectator.queryAll('.page-tools-list__item');

        expect(menuListItems.length).toEqual(3);
    });

    it('should have correct href values in links', () => {
        const tools = mockPageTools.pageTools;
        spectator.detectChanges();

        const anchorElements = spectator.queryAll('.page-tools-list__link');

        expect(anchorElements.length).toEqual(3);

        anchorElements.forEach((anchorElement, index) => {
            const href = anchorElement.getAttribute('href');
            expect(href).toEqual(spectator.component.getRunnableLink(tools[index].runnableLink));
        });
    });
});
