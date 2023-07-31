import { of } from 'rxjs';

import { DotPageToolsService } from '@dotcms/data-access';
import { DotPageTool, DotPageToolUrlParams } from '@dotcms/dotcms-models';
import { getRunnableLink } from '@dotcms/utils';
import { mockPageTools } from '@dotcms/utils-testing';

import { DotPageToolsSeoStore } from './dot-page-tools-seo.store';

describe('DotPageToolsSeoStore', () => {
    let store: DotPageToolsSeoStore;
    let dotPageToolsService: jasmine.SpyObj<DotPageToolsService>;
    let pageToolUrlParamsTest: DotPageToolUrlParams;

    beforeEach(() => {
        dotPageToolsService = jasmine.createSpyObj('DotPageToolsService', ['get']);
        store = new DotPageToolsSeoStore(dotPageToolsService);
        pageToolUrlParamsTest = {
            currentUrl: '/blogTest',
            requestHostName: 'localhost',
            siteId: '123',
            languageId: 1
        };
    });

    it('should load page tools', () => {
        const pageTools: DotPageTool[] = mockPageTools.pageTools.map((tool) => {
            return {
                ...tool,
                runnableLink: getRunnableLink(tool.runnableLink, pageToolUrlParamsTest)
            };
        });
        dotPageToolsService.get.and.returnValue(of(pageTools));

        store.getTools(of(pageToolUrlParamsTest));

        store.tools$.subscribe((tools) => {
            expect(tools.pageTools).toEqual(pageTools);
        });
    });
});
