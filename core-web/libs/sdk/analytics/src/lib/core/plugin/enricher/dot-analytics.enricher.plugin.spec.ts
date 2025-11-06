/* eslint-disable @typescript-eslint/no-explicit-any */

import { dotAnalyticsEnricherPlugin } from './dot-analytics.enricher.plugin';

import { DotCMSPredefinedEventType } from '../../shared/constants/dot-content-analytics.constants';
import { enrichPagePayloadOptimized, getLocalTime } from '../../shared/dot-content-analytics.utils';

// Mock the utility functions
jest.mock('../../shared/dot-content-analytics.utils', () => ({
    enrichPagePayloadOptimized: jest.fn(),
    getLocalTime: jest.fn()
}));

describe('dotAnalyticsEnricherPlugin', () => {
    let plugin: ReturnType<typeof dotAnalyticsEnricherPlugin>;
    const mockEnrichPagePayloadOptimized = enrichPagePayloadOptimized as jest.MockedFunction<
        typeof enrichPagePayloadOptimized
    >;
    const mockGetLocalTime = getLocalTime as jest.MockedFunction<typeof getLocalTime>;

    beforeEach(() => {
        // Reset all mocks before each test
        jest.clearAllMocks();

        // Create fresh plugin instance
        plugin = dotAnalyticsEnricherPlugin();

        // Set default mock return values
        mockEnrichPagePayloadOptimized.mockReturnValue({} as any);
        mockGetLocalTime.mockReturnValue('2024-01-01T10:00:00.000Z');
    });

    describe('Plugin Configuration', () => {
        it('should have the correct plugin name', () => {
            expect(plugin.name).toBe('enrich-dot-analytics');
        });

        it('should expose page and track enrichment methods', () => {
            expect(plugin).toHaveProperty('page:dot-analytics');
            expect(plugin).toHaveProperty('track:dot-analytics');
            expect(typeof plugin['page:dot-analytics']).toBe('function');
            expect(typeof plugin['track:dot-analytics']).toBe('function');
        });
    });

    describe('Page View Enrichment', () => {
        it('should call enrichPagePayloadOptimized with the provided payload', () => {
            // Arrange
            const mockPayload = { event: 'pageview', properties: { page: '/test' } } as any;
            mockEnrichPagePayloadOptimized.mockReturnValue({
                context: {
                    site_auth: 'test-auth',
                    session_id: 'session',
                    user_id: 'user',
                    device: {
                        language: 'en',
                        screen_resolution: '1920x1080',
                        viewport_width: '1024',
                        viewport_height: '768'
                    }
                },
                page: { url: 'test' },
                local_time: '2024-01-01T10:00:00.000Z'
            } as any);

            // Act
            plugin['page:dot-analytics']({ payload: mockPayload });

            // Assert
            expect(mockEnrichPagePayloadOptimized).toHaveBeenCalledWith(mockPayload);
            expect(mockEnrichPagePayloadOptimized).toHaveBeenCalledTimes(1);
        });

        it('should return enriched payload with page, utm, custom data, and local_time', () => {
            // Arrange
            const mockPayload = { event: 'pageview' } as any;
            const expectedEnriched = {
                context: {
                    site_auth: 'test-auth',
                    session_id: 'session',
                    user_id: 'user',
                    device: {
                        language: 'en',
                        screen_resolution: '1920x1080',
                        viewport_width: '1024',
                        viewport_height: '768'
                    }
                },
                page: { url: 'test', title: 'Test' },
                utm: { source: 'google', medium: 'cpc' },
                custom: { custom_prop: 'value' },
                local_time: '2024-01-01T10:00:00.000Z'
            };
            mockEnrichPagePayloadOptimized.mockReturnValue(expectedEnriched as any);

            // Act
            const result = plugin['page:dot-analytics']({ payload: mockPayload });

            // Assert
            // Plugin should return enriched data, NOT structured events
            expect(result).toEqual(expectedEnriched);
        });

        it('should throw error if page data is missing', () => {
            // Arrange
            const mockPayload = { event: 'pageview' } as any;
            mockEnrichPagePayloadOptimized.mockReturnValue({
                context: { site_auth: 'test-auth', session_id: 'session', user_id: 'user' },
                // page is undefined
                local_time: '2024-01-01T10:00:00.000Z'
            } as any);

            // Act & Assert
            expect(() => plugin['page:dot-analytics']({ payload: mockPayload })).toThrow(
                'DotCMS Analytics: Missing required page data'
            );
        });
    });

    describe('Track Event Enrichment', () => {
        let originalTitle: string;
        let originalHref: string;

        beforeEach(() => {
            // Save originals
            originalTitle = document.title;
            originalHref = window.location.href;

            // Mock document title and window location
            Object.defineProperty(document, 'title', {
                value: 'Test Page',
                writable: true,
                configurable: true
            });

            // Mock window.location.href
            delete (window as any).location;
            (window as any).location = { href: 'http://localhost/test' };
        });

        afterEach(() => {
            // Restore originals
            Object.defineProperty(document, 'title', {
                value: originalTitle,
                writable: true,
                configurable: true
            });
        });

        it('should enrich content_impression events with page data', () => {
            // Arrange
            const mockPayload = {
                event: DotCMSPredefinedEventType.CONTENT_IMPRESSION,
                context: {
                    site_auth: 'test-auth',
                    session_id: 'session',
                    user_id: 'user'
                },
                properties: {
                    content: {
                        identifier: 'content-123',
                        inode: 'inode-456',
                        title: 'Test Content',
                        content_type: 'Blog'
                    },
                    position: {
                        viewport_offset_pct: 25.5,
                        dom_index: 2
                    }
                }
            } as any;

            // Act
            const result = plugin['track:dot-analytics']({ payload: mockPayload });

            // Assert
            // Should NOT call enrichPagePayloadOptimized for track events
            expect(mockEnrichPagePayloadOptimized).not.toHaveBeenCalled();
            expect(result).toMatchObject({
                ...mockPayload,
                page: {
                    title: 'Test Page',
                    url: 'http://localhost/test'
                }
            });
            expect(result.local_time).toBeDefined();
            expect(typeof result.local_time).toBe('string');
        });

        it('should enrich custom events with only local_time', () => {
            // Arrange
            const mockPayload = {
                event: 'button_click',
                context: { site_auth: 'test-auth', session_id: 'session', user_id: 'user' },
                properties: {
                    button_id: 'submit-btn',
                    button_text: 'Submit'
                }
            } as any;

            // Act
            const result = plugin['track:dot-analytics']({ payload: mockPayload });

            // Assert
            // Should NOT call enrichPagePayloadOptimized for custom events
            expect(mockEnrichPagePayloadOptimized).not.toHaveBeenCalled();
            // Custom events should only have local_time added, no page/utm/custom data
            expect(result).toMatchObject({
                ...mockPayload
            });
            expect(result.local_time).toBeDefined();
            expect(typeof result.local_time).toBe('string');
            // Should NOT have page/utm/custom data
            expect(result.page).toBeUndefined();
            expect(result.utm).toBeUndefined();
            expect(result.custom).toBeUndefined();
        });

        it('should pass through properties without modifying them for custom events', () => {
            // Arrange
            const mockPayload = {
                event: 'form_submit',
                context: { site_auth: 'test-auth', session_id: 'session', user_id: 'user' },
                properties: {
                    form_name: 'contact_form',
                    validation_errors: 0
                }
            } as any;

            // Act
            const result = plugin['track:dot-analytics']({ payload: mockPayload });

            // Assert
            // Properties should remain unchanged
            expect(result.properties).toEqual(mockPayload.properties);
        });
    });

    describe('Error Handling', () => {
        it('should propagate enrichPagePayloadOptimized errors', () => {
            // Arrange
            const mockPayload = {
                event: 'pageview',
                context: { site_auth: 'test-auth', session_id: 'session', user_id: 'user' },
                properties: {}
            } as any;
            mockEnrichPagePayloadOptimized.mockImplementation(() => {
                throw new Error('Enrichment failed');
            });

            // Act & Assert
            expect(() => plugin['page:dot-analytics']({ payload: mockPayload })).toThrow(
                'Enrichment failed'
            );
        });
    });

    describe('Plugin Behavior', () => {
        it('should maintain plugin instance properties', () => {
            // Arrange & Act
            const pluginInstance1 = dotAnalyticsEnricherPlugin();
            const pluginInstance2 = dotAnalyticsEnricherPlugin();

            // Assert
            expect(pluginInstance1.name).toBe(pluginInstance2.name);
            expect(pluginInstance1).not.toBe(pluginInstance2);
        });
    });
});
