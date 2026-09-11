import { render, waitFor } from '@testing-library/react';
import { vi } from 'vitest';

import { UVE_MODE, UVEState } from '@dotcms/types';
import * as uve from '@dotcms/uve';

import { DotExperimentsProvider } from './DotExperimentsProvider';

import { DotExperiments } from '../dot-experiments';

vi.mock('../dot-experiments');
vi.mock('@dotcms/client');

const mockDotExperimentsInstance = {
    getInstance: vi.fn().mockResolvedValue(true),
    ready: vi.fn().mockResolvedValue(true),
    locationChanged: vi.fn().mockResolvedValue(true)
};

describe('DotExperimentsProvider', () => {
    beforeEach(() => {
        DotExperiments.getInstance = vi.fn().mockReturnValue(mockDotExperimentsInstance);
    });

    it('initializes DotExperiments instance when not inside the editor', async () => {
        const config = { apiKey: 'key', server: 'server', debug: true };

        vi.spyOn(uve, 'getUVEState').mockReturnValue({ mode: UVE_MODE.EDIT } as UVEState);

        const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => undefined);

        render(
            <DotExperimentsProvider config={config}>
                <div>Test</div>
            </DotExperimentsProvider>
        );

        await waitFor(() => expect(DotExperiments.getInstance).not.toHaveBeenCalled());

        expect(consoleWarnSpy).toHaveBeenCalledWith(
            'DotExperimentsProvider: DotExperiments instance not initialized because it is inside the editor.'
        );

        consoleWarnSpy.mockRestore();
    });

    it('initializes DotExperiments instance when is inside the editor', async () => {
        const config = { apiKey: 'key', server: 'server', debug: true };

        vi.spyOn(uve, 'getUVEState').mockReturnValue(undefined);

        const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => undefined);

        render(
            <DotExperimentsProvider config={config}>
                <div>Test</div>
            </DotExperimentsProvider>
        );

        await waitFor(() => expect(DotExperiments.getInstance).toHaveBeenCalled());

        consoleWarnSpy.mockRestore();
    });
});
