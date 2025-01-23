import { createServiceFactory, mockProvider, SpectatorService, SpyObject } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { HttpClient, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { DotSiteService } from '@dotcms/data-access';
import { Site } from '@dotcms/dotcms-js';
import { createFakeSite } from '@dotcms/utils-testing';

import { HostFieldService, SYSTEM_HOST_NAME } from './host-field.service';

describe('HostFieldService', () => {
    let spectator: SpectatorService<HostFieldService>;
    let dotSiteService: SpyObject<DotSiteService>;
    let httpClient: SpyObject<HttpClient>;

    const mockSites: Site[] = [
        createFakeSite(),
        createFakeSite()
    ];

    const mockFolders = [
        {
            id: 'parent1',
            hostName: 'demo.dotcms.com',
            path: '/parent',
            name: 'parent',
            addChildrenAllowed: true
        },
        {
            id: 'child1',
            hostName: 'demo.dotcms.com',
            path: '/parent/child1',
            name: 'child1',
            addChildrenAllowed: true
        },
        {
            id: 'child2',
            hostName: 'demo.dotcms.com',
            path: '/parent/child2',
            name: 'child2',
            addChildrenAllowed: true
        }
    ];

    const createService = createServiceFactory({
        service: HostFieldService,
        providers: [
            mockProvider(DotSiteService),
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    beforeEach(() => {
        spectator = createService();
        dotSiteService = spectator.inject(DotSiteService);
        httpClient = spectator.inject(HttpClient);
    });

    describe('getSites', () => {
        it('should get and transform sites into TreeNodeItems', (done) => {
            dotSiteService.getSites.mockReturnValue(of(mockSites));

            const params = {
                filter: '',
                perPage: 10,
                page: 1,
                isRequired: false
            };

            spectator.service.getSites(params).subscribe((result) => {
                expect(result.length).toBe(2);
                expect(result[0]).toEqual({
                    key: 'site1',
                    label: 'demo.dotcms.com',
                    data: {
                        id: 'site1',
                        hostname: 'demo.dotcms.com',
                        path: '',
                        type: 'site'
                    },
                    expandedIcon: 'pi pi-folder-open',
                    collapsedIcon: 'pi pi-folder',
                    leaf: false
                });
                expect(dotSiteService.getSites).toHaveBeenCalledWith('', 10, 1);
                done();
            });
        });

        it('should filter out System Host when isRequired is true', (done) => {
            dotSiteService.getSites.mockReturnValue(of(mockSites));

            const params = {
                filter: '',
                isRequired: true
            };

            spectator.service.getSites(params).subscribe((result) => {
                expect(result.length).toBe(1);
                expect(result[0].data.hostname).not.toBe(SYSTEM_HOST_NAME);
                done();
            });
        });

        it('should handle empty sites response', (done) => {
            dotSiteService.getSites.mockReturnValue(of([]));

            spectator.service.getSites({ filter: '', isRequired: false }).subscribe((result) => {
                expect(result).toEqual([]);
                done();
            });
        });
    });

    describe('getCurrentSiteAsTreeNodeItem', () => {
        it('should transform current site into TreeNodeItem', (done) => {
            const currentSite = mockSites[0];
            dotSiteService.getCurrentSite.mockReturnValue(of(currentSite));

            spectator.service.getCurrentSiteAsTreeNodeItem().subscribe((result) => {
                expect(result).toEqual({
                    key: 'site1',
                    label: 'demo.dotcms.com',
                    data: {
                        id: 'site1',
                        hostname: 'demo.dotcms.com',
                        path: '',
                        type: 'site'
                    },
                    expandedIcon: 'pi pi-folder-open',
                    collapsedIcon: 'pi pi-folder',
                    leaf: false
                });
                done();
            });
        });

        it('should handle error when getting current site', (done) => {
            dotSiteService.getCurrentSite.mockReturnValue(throwError(() => new Error('Failed')));

            spectator.service.getCurrentSiteAsTreeNodeItem().subscribe({
                error: (error) => {
                    expect(error.message).toBe('Failed');
                    done();
                }
            });
        });
    });

    describe('getFolders', () => {
        it('should get folders by path', (done) => {
            const mockResponse = { entity: mockFolders };
            httpClient.post.mockReturnValue(of(mockResponse));

            spectator.service.getFolders('/demo.dotcms.com/folder').subscribe((result) => {
                expect(result).toEqual(mockFolders);
                expect(httpClient.post).toHaveBeenCalledWith('/api/v1/folder/byPath', {
                    path: '/demo.dotcms.com/folder'
                });
                done();
            });
        });

        it('should handle error when getting folders', (done) => {
            httpClient.post.mockReturnValue(throwError(() => new Error('Failed')));

            spectator.service.getFolders('/demo.dotcms.com/folder').subscribe({
                error: (error) => {
                    expect(error.message).toBe('Failed');
                    done();
                }
            });
        });
    });

    describe('getFoldersTreeNode', () => {
        it('should transform folders into tree nodes', (done) => {
            const mockResponse = { entity: mockFolders };
            httpClient.post.mockReturnValue(of(mockResponse));

            spectator.service.getFoldersTreeNode('demo.dotcms.com/parent').subscribe((result) => {
                expect(result.parent).toEqual(mockFolders[0]);
                expect(result.folders.length).toBe(2);
                expect(result.folders[0]).toEqual({
                    key: 'child1',
                    label: 'demo.dotcms.com/parent/child1',
                    data: {
                        id: 'child1',
                        hostname: 'demo.dotcms.com',
                        path: '/parent/child1',
                        type: 'folder'
                    },
                    expandedIcon: 'pi pi-folder-open',
                    collapsedIcon: 'pi pi-folder',
                    leaf: false
                });
                done();
            });
        });

        it('should handle empty folders response', (done) => {
            const mockResponse = { entity: [] };
            httpClient.post.mockReturnValue(of(mockResponse));

            spectator.service.getFoldersTreeNode('demo.dotcms.com/empty').subscribe((result) => {
                expect(result.parent).toBeUndefined();
                expect(result.folders).toEqual([]);
                done();
            });
        });
    });

    describe('buildTreeByPaths', () => {
        it('should build hierarchical tree structure', (done) => {
            const mockResponses = [
                { entity: [mockFolders[0], mockFolders[1]] },
                { entity: [mockFolders[0], mockFolders[2]] }
            ];

            httpClient.post.mockReturnValue(of(mockResponses[0]));

            spectator.service.buildTreeByPaths('demo.dotcms.com/parent/child1').subscribe((result) => {
                expect(result.tree).toBeDefined();
                expect(result.node).toBeDefined();
                expect(httpClient.post).toHaveBeenCalledTimes(2);
                done();
            });
        });

        it('should handle single level path', (done) => {
            const mockResponse = { entity: [mockFolders[0]] };
            httpClient.post.mockReturnValue(of(mockResponse));

            spectator.service.buildTreeByPaths('demo.dotcms.com').subscribe((result) => {
                expect(result.tree).toBeDefined();
                expect(result.node).toBeNull();
                expect(httpClient.post).toHaveBeenCalledTimes(1);
                done();
            });
        });

        it('should handle error in tree building', (done) => {
            httpClient.post.mockReturnValue(throwError(() => new Error('Failed')));

            spectator.service.buildTreeByPaths('demo.dotcms.com/parent').subscribe({
                error: (error) => {
                    expect(error.message).toBe('Failed');
                    done();
                }
            });
        });
    });
}); 