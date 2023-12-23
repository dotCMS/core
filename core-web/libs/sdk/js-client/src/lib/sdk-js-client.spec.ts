import { sdkJsClient } from './sdk-js-client';

describe('sdkJsClient', () => {
    it('should work', () => {
        expect(sdkJsClient()).toEqual('sdk-js-client');
    });
});
