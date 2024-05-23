import { SpectatorService, createServiceFactory } from '@ngneat/spectator';

import { TestBed } from '@angular/core/testing';

import { PageContextService } from './page-context.service';

import { DotCMSPageAsset, DynamicComponentEntity } from '../../models';

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
        const mockPageContext = {
            page: {}
        } as DotCMSPageAsset;

        service.setContext(mockPageContext);

        expect(service.pageContextValue).toEqual({
            ...mockPageContext,
            isInsideEditor: false
        });
    });

    it('should set/get components map', () => {
        const mockComponentsMap: Record<string, DynamicComponentEntity> = {
            '1': Promise.resolve(() => ({}))
        } as unknown as Record<string, DynamicComponentEntity>;

        service.setComponentMap(mockComponentsMap);

        expect(service.getComponentMap()).toEqual(mockComponentsMap);
    });
});
