import { SpectatorService, createServiceFactory } from '@ngneat/spectator';

import { TestBed } from '@angular/core/testing';

import { PageContextService } from './page-context.service';

import { DotCMSPageAsset, DotCMSPageComponent } from '../../models';

describe('PageContextService', () => {
    let spectator: SpectatorService<PageContextService>;
    let service: PageContextService;

    const createService = createServiceFactory(PageContextService);

    beforeEach(() => {
        TestBed.configureTestingModule({});
        spectator = createService();
        service = spectator.service;
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should set the context', () => {
        const pageAssetMock = {} as DotCMSPageAsset;
        const componentsMock = {} as DotCMSPageComponent;

        service.setContext(pageAssetMock, componentsMock);

        expect(service.pageContextValue).toEqual({
            components: componentsMock,
            pageAsset: pageAssetMock,
            isInsideEditor: false
        });
    });

    it('should return the context', () => {
        const pageAssetMock = {} as DotCMSPageAsset;
        const componentsMock = {} as DotCMSPageComponent;

        service.setContext(pageAssetMock, componentsMock);

        expect(service.pageContextValue).toEqual({
            components: componentsMock,
            pageAsset: pageAssetMock,
            isInsideEditor: false
        });
    });
});
