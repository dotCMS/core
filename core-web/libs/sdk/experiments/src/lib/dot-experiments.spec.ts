import fakeIndexedDB from 'fake-indexeddb';
import fetchMock from 'fetch-mock';

import { API_EXPERIMENTS_URL, EXPERIMENT_DB_KEY_PATH, EXPERIMENT_DB_STORE_NAME } from './constants';
import { DotExperiments } from './dot-experiments';
import { IsUserIncludedResponse } from './mocks/is-user-included.mock';
import { DotExperimentConfig } from './models';
import { IndexDBDatabaseHandler } from './persistence/index-db-database-handler';

if (!globalThis.structuredClone) {
    globalThis.structuredClone = function (obj) {
        return JSON.parse(JSON.stringify(obj));
    };
}

describe('DotExperiments', () => {
    afterEach(() => {
        fetchMock.restore();
    });
    const configStub: DotExperimentConfig = {
        'api-key': 'yourApiKey',
        server: 'yourServerUrl',
        debug: false
    };

    it('returns mocked experiments data when server returns correctly', async () => {
        fetchMock.mock(`${configStub.server}${API_EXPERIMENTS_URL}`, {
            body: IsUserIncludedResponse,
            status: 200
        });

        const instance = DotExperiments.getInstance(configStub);

        expect(instance).toBeInstanceOf(DotExperiments);
    });

    it('throws error when server returns error', async () => {
        fetchMock.mock(`${configStub.server}${API_EXPERIMENTS_URL}`, 500);

        try {
            DotExperiments.getInstance(configStub);
        } catch (error) {
            expect(error).toEqual('HTTP error! status: 500');
        }
    });

    it('is debug active', async () => {
        const instance = DotExperiments.getInstance(configStub);
        expect(instance.getIsDebugActive()).toBe(false);
    });

    it('calls persistData when persisting experiments', async () => {
        const expectedData = IsUserIncludedResponse.entity;
        const persistDatabase = new IndexDBDatabaseHandler({
            db_store: EXPERIMENT_DB_STORE_NAME,
            db_name: EXPERIMENT_DB_STORE_NAME,
            db_key_path: EXPERIMENT_DB_KEY_PATH
        });

        Object.defineProperty(window, 'indexedDB', {
            writable: true,
            value: fakeIndexedDB
        });

        fetchMock.mock(`${configStub.server}${API_EXPERIMENTS_URL}`, {
            body: IsUserIncludedResponse,
            status: 200
        });

        const key = await persistDatabase.persistData(expectedData);
        expect(key).toBe(EXPERIMENT_DB_KEY_PATH);

        const data = await persistDatabase.getData();
        expect(data).toEqual(expectedData);
    });
});
