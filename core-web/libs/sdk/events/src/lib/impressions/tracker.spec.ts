/* eslint-disable @typescript-eslint/no-explicit-any */

import { vi } from 'vitest';

import { DEFAULT_IMPRESSION_CONFIG, IMPRESSION_EVENT_TYPE } from './constants';
import { DotCMSImpressionTracker } from './tracker';

import {
    CONTENTLET_CLASS,
    CONTENTLET_IDENTIFIER_ATTRIBUTE,
    CONTENTLET_RESCAN_EVENT
} from '../contentlets/constants';
import { INITIAL_SCAN_DELAY_MS } from '../contentlets/utils';
import { isBrowser } from '../pipeline/utils';

import type { PipelineConfig } from '../pipeline/models';
import type { Mock } from 'vitest';

// Mock dependencies
vi.mock('../pipeline/utils', async () => ({
    ...(await vi.importActual('../pipeline/utils')),
    isBrowser: vi.fn(() => true)
}));

vi.mock('./utils', async () => ({
    ...(await vi.importActual('./utils')),
    createDebounce: vi.fn((callback) => callback) // Execute immediately for testing
}));

describe('DotCMSImpressionTracker', () => {
    let tracker: DotCMSImpressionTracker;
    let mockConfig: PipelineConfig;
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
        element.dataset['dotIdentifier'] = identifier;
        element.dataset['dotInode'] = options.inode || 'inode-123';
        element.dataset['dotType'] = options.contentType || 'Blog';
        element.dataset['dotTitle'] = options.title || 'Test Content';
        element.dataset['dotBasetype'] = options.baseType || 'CONTENT';

        // Mock getBoundingClientRect
        element.getBoundingClientRect = vi.fn(() => ({
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
        // Reset mocks. clearAllMocks keeps return values, so a test that turns isBrowser off
        // must not leave it off for the next one
        vi.clearAllMocks();
        (isBrowser as Mock).mockReturnValue(true);
        vi.useFakeTimers();

        // Setup config
        mockConfig = {
            server: 'https://test.com',
            siteAuth: 'test-key',
            debug: false,
            impressions: true
        };

        // Mock IntersectionObserver
        mockIntersectionObserver = {
            observe: vi.fn(),
            unobserve: vi.fn(),
            disconnect: vi.fn()
        };

        // A function expression, not an arrow: the tracker calls
        // `new IntersectionObserver(...)`, and an arrow is not constructible.
        (global as any).IntersectionObserver = vi.fn(function (callback) {
            intersectionCallback = callback;

            return mockIntersectionObserver;
        });

        // Mock MutationObserver
        mockMutationObserver = {
            observe: vi.fn(),
            disconnect: vi.fn()
        };

        (global as any).MutationObserver = vi.fn(function (callback) {
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
        vi.useRealTimers();
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
            (isBrowser as Mock).mockReturnValue(false);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();

            expect(global.IntersectionObserver).not.toHaveBeenCalled();
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
            const customConfig: PipelineConfig = {
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

        it('should clamp a visibility threshold outside 0 to 1, which IntersectionObserver rejects', () => {
            tracker = new DotCMSImpressionTracker({
                ...mockConfig,
                impressions: { visibilityThreshold: 50 }
            });
            tracker.initialize();

            expect(global.IntersectionObserver).toHaveBeenLastCalledWith(
                expect.any(Function),
                expect.objectContaining({ threshold: 1 })
            );

            tracker.cleanup();
            tracker = new DotCMSImpressionTracker({
                ...mockConfig,
                impressions: { visibilityThreshold: -0.5 }
            });
            tracker.initialize();

            expect(global.IntersectionObserver).toHaveBeenLastCalledWith(
                expect.any(Function),
                expect.objectContaining({ threshold: 0 })
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
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

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
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

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
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            expect(mockIntersectionObserver.observe).not.toHaveBeenCalled();
        });

        it('should skip hidden elements (visibility: hidden)', () => {
            const element = createMockContentletElement('content-123');
            element.style.visibility = 'hidden';
            document.body.appendChild(element);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            expect(mockIntersectionObserver.observe).not.toHaveBeenCalled();
        });

        it('should skip transparent elements (opacity: 0)', () => {
            const element = createMockContentletElement('content-123');
            element.style.opacity = '0';
            document.body.appendChild(element);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            expect(mockIntersectionObserver.observe).not.toHaveBeenCalled();
        });

        it('should skip display:none elements', () => {
            const element = createMockContentletElement('content-123');
            element.style.display = 'none';
            document.body.appendChild(element);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            expect(mockIntersectionObserver.observe).not.toHaveBeenCalled();
        });

        it('should respect maxNodes limit', () => {
            const customConfig: PipelineConfig = {
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
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

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
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            expect(mockIntersectionObserver.observe).not.toHaveBeenCalled();
        });
    });

    describe('Dwell Timer Logic', () => {
        it('should start dwell timer when element becomes visible', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            // Simulate intersection (element becomes visible)
            const entry = {
                target: element,
                isIntersecting: true,
                intersectionRatio: 1
            } as unknown as IntersectionObserverEntry;

            intersectionCallback([entry], mockIntersectionObserver);

            // Verify timer was started (we're using fake timers)
            expect(vi.getTimerCount()).toBeGreaterThan(0);
        });

        it('should fire impression after dwellMs timeout', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            const callback = vi.fn();
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
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
            vi.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

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

            const callback = vi.fn();
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            tracker.onImpression(callback);

            // Element becomes visible
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );

            // Wait half the dwell time
            vi.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs / 2);

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
            vi.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

            // Impression should NOT fire
            expect(callback).not.toHaveBeenCalled();
        });

        it('should NOT fire if element is hidden when timer expires', () => {
            const element = createMockContentletElement('content-123', { visible: true });
            document.body.appendChild(element);

            const callback = vi.fn();
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            tracker.onImpression(callback);

            // Element becomes visible
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );

            // Make element not meet visibility threshold
            element.getBoundingClientRect = vi.fn(() => ({
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
            vi.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

            // Impression should NOT fire
            expect(callback).not.toHaveBeenCalled();
        });

        it('should NOT start timer if already tracked', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            const callback = vi.fn();
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            tracker.onImpression(callback);

            // First visibility
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );
            vi.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

            expect(callback).toHaveBeenCalledTimes(1);

            // Second visibility (should not fire again)
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );
            vi.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

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
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            const timerCountBefore = vi.getTimerCount();

            // Try to start tracking
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );

            // No NEW timers should be started (timer count should not increase)
            expect(vi.getTimerCount()).toBe(timerCountBefore);
        });
    });

    describe('Session Tracking', () => {
        it('should track impression only once per session', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            const callback = vi.fn();
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            tracker.onImpression(callback);

            // Fire first impression
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );
            vi.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

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
            vi.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

            // Should NOT fire again
            expect(callback).toHaveBeenCalledTimes(1);
        });

        it('should unobserve element after tracking impression', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            // Fire impression
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );
            vi.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

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

            const callback = vi.fn();
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            tracker.onImpression(callback);

            const timerCountBeforeElements = vi.getTimerCount();

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
            expect(vi.getTimerCount()).toBe(timerCountBeforeElements + 2);

            // Page becomes hidden
            Object.defineProperty(document, 'visibilityState', {
                writable: true,
                value: 'hidden'
            });
            document.dispatchEvent(new Event('visibilitychange'));

            // Dwell timers should be cleared (back to initial count)
            expect(vi.getTimerCount()).toBe(timerCountBeforeElements);

            // Fast-forward time - impressions should NOT fire
            vi.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);
            expect(callback).not.toHaveBeenCalled();
        });

        it('should ignore intersection events when page is hidden', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            const timerCountBeforeHidden = vi.getTimerCount();

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
            expect(vi.getTimerCount()).toBe(timerCountBeforeHidden);
        });
    });

    describe('SPA Navigation Detection', () => {
        it('should clear element states on navigation', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

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
            const timerCountWithActive = vi.getTimerCount();
            expect(timerCountWithActive).toBeGreaterThanOrEqual(1); // At least navigation interval

            // Simulate SPA navigation (tracker listens to pushState and checks pathname)
            history.pushState({}, '', '/new-page');

            // Trigger navigation check via interval
            vi.advanceTimersByTime(1000);

            // Dwell timer should be cancelled after navigation
            expect(vi.getTimerCount()).toBeLessThan(timerCountWithActive);
        });

        it('should clear session tracking on navigation', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            const callback = vi.fn();
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.onImpression(callback);
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            // Fire first impression on initial page
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );
            vi.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);
            expect(callback).toHaveBeenCalledTimes(1);

            // Simulate SPA navigation (tracker listens to pushState and checks pathname)
            history.pushState({}, '', '/new-page');

            // Advance timers to trigger interval check
            vi.advanceTimersByTime(1000);

            // Session tracking should be cleared after navigation
            expect(callback).toHaveBeenCalledTimes(1); // Still only 1 from before navigation
        });

        it('should reset on a navigation through the Navigation API, without patching history', () => {
            const globalScope = window as unknown as Record<string, unknown>;
            const navigation = new EventTarget();
            globalScope['navigation'] = navigation;
            const { pushState, replaceState } = history;

            try {
                const element = createMockContentletElement('content-123');
                document.body.appendChild(element);
                const callback = vi.fn();
                tracker = new DotCMSImpressionTracker(mockConfig);
                tracker.onImpression(callback);
                tracker.initialize();
                vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

                expect(history.pushState).toBe(pushState);
                expect(history.replaceState).toBe(replaceState);

                const seen = () => {
                    intersectionCallback(
                        [
                            {
                                target: element,
                                isIntersecting: true
                            } as unknown as IntersectionObserverEntry
                        ],
                        mockIntersectionObserver
                    );
                    vi.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);
                };

                seen();
                expect(callback).toHaveBeenCalledTimes(1);

                // The next page shows the same contentlet: it counts again there
                history.pushState({}, '', '/next-page');
                navigation.dispatchEvent(new Event('currententrychange'));
                window.dispatchEvent(new Event(CONTENTLET_RESCAN_EVENT));
                seen();

                expect(callback).toHaveBeenCalledTimes(2);
            } finally {
                tracker.cleanup();
                delete globalScope['navigation'];
                history.replaceState({}, '', '/');
            }
        });

        it('should stop watching navigations on cleanup', () => {
            const globalScope = window as unknown as Record<string, unknown>;
            const navigation = new EventTarget();
            const removeEventListener = vi.spyOn(navigation, 'removeEventListener');
            globalScope['navigation'] = navigation;

            try {
                tracker = new DotCMSImpressionTracker(mockConfig);
                tracker.initialize();
                tracker.cleanup();

                expect(removeEventListener).toHaveBeenCalledWith(
                    'currententrychange',
                    expect.any(Function)
                );
            } finally {
                delete globalScope['navigation'];
            }
        });
    });

    describe('Back/forward cache restore', () => {
        const pageshow = (persisted: boolean) =>
            window.dispatchEvent(Object.assign(new Event('pageshow'), { persisted }));

        const setUp = () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);
            const callback = vi.fn();
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.onImpression(callback);
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            const seen = () => {
                intersectionCallback(
                    [
                        {
                            target: element,
                            isIntersecting: true
                        } as unknown as IntersectionObserverEntry
                    ],
                    mockIntersectionObserver
                );
                vi.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);
            };

            return { element, callback, seen };
        };

        afterEach(() => {
            tracker.cleanup();
        });

        it('should count the contentlets in view again when the browser restores the page', () => {
            const { element, callback, seen } = setUp();
            seen();
            expect(callback).toHaveBeenCalledTimes(1);
            mockIntersectionObserver.observe.mockClear();

            pageshow(true);

            // The restored DOM is the one left: observed again from scratch, the observer
            // reports what is in view, as after a load
            expect(mockIntersectionObserver.disconnect).toHaveBeenCalled();
            expect(mockIntersectionObserver.observe).toHaveBeenCalledWith(element);
            seen();
            expect(callback).toHaveBeenCalledTimes(2);
        });

        it('should count nothing again when a page that loads is shown', () => {
            const { callback, seen } = setUp();
            seen();

            pageshow(false);
            seen();

            expect(callback).toHaveBeenCalledTimes(1);
        });

        it('should stop watching restores on cleanup', () => {
            const removeEventListener = vi.spyOn(window, 'removeEventListener');
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();

            tracker.cleanup();

            expect(removeEventListener).toHaveBeenCalledWith('pageshow', expect.any(Function));
            removeEventListener.mockRestore();
        });
    });

    describe('Subscription Pattern', () => {
        it('should notify all subscribers when impression fires', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            const callback1 = vi.fn();
            const callback2 = vi.fn();

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            tracker.onImpression(callback1);
            tracker.onImpression(callback2);

            // Fire impression
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );
            vi.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

            expect(callback1).toHaveBeenCalledTimes(1);
            expect(callback2).toHaveBeenCalledTimes(1);
        });

        it('should allow unsubscribe', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            const callback = vi.fn();

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            const subscription = tracker.onImpression(callback);

            // Unsubscribe before impression
            subscription.unsubscribe();

            // Fire impression
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );
            vi.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

            // Callback should NOT be called
            expect(callback).not.toHaveBeenCalled();
        });

        it('should handle subscriber errors gracefully', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            const errorCallback = vi.fn(() => {
                throw new Error('Subscriber error');
            });
            const validCallback = vi.fn();

            tracker = new DotCMSImpressionTracker({ ...mockConfig, debug: true });
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            tracker.onImpression(errorCallback);
            tracker.onImpression(validCallback);

            const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);

            // Fire impression
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );
            vi.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

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

            const callback = vi.fn();
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            tracker.onImpression(callback);

            // Fire impression
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );
            vi.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

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
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            const timerCountBeforeElements = vi.getTimerCount(); // Just the interval

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
            expect(vi.getTimerCount()).toBe(timerCountBeforeElements + 2);

            tracker.cleanup();

            // Dwell timers should be cleared, only interval remains (not cleaned up)
            // Note: The interval timer from navigation check is not cleaned up in current implementation
            expect(vi.getTimerCount()).toBe(timerCountBeforeElements);
        });

        it('should clear all subscribers on cleanup', () => {
            const element = createMockContentletElement('content-123');
            document.body.appendChild(element);

            const callback = vi.fn();
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            tracker.onImpression(callback);

            tracker.cleanup();

            // Re-initialize and fire impression
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);
            intersectionCallback(
                [{ target: element, isIntersecting: true } as unknown as IntersectionObserverEntry],
                mockIntersectionObserver
            );
            vi.advanceTimersByTime(DEFAULT_IMPRESSION_CONFIG.dwellMs);

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
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

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

        it('should watch the identifier attribute, which the renderers print after hydration', () => {
            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();

            expect(mockMutationObserver.observe).toHaveBeenCalledWith(
                document.body,
                expect.objectContaining({
                    attributes: true,
                    attributeFilter: [CONTENTLET_IDENTIFIER_ATTRIBUTE]
                })
            );
        });

        it('should observe a contentlet once its identifier arrives', () => {
            const element = createMockContentletElement('content-123');
            delete element.dataset['dotIdentifier'];
            document.body.appendChild(element);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            // The initial scan found the contentlet without an identifier
            expect(mockIntersectionObserver.observe).not.toHaveBeenCalled();

            element.dataset['dotIdentifier'] = 'content-123';
            const mutations = [
                {
                    type: 'attributes',
                    target: element,
                    attributeName: CONTENTLET_IDENTIFIER_ATTRIBUTE,
                    addedNodes: [],
                    removedNodes: []
                }
            ] as unknown as MutationRecord[];
            mutationCallback(mutations, mockMutationObserver);

            expect(mockIntersectionObserver.observe).toHaveBeenCalledWith(element);
        });

        it('should observe hidden contentlets when a rescan is requested after they show', () => {
            const element = createMockContentletElement('content-123');
            element.style.visibility = 'hidden';
            document.body.appendChild(element);

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            vi.advanceTimersByTime(INITIAL_SCAN_DELAY_MS);

            // Skipped while hidden, as an experiment's rows are until it decides
            expect(mockIntersectionObserver.observe).not.toHaveBeenCalled();

            element.style.visibility = '';
            window.dispatchEvent(new CustomEvent(CONTENTLET_RESCAN_EVENT));

            expect(mockIntersectionObserver.observe).toHaveBeenCalledWith(element);
        });

        it('should remove its rescan listener on cleanup', () => {
            const addSpy = vi.spyOn(window, 'addEventListener');
            const removeSpy = vi.spyOn(window, 'removeEventListener');

            tracker = new DotCMSImpressionTracker(mockConfig);
            tracker.initialize();
            const added = addSpy.mock.calls.find(([type]) => type === CONTENTLET_RESCAN_EVENT);
            tracker.cleanup();

            expect(added).toBeDefined();
            expect(removeSpy).toHaveBeenCalledWith(CONTENTLET_RESCAN_EVENT, added![1]);

            addSpy.mockRestore();
            removeSpy.mockRestore();
        });
    });

    describe('Debug Mode', () => {
        it('should log debug information when enabled', () => {
            const consoleInfoSpy = vi.spyOn(console, 'info').mockImplementation(() => undefined);

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
