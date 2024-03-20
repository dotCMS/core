import { EXPERIMENT_WINDOWS_KEY } from './constants';
import { DotExperiments } from './dot-experiments';
import { getScriptDataAttributes } from './utils/utils';

declare global {
    interface Window {
        [EXPERIMENT_WINDOWS_KEY]: DotExperiments;
    }
}

jest.mock('./utils/utils', () => ({
    getScriptDataAttributes: jest.fn().mockReturnValue({ server: 'http://localhost' }),
    Logger: jest.fn()
}));

describe('IIFE Execution', () => {
    it('should call getScriptDataAttributes and set window[EXPERIMENT_WINDOWS_KEY]', () => {
        const fakeInstance = {
            initialize: jest.fn()
        } as unknown as DotExperiments;

        const getInstanceMock = jest
            .spyOn(DotExperiments, 'getInstance')
            .mockReturnValue(fakeInstance);

        const initializeMock = jest.spyOn(fakeInstance, 'initialize');

        require('./standalone');

        expect(getScriptDataAttributes).toHaveBeenCalled();

        expect(getInstanceMock).toHaveBeenCalledWith({ server: 'http://localhost' });
        expect(getInstanceMock).toHaveBeenCalled();

        expect(initializeMock).toHaveBeenCalled();

        expect(window[EXPERIMENT_WINDOWS_KEY]).toBeDefined();
        expect(window[EXPERIMENT_WINDOWS_KEY]).toEqual(fakeInstance);
    });
});
