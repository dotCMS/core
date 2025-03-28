import { SpectatorService, createServiceFactory } from '@ngneat/spectator';

import { TestBed } from '@angular/core/testing';

import { DotCMSContextService } from './dotcms-context.service';

// import { DotCMSPageAsset, DotCMSPageComponent } from '../../models';
// import { PageResponseMock } from '../../utils/testing.utils';

// const initialPageAssetMock = {} as DotCMSPageAsset;
// const initialComponentsMock = {} as DotCMSPageComponent;

describe('PageContextService', () => {
    let spectator: SpectatorService<DotCMSContextService>;
    let service: DotCMSContextService;

    const createService = createServiceFactory(DotCMSContextService);

    beforeEach(() => {
        TestBed.configureTestingModule({});
        spectator = createService();
        service = spectator.service;
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
