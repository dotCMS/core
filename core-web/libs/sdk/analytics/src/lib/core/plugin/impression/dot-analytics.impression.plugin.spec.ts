/* eslint-disable @typescript-eslint/no-explicit-any */

import { AnalyticsInstance } from 'analytics';

import {
    DotCMSImpressionTracker,
    ImpressionSubscription
} from './dot-analytics.impression-tracker';
import { dotAnalyticsImpressionPlugin } from './dot-analytics.impression.plugin';

import { IMPRESSION_EVENT_TYPE } from '../../shared/constants/dot-analytics.constants';
import { DotCMSAnalyticsConfig } from '../../shared/models';

// Mock the tracker
jest.mock('./dot-analytics.impression-tracker');

describe('dotAnalyticsImpressionPlugin', () => {
    let mockConfig: DotCMSAnalyticsConfig;
    let mockAnalyticsInstance: AnalyticsInstance;
    let mockTracker: jest.Mocked<DotCMSImpressionTracker>;
    let mockSubscription: jest.Mocked<ImpressionSubscription>;

    beforeEach(() => {
        jest.clearAllMocks();

        // Mock config
        mockConfig = {
            server: 'https://test.com',
            siteAuth: 'test-key',
            debug: false,
            impressions: true
        };

        // Mock analytics instance
        mockAnalyticsInstance = {
            track: jest.fn()
        } as any;

        // Mock subscription
        mockSubscription = {
            unsubscribe: jest.fn()
        };

        // Mock tracker instance
        mockTracker = {
            initialize: jest.fn(),
            cleanup: jest.fn(),
            onImpression: jest.fn().mockReturnValue(mockSubscription)
        } as any;

        // Mock tracker constructor
        (DotCMSImpressionTracker as jest.Mock).mockImplementation(() => mockTracker);
    });

    describe('Plugin Configuration', () => {
        it('should have correct plugin name', () => {
            const plugin = dotAnalyticsImpressionPlugin(mockConfig);

            expect(plugin.name).toBe('dot-analytics-impression');
        });

        it('should expose initialize and loaded methods', () => {
            const plugin = dotAnalyticsImpressionPlugin(mockConfig);

            expect(plugin).toHaveProperty('initialize');
            expect(plugin).toHaveProperty('loaded');
            expect(typeof plugin.initialize).toBe('function');
            expect(typeof plugin.loaded).toBe('function');
        });
    });

    describe('Initialization', () => {
        it('should initialize tracker when impressions is enabled', async () => {
            const plugin = dotAnalyticsImpressionPlugin(mockConfig);

            await plugin.initialize({ instance: mockAnalyticsInstance });

            expect(DotCMSImpressionTracker).toHaveBeenCalledWith(mockConfig);
            expect(mockTracker.initialize).toHaveBeenCalled();
        });

        it('should NOT initialize when impressions is false', async () => {
            mockConfig.impressions = false;
            const plugin = dotAnalyticsImpressionPlugin(mockConfig);

            await plugin.initialize({ instance: mockAnalyticsInstance });

            expect(DotCMSImpressionTracker).not.toHaveBeenCalled();
            expect(mockTracker.initialize).not.toHaveBeenCalled();
        });

        it('should NOT initialize when impressions is undefined', async () => {
            mockConfig.impressions = undefined;
            const plugin = dotAnalyticsImpressionPlugin(mockConfig);

            await plugin.initialize({ instance: mockAnalyticsInstance });

            expect(DotCMSImpressionTracker).not.toHaveBeenCalled();
            expect(mockTracker.initialize).not.toHaveBeenCalled();
        });

        it('should initialize with custom impression config object', async () => {
            const customConfig: DotCMSAnalyticsConfig = {
                ...mockConfig,
                impressions: {
                    dwellMs: 5000,
                    visibilityThreshold: 0.75
                }
            };

            const plugin = dotAnalyticsImpressionPlugin(customConfig);

            await plugin.initialize({ instance: mockAnalyticsInstance });

            expect(DotCMSImpressionTracker).toHaveBeenCalledWith(customConfig);
            expect(mockTracker.initialize).toHaveBeenCalled();
        });

        it('should subscribe to impression events', async () => {
            const plugin = dotAnalyticsImpressionPlugin(mockConfig);

            await plugin.initialize({ instance: mockAnalyticsInstance });

            expect(mockTracker.onImpression).toHaveBeenCalledWith(expect.any(Function));
        });

        it('should call instance.track() when impression fires', async () => {
            const plugin = dotAnalyticsImpressionPlugin(mockConfig);

            await plugin.initialize({ instance: mockAnalyticsInstance });

            // Get the callback passed to onImpression
            const impressionCallback = mockTracker.onImpression.mock.calls[0][0];

            // Simulate impression event
            const impressionPayload = {
                content: {
                    identifier: 'content-123',
                    inode: 'inode-456',
                    title: 'Test Content',
                    content_type: 'Blog'
                },
                position: {
                    viewport_offset_pct: 50,
                    dom_index: 0
                }
            };

            impressionCallback(IMPRESSION_EVENT_TYPE, impressionPayload);

            // Verify analytics.track was called
            expect(mockAnalyticsInstance.track).toHaveBeenCalledWith(
                IMPRESSION_EVENT_TYPE,
                impressionPayload
            );
        });

        it('should log debug message when impressions enabled in debug mode', async () => {
            const consoleInfoSpy = jest.spyOn(console, 'info').mockImplementation();
            mockConfig.debug = true;

            const plugin = dotAnalyticsImpressionPlugin(mockConfig);

            await plugin.initialize({ instance: mockAnalyticsInstance });

            expect(consoleInfoSpy).toHaveBeenCalledWith(
                '[DotCMS Analytics | Impression] [INFO]',
                'Impression tracking plugin initialized'
            );

            consoleInfoSpy.mockRestore();
        });

        it('should log debug message when impressions disabled in debug mode', async () => {
            const consoleInfoSpy = jest.spyOn(console, 'info').mockImplementation();
            mockConfig.debug = true;
            mockConfig.impressions = false;

            const plugin = dotAnalyticsImpressionPlugin(mockConfig);

            await plugin.initialize({ instance: mockAnalyticsInstance });

            expect(consoleInfoSpy).toHaveBeenCalledWith(
                '[DotCMS Analytics | Impression] [INFO]',
                'Impression tracking disabled (config.impressions not set)'
            );

            consoleInfoSpy.mockRestore();
        });

        it('should return resolved promise', async () => {
            const plugin = dotAnalyticsImpressionPlugin(mockConfig);

            const result = await plugin.initialize({ instance: mockAnalyticsInstance });

            expect(result).toBeUndefined();
        });
    });

    describe('Loaded Hook', () => {
        it('should setup cleanup handlers on beforeunload', async () => {
            const addEventListenerSpy = jest.spyOn(window, 'addEventListener');

            const plugin = dotAnalyticsImpressionPlugin(mockConfig);
            await plugin.initialize({ instance: mockAnalyticsInstance }); // Initialize first
            plugin.loaded();

            expect(addEventListenerSpy).toHaveBeenCalledWith('beforeunload', expect.any(Function));

            addEventListenerSpy.mockRestore();
        });

        it('should setup cleanup handlers on pagehide', async () => {
            const addEventListenerSpy = jest.spyOn(window, 'addEventListener');

            const plugin = dotAnalyticsImpressionPlugin(mockConfig);
            await plugin.initialize({ instance: mockAnalyticsInstance }); // Initialize first
            plugin.loaded();

            expect(addEventListenerSpy).toHaveBeenCalledWith('pagehide', expect.any(Function));

            addEventListenerSpy.mockRestore();
        });

        it('should return true when loaded', () => {
            const plugin = dotAnalyticsImpressionPlugin(mockConfig);

            const result = plugin.loaded();

            expect(result).toBe(true);
        });

        it('should not setup handlers if window is undefined (SSR)', () => {
            const originalWindow = global.window;
            delete (global as any).window;

            const plugin = dotAnalyticsImpressionPlugin(mockConfig);
            const result = plugin.loaded();

            expect(result).toBe(true);

            // Restore
            (global as any).window = originalWindow;
        });
    });

    describe('Cleanup', () => {
        it('should unsubscribe and cleanup tracker on page unload', async () => {
            let unloadCallback: (() => void) | undefined;
            const addEventListenerSpy = jest
                .spyOn(window, 'addEventListener')
                .mockImplementation((event, handler) => {
                    if (event === 'beforeunload') {
                        unloadCallback = handler as () => void;
                    }
                });

            const plugin = dotAnalyticsImpressionPlugin(mockConfig);

            // Initialize to create tracker
            await plugin.initialize({ instance: mockAnalyticsInstance });

            // Setup cleanup handlers
            plugin.loaded();

            // Simulate page unload
            expect(unloadCallback).toBeDefined();
            // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
            unloadCallback!();

            // Verify cleanup
            expect(mockSubscription.unsubscribe).toHaveBeenCalled();
            expect(mockTracker.cleanup).toHaveBeenCalled();

            addEventListenerSpy.mockRestore();
        });

        it('should handle cleanup when tracker is not initialized', async () => {
            let unloadCallback: (() => void) | undefined;
            const addEventListenerSpy = jest
                .spyOn(window, 'addEventListener')
                .mockImplementation((event, handler) => {
                    if (event === 'beforeunload') {
                        unloadCallback = handler as () => void;
                    }
                });

            mockConfig.impressions = false;
            const plugin = dotAnalyticsImpressionPlugin(mockConfig);

            // Initialize (but tracker won't be created)
            await plugin.initialize({ instance: mockAnalyticsInstance });

            // Setup cleanup handlers
            plugin.loaded();

            // When tracker is not initialized, no callback is registered
            // This is expected behavior - no cleanup needed if no tracker
            expect(unloadCallback).toBeUndefined();

            addEventListenerSpy.mockRestore();
        });

        it('should log debug message on cleanup in debug mode', async () => {
            let unloadCallback: (() => void) | undefined;
            const addEventListenerSpy = jest
                .spyOn(window, 'addEventListener')
                .mockImplementation((event, handler) => {
                    if (event === 'beforeunload') {
                        unloadCallback = handler as () => void;
                    }
                });

            const consoleInfoSpy = jest.spyOn(console, 'info').mockImplementation();
            mockConfig.debug = true;

            const plugin = dotAnalyticsImpressionPlugin(mockConfig);

            await plugin.initialize({ instance: mockAnalyticsInstance });
            plugin.loaded();

            // Simulate page unload
            // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
            unloadCallback!();

            expect(consoleInfoSpy).toHaveBeenCalledWith(
                '[DotCMS Analytics | Impression] [INFO]',
                'Impression tracking cleaned up on page unload'
            );

            addEventListenerSpy.mockRestore();
            consoleInfoSpy.mockRestore();
        });

        it('should register cleanup handlers for both beforeunload and pagehide events', async () => {
            const eventHandlers: { [key: string]: (() => void)[] } = {};
            const addEventListenerSpy = jest
                .spyOn(window, 'addEventListener')
                .mockImplementation((event: string, handler) => {
                    const eventName = event as string;
                    if (!eventHandlers[eventName]) {
                        eventHandlers[eventName] = [];
                    }
                    eventHandlers[eventName].push(handler as () => void);
                });

            const plugin = dotAnalyticsImpressionPlugin(mockConfig);

            await plugin.initialize({ instance: mockAnalyticsInstance });
            plugin.loaded();

            // Verify both events are registered with the same cleanup function
            expect(eventHandlers['beforeunload']).toBeDefined();
            expect(eventHandlers['pagehide']).toBeDefined();
            expect(eventHandlers['beforeunload'].length).toBe(1);
            expect(eventHandlers['pagehide'].length).toBe(1);

            // Trigger beforeunload
            eventHandlers['beforeunload'][0]();
            expect(mockTracker.cleanup).toHaveBeenCalledTimes(1);
            expect(mockSubscription.unsubscribe).toHaveBeenCalledTimes(1);

            // Trigger pagehide - cleanup is idempotent (tracker already null)
            // So cleanup() won't be called again, but the handler runs
            eventHandlers['pagehide'][0]();
            // Still only 1 because impressionTracker becomes null after first cleanup
            expect(mockTracker.cleanup).toHaveBeenCalledTimes(1);
            expect(mockSubscription.unsubscribe).toHaveBeenCalledTimes(1);

            addEventListenerSpy.mockRestore();
        });
    });

    describe('Integration Flow', () => {
        it('should complete full lifecycle: initialize -> impression -> cleanup', async () => {
            let unloadCallback: (() => void) | undefined;
            const addEventListenerSpy = jest
                .spyOn(window, 'addEventListener')
                .mockImplementation((event, handler) => {
                    if (event === 'beforeunload') {
                        unloadCallback = handler as () => void;
                    }
                });

            const plugin = dotAnalyticsImpressionPlugin(mockConfig);

            // Step 1: Initialize
            await plugin.initialize({ instance: mockAnalyticsInstance });
            expect(mockTracker.initialize).toHaveBeenCalled();
            expect(mockTracker.onImpression).toHaveBeenCalled();

            // Step 2: Setup cleanup
            plugin.loaded();

            // Step 3: Simulate impression
            const impressionCallback = mockTracker.onImpression.mock.calls[0][0];
            impressionCallback(IMPRESSION_EVENT_TYPE, {
                content: {
                    identifier: 'test-123',
                    inode: 'inode',
                    title: 'Test',
                    content_type: 'Blog'
                },
                position: { viewport_offset_pct: 50, dom_index: 0 }
            });

            expect(mockAnalyticsInstance.track).toHaveBeenCalled();

            // Step 4: Cleanup
            // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
            unloadCallback!();
            expect(mockSubscription.unsubscribe).toHaveBeenCalled();
            expect(mockTracker.cleanup).toHaveBeenCalled();

            addEventListenerSpy.mockRestore();
        });
    });
});
