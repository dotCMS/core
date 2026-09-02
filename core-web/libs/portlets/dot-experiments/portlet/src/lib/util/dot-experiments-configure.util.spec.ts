import { DotPageBrowserPage } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { fromBrowserPage, toConfigurePage } from './dot-experiments-configure.util';

/**
 * `DotExperimentConfigurePage` is the only page data the Configure screen holds, and #37005's
 * variant deep link is built from it (FR-003). It gains `languageId` here.
 *
 * Why the field matters more than it looks: `editEmaGuard` treats `language_id` as required but
 * *substitutes* a default of `1` when it is missing, rather than rejecting. So a page object
 * without a language does not produce a broken link — it produces a link that opens the wrong
 * language's content, silently, on every multilingual site. Both constructors already read a
 * source that carries the value, so the only thing standing between correct and silently-wrong is
 * that it be copied across.
 */
describe('dot-experiments-configure.util', () => {
    describe('toConfigurePage', () => {
        it('should carry pageId, title, path and languageId from the contentlet', () => {
            const contentlet = {
                identifier: 'page-1',
                title: 'Pricing',
                url: '/pricing/index',
                languageId: 2
            } as unknown as DotCMSContentlet;

            expect(toConfigurePage(contentlet)).toEqual({
                pageId: 'page-1',
                title: 'Pricing',
                path: '/pricing/index',
                languageId: 2
            });
        });

        it('should keep the existing title fallback to the path when the title is empty', () => {
            const contentlet = {
                identifier: 'page-1',
                title: '',
                url: '/pricing/index',
                languageId: 1
            } as unknown as DotCMSContentlet;

            expect(toConfigurePage(contentlet).title).toBe('/pricing/index');
        });

        // The deep-link builder refuses on a missing languageId (see
        // dot-experiments-uve-link.util.spec.ts). For that refusal to be reachable, the absence has
        // to survive this mapping rather than being defaulted to 1 here — defaulting at either
        // layer is what FR-004 forbids.
        it('should leave languageId undefined when the contentlet has none, not default it to 1', () => {
            const contentlet = {
                identifier: 'page-1',
                title: 'Pricing',
                url: '/pricing/index'
            } as unknown as DotCMSContentlet;

            expect(toConfigurePage(contentlet).languageId).toBeUndefined();
        });
    });

    describe('fromBrowserPage', () => {
        it('should carry pageId, title, path and languageId from the browser row', () => {
            const page = {
                identifier: 'page-2',
                title: 'About us',
                path: '/about-us/index',
                url: '/index',
                languageId: 3
            } as unknown as DotPageBrowserPage;

            expect(fromBrowserPage(page)).toEqual({
                pageId: 'page-2',
                title: 'About us',
                path: '/about-us/index',
                languageId: 3
            });
        });

        it('should keep the existing path fallback to url when path is empty', () => {
            const page = {
                identifier: 'page-2',
                title: 'About us',
                path: '',
                url: '/index',
                languageId: 1
            } as unknown as DotPageBrowserPage;

            expect(fromBrowserPage(page).path).toBe('/index');
        });
    });
});
