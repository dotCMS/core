import { render, waitFor } from '@testing-library/react';

import * as dotcmsClient from '@dotcms/client';

import { DotExperimentsProvider } from './DotExperimentsProvider';

import { DotExperiments } from '../dot-experiments';

jest.mock('../dot-experiments');
jest.mock('@dotcms/client');

const mockDotExperimentsInstance = {
    getInstance: jest.fn().mockResolvedValue(true),
    ready: jest.fn().mockResolvedValue(true),
    locationChanged: jest.fn().mockResolvedValue(true)
};

describe('DotExperimentsProvider', () => {
    beforeEach(() => {
        DotExperiments.getInstance = jest.fn().mockReturnValue(mockDotExperimentsInstance);
    });

    it('initializes DotExperiments instance when not inside the editor', async () => {
        const config = { apiKey: 'key', server: 'server', debug: true };

        jest.spyOn(dotcmsClient, 'isInsideEditor').mockReturnValue(true);

        const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation();

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

        jest.spyOn(dotcmsClient, 'isInsideEditor').mockReturnValue(false);

        const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation();

        render(
            <DotExperimentsProvider config={config}>
                <div>Test</div>
            </DotExperimentsProvider>
        );

        await waitFor(() => expect(DotExperiments.getInstance).toHaveBeenCalled());

        consoleWarnSpy.mockRestore();
    });
});
