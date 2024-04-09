jest.mock('@dotcms/client', () => ({
    isInsideEditor: jest.fn()
}));

describe('useExperiments', () => {
    beforeEach(() => {
        jest.clearAllMocks(); // Limpia los mocks antes de cada prueba
    });

    //WIP
    it('should call locationChanged if not inside the editor', () => {
        pending('WIP');
    });
});
