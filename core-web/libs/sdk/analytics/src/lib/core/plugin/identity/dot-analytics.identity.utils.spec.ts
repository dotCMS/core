/* eslint-disable @typescript-eslint/no-explicit-any */
import { beforeEach, describe, expect, it, jest } from '@jest/globals';

import {
    extractUTMParameters,
    getLastActivityTime,
    hasPassedMidnight,
    hasUTMChanged,
    isUserInactive,
    updateActivityTime
} from './dot-analytics.identity.utils';

import {
    DEFAULT_SESSION_TIMEOUT_MINUTES,
    SESSION_UTM_KEY
} from '../../shared/constants';
import { safeSessionStorage } from '../../shared/dot-content-analytics.utils';

// Mock the safeSessionStorage dependency
jest.mock('../../shared/dot-content-analytics.utils', () => ({
    safeSessionStorage: {
        getItem: jest.fn(),
        setItem: jest.fn()
    }
}));

describe('DotAnalytics Identity Utils', () => {
    let mockLocation: Location;
    let mockSetTimeout: jest.MockedFunction<any>;
    let mockClearTimeout: jest.MockedFunction<any>;

    beforeAll(() => {
        jest.useFakeTimers({ doNotFake: [] });
        jest.setSystemTime(new Date('2024-01-01T12:00:00Z'));
    });

    beforeEach(() => {
        // Create simple mocks
        mockSetTimeout = jest.fn().mockReturnValue(123); // Mock timer ID
        mockClearTimeout = jest.fn();

        // Replace global functions
        global.setTimeout = mockSetTimeout;
        global.clearTimeout = mockClearTimeout;

        // Mock Location object
        mockLocation = {
            href: 'https://example.com/page?utm_source=google&utm_medium=cpc',
            pathname: '/page',
            hostname: 'example.com',
            protocol: 'https:',
            hash: '#section1',
            search: '?utm_source=google&utm_medium=cpc',
            origin: 'https://example.com'
        } as Location;

        // Mock window.location
        Object.defineProperty(window, 'location', {
            value: mockLocation,
            writable: true,
            configurable: true
        });

        // Reset mocks
        jest.clearAllMocks();
    });

    afterEach(() => {
        // Clear mocks
        jest.clearAllMocks();
    });

    afterAll(() => {
        jest.useRealTimers();
    });

    describe('updateActivityTime', () => {
        it('should update the last activity time', () => {
            // Set initial time
            jest.setSystemTime(new Date('2024-01-01T12:00:00Z'));
            updateActivityTime();
            const initialTime = getLastActivityTime();

            // Advance time
            jest.setSystemTime(new Date('2024-01-01T12:05:00Z'));
            updateActivityTime();

            const newTime = getLastActivityTime();
            expect(newTime).toBeGreaterThan(initialTime);
        });

        it('should clear existing timeout before setting new one', () => {
            // Clear mock history first
            mockSetTimeout.mockClear();
            mockClearTimeout.mockClear();

            // First call - might clear existing timer
            updateActivityTime();
            const initialClearCalls = mockClearTimeout.mock.calls.length;
            expect(mockSetTimeout).toHaveBeenCalledTimes(1);

            // Second call should clear the previous timeout
            updateActivityTime();
            expect(mockClearTimeout).toHaveBeenCalledTimes(initialClearCalls + 1);
            expect(mockSetTimeout).toHaveBeenCalledTimes(2);
        });

        it('should set timeout for session timeout duration', () => {
            updateActivityTime();

            const expectedTimeout = DEFAULT_SESSION_TIMEOUT_MINUTES * 60 * 1000;
            expect(mockSetTimeout).toHaveBeenCalledWith(expect.any(Function), expectedTimeout);
        });
    });

    describe('isUserInactive', () => {
        it('should return false when user is active', () => {
            updateActivityTime();
            expect(isUserInactive()).toBe(false);
        });

        it('should return true when user has been inactive for timeout period', () => {
            updateActivityTime();

            // Move time forward beyond timeout
            const timeoutMs = DEFAULT_SESSION_TIMEOUT_MINUTES * 60 * 1000;
            jest.setSystemTime(new Date(Date.now() + timeoutMs + 1000));

            expect(isUserInactive()).toBe(true);
        });

        it('should return false when user activity is within timeout period', () => {
            updateActivityTime();

            // Move time forward but within timeout
            const timeoutMs = DEFAULT_SESSION_TIMEOUT_MINUTES * 60 * 1000;
            jest.setSystemTime(new Date(Date.now() + timeoutMs - 1000));

            expect(isUserInactive()).toBe(false);
        });
    });

    describe('getLastActivityTime', () => {
        it('should return the current last activity time', () => {
            const testTime = new Date('2024-01-01T12:30:00Z').getTime();
            jest.setSystemTime(testTime);

            updateActivityTime();

            expect(getLastActivityTime()).toBe(testTime);
        });

        it('should return different times after multiple updates', () => {
            updateActivityTime();
            const firstTime = getLastActivityTime();

            // Advance time and update again
            jest.setSystemTime(new Date('2024-01-01T12:35:00Z'));
            updateActivityTime();
            const secondTime = getLastActivityTime();

            expect(secondTime).toBeGreaterThan(firstTime);
        });
    });

    describe('hasPassedMidnight', () => {
        it('should return false when session started on the same UTC day', () => {
            const sessionStart = new Date('2024-01-01T10:00:00Z').getTime();
            jest.setSystemTime(new Date('2024-01-01T14:00:00Z'));

            expect(hasPassedMidnight(sessionStart)).toBe(false);
        });

        it('should return true when session started on a different UTC day', () => {
            const sessionStart = new Date('2024-01-01T10:00:00Z').getTime();
            jest.setSystemTime(new Date('2024-01-02T02:00:00Z'));

            expect(hasPassedMidnight(sessionStart)).toBe(true);
        });

        it('should handle timezone differences correctly using UTC', () => {
            // Session starts at 23:30 UTC on Jan 1st
            const sessionStart = new Date('2024-01-01T23:30:00Z').getTime();
            // Current time is 01:30 UTC on Jan 2nd (same local day in some timezones)
            jest.setSystemTime(new Date('2024-01-02T01:30:00Z'));

            expect(hasPassedMidnight(sessionStart)).toBe(true);
        });

        it('should return false for sessions on same UTC day but different local days', () => {
            // Edge case: same UTC day
            const sessionStart = new Date('2024-01-01T06:00:00Z').getTime();
            jest.setSystemTime(new Date('2024-01-01T18:00:00Z'));

            expect(hasPassedMidnight(sessionStart)).toBe(false);
        });
    });

    describe('extractUTMParameters', () => {
        it('should return empty object when no UTM parameters are present', () => {
            const mockLocationNoUTM = {
                ...mockLocation,
                search: '?param=value&other=test'
            } as Location;

            // Temporarily override window.location
            Object.defineProperty(window, 'location', {
                value: mockLocationNoUTM,
                writable: true,
                configurable: true
            });

            const result = extractUTMParameters();
            expect(result).toEqual({});
        });

        it('should extract UTM parameters correctly', () => {
            const mockLocationWithUTM = {
                ...mockLocation,
                search: '?utm_source=google&utm_medium=cpc&utm_campaign=spring_sale&utm_id=12345'
            } as Location;

            Object.defineProperty(window, 'location', {
                value: mockLocationWithUTM,
                writable: true,
                configurable: true
            });

            const result = extractUTMParameters();
            expect(result).toEqual({
                source: 'google',
                medium: 'cpc',
                campaign: 'spring_sale',
                id: '12345'
            });
        });

        it('should ignore non-UTM parameters', () => {
            const mockLocationMixed = {
                ...mockLocation,
                search: '?utm_source=google&regular_param=value&utm_medium=cpc&other=test'
            } as Location;

            Object.defineProperty(window, 'location', {
                value: mockLocationMixed,
                writable: true,
                configurable: true
            });

            const result = extractUTMParameters();
            expect(result).toEqual({
                source: 'google',
                medium: 'cpc'
            });
        });

        it('should handle partial UTM parameters', () => {
            const mockLocationPartial = {
                ...mockLocation,
                search: '?utm_source=facebook&utm_campaign=summer'
            } as Location;

            Object.defineProperty(window, 'location', {
                value: mockLocationPartial,
                writable: true,
                configurable: true
            });

            const result = extractUTMParameters();
            expect(result).toEqual({
                source: 'facebook',
                campaign: 'summer'
            });
        });

        it('should handle URL encoded UTM parameters', () => {
            const mockLocationEncoded = {
                ...mockLocation,
                search: '?utm_source=google&utm_campaign=spring%20sale&utm_id=test%201'
            } as Location;

            Object.defineProperty(window, 'location', {
                value: mockLocationEncoded,
                writable: true,
                configurable: true
            });

            const result = extractUTMParameters();
            expect(result).toEqual({
                source: 'google',
                campaign: 'spring sale',
                id: 'test 1'
            });
        });
    });

    describe('hasUTMChanged', () => {
        const mockSafeSessionStorage = safeSessionStorage as jest.Mocked<typeof safeSessionStorage>;

        beforeEach(() => {
            mockSafeSessionStorage.getItem.mockClear();
            mockSafeSessionStorage.setItem.mockClear();
        });

        it('should return false and store UTM when no previous UTM exists', () => {
            mockSafeSessionStorage.getItem.mockReturnValue(null);

            const currentUTM = {
                source: 'google',
                medium: 'cpc',
                campaign: 'spring'
            };

            const result = hasUTMChanged(currentUTM);

            expect(result).toBe(false);
            expect(mockSafeSessionStorage.setItem).toHaveBeenCalledWith(
                SESSION_UTM_KEY,
                JSON.stringify(currentUTM)
            );
        });

        it('should return false when UTM parameters have not changed', () => {
            const storedUTM = {
                source: 'google',
                medium: 'cpc',
                campaign: 'spring'
            };

            mockSafeSessionStorage.getItem.mockReturnValue(JSON.stringify(storedUTM));

            const currentUTM = {
                source: 'google',
                medium: 'cpc',
                campaign: 'spring',
                term: 'shoes' // Non-significant parameter
            };

            const result = hasUTMChanged(currentUTM);

            expect(result).toBe(false);
            expect(mockSafeSessionStorage.setItem).not.toHaveBeenCalled();
        });

        it('should return true when UTM source changes', () => {
            const storedUTM = {
                source: 'google',
                medium: 'cpc',
                campaign: 'spring'
            };

            mockSafeSessionStorage.getItem.mockReturnValue(JSON.stringify(storedUTM));

            const currentUTM = {
                source: 'facebook', // Changed
                medium: 'cpc',
                campaign: 'spring'
            };

            const result = hasUTMChanged(currentUTM);

            expect(result).toBe(true);
            expect(mockSafeSessionStorage.setItem).toHaveBeenCalledWith(
                SESSION_UTM_KEY,
                JSON.stringify(currentUTM)
            );
        });

        it('should return true when UTM medium changes', () => {
            const storedUTM = {
                source: 'google',
                medium: 'cpc',
                campaign: 'spring'
            };

            mockSafeSessionStorage.getItem.mockReturnValue(JSON.stringify(storedUTM));

            const currentUTM = {
                source: 'google',
                medium: 'organic', // Changed
                campaign: 'spring'
            };

            const result = hasUTMChanged(currentUTM);

            expect(result).toBe(true);
            expect(mockSafeSessionStorage.setItem).toHaveBeenCalledWith(
                SESSION_UTM_KEY,
                JSON.stringify(currentUTM)
            );
        });

        it('should return true when UTM campaign changes', () => {
            const storedUTM = {
                source: 'google',
                medium: 'cpc',
                campaign: 'spring'
            };

            mockSafeSessionStorage.getItem.mockReturnValue(JSON.stringify(storedUTM));

            const currentUTM = {
                source: 'google',
                medium: 'cpc',
                campaign: 'summer' // Changed
            };

            const result = hasUTMChanged(currentUTM);

            expect(result).toBe(true);
            expect(mockSafeSessionStorage.setItem).toHaveBeenCalledWith(
                SESSION_UTM_KEY,
                JSON.stringify(currentUTM)
            );
        });

        it('should ignore changes in non-significant UTM parameters', () => {
            const storedUTM = {
                source: 'google',
                medium: 'cpc',
                campaign: 'spring',
                term: 'shoes',
                content: 'ad1'
            };

            mockSafeSessionStorage.getItem.mockReturnValue(JSON.stringify(storedUTM));

            const currentUTM = {
                source: 'google',
                medium: 'cpc',
                campaign: 'spring',
                term: 'boots', // Changed but not significant
                content: 'ad2' // Changed but not significant
            };

            const result = hasUTMChanged(currentUTM);

            expect(result).toBe(false);
            expect(mockSafeSessionStorage.setItem).not.toHaveBeenCalled();
        });

        it('should handle JSON parsing errors gracefully', () => {
            mockSafeSessionStorage.getItem.mockReturnValue('invalid-json');

            const currentUTM = {
                source: 'google',
                medium: 'cpc',
                campaign: 'spring'
            };

            const result = hasUTMChanged(currentUTM);

            expect(result).toBe(false);
            expect(mockSafeSessionStorage.setItem).not.toHaveBeenCalled();
        });

        it('should handle missing UTM parameters in stored data', () => {
            const storedUTM = {
                source: 'google'
                // Missing medium and campaign
            };

            mockSafeSessionStorage.getItem.mockReturnValue(JSON.stringify(storedUTM));

            const currentUTM = {
                source: 'google',
                medium: 'cpc',
                campaign: 'spring'
            };

            const result = hasUTMChanged(currentUTM);

            expect(result).toBe(true);
            expect(mockSafeSessionStorage.setItem).toHaveBeenCalledWith(
                SESSION_UTM_KEY,
                JSON.stringify(currentUTM)
            );
        });

        it('should handle missing UTM parameters in current data', () => {
            const storedUTM = {
                source: 'google',
                medium: 'cpc',
                campaign: 'spring'
            };

            mockSafeSessionStorage.getItem.mockReturnValue(JSON.stringify(storedUTM));

            const currentUTM = {
                source: 'google'
                // Missing medium and campaign
            };

            const result = hasUTMChanged(currentUTM);

            expect(result).toBe(true);
            expect(mockSafeSessionStorage.setItem).toHaveBeenCalledWith(
                SESSION_UTM_KEY,
                JSON.stringify(currentUTM)
            );
        });

        it('should handle sessionStorage errors', () => {
            mockSafeSessionStorage.getItem.mockImplementation(() => {
                throw new Error('SessionStorage error');
            });

            const currentUTM = {
                source: 'google',
                medium: 'cpc',
                campaign: 'spring'
            };

            const result = hasUTMChanged(currentUTM);

            expect(result).toBe(false);
        });
    });
});
