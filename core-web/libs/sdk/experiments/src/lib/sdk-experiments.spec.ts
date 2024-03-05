import { DotSdkExperiment } from './sdk-experiments';

describe('sdkExperiments', () => {
    let sdk: DotSdkExperiment;
    beforeEach(() => {
        sdk = new DotSdkExperiment();
    });
    it('should work', () => {
        expect(sdk.init()).toEqual('Pending Implementation');
    });
});
