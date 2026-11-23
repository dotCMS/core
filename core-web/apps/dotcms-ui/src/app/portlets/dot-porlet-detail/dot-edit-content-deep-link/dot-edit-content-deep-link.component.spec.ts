import { mockProvider } from '@openng/spectator/vitest';
import { of, throwError } from 'rxjs';
import { Mock, vi } from 'vitest';

import { Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { provideLocationMocks, SpyLocation } from '@angular/common/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, RouterOutlet, Routes } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import {
    DotContentletService,
    DotHttpErrorManagerService,
    DotRouterService
} from '@dotcms/data-access';
// The component must not open the panel itself; the app also lazy-loads the library (#38303).
// eslint-disable-next-line @nx/enforce-module-boundaries
import { DotEditContentOpenerService } from '@dotcms/edit-content';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotEditContentDeepLinkComponent } from './dot-edit-content-deep-link.component';

const IDENTIFIER = '3f0c8e5a-7b2d-4c1e-9a6f-0d4b8e2c1a57';
const FOLDER_INODE = '7c2e9b14-5a3d-4f6e-8b1c-2d9a0e4f6b38';

/** Stands in for a portlet: what the deep link redirects to renders here. */
@Component({ selector: 'dot-test-portlet', imports: [RouterOutlet], template: '<router-outlet />' })
class TestPortletComponent {}

/**
 * The deep-link routes as the admin app registers them: a legacy portlet (`/c/:id`) and the Pages
 * portlet (`/pages`), each with its `:asset` and `new/:contentType` children.
 */
const ROUTES: Routes = [
    {
        path: 'c/:id',
        component: TestPortletComponent,
        children: [
            { path: 'new/:contentType', component: DotEditContentDeepLinkComponent },
            { path: ':asset', component: DotEditContentDeepLinkComponent }
        ]
    },
    {
        path: 'pages',
        component: TestPortletComponent,
        children: [
            { path: 'new/:contentType', component: DotEditContentDeepLinkComponent },
            { path: ':asset', component: DotEditContentDeepLinkComponent }
        ]
    }
];

/**
 * #38303 (FR-026, FR-049) — the path deep links to content (`/<portlet>/<inode>`,
 * `/<portlet>/new/<type>`) are no longer a second way to open a panel. Velocity, workflow emails
 * and old bookmarks still emit them, so the routes stay, and the route component turns what the
 * path names into its portlet's URL with the panel params, replacing the path form in history:
 * the legacy portlets' `/c/<id>`, and the Pages portlet's `/pages`. The portlet then opens the
 * panel as it does for any URL that names one.
 */
describe('DotEditContentDeepLinkComponent', () => {
    let getContentletByInode: Mock;
    let handleError: Mock;

    beforeEach(() => {
        getContentletByInode = vi
            .fn()
            .mockReturnValue(
                of(createFakeContentlet({ inode: 'abc', identifier: IDENTIFIER, languageId: 2 }))
            );
        handleError = vi.fn();

        TestBed.configureTestingModule({
            providers: [
                provideRouter(ROUTES),
                provideLocationMocks(),
                { provide: DotContentletService, useValue: { getContentletByInode } },
                { provide: DotHttpErrorManagerService, useValue: { handle: handleError } },
                // The portlet the admin app reports as current, which the redirect used to read.
                { provide: DotRouterService, useValue: { currentPortlet: { id: 'content' } } },
                mockProvider(DotEditContentOpenerService, { open: vi.fn() })
            ]
        });
    });

    /**
     * Opens `url` and waits for the redirect it leads to.
     *
     * @returns Where it ended: the path, and the query params.
     */
    const follow = async (
        url: string
    ): Promise<{ path: string; params: Record<string, string> }> => {
        const harness = await RouterTestingHarness.create();
        const router = TestBed.inject(Router);

        await harness.navigateByUrl(url);
        await vi.waitFor(() => expect(router.url).not.toBe(url));
        await harness.fixture.whenStable();

        const tree = router.parseUrl(router.url);
        const segments = tree.root.children['primary'].segments.map((segment) => segment.path);

        return {
            path: `/${segments.join('/')}`,
            params: tree.queryParams as Record<string, string>
        };
    };

    /** Whether the last history write replaced the entry the link was opened in. */
    const lastWriteReplaced = (): boolean => {
        const changes = (TestBed.inject(Location) as SpyLocation).urlChanges;

        return changes[changes.length - 1].startsWith('replace: ');
    };

    describe('on a legacy portlet', () => {
        it('redirects a link to content to the portlet with the content and its language, in place', async () => {
            const landed = await follow('/c/content/abc?mId=1a2b');

            expect(getContentletByInode).toHaveBeenCalledWith('abc');
            expect(landed).toEqual({
                path: '/c/content',
                params: { mId: '1a2b', editContent: IDENTIFIER, editContentLang: '2' }
            });
            expect(lastWriteReplaced()).toBe(true);
        });

        it('redirects a link to a create to the portlet with the content type and its folder', async () => {
            const landed = await follow(`/c/content/new/Blog?folder=${FOLDER_INODE}`);

            expect(getContentletByInode).not.toHaveBeenCalled();
            expect(landed).toEqual({
                path: '/c/content',
                params: { folder: FOLDER_INODE, createContent: 'Blog' }
            });
            expect(lastWriteReplaced()).toBe(true);
        });

        // A bad inode, a deleted contentlet or no permission: the standard error, and the portlet.
        it('shows the standard error and goes to the portlet when the content cannot be read', async () => {
            const error = new HttpErrorResponse({ status: 404 });
            getContentletByInode.mockReturnValue(throwError(() => error));

            const landed = await follow('/c/content/gone');

            expect(handleError).toHaveBeenCalledWith(error);
            expect(landed).toEqual({ path: '/c/content', params: {} });
        });
    });

    // FR-026: main had these Pages links, and they keep working (T158).
    describe('on the Pages portlet', () => {
        it('redirects a link to a page to the Pages portlet with the page and its language, in place', async () => {
            const landed = await follow('/pages/abc');

            expect(landed).toEqual({
                path: '/pages',
                params: { editContent: IDENTIFIER, editContentLang: '2' }
            });
            expect(lastWriteReplaced()).toBe(true);
        });

        it('redirects a link to a create to the Pages portlet with the content type, in place', async () => {
            const landed = await follow('/pages/new/htmlpageasset');

            expect(landed).toEqual({ path: '/pages', params: { createContent: 'htmlpageasset' } });
            expect(lastWriteReplaced()).toBe(true);
        });
    });

    it('opens no panel itself: the portlet opens what its URL names', async () => {
        await follow('/c/content/abc');

        expect(TestBed.inject(DotEditContentOpenerService).open).not.toHaveBeenCalled();
    });
});
