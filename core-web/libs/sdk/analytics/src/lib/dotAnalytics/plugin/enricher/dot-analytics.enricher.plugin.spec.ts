/* eslint-disable @typescript-eslint/no-explicit-any */

import { dotAnalyticsEnricherPlugin } from './dot-analytics.enricher.plugin';

import { ANALYTICS_SOURCE_TYPE, EVENT_TYPES } from '../../shared/dot-content-analytics.constants';
import { enrichPagePayloadOptimized, getLocalTime } from '../../shared/dot-content-analytics.utils';

// Mock the utility functions
jest.mock('../../shared/dot-content-analytics.utils', () => ({
    enrichPagePayloadOptimized: jest.fn(),
    getLocalTime: jest.fn().mockReturnValue('2024-01-01T10:00:00.000Z')
}));

// Mock constants
jest.mock('../../shared/dot-content-analytics.constants', () => ({
    ANALYTICS_SOURCE_TYPE: 'dotAnalytics',
    EVENT_TYPES: {
        PAGEVIEW: 'pageview',
        TRACK: 'track'
    }
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
        mockGetLocalTime.mockReturnValue('2024-01-01T10:00:00.000Z');
        mockEnrichPagePayloadOptimized.mockReturnValue({} as any);
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

            // Act
            plugin['page:dot-analytics']({ payload: mockPayload });

            // Assert
            expect(mockEnrichPagePayloadOptimized).toHaveBeenCalledWith(mockPayload);
            expect(mockEnrichPagePayloadOptimized).toHaveBeenCalledTimes(1);
        });

        it('should return the result from enrichPagePayloadOptimized', () => {
            // Arrange
            const mockPayload = { event: 'pageview' } as any;
            const expectedResult = { enriched: true };
            mockEnrichPagePayloadOptimized.mockReturnValue(expectedResult as any);

            // Act
            const result = plugin['page:dot-analytics']({ payload: mockPayload });

            // Assert
            expect(result).toEqual(expectedResult);
        });
    });

    describe('Track Event Enrichment', () => {
        it('should create enriched track event with correct structure', () => {
            // Arrange
            const mockPayload = {
                event: 'button_click',
                properties: {
                    button_id: 'submit-btn',
                    page: '/contact'
                }
            } as any;

            // Act
            const result = plugin['track:dot-analytics']({ payload: mockPayload });

            // Assert
            expect(mockGetLocalTime).toHaveBeenCalledTimes(1);
            expect(result).toEqual({
                events: [
                    {
                        event_type: EVENT_TYPES.TRACK,
                        local_time: '2024-01-01T10:00:00.000Z',
                        data: {
                            event: 'button_click',
                            button_id: 'submit-btn',
                            page: '/contact',
                            src: ANALYTICS_SOURCE_TYPE
                        }
                    }
                ]
            });
        });

        it('should preserve existing properties and add src', () => {
            // Arrange
            const mockPayload = {
                event: 'form_submit',
                properties: {
                    form_name: 'contact_form',
                    validation_errors: 0,
                    session_id: 'session_123'
                }
            } as any;

            // Act
            const result = plugin['track:dot-analytics']({ payload: mockPayload });

            // Assert
            expect(result.events[0].data).toEqual({
                event: 'form_submit',
                form_name: 'contact_form',
                validation_errors: 0,
                session_id: 'session_123',
                src: ANALYTICS_SOURCE_TYPE
            });
        });

        it('should handle empty properties', () => {
            // Arrange
            const mockPayload = {
                event: 'pageview',
                properties: {}
            } as any;

            // Act
            const result = plugin['track:dot-analytics']({ payload: mockPayload });

            // Assert
            expect(result.events[0].data).toEqual({
                event: 'pageview',
                src: ANALYTICS_SOURCE_TYPE
            });
        });

        it('should handle undefined properties', () => {
            // Arrange
            const mockPayload = {
                event: 'custom_event'
                // properties is undefined
            } as any;

            // Act
            const result = plugin['track:dot-analytics']({ payload: mockPayload });

            // Assert
            expect(result.events[0].data).toEqual({
                event: 'custom_event',
                src: ANALYTICS_SOURCE_TYPE
            });
        });

        it('should use different local times when called multiple times', () => {
            // Arrange
            const mockPayload = { event: 'test_event', properties: {} } as any;
            const time1 = '2024-01-01T10:00:00.000Z';
            const time2 = '2024-01-01T11:00:00.000Z';

            mockGetLocalTime.mockReturnValueOnce(time1).mockReturnValueOnce(time2);

            // Act
            const result1 = plugin['track:dot-analytics']({ payload: mockPayload });
            const result2 = plugin['track:dot-analytics']({ payload: mockPayload });

            // Assert
            expect(result1.events[0].local_time).toBe(time1);
            expect(result2.events[0].local_time).toBe(time2);
            expect(mockGetLocalTime).toHaveBeenCalledTimes(2);
        });
    });

    describe('Integration Scenarios', () => {
        it('should handle real-world track event data', () => {
            // Arrange
            const realWorldPayload = {
                event: 'content_interaction',
                properties: {
                    contentId: 'article-123',
                    contentType: 'blog-post',
                    category: 'technology',
                    utm_source: 'newsletter',
                    utm_medium: 'email'
                }
            } as any;

            // Act
            const result = plugin['track:dot-analytics']({ payload: realWorldPayload });

            // Assert
            expect(result.events[0]).toMatchObject({
                event_type: EVENT_TYPES.TRACK,
                data: {
                    event: 'content_interaction',
                    contentId: 'article-123',
                    contentType: 'blog-post',
                    category: 'technology',
                    utm_source: 'newsletter',
                    utm_medium: 'email',
                    src: ANALYTICS_SOURCE_TYPE
                }
            });
        });
    });

    describe('Error Handling', () => {
        it('should propagate getLocalTime errors', () => {
            // Arrange
            const mockPayload = { event: 'test_event', properties: {} } as any;
            mockGetLocalTime.mockImplementation(() => {
                throw new Error('Time service unavailable');
            });

            // Act & Assert
            expect(() => plugin['track:dot-analytics']({ payload: mockPayload })).toThrow(
                'Time service unavailable'
            );
        });

        it('should propagate enrichPagePayloadOptimized errors', () => {
            // Arrange
            const mockPayload = { event: 'pageview', properties: {} } as any;
            mockEnrichPagePayloadOptimized.mockImplementation(() => {
                throw new Error('Enrichment failed');
            });

            // Act & Assert
            expect(() => plugin['page:dot-analytics']({ payload: mockPayload })).toThrow(
                'Enrichment failed'
            );
        });
    });

    describe('Memory and Performance', () => {
        it('should not mutate the original payload', () => {
            // Arrange
            const originalPayload = {
                event: 'test_event',
                properties: { original: 'value' }
            } as any;
            const payloadCopy = JSON.parse(JSON.stringify(originalPayload));

            // Act
            plugin['track:dot-analytics']({ payload: originalPayload });

            // Assert
            expect(originalPayload).toEqual(payloadCopy);
        });

        it('should create new objects for each track call', () => {
            // Arrange
            const mockPayload = { event: 'test_event', properties: {} } as any;

            // Act
            const result1 = plugin['track:dot-analytics']({ payload: mockPayload });
            const result2 = plugin['track:dot-analytics']({ payload: mockPayload });

            // Assert
            expect(result1).not.toBe(result2);
            expect(result1.events).not.toBe(result2.events);
            expect(result1.events[0]).not.toBe(result2.events[0]);
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

        it('should handle different event types correctly', () => {
            // Arrange
            const events = ['click', 'scroll', 'form_submit', 'page_exit'];

            events.forEach((eventType) => {
                const payload = { event: eventType, properties: {} } as any;

                // Act
                const result = plugin['track:dot-analytics']({ payload });

                // Assert
                expect(result.events[0].data.event).toBe(eventType);
                expect(result.events[0].event_type).toBe(EVENT_TYPES.TRACK);
            });
        });
    });
});
