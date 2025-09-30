/* eslint-disable @typescript-eslint/no-explicit-any */

import { dotAnalyticsEnricherPlugin } from './dot-analytics.enricher.plugin';

import { enrichPagePayloadOptimized, getLocalTime } from '../../shared/dot-content-analytics.utils';

// Mock the utility functions
jest.mock('../../shared/dot-content-analytics.utils', () => ({
    enrichPagePayloadOptimized: jest.fn(),
    getLocalTime: jest.fn().mockReturnValue('2024-01-01T10:00:00.000Z')
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
            mockEnrichPagePayloadOptimized.mockReturnValue({
                context: { site_key: 'test', session_id: 'session', user_id: 'user' },
                page: { url: 'test' },
                device: { language: 'en' },
                local_time: '2024-01-01T10:00:00.000Z'
            } as any);

            // Act
            plugin['page:dot-analytics']({ payload: mockPayload });

            // Assert
            expect(mockEnrichPagePayloadOptimized).toHaveBeenCalledWith(mockPayload);
            expect(mockEnrichPagePayloadOptimized).toHaveBeenCalledTimes(1);
        });

        it('should return the result from enrichPagePayloadOptimized', () => {
            // Arrange
            const mockPayload = { event: 'pageview' } as any;
            const expectedEnriched = {
                context: { site_key: 'test', session_id: 'session', user_id: 'user' },
                page: { url: 'test', title: 'Test' },
                device: { language: 'en' },
                local_time: '2024-01-01T10:00:00.000Z'
            };
            mockEnrichPagePayloadOptimized.mockReturnValue(expectedEnriched as any);

            // Act
            const result = plugin['page:dot-analytics']({ payload: mockPayload });

            // Assert
            expect(result).toEqual({
                context: expectedEnriched.context,
                events: [
                    {
                        event_type: 'pageview',
                        local_time: expectedEnriched.local_time,
                        data: {
                            page: expectedEnriched.page,
                            device: expectedEnriched.device
                        }
                    }
                ]
            });
        });
    });

    describe('Track Event Enrichment', () => {
        it('should create enriched track event with correct structure', () => {
            // Arrange
            const mockPayload = {
                event: 'button_click',
                context: { site_key: 'test', session_id: 'session', user_id: 'user' },
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
                context: mockPayload.context,
                events: [
                    {
                        event_type: 'button_click',
                        local_time: '2024-01-01T10:00:00.000Z',
                        data: {
                            custom: {
                                button_id: 'submit-btn',
                                page: '/contact'
                            }
                        }
                    }
                ]
            });
        });

        it('should preserve existing properties in custom data', () => {
            // Arrange
            const mockPayload = {
                event: 'form_submit',
                context: { site_key: 'test', session_id: 'session', user_id: 'user' },
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
                custom: {
                    form_name: 'contact_form',
                    validation_errors: 0,
                    session_id: 'session_123'
                }
            });
        });

        it('should handle empty properties', () => {
            // Arrange
            const mockPayload = {
                event: 'pageview',
                context: { site_key: 'test', session_id: 'session', user_id: 'user' },
                properties: {}
            } as any;

            // Act
            const result = plugin['track:dot-analytics']({ payload: mockPayload });

            // Assert
            expect(result.events[0].data).toEqual({
                custom: {}
            });
        });

        it('should handle undefined properties', () => {
            // Arrange
            const mockPayload = {
                event: 'custom_event',
                context: { site_key: 'test', session_id: 'session', user_id: 'user' }
                // properties is undefined
            } as any;

            // Act
            const result = plugin['track:dot-analytics']({ payload: mockPayload });

            // Assert
            expect(result.events[0].data).toEqual({
                custom: undefined
            });
        });

        it('should use different local times when called multiple times', () => {
            // Arrange
            const mockPayload = {
                event: 'test_event',
                context: { site_key: 'test', session_id: 'session', user_id: 'user' },
                properties: {}
            } as any;
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
                context: { site_key: 'test', session_id: 'session', user_id: 'user' },
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
                event_type: 'content_interaction',
                data: {
                    custom: {
                        contentId: 'article-123',
                        contentType: 'blog-post',
                        category: 'technology',
                        utm_source: 'newsletter',
                        utm_medium: 'email'
                    }
                }
            });
        });
    });

    describe('Error Handling', () => {
        it('should propagate getLocalTime errors', () => {
            // Arrange
            const mockPayload = {
                event: 'test_event',
                context: { site_key: 'test', session_id: 'session', user_id: 'user' },
                properties: {}
            } as any;
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
            const mockPayload = {
                event: 'pageview',
                context: { site_key: 'test', session_id: 'session', user_id: 'user' },
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

    describe('Memory and Performance', () => {
        it('should not mutate the original payload', () => {
            // Arrange
            const originalPayload = {
                event: 'test_event',
                context: { site_key: 'test', session_id: 'session', user_id: 'user' },
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
            const mockPayload = {
                event: 'test_event',
                context: { site_key: 'test', session_id: 'session', user_id: 'user' },
                properties: {}
            } as any;

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
                const payload = {
                    event: eventType,
                    context: { site_key: 'test', session_id: 'session', user_id: 'user' },
                    properties: {}
                } as any;

                // Act
                const result = plugin['track:dot-analytics']({ payload });

                // Assert
                expect(result.events[0].event_type).toBe(eventType);
                expect(result.events[0].data).toEqual({ custom: {} });
            });
        });
    });
});
