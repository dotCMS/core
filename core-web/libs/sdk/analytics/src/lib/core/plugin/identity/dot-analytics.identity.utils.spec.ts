/* eslint-disable @typescript-eslint/no-explicit-any */
import { beforeEach, describe, expect, it, jest } from '@jest/globals';

import { hasUTMChanged } from './dot-analytics.identity.utils';

import { SESSION_UTM_KEY } from '../../shared/constants';
import { extractUTMParameters, safeSessionStorage } from '../../shared/utils/dot-analytics.utils';

// Mock the safeSessionStorage dependency but keep other exports
jest.mock('../../shared/dot-analytics.utils', () => {
    const actual = jest.requireActual('../../shared/dot-analytics.utils') as Record<
        string,
        unknown
    >;
    return {
        ...actual,
        safeSessionStorage: {
            getItem: jest.fn(),
            setItem: jest.fn()
        }
    };
});

describe('DotAnalytics Identity Utils', () => {
    let mockLocation: Location;

    beforeAll(() => {
        jest.useFakeTimers({ doNotFake: [] });
        jest.setSystemTime(new Date('2024-01-01T12:00:00Z'));
    });

    beforeEach(() => {
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

            const result = extractUTMParameters(window.location);
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

            const result = extractUTMParameters(window.location);
            expect(result).toEqual({
                source: 'google',
                medium: 'cpc',
                campaign: 'spring_sale'
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

            const result = extractUTMParameters(window.location);
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

            const result = extractUTMParameters(window.location);
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

            const result = extractUTMParameters(window.location);
            expect(result).toEqual({
                source: 'google',
                campaign: 'spring sale'
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
