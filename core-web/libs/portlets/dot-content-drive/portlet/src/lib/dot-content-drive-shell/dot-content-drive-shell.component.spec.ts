import { it, describe, expect, beforeEach } from '@jest/globals';
import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';

import { DotContentSearchService, DotSiteService } from '@dotcms/data-access';
import { mockSites } from '@dotcms/dotcms-js';
import { DotFolderListViewComponent } from '@dotcms/portlets/content-drive/ui';

import { DotContentDriveShellComponent } from './dot-content-drive-shell.component';

import { SYSTEM_HOST } from '../shared/constants';
import { mockItems, mockRoute, mockSearchResponse } from '../shared/mocks';
import { DotContentDriveStatus } from '../shared/models';
import { DotContentDriveStore } from '../store/dot-content-drive.store';

describe('DotContentDriveShellComponent', () => {
    let spectator: Spectator<DotContentDriveShellComponent>;
    let contentSearchService: DotContentSearchService;
    let siteService: DotSiteService;
    let activatedRoute: ActivatedRoute;
    let store: jest.Mocked<InstanceType<typeof DotContentDriveStore>>;

    const createComponent = createComponentFactory({
        component: DotContentDriveShellComponent,
        providers: [
            {
                provide: DotSiteService,
                useValue: {
                    getCurrentSite: jest.fn().mockReturnValue(of(mockSites[0]))
                }
            },
            {
                provide: DotContentSearchService,
                useValue: {
                    get: jest.fn().mockReturnValue(of(mockSearchResponse))
                }
            },
            {
                provide: ActivatedRoute,
                useValue: mockRoute
            },
            provideHttpClient()
        ],
        componentProviders: [DotContentDriveStore],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent({
            providers: [
                mockProvider(DotContentDriveStore, {
                    initContentDrive: jest.fn(),
                    setItems: jest.fn(),
                    setStatus: jest.fn(),
                    currentSite: jest.fn(),
                    $query: jest.fn(),
                    items: jest.fn().mockReturnValue(mockItems)
                })
            ]
        });
        contentSearchService = spectator.inject(DotContentSearchService);
        siteService = spectator.inject(DotSiteService);
        activatedRoute = spectator.inject(ActivatedRoute);
        store = spectator.inject(DotContentDriveStore, true);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('Initialization', () => {
        it('should initialize the store with current site and route params', () => {
            spectator.detectChanges();

            expect(siteService.getCurrentSite).toHaveBeenCalled();

            expect(store.initContentDrive).toHaveBeenCalledWith({
                currentSite: mockSites[0],
                path: '/test/path',
                filters: {
                    contentType: 'Blog',
                    status: 'published'
                }
            });
        });

        it('should use SYSTEM_HOST if getCurrentSite fails', () => {
            jest.spyOn(siteService, 'getCurrentSite').mockReturnValue(
                throwError(() => new Error('Failed to get site'))
            );

            spectator.detectChanges();

            expect(store.initContentDrive).toHaveBeenCalledWith(
                expect.objectContaining({
                    currentSite: SYSTEM_HOST
                })
            );
        });

        it('should use empty string for path if not provided in query params', () => {
            Object.defineProperty(activatedRoute, 'snapshot', {
                get: jest.fn().mockReturnValue({
                    queryParams: {
                        filters: 'contentType:Blog'
                    }
                })
            });

            spectator.detectChanges();

            expect(store.initContentDrive).toHaveBeenCalledWith(
                expect.objectContaining({
                    path: '',
                    filters: {
                        contentType: 'Blog'
                    }
                })
            );
        });

        it('should use empty object for filters if not provided in query params', () => {
            Object.defineProperty(activatedRoute, 'snapshot', {
                get: jest.fn().mockReturnValue({
                    queryParams: {
                        path: '/test/path/'
                    }
                })
            });

            spectator.detectChanges();

            expect(store.initContentDrive).toHaveBeenCalledWith(
                expect.objectContaining({
                    filters: {},
                    path: '/test/path/'
                })
            );
        });
    });

    describe('Content Loading Effect', () => {
        it('should fetch content when store has a non-SYSTEM_HOST site', () => {
            // Setup store mock to simulate the effect running
            store.currentSite.mockReturnValue(mockSites[0]);
            store.$query.mockReturnValue('+testField:testValue');

            spectator.detectChanges();

            expect(contentSearchService.get).toHaveBeenCalledWith({
                query: '+testField:testValue',
                limit: 40,
                offset: 0
            });

            expect(store.setItems).toHaveBeenCalledWith(mockItems);
        });

        it('should not fetch content when current site is SYSTEM_HOST', () => {
            // Setup store to simulate the effect with SYSTEM_HOST
            store.currentSite.mockReturnValue(SYSTEM_HOST);

            spectator.detectChanges();

            expect(contentSearchService.get).not.toHaveBeenCalled();
        });

        it('should handle errors from content search service', () => {
            // Setup store mock
            store.currentSite.mockReturnValue(mockSites[0]);
            store.$query.mockReturnValue('test query');

            // Mock error from content search
            jest.spyOn(contentSearchService, 'get').mockReturnValue(
                throwError(() => new Error('Failed to get content'))
            );

            spectator.detectChanges();

            expect(store.setStatus).toHaveBeenCalledWith(DotContentDriveStatus.ERROR);
            expect(store.setItems).not.toHaveBeenCalled();
        });
    });

    describe('DOM', () => {
        it('should have a dot-folder-list-view with items from store', () => {
            spectator.detectChanges();

            const folderListView = spectator.query(DotFolderListViewComponent);

            expect(folderListView).toBeTruthy();
            expect(folderListView?.items()).toEqual(mockItems);
        });
    });
});
