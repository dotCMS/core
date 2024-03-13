import fakeIndexedDB from 'fake-indexeddb';

import { IndexDBDatabaseHandler } from './index-db-database-handler';

import { EXPERIMENT_DB_KEY_PATH, EXPERIMENT_DB_STORE_NAME } from '../constants';
import { IsUserIncludedResponse } from '../mocks/mock';

if (!globalThis.structuredClone) {
    globalThis.structuredClone = function (obj) {
        return JSON.parse(JSON.stringify(obj));
    };
}

let persistDatabaseHandler: IndexDBDatabaseHandler;

beforeAll(() => {
    Object.defineProperty(window, 'indexedDB', {
        writable: true,
        value: fakeIndexedDB
    });

    persistDatabaseHandler = new IndexDBDatabaseHandler({
        db_store: EXPERIMENT_DB_STORE_NAME,
        db_name: EXPERIMENT_DB_STORE_NAME,
        db_key_path: EXPERIMENT_DB_KEY_PATH
    });
});

describe('IndexedDB tests', () => {
    it('saveData successfully saves data to the store', async () => {
        const key = await persistDatabaseHandler.persistData(IsUserIncludedResponse.entity);
        expect(key).toBe(EXPERIMENT_DB_KEY_PATH);
    });

    it('getDataByKey successfully retrieves data from the store', async () => {
        await persistDatabaseHandler.persistData(IsUserIncludedResponse.entity);
        const data = await persistDatabaseHandler.getData();
        expect(data).toEqual(IsUserIncludedResponse.entity);
    });
});
