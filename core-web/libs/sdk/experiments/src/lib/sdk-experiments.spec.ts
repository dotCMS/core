import fakeIndexedDB from 'fake-indexeddb';
import fetchMock from 'fetch-mock';

import { API_EXPERIMENTS_URL, EXPERIMENT_DB_KEY_PATH } from './constants';
import { IsUserIncludedResponse } from './mocks/is-user-included.mock';
import { SdkExperimentConfig } from './models';
// import { persistData } from './persistence/persistence';
import { getData, persistData } from './persistence/persistence';
import { SdkExperiments } from './sdk-experiments';

if (!globalThis.structuredClone) {
    globalThis.structuredClone = function (obj) {
        return JSON.parse(JSON.stringify(obj));
    };
}

describe('SdkExperiments', () => {
    afterEach(() => {
        fetchMock.restore();
    });
    const configStub: SdkExperimentConfig = {
        'api-key': 'yourApiKey',
        server: 'yourServerUrl',
        debug: false
    };

    it('returns mocked experiments data when server returns correctly', async () => {
        fetchMock.mock(`${configStub.server}${API_EXPERIMENTS_URL}`, {
            body: IsUserIncludedResponse,
            status: 200
        });

        const instance = SdkExperiments.getInstance(configStub);

        expect(instance).toBeInstanceOf(SdkExperiments);
    });

    it('throws error when server returns error', async () => {
        fetchMock.mock(`${configStub.server}${API_EXPERIMENTS_URL}`, 500);

        try {
            SdkExperiments.getInstance(configStub);
        } catch (error) {
            expect(error).toEqual('HTTP error! status: 500');
        }
    });

    it('is debug active', async () => {
        const instance = SdkExperiments.getInstance(configStub);
        expect(instance.getIsDebugActive()).toBe(false);
    });

    it('calls persistData when persisting experiments', async () => {
        const expectedData = IsUserIncludedResponse.entity;

        Object.defineProperty(window, 'indexedDB', {
            writable: true,
            value: fakeIndexedDB
        });

        fetchMock.mock(`${configStub.server}${API_EXPERIMENTS_URL}`, {
            body: IsUserIncludedResponse,
            status: 200
        });

        const key = await persistData(expectedData);
        expect(key).toBe(EXPERIMENT_DB_KEY_PATH);

        const data = await getData();
        expect(data).toEqual(expectedData);
    });
});
