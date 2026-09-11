import { mockProvider } from '@openng/spectator/vitest';
import { firstValueFrom, of } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { filter } from 'rxjs/operators';

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

    it('should load page tools', async () => {
        const pageToolUrlParamsTest = {
            currentUrl: '/blogTest',
            // An absolute URL, not a bare host: getRunnableLink() does
            // `new URL(requestHostName)`, and `new URL('localhost')` throws "Invalid
            // URL". That killed the effect before it ever reached updatePageTools, so
            // the state stayed empty — invisible until now because the assertion lived
            // inside a subscribe handler whose failure Jest dropped.
            requestHostName: 'http://localhost',
            siteId: '123',
            languageId: 1
        };
        store.getTools(pageToolUrlParamsTest);

        // Awaited, not asserted inside subscribe. ComponentStore's select() emits on
        // rxjs' queue scheduler, so the loaded list is not in state by the time the
        // next statement runs: the old test asserted on the initial empty emission and
        // threw inside the subscribe handler, where rxjs reports it asynchronously and
        // Jest dropped it. `tools$` replays current state, so this resolves at once
        // once the effect has settled.
        const tools = await firstValueFrom(
            store.tools$.pipe(filter((state) => state.pageTools.length > 0))
        );

        expect(tools.pageTools).toEqual(pageTools);
    });
});
