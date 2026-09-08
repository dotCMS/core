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
    it('should call getScriptDataAttributes and set window[EXPERIMENT_WINDOWS_KEY]', async () => {
        const fakeInstance = {
            initialize: vi.fn()
        } as unknown as DotExperiments;

        const getInstanceMock = vi
            .spyOn(DotExperiments, 'getInstance')
            .mockReturnValue(fakeInstance);

        // A dynamic import, not require(): this file is served as ESM under Vite,
        // where `require` does not exist — it failed with "Cannot find module
        // './standalone'", naming the module rather than the module system. The import
        // still has to happen inside the test, after the spy is installed, because the
        // module's side effect IS what is under test.
        await import('./standalone');

        expect(getScriptDataAttributes).toHaveBeenCalled();

        expect(getInstanceMock).toHaveBeenCalledWith({ server: 'http://localhost' });
        expect(getInstanceMock).toHaveBeenCalled();

        expect(window[EXPERIMENT_WINDOWS_KEY]).toBeDefined();
        expect(window[EXPERIMENT_WINDOWS_KEY]).toEqual(fakeInstance);
    });
});
