import { of } from 'rxjs';

import { DotPageToolsService } from '@dotcms/data-access';
import { DotPageTool, DotPageToolUrlParams } from '@dotcms/dotcms-models';
import { mockPageTools } from '@dotcms/utils-testing';

import { DotPageToolsSeoStore } from './dot-page-tools-seo.store';

describe('DotPageToolsSeoStore', () => {
    let store: DotPageToolsSeoStore;
    let dotPageToolsService: unknown;
    let pageToolUrlParamsTest: DotPageToolUrlParams;
    const pageTools: DotPageTool[] = mockPageTools.pageTools;

    beforeEach(() => {
        dotPageToolsService = {
            get: () => of(pageTools),
            http: jest.fn(),
            seoToolsUrl: 'assets/seo/page-tools.json'
        };
        store = new DotPageToolsSeoStore(dotPageToolsService as DotPageToolsService);
        pageToolUrlParamsTest = {
            currentUrl: '/blogTest',
            requestHostName: 'localhost',
            siteId: '123',
            languageId: 1
        };
    });

    it('should load page tools', () => {
        store.getTools(pageToolUrlParamsTest);

        store.tools$.subscribe((tools) => {
            expect(tools.pageTools).toEqual(pageTools);
        });
    });
});
