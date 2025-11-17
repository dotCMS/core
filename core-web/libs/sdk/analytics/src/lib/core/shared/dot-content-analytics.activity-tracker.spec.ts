/* eslint-disable @typescript-eslint/no-explicit-any */

import { ANALYTICS_WINDOWS_ACTIVE_KEY, ANALYTICS_WINDOWS_CLEANUP_KEY } from '@dotcms/uve/internal';

import { ACTIVITY_EVENTS, DEFAULT_SESSION_TIMEOUT_MINUTES } from './constants';
import {
    cleanupActivityTracking,
    getLastActivity,
    getSessionInfo,
    initializeActivityTracking,
    isUserInactive,
    updateSessionActivity
} from './dot-content-analytics.activity-tracker';
import { DotCMSAnalyticsConfig } from './models';

describe('DotCMS Activity Tracker', () => {
    let mockConfig: DotCMSAnalyticsConfig;
    const BASE_TIME = new Date('2024-01-01T00:00:00.000Z').getTime();
    let currentTime: number;

    beforeEach(() => {
        jest.clearAllMocks();
        currentTime = BASE_TIME;

        // Use jest.spyOn to properly mock Date.now()
        jest.spyOn(Date, 'now').mockImplementation(() => currentTime);

        jest.useFakeTimers();

        mockConfig = {
            server: 'https://test.com',
            siteAuth: 'test-key',
            debug: false
        };

        // Clean up any previous state
        cleanupActivityTracking();
    });

    afterEach(() => {
        jest.restoreAllMocks();
        jest.useRealTimers();
        cleanupActivityTracking();
    });

    describe('Initialization', () => {
        it('should initialize activity tracking and set up event listeners', () => {
            const addEventListenerSpy = jest.spyOn(window, 'addEventListener');
            const documentAddEventListenerSpy = jest.spyOn(document, 'addEventListener');

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

        it('should set initial activity time', () => {
            const startTime = Date.now();
            initializeActivityTracking(mockConfig);
            const lastActivity = getLastActivity();

            expect(lastActivity).toBeGreaterThanOrEqual(startTime);
        });

        it('should log debug message when debug is enabled', () => {
            const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation();
            mockConfig.debug = true;

            initializeActivityTracking(mockConfig);

            expect(consoleWarnSpy).toHaveBeenCalledWith(
                'DotCMS Analytics: Activity tracking initialized'
            );

            consoleWarnSpy.mockRestore();
        });

        it('should cleanup previous listeners before re-initializing', () => {
            const removeEventListenerSpy = jest.spyOn(window, 'removeEventListener');

            // Initialize twice
            initializeActivityTracking(mockConfig);
            initializeActivityTracking(mockConfig);

            // Should have removed listeners from first initialization
            expect(removeEventListenerSpy).toHaveBeenCalled();

            removeEventListenerSpy.mockRestore();
        });
    });

    describe('Activity Throttling', () => {
        it('should throttle activity updates to max 1 per second', () => {
            initializeActivityTracking(mockConfig);
            const startTime = Date.now();

            // First update - should work
            updateSessionActivity();
            const firstUpdate = getLastActivity();
            expect(firstUpdate).toBeGreaterThanOrEqual(startTime);

            // Immediate second update - should be throttled
            updateSessionActivity();
            const secondUpdate = getLastActivity();
            expect(secondUpdate).toBe(firstUpdate); // Same time, was throttled

            // Wait for throttle to expire
            jest.advanceTimersByTime(1000);

            // Third update - should work
            updateSessionActivity();
            const thirdUpdate = getLastActivity();
            expect(thirdUpdate).toBeGreaterThanOrEqual(firstUpdate);
        });
    });

    describe('Session Timeout Detection', () => {
        it('should detect user is active when recently active', () => {
            initializeActivityTracking(mockConfig);
            updateSessionActivity();

            expect(isUserInactive()).toBe(false);
        });

        it('should detect user is inactive after timeout period', () => {
            initializeActivityTracking(mockConfig);
            updateSessionActivity();

            expect(isUserInactive()).toBe(false);

            // Fast-forward past timeout
            jest.advanceTimersByTime(DEFAULT_SESSION_TIMEOUT_MINUTES * 60 * 1000 + 1000);

            expect(isUserInactive()).toBe(true);
        });

        it('should log debug message when user becomes inactive', () => {
            const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation();
            mockConfig.debug = true;

            initializeActivityTracking(mockConfig);
            updateSessionActivity();

            // Fast-forward past timeout
            jest.advanceTimersByTime(DEFAULT_SESSION_TIMEOUT_MINUTES * 60 * 1000);

            expect(consoleWarnSpy).toHaveBeenCalledWith(
                'DotCMS Analytics: User became inactive after timeout'
            );

            consoleWarnSpy.mockRestore();
        });

        it('should handle multiple activity bursts correctly', () => {
            initializeActivityTracking(mockConfig);

            // First activity
            updateSessionActivity();
            expect(isUserInactive()).toBe(false);

            // Wait and do another activity before timeout
            jest.advanceTimersByTime(5 * 60 * 1000); // 5 minutes
            jest.advanceTimersByTime(1000); // Clear throttle
            updateSessionActivity();
            expect(isUserInactive()).toBe(false);

            // Wait and do another activity
            jest.advanceTimersByTime(5 * 60 * 1000);
            jest.advanceTimersByTime(1000); // Clear throttle
            updateSessionActivity();
            expect(isUserInactive()).toBe(false);

            // Now wait full timeout
            jest.advanceTimersByTime(DEFAULT_SESSION_TIMEOUT_MINUTES * 60 * 1000 + 1000);
            expect(isUserInactive()).toBe(true);
        });
    });

    describe('Visibility Change Handling', () => {
        it('should NOT update activity when page becomes hidden', () => {
            initializeActivityTracking(mockConfig);

            jest.advanceTimersByTime(1000);
            updateSessionActivity();
            const activityBeforeHidden = getLastActivity();

            // Simulate page becoming hidden
            Object.defineProperty(document, 'visibilityState', {
                value: 'hidden',
                writable: true,
                configurable: true
            });
            document.dispatchEvent(new Event('visibilitychange'));

            expect(getLastActivity()).toBe(activityBeforeHidden);
        });

        it('should log debug message when user returns to tab', () => {
            const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation();
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
                'DotCMS Analytics: User returned to tab, session reactivated'
            );

            consoleWarnSpy.mockRestore();
        });
    });

    describe('Cleanup', () => {
        it('should remove all event listeners on cleanup', () => {
            const removeEventListenerSpy = jest.spyOn(window, 'removeEventListener');
            const documentRemoveEventListenerSpy = jest.spyOn(document, 'removeEventListener');

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

            const timerCount = jest.getTimerCount();
            expect(timerCount).toBeGreaterThan(0);

            cleanupActivityTracking();

            // Inactivity timer should be cleared
            expect(jest.getTimerCount()).toBe(0);
        });

        it('should reset window analytics properties', () => {
            initializeActivityTracking(mockConfig);
            window[ANALYTICS_WINDOWS_ACTIVE_KEY] = true;
            window[ANALYTICS_WINDOWS_CLEANUP_KEY] = jest.fn();

            cleanupActivityTracking();

            expect(window[ANALYTICS_WINDOWS_ACTIVE_KEY]).toBe(false);
            expect(window[ANALYTICS_WINDOWS_CLEANUP_KEY]).toBeUndefined();
        });

        it('should dispatch cleanup event', () => {
            const dispatchEventSpy = jest.spyOn(window, 'dispatchEvent');

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

        it('should not respond to events after cleanup', () => {
            initializeActivityTracking(mockConfig);
            const activityBeforeCleanup = getLastActivity();

            cleanupActivityTracking();

            // Try to trigger activity
            jest.advanceTimersByTime(1000);
            window.dispatchEvent(new MouseEvent('click'));

            // Activity should not have been updated
            expect(getLastActivity()).toBe(activityBeforeCleanup);
        });
    });

    describe('Session Info', () => {
        it('should return correct session info when active', () => {
            initializeActivityTracking(mockConfig);
            updateSessionActivity();

            const info = getSessionInfo();

            expect(info).toHaveProperty('lastActivity');
            expect(info).toHaveProperty('isActive');
            expect(info.isActive).toBe(true);
            expect(info.lastActivity).toBeGreaterThan(0);
        });

        it('should return correct session info when inactive', () => {
            initializeActivityTracking(mockConfig);
            updateSessionActivity();

            // Wait past timeout
            jest.advanceTimersByTime(DEFAULT_SESSION_TIMEOUT_MINUTES * 60 * 1000 + 1000);

            const info = getSessionInfo();

            expect(info.isActive).toBe(false);
            expect(info.lastActivity).toBeGreaterThan(0);
        });
    });

    describe('Real-World Scenarios', () => {
        it('should handle typical user session with intermittent activity', () => {
            initializeActivityTracking(mockConfig);

            // User clicks around
            window.dispatchEvent(new MouseEvent('click'));
            expect(isUserInactive()).toBe(false);

            // User scrolls after 30 seconds
            jest.advanceTimersByTime(30 * 1000);
            jest.advanceTimersByTime(1000); // Clear throttle
            window.dispatchEvent(new Event('scroll'));
            expect(isUserInactive()).toBe(false);

            // User moves mouse after 1 minute
            jest.advanceTimersByTime(60 * 1000);
            jest.advanceTimersByTime(1000); // Clear throttle
            window.dispatchEvent(new MouseEvent('mousemove'));
            expect(isUserInactive()).toBe(false);

            // User types after 2 minutes
            jest.advanceTimersByTime(2 * 60 * 1000);
            jest.advanceTimersByTime(1000); // Clear throttle
            window.dispatchEvent(new KeyboardEvent('keypress'));
            expect(isUserInactive()).toBe(false);

            // User goes idle for full timeout
            jest.advanceTimersByTime(DEFAULT_SESSION_TIMEOUT_MINUTES * 60 * 1000 + 1000);
            expect(isUserInactive()).toBe(true);
        });

        it('should handle SPA navigation with re-initialization', () => {
            // Initial page
            initializeActivityTracking(mockConfig);
            updateSessionActivity();
            const firstActivity = getLastActivity();

            // Navigate to new page (cleanup + re-init)
            cleanupActivityTracking();
            jest.advanceTimersByTime(1000);
            initializeActivityTracking(mockConfig);

            // Activity should be reset
            const secondActivity = getLastActivity();
            expect(secondActivity).toBeGreaterThanOrEqual(firstActivity);

            // Old listeners should not respond
            expect(jest.getTimerCount()).toBeGreaterThan(0);
        });
    });
});
