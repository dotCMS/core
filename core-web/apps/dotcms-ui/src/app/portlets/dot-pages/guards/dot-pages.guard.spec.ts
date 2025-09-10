import { SpectatorRouting, createRoutingFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

import { DotContentTypeService, DotContentletService, DotRouterService } from '@dotcms/data-access';
import { EMPTY_CONTENTLET, dotcmsContentTypeBasicMock } from '@dotcms/utils-testing';

import {
    newEditContentForContentTypeGuard,
    newEditContentForContentletGuard
} from './dot-pages.guard';

@Component({
    selector: 'dot-test-component',
    template: '',
    standalone: true
})
class TestComponent {}

const CONTENTLET_MOCK = {
    ...EMPTY_CONTENTLET,
    contentType: 'Blog'
};

const CONTENT_TYPE_WITH_CONTENT_EDITOR2_ENABLED_MOCK = {
    ...dotcmsContentTypeBasicMock,
    metadata: {
        CONTENT_EDITOR2_ENABLED: true
    }
};

const CONTENT_TYPE_WITHOUT_CONTENT_EDITOR2_ENABLED_MOCK = {
    ...dotcmsContentTypeBasicMock,
    metadata: {
        CONTENT_EDITOR2_ENABLED: false
    }
};

describe('Guards', () => {
    let spectator: SpectatorRouting<TestComponent>;
    let dotRouterService: DotRouterService;
    let dotContentTypeService: DotContentTypeService;
    let dotContentletService: DotContentletService;

    const createService = createRoutingFactory({
        component: TestComponent,
        imports: [RouterModule],
        providers: [
            DotRouterService,
            {
                provide: DotContentTypeService,
                useValue: {
                    getContentType: () => of({})
                }
            },
            {
                provide: DotContentletService,
                useValue: {
                    getContentletByInode: () => of({})
                }
            }
        ],
        stubsEnabled: false,
        detectChanges: false,
        routes: [
            {
                path: ':asset',
                canActivate: [newEditContentForContentletGuard],
                component: TestComponent
            },
            {
                path: 'new/:contentType',
                canActivate: [newEditContentForContentTypeGuard],
                component: TestComponent
            }
        ]
    });

    beforeEach(() => {
        spectator = createService();
        dotRouterService = spectator.inject(DotRouterService);
        dotContentTypeService = spectator.inject(DotContentTypeService);
        dotContentletService = spectator.inject(DotContentletService);
    });

    describe('newEditContentForContentTypeGuard', () => {
        it('should return true when CONTENT_EDITOR2_ENABLED is false', async () => {
            const spyContentType = jest
                .spyOn(dotContentTypeService, 'getContentType')
                .mockReturnValue(of(CONTENT_TYPE_WITHOUT_CONTENT_EDITOR2_ENABLED_MOCK));

            spectator.detectChanges();

            const success = await spectator.router.navigate(['new', 'Blog']);
            expect(spyContentType).toHaveBeenCalledWith('Blog');
            expect(spyContentType).toHaveBeenCalledTimes(1);
            expect(success).toBe(true);
        });

        it('should redirect to the new Edit Content portlet when CONTENT_EDITOR2_ENABLED is true', async () => {
            const spyContentType = jest
                .spyOn(dotContentTypeService, 'getContentType')
                .mockReturnValue(of(CONTENT_TYPE_WITH_CONTENT_EDITOR2_ENABLED_MOCK));

            const spyRouter = jest.spyOn(dotRouterService, 'goToURL');

            spectator.detectChanges();

            const success = await spectator.router.navigate(['new', 'Blog']);
            expect(success).toBe(false);
            expect(spyContentType).toHaveBeenCalledWith('Blog');
            expect(spyContentType).toHaveBeenCalledTimes(2);
            expect(spyRouter).toHaveBeenCalledWith('content/new/Blog');
            expect(spyRouter).toHaveBeenCalledTimes(1);
        });
    });

    describe('newEditContentForContentletGuard', () => {
        it('should return true when CONTENT_EDITOR2_ENABLED is false', async () => {
            const spyContentlet = jest
                .spyOn(dotContentletService, 'getContentletByInode')
                .mockReturnValue(of(CONTENTLET_MOCK));
            const spyContentType = jest
                .spyOn(dotContentTypeService, 'getContentType')
                .mockReturnValue(of(CONTENT_TYPE_WITHOUT_CONTENT_EDITOR2_ENABLED_MOCK));

            spectator.detectChanges();

            const success = await spectator.router.navigate(['1234']);
            expect(success).toBe(true);
            expect(spyContentlet).toHaveBeenCalledWith('1234');
            expect(spyContentlet).toHaveBeenCalledTimes(1);
            expect(spyContentType).toHaveBeenCalledWith('Blog');
            expect(spyContentType).toHaveBeenCalledTimes(1);
        });

        it('should redirect to the new Edit Content portlet when CONTENT_EDITOR2_ENABLED is true', async () => {
            const spyContentlet = jest
                .spyOn(dotContentletService, 'getContentletByInode')
                .mockReturnValue(of(CONTENTLET_MOCK));
            const spyContentType = jest
                .spyOn(dotContentTypeService, 'getContentType')
                .mockReturnValue(of(CONTENT_TYPE_WITH_CONTENT_EDITOR2_ENABLED_MOCK));

            const spyRouter = jest.spyOn(dotRouterService, 'goToURL');

            spectator.detectChanges();

            const success = await spectator.router.navigate(['1234']);
            expect(success).toBe(false);
            expect(spyContentlet).toHaveBeenCalledWith('1234');
            expect(spyContentlet).toHaveBeenCalledTimes(4);
            expect(spyContentType).toHaveBeenCalledWith('Blog');
            expect(spyContentType).toHaveBeenCalledTimes(4);
            expect(spyRouter).toHaveBeenCalledWith('content/1234');
            expect(spyRouter).toHaveBeenCalledTimes(1);
        });
    });
});
