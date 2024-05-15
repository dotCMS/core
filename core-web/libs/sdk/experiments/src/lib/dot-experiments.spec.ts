/* eslint-disable @typescript-eslint/ban-ts-comment */
import fakeIndexedDB from 'fake-indexeddb';
import fetchMock from 'fetch-mock';

import { DotExperiments } from './dot-experiments';
import { API_EXPERIMENTS_URL, EXPERIMENT_QUERY_PARAM_KEY } from './shared/constants';
import {
    After15DaysIsUserIncludedResponse,
    IsUserIncludedResponse,
    LocationMock,
    MOCK_CURRENT_TIMESTAMP,
    MockDataStoredIndexDB,
    MockDataStoredIndexDBWithNew,
    MockDataStoredIndexDBWithNew15DaysLater,
    NewIsUserIncludedResponse,
    NoExperimentsIsUserIncludedResponse,
    sessionStorageMock,
    TIME_15_DAYS_MILLISECONDS,
    TIME_5_DAYS_MILLISECONDS
} from './shared/mocks/mock';
import { DotExperimentConfig } from './shared/models';

jest.spyOn(Date, 'now').mockImplementation(() => MOCK_CURRENT_TIMESTAMP);

// Jitsu SDK Mock
jest.mock('@jitsu/sdk-js', () => ({
    jitsuClient: jest.fn(() => ({
        set: jest.fn(),
        track: jest.fn().mockResolvedValue(true)
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
        href: 'http://localhost:8080/',
        origin: 'http://localhost:8080'
    }
});

describe('DotExperiments', () => {
    const configMock: DotExperimentConfig = {
        apiKey: 'yourApiKey',
        server: 'http://localhost:8080/',
        debug: false,
        trackPageView: true
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
            ).toThrow('`apiKey` must be provided and should not be empty!');
        });

        it('should return false if the debug inactive', async () => {
            const instance = DotExperiments.getInstance(configMock);

            expect(instance.getIsDebugActive()).toBe(false);
        });

        it('should not call to trackPageView if you send the flag', async () => {
            const config: DotExperimentConfig = { ...configMock, trackPageView: false };

            fetchMock.post(`${configMock.server}/${API_EXPERIMENTS_URL}`, {
                status: 200,
                body: IsUserIncludedResponse,
                headers: {
                    Accept: 'application/json',
                    'Content-Type': 'application/json'
                }
            });

            const instance = DotExperiments.getInstance(config);

            const spyTrackPageView = jest.spyOn(instance, 'trackPageView');

            expect(spyTrackPageView).not.toHaveBeenCalled();

            await instance.locationChanged(LocationMock).then(() => {
                expect(spyTrackPageView).not.toHaveBeenCalled();
            });
        });

        it('should not call to trackPageView if you dont have experiment to track', async () => {
            const config: DotExperimentConfig = { ...configMock, trackPageView: false };

            fetchMock.post(
                `${configMock.server}/${API_EXPERIMENTS_URL}`,
                {
                    status: 200,
                    body: NoExperimentsIsUserIncludedResponse,
                    headers: {
                        Accept: 'application/json',
                        'Content-Type': 'application/json'
                    }
                },
                { overwriteRoutes: true }
            );

            const instance = DotExperiments.getInstance(config);

            const spyTrackPageView = jest.spyOn(instance, 'trackPageView');

            expect(spyTrackPageView).not.toHaveBeenCalled();

            await instance.locationChanged(LocationMock).then(() => {
                expect(spyTrackPageView).not.toHaveBeenCalled();
            });
        });

        it('should return a a string with the query params of variant by the url given', async () => {
            const config: DotExperimentConfig = { ...configMock, trackPageView: false };

            fetchMock.post(
                `${configMock.server}/${API_EXPERIMENTS_URL}`,
                {
                    status: 200,
                    body: IsUserIncludedResponse,
                    headers: {
                        Accept: 'application/json',
                        'Content-Type': 'application/json'
                    }
                },
                { overwriteRoutes: true }
            );

            const instance = DotExperiments.getInstance(config);

            await instance.ready();

            const EMPTY_URL = '';

            const expected1 = new URLSearchParams({});

            expect(instance.getVariantAsQueryParam(EMPTY_URL)).toStrictEqual(expected1);

            const URL_WITH_EXPERIMENT = '/blog';

            const expected2 = new URLSearchParams({
                [EXPERIMENT_QUERY_PARAM_KEY]:
                    IsUserIncludedResponse.entity.experiments[0].variant.name
            });

            expect(instance.getVariantAsQueryParam(URL_WITH_EXPERIMENT)).toStrictEqual(expected2);

            const URL_NO_EXPERIMENT = '/destinations';

            const expected3 = new URLSearchParams({});

            expect(instance.getVariantAsQueryParam(URL_NO_EXPERIMENT)).toStrictEqual(expected3);
        });
    });

    describe('Class interactions', () => {
        beforeEach(() => {
            fetchMock.restore();
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            (DotExperiments as any).instance = null;
        });

        it('should simulate the changes of the data in first run, after 5 days and 15 days.', async () => {
            // First time the user enter to the page

            fetchMock.post(`${configMock.server}/${API_EXPERIMENTS_URL}`, {
                status: 200,
                body: IsUserIncludedResponse,
                headers: {
                    Accept: 'application/json',
                    'Content-Type': 'application/json'
                }
            });

            const instance = DotExperiments.getInstance(configMock);

            const spyTrackPageView = jest.spyOn(instance, 'trackPageView');

            await instance.ready().then(() => {
                const experiments = instance.experiments;

                expect(experiments.length).toBe(1);
                expect(experiments).toEqual(MockDataStoredIndexDB);

                expect(spyTrackPageView).toBeCalledTimes(1);
            });

            // Second time the user enter to the page
            // change the time 5 days later
            jest.spyOn(Date, 'now').mockImplementation(
                () => MOCK_CURRENT_TIMESTAMP + TIME_5_DAYS_MILLISECONDS
            );

            fetchMock.post(
                `${configMock.server}/${API_EXPERIMENTS_URL}`,
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
                // get the experiments stored in the indexDB
                const experiments = instance.experiments;

                expect(experiments.length).toBe(1);
                expect(experiments).toEqual(MockDataStoredIndexDBWithNew);
                expect(spyTrackPageView).toBeCalledTimes(2);
            });

            fetchMock.post(
                `${configMock.server}/${API_EXPERIMENTS_URL}`,
                {
                    status: 200,
                    body: After15DaysIsUserIncludedResponse,
                    headers: {
                        Accept: 'application/json',
                        'Content-Type': 'application/json'
                    }
                },
                { overwriteRoutes: true }
            );

            // Third try, after 15 days
            const location = { ...LocationMock, href: 'http://localhost/destinations' };

            jest.spyOn(Date, 'now').mockImplementation(
                () => MOCK_CURRENT_TIMESTAMP + TIME_15_DAYS_MILLISECONDS
            );
            await instance.locationChanged(location).then(() => {
                // get the experiments stored in the indexDB
                const experiments = instance.experiments;

                expect(experiments.length).toBe(1);
                expect(experiments).toEqual(MockDataStoredIndexDBWithNew15DaysLater);
                expect(spyTrackPageView).toBeCalledTimes(3);
            });
        });
    });
});
