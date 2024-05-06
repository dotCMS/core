import fakeIndexedDB from 'fake-indexeddb';

import { IndexDBDatabaseHandler } from './index-db-database-handler';

import {
    EXPERIMENT_ALREADY_CHECKED_KEY,
    EXPERIMENT_DB_KEY_PATH,
    EXPERIMENT_DB_STORE_NAME
} from '../constants';
import { IsUserIncludedResponse } from '../mocks/mock';
import { checkFlagExperimentAlreadyChecked } from '../utils/utils';

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

describe('SessionStorage EXPERIMENT_ALREADY_CHECKED_KEY handle', () => {
    Object.defineProperty(window, 'sessionStorage', {
        value: {
            setItem: jest.fn(),
            getItem: jest.fn()
        },
        writable: true
    });
    it('should set true to sessionStorage key `EXPERIMENT_ALREADY_CHECKED_KEY` ', () => {
        const value = 'true';

        persistDatabaseHandler.setFlagExperimentAlreadyChecked();

        expect(window.sessionStorage.setItem).toHaveBeenLastCalledWith(
            EXPERIMENT_ALREADY_CHECKED_KEY,
            value
        );
    });

    it('should set true to sessionStorage key `EXPERIMENT_ALREADY_CHECKED_KEY` ', () => {
        const value = 'true';

        persistDatabaseHandler.setFlagExperimentAlreadyChecked();

        expect(window.sessionStorage.setItem).toHaveBeenLastCalledWith(
            EXPERIMENT_ALREADY_CHECKED_KEY,
            value
        );
    });

    describe('checkFlagExperimentAlreadyChecked', () => {
        const getItemMock = window.sessionStorage.getItem as jest.MockedFunction<
            typeof window.sessionStorage.getItem
        >;

        const testCases = [
            { value: '', expected: false, description: 'sessionStorage value is ""' },
            { value: 'true', expected: true, description: 'sessionStorage value is "true"' },
            { value: null, expected: false, description: 'sessionStorage value is null' }
        ];

        testCases.forEach(({ description, value, expected }) => {
            it(`returns ${expected} when ${description}`, () => {
                getItemMock.mockReturnValue(value);

                expect(checkFlagExperimentAlreadyChecked()).toBe(expected);
                expect(getItemMock).toHaveBeenCalledWith(EXPERIMENT_ALREADY_CHECKED_KEY);
            });
        });
    });
});
