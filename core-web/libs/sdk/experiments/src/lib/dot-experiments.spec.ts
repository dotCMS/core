/* eslint-disable @typescript-eslint/ban-ts-comment */
import fakeIndexedDB from 'fake-indexeddb';
import fetchMock from 'fetch-mock';

import { API_EXPERIMENTS_URL } from './constants';
import { DotExperiments } from './dot-experiments';
import {
    IsUserIncludedResponse,
    LocationMock,
    MOCK_CURRENT_TIMESTAMP,
    MockDataStoredIndexDB,
    MockDataStoredIndexDBWithNew,
    MockDataStoredIndexDBWithNew15DaysLater,
    NewIsUserIncludedResponse,
    sessionStorageMock,
    TIME_15_DAYS_MILLISECONDS,
    TIME_5_DAYS_MILLISECONDS
} from './mocks/mock';
import { DotExperimentConfig } from './models';

// Jitsu library Mock
jest.mock('@jitsu/sdk-js', () => ({
    jitsuClient: jest.fn().mockImplementation(() => ({
        set: jest.fn()
    }))
}));

// SessionStorage mock
global.sessionStorage = sessionStorageMock;

// IndexDB Mock
Object.defineProperty(window, 'indexedDB', {
    writable: true,
    value: fakeIndexedDB
});

if (!globalThis.structuredClone) {
    globalThis.structuredClone = function (obj) {
        return JSON.parse(JSON.stringify(obj));
    };
}

// Windows Mock
global.window = Object.create(window);
Object.defineProperty(window, 'location', {
    value: {
        href: 'http://localhost:8080/'
    }
});

describe('DotExperiments', () => {
    const configMock: DotExperimentConfig = {
        'api-key': 'yourApiKey',
        server: 'http://localhost:8080/',
        debug: false
    };

    describe('DotExperiments Instance and Initialization', () => {
        beforeEach(() => {
            // destroy the instance of the singleton
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            (DotExperiments as any).instance = null;
        });
        it('should throw an error if config is not provided', () => {
            expect(() => DotExperiments.getInstance()).toThrow(
                'Configuration is required to create a new instance.'
            );
        });

        it('should instantiate the class when getInstance is called with config', () => {
            const dotExperimentsInstance = DotExperiments.getInstance(configMock);
            expect(dotExperimentsInstance).toBeInstanceOf(DotExperiments);
        });

        it('should throw an error if server is not provided in config', () => {
            expect(() =>
                // @ts-ignore
                DotExperiments.getInstance({ 'api-key': 'api-key-test', debug: true })
            ).toThrow('`server` must be provided and should not be empty!');
        });

        it('should throw an error if api-key is not provided in config', () => {
            expect(() =>
                // @ts-ignore
                DotExperiments.getInstance({ server: 'http://server-test.com', debug: true })
            ).toThrow('`api-key` must be provided and should not be empty!');
        });

        it('should return false if the debug inactive', async () => {
            const instance = DotExperiments.getInstance(configMock);
            expect(instance.getIsDebugActive()).toBe(false);
        });
    });

    describe('Class interactions', () => {
        beforeEach(() => {
            fetchMock.restore();
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            (DotExperiments as any).instance = null;
            jest.restoreAllMocks();
            jest.clearAllMocks();
        });

        it('should simulate the changes of the data in first run, after 5 days and 15 days.', async () => {
            // First time the user enter to the page

            const mockNow = jest.spyOn(Date, 'now');
            mockNow.mockImplementation(() => MOCK_CURRENT_TIMESTAMP);

            fetchMock.post(`${configMock.server}${API_EXPERIMENTS_URL}`, {
                status: 200,
                body: IsUserIncludedResponse,
                headers: {
                    Accept: 'application/json',
                    'Content-Type': 'application/json'
                }
            });

            const instance = DotExperiments.getInstance(configMock);
            await instance.ready().then(() => {
                const experiments = instance.experiments;
                expect(experiments.length).toBe(1);
                expect(experiments).toEqual(MockDataStoredIndexDB);
            });

            // Second time the user enter to the page
            // change the time 5 days later
            mockNow.mockImplementation(() => MOCK_CURRENT_TIMESTAMP + TIME_5_DAYS_MILLISECONDS);

            fetchMock.post(
                `${configMock.server}${API_EXPERIMENTS_URL}`,
                {
                    status: 200,
                    body: NewIsUserIncludedResponse,
                    headers: {
                        Accept: 'application/json',
                        'Content-Type': 'application/json'
                    }
                },
                { overwriteRoutes: true }
            );

            await instance.locationChanged(LocationMock).then(() => {
                const experiments = instance.experiments;
                expect(experiments.length).toBe(2);
                expect(experiments).toEqual(MockDataStoredIndexDBWithNew);
            });

            fetchMock.post(
                `${configMock.server}${API_EXPERIMENTS_URL}`,
                {
                    status: 200,
                    body: [],
                    headers: {
                        Accept: 'application/json',
                        'Content-Type': 'application/json'
                    }
                },
                { overwriteRoutes: true }
            );

            // Third try, after 15 days
            const location = { ...LocationMock, href: 'http://localhost/destinations' };
            mockNow.mockImplementation(() => MOCK_CURRENT_TIMESTAMP + TIME_15_DAYS_MILLISECONDS);
            await instance.locationChanged(location).then(() => {
                const experiments = instance.experiments;
                expect(experiments.length).toBe(1);
                expect(experiments).toEqual(MockDataStoredIndexDBWithNew15DaysLater);
            });
        });
    });
});
