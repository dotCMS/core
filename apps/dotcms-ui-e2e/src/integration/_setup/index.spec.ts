import Utils from '@e2e/shared/Utils';

describe('Setup', () => {
    it('Sets initial DB data', async () => {
        await Utils.DBSeed();
    });
});
