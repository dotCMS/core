import { SpectatorRouting, createRoutingFactory } from '@ngneat/spectator';
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
        it('should return true when CONTENT_EDITOR2_ENABLED is false', (done) => {
            const spyContentType = spyOn(dotContentTypeService, 'getContentType').and.returnValue(
                of(CONTENT_TYPE_WITHOUT_CONTENT_EDITOR2_ENABLED_MOCK)
            );

            spectator.detectChanges();

            spectator.router.navigate(['new', 'Blog']).then((success) => {
                expect(spyContentType).toHaveBeenCalledWith('Blog');
                expect(success).toBe(true);
                done();
            });
        });

        it('should redirect to the new Edit Content portlet when CONTENT_EDITOR2_ENABLED is true', (done) => {
            const spyContentType = spyOn(dotContentTypeService, 'getContentType').and.returnValue(
                of(CONTENT_TYPE_WITH_CONTENT_EDITOR2_ENABLED_MOCK)
            );

            const spyRouter = spyOn(dotRouterService, 'goToURL');

            spectator.detectChanges();

            spectator.router.navigate(['new', 'Blog']).then((success) => {
                expect(success).toBe(false);
                expect(spyContentType).toHaveBeenCalledWith('Blog');
                expect(spyRouter).toHaveBeenCalledWith('content/new/Blog');
                done();
            });
        });
    });

    describe('newEditContentForContentletGuard', () => {
        it('should return true when CONTENT_EDITOR2_ENABLED is false', (done) => {
            const spyContentlet = spyOn(
                dotContentletService,
                'getContentletByInode'
            ).and.returnValue(of(CONTENTLET_MOCK));
            const spyContentType = spyOn(dotContentTypeService, 'getContentType').and.returnValue(
                of(CONTENT_TYPE_WITHOUT_CONTENT_EDITOR2_ENABLED_MOCK)
            );

            spectator.detectChanges();

            spectator.router.navigate(['1234']).then((success) => {
                expect(success).toBe(true);
                expect(spyContentlet).toHaveBeenCalledWith('1234');
                expect(spyContentType).toHaveBeenCalledWith('Blog');
                done();
            });
        });

        it('should redirect to the new Edit Content portlet when CONTENT_EDITOR2_ENABLED is true', (done) => {
            const spyContentlet = spyOn(
                dotContentletService,
                'getContentletByInode'
            ).and.returnValue(of(CONTENTLET_MOCK));
            const spyContentType = spyOn(dotContentTypeService, 'getContentType').and.returnValue(
                of(CONTENT_TYPE_WITH_CONTENT_EDITOR2_ENABLED_MOCK)
            );

            const spyRouter = spyOn(dotRouterService, 'goToURL');

            spectator.detectChanges();

            spectator.router.navigate(['1234']).then((success) => {
                expect(success).toBe(false);
                expect(spyContentlet).toHaveBeenCalledWith('1234');
                expect(spyContentType).toHaveBeenCalledWith('Blog');
                expect(spyRouter).toHaveBeenCalledWith('content/1234');
                done();
            });
        });
    });
});
