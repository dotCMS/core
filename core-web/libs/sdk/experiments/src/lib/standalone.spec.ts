import { vi } from 'vitest';

import { DotExperiments } from './dot-experiments';
import { EXPERIMENT_WINDOWS_KEY } from './shared/constants';
import { getScriptDataAttributes } from './shared/utils/utils';

declare global {
    interface Window {
        [EXPERIMENT_WINDOWS_KEY]: DotExperiments;
    }
}

vi.mock('./shared/utils/utils', () => ({
    getScriptDataAttributes: vi.fn().mockReturnValue({ server: 'http://localhost' }),
    Logger: vi.fn()
}));

describe('IIFE Execution', () => {
    it('should call getScriptDataAttributes and set window[EXPERIMENT_WINDOWS_KEY]', () => {
        const fakeInstance = {
            initialize: vi.fn()
        } as unknown as DotExperiments;

        const getInstanceMock = vi
            .spyOn(DotExperiments, 'getInstance')
            .mockReturnValue(fakeInstance);

        require('./standalone');

        expect(getScriptDataAttributes).toHaveBeenCalled();

        expect(getInstanceMock).toHaveBeenCalledWith({ server: 'http://localhost' });
        expect(getInstanceMock).toHaveBeenCalled();

        expect(window[EXPERIMENT_WINDOWS_KEY]).toBeDefined();
        expect(window[EXPERIMENT_WINDOWS_KEY]).toEqual(fakeInstance);
    });
});
