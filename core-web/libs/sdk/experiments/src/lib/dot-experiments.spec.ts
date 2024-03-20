/* eslint-disable @typescript-eslint/ban-ts-comment */
import fakeIndexedDB from 'fake-indexeddb';
import fetchMock from 'fetch-mock';

import { DotExperiments } from './dot-experiments';
import { DotExperimentConfig } from './models';

jest.mock('@jitsu/sdk-js', () => ({
    jitsuClient: jest.fn().mockImplementation(() => ({
        set: jest.fn()
    }))
}));

Object.defineProperty(window, 'indexedDB', {
    writable: true,
    value: fakeIndexedDB
});

if (!globalThis.structuredClone) {
    globalThis.structuredClone = function (obj) {
        return JSON.parse(JSON.stringify(obj));
    };
}

describe('DotExperiments', () => {
    const configMock: DotExperimentConfig = {
        'api-key': 'yourApiKey',
        server: 'yourServerUrl',
        debug: false
    };

    afterEach(() => {
        fetchMock.restore();
    });

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
            const instance = DotExperiments.getInstance(configMock);
            expect(instance).toBeInstanceOf(DotExperiments);
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

        it('should call all the necessary at initialize()', async () => {
            const instance = DotExperiments.getInstance(configMock);
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const spyGetPersistedData = jest.spyOn(instance as any, 'getPersistedData');
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            const spySetExperimentData = jest.spyOn(instance as any, 'setExperimentData');
            const spySetinitializeDatabaseHandler = jest.spyOn(
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                instance as any,
                'initializeDatabaseHandler'
            );

            spyGetPersistedData.mockResolvedValueOnce(undefined);

            spySetExperimentData.mockResolvedValueOnce(undefined);
            spySetinitializeDatabaseHandler.mockResolvedValueOnce(undefined);

            await instance.initialize();

            expect(spyGetPersistedData).toHaveBeenCalled();
            expect(spySetExperimentData).toHaveBeenCalled();
            expect(spySetinitializeDatabaseHandler).toHaveBeenCalled();
        });

        it('should return false if the debug inactive', async () => {
            const instance = DotExperiments.getInstance(configMock);
            expect(instance.getIsDebugActive()).toBe(false);
        });
    });
});
