import { mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { DotPageToolsService } from '@dotcms/data-access';
import { DotPageTool } from '@dotcms/dotcms-models';
import { mockPageTools } from '@dotcms/utils-testing';

import { DotPageToolsSeoStore } from './dot-page-tools-seo.store';

describe('DotPageToolsSeoStore', () => {
    let store: InstanceType<typeof DotPageToolsSeoStore>;
    const pageTools: DotPageTool[] = mockPageTools.pageTools;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotPageToolsSeoStore,
                mockProvider(DotPageToolsService, {
                    get: () => of(pageTools)
                })
            ]
        });

        store = TestBed.inject(DotPageToolsSeoStore);
    });

    it('should load page tools', () => {
        const pageToolUrlParamsTest = {
            currentUrl: '/blogTest',
            requestHostName: 'localhost',
            siteId: '123',
            languageId: 1
        };
        store.getTools(pageToolUrlParamsTest);

        store.tools$.subscribe((tools) => {
            expect(tools.pageTools).toEqual(pageTools);
        });
    });
});
