import { SdkExperiments } from './sdk-experiments';
import { getScriptDataAttributes } from './utils/utils';

declare global {
    interface Window {
        dotcmsExperiment: never;
    }
}

jest.mock('./utils/utils', () => ({
    getScriptDataAttributes: jest.fn().mockReturnValue({ server: 'http://localhost' }),
    Logger: jest.fn()
}));

describe('IIFE Execution', () => {
    beforeEach(() => {
        delete window.dotcmsExperiment;
    });
    it('should call getScriptDataAttributes and set window.experiment', () => {
        const fakeInstance = {} as SdkExperiments;

        const getInstanceMock = jest
            .spyOn(SdkExperiments, 'getInstance')
            .mockReturnValue(fakeInstance);

        require('./standalone');

        expect(getScriptDataAttributes).toHaveBeenCalled();

        expect(getInstanceMock).toHaveBeenCalledWith({ server: 'http://localhost' });
        expect(getInstanceMock).toHaveBeenCalled();

        expect(window.dotcmsExperiment).toBeDefined();
        expect(window.dotcmsExperiment).toEqual(fakeInstance);
    });
});
