/* eslint-disable @typescript-eslint/no-explicit-any */

import { Mock, vi } from 'vitest';

import { DotCMSClickTracker } from './dot-analytics.click-tracker';
import * as clickUtils from './dot-analytics.click.utils';

import { ANALYTICS_CONTENTLET_CLASS } from '../../shared/constants';
import { DotCMSAnalyticsConfig } from '../../shared/models';
import * as sharedUtils from '../../shared/utils/dot-analytics.utils';

// Mock dependencies
vi.mock('./dot-analytics.click.utils', () => {
    const actual = jest.requireActual('./dot-analytics.click.utils') as Record<string, unknown>;
    return {
        ...actual,
        handleContentletClick: vi.fn()
    };
});
vi.mock('../../shared/utils/dot-analytics.utils', () => {
    const actual = jest.requireActual('../../shared/utils/dot-analytics.utils') as Record<
        string,
        unknown
    >;
    return {
        ...actual,
        createPluginLogger: vi.fn(() => ({
            debug: vi.fn(),
            info: vi.fn(),
            warn: vi.fn(),
            error: vi.fn(),
            log: vi.fn()
        })),
        findContentlets: vi.fn(() => []),
        createContentletObserver: vi.fn(),
        isBrowser: vi.fn(() => true)
    };
});

describe('DotCMSClickTracker', () => {
    let tracker: DotCMSClickTracker;
    let mockConfig: DotCMSAnalyticsConfig;
    let mockCallback: Mock;

    const createMockContentletElement = (identifier: string): HTMLElement => {
        const element = document.createElement('div');
        element.className = ANALYTICS_CONTENTLET_CLASS;
        element.dataset.dotAnalyticsIdentifier = identifier;
        element.dataset.dotAnalyticsInode = 'inode-123';
        element.dataset.dotAnalyticsContenttype = 'Blog';
        element.dataset.dotAnalyticsTitle = 'Test Content';
        element.dataset.dotAnalyticsBasetype = 'CONTENT';

        // Mock addEventListener to track calls
        element.addEventListener = vi.fn(element.addEventListener.bind(element));
        element.removeEventListener = vi.fn(element.removeEventListener.bind(element));

        return element;
    };

    beforeEach(() => {
        vi.clearAllMocks();
        vi.useFakeTimers();

        mockConfig = {
            server: 'https://test.com',
            siteAuth: 'test-key',
            debug: false
        };

        mockCallback = vi.fn();

        // Reset isBrowser to return true by default
        (sharedUtils.isBrowser as Mock).mockReturnValue(true);
        (sharedUtils.findContentlets as Mock).mockReturnValue([]);
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    describe('Constructor', () => {
        it('should create tracker instance with logger', () => {
            tracker = new DotCMSClickTracker(mockConfig);

            expect(sharedUtils.createPluginLogger).toHaveBeenCalledWith('Click', mockConfig);
        });
    });

    describe('onClick()', () => {
        it('should add callback to subscribers', () => {
            tracker = new DotCMSClickTracker(mockConfig);
            const subscription = tracker.onClick(mockCallback);

            expect((tracker as any).subscribers.size).toBe(1);
            expect((tracker as any).subscribers.has(mockCallback)).toBe(true);

            // Cleanup
            subscription.unsubscribe();
        });

        it('should return subscription with unsubscribe method', () => {
            tracker = new DotCMSClickTracker(mockConfig);
            const subscription = tracker.onClick(mockCallback);

            expect(subscription).toHaveProperty('unsubscribe');
            expect(typeof subscription.unsubscribe).toBe('function');
        });

        it('should remove callback when unsubscribe is called', () => {
            tracker = new DotCMSClickTracker(mockConfig);
            const subscription = tracker.onClick(mockCallback);

            expect((tracker as any).subscribers.has(mockCallback)).toBe(true);

            subscription.unsubscribe();

            expect((tracker as any).subscribers.has(mockCallback)).toBe(false);
            expect((tracker as any).subscribers.size).toBe(0);
        });
    });

    describe('initialize()', () => {
        it('should skip initialization if not in browser environment', () => {
            (sharedUtils.isBrowser as Mock).mockReturnValue(false);

            tracker = new DotCMSClickTracker(mockConfig);
            const logger = (tracker as any).logger;

            tracker.initialize();

            expect(logger.warn).toHaveBeenCalledWith('No document, skipping');
            expect(sharedUtils.findContentlets).not.toHaveBeenCalled();
        });

        it('should run initial scan after delay', () => {
            const mockElement = createMockContentletElement('test-123');
            (sharedUtils.findContentlets as Mock).mockReturnValue([mockElement]);

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.initialize();

            // Before timeout
            expect(sharedUtils.findContentlets).not.toHaveBeenCalled();

            // After timeout
            vi.advanceTimersByTime(100);
            expect(sharedUtils.findContentlets).toHaveBeenCalled();
        });

        it('should initialize MutationObserver', () => {
            tracker = new DotCMSClickTracker(mockConfig);
            tracker.initialize();

            expect(sharedUtils.createContentletObserver).toHaveBeenCalled();
        });

        it('should log initialization', () => {
            tracker = new DotCMSClickTracker(mockConfig);
            const logger = (tracker as any).logger;

            tracker.initialize();

            expect(logger.debug).toHaveBeenCalledWith('Plugin initializing');
            expect(logger.info).toHaveBeenCalledWith('Plugin initialized');
        });
    });

    describe('attachClickListener()', () => {
        it('should attach click listener to new contentlet', () => {
            const mockElement = createMockContentletElement('test-123');
            (sharedUtils.findContentlets as Mock).mockReturnValue([mockElement]);

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.initialize();

            // Trigger initial scan
            vi.advanceTimersByTime(100);

            expect(mockElement.addEventListener).toHaveBeenCalledWith(
                'click',
                expect.any(Function)
            );
        });

        it('should NOT attach duplicate listener to same element', () => {
            const mockElement = createMockContentletElement('test-123');
            (sharedUtils.findContentlets as Mock).mockReturnValue([mockElement]);

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.initialize();

            // Trigger initial scan
            vi.advanceTimersByTime(100);
            expect(mockElement.addEventListener).toHaveBeenCalledTimes(1);

            // Clear and trigger second scan
            (mockElement.addEventListener as Mock).mockClear();
            (tracker as any).findAndAttachListeners();

            // Should NOT attach again
            expect(mockElement.addEventListener).not.toHaveBeenCalled();
        });

        it('should track element in WeakSet after attaching', () => {
            const mockElement = createMockContentletElement('test-123');
            (sharedUtils.findContentlets as Mock).mockReturnValue([mockElement]);

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.initialize();

            // Trigger initial scan
            vi.advanceTimersByTime(100);

            const trackedElements = (tracker as any).trackedElements as WeakSet<HTMLElement>;
            expect(trackedElements.has(mockElement)).toBe(true);
        });

        it('should store handler in WeakMap for cleanup', () => {
            const mockElement = createMockContentletElement('test-123');
            (sharedUtils.findContentlets as Mock).mockReturnValue([mockElement]);

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.initialize();

            // Trigger initial scan
            vi.advanceTimersByTime(100);

            const elementHandlers = (tracker as any).elementHandlers as WeakMap<
                HTMLElement,
                (event: MouseEvent) => void
            >;
            expect(elementHandlers.has(mockElement)).toBe(true);
            expect(elementHandlers.get(mockElement)).toBeInstanceOf(Function);
        });
    });

    describe('Click Handler & Subscription', () => {
        it('should call handleContentletClick when contentlet is clicked', () => {
            const mockElement = createMockContentletElement('test-123');
            (sharedUtils.findContentlets as Mock).mockReturnValue([mockElement]);

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.initialize();

            // Trigger initial scan
            vi.advanceTimersByTime(100);

            // Get the click handler that was attached
            const clickHandler = (mockElement.addEventListener as Mock).mock.calls.find(
                (call) => call[0] === 'click'
            )?.[1];

            expect(clickHandler).toBeDefined();

            // Simulate click
            const mockEvent = new MouseEvent('click');
            clickHandler(mockEvent);

            expect(clickUtils.handleContentletClick).toHaveBeenCalledWith(
                mockEvent,
                mockElement,
                expect.any(Function),
                expect.any(Object) // logger
            );
        });

        it('should notify subscribers when click is valid', () => {
            const mockElement = createMockContentletElement('test-123');
            (sharedUtils.findContentlets as Mock).mockReturnValue([mockElement]);

            // Mock handleContentletClick to call the callback with valid payload structure
            const mockPayload = {
                content: {
                    identifier: 'test-123',
                    inode: 'inode-123',
                    title: 'Test',
                    content_type: 'Blog'
                },
                element: { attributes: [] }
            };
            (clickUtils.handleContentletClick as Mock).mockImplementation(
                (event, element, callback) => {
                    callback('content.click', mockPayload);
                }
            );

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.onClick(mockCallback);
            tracker.initialize();

            // Trigger initial scan
            vi.advanceTimersByTime(100);

            // Get and trigger click handler
            const clickHandler = (mockElement.addEventListener as Mock).mock.calls.find(
                (call) => call[0] === 'click'
            )?.[1];
            clickHandler(new MouseEvent('click'));

            expect(mockCallback).toHaveBeenCalledWith('content.click', mockPayload);
        });

        it('should apply throttling to prevent duplicate clicks', () => {
            const mockElement = createMockContentletElement('test-123');
            (sharedUtils.findContentlets as Mock).mockReturnValue([mockElement]);

            const mockPayload = {
                content: {
                    identifier: 'test-123',
                    inode: 'inode-123',
                    title: 'Test',
                    content_type: 'Blog'
                },
                element: { attributes: [] }
            };
            (clickUtils.handleContentletClick as Mock).mockImplementation(
                (event, element, callback) => {
                    callback('content.click', mockPayload);
                }
            );

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.onClick(mockCallback);
            tracker.initialize();

            vi.advanceTimersByTime(100);

            const clickHandler = (mockElement.addEventListener as Mock).mock.calls[0][1];

            // First click
            clickHandler(new MouseEvent('click'));
            expect(mockCallback).toHaveBeenCalledTimes(1);

            // Second click immediately (should be throttled)
            clickHandler(new MouseEvent('click'));
            expect(mockCallback).toHaveBeenCalledTimes(1); // Still 1

            // Advance past throttle
            vi.advanceTimersByTime(500);

            // Third click should work
            clickHandler(new MouseEvent('click'));
            expect(mockCallback).toHaveBeenCalledTimes(2);
        });

        it('should NOT notify unsubscribed callbacks', () => {
            const mockElement = createMockContentletElement('test-123');
            (sharedUtils.findContentlets as Mock).mockReturnValue([mockElement]);

            const mockPayload = {
                content: {
                    identifier: 'test-123',
                    inode: 'inode-123',
                    title: 'Test',
                    content_type: 'Blog'
                },
                element: { attributes: [] }
            };
            (clickUtils.handleContentletClick as Mock).mockImplementation(
                (event, element, callback) => {
                    callback('content.click', mockPayload);
                }
            );

            tracker = new DotCMSClickTracker(mockConfig);
            const subscription = tracker.onClick(mockCallback);
            tracker.initialize();

            vi.advanceTimersByTime(100);

            const clickHandler = (mockElement.addEventListener as Mock).mock.calls[0][1];

            // Click before unsubscribe
            clickHandler(new MouseEvent('click'));
            expect(mockCallback).toHaveBeenCalledTimes(1);

            // Unsubscribe
            subscription.unsubscribe();

            // Click after unsubscribe should NOT call callback
            vi.advanceTimersByTime(500);
            clickHandler(new MouseEvent('click'));
            expect(mockCallback).toHaveBeenCalledTimes(1); // Still 1
        });
    });

    describe('findAndAttachListeners()', () => {
        it('should attach listeners to all found contentlets', () => {
            const element1 = createMockContentletElement('test-1');
            const element2 = createMockContentletElement('test-2');
            const element3 = createMockContentletElement('test-3');
            (sharedUtils.findContentlets as Mock).mockReturnValue([element1, element2, element3]);

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.initialize();

            // Trigger initial scan
            vi.advanceTimersByTime(100);

            expect(element1.addEventListener).toHaveBeenCalledWith('click', expect.any(Function));
            expect(element2.addEventListener).toHaveBeenCalledWith('click', expect.any(Function));
            expect(element3.addEventListener).toHaveBeenCalledWith('click', expect.any(Function));
        });

        it('should log info when new listeners are attached', () => {
            const mockElement = createMockContentletElement('test-123');
            (sharedUtils.findContentlets as Mock).mockReturnValue([mockElement]);

            tracker = new DotCMSClickTracker(mockConfig);
            const logger = (tracker as any).logger;
            tracker.initialize();

            // Trigger initial scan
            vi.advanceTimersByTime(100);

            expect(logger.info).toHaveBeenCalledWith(
                expect.stringContaining('Attached 1 new click listeners')
            );
        });

        it('should handle empty contentlet list gracefully', () => {
            (sharedUtils.findContentlets as Mock).mockReturnValue([]);

            tracker = new DotCMSClickTracker(mockConfig);
            const logger = (tracker as any).logger;
            tracker.initialize();

            // Trigger initial scan
            vi.advanceTimersByTime(100);

            // Should not log attachment message (0 attached)
            expect(logger.info).not.toHaveBeenCalledWith(expect.stringContaining('Attached'));
        });
    });

    describe('MutationObserver', () => {
        it('should call findAndAttachListeners when new contentlets are added', () => {
            let observerCallback: (() => void) | undefined;
            (sharedUtils.createContentletObserver as Mock).mockImplementation((callback) => {
                observerCallback = callback;
                return {
                    observe: vi.fn(),
                    disconnect: vi.fn()
                };
            });

            const element1 = createMockContentletElement('test-1');
            (sharedUtils.findContentlets as Mock).mockReturnValue([element1]);

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.initialize();

            // Trigger initial scan
            vi.advanceTimersByTime(100);
            expect(element1.addEventListener).toHaveBeenCalledTimes(1);

            // Simulate new contentlet added
            const element2 = createMockContentletElement('test-2');
            (sharedUtils.findContentlets as Mock).mockReturnValue([element1, element2]);
            (element1.addEventListener as Mock).mockClear();

            // Trigger mutation callback
            observerCallback?.();

            // Should only attach to new element (element2)
            expect(element1.addEventListener).not.toHaveBeenCalled();
            expect(element2.addEventListener).toHaveBeenCalledWith('click', expect.any(Function));
        });
    });

    describe('cleanup()', () => {
        it('should remove all click listeners', () => {
            const element1 = createMockContentletElement('test-1');
            const element2 = createMockContentletElement('test-2');
            (sharedUtils.findContentlets as Mock).mockReturnValue([element1, element2]);

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.initialize();

            // Trigger initial scan to attach listeners
            vi.advanceTimersByTime(100);

            // Cleanup
            tracker.cleanup();

            expect(element1.removeEventListener).toHaveBeenCalledWith(
                'click',
                expect.any(Function)
            );
            expect(element2.removeEventListener).toHaveBeenCalledWith(
                'click',
                expect.any(Function)
            );
        });

        it('should disconnect MutationObserver', () => {
            const mockObserver = {
                observe: vi.fn(),
                disconnect: vi.fn()
            };
            (sharedUtils.createContentletObserver as Mock).mockReturnValue(mockObserver);

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.initialize();

            tracker.cleanup();

            expect(mockObserver.disconnect).toHaveBeenCalled();
        });

        it('should clear internal state', () => {
            const mockElement = createMockContentletElement('test-123');
            (sharedUtils.findContentlets as Mock).mockReturnValue([mockElement]);

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.initialize();
            vi.advanceTimersByTime(100);

            tracker.cleanup();

            expect((tracker as any).mutationObserver).toBeNull();
        });

        it('should log cleanup message', () => {
            tracker = new DotCMSClickTracker(mockConfig);
            const logger = (tracker as any).logger;
            tracker.initialize();

            tracker.cleanup();

            expect(logger.info).toHaveBeenCalledWith('Click tracking cleaned up');
        });

        it('should handle cleanup when no observers exist', () => {
            tracker = new DotCMSClickTracker(mockConfig);

            // Should not throw
            expect(() => tracker.cleanup()).not.toThrow();
        });
    });

    describe('Integration - Full Flow', () => {
        it('should handle complete lifecycle: subscribe → init → track clicks → cleanup', () => {
            const mockElement = createMockContentletElement('test-123');
            (sharedUtils.findContentlets as Mock).mockReturnValue([mockElement]);

            // Mock handleContentletClick to invoke callback with valid payload structure
            const mockPayload = {
                content: {
                    identifier: 'test-123',
                    inode: 'inode-123',
                    title: 'Test',
                    content_type: 'Blog'
                },
                element: { attributes: [] }
            };
            (clickUtils.handleContentletClick as Mock).mockImplementation(
                (event, element, callback) => {
                    callback('content.click', mockPayload);
                }
            );

            // Initialize with subscription
            tracker = new DotCMSClickTracker(mockConfig);
            const subscription = tracker.onClick(mockCallback);
            tracker.initialize();
            vi.advanceTimersByTime(100);

            // Verify listener attached
            expect(mockElement.addEventListener).toHaveBeenCalledWith(
                'click',
                expect.any(Function)
            );

            // Simulate click
            const clickHandler = (mockElement.addEventListener as Mock).mock.calls[0][1];
            clickHandler(new MouseEvent('click'));

            // Verify callback was called
            expect(mockCallback).toHaveBeenCalledWith('content.click', mockPayload);

            // Cleanup
            subscription.unsubscribe();
            tracker.cleanup();

            // Verify cleanup
            expect(mockElement.removeEventListener).toHaveBeenCalled();
        });
    });
});
