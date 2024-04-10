jest.mock('@dotcms/client', () => ({
    isInsideEditor: jest.fn()
}));

export const DotExperiments = {
    getInstance: jest.fn().mockImplementation((config) => ({
        ready: jest.fn().mockResolvedValue(true),
        config
    }))
};

describe('DotExperimentsProvider', () => {
    it('initializes DotExperiments instance when not inside the editor', async () => {
        //
    });
});
