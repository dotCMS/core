import { testExternalLibrary } from './test-external-library';

describe('testExternalLibrary', () => {
    it('should work', () => {
        expect(testExternalLibrary()).toEqual('test-external-library');
    });
});
