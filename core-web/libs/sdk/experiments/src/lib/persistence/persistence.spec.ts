import fakeIndexedDB from 'fake-indexeddb';

import { getData, persistData } from './persistence';

import { EXPERIMENT_DB_KEY_PATH } from '../constants';
import { IsUserIncludedResponse } from '../mocks/is-user-included.mock';

if (!globalThis.structuredClone) {
    globalThis.structuredClone = function (obj) {
        return JSON.parse(JSON.stringify(obj));
    };
}

beforeAll(() => {
    Object.defineProperty(window, 'indexedDB', {
        writable: true,
        value: fakeIndexedDB
    });
});

describe('IndexedDB tests', () => {
    it('saveData successfully saves data to the store', async () => {
        const key = await persistData(IsUserIncludedResponse.entity);
        expect(key).toBe(EXPERIMENT_DB_KEY_PATH);
    });

    it('getDataByKey successfully retrieves data from the store', async () => {
        await persistData(IsUserIncludedResponse.entity);
        const data = await getData();
        expect(data).toEqual(IsUserIncludedResponse.entity);
    });
});
