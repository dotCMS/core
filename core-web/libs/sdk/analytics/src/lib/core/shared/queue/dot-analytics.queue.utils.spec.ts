/* eslint-disable @typescript-eslint/no-explicit-any */
import { beforeEach, describe, expect, it, jest } from '@jest/globals';

import { DEFAULT_QUEUE_CONFIG } from '../constants';
import { sendAnalyticsEvent } from '../http/dot-analytics.http';
import { DotCMSAnalyticsConfig, DotCMSAnalyticsEventContext, DotCMSEvent } from '../models';

// Mock the HTTP utility
jest.mock('../dot-analytics.http', () => ({
    sendAnalyticsEvent: jest.fn()
}));

// Mock @analytics/queue-utils
jest.mock('@analytics/queue-utils', () => ({
    __esModule: true,
    default: jest.fn()
}));

// Import after mocking
// eslint-disable-next-line import/order
import smartQueue from '@analytics/queue-utils';

// eslint-disable-next-line import/order
import { createAnalyticsQueue } from './dot-analytics.queue.utils';

// Mock queue methods
const mockQueuePush = jest.fn();
const mockQueueSize = jest.fn();
const mockQueueFlush = jest.fn();
const mockQueuePause = jest.fn();
const mockQueueResume = jest.fn();

// Mock console methods
const mockConsoleLog = jest.spyOn(console, 'log').mockImplementation(() => {
    // do nothing
});
const mockConsoleWarn = jest.spyOn(console, 'warn').mockImplementation(() => {
    // do nothing
});

describe('createAnalyticsQueue', () => {
    let mockConfig: DotCMSAnalyticsConfig;
    let mockContext: DotCMSAnalyticsEventContext;
    let mockEvent: DotCMSEvent;
    let addEventListenerSpy: jest.SpiedFunction<typeof window.addEventListener>;
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    let removeEventListenerSpy: jest.SpiedFunction<typeof window.removeEventListener>;

    beforeEach(() => {
        // Reset all mocks
        jest.clearAllMocks();
        mockQueuePush.mockClear();
        mockQueueSize.mockClear();
        mockQueueFlush.mockClear();
        mockQueuePause.mockClear();
        mockQueueResume.mockClear();
        mockConsoleLog.mockClear();
        mockConsoleWarn.mockClear();

        // Reset mock implementations
        mockQueueSize.mockReturnValue(0);

        // Configure smartQueue mock to return our mock queue
        const mockQueue = {
            push: mockQueuePush,
            size: mockQueueSize,
            flush: mockQueueFlush,
            pause: mockQueuePause,
            resume: mockQueueResume
        };
        (smartQueue as jest.MockedFunction<typeof smartQueue>).mockReturnValue(mockQueue as any);

        // Setup window event listener spies
        addEventListenerSpy = jest.spyOn(window, 'addEventListener');
        removeEventListenerSpy = jest.spyOn(window, 'removeEventListener');

        // Setup test data
        mockConfig = {
            server: 'https://example.com',
            debug: false,
            autoPageView: true,
            siteAuth: 'test-site-key'
        };

        mockContext = {
            site_auth: 'test-site-key',
            session_id: 'test-session-id',
            user_id: 'test-user-id',
            device: {
                screen_resolution: '1920x1080',
                language: 'en-US',
                viewport_width: '1024',
                viewport_height: '768'
            }
        };

        mockEvent = {
            event_type: 'pageview',
            local_time: Date.now().toString(),
            data: {
                page: {
                    url: 'https://example.com/page',
                    doc_encoding: 'UTF-8',
                    doc_hash: 'test-hash',
                    doc_protocol: 'https',
                    doc_search: 'test-search',
                    doc_host: 'example.com',
                    doc_path: '/page',
                    title: 'Test Page'
                }
            }
        };
    });

    afterAll(() => {
        // Restore console methods
        mockConsoleLog.mockRestore();
        mockConsoleWarn.mockRestore();
    });

    describe('Factory Function', () => {
        it('should create a queue manager with all methods', () => {
            const queue = createAnalyticsQueue(mockConfig);

            expect(queue).toBeDefined();
            expect(queue.initialize).toBeDefined();
            expect(queue.enqueue).toBeDefined();
            expect(queue.size).toBeDefined();
            expect(queue.cleanup).toBeDefined();
        });

        it('should merge custom config with defaults', () => {
            const customConfig = {
                ...mockConfig,
                queue: {
                    eventBatchSize: 10,
                    flushInterval: 3000
                }
            };

            const queue = createAnalyticsQueue(customConfig);
            queue.initialize();

            expect(smartQueue).toHaveBeenCalledWith(
                expect.any(Function),
                expect.objectContaining({
                    max: 10,
                    interval: 3000,
                    throttle: false
                })
            );
        });

        it('should use default config when queue is true', () => {
            const configWithTrueQueue = {
                ...mockConfig,
                queue: true
            };

            const queue = createAnalyticsQueue(configWithTrueQueue);
            queue.initialize();

            expect(smartQueue).toHaveBeenCalledWith(
                expect.any(Function),
                expect.objectContaining({
                    max: DEFAULT_QUEUE_CONFIG.eventBatchSize,
                    interval: DEFAULT_QUEUE_CONFIG.flushInterval,
                    throttle: false
                })
            );
        });
    });

    describe('initialize', () => {
        it('should initialize smartQueue with correct options', () => {
            const queue = createAnalyticsQueue(mockConfig);
            queue.initialize();

            expect(smartQueue).toHaveBeenCalledTimes(1);
            expect(smartQueue).toHaveBeenCalledWith(
                expect.any(Function),
                expect.objectContaining({
                    max: DEFAULT_QUEUE_CONFIG.eventBatchSize,
                    interval: DEFAULT_QUEUE_CONFIG.flushInterval,
                    throttle: false
                })
            );
        });

        it('should setup page visibility and unload event listeners', () => {
            const documentSpy = jest.spyOn(document, 'addEventListener');
            const queue = createAnalyticsQueue(mockConfig);
            queue.initialize();

            expect(documentSpy).toHaveBeenCalledWith('visibilitychange', expect.any(Function));
            expect(addEventListenerSpy).toHaveBeenCalledWith('pagehide', expect.any(Function));

            documentSpy.mockRestore();
        });
    });

    describe('enqueue', () => {
        it('should push event to queue', () => {
            const queue = createAnalyticsQueue(mockConfig);
            queue.initialize();

            queue.enqueue(mockEvent, mockContext);

            expect(mockQueuePush).toHaveBeenCalledTimes(1);
            expect(mockQueuePush).toHaveBeenCalledWith(mockEvent);
        });

        it('should update current context', () => {
            const queue = createAnalyticsQueue(mockConfig);
            queue.initialize();

            const newContext = { ...mockContext, session_id: 'new-session-id' };
            queue.enqueue(mockEvent, newContext);

            // Context should be used in the next sendBatch call
            expect(mockQueuePush).toHaveBeenCalledWith(mockEvent);
        });

        it('should not push if queue is not initialized', () => {
            const queue = createAnalyticsQueue(mockConfig);
            // Don't call initialize

            queue.enqueue(mockEvent, mockContext);

            expect(mockQueuePush).not.toHaveBeenCalled();
        });

        it('should log debug info when debug is enabled', () => {
            const debugConfig = { ...mockConfig, debug: true };
            const queue = createAnalyticsQueue(debugConfig);
            queue.initialize();

            mockQueueSize.mockReturnValue(1);
            queue.enqueue(mockEvent, mockContext);

            expect(mockConsoleLog).toHaveBeenCalledWith(
                expect.stringContaining('DotCMS Analytics Queue: Event added'),
                expect.objectContaining({
                    eventType: 'pageview',
                    event: mockEvent
                })
            );
        });

        it('should not log debug info when debug is disabled', () => {
            const queue = createAnalyticsQueue(mockConfig);
            queue.initialize();

            queue.enqueue(mockEvent, mockContext);

            expect(mockConsoleLog).not.toHaveBeenCalled();
        });

        it('should show correct queue size in debug log', () => {
            const debugConfig = { ...mockConfig, debug: true };
            const queue = createAnalyticsQueue(debugConfig);
            queue.initialize();

            // Mock size to return 4 (so predicted size will be 5)
            mockQueueSize.mockReturnValue(4);

            queue.enqueue(mockEvent, mockContext);

            expect(mockConsoleLog).toHaveBeenCalledWith(
                expect.stringContaining(`Queue size: 5/${DEFAULT_QUEUE_CONFIG.eventBatchSize}`),
                expect.any(Object)
            );
        });
    });

    describe('size', () => {
        it('should return queue size', () => {
            const queue = createAnalyticsQueue(mockConfig);
            queue.initialize();

            mockQueueSize.mockReturnValue(5);

            expect(queue.size()).toBe(5);
            expect(mockQueueSize).toHaveBeenCalledTimes(1);
        });

        it('should return 0 if queue is not initialized', () => {
            const queue = createAnalyticsQueue(mockConfig);
            // Don't call initialize

            expect(queue.size()).toBe(0);
        });

        it('should return 0 if queue is null', () => {
            const queue = createAnalyticsQueue(mockConfig);
            queue.initialize();
            queue.cleanup(); // This sets queue to null

            expect(queue.size()).toBe(0);
        });
    });

    describe('sendBatch', () => {
        it('should send events with keepalive=false by default', () => {
            const queue = createAnalyticsQueue(mockConfig);
            queue.initialize();

            // Get the callback function passed to smartQueue
            const mockedSmartQueue = smartQueue as jest.MockedFunction<typeof smartQueue>;
            const sendBatchCallback = mockedSmartQueue.mock.calls[0][0];

            // Enqueue to set context
            queue.enqueue(mockEvent, mockContext);

            // Call the callback with events (simulating smartQueue calling it)
            const events = [mockEvent, mockEvent];
            sendBatchCallback(events, []);

            expect(sendAnalyticsEvent).toHaveBeenCalledTimes(1);
            expect(sendAnalyticsEvent).toHaveBeenCalledWith(
                {
                    context: mockContext,
                    events
                },
                mockConfig,
                false // Default keepalive
            );
        });

        it('should not send if context is not set', () => {
            const queue = createAnalyticsQueue(mockConfig);
            queue.initialize();

            const mockedSmartQueue = smartQueue as jest.MockedFunction<typeof smartQueue>;
            const sendBatchCallback = mockedSmartQueue.mock.calls[0][0];

            // Don't enqueue anything (no context set)
            sendBatchCallback([mockEvent], []);

            expect(sendAnalyticsEvent).not.toHaveBeenCalled();
        });

        it('should log debug info when debug is enabled', () => {
            const debugConfig = { ...mockConfig, debug: true };
            const queue = createAnalyticsQueue(debugConfig);
            queue.initialize();

            const mockedSmartQueue = smartQueue as jest.MockedFunction<typeof smartQueue>;
            const sendBatchCallback = mockedSmartQueue.mock.calls[0][0];

            queue.enqueue(mockEvent, mockContext);
            sendBatchCallback([mockEvent, mockEvent], []);

            expect(mockConsoleLog).toHaveBeenCalledWith(
                expect.stringContaining('Sending batch of 2 event(s)'),
                expect.objectContaining({
                    keepalive: false,
                    events: expect.any(Array)
                })
            );
        });
    });

    describe('flushRemaining', () => {
        it('should flush all events when page becomes hidden', () => {
            const documentSpy = jest.spyOn(document, 'addEventListener');
            const queue = createAnalyticsQueue(mockConfig);
            queue.initialize();

            // Enqueue to set context
            queue.enqueue(mockEvent, mockContext);

            // Mock queue has events
            mockQueueSize.mockReturnValue(3);

            // Mock document.visibilityState
            Object.defineProperty(document, 'visibilityState', {
                writable: true,
                configurable: true,
                value: 'hidden'
            });

            // Get the visibilitychange listener
            const visibilityListener = documentSpy.mock.calls.find(
                (call) => call[0] === 'visibilitychange'
            )?.[1] as EventListener;

            // Trigger visibilitychange
            visibilityListener(new Event('visibilitychange'));

            expect(mockQueueFlush).toHaveBeenCalledWith(true);

            documentSpy.mockRestore();
        });

        it('should use keepalive=true when flushing on page unload', () => {
            const queue = createAnalyticsQueue(mockConfig);
            queue.initialize();

            const mockedSmartQueue = smartQueue as jest.MockedFunction<typeof smartQueue>;
            const sendBatchCallback = mockedSmartQueue.mock.calls[0][0];

            queue.enqueue(mockEvent, mockContext);
            mockQueueSize.mockReturnValue(2);

            // Trigger pagehide
            const pagehideListener = addEventListenerSpy.mock.calls.find(
                (call) => call[0] === 'pagehide'
            )?.[1] as EventListener;
            pagehideListener(new Event('pagehide'));

            // Now simulate smartQueue calling sendBatch
            sendBatchCallback([mockEvent], []);

            expect(sendAnalyticsEvent).toHaveBeenCalledWith(
                expect.any(Object),
                mockConfig,
                true // Should use keepalive for page unload
            );
        });

        it('should not flush if queue is empty', () => {
            const queue = createAnalyticsQueue(mockConfig);
            queue.initialize();

            mockQueueSize.mockReturnValue(0);

            const pagehideListener = addEventListenerSpy.mock.calls.find(
                (call) => call[0] === 'pagehide'
            )?.[1] as EventListener;
            pagehideListener(new Event('pagehide'));

            expect(mockQueueFlush).not.toHaveBeenCalled();
        });

        it('should not flush if context is not set', () => {
            const queue = createAnalyticsQueue(mockConfig);
            queue.initialize();

            mockQueueSize.mockReturnValue(3);

            // Don't enqueue anything (no context set)
            const pagehideListener = addEventListenerSpy.mock.calls.find(
                (call) => call[0] === 'pagehide'
            )?.[1] as EventListener;
            pagehideListener(new Event('pagehide'));

            expect(mockQueueFlush).not.toHaveBeenCalled();
        });

        it('should log debug info when debug is enabled', () => {
            const debugConfig = { ...mockConfig, debug: true };
            const queue = createAnalyticsQueue(debugConfig);
            queue.initialize();

            queue.enqueue(mockEvent, mockContext);
            mockQueueSize.mockReturnValue(5);

            const pagehideListener = addEventListenerSpy.mock.calls.find(
                (call) => call[0] === 'pagehide'
            )?.[1] as EventListener;
            pagehideListener(new Event('pagehide'));

            expect(mockConsoleWarn).toHaveBeenCalledWith(
                expect.stringContaining('Flushing 5 events')
            );
        });

        it('should handle pagehide event', () => {
            const queue = createAnalyticsQueue(mockConfig);
            queue.initialize();

            queue.enqueue(mockEvent, mockContext);
            mockQueueSize.mockReturnValue(2);

            // Get the pagehide listener
            const pagehideListener = addEventListenerSpy.mock.calls.find(
                (call) => call[0] === 'pagehide'
            )?.[1] as EventListener;

            pagehideListener(new Event('pagehide'));

            expect(mockQueueFlush).toHaveBeenCalledWith(true);
        });
    });

    describe('cleanup', () => {
        it('should flush remaining events', () => {
            const queue = createAnalyticsQueue(mockConfig);
            queue.initialize();

            queue.enqueue(mockEvent, mockContext);
            mockQueueSize.mockReturnValue(3);

            queue.cleanup();

            expect(mockQueueFlush).toHaveBeenCalledWith(true);
        });

        it('should reset internal state', () => {
            const queue = createAnalyticsQueue(mockConfig);
            queue.initialize();

            queue.enqueue(mockEvent, mockContext);
            queue.cleanup();

            // After cleanup, size should return 0 (queue is null)
            expect(queue.size()).toBe(0);
        });

        it('should not throw if queue is not initialized', () => {
            const queue = createAnalyticsQueue(mockConfig);

            expect(() => queue.cleanup()).not.toThrow();
        });

        it('should reset keepalive flag', () => {
            const queue = createAnalyticsQueue(mockConfig);
            queue.initialize();

            const mockedSmartQueue = smartQueue as jest.MockedFunction<typeof smartQueue>;
            const sendBatchCallback = mockedSmartQueue.mock.calls[0][0];

            // Enqueue and trigger flush (sets useKeepalive = true)
            queue.enqueue(mockEvent, mockContext);
            mockQueueSize.mockReturnValue(1);

            const pagehideListener = addEventListenerSpy.mock.calls.find(
                (call) => call[0] === 'pagehide'
            )?.[1] as EventListener;
            pagehideListener(new Event('pagehide'));

            // Cleanup
            queue.cleanup();

            // Re-initialize
            queue.initialize();
            queue.enqueue(mockEvent, mockContext);

            // Should use keepalive=false again after cleanup
            sendBatchCallback([mockEvent], []);

            expect(sendAnalyticsEvent).toHaveBeenCalledWith(
                expect.any(Object),
                mockConfig,
                false // Should use keepalive=false after cleanup
            );
        });
    });

    describe('Debug Logging', () => {
        it('should show keepalive mode in sendBatch debug log', () => {
            const debugConfig = { ...mockConfig, debug: true };
            const queue = createAnalyticsQueue(debugConfig);
            queue.initialize();

            const mockedSmartQueue = smartQueue as jest.MockedFunction<typeof smartQueue>;
            const sendBatchCallback = mockedSmartQueue.mock.calls[0][0];

            queue.enqueue(mockEvent, mockContext);
            sendBatchCallback([mockEvent], []);

            expect(mockConsoleLog).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({
                    keepalive: false
                })
            );
        });

        it('should show keepalive=true in debug log during page unload', () => {
            const debugConfig = { ...mockConfig, debug: true };
            const queue = createAnalyticsQueue(debugConfig);
            queue.initialize();

            const mockedSmartQueue = smartQueue as jest.MockedFunction<typeof smartQueue>;
            const sendBatchCallback = mockedSmartQueue.mock.calls[0][0];

            queue.enqueue(mockEvent, mockContext);
            mockQueueSize.mockReturnValue(1);

            // Trigger pagehide
            const pagehideListener = addEventListenerSpy.mock.calls.find(
                (call) => call[0] === 'pagehide'
            )?.[1] as EventListener;
            pagehideListener(new Event('pagehide'));

            // Simulate smartQueue calling sendBatch
            sendBatchCallback([mockEvent], []);

            // Find the sendBatch call (second console.log call)
            const sendBatchCall = mockConsoleLog.mock.calls.find((call) =>
                call[0].includes('Sending batch')
            );

            expect(sendBatchCall).toBeDefined();
            expect(sendBatchCall?.[1]).toEqual(
                expect.objectContaining({
                    keepalive: true
                })
            );
        });
    });

    describe('Edge Cases', () => {
        it('should handle multiple enqueues with different contexts', () => {
            const queue = createAnalyticsQueue(mockConfig);
            queue.initialize();

            const context1 = { ...mockContext, session_id: 'session-1' };
            const context2 = { ...mockContext, session_id: 'session-2' };

            queue.enqueue(mockEvent, context1);
            queue.enqueue(mockEvent, context2);

            expect(mockQueuePush).toHaveBeenCalledTimes(2);
        });

        it('should work with custom queue config', () => {
            const customConfig = {
                ...mockConfig,
                queue: {
                    eventBatchSize: 50,
                    flushInterval: 15000
                }
            };

            const queue = createAnalyticsQueue(customConfig);
            queue.initialize();

            expect(smartQueue).toHaveBeenCalledWith(
                expect.any(Function),
                expect.objectContaining({
                    max: 50,
                    interval: 15000
                })
            );
        });
    });
});
