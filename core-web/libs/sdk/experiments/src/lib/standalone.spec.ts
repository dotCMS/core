import { EXPERIMENT_WINDOWS_KEY } from './constants';
import { SdkExperiments } from './sdk-experiments';
import { getScriptDataAttributes } from './utils/utils';

declare global {
    interface Window {
        [EXPERIMENT_WINDOWS_KEY]: never;
    }
}

jest.mock('./utils/utils', () => ({
    getScriptDataAttributes: jest.fn().mockReturnValue({ server: 'http://localhost' }),
    Logger: jest.fn()
}));

describe('IIFE Execution', () => {
    beforeEach(() => {
        delete window[EXPERIMENT_WINDOWS_KEY];
    });
    it('should call getScriptDataAttributes and set window[EXPERIMENT_WINDOWS_KEY]', () => {
        const fakeInstance = {} as SdkExperiments;

        const getInstanceMock = jest
            .spyOn(SdkExperiments, 'getInstance')
            .mockReturnValue(fakeInstance);

        require('./standalone');

        expect(getScriptDataAttributes).toHaveBeenCalled();

        expect(getInstanceMock).toHaveBeenCalledWith({ server: 'http://localhost' });
        expect(getInstanceMock).toHaveBeenCalled();

        expect(window[EXPERIMENT_WINDOWS_KEY]).toBeDefined();
        expect(window[EXPERIMENT_WINDOWS_KEY]).toEqual(fakeInstance);
    });
});
