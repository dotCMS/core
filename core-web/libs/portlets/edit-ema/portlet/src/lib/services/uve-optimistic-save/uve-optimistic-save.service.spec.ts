import { describe, expect, it, jest, beforeEach } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';

import { computed, signal } from '@angular/core';

import { DotCMSPageAsset } from '@dotcms/types';

import { UveOptimisticSaveService } from './uve-optimistic-save.service';

import { ActionPayload } from '../../shared/models';
import { UVEStore } from '../../store/dot-uve.store';
import { UveIframeMessengerService } from '../iframe-messenger/uve-iframe-messenger.service';

const MOCK_ACTIVE_CONTENTLET: ActionPayload = {
    contentlet: {
        identifier: 'contentlet-id',
        inode: 'contentlet-inode',
        title: 'Test Contentlet',
        contentType: 'test-type',
        testProp: 'initial-value'
    },
    container: {
        acceptTypes: 'test-type',
        identifier: 'container-id',
        maxContentlets: 1,
        uuid: 'container-uuid'
    },
    language_id: '1',
    pageContainers: [],
    pageId: 'page-id'
} as unknown as ActionPayload;

const createMockPageAsset = (testPropValue = 'initial-value'): DotCMSPageAsset =>
    ({
        page: { identifier: 'page-id', title: 'Test Page' },
        containers: {
            'container-id': {
                contentlets: {
                    'uuid-container-uuid': [
                        {
                            identifier: 'contentlet-id',
                            inode: 'contentlet-inode',
                            title: 'Test Contentlet',
                            contentType: 'test-type',
                            testProp: testPropValue
                        }
                    ]
                }
            }
        }
    }) as unknown as DotCMSPageAsset;

describe('UveOptimisticSaveService', () => {
    let spectator: SpectatorService<UveOptimisticSaveService>;
    let service: UveOptimisticSaveService;
    let pageAssetSignal: ReturnType<typeof signal<DotCMSPageAsset | null>>;
    let includeClientResponse: ReturnType<typeof signal<boolean>>;
    let mockUveStore: {
        pageAsset: ReturnType<typeof computed>;
        setPageAsset: jest.Mock;
    };
    let mockIframeMessenger: { sendPageData: jest.Mock };

    const createService = createServiceFactory({
        service: UveOptimisticSaveService,
        providers: [
            {
                provide: UVEStore,
                useFactory: () => mockUveStore
            },
            {
                provide: UveIframeMessengerService,
                useFactory: () => mockIframeMessenger
            }
        ]
    });

    beforeEach(() => {
        pageAssetSignal = signal<DotCMSPageAsset | null>(null);
        includeClientResponse = signal(true);

        mockIframeMessenger = {
            sendPageData: jest.fn()
        };

        mockUveStore = {
            pageAsset: computed(() => {
                const asset = pageAssetSignal();
                if (!asset) return null;
                return includeClientResponse() ? { ...asset, clientResponse: asset } : { ...asset };
            }),
            setPageAsset: jest.fn((payload: { pageAsset: DotCMSPageAsset | null }) => {
                pageAssetSignal.set(payload?.pageAsset ?? null);
            })
        };

        spectator = createService();
        service = spectator.service;
    });

    describe('updateIframeOptimistically', () => {
        it('should do nothing when pageAsset is null', () => {
            pageAssetSignal.set(null);

            service.updateIframeOptimistically(MOCK_ACTIVE_CONTENTLET, { testProp: 'new-value' });

            expect(mockUveStore.setPageAsset).not.toHaveBeenCalled();
            expect(mockIframeMessenger.sendPageData).not.toHaveBeenCalled();
        });

        it('should do nothing when activeContentlet is null', () => {
            pageAssetSignal.set(createMockPageAsset());

            service.updateIframeOptimistically(null as unknown as ActionPayload, {
                testProp: 'new-value'
            });

            expect(mockUveStore.setPageAsset).not.toHaveBeenCalled();
        });

        it('should update the store with cloned page asset containing new properties', () => {
            pageAssetSignal.set(createMockPageAsset('initial-value'));

            service.updateIframeOptimistically(MOCK_ACTIVE_CONTENTLET, {
                testProp: 'updated-value'
            });

            expect(mockUveStore.setPageAsset).toHaveBeenCalledTimes(1);

            const updatedContentlet = (
                mockUveStore.pageAsset() as DotCMSPageAsset & {
                    containers: {
                        'container-id': {
                            contentlets: { 'uuid-container-uuid': Array<{ testProp: string }> };
                        };
                    };
                }
            ).containers['container-id'].contentlets['uuid-container-uuid'][0];

            expect(updatedContentlet.testProp).toBe('updated-value');
        });

        it('should not mutate the original page asset (uses structuredClone)', () => {
            const original = createMockPageAsset('original');
            pageAssetSignal.set(original);

            service.updateIframeOptimistically(MOCK_ACTIVE_CONTENTLET, { testProp: 'mutated' });

            // The original object reference should not have been mutated
            const originalContainers = original.containers as unknown as {
                'container-id': {
                    contentlets: { 'uuid-container-uuid': Array<{ testProp: string }> };
                };
            };
            expect(
                originalContainers['container-id'].contentlets['uuid-container-uuid'][0].testProp
            ).toBe('original');
        });

        it('should send page data to iframe when clientResponse is present after update', () => {
            includeClientResponse.set(true);
            pageAssetSignal.set(createMockPageAsset());

            service.updateIframeOptimistically(MOCK_ACTIVE_CONTENTLET, {
                testProp: 'updated-value'
            });

            expect(mockIframeMessenger.sendPageData).toHaveBeenCalledTimes(1);
        });

        it('should not send page data when clientResponse is absent after update', () => {
            includeClientResponse.set(false);
            pageAssetSignal.set(createMockPageAsset());

            service.updateIframeOptimistically(MOCK_ACTIVE_CONTENTLET, { testProp: 'v' });

            expect(mockIframeMessenger.sendPageData).not.toHaveBeenCalled();
        });

        it('should strip .content, .requestMetadata, .clientResponse before cloning', () => {
            const dirtyAsset = {
                ...createMockPageAsset(),
                content: { some: 'content' },
                requestMetadata: { meta: 'data' },
                clientResponse: { client: 'response' }
            } as unknown as DotCMSPageAsset;

            pageAssetSignal.set(dirtyAsset);

            service.updateIframeOptimistically(MOCK_ACTIVE_CONTENTLET, { testProp: 'v' });

            const { pageAsset } = (mockUveStore.setPageAsset as jest.Mock).mock.calls[0][0] as {
                pageAsset: DotCMSPageAsset & {
                    content?: unknown;
                    requestMetadata?: unknown;
                    clientResponse?: unknown;
                };
            };
            expect(pageAsset.content).toBeUndefined();
            expect(pageAsset.requestMetadata).toBeUndefined();
            expect(pageAsset.clientResponse).toBeUndefined();
        });
    });

    describe('extractFromRollback', () => {
        it('should return empty object when pageAsset is null', () => {
            pageAssetSignal.set(null);

            const result = service.extractFromRollback(MOCK_ACTIVE_CONTENTLET, ['testProp']);

            expect(result).toEqual({});
        });

        it('should return empty object when activeContentlet is null', () => {
            pageAssetSignal.set(createMockPageAsset());

            const result = service.extractFromRollback(null as unknown as ActionPayload, [
                'testProp'
            ]);

            expect(result).toEqual({});
        });

        it('should extract specified field values from the page asset', () => {
            pageAssetSignal.set(createMockPageAsset('rolled-back-value'));

            const result = service.extractFromRollback(MOCK_ACTIVE_CONTENTLET, ['testProp']);

            expect(result).toEqual({ testProp: 'rolled-back-value' });
        });

        it('should extract multiple fields in a single call', () => {
            const assetWithMultiple = {
                ...createMockPageAsset(),
                containers: {
                    'container-id': {
                        contentlets: {
                            'uuid-container-uuid': [
                                {
                                    identifier: 'contentlet-id',
                                    inode: 'contentlet-inode',
                                    title: 'Test Contentlet',
                                    contentType: 'test-type',
                                    fieldA: 'value-a',
                                    fieldB: 'value-b'
                                }
                            ]
                        }
                    }
                }
            } as unknown as DotCMSPageAsset;

            pageAssetSignal.set(assetWithMultiple);

            const result = service.extractFromRollback(MOCK_ACTIVE_CONTENTLET, [
                'fieldA',
                'fieldB'
            ]);

            expect(result).toEqual({ fieldA: 'value-a', fieldB: 'value-b' });
        });

        it('should return undefined values for fields not present on the contentlet', () => {
            pageAssetSignal.set(createMockPageAsset());

            const result = service.extractFromRollback(MOCK_ACTIVE_CONTENTLET, ['nonExistentProp']);

            expect(result).toEqual({ nonExistentProp: undefined });
        });
    });
});
