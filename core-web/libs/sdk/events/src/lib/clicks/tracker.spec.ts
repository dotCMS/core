import { vi } from 'vitest';

import { DotCMSClickTracker } from './tracker';
import * as clickUtils from './utils';

import { CONTENTLET_CLASS } from '../contentlets/constants';
import * as trackingUtils from '../contentlets/utils';
import * as coreUtils from '../pipeline/utils';

import type { PipelineConfig } from '../pipeline/models';
import type { Mock } from 'vitest';

// Mock dependencies
vi.mock('./utils', async () => {
    const actual = (await vi.importActual('./utils')) as Record<string, unknown>;
    return {
        ...actual,
        handleContentletClick: vi.fn()
    };
});
vi.mock('../pipeline/utils', async () => {
    const actual = (await vi.importActual('../pipeline/utils')) as Record<string, unknown>;
    return {
        ...actual,
        createPluginLogger: vi.fn(() => ({
            debug: vi.fn(),
            info: vi.fn(),
            warn: vi.fn(),
            error: vi.fn(),
            log: vi.fn()
        })),
        isBrowser: vi.fn(() => true)
    };
});
vi.mock('../contentlets/utils', async () => {
    const actual = (await vi.importActual('../contentlets/utils')) as Record<string, unknown>;
    return {
        ...actual,
        findContentlets: vi.fn(() => []),
        createContentletObserver: vi.fn()
    };
});

describe('DotCMSClickTracker', () => {
    let tracker: DotCMSClickTracker;
    let mockConfig: PipelineConfig;
    let mockCallback: Mock;

    const createMockContentletElement = (identifier: string): HTMLElement => {
        const element = document.createElement('div');
        element.className = CONTENTLET_CLASS;
        element.dataset['dotIdentifier'] = identifier;
        element.dataset['dotInode'] = 'inode-123';
        element.dataset['dotType'] = 'Blog';
        element.dataset['dotTitle'] = 'Test Content';
        element.dataset['dotBasetype'] = 'CONTENT';

        // Mock addEventListener to track calls
        element.addEventListener = vi.fn(element.addEventListener.bind(element));
        element.removeEventListener = vi.fn(element.removeEventListener.bind(element));

        return element;
    };

    /** The logger the last tracker created, from the mocked createPluginLogger */
    const createdLogger = (): Record<'debug' | 'info' | 'warn' | 'error' | 'log', Mock> => {
        const { results } = (coreUtils.createPluginLogger as Mock).mock;

        return results[results.length - 1]!.value;
    };

    /** Makes the mocked observer keep its callback, so a test can run a scan as a DOM change would */
    const captureObserverCallback = (): { scan: () => void } => {
        const captured = { scan: () => undefined as void };
        (trackingUtils.createContentletObserver as Mock).mockImplementation((callback) => {
            captured.scan = callback;
            return { observe: vi.fn(), disconnect: vi.fn() };
        });

        return captured;
    };

    const clickHandlerOf = (element: HTMLElement) =>
        (element.addEventListener as Mock).mock.calls.find((call) => call[0] === 'click')?.[1];

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
        (coreUtils.isBrowser as Mock).mockReturnValue(true);
        (trackingUtils.findContentlets as Mock).mockReturnValue([]);
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    describe('Constructor', () => {
        it('should create tracker instance with logger', () => {
            tracker = new DotCMSClickTracker(mockConfig);

            expect(coreUtils.createPluginLogger).toHaveBeenCalledWith('Click', mockConfig);
        });
    });

    describe('onClick()', () => {
        it('should notify every subscribed callback of a click', () => {
            const mockElement = createMockContentletElement('test-123');
            (trackingUtils.findContentlets as Mock).mockReturnValue([mockElement]);
            (clickUtils.handleContentletClick as Mock).mockImplementation(
                (_event, _element, callback) => {
                    callback('content.click', {
                        content: { identifier: 'test-123' },
                        element: { attributes: [] }
                    });
                }
            );
            const otherCallback = vi.fn();

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.onClick(mockCallback);
            tracker.onClick(otherCallback);
            tracker.initialize();
            vi.advanceTimersByTime(100);

            clickHandlerOf(mockElement)(new MouseEvent('click'));

            expect(mockCallback).toHaveBeenCalledTimes(1);
            expect(otherCallback).toHaveBeenCalledTimes(1);
        });

        it('should return subscription with unsubscribe method', () => {
            tracker = new DotCMSClickTracker(mockConfig);
            const subscription = tracker.onClick(mockCallback);

            expect(subscription).toHaveProperty('unsubscribe');
            expect(typeof subscription.unsubscribe).toBe('function');
        });
    });

    describe('initialize()', () => {
        it('should skip initialization if not in browser environment', () => {
            (coreUtils.isBrowser as Mock).mockReturnValue(false);

            tracker = new DotCMSClickTracker(mockConfig);
            const logger = createdLogger();

            tracker.initialize();

            expect(logger.warn).toHaveBeenCalledWith('No document, skipping');
            expect(trackingUtils.findContentlets).not.toHaveBeenCalled();
        });

        it('should run initial scan after delay', () => {
            const mockElement = createMockContentletElement('test-123');
            (trackingUtils.findContentlets as Mock).mockReturnValue([mockElement]);

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.initialize();

            // Before timeout
            expect(trackingUtils.findContentlets).not.toHaveBeenCalled();

            // After timeout
            vi.advanceTimersByTime(100);
            expect(trackingUtils.findContentlets).toHaveBeenCalled();
        });

        it('should initialize MutationObserver', () => {
            tracker = new DotCMSClickTracker(mockConfig);
            tracker.initialize();

            expect(trackingUtils.createContentletObserver).toHaveBeenCalled();
        });

        it('should log initialization', () => {
            tracker = new DotCMSClickTracker(mockConfig);
            const logger = createdLogger();

            tracker.initialize();

            expect(logger.debug).toHaveBeenCalledWith('Plugin initializing');
            expect(logger.info).toHaveBeenCalledWith('Plugin initialized');
        });
    });

    describe('attachClickListener()', () => {
        it('should attach click listener to new contentlet', () => {
            const mockElement = createMockContentletElement('test-123');
            (trackingUtils.findContentlets as Mock).mockReturnValue([mockElement]);

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
            const observer = captureObserverCallback();
            const mockElement = createMockContentletElement('test-123');
            (trackingUtils.findContentlets as Mock).mockReturnValue([mockElement]);

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.initialize();

            // Trigger initial scan
            vi.advanceTimersByTime(100);
            expect(mockElement.addEventListener).toHaveBeenCalledTimes(1);

            // Clear and trigger a second scan, as a DOM change does
            (mockElement.addEventListener as Mock).mockClear();
            observer.scan();

            // Should NOT attach again
            expect(mockElement.addEventListener).not.toHaveBeenCalled();
        });

        it('should remove on cleanup the same handler it attached', () => {
            const mockElement = createMockContentletElement('test-123');
            (trackingUtils.findContentlets as Mock).mockReturnValue([mockElement]);

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.initialize();

            // Trigger initial scan
            vi.advanceTimersByTime(100);
            tracker.cleanup();

            expect(mockElement.removeEventListener).toHaveBeenCalledWith(
                'click',
                clickHandlerOf(mockElement)
            );
        });
    });

    describe('Click Handler & Subscription', () => {
        it('should call handleContentletClick when contentlet is clicked', () => {
            const mockElement = createMockContentletElement('test-123');
            (trackingUtils.findContentlets as Mock).mockReturnValue([mockElement]);

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
            (trackingUtils.findContentlets as Mock).mockReturnValue([mockElement]);

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
                (_event, _element, callback) => {
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
            (trackingUtils.findContentlets as Mock).mockReturnValue([mockElement]);

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
                (_event, _element, callback) => {
                    callback('content.click', mockPayload);
                }
            );

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.onClick(mockCallback);
            tracker.initialize();

            vi.advanceTimersByTime(100);

            const clickHandler = (mockElement.addEventListener as Mock).mock.calls[0]![1];

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

        it('throttles each contentlet on its own, so a click on another one right after counts', () => {
            const first = createMockContentletElement('test-1');
            const second = createMockContentletElement('test-2');
            (trackingUtils.findContentlets as Mock).mockReturnValue([first, second]);
            (clickUtils.handleContentletClick as Mock).mockImplementation(
                (_event, element: HTMLElement, callback) => {
                    callback('content.click', {
                        content: { identifier: element.dataset['dotIdentifier'] },
                        element: { attributes: [] }
                    });
                }
            );

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.onClick(mockCallback);
            tracker.initialize();
            vi.advanceTimersByTime(100);

            const handlerOf = (element: HTMLElement) =>
                (element.addEventListener as Mock).mock.calls.find(
                    (call) => call[0] === 'click'
                )?.[1];

            handlerOf(first)(new MouseEvent('click'));
            handlerOf(second)(new MouseEvent('click'));

            expect(mockCallback).toHaveBeenCalledTimes(2);
        });

        it('should NOT notify unsubscribed callbacks', () => {
            const mockElement = createMockContentletElement('test-123');
            (trackingUtils.findContentlets as Mock).mockReturnValue([mockElement]);

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
                (_event, _element, callback) => {
                    callback('content.click', mockPayload);
                }
            );

            tracker = new DotCMSClickTracker(mockConfig);
            const subscription = tracker.onClick(mockCallback);
            tracker.initialize();

            vi.advanceTimersByTime(100);

            const clickHandler = (mockElement.addEventListener as Mock).mock.calls[0]![1];

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
            (trackingUtils.findContentlets as Mock).mockReturnValue([element1, element2, element3]);

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
            (trackingUtils.findContentlets as Mock).mockReturnValue([mockElement]);

            tracker = new DotCMSClickTracker(mockConfig);
            const logger = createdLogger();
            tracker.initialize();

            // Trigger initial scan
            vi.advanceTimersByTime(100);

            expect(logger.info).toHaveBeenCalledWith(
                expect.stringContaining('Attached 1 new click listeners')
            );
        });

        it('should handle empty contentlet list gracefully', () => {
            (trackingUtils.findContentlets as Mock).mockReturnValue([]);

            tracker = new DotCMSClickTracker(mockConfig);
            const logger = createdLogger();
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
            (trackingUtils.createContentletObserver as Mock).mockImplementation((callback) => {
                observerCallback = callback;
                return {
                    observe: vi.fn(),
                    disconnect: vi.fn()
                };
            });

            const element1 = createMockContentletElement('test-1');
            (trackingUtils.findContentlets as Mock).mockReturnValue([element1]);

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.initialize();

            // Trigger initial scan
            vi.advanceTimersByTime(100);
            expect(element1.addEventListener).toHaveBeenCalledTimes(1);

            // Simulate new contentlet added
            const element2 = createMockContentletElement('test-2');
            (trackingUtils.findContentlets as Mock).mockReturnValue([element1, element2]);
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
            (trackingUtils.findContentlets as Mock).mockReturnValue([element1, element2]);

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
            (trackingUtils.createContentletObserver as Mock).mockReturnValue(mockObserver);

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.initialize();

            tracker.cleanup();

            expect(mockObserver.disconnect).toHaveBeenCalled();
        });

        it('should disconnect the observer once, however many times it is cleaned up', () => {
            const mockObserver = {
                observe: vi.fn(),
                disconnect: vi.fn()
            };
            (trackingUtils.createContentletObserver as Mock).mockReturnValue(mockObserver);

            tracker = new DotCMSClickTracker(mockConfig);
            tracker.initialize();
            vi.advanceTimersByTime(100);

            tracker.cleanup();
            tracker.cleanup();

            expect(mockObserver.disconnect).toHaveBeenCalledTimes(1);
        });

        it('should log cleanup message', () => {
            tracker = new DotCMSClickTracker(mockConfig);
            const logger = createdLogger();
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
            (trackingUtils.findContentlets as Mock).mockReturnValue([mockElement]);

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
                (_event, _element, callback) => {
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
            const clickHandler = (mockElement.addEventListener as Mock).mock.calls[0]![1];
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
