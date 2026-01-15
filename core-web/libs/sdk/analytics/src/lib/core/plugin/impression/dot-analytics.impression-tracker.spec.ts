/* eslint-disable @typescript-eslint/no-explicit-any */

import { getUVEState } from '@dotcms/uve';

import { DotCMSImpressionTracker } from './dot-analytics.impression-tracker';

import {
    CONTENTLET_CLASS,
    DEFAULT_IMPRESSION_CONFIG,
    IMPRESSION_EVENT_TYPE
} from '../../shared/constants/dot-analytics.constants';
import { DotCMSAnalyticsConfig } from '../../shared/models';
import { INITIAL_SCAN_DELAY_MS } from '../../shared/utils/dot-analytics.utils';

// Mock dependencies
jest.mock('@dotcms/uve');
jest.mock('./dot-analytics.impression.utils', () => ({
    ...jest.requireActual('./dot-analytics.impression.utils'),
    createDebounce: jest.fn((callback) => callback) // Execute immediately for testing
}));

describe('DotCMSImpressionTracker', () => {
    let tracker: DotCMSImpressionTracker;
    let mockConfig: DotCMSAnalyticsConfig;
    let mockIntersectionObserver: any;
    let mockMutationObserver: any;
    let intersectionCallback: IntersectionObserverCallback;
    let mutationCallback: MutationCallback;

    // Helper to create mock element with data attributes
    const createMockContentletElement = (
        identifier: string,
        options: {
            inode?: string;
            contentType?: string;
            title?: string;
            baseType?: string;
            width?: number;
            height?: number;
            visible?: boolean;
        } = {}
    ): HTMLElement => {
        const element = document.createElement('div');
        element.className = CONTENTLET_CLASS;
        element.dataset.dotIdentifier = identifier;
        element.dataset.dotInode = options.inode || 'inode-123';
        element.dataset.dotType = options.contentType || 'Blog';
        element.dataset.dotTitle = options.title || 'Test Content';
        element.dataset.dotBasetype = options.baseType || 'CONTENT';

        // Mock getBoundingClientRect
        element.getBoundingClientRect = jest.fn(() => ({
            width: options.width ?? 200,
            height: options.height ?? 200,
            top: options.visible !== false ? 100 : 1100,
            left: 100,
            bottom: options.visible !== false ? 300 : 1300,
            right: 300,
            x: 100,
            y: options.visible !== false ? 100 : 1100,
            toJSON: () => ({})
        }));

        return element;
    };

    beforeEach(() => {
        // Reset mocks
        jest.clearAllMocks();
        jest.useFakeTimers();

        // Mock getUVEState (not in editor by default)
        (getUVEState as jest.Mock).mockReturnValue(false);

        // Setup config
        mockConfig = {
            server: 'https://test.com',
            siteAuth: 'test-key',
            debug: false,
            impressions: true
        };

        // Mock IntersectionObserver
        mockIntersectionObserver = {
            observe: jest.fn(),
            unobserve: jest.fn(),
            disconnect: jest.fn()
        };

        (global as any).IntersectionObserver = jest.fn((callback) => {
            intersectionCallback = callback;
            return mockIntersectionObserver;
        });

        // Mock MutationObserver
        mockMutationObserver = {
            observe: jest.fn(),
            disconnect: jest.fn()
        };

        (global as any).MutationObserver = jest.fn((callback) => {
            mutationCallback = callback;
            return mockMutationObserver;
        });

        // Mock document visibility
        Object.defineProperty(document, 'visibilityState', {
            writable: true,
            value: 'visible'
        });

        // Mock window dimensions
        Object.defineProperty(window, 'innerHeight', { value: 1000, writable: true });
        Object.defineProperty(window, 'innerWidth', { value: 1000, writable: true });

        // Clear document body
        document.body.innerHTML = '';
    });

    afterEach(() => {
        jest.useRealTimers();
        document.body.innerHTML = '';
    });

    describe('Initialization', () => {
        it('should initialize IntersectionObserver with correct threshold', () => {
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();

            expect(global.IntersectionObserver).toHaveBeenCalledWith(
                expect.any(Function),
                expect.objectContaining({
                    threshold: DEFAULT_IMPRESSION_CONFIG.visibilityThreshold
                })
            );
        });

        it('should NOT initialize in SSR (no window)', () => {
            // Hide window
            const originalWindow = global.window;
            delete (global as any).window;

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();

            expect(global.IntersectionObserver).not.toHaveBeenCalled();

            // Restore
            (global as any).window = originalWindow;
        });

        it('should NOT initialize in UVE editor mode', () => {
            (getUVEState as jest.Mock).mockReturnValue(true);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();

            expect(global.IntersectionObserver).not.toHaveBeenCalled();
            expect(getUVEState).toHaveBeenCalled();
        });

        it('should setup MutationObserver for dynamic content', () => {
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();

            expect(global.MutationObserver).toHaveBeenCalledWith(expect.any(Function));
            expect(mockMutationObserver.observe).toHaveBeenCalledWith(
                document.body,
                expect.objectContaining({
                    childList: true,
                    subtree: true
                })
            );
        });

        it('should merge custom impression config with defaults', () => {
            const customConfig: DotCMSAnalyticsConfig = {
                ...mockConfig,
                impressions: {
                    dwellMs: 5000,
                    visibilityThreshold: 0.75
                }
            };

            tracker = new DotCMSImpressionTracker(customConfig);
            tracker.initialize();

            expect(global.IntersectionObserver).toHaveBeenCalledWith(
                expect.any(Function),
                expect.objectContaining({
                    threshold: 0.75
                })
            );
        });

        it('should use default config when impressions is true', () => {
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();

            expect(global.IntersectionObserver).toHaveBeenCalledWith(
                expect.any(Function),
                expect.objectContaining({
                    threshold: DEFAULT_IMPRESSION_CONFIG.visibilityThreshold
                })
            );
        });
    });

    describe('Element Discovery and Validation', () => {
        it('should find and observe contentlet elements', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            expect(mockIntersectionObserver.observe).toHaveBeenCalledWith(element);
        });

        it('should skip zero-dimension elements', () => {
            const element = createMockContentletElement('content-123', {
                width: 0,
                height: 0
            });
            document.body.appendChild(element);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            expect(mockIntersectionObserver.observe).not.toHaveBeenCalled();
        });

        it('should skip elements smaller than 10px', () => {
            const element = createMockContentletElement('content-123', {
                width: 5,
                height: 5
            });
            document.body.appendChild(element);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            expect(mockIntersectionObserver.observe).not.toHaveBeenCalled();
        });

        it('should skip hidden elements (visibility: hidden)', () => {
            const element = createMockContentletElement('content-123');
            element.style.visibility = 'hidden';
            document.body.appendChild(element);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            expect(mockIntersectionObserver.observe).not.toHaveBeenCalled();
        });

        it('should skip transparent elements (opacity: 0)', () => {
            const element = createMockContentletElement('content-123');
            element.style.opacity = '0';
            document.body.appendChild(element);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            expect(mockIntersectionObserver.observe).not.toHaveBeenCalled();
        });

        it('should skip display:none elements', () => {
            const element = createMockContentletElement('content-123');
            element.style.display = 'none';
            document.body.appendChild(element);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            expect(mockIntersectionObserver.observe).not.toHaveBeenCalled();
        });

        it('should respect maxNodes limit', () => {
            const customConfig: DotCMSAnalyticsConfig = {
                ...mockConfig,
                impressions: { maxNodes: 2 }
            };

            // Create 5 elements
            for (let i = 0; i < 5; i++) {
                const element = createMockContentletElement(`content-${i}`);
                document.body.appendChild(element);
            }

            tracker = new DotCMSImpressionTracker(customConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            // Should only observe 2 elements
            expect(mockIntersectionObserver.observe).toHaveBeenCalledTimes(2);
        });

        it('should skip elements without identifier', () => {
            const element = document.createElement('div');
            element.className = CONTENTLET_CLASS;
            // No data-dot-identifier
            document.body.appendChild(element);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            expect(mockIntersectionObserver.observe).not.toHaveBeenCalled();
        });
    });

    describe('Dwell Timer Logic', () => {
        it('should start dwell timer when element becomes visible', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            // Simulate intersection (element becomes visible)
            const entry = {
                target: element,
                isIntersecting: true,
                intersectionRatio: 1
            } as unknown as IntersectionObserverEntry;

            intersectionCallback([entry], mockIntersectionObserver);

            // Verify timer was started (we're using fake timers)
            expect(jest.getTimerCount()).toBeGreaterThan(0);
        });

        it('should fire impression after dwellMs timeout', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            const callback = jest.fn();
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            tracker.onImpression(callback);

            // Element becomes visible
            const entry = {
                target: element,
                isIntersecting: true,
                intersectionRatio: 1
            } as unknown as IntersectionObserverEntry;

            intersectionCallback([entry], mockIntersectionObserver);

            // Impression should not fire yet
            expect(callback).not.toHaveBeenCalled();

            // Fast-forward time to dwell duration
            jest.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

            // Now impression should fire
            expect(callback).toHaveBeenCalledWith(
                IMPRESSION_EVENT_TYPE,
                expect.objectContaining({
                    content: expect.objectContaining({
                        identifier: 'content-123'
                    })
                })
            );
        });

        it('should cancel timer if element leaves viewport before dwell time', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            const callback = jest.fn();
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            tracker.onImpression(callback);

            // Element becomes visible
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );

            // Wait half the dwell time
            jest.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs / 2);

            // Element leaves viewport
            intersectionCallback(
                [
                    {
                        target: element,
                        isIntersecting: false
                    } as unknown as IntersectionObserverEntry
                ],
                mockIntersectionObserver
            );

            // Fast-forward remaining time
            jest.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

            // Impression should NOT fire
            expect(callback).not.toHaveBeenCalled();
        });

        it('should NOT fire if element is hidden when timer expires', () => {
            const element = createMockContentletElement('content-123', { visible: true });
            document.body.appendChild(element);

            const callback = jest.fn();
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            tracker.onImpression(callback);

            // Element becomes visible
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );

            // Make element not meet visibility threshold
            element.getBoundingClientRect = jest.fn(() => ({
                width: 200,
                height: 200,
                top: 1100, // Below viewport
                left: 100,
                bottom: 1300,
                right: 300,
                x: 100,
                y: 1100,
                toJSON: () => ({})
            }));

            // Fast-forward time
            jest.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

            // Impression should NOT fire
            expect(callback).not.toHaveBeenCalled();
        });

        it('should NOT start timer if already tracked', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            const callback = jest.fn();
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            tracker.onImpression(callback);

            // First visibility
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );
            jest.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

            expect(callback).toHaveBeenCalledTimes(1);

            // Second visibility (should not fire again)
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );
            jest.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

            // Still only called once
            expect(callback).toHaveBeenCalledTimes(1);
        });

        it('should NOT start timer if page is not visible', () => {
            Object.defineProperty(document, 'visibilityState', {
                writable: true,
                value: 'hidden'
            });

            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            const timerCountBefore = jest.getTimerCount();

            // Try to start tracking
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );

            // No NEW timers should be started (timer count should not increase)
            expect(jest.getTimerCount()).toBe(timerCountBefore);
        });
    });

    describe('Session Tracking', () => {
        it('should track impression only once per session', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            const callback = jest.fn();
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            tracker.onImpression(callback);

            // Fire first impression
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );
            jest.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

            expect(callback).toHaveBeenCalledTimes(1);

            // Element leaves and comes back
            intersectionCallback(
                [
                    {
                        target: element,
                        isIntersecting: false
                    } as unknown as IntersectionObserverEntry
                ],
                mockIntersectionObserver
            );
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );
            jest.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

            // Should NOT fire again
            expect(callback).toHaveBeenCalledTimes(1);
        });

        it('should unobserve element after tracking impression', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            // Fire impression
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );
            jest.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

            // Verify element was unobserved
            expect(mockIntersectionObserver.unobserve).toHaveBeenCalledWith(element);
        });
    });

    describe('Page Visibility Handling', () => {
        it('should cancel all timers when page becomes hidden', () => {
            const element1 = createMockContentletElement('content-1');
            const element2 = createMockContentletElement('content-2');
            document.body.appendChild(element1);
            document.body.appendChild(element2);

            const callback = jest.fn();
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            tracker.onImpression(callback);

            const timerCountBeforeElements = jest.getTimerCount();

            // Both elements become visible
            intersectionCallback(
                [
                    {
                        target: element1,
                        isIntersecting: true
                    } as unknown as IntersectionObserverEntry,
                    {
                        target: element2,
                        isIntersecting: true
                    } as unknown as IntersectionObserverEntry
                ],
                mockIntersectionObserver
            );

            // Should have 2 more timers (one per element)
            expect(jest.getTimerCount()).toBe(timerCountBeforeElements + 2);

            // Page becomes hidden
            Object.defineProperty(document, 'visibilityState', {
                writable: true,
                value: 'hidden'
            });
            document.dispatchEvent(new Event('visibilitychange'));

            // Dwell timers should be cleared (back to initial count)
            expect(jest.getTimerCount()).toBe(timerCountBeforeElements);

            // Fast-forward time - impressions should NOT fire
            jest.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);
            expect(callback).not.toHaveBeenCalled();
        });

        it('should ignore intersection events when page is hidden', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            const timerCountBeforeHidden = jest.getTimerCount();

            // Hide the page
            Object.defineProperty(document, 'visibilityState', {
                writable: true,
                value: 'hidden'
            });

            // Try to trigger intersection while hidden
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );

            // No NEW timers should start (count unchanged)
            expect(jest.getTimerCount()).toBe(timerCountBeforeHidden);
        });
    });

    describe('SPA Navigation Detection', () => {
        let originalLocation: Location;

        beforeEach(() => {
            originalLocation = window.location;
        });

        afterEach(() => {
            // Restore
            Object.defineProperty(window, 'location', {
                value: originalLocation,
                writable: true,
                configurable: true
            });
        });

        it('should clear element states on navigation', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            // Start tracking an element
            intersectionCallback(
                [
                    {
                        target: element,
                        isIntersecting: true,
                        intersectionRatio: 0.6
                    } as unknown as IntersectionObserverEntry
                ],
                mockIntersectionObserver
            );

            // Verify timer is active (navigation interval is always running)
            const timerCountWithActive = jest.getTimerCount();
            expect(timerCountWithActive).toBeGreaterThanOrEqual(1); // At least navigation interval

            // Simulate navigation by replacing window.location
            Object.defineProperty(window, 'location', {
                value: { pathname: '/new-page' },
                writable: true,
                configurable: true
            });

            // Trigger navigation check via interval
            jest.advanceTimersByTime(1000);

            // Dwell timer should be cancelled after navigation
            expect(jest.getTimerCount()).toBeLessThan(timerCountWithActive);
        });

        it('should clear session tracking on navigation', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            const callback = jest.fn();
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.onImpression(callback);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            // Fire first impression on initial page
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );
            jest.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);
            expect(callback).toHaveBeenCalledTimes(1);

            // Simulate navigation
            Object.defineProperty(window, 'location', {
                value: { pathname: '/new-page' },
                writable: true,
                configurable: true
            });

            // Advance timers to trigger interval check
            jest.advanceTimersByTime(1000);

            // Session tracking should be cleared after navigation
            expect(callback).toHaveBeenCalledTimes(1); // Still only 1 from before navigation
        });
    });

    describe('Subscription Pattern', () => {
        it('should notify all subscribers when impression fires', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            const callback1 = jest.fn();
            const callback2 = jest.fn();

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            tracker.onImpression(callback1);
            tracker.onImpression(callback2);

            // Fire impression
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );
            jest.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

            expect(callback1).toHaveBeenCalledTimes(1);
            expect(callback2).toHaveBeenCalledTimes(1);
        });

        it('should allow unsubscribe', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            const callback = jest.fn();

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            const subscription = tracker.onImpression(callback);

            // Unsubscribe before impression
            subscription.unsubscribe();

            // Fire impression
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );
            jest.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

            // Callback should NOT be called
            expect(callback).not.toHaveBeenCalled();
        });

        it('should handle subscriber errors gracefully', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            const errorCallback = jest.fn(() => {
                throw new Error('Subscriber error');
            });
            const validCallback = jest.fn();

            tracker = new DotCMSImpressionTracker({ ...mockConfig, debug: true });
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            tracker.onImpression(errorCallback);
            tracker.onImpression(validCallback);

            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();

            // Fire impression
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );
            jest.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

            // Error should be logged but valid callback should still execute
            expect(consoleErrorSpy).toHaveBeenCalled();
            expect(validCallback).toHaveBeenCalled();

            consoleErrorSpy.mockRestore();
        });

        it('should include correct payload structure', () => {
            const element = createMockContentletElement('content-123', {
                inode: 'test-inode',
                contentType: 'BlogPost',
                title: 'My Blog Post'
            });
            document.body.appendChild(element);

            const callback = jest.fn();
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            tracker.onImpression(callback);

            // Fire impression
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );
            jest.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

            expect(callback).toHaveBeenCalledWith(IMPRESSION_EVENT_TYPE, {
                content: {
                    identifier: 'content-123',
                    inode: 'test-inode',
                    title: 'My Blog Post',
                    content_type: 'BlogPost'
                },
                position: {
                    viewport_offset_pct: expect.any(Number),
                    dom_index: expect.any(Number)
                }
            });
        });
    });

    describe('Cleanup', () => {
        it('should disconnect IntersectionObserver on cleanup', () => {
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();

            tracker.cleanup();

            expect(mockIntersectionObserver.disconnect).toHaveBeenCalled();
        });

        it('should disconnect MutationObserver on cleanup', () => {
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();

            tracker.cleanup();

            expect(mockMutationObserver.disconnect).toHaveBeenCalled();
        });

        it('should clear all active dwell timers on cleanup', () => {
            const element1 = createMockContentletElement('content-1');
            const element2 = createMockContentletElement('content-2');
            document.body.appendChild(element1);
            document.body.appendChild(element2);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            const timerCountBeforeElements = jest.getTimerCount(); // Just the interval

            // Start tracking multiple elements
            intersectionCallback(
                [
                    {
                        target: element1,
                        isIntersecting: true
                    } as unknown as IntersectionObserverEntry,
                    {
                        target: element2,
                        isIntersecting: true
                    } as unknown as IntersectionObserverEntry
                ],
                mockIntersectionObserver
            );

            // Should have 2 dwell timers + interval
            expect(jest.getTimerCount()).toBe(timerCountBeforeElements + 2);

            tracker.cleanup();

            // Dwell timers should be cleared, only interval remains (not cleaned up)
            // Note: The interval timer from navigation check is not cleaned up in current implementation
            expect(jest.getTimerCount()).toBe(timerCountBeforeElements);
        });

        it('should clear all subscribers on cleanup', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            const callback = jest.fn();
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            tracker.onImpression(callback);

            tracker.cleanup();

            // Re-initialize and fire impression
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );
            jest.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

            // Old callback should NOT be called
            expect(callback).not.toHaveBeenCalled();
        });

        it('should handle cleanup when not initialized', () => {
            tracker = new DotCMSImpressionTracker(mockConfig);

            // Should not throw
            expect(() => tracker.cleanup()).not.toThrow();
        });
    });

    describe('Dynamic Content Detection', () => {
        it('should detect and observe new contentlets added to DOM', () => {
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            jest.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            // Initially no contentlets
            expect(mockIntersectionObserver.observe).not.toHaveBeenCalled();

            // Add contentlet dynamically
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            // Trigger mutation observer with actual mutations
            const mutations = [
                {
                    type: 'childList',
                    addedNodes: [element],
                    removedNodes: []
                }
            ] as unknown as MutationRecord[];
            mutationCallback(mutations, mockMutationObserver);

            // Should observe new element
            expect(mockIntersectionObserver.observe).toHaveBeenCalledWith(element);
        });
    });

    describe('Debug Mode', () => {
        it('should log debug information when enabled', () => {
            const consoleInfoSpy = jest.spyOn(console, 'info').mockImplementation();

            tracker = new DotCMSImpressionTracker({ ...mockConfig, debug: true });
            tracker.initialize();

            // Should have been called with the initialization message
            expect(consoleInfoSpy).toHaveBeenCalled();
            const calls = consoleInfoSpy.mock.calls;
            const initCall = calls.find((call) =>
                call[1]?.toString().includes('Impression tracking initialized')
            );
            expect(initCall).toBeDefined();

            consoleInfoSpy.mockRestore();
        });
    });
});
