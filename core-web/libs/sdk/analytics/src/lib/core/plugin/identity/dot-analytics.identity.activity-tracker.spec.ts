/* eslint-disable @typescript-eslint/no-explicit-any */

import { vi } from 'vitest';

import {
    cleanupActivityTracking,
    initializeActivityTracking,
    updateSessionActivity
} from './dot-analytics.identity.activity-tracker';

import { ACTIVITY_EVENTS } from '../../shared/constants';
import {
    ANALYTICS_WINDOWS_ACTIVE_KEY,
    ANALYTICS_WINDOWS_CLEANUP_KEY
} from '../../shared/constants/dot-analytics.constants';
import { DotCMSAnalyticsConfig } from '../../shared/models';

describe('DotCMS Activity Tracker', () => {
    let mockConfig: DotCMSAnalyticsConfig;
    const BASE_TIME = new Date('2024-01-01T00:00:00.000Z').getTime();
    let currentTime: number;

    beforeEach(() => {
        vi.clearAllMocks();
        currentTime = BASE_TIME;

        // Use vi.spyOn to properly mock Date.now()
        vi.spyOn(Date, 'now').mockImplementation(() => currentTime);

        vi.useFakeTimers();

        mockConfig = {
            server: 'https://test.com',
            siteAuth: 'test-key',
            debug: false
        };

        // Clean up any previous state
        cleanupActivityTracking();
    });

    afterEach(() => {
        vi.restoreAllMocks();
        vi.useRealTimers();
        cleanupActivityTracking();
    });

    describe('Initialization', () => {
        it('should initialize activity tracking and set up event listeners', () => {
            const addEventListenerSpy = vi.spyOn(window, 'addEventListener');
            const documentAddEventListenerSpy = vi.spyOn(document, 'addEventListener');

            initializeActivityTracking(mockConfig);

            // Should register all activity events
            expect(addEventListenerSpy).toHaveBeenCalledTimes(ACTIVITY_EVENTS.length);
            ACTIVITY_EVENTS.forEach((eventType) => {
                expect(addEventListenerSpy).toHaveBeenCalledWith(eventType, expect.any(Function), {
                    passive: true
                });
            });

            // Should register visibility change listener
            expect(documentAddEventListenerSpy).toHaveBeenCalledWith(
                'visibilitychange',
                expect.any(Function)
            );

            addEventListenerSpy.mockRestore();
            documentAddEventListenerSpy.mockRestore();
        });

        it('should NOT initialize in SSR (no window)', () => {
            const originalWindow = global.window;
            delete (global as any).window;

            // No error should be thrown
            expect(() => initializeActivityTracking(mockConfig)).not.toThrow();

            // Restore
            (global as any).window = originalWindow;
        });

        it('should log debug message when debug is enabled', () => {
            const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation();
            mockConfig.debug = true;

            initializeActivityTracking(mockConfig);

            expect(consoleWarnSpy).toHaveBeenCalledWith(
                'DotCMS Analytics [Activity]: Activity tracking initialized'
            );

            consoleWarnSpy.mockRestore();
        });

        it('should cleanup previous listeners before re-initializing', () => {
            const removeEventListenerSpy = vi.spyOn(window, 'removeEventListener');

            // Initialize twice
            initializeActivityTracking(mockConfig);
            initializeActivityTracking(mockConfig);

            // Should have removed listeners from first initialization
            expect(removeEventListenerSpy).toHaveBeenCalled();

            removeEventListenerSpy.mockRestore();
        });
    });

    describe('Visibility Change Handling', () => {
        it('should log debug message when user returns to tab', () => {
            const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation();
            mockConfig.debug = true;

            initializeActivityTracking(mockConfig);

            // Simulate page becoming visible
            Object.defineProperty(document, 'visibilityState', {
                value: 'visible',
                writable: true,
                configurable: true
            });
            document.dispatchEvent(new Event('visibilitychange'));

            expect(consoleWarnSpy).toHaveBeenCalledWith(
                'DotCMS Analytics [Activity]: User returned to tab, session reactivated'
            );

            consoleWarnSpy.mockRestore();
        });
    });

    describe('Cleanup', () => {
        it('should remove all event listeners on cleanup', () => {
            const removeEventListenerSpy = vi.spyOn(window, 'removeEventListener');
            const documentRemoveEventListenerSpy = vi.spyOn(document, 'removeEventListener');

            initializeActivityTracking(mockConfig);
            cleanupActivityTracking();

            // Should have removed all activity event listeners
            expect(removeEventListenerSpy).toHaveBeenCalledTimes(ACTIVITY_EVENTS.length);
            ACTIVITY_EVENTS.forEach((eventType) => {
                expect(removeEventListenerSpy).toHaveBeenCalledWith(
                    eventType,
                    expect.any(Function)
                );
            });

            // Should have removed visibility change listener
            expect(documentRemoveEventListenerSpy).toHaveBeenCalledWith(
                'visibilitychange',
                expect.any(Function)
            );

            removeEventListenerSpy.mockRestore();
            documentRemoveEventListenerSpy.mockRestore();
        });

        it('should clear inactivity timer on cleanup', () => {
            initializeActivityTracking(mockConfig);
            updateSessionActivity();

            const timerCount = vi.getTimerCount();
            expect(timerCount).toBeGreaterThan(0);

            cleanupActivityTracking();

            // Inactivity timer should be cleared
            expect(vi.getTimerCount()).toBe(0);
        });

        it('should reset window analytics properties', () => {
            initializeActivityTracking(mockConfig);
            window[ANALYTICS_WINDOWS_ACTIVE_KEY] = true;
            window[ANALYTICS_WINDOWS_CLEANUP_KEY] = vi.fn();

            cleanupActivityTracking();

            expect(window[ANALYTICS_WINDOWS_ACTIVE_KEY]).toBe(false);
            expect(window[ANALYTICS_WINDOWS_CLEANUP_KEY]).toBeUndefined();
        });

        it('should dispatch cleanup event', () => {
            const dispatchEventSpy = vi.spyOn(window, 'dispatchEvent');

            initializeActivityTracking(mockConfig);
            cleanupActivityTracking();

            expect(dispatchEventSpy).toHaveBeenCalledWith(
                expect.objectContaining({
                    type: 'dotcms:analytics:cleanup'
                })
            );

            dispatchEventSpy.mockRestore();
        });

        it('should handle cleanup when not initialized', () => {
            // Should not throw
            expect(() => cleanupActivityTracking()).not.toThrow();
        });

        it('should handle multiple cleanups without errors', () => {
            initializeActivityTracking(mockConfig);

            cleanupActivityTracking();
            cleanupActivityTracking();
            cleanupActivityTracking();

            // Should not throw
            expect(true).toBe(true);
        });

        it('should handle SPA navigation with re-initialization', () => {
            // Initial page
            initializeActivityTracking(mockConfig);
            updateSessionActivity();

            // Navigate to new page (cleanup + re-init)
            cleanupActivityTracking();
            vi.advanceTimersByTime(1000);
            initializeActivityTracking(mockConfig);

            // Old listeners should not respond
            expect(vi.getTimerCount()).toBeGreaterThan(0);
        });
    });
});
