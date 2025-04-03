import { SpectatorService, createServiceFactory } from '@ngneat/spectator';

import { TestBed } from '@angular/core/testing';

import { DotCMSStore } from './dotcms.store';

// import { DotCMSPageAsset, DotCMSPageComponent } from '../../models';
// import { PageResponseMock } from '../../utils/testing.utils';

// const initialPageAssetMock = {} as DotCMSPageAsset;
// const initialComponentsMock = {} as DotCMSPageComponent;

describe('DotCMSStore', () => {
    let spectator: SpectatorService<DotCMSStore>;
    let service: DotCMSStore;

    const createService = createServiceFactory(DotCMSStore);

    beforeEach(() => {
        TestBed.configureTestingModule({});
        spectator = createService();
        service = spectator.service;
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
