import { SpectatorService, createServiceFactory } from '@ngneat/spectator';

import { TestBed } from '@angular/core/testing';

import { PageContextService } from './page-context.service';

import { DotCMSPageAsset, DotCMSPageComponent } from '../../models';
import { PageResponseMock } from '../../utils/testing.utils';

const initialPageAssetMock = {} as DotCMSPageAsset;
const initialComponentsMock = {} as DotCMSPageComponent;

describe('PageContextService', () => {
    let spectator: SpectatorService<PageContextService>;
    let service: PageContextService;

    const createService = createServiceFactory(PageContextService);

    beforeEach(() => {
        TestBed.configureTestingModule({});
        spectator = createService();
        service = spectator.service;
    });

    it('should set the context', () => {
        service.setContext(initialPageAssetMock, initialComponentsMock);

        expect(service.context).toEqual({
            components: initialComponentsMock,
            pageAsset: initialPageAssetMock,
            isInsideEditor: false
        });
    });

    it('should set the page asset in the context', () => {
        service.setContext(initialPageAssetMock, initialComponentsMock);

        const newPageAssetMock = PageResponseMock as unknown as DotCMSPageAsset;

        service.setPageAsset(newPageAssetMock);

        expect(service.context).toEqual({
            components: initialComponentsMock,
            pageAsset: newPageAssetMock,
            isInsideEditor: false
        });
    });

    it('should return the context', () => {
        service.setContext(initialPageAssetMock, initialComponentsMock);

        expect(service.context).toEqual({
            components: initialComponentsMock,
            pageAsset: initialPageAssetMock,
            isInsideEditor: false
        });
    });

    it('should return the context as an observable', (done) => {
        service.setContext(initialPageAssetMock, initialComponentsMock);

        service.contextObs$.subscribe((context) => {
            expect(context).toEqual({
                components: initialComponentsMock,
                pageAsset: initialPageAssetMock,
                isInsideEditor: false
            });
            done();
        });
    });

    it('should return the page asset as an observable', (done) => {
        service.setContext(initialPageAssetMock, initialComponentsMock);

        service.currentPage$.subscribe((pageAsset) => {
            expect(pageAsset).toEqual(initialPageAssetMock);
            done();
        });
    });
});
