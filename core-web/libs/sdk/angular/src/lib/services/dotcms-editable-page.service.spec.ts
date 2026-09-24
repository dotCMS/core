import { createServiceFactory, SpectatorService } from '@openng/spectator/vitest';
import { Mock, expect, vi } from 'vitest';

import { UVEEventType, DotCMSPageResponse } from '@dotcms/types';
import { getUVEState, initUVE, createUVESubscription, updateNavigation } from '@dotcms/uve';

import { DotCMSEditablePageService } from './dotcms-editable-page.service';

// Import the mocked modules
// Mock external dependencies
vi.mock('@dotcms/uve', () => ({
    getUVEState: vi.fn(),
    initUVE: vi.fn(),
    createUVESubscription: vi.fn(),
    updateNavigation: vi.fn()
}));

describe('DotCMSEditablePageService', () => {
    let spectator: SpectatorService<DotCMSEditablePageService>;
    let service: DotCMSEditablePageService;
    let unsubscribeMock: Mock;

    const mockPageAsset: DotCMSPageResponse = {
        pageAsset: {
            page: {
                pageURI: '/test-page',
                title: 'Test Page'
            }
        },
        graphql: {}
    } as DotCMSPageResponse;

    const mockUpdatedPageAsset: DotCMSPageResponse = {
        pageAsset: {
            page: {
                pageURI: '/test-page',
                title: 'Updated Test Page'
            }
        },
        graphql: {}
    } as DotCMSPageResponse;

    const createService = createServiceFactory({
        service: DotCMSEditablePageService
    });

    beforeEach(() => {
        // Reset all mocks
        vi.clearAllMocks();

        // Setup default mock behaviors
        unsubscribeMock = vi.fn();
        (getUVEState as Mock).mockReturnValue(true);
        (createUVESubscription as Mock).mockReturnValue({ unsubscribe: unsubscribeMock });

        spectator = createService();
        service = spectator.service;
    });

    describe('listenEditablePage', () => {
        it('should initialize UVE and setup subscriptions', () => {
            (getUVEState as Mock).mockReturnValue(true);

            const result = service.listen(mockPageAsset);

            expect(initUVE).toHaveBeenCalledWith(mockPageAsset);
            expect(updateNavigation).toHaveBeenCalledWith('/test-page');
            expect(createUVESubscription).toHaveBeenCalledWith(
                UVEEventType.CONTENT_CHANGES,
                expect.any(Function)
            );
            expect(result).toBeTruthy();
        });

        it('should return observable with the initial page asset when UVE state is false', () => {
            (getUVEState as Mock).mockReturnValue(false);
            let result: DotCMSPageResponse | undefined = undefined;

            service.listen(mockPageAsset).subscribe((res) => {
                result = res;
            });

            expect(initUVE).not.toHaveBeenCalled();
            expect(updateNavigation).not.toHaveBeenCalled();
            expect(createUVESubscription).not.toHaveBeenCalled();
            expect(result).toEqual(mockPageAsset);
        });

        it('should emit updated page asset when UVE event occurs', () => {
            let capturedCallback = (_payload: DotCMSPageResponse): void => {
                // Empty default implementation
            };

            (createUVESubscription as Mock).mockImplementation((eventType, callback) => {
                capturedCallback = callback;

                return { unsubscribe: unsubscribeMock };
            });

            const emittedValues: Array<DotCMSPageResponse | undefined> = [];

            service.listen(mockPageAsset).subscribe((value) => {
                emittedValues.push(value);
            });

            capturedCallback(mockUpdatedPageAsset);

            expect(emittedValues.length).toBe(1);
            expect(emittedValues[0]).toEqual(mockUpdatedPageAsset);
        });

        it('should handle undefined page asset', () => {
            service.listen();

            expect(initUVE).toHaveBeenCalled();
            expect(updateNavigation).not.toHaveBeenCalled();
        });
    });
});
