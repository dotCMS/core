import { jitsuClient } from '@jitsu/sdk-js';
import fakeIndexedDB from 'fake-indexeddb';
import fetchMock from 'fetch-mock';

import { API_EXPERIMENTS_URL, EXPERIMENT_DB_KEY_PATH, EXPERIMENT_DB_STORE_NAME } from './constants';
import { DotExperiments } from './dot-experiments';
import { ExpectedExperimentsParsedEvent, IsUserIncludedResponse } from './mocks/mock';
import { DotExperimentConfig } from './models';
import { IndexDBDatabaseHandler } from './persistence/index-db-database-handler';

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
    afterEach(() => {
        fetchMock.restore();
    });
    const configMock: DotExperimentConfig = {
        'api-key': 'yourApiKey',
        server: 'yourServerUrl',
        debug: false
    };
    const persistDatabase = new IndexDBDatabaseHandler({
        db_store: EXPERIMENT_DB_STORE_NAME,
        db_name: EXPERIMENT_DB_STORE_NAME,
        db_key_path: EXPERIMENT_DB_KEY_PATH
    });

    it('returns mocked experiments data when server returns correctly', async () => {
        fetchMock.mock(`${configMock.server}${API_EXPERIMENTS_URL}`, {
            body: IsUserIncludedResponse,
            status: 200
        });

        const instance = DotExperiments.getInstance(configMock);

        expect(instance).toBeInstanceOf(DotExperiments);
    });

    it('throws error when server returns error', async () => {
        fetchMock.mock(`${configMock.server}${API_EXPERIMENTS_URL}`, 500);

        try {
            DotExperiments.getInstance(configMock);
        } catch (error) {
            expect(error).toEqual('HTTP error! status: 500');
        }
    });

    it('is debug active', async () => {
        const instance = DotExperiments.getInstance(configMock);
        expect(instance.getIsDebugActive()).toBe(false);
    });

    it('calls persistData when persisting experiments', async () => {
        const expectedData = IsUserIncludedResponse.entity;

        fetchMock.mock(`${configMock.server}${API_EXPERIMENTS_URL}`, {
            body: IsUserIncludedResponse,
            status: 200
        });

        const key = await persistDatabase.persistData(expectedData);
        expect(key).toBe(EXPERIMENT_DB_KEY_PATH);

        const data = await persistDatabase.getData();
        expect(data).toEqual(expectedData);
    });

    it('should initialize jitsuClient with correct configuration', async () => {
        DotExperiments.getInstance(configMock);

        await new Promise(process.nextTick);

        expect(jitsuClient).toHaveBeenCalledWith({
            key: configMock['api-key'],
            tracking_host: configMock.server,
            log_level: configMock.debug ? 'DEBUG' : 'WARN'
        });
    });

    it('should call set with the correct arguments', async () => {
        DotExperiments.getInstance(configMock);

        await new Promise(process.nextTick);

        const mockJitsuClientInstance = (jitsuClient as jest.MockedFunction<typeof jitsuClient>)
            .mock.results[0].value;

        expect(mockJitsuClientInstance.set).toHaveBeenCalledWith({
            experiments: [...ExpectedExperimentsParsedEvent]
        });
    });
});
