import { sdkExperiment } from '@dotcms/sdk/experiments';

describe('sdkExperiments', () => {
  let sdk: sdkExperiment;
  beforeEach(() => {
    sdk = new sdkExperiment();
  });
  it('should work', () => {
    expect(sdk.init()).toEqual('Pending Implementation');
  });
});
