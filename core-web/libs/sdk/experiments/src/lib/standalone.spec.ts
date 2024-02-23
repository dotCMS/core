import { getScriptDataAttributes } from './utils/utils';
import { SdkExperiments } from './sdk-experiments';

jest.mock('./utils/utils', () => ({
    getScriptDataAttributes: jest.fn().mockReturnValue({ mode: 'js', server: 'http://localhost' }),
    Logger: jest.fn()
}));

describe('IIFE Execution', () => {
    it('should call getScriptDataAttributes and set window.experiment', () => {
        const getInstanceMock = jest
            .spyOn(SdkExperiments, 'getInstance')
            .mockReturnValue({} as SdkExperiments);

        require('./standalone');

        expect(getScriptDataAttributes).toHaveBeenCalled();

        expect(getInstanceMock).toHaveBeenCalledWith({ mode: 'js', server: 'http://localhost' });
        expect(getInstanceMock).toHaveBeenCalled();
    });
});
