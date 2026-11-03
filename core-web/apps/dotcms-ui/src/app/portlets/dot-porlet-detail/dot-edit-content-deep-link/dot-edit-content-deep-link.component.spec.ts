import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/vitest';
import { of, throwError } from 'rxjs';
import { Mock, vi } from 'vitest';

import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, Params, Router } from '@angular/router';

import {
    DotContentletService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotRouterService
} from '@dotcms/data-access';
// The component must not open the panel itself; the app also lazy-loads the library (#38303).
// eslint-disable-next-line @nx/enforce-module-boundaries
import { DotEditContentOpenerService } from '@dotcms/edit-content';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { DotEditContentDeepLinkComponent } from './dot-edit-content-deep-link.component';

const IDENTIFIER = '3f0c8e5a-7b2d-4c1e-9a6f-0d4b8e2c1a57';
const FOLDER_INODE = '7c2e9b14-5a3d-4f6e-8b1c-2d9a0e4f6b38';

/**
 * #38303 (FR-049) — the path deep links to content (`/c/:id/:asset`, `/c/:id/new/:contentType`)
 * are no longer a second way to open a panel. Velocity and old bookmarks still emit them, so the
 * routes stay, and the route component turns what the path names into the portlet's URL with the
 * panel params, replacing the path form in history. The portlet then opens the panel as it does
 * for any URL that names one.
 *
 * The portlet is the one `DotRouterService.currentPortlet` names.
 */
describe('DotEditContentDeepLinkComponent', () => {
    let spectator: Spectator<DotEditContentDeepLinkComponent>;
    let navigate: Mock;
    let getContentletByInode: Mock;
    let handleError: Mock;

    const createComponent = createComponentFactory({
        component: DotEditContentDeepLinkComponent,
        detectChanges: false,
        providers: [
            mockProvider(DotEditContentOpenerService, { open: vi.fn() }),
            // Reloading the portlet is the portlet's business once it opens the panel.
            mockProvider(DotIframeService)
        ]
    });

    /**
     * Mounts the component on a deep link.
     *
     * @param params The route params (`asset`, or `contentType`).
     * @param queryParams The route query params (`folder` for a create).
     */
    const mountOn = (params: Params, queryParams: Params = {}): void => {
        navigate = vi.fn().mockResolvedValue(true);
        handleError = vi.fn();
        getContentletByInode = vi
            .fn()
            .mockReturnValue(
                of(createFakeContentlet({ inode: 'abc', identifier: IDENTIFIER, languageId: 2 }))
            );

        spectator = createComponent({
            providers: [
                { provide: Router, useValue: { navigate } },
                {
                    provide: DotRouterService,
                    useValue: { currentPortlet: { id: 'content', url: '/c/content/abc' } }
                },
                { provide: DotContentletService, useValue: { getContentletByInode } },
                { provide: DotHttpErrorManagerService, useValue: { handle: handleError } },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        snapshot: {
                            params,
                            queryParams,
                            paramMap: convertToParamMap(params),
                            queryParamMap: convertToParamMap(queryParams)
                        }
                    }
                }
            ]
        });
    };

    describe('a link to content (`:asset`)', () => {
        it('redirects to the portlet with the content and its language, replacing the path form', () => {
            mountOn({ asset: 'abc' });
            spectator.detectChanges();

            expect(getContentletByInode).toHaveBeenCalledWith('abc');
            expect(navigate).toHaveBeenCalledTimes(1);
            expect(navigate).toHaveBeenCalledWith(['/c', 'content'], {
                queryParams: { editContent: IDENTIFIER, editContentLang: '2' },
                queryParamsHandling: 'merge',
                replaceUrl: true
            });
        });

        // A bad inode, a deleted contentlet or no permission: the standard error, and the portlet.
        it('shows the standard error and goes to the portlet when the content cannot be read', () => {
            mountOn({ asset: 'gone' });
            const error = new HttpErrorResponse({ status: 404 });
            getContentletByInode.mockReturnValue(throwError(() => error));
            spectator.detectChanges();

            expect(handleError).toHaveBeenCalledWith(error);
            expect(navigate).toHaveBeenCalledWith(['/c', 'content'], {
                queryParamsHandling: 'merge',
                replaceUrl: true
            });
        });
    });

    describe('a link to a create (`new/:contentType`)', () => {
        it('redirects to the portlet with the content type, replacing the path form', () => {
            mountOn({ contentType: 'Blog' });
            spectator.detectChanges();

            expect(getContentletByInode).not.toHaveBeenCalled();
            expect(navigate).toHaveBeenCalledWith(['/c', 'content'], {
                queryParams: { createContent: 'Blog' },
                queryParamsHandling: 'merge',
                replaceUrl: true
            });
        });

        it('keeps the folder the link names beside it', () => {
            mountOn({ contentType: 'Blog' }, { folder: FOLDER_INODE });
            spectator.detectChanges();

            expect(navigate).toHaveBeenCalledWith(['/c', 'content'], {
                queryParams: { createContent: 'Blog', folder: FOLDER_INODE },
                queryParamsHandling: 'merge',
                replaceUrl: true
            });
        });
    });

    it('opens no panel itself: the portlet opens what its URL names', () => {
        mountOn({ asset: 'abc' });
        spectator.detectChanges();

        expect(spectator.inject(DotEditContentOpenerService).open).not.toHaveBeenCalled();
    });
});
