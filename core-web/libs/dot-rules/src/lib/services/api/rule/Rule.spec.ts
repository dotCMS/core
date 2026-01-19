import { RuleModel } from './Rule';

describe('Unit.api.rule-engine.Rule', () => {
    beforeEach(() => {
        // Setup
    });

    it("Isn't valid when new.", () => {
        const foo = new RuleModel({});
        expect(foo.isValid()).toEqual(false);
    });
});
